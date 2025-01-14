/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.functional.*
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass.Configuration
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn

/**
 * Returns a string that allows a human to identify the node. Mostly, this is simply the node's
 * localName, but for Literals, it is their value
 */
fun nodeNameToString(node: Node): Name {
    return if (node is Literal<*>) Name((node as? Literal<*>)?.value.toString()) else node.name
}

/**
 * Resolve a MemberExpression as long as it's base no longer is a MemberExpression itself. Returns
 * the base a Name that identifies the access
 */
fun resolveMemberExpression(node: MemberExpression): Pair<Node, Name> {
    // As long as the base in itself is a MemberExpression, resolve that one
    var base: Node = node
    var newLocalname = ""
    while (base is MemberExpression) {
        val b = base.name.split("::")
        val tmp = if (b.size > 1) b[1] else ""
        newLocalname = if (newLocalname.isEmpty()) tmp else "$tmp.$newLocalname"
        base = base.base
    }

    return Pair(base, Name(newLocalname))
}

@DependsOn(SymbolResolver::class)
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(DFGPass::class)
class PointsToPass(ctx: TranslationContext) : EOGStarterPass(ctx, orderDependencies = true) {

    // For recursive creation of FunctionSummaries, we have to make sure that we don't run in
    // circles.
    // Therefore, we store the chain of FunctionDeclarations we currently analyse
    val functionSummaryAnalysisChain = mutableSetOf<FunctionDeclaration>()

    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(node: Node) {
        functionSummaryAnalysisChain.clear()
        return acceptInternal(node)
    }

    fun acceptInternal(node: Node) {
        // For now, we only execute this for function declarations, we will support all EOG starters
        // in the future.
        if (node !is FunctionDeclaration) {
            return
        }
        // If the node already has a function summary, we have visited it before and can
        // return here.
        if (
            node.functionSummary.isNotEmpty() &&
                node.functionSummary.keys.any { it in node.parameters || it in node.returns }
        ) {
            return
        }

        // Skip empty functions
        if (node.body == null) {
            return
        }

        // Calculate the complexity of the function and see, if it exceeds our threshold
        val max = passConfig<Configuration>()?.maxComplexity
        val c = node.body?.cyclomaticComplexity ?: 0
        if (max != null && c > max) {
            log.info(
                "Ignoring function ${node.name} because its complexity (${c}) is greater than the configured maximum (${max})"
            )
            return
        }

        log.trace("Handling {} (complexity: {})", node.name, c)

        var startState = PointsToState2()
        startState =
            startState.pushToDeclarationsState(
                node,
                TupleLattice(
                    Pair(PowersetLattice(identitySetOf()), PowersetLattice(identitySetOf()))
                )
            )

        startState = initializeParameters(node.parameters, startState)

        val finalState = iterateEOGClean(node.nextEOGEdges, startState, ::transfer)
        if (finalState !is PointsToState2) return

        /* Store function summary for this FunctionDeclaration. */
        storeFunctionSummary(node, finalState)

        for ((key, value) in finalState.generalState.elements) {
            // All nodes in the state get new memoryValues, Expressions and Declarations
            // additionally get new MemoryAddresses
            val newMemoryValues = value.elements.second.elements
            val newMemoryAddresses = value.elements.first.elements as Collection<Node>
            if (newMemoryValues.isNotEmpty()) {
                key.prevDFG.clear()
                key.prevDFG.addAll(newMemoryValues)
            }
            if (newMemoryAddresses.isNotEmpty()) {
                when (key) {
                    is Expression -> {
                        key.memoryAddress.clear()
                        key.memoryAddress.addAll(newMemoryAddresses)
                    }
                    is Declaration -> {
                        if (
                            newMemoryAddresses.size == 1 &&
                                newMemoryAddresses.first() is MemoryAddress
                        )
                            key.memoryAddress = newMemoryAddresses.first() as MemoryAddress
                    }
                }
            }
        }
    }

    private fun storeFunctionSummary(node: FunctionDeclaration, doubleState: PointsToState2) {
        node.parameters.forEach { param ->
            // fetch the parameter's current address and value
            val addresses = doubleState.getAddresses(param)
            val values = addresses.flatMap { doubleState.getValues(it) }
            val indexes = mutableSetOf<Pair<Node, String>>()

            // Add the values
            // indexes.addAll(values.map { Pair(it, "") })
            values.forEach { value ->
                indexes.add(Pair(value, ""))
                // Additionally check for partial writes to fields
                // For the partial writes, we also store the field name in the function Summary
                // TODO: we are going too far here
                /*                doubleState.fetchElementFromDeclarationState(value, true).filter {
                    it.name != param.name
                }*/
            }
            // TODO
            /*            doubleState
            .fetchElementFromDeclarationState(param.memoryValue)
            // .filter { it is ParameterMemoryValue && it.name.parent == param.name }
            .forEach { indexes.add(Pair(it, "")) }*/

            indexes.forEach { (index, subAccessName) ->
                /*                val finalValue =
                doubleState.declarationsState.elements
                    .filter { it.key == index }
                    .entries
                    .firstOrNull()
                    ?.value
                    ?.elements
                    ?.second
                    ?.elements*/
                val finalValue =
                    doubleState.fetchElementFromDeclarationState(index, true).filter {
                        it.name != param.name
                    }
                finalValue
                    // See if we can find something that is different from the initial value
                    ?.filter {
                        !(it is ParameterMemoryValue &&
                            it.name.localName.contains("derefvalue") &&
                            it.name.parent == param.name)
                    }
                    // If so, store the last write for the parameter in the FunctionSummary
                    ?.forEach { value ->
                        // TODO: Do we also map the writes of pointer-to-pointer to the param or
                        // should we do something else?
                        node.functionSummary
                            .computeIfAbsent(param) { mutableSetOf() }
                            .add(FunctionDeclaration.FSEntry(true, value, true, subAccessName))
                    }
            }
        }
    }

    protected fun transfer(currentEdge: Edge<Node>, state: LatticeElement<*>): PointsToState2 {
        val currentNode = currentEdge.end

        var doubleState = state as PointsToState2

        // Used to keep iterating for steps which do not modify the alias-state otherwise
        doubleState =
            doubleState.pushToDeclarationsState(
                currentNode,
                doubleState.getFromDecl(currentEdge.end)
                    ?: TupleLattice(Pair(emptyPowersetLattice(), emptyPowersetLattice()))
            )

        doubleState =
            when (currentNode) {
                is Declaration,
                is MemoryAddress -> handleDeclaration(currentNode, doubleState)
                is AssignExpression -> handleAssignExpression(currentNode, doubleState)
                is UnaryOperator -> handleUnaryOperator(currentNode, doubleState)
                is CallExpression -> handleCallExpression(currentNode, doubleState)
                is Expression -> handleExpression(currentNode, doubleState)
                is ReturnStatement -> handleReturnStatement(currentNode, doubleState)
                else -> doubleState
            }

        return doubleState
    }

    private fun handleReturnStatement(
        currentNode: ReturnStatement,
        doubleState: PointsToPass.PointsToState2
    ): PointsToPass.PointsToState2 {
        /* For Return Statements, all we really want to do is to collect their return values
        to add them to the FunctionSummary */
        var doubleState = doubleState
        if (currentNode.returnValues.isNotEmpty()) {
            val parentFD =
                currentNode.firstParentOrNull { it is FunctionDeclaration } as? FunctionDeclaration
            if (parentFD != null) {
                currentNode.returnValues.forEach { retval ->
                    parentFD.functionSummary
                        .computeIfAbsent(currentNode) { mutableSetOf() }
                        .addAll(
                            doubleState.getValues(retval).map {
                                FunctionDeclaration.FSEntry(false, it, false, "")
                            }
                        )
                }
            }
        }
        return doubleState
    }

    private fun handleCallExpression(
        currentNode: CallExpression,
        doubleState: PointsToPass.PointsToState2
    ): PointsToPass.PointsToState2 {
        var doubleState = doubleState

        val mapDstToSrc = mutableMapOf<Node, MutableSet<Node>>()

        // First, check if there are missing FunctionSummaries
        currentNode.language?.let { language ->
            currentNode.invokes.addAll(
                ctx.config.functionSummaries.run {
                    this.functionToDFGEntryMap
                        .filterKeys { it.methodName == currentNode.name.localName }
                        .map { (declEntry, summary) ->
                            this.generateFunctionForEntry(
                                this@PointsToPass,
                                language,
                                declEntry,
                                summary
                            )
                        }
                        .filter { it !in currentNode.invokes }
                }
            )
        }
        var i = 0
        val invokes = currentNode.invokes.toList()
        while (i < invokes.size) {
            val invoke = invokes[i]
            if (invoke.functionSummary.isEmpty()) {
                if (invoke.hasBody()) {
                    log.debug("functionSummaryAnalysisChain: {}", functionSummaryAnalysisChain)
                    if (invoke !in functionSummaryAnalysisChain) {
                        val summaryCopy = functionSummaryAnalysisChain.toSet()
                        functionSummaryAnalysisChain.add(invoke)
                        acceptInternal(invoke)
                        functionSummaryAnalysisChain.clear()
                        functionSummaryAnalysisChain.addAll(summaryCopy)
                    } else {
                        log.error(
                            "Cannot calculate functionSummary for $invoke as it's recursively called. callChain: $functionSummaryAnalysisChain"
                        )
                        val newValues: MutableSet<FunctionDeclaration.FSEntry> =
                            invoke.parameters
                                .map { FunctionDeclaration.FSEntry(false, it, false, "") }
                                .toMutableSet()
                        invoke.functionSummary[ReturnStatement()] = newValues
                    }
                } else {
                    // Add a dummy function summary so that we don't try this every time
                    // In this dummy, all parameters point to the return
                    // TODO: This actually generates a new return statement but it's not part of the
                    // function. Wouldn't the edges better point to the FunctionDeclaration and in a
                    // case with a body, all returns flow to the FunctionDeclaration too?
                    val newValues: MutableSet<FunctionDeclaration.FSEntry> =
                        invoke.parameters
                            .map { FunctionDeclaration.FSEntry(false, it, false, "") }
                            .toMutableSet()
                    invoke.functionSummary[ReturnStatement()] = newValues
                }
            }
            currentNode.arguments.forEach { arg ->
                if (arg.argumentIndex < invoke.parameters.size) {
                    // Create a DFG-Edge from the argument to the parameter's memoryValue
                    val p = invoke.parameters[arg.argumentIndex]
                    val pValue = doubleState.getValues(p)
                    pValue.forEach { value ->
                        doubleState =
                            doubleState.push(
                                value,
                                TupleLattice(
                                    Pair(
                                        PowersetLattice(pValue),
                                        PowersetLattice(identitySetOf(arg))
                                    )
                                )
                            )
                    }
                }
            }
            // }

            // If we have a FunctionSummary, we push the values of the arguments and return value
            // after executing the function call to our doubleState.
            for ((param, fsEntries) in invoke.functionSummary) {
                val destination =
                    when (param) {
                        is ParameterDeclaration ->
                            // Dereference the parameter
                            if (param.argumentIndex < currentNode.arguments.size) {
                                currentNode.arguments[param.argumentIndex]
                            } else null
                        is ReturnStatement -> currentNode
                        else -> null
                    }
                if (destination != null) {
                    fsEntries.forEach { (derefDestination, value, derefSource, subAccessName) ->
                        val dst =
                            if (subAccessName.isNotEmpty()) {
                                val fieldAddresses = identitySetOf<Node>()
                                // Collect the fieldAddresses for each possible value
                                val d =
                                    if (derefDestination) doubleState.getValues(destination)
                                    else doubleState.getAddresses(destination)
                                d.forEach { v ->
                                    val parentName = nodeNameToString(v)
                                    val newName = Name(subAccessName, parentName)
                                    fieldAddresses.addAll(
                                        doubleState.getFieldAddresses(identitySetOf(v), newName)
                                    )
                                }
                                fieldAddresses
                            } else {
                                if (derefDestination) doubleState.getValues(destination)
                                else doubleState.getAddresses(destination)
                            }
                        when (value) {
                            is ParameterDeclaration ->
                                // Add the value of the respective argument in the CallExpression
                                // Only dereference the parameter when we stored that in the
                                // functionSummary
                                if (value.argumentIndex < currentNode.arguments.size) {
                                    if (derefSource) {
                                        doubleState
                                            .getValues(currentNode.arguments[value.argumentIndex])
                                            .forEach { v ->
                                                dst.forEach { d ->
                                                    mapDstToSrc.computeIfAbsent(d) {
                                                        mutableSetOf<Node>()
                                                    } +=
                                                        doubleState
                                                            .fetchElementFromDeclarationState(v)
                                                }
                                            }
                                    } else {
                                        dst.forEach {
                                            mapDstToSrc.computeIfAbsent(it) {
                                                mutableSetOf<Node>()
                                            } += currentNode.arguments[value.argumentIndex]
                                        }
                                    }
                                }
                            is ParameterMemoryValue -> {
                                // In case the FunctionSummary says that we have to use the
                                // dereferenced value here, we look up the argument, dereference it,
                                // and then add it to the sources
                                if (value.name.localName == "derefvalue") {
                                    val p =
                                        currentNode.invokes
                                            .flatMap { it.parameters }
                                            .filter { it.name == value.name.parent }
                                    p.forEach {
                                        if (it.argumentIndex < currentNode.arguments.size) {
                                            val arg = currentNode.arguments[it.argumentIndex]
                                            dst.forEach { d ->
                                                mapDstToSrc.computeIfAbsent(d) { mutableSetOf() } +=
                                                    doubleState.getValues(arg).flatMap {
                                                        doubleState.getValues(it)
                                                    }
                                            }
                                        }
                                    }
                                }
                            }
                            is MemoryAddress -> {
                                dst.forEach { d ->
                                    mapDstToSrc.computeIfAbsent(d) { mutableSetOf() } += value
                                }
                            }
                            else -> {
                                dst.forEach { d ->
                                    mapDstToSrc.computeIfAbsent(d) { mutableSetOf<Node>() } +=
                                        doubleState.getValues(value)
                                }
                            }
                        }
                    }
                }
            }
            i++
        }

        mapDstToSrc.forEach { (dst, src) ->
            // If the values of the destination are the same as the destination (e.g. if dst is a
            // CallExpression), we also add destinations to update the generalState, otherwise, the
            // destinationAddresses for the DeclarationState are enough
            val dstValues = doubleState.getValues(dst)
            if (dstValues.all { it == dst })
                doubleState = doubleState.updateValues(src, dstValues, setOf(dst))
            else doubleState = doubleState.updateValues(src, identitySetOf(), setOf(dst))
            // doubleState = doubleState.updateValues(src, identitySetOf(), setOf(dst))
        }

        return doubleState
    }

    private fun handleUnaryOperator(
        currentNode: UnaryOperator,
        doubleState: PointsToPass.PointsToState2
    ): PointsToPass.PointsToState2 {
        var doubleState = doubleState
        /* For UnaryOperators, we have to update the value if it's a ++ or -- operator
         */
        // TODO: Check out cases where the input is no Reference
        if (currentNode.operatorCode in (listOf("++", "--")) && currentNode.input is Reference) {
            val addresses = doubleState.getAddresses(currentNode)
            val newDeclState = doubleState.declarationsState.elements.toMutableMap()
            /* Update the declarationState for the refersTo */
            doubleState.getAddresses(currentNode.input).forEach { addr ->
                newDeclState.replace(
                    addr,
                    TupleLattice(
                        Pair(
                            PowersetLattice(addresses),
                            PowersetLattice(identitySetOf(currentNode))
                        )
                    )
                )
            }
            // TODO: Should we already update the input's value in the generalState, or is it
            // enough at the next use?
            doubleState = PointsToState2(doubleState.generalState, MapLattice(newDeclState))
        }
        return doubleState
    }

    private fun handleAssignExpression(
        currentNode: AssignExpression,
        doubleState: PointsToPass.PointsToState2
    ): PointsToPass.PointsToState2 {
        var doubleState = doubleState
        /* For AssignExpressions, we update the value of the rhs with the lhs
         * In C(++), both the lhs and the rhs should only have one element
         */
        if (currentNode.lhs.size == 1 && currentNode.rhs.size == 1) {
            val sources = currentNode.rhs.flatMap { doubleState.getValues(it) }.toIdentitySet()
            val destinations = currentNode.lhs.map { it }.toIdentitySet().toIdentitySet()
            val destinationsAddresses =
                destinations.flatMap { doubleState.getAddresses(it) }.toIdentitySet()
            doubleState = doubleState.updateValues(sources, destinations, destinationsAddresses)
        }

        return doubleState
    }

    private fun handleExpression(
        currentNode: Expression,
        doubleState: PointsToPass.PointsToState2
    ): PointsToPass.PointsToState2 {
        var doubleState = doubleState
        /* For MemberExpressions and SubscriptExpressions, we may have to create a memoryAddress first */
        if (currentNode is MemberExpression) {
            val (base, fieldName) = resolveMemberExpression(currentNode)
            doubleState =
                doubleState.createFieldAddresses(
                    doubleState.getAddresses(base).toIdentitySet(),
                    fieldName
                )
        } else if (currentNode is SubscriptExpression) {
            val fieldName = nodeNameToString(currentNode.subscriptExpression)
            doubleState =
                doubleState.createFieldAddresses(
                    doubleState.getValues(currentNode.base).toIdentitySet(),
                    Name(fieldName.localName, currentNode.base.name)
                )
        }

        /* If we have an Expression that is written to, we handle it later and ignore it now */
        val access =
            if (currentNode is Reference) currentNode.access
            else if (currentNode is SubscriptExpression && currentNode.arrayExpression is Reference)
                (currentNode.arrayExpression as Reference).access
            else null
        if (access == AccessValues.READ) {
            val addresses = doubleState.getAddresses(currentNode)
            val values = doubleState.getValues(currentNode)

            doubleState =
                doubleState.push(
                    currentNode,
                    TupleLattice(Pair(PowersetLattice(addresses), PowersetLattice(values)))
                )
        }
        return doubleState
    }

    private fun handleDeclaration(
        currentNode: Node,
        doubleState: PointsToPass.PointsToState2
    ): PointsToState2 {
        /* No need to set the address, this already happens in the constructor */
        val addresses = doubleState.getAddresses(currentNode)

        val values = identitySetOf<Node>()

        (currentNode as? HasInitializer)?.initializer?.let { initializer ->
            if (initializer is Literal<*>) values.add(initializer)
            else values.addAll(doubleState.getValues(initializer))
        }

        var doubleState =
            doubleState.push(
                currentNode,
                TupleLattice(Pair(PowersetLattice(addresses), PowersetLattice(values)))
            )
        /* In the DeclarationsState, we save the address which we wrote to the value for easier work with pointers
         * */
        addresses.forEach { addr ->
            doubleState =
                doubleState.pushToDeclarationsState(
                    addr,
                    TupleLattice(Pair(PowersetLattice(addresses), PowersetLattice(values)))
                )
        }
        return doubleState
    }

    private fun initializeParameters(
        parameters: MutableList<ParameterDeclaration>,
        doubleState: PointsToState2
    ): PointsToState2 {
        var doubleState = doubleState
        // Until which depth do we create ParameterMemoryValues
        val depth = 2
        parameters.forEach { param ->
            // In the first step, we have a triangle of ParameterDeclaration, the
            // ParameterDeclaration's Memory Address and the ParameterMemoryValue
            // Therefore, the src and the addresses are different. For all other depths, we set both
            // to the ParameterMemoryValue we create in the first step
            var src: Node = param
            var addresses = doubleState.getAddresses(src)
            for (i in 0..depth) {
                val pmvName = "deref".repeat(i) + "value"
                val pmv =
                    ParameterMemoryValue(Name(pmvName)).apply {
                        memoryAddress = addresses.first() /* TODO: might there also be more? */
                    }
                val state: LatticeElement<Pair<PowersetLatticeT<Node>, PowersetLatticeT<Node>>> =
                    TupleLattice(
                        Pair(PowersetLattice(addresses), PowersetLattice(identitySetOf(pmv)))
                    )
                addresses.forEach { addr ->
                    doubleState = doubleState.pushToDeclarationsState(addr, state)
                }
                doubleState = doubleState.push(src, state)
                // prepare for next step
                src = pmv
                addresses = setOf(pmv)
            }
        }
        return doubleState
    }

    protected class PointsToState2(
        generalState:
            LatticeElement<
                Map<
                    Node, LatticeElement<Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>>
                >
            > =
            MapLattice(mutableMapOf()),
        declarationsState:
            LatticeElement<
                Map<
                    Node, LatticeElement<Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>>
                >
            > =
            MapLattice(mutableMapOf())
    ) :
        TupleLattice<
            Map<Node, LatticeElement<Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>>>,
            Map<Node, LatticeElement<Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>>>
        >(Pair(generalState, declarationsState)) {
        override fun lub(
            other:
                LatticeElement<
                    Pair<
                        MapLatticeT<
                            Node,
                            LatticeElement<
                                Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>
                            >
                        >,
                        MapLatticeT<
                            Node,
                            LatticeElement<
                                Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>
                            >
                        >
                    >
                >
        ) =
            PointsToState2(
                this.generalState.lub(other.elements.first),
                this.elements.second.lub(other.elements.second)
            )

        override fun duplicate() =
            PointsToState2(elements.first.duplicate(), elements.second.duplicate())

        val generalState:
            MapLatticeT<
                Node, LatticeElement<Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>>
            >
            get() = this.elements.first

        val declarationsState:
            MapLatticeT<
                Node, LatticeElement<Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>>
            >
            get() = this.elements.second

        fun get(
            key: Node
        ): LatticeElement<Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>>? {
            return this.generalState.elements[key] ?: this.declarationsState.elements[key]
        }

        fun getFromDecl(
            key: Node
        ): LatticeElement<Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>>? {
            return this.declarationsState.elements[key]
        }

        fun push(
            newNode: Node,
            newLatticeElement:
                LatticeElement<Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>>
        ): PointsToState2 {
            val newGeneralState =
                this.generalState.lub(MapLattice(mutableMapOf(Pair(newNode, newLatticeElement))))
            return PointsToState2(newGeneralState, declarationsState)
        }

        /** Pushes the [newNode] and its [newLatticeElement] to the [declarationsState]. */
        fun pushToDeclarationsState(
            newNode: Node,
            newLatticeElement:
                LatticeElement<Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>>
        ): PointsToState2 {
            val newDeclarationsState =
                this.declarationsState.lub(
                    MapLattice(mutableMapOf(Pair(newNode, newLatticeElement)))
                )
            return PointsToState2(generalState, newDeclarationsState)
        }

        override fun equals(other: Any?): Boolean {
            if (other !is PointsToState2) return false
            return other.elements.first == this.elements.first &&
                other.elements.second == this.elements.second
        }

        /**
         * Fetch the entry for `node` from the DeclarationState. If there isn't any, create an
         * UnknownMemoryValue
         */
        fun fetchElementFromDeclarationState(
            node: Node,
            fetchFields: Boolean = false
        ): IdentitySet<Node> {
            val ret = identitySetOf<Node>()

            // First, the main element
            val elements = this.declarationsState.elements[node]?.elements?.second?.elements
            if (elements.isNullOrEmpty()) {
                val newName = nodeNameToString(node)
                val newEntry = identitySetOf<Node>(UnknownMemoryValue(newName))
                (this.declarationsState.elements
                        as?
                        MutableMap<
                            Node,
                            LatticeElement<
                                Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>
                            >
                        >)
                    ?.computeIfAbsent(node) {
                        TupleLattice(
                            Pair(PowersetLattice(identitySetOf(node)), PowersetLattice(newEntry))
                        )
                    }
                val newElements = this.declarationsState.elements[node]?.elements?.second?.elements
                (newElements as? IdentitySet<Node>)?.addAll(newEntry)
                // return newEntry
                ret.addAll(newEntry)
            } else // return elements.toIdentitySet()
             ret.addAll(elements)

            // if fetchFields is true, we also fetch the values for fields
            if (fetchFields) {
                val fields =
                    this.declarationsState.elements[node]?.elements?.first?.elements?.filter {
                        it != node
                    }
                fields?.forEach { field ->
                    this.declarationsState.elements[field]?.elements?.second?.elements?.let {
                        ret.addAll(it)
                    }
                }
            }

            return ret
        }

        fun getValues(node: Node): Set<Node> {
            return when (node) {
                is PointerReference -> {
                    /* For PointerReferences, the value is the address of the input
                     * For example, the value of `&i` is the address of `i`
                     * */
                    this.getAddresses(node.input)
                }
                is PointerDereference -> {
                    /* To find the value for PointerDereferences, we first check what's the current value of the input, which is probably a MemoryAddress
                     * Then we look up the current value at this MemoryAddress
                     */
                    val inputVal =
                        when (node.input) {
                            is Reference -> this.getValues(node.input)
                            else -> // TODO: How can we handle other cases?
                            identitySetOf(UnknownMemoryValue(node.name))
                        }
                    val retVal = identitySetOf<Node>()
                    inputVal.forEach { input ->
                        retVal.addAll(fetchElementFromDeclarationState(input, true))
                    }
                    retVal
                }
                is Declaration -> {
                    /* For Declarations, we have to look up the last value written to it.
                     */
                    if (!node.memoryAddressIsInitialized())
                        node.memoryAddress = MemoryAddress(node.name)
                    fetchElementFromDeclarationState(node)
                }
                is MemoryAddress -> {
                    fetchElementFromDeclarationState(node)
                }
                is Reference -> {
                    /* For References, we have to look up the last value written to its declaration.
                     */
                    this.getAddresses(node).flatMap { this.getValues(it) }.toIdentitySet()
                }
                is CastExpression -> {
                    this.getValues(node.expression)
                }
                is UnaryOperator -> this.getValues(node.input)
                is SubscriptExpression -> {
                    this.getAddresses(node).flatMap { this.getValues(it) }.toIdentitySet()
                }
                is CallExpression -> {
                    identitySetOf(node)
                }
                /*is BinaryOperator -> identitySetOf(node)*/
                /* In these cases, we simply have to fetch the current value for the MemoryAddress from the DeclarationState */
                else -> /*fetchElementFromDeclarationState(node)*/ identitySetOf(node)
            }
        }

        fun getAddresses(node: Node): Set<Node> {
            return when (node) {
                is Declaration -> {
                    /*
                     * For declarations, we created a new MemoryAddress node, so that's the one we use here
                     */
                    if (!node.memoryAddressIsInitialized())
                        node.memoryAddress = MemoryAddress(node.name)
                    identitySetOf(node.memoryAddress)
                }
                is ParameterMemoryValue -> {
                    if (node.memoryAddress != null) identitySetOf(node.memoryAddress!!)
                    else identitySetOf()
                }
                is MemoryAddress -> {
                    TODO()
                    //                    fetchElementFromDeclarationState(node, useAddress = true)
                    identitySetOf(node)
                }
                is PointerReference -> {
                    identitySetOf()
                }
                is PointerDereference -> {
                    /*
                    PointerDereferences have as address the value of their input.
                    For example, the address of `*a` is the value of `a`
                     */
                    this.getValues(node.input)
                }
                is MemberExpression -> {
                    /*
                     * For MemberExpressions, the fieldAddresses in the MemoryAddress node of the base hold the information we are looking for
                     */
                    val (base, newName) = resolveMemberExpression(node)
                    getFieldAddresses(this.getAddresses(base).toIdentitySet(), newName)
                }
                is Reference -> {
                    /*
                    For references, the address is the same as for the declaration, AKA the refersTo
                    */
                    node.refersTo?.let { refersTo ->
                        /* In some cases, the refersTo might not yet have an initialized MemoryAddress, for example if it's a FunctionDeclaration. So let's to this here */
                        if (!refersTo.memoryAddressIsInitialized())
                            refersTo.memoryAddress = MemoryAddress(node.name)

                        identitySetOf(refersTo.memoryAddress)
                    } ?: identitySetOf()
                }
                is CastExpression -> {
                    /*
                    For CastExpressions we take the expression as the cast itself does not have any impact on the address
                     */
                    this.getAddresses(node.expression)
                }
                is SubscriptExpression -> {
                    val localName = nodeNameToString(node.subscriptExpression)
                    this.getValues(node.base)
                        .flatMap {
                            getFieldAddresses(
                                identitySetOf(it),
                                Name(localName.localName, nodeNameToString(it))
                            )
                        }
                        .toIdentitySet()
                }
                else -> identitySetOf(node)
            }
        }

        /**
         * Create the field `nodeName` at the addresses in `baseAddresses`. If fieldAddresses
         * already exit, do nothing
         */
        fun createFieldAddresses(baseAddresses: IdentitySet<Node>, nodeName: Name): PointsToState2 {
            var doubleState = this

            baseAddresses.forEach { addr ->
                /* If we do not yet have a MemoryAddress for this FieldDeclaration, we create one */
                val addrState = declarationsState.elements[addr]
                if (
                    addrState == null ||
                        addrState.elements.first.elements.filter { it.name == nodeName }.isEmpty()
                ) {
                    val fieldAddress = MemoryAddress(nodeName, addr)
                    doubleState =
                        pushToDeclarationsState(
                            addr,
                            TupleLattice(
                                Pair(
                                    PowersetLattice(identitySetOf(fieldAddress)),
                                    PowersetLattice(identitySetOf())
                                )
                            )
                        )
                }
            }
            return doubleState
        }

        /**
         * Lookup the field `nodeName` at the addresses in `baseAddresses` and return their values
         */
        fun getFieldAddresses(baseAddresses: IdentitySet<Node>, nodeName: Name): Set<Node> {
            val fieldAddresses = identitySetOf<Node>()

            baseAddresses.forEach { addr ->
                declarationsState.elements[addr]
                    ?.elements
                    ?.first
                    ?.elements
                    ?.filter { it.name.localName == nodeName.localName }
                    ?.let { fieldAddresses.addAll(it) }
            }
            return fieldAddresses
        }

        /**
         * Updates the declarationState at `destinationAddresses` to the values in `sources`.
         * Additionally updates the generalstate at `destinations` if there is any
         */
        fun updateValues(
            sources: Set<Node>,
            destinations: Set<Node>,
            destinationAddresses: Set<Node>
        ): PointsToState2 {
            // val addresses = destinations.flatMap { this.getAddresses(it) }.toIdentitySet()
            val newDeclState = this.declarationsState.elements.toMutableMap()
            val newGenState = this.generalState.elements.toMutableMap()
            /* Update the declarationState for the address */
            destinationAddresses.forEach { addr ->
                newDeclState[addr] =
                    TupleLattice(
                        Pair(PowersetLattice(destinationAddresses), PowersetLattice(sources))
                    )
            }
            /* Also update the generalState for dst (if we have any destinations) */
            destinations.forEach { d ->
                newGenState[d] =
                    TupleLattice(
                        Pair(PowersetLattice(destinationAddresses), PowersetLattice(sources))
                    )
            }
            var doubleState = PointsToState2(MapLattice(newGenState), MapLattice(newDeclState))

            /* When we are dealing with SubscriptExpression, we also have to initialise the arrayExpression
            , since that hasn't been done yet */
            destinations.filterIsInstance<SubscriptExpression>().forEach { d ->
                val AEaddresses = this.getAddresses(d.arrayExpression)
                val AEvalues = this.getValues(d.arrayExpression)

                doubleState =
                    doubleState.push(
                        d.arrayExpression,
                        TupleLattice(Pair(PowersetLattice(AEaddresses), PowersetLattice(AEvalues)))
                    )
            }

            return doubleState
        }
    }
}

/**
 * It's most likely a stub if it calls the same method and has only a call and return and maximum
 * one more statement.
 */
fun FunctionDeclaration.isStub(): Boolean {
    val bodySize = (body as? Block)?.statements?.size
    return body == null ||
        (bodySize != null &&
            bodySize <= 3 &&
            this.calls.singleOrNull()?.name == this.name &&
            this.returns.singleOrNull() != null)
}
