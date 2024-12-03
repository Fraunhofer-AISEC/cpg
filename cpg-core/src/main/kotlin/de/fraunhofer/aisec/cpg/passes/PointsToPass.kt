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
import de.fraunhofer.aisec.cpg.helpers.functional.*
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass.Configuration
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore

@DependsOn(SymbolResolver::class)
@DependsOn(EvaluationOrderGraphPass::class)
@ExecuteBefore(ControlFlowSensitiveDFGPass::class)
class PointsToPass(ctx: TranslationContext) : EOGStarterPass(ctx, orderDependencies = true) {

    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(node: Node) {
        // For now, we only execute this for function declarations, we will support all EOG starters
        // in the future.
        if (node !is FunctionDeclaration) {
            return
        }
        // If the node already has a function summary, we have visited it before and can
        // return here.
        if (config.functionSummaries.hasSummary(node)) {
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

        // clearFlowsOfVariableDeclarations(node)
        // val startState = PointsToState2<Node?>()
        // val startState = PointsToState2(Pair(emptyMapLattice<Node?,
        // TupleLattice<PowersetLattice<Node?>, PowersetLattice<Node?>>>(), emptyMapLattice<Node?,
        // TupleLattice<PowersetLattice<Node?>, PowersetLattice<Node?>>>()))
        // val startState = PointsToState(Pair<LatticeElement<Map<Node?,
        // TupleLattice<emptyPowersetLattice<Node?>(), emptyPowersetLattice<Node?>()>>>,
        // LatticeElement<Map<Node?, TupleLattice(Pair(emptyPowersetLattice<Node?>() ,
        // emptyPowersetLattice<Node?>())) >>>)
        // val test = emptyMapLattice<Node?, Pair<PowersetLattice<Node?>, PowersetLattice<Node?>>>()

        /*MapLattice(
        mapOf(Pair(null)) TupleLattice(Pair(emptyPowersetLattice<Node?>() , emptyPowersetLattice<Node?>()))*/

        var startState = PointsToState2()
        startState =
            startState.pushToDeclarationsState(
                node,
                TupleLattice(
                    Pair(PowersetLattice(identitySetOf()), PowersetLattice(identitySetOf()))
                )
            )
        node.parameters.forEach { param ->
            val addresses = startState.getAddresses(param)
            // val values = startState.getValues(param)
            val parameterMemoryValue = ParameterMemoryValue(Name("value", param.name))
            val paramState: LatticeElement<Pair<PowersetLatticeT<Node>, PowersetLatticeT<Node>>> =
                TupleLattice(
                    Pair(PowersetLattice(addresses), PowersetLattice(setOf(parameterMemoryValue)))
                )
            // We also need to track the MemoryValue of the dereference of the parameter, since that
            // is what would have an influence outside the function
            val paramDerefState:
                LatticeElement<Pair<PowersetLatticeT<Node>, PowersetLatticeT<Node>>> =
                TupleLattice(
                    Pair(
                        PowersetLattice(identitySetOf(parameterMemoryValue)),
                        PowersetLattice(setOf(ParameterMemoryValue(Name("derefvalue", param.name))))
                    )
                )
            addresses.forEach { addr ->
                startState = startState.pushToDeclarationsState(addr, paramState)
            }
            startState = startState.pushToDeclarationsState(parameterMemoryValue, paramDerefState)

            startState = startState.push(param, paramState)
        }
        val finalState = iterateEOGClean(node.nextEOGEdges, startState, ::transfer)
        if (finalState !is PointsToState2) return

        /* Store function summary for this FunctionDeclaration.
        Let's start with the parameters
         */
        node.parameters.forEach { param ->
            val indexes = mutableSetOf<Node>()
            startState.getAddresses(param).forEach { indexes.addAll(startState.getValues(it)) }
            indexes.forEach { index ->
                val finalValue =
                    finalState.declarationsState.elements
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
                        println("Parameter $param's last write: $value")
                        config.functionSummaries.functionToChangedParameters
                            .computeIfAbsent(node) { mutableMapOf() }
                            .computeIfAbsent(param) { mutableSetOf() }
                            .add(value)
                    }
            }
        }
        // TODO: Check if the return value contains any parameter values
        /* Now the return values */
        val retValues =
            finalState.declarationsState.elements
                .filter { it.key is ReturnStatement }
                .entries
                .firstOrNull()
                ?.value
                ?.elements
                ?.second
                ?.elements

        for ((key, value) in finalState.generalState.elements) {
            when (key) {
                is Expression -> {
                    val newMemoryValues = value.elements.second.elements
                    val newMemoryAddresses =
                        value.elements.first.elements.filterIsInstance<MemoryAddress>()
                    if (newMemoryValues.isNotEmpty()) {
                        key.memoryValue.clear()
                        key.memoryValue.addAll(newMemoryValues)
                    }
                    if (newMemoryAddresses.isNotEmpty()) {
                        key.memoryAddress.clear()
                        key.memoryAddress.addAll(newMemoryAddresses)
                    }
                }
                is ValueDeclaration -> {
                    val newMemoryValues = value.elements.second.elements
                    if (newMemoryValues.isNotEmpty()) {
                        key.memoryValue.clear()
                        key.memoryValue.addAll(newMemoryValues)
                    }
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
                is Declaration -> handleDeclaration(currentNode, doubleState)
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
        /* For Return Statements, all we really want to do is to collect the values they return to later create the FunctionSummary */
        var doubleState = doubleState
        if (currentNode.returnValues.isNotEmpty()) {
            // val newDeclState = doubleState.declarationsState.elements.toMutableMap()
            val newGenState = doubleState.generalState.elements.toMutableMap()
            /* Add the values to the declarationState */
            // TODO: use DeclState
            newGenState[currentNode] =
                TupleLattice(
                    Pair(
                        PowersetLattice(setOf(currentNode)),
                        PowersetLattice(currentNode.returnValues.toSet())
                    )
                )
            doubleState = PointsToState2(MapLattice(newGenState), doubleState.declarationsState)
            //            doubleState = PointsToState2(doubleState.generalState,
            // MapLattice(newDeclState))
        }
        return doubleState
    }

    private fun handleCallExpression(
        currentNode: CallExpression,
        doubleState: PointsToPass.PointsToState2
    ): PointsToPass.PointsToState2 {
        var doubleState = doubleState

        // TODO: Check if we have a function summary for everything in currentNode.invokes
        if (currentNode.invokes.all { ctx.config.functionSummaries.hasSummary(it) }) {
            // We already have a FunctionSummary. Set the arguments. Push the
            // values of the arguments and return value after executing the function call to our
            // doubleState.
            // TODO: Use all invokes elements
            val changedParams =
                ctx.config.functionSummaries.getLastWrites(currentNode.invokes.first())
            for ((param, newValue) in changedParams) {
                val arg =
                    when (param) {
                        (currentNode.invokes.first() as? MethodDeclaration)?.receiver ->
                            (currentNode as? MemberCallExpression)?.base.unwrapReference()
                        is ParameterDeclaration ->
                            currentNode.arguments[param.argumentIndex].unwrapReference()
                        else -> null
                    }
                if (arg != null) {
                    // Since arg is a pointer (otherwise it couldn't change in the function), we
                    // have to work with the value of the pointer
                    doubleState.getValues(arg).forEach { a ->
                        newValue.forEach { value ->
                            doubleState = doubleState.updateValues(value, a)
                        }
                    }
                }
                println(arg)
            }
        } else if (currentNode.invokes.all { it.hasBody() }) {
            // Process the missing FunctionDeclarations

        } else {
            // We don't have a body or a FunctionSummary
        }

        /*
        For now, we only care about memcpy* and memset, which can influence memoryAddresses or memoryValues
         */
        var src: Expression? = null
        var dst: Expression? = null
        if (
            (currentNode.name.localName in listOf("memcpy_s", "memcpy_verw_s", "memset_s")) &&
                currentNode.arguments.size == 4
        ) {
            dst = currentNode.arguments[0]
            src = currentNode.arguments[2]
        } else if (
            (currentNode.name.localName in listOf("memcpy", "memcpy_verw", "memset")) &&
                currentNode.arguments.size == 3
        ) {
            dst = currentNode.arguments[0]
            src = currentNode.arguments[1]
        }

        /*
        Something is getting copied here, so we need to update the state
         */
        if (src != null && dst != null) {
            // Since memcpy takes pointers as arguments, we need to resolve them here to determine
            // the values we need to read/write
            doubleState.getValues(src).forEach { s ->
                doubleState.getValues(dst).forEach { d ->
                    doubleState = doubleState.updateValues(s, d)
                }
            }
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
                        Pair(PowersetLattice(addresses), PowersetLattice(setOf(currentNode)))
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
         * */
        if (currentNode.lhs.size == 1 && currentNode.rhs.size == 1) {
            doubleState = doubleState.updateValues(currentNode.rhs.first(), currentNode.lhs.first())
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
        currentNode: Declaration,
        doubleState: PointsToPass.PointsToState2
    ): PointsToState2 {
        /* No need to set the address, this already happens in the constructor */
        val addresses = doubleState.getAddresses(currentNode)

        val values =
            (currentNode as? HasInitializer)?.initializer?.let { initializer ->
                doubleState.getValues(initializer)
            } ?: setOf()

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

    protected class PointsToState2(
        generalState:
            LatticeElement<
                Map<
                    Node, LatticeElement<Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>>
                >
            > =
            MapLattice(mapOf()),
        declarationsState:
            LatticeElement<
                Map<
                    Node, LatticeElement<Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>>
                >
            > =
            MapLattice(mapOf())
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
                this.generalState.lub(MapLattice(mapOf(Pair(newNode, newLatticeElement))))
            return PointsToState2(newGeneralState, declarationsState)
        }

        /** Pushes the [newNode] and its [newLatticeElement] to the [declarationsState]. */
        fun pushToDeclarationsState(
            newNode: Node,
            newLatticeElement:
                LatticeElement<Pair<LatticeElement<Set<Node>>, LatticeElement<Set<Node>>>>
        ): PointsToState2 {
            val newDeclarationsState =
                this.declarationsState.lub(MapLattice(mapOf(Pair(newNode, newLatticeElement))))
            return PointsToState2(generalState, newDeclarationsState)
        }

        override fun equals(other: Any?): Boolean {
            if (other !is PointsToState2) return false
            return other.elements.first == this.elements.first &&
                other.elements.second == this.elements.second
        }

        fun getValues(node: Node): Set<Node> {
            return when (node) {
                is MemoryAddress,
                is ParameterMemoryValue ->
                    /* In these cases, we simply have to fetch the current value for the MemoryAddress from the DeclarationState */
                    this.declarationsState.elements[node]?.elements?.second?.elements
                        ?: setOf(UnknownMemoryValue())
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
                            setOf(UnknownMemoryValue())
                        }
                    inputVal.flatMap { this.getValues(it) }.toSet()
                }
                is Declaration -> {
                    /* For Declarations, we have to look up the last value written to it.
                     */
                    if (!node.memoryAddressIsInitialized())
                        node.memoryAddress = MemoryAddress(node.name)
                    this.declarationsState.elements[node.memoryAddress]?.elements?.second?.elements
                        ?: setOf(UnknownMemoryValue())
                }
                is Reference -> {
                    /* For References, we have to look up the last value written to its declaration.
                     */
                    this.getAddresses(node)
                        .flatMap {
                            this.declarationsState.elements[it]?.elements?.second?.elements
                                ?: setOf(UnknownMemoryValue())
                        }
                        .toSet()
                }
                is CastExpression -> {
                    this.getValues(node.expression)
                }
                is UnaryOperator -> this.getValues(node.input)
                is SubscriptExpression ->
                    this.getAddresses(node)
                        .flatMap {
                            this.declarationsState.elements[it]?.elements?.second?.elements
                                ?: setOf(UnknownMemoryValue())
                        }
                        .toSet()
                is Literal<*>,
                is BinaryOperator -> setOf(node)
                // TODO: This may no longer be necessary with function summaries
                is CallExpression -> setOf(UnknownMemoryValue(node.name))
                else -> setOf(UnknownMemoryValue()) /*setOf(node)*/
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
                    setOf(node.memoryAddress)
                }
                is PointerReference -> {
                    setOf()
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
                    node.refersTo.let { refersTo ->
                        /* In some cases, the refersTo might not yet have an initialized MemoryAddress, for example if it's a FunctionDeclaration. So let's to this here */
                        if (!refersTo!!.memoryAddressIsInitialized())
                            refersTo.memoryAddress = MemoryAddress(node.name)

                        setOf(refersTo.memoryAddress)
                    }
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
                else -> setOf()
            }
        }

        /*
         * Look up the `indexString` in the `baseAddress`es and return the fieldAddresses
         * If no MemoryAddress exits at `indexString`, it will be created
         */
        fun getFieldAddresses(
            baseAddresses: List<MemoryAddress>,
            /*indexString: String,*/
            nodeName: Name
        ): Set<Node> {
            val fieldAddresses = mutableSetOf<MemoryAddress>()

            /* Theoretically, the base can have multiple addresses. Additionally, also the fieldDeclaration can have multiple Addresses. To simplify, we flatten the set and collect all possible addresses of the fieldDeclaration in a flat set */
            baseAddresses.forEach { addr ->
                addr.fieldAddresses[nodeName.localName]?.forEach { fieldAddresses.add(it) }
            }
            /* If we do not yet have a MemoryAddress for this FieldDeclaration, we create one */
            if (fieldAddresses.isEmpty()) {
                val newMemoryAddress = MemoryAddress(nodeName)

                fieldAddresses.add(newMemoryAddress)
                baseAddresses.forEach { addr ->
                    addr.fieldAddresses[nodeName.localName] = setOf(newMemoryAddress)
                }
            }
            return fieldAddresses
        }

        /**
         * Update the node `dst` to the values of `src`. Updates the declarationState and the
         * generalState for `dst`
         */
        fun updateValues(src: Node, dst: Node): PointsToState2 {
            val addresses = this.getAddresses(dst)
            val values: Set<Node> = this.getValues(src)
            val newDeclState = this.declarationsState.elements.toMutableMap()
            val newGenState = this.generalState.elements.toMutableMap()
            /* Update the declarationState for the refersTo */
            addresses.forEach { addr ->
                newDeclState[addr] =
                    TupleLattice(Pair(PowersetLattice(addresses), PowersetLattice(values)))
            }
            /* Also update the generalState for dst */
            newGenState[dst] =
                TupleLattice(Pair(PowersetLattice(addresses), PowersetLattice(values)))
            var doubleState = PointsToState2(MapLattice(newGenState), MapLattice(newDeclState))

            /* When we are dealing with SubscriptExpression, we also have to initialise the arrayExpression, since that hasn't been done yet
             */
            if (dst is SubscriptExpression) {
                val AEaddresses = this.getAddresses(dst.arrayExpression)
                val AEvalues = this.getValues(dst.arrayExpression)

                doubleState =
                    doubleState.push(
                        dst.arrayExpression,
                        TupleLattice(Pair(PowersetLattice(AEaddresses), PowersetLattice(AEvalues)))
                    )
            }

            //            var doubleState = PointsToState2(this.generalState,
            // MapLattice(newDeclState))
            /*            doubleState =
            doubleState.push(
                dst,
                TupleLattice(Pair(PowersetLattice(addresses), PowersetLattice(values)))
            )*/
            return doubleState
        }
    }
}

/*
typealias PointsToState = TupleLattice<MapLattice<Node?, TupleLattice<PowersetLattice<Node?>, PowersetLattice<Node?>>>,MapLattice<Node?, TupleLattice<PowersetLattice<Node?>, PowersetLattice<Node?>>>>
val PointsToState.generalState
    get() = this.elements.first
val PointsToState.declarationState
    get() = this.elements.second*/
