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
            val addresses = doubleState.getAddresses(param)
            val indexes = mutableSetOf<Node>()
            addresses.forEach { addr ->
                indexes.addAll(doubleState.getValues(addr))
                // Additionally check for partial writes to fields
                if (addr is MemoryAddress) {
                    addr.fieldAddresses.flatMap { it.value }.forEach { indexes.add(it) }
                }
            }
            indexes.forEach { index ->
                val finalValue =
                    doubleState.declarationsState.elements
                        .filter { it.key == index }
                        .entries
                        .firstOrNull()
                        ?.value
                        ?.elements
                        ?.second
                        ?.elements
                finalValue
                    // See if we can find something that is different from the initial value
                    ?.filter {
                        !(it is ParameterMemoryValue &&
                            it.name.localName == "derefvalue" &&
                            it.name.parent == param.name)
                    }
                    // If so, store the last write for the parameter in the FunctionSummary
                    ?.forEach { value ->
                        node.functionSummary
                            .computeIfAbsent(param) { mutableSetOf() }
                            .add(Pair(value, true))
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
                        .addAll(doubleState.getValues(retval).map { Pair(it, false) })
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
        currentNode.invokes.forEach { invoke ->
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
                        val newValues: MutableSet<Pair<Node, Boolean>> =
                            invoke.parameters.map { Pair(it, false) }.toMutableSet()
                        invoke.functionSummary[ReturnStatement()] = newValues
                    }
                } else {
                    // Add a dummy function summary so that we don't try this every time
                    // In this dummy, all parameters point to the return
                    // TODO: This actually generates a new return statement but it's not part of the
                    // function. Wouldn't the edges better point to the FunctionDeclaration and in a
                    // case with a body, all returns flow to the FunctionDeclaration too?
                    val newValues: MutableSet<Pair<Node, Boolean>> =
                        invoke.parameters.map { Pair(it, false) }.toMutableSet()
                    invoke.functionSummary[ReturnStatement()] = newValues
                }
            }
            currentNode.arguments.forEach { arg ->
                if (arg.argumentIndex < invoke.parameters.size) {
                    // Create a DFG-Edge from the argument to the parameter's memoryValue
                    val p = invoke.parameters[arg.argumentIndex]
                    doubleState =
                        doubleState.push(
                            p.memoryValue,
                            TupleLattice(
                                Pair(
                                    PowersetLattice(identitySetOf(p.memoryValue)),
                                    PowersetLattice(identitySetOf(arg))
                                )
                            )
                        )
                }
            }
            // }

            // If we have a FunctionSummary, we push the
            // values of the arguments and return value after executing the function call to our
            // doubleState.

            // First, collect all writes to all parameters
            // val changedParams = mutableMapOf<Node, MutableSet<Pair<Node, Boolean>>>()
            // currentNode.invokes
            //    .forEach { fd ->
            //      for ((k, v) in invoke.functionSummary) {
            //          changedParams.computeIfAbsent(k) { mutableSetOf() }.addAll(v)
            //      }
            // }

            for ((param, newValues) in invoke.functionSummary) {

                // Ignore the ReturnStatements here, we use them when handling AssignExpressions
                // if (param !is ReturnStatement) {
                val destinations =
                    when (param) {
                        is ParameterDeclaration ->
                            // Dereference the parameter
                            if (param.argumentIndex < currentNode.arguments.size) {
                                doubleState.getValues(currentNode.arguments[param.argumentIndex])
                            } else null
                        is ReturnStatement -> identitySetOf(currentNode)
                        else -> null
                    }
                if (destinations != null) {
                    val sources = mutableSetOf<Node>()
                    newValues.forEach { (value, derefSource) ->
                        when (value) {
                            is ParameterDeclaration ->
                                // Add the value of the respective argument in the CallExpression
                                // Only dereference the parameter when we stored that in the
                                // functionSummary
                                if (value.argumentIndex < currentNode.arguments.size) {
                                    if (derefSource) {
                                        doubleState
                                            .getValues(currentNode.arguments[value.argumentIndex])
                                            .forEach { sources.addAll(doubleState.getValues(it)) }
                                    } else {
                                        sources.add(currentNode.arguments[value.argumentIndex])
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
                                            sources.addAll(
                                                doubleState.getValues(arg).flatMap {
                                                    doubleState.getValues(it)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            else -> sources.add(value)
                        }
                    }
                    if (sources.isNotEmpty()) {
                        destinations.forEach { dst ->
                            mapDstToSrc.computeIfAbsent(dst) { mutableSetOf<Node>() } += sources
                        }
                    }
                    // }
                }
            }
        }

        mapDstToSrc.forEach { (dst, src) ->
            doubleState = doubleState.updateValues(src, setOf(dst))
        }
        // }

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
            // We fetch the value of the source, but not the destination, this is done by the
            // updateValues-Function
            val sources = identitySetOf<Node>()
            currentNode.rhs.forEach { sources.addAll(doubleState.getValues(it)) }
            doubleState = doubleState.updateValues(sources, currentNode.lhs.toIdentitySet())
        }

        return doubleState
    }

    private fun handleExpression(
        currentNode: Expression,
        doubleState: PointsToPass.PointsToState2
    ): PointsToPass.PointsToState2 {
        var doubleState = doubleState
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
        parameters.forEach { param ->
            val addresses = doubleState.getAddresses(param)
            param.memoryValue.name = Name("value", param.name)
            val paramState: LatticeElement<Pair<PowersetLatticeT<Node>, PowersetLatticeT<Node>>> =
                TupleLattice(
                    Pair(
                        PowersetLattice(addresses),
                        PowersetLattice(identitySetOf(param.memoryValue))
                    )
                )
            // We also need to track the MemoryValue of the dereference of the parameter, since that
            // is what would have an influence outside the function
            val paramDerefState:
                LatticeElement<Pair<PowersetLatticeT<Node>, PowersetLatticeT<Node>>> =
                TupleLattice(
                    Pair(
                        PowersetLattice(identitySetOf(param.memoryValue)),
                        PowersetLattice(
                            identitySetOf(ParameterMemoryValue(Name("derefvalue", param.name)))
                        )
                    )
                )
            addresses.forEach { addr ->
                doubleState = doubleState.pushToDeclarationsState(addr, paramState)
            }
            doubleState = doubleState.pushToDeclarationsState(param.memoryValue, paramDerefState)

            doubleState = doubleState.push(param, paramState)
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

        /* Fetch the entry for `node` from the DeclarationState. If there isn't any, create
        an UnknownMemoryValue
        */
        private fun fetchElementFromDeclarationState(node: Node): IdentitySet<Node> {
            val elements = this.declarationsState.elements[node]?.elements?.second?.elements
            if (elements.isNullOrEmpty()) {
                val newName = if (node is Literal<*>) Name(node.value.toString()) else node.name
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
                (this.declarationsState.elements[node]?.elements?.second?.elements
                        as? IdentitySet<Node>)
                    ?.addAll(newEntry)
                return newEntry
            } else return elements.toIdentitySet()
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
                    inputVal.forEach { retVal.addAll(this.getValues(it)) }
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
                is SubscriptExpression ->
                    this.getAddresses(node).flatMap { this.getValues(it) }.toIdentitySet()
                is CallExpression -> {
                    identitySetOf(node)
                    // Let's see if we have a functionSummary for the CallExpression
                    /*val functionDeclaration =
                        node.invokes.firstOrNull()
                            ?: return identitySetOf(UnknownMemoryValue(node.name))
                    val functionSummaries = node.ctx?.config?.functionSummaries
                    if (
                        functionSummaries?.hasSummary(functionDeclaration) == true &&
                            // Also check that we don't just have a dummy Summary
                            node.ctx
                                ?.config
                                ?.functionSummaries
                                ?.getLastWrites(functionDeclaration)
                                ?.isNotEmpty() == true
                    ) {
                        // Get all the ReturnValues from the Summary and return their values
                        val retVals =
                            node.ctx?.config?.functionSummaries?.getLastWrites(functionDeclaration)
                        if (retVals != null) {
                            val r = identitySetOf<Node>()
                            for ((param, values) in retVals) {
                                if (param is ReturnStatement) {
                                    values.forEach { (v, derefSource) ->
                                        if (v is ParameterDeclaration) {
                                            if (derefSource) {
                                                this.getValues(node.arguments[v.argumentIndex])
                                                    .forEach { r.addAll(this.getValues(it)) }
                                            } else if (v.argumentIndex < node.arguments.size) {
                                                r.add(node.arguments[v.argumentIndex])
                                            }
                                        } else r.add(v)
                                    }
                                }
                            }
                            r
                        } else identitySetOf(UnknownMemoryValue(node.name))
                    } else identitySetOf(UnknownMemoryValue(node.name))*/
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
                is MemoryAddress -> {
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
                    // TODO: Are there any cases where the address of the base is no MemoryAddress?
                    // but still relevant for us?
                    getFieldAddresses(
                        this.getAddresses(node.base).filterIsInstance<MemoryAddress>(),
                        /*node.refersTo?.name.toString(),*/
                        node.name
                    )
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
                    val localName =
                        if (node.subscriptExpression is Literal<*>)
                            (node.subscriptExpression as? Literal<*>)?.value.toString()
                        else node.subscriptExpression.name.toString()
                    getFieldAddresses(
                        this.getAddresses(node.base).filterIsInstance<MemoryAddress>(),
                        Name(localName, node.arrayExpression.name)
                    )
                }
                else -> identitySetOf()
            }
        }

        /*
         * Look up the `indexString` in the `baseAddress`es and return the fieldAddresses
         * If no MemoryAddress exits at `indexString`, it will be created
         */
        fun getFieldAddresses(baseAddresses: List<MemoryAddress>, nodeName: Name): Set<Node> {
            val fieldAddresses = identitySetOf<MemoryAddress>()

            /* Theoretically, the base can have multiple addresses. Additionally, also the fieldDeclaration can have multiple Addresses. To simplify, we flatten the set and collect all possible addresses of the fieldDeclaration in a flat set */
            baseAddresses.forEach { addr ->
                addr.fieldAddresses[nodeName.localName]?.forEach { fieldAddresses.add(it) }
            }
            /* If we do not yet have a MemoryAddress for this FieldDeclaration, we create one */
            if (fieldAddresses.isEmpty()) {
                val newMemoryAddress = MemoryAddress(nodeName)

                fieldAddresses.add(newMemoryAddress)
                baseAddresses.forEach { addr ->
                    addr.fieldAddresses[nodeName.localName] = identitySetOf(newMemoryAddress)
                }
            }
            return fieldAddresses
        }

        /**
         * Update the node `dst` to the values of `src`. Updates the declarationState and the
         * generalState for `dst` For the destination, we dereference ourselves, b/c we also need
         * the destination themselves to update the set. However, the source will not be
         * dereferences b/c we don't need if there's the need to
         */
        fun updateValues(sources: Set<Node>, destinations: Set<Node>): PointsToState2 {
            val addresses = destinations.flatMap { this.getAddresses(it) }.toIdentitySet()
            val newDeclState = this.declarationsState.elements.toMutableMap()
            val newGenState = this.generalState.elements.toMutableMap()
            /* Update the declarationState for the address */
            addresses.forEach { addr ->
                newDeclState[addr] =
                    TupleLattice(Pair(PowersetLattice(addresses), PowersetLattice(sources)))
            }
            /* Also update the generalState for dst */
            destinations.forEach { d ->
                newGenState[d] =
                    TupleLattice(Pair(PowersetLattice(addresses), PowersetLattice(sources)))
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
