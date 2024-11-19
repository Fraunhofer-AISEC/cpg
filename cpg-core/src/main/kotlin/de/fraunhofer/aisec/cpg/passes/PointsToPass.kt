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
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.cyclomaticComplexity
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.functional.*
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass.Configuration
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore

@DependsOn(SymbolResolver::class)
@DependsOn(EvaluationOrderGraphPass::class)
@ExecuteBefore(ControlFlowSensitiveDFGPass::class)
class PointsToPass(ctx: TranslationContext) : EOGStarterPass(ctx) {
    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(node: Node) {
        // For now, we only execute this for function declarations, we will support all EOG starters
        // in the future.
        if (node !is FunctionDeclaration) {
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
        node.parameters.forEach {
            val paramState: LatticeElement<Pair<PowersetLatticeT<Node>, PowersetLatticeT<Node>>> =
                TupleLattice(
                    Pair(PowersetLattice(identitySetOf()), PowersetLattice(identitySetOf()))
                )
            // startState.push(it, paramState)
            startState = startState.pushToDeclarationsState(it, paramState)
        }
        val finalState = iterateEOGClean(node.nextEOGEdges, startState, ::transfer)
        if (finalState !is PointsToState2) return

        for ((key, value) in finalState.generalState.elements) {
            when (key) {
                is Reference -> {
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
                is Reference -> handleReference(currentNode, doubleState)
                is AssignExpression -> handleAssignExpression(currentNode, doubleState)
                is UnaryOperator -> handleUnaryOperator(currentNode, doubleState)
                else -> doubleState
            }

        return doubleState
    }

    private fun handleUnaryOperator(
        currentNode: UnaryOperator,
        doubleState: PointsToPass.PointsToState2
    ): PointsToPass.PointsToState2 {
        /* For UnaryOperators, we have to update the value if it's a ++ or -- operator
         */
        // TODO: Check out cases where the input is no Reference
        if (currentNode.input is Reference) {
            val address = doubleState.getAddress(currentNode)
            val value = doubleState.getValue(currentNode)
            val newValue =
                if (currentNode.operatorCode == "++" || currentNode.operatorCode == "--") {
                    val binOp =
                        newBinaryOperator(currentNode.operatorCode.toString().substring(0, 1))
                    val expList = newExpressionList()
                    value.filterIsInstance<Statement>().forEach { stmt ->
                        expList.expressions.add(stmt)
                    }
                    binOp.lhs = expList
                    binOp.rhs = newLiteral(1, currentNode.primitiveType("int"))
                    setOf(binOp)
                } else value
            val newDeclState = doubleState.declarationsState.elements.toMutableMap()
            /* Update the declarationState for the refersTo */
            newDeclState.replace(
                doubleState.getAddress(currentNode.input).first(),
                TupleLattice(Pair(PowersetLattice(address), PowersetLattice(newValue)))
            )
            // TODO: Should we already update the input's value in the generalState, or is it
            // enough at the next use?
            val newDoubleState = PointsToState2(doubleState.generalState, MapLattice(newDeclState))
            return newDoubleState
        }
        return doubleState
    }

    private fun handleAssignExpression(
        currentNode: AssignExpression,
        doubleState: PointsToPass.PointsToState2
    ): PointsToPass.PointsToState2 {
        /* For AssignExpressions, we update the value of the rhs with the lhs
         * In C(++), both the lhs and the rhs should only have one element
         * */
        if (currentNode.lhs.size == 1 && currentNode.rhs.size == 1) {
            val ref = currentNode.lhs.first() as? Reference
            if (ref?.refersTo != null) {
                val address = doubleState.getAddress(ref.refersTo!!)
                // TODO: resolve rhs
                val value: Set<Node> = doubleState.getValue(currentNode.rhs.first())
                val newDeclState = doubleState.declarationsState.elements.toMutableMap()
                val newGeneralState = doubleState.generalState.elements.toMutableMap()
                /* Update the declarationState for the refersTo */
                newDeclState.replace(
                    doubleState.getAddress(ref.refersTo!!).first(),
                    TupleLattice(Pair(PowersetLattice(address), PowersetLattice(value)))
                )
                /* Also update the generalState for the ref */
                newGeneralState.replace(
                    ref,
                    TupleLattice(Pair(PowersetLattice(address), PowersetLattice(value)))
                )
                val newDoubleState =
                    PointsToState2(MapLattice(newGeneralState), MapLattice(newDeclState))
                return newDoubleState
            }
        }

        return doubleState
    }

    private fun handleReference(
        currentNode: Reference,
        doubleState: PointsToPass.PointsToState2
    ): PointsToPass.PointsToState2 {
        /* The MemoryAddress of a Reference is the same is from its Declaration AKA refersTo
         * Except for PointerReferences, they don't really have MemoryAddress, so we leave the set empty */
        val address = doubleState.getAddress(currentNode)
        val value = doubleState.getValue(currentNode)

        val doubleState =
            doubleState.push(
                currentNode,
                TupleLattice(Pair(PowersetLattice(address), PowersetLattice(value)))
            )

        return doubleState
    }

    private fun handleDeclaration(
        currentNode: Declaration,
        doubleState: PointsToPass.PointsToState2
    ): PointsToState2 {
        /* No need to set the address, this already happens in the constructor */
        val address = doubleState.getAddress(currentNode)

        val value =
            (currentNode as? HasInitializer)?.initializer?.let { initializer ->
                doubleState.getValue(initializer)
            } ?: setOf()

        var doubleState =
            doubleState.push(
                currentNode,
                TupleLattice(Pair(PowersetLattice(address), PowersetLattice(value)))
            )
        doubleState =
            /* In the DeclarationsState, we save the address so which we wrote the value for easier work with pointers
             * TODO: How can we do this when we have multiple addresses?
             * */

            doubleState.pushToDeclarationsState(
                address.first(),
                TupleLattice(Pair(PowersetLattice(address), PowersetLattice(value)))
            )

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

        fun getValue(node: Node): Set<Node> {
            return when (node) {
                is MemoryAddress ->
                    /* In this case, we simply have to fetch the current value for the MemoryAddress from the DeclarationState */
                    this.declarationsState.elements[node]?.elements?.second?.elements ?: setOf()
                is PointerReference -> {
                    /* For PointerReferences, the value is the address of the input
                     * For example, the value of `&i` is the address of `i`
                     * */
                    this.getAddress(node.input)
                }
                is PointerDereference -> {
                    /* To find the value for PointerDereferences, we first check what's the current value of the input, which is probably a MemoryAddress
                     * Then we look up the current value at this MemoryAddress
                     * TODO: We assume that there the value of the input is a single MemoryAddress
                     */
                    val inputVal =
                        when (node.input) {
                            is Reference ->
                                (node.input as Reference).refersTo.let { this.getValue(it!!) }
                            else -> // TODO: How can we handle other cases?
                            emptySet()
                        }
                    if (inputVal.size == 1) this.getValue(inputVal.first()) else emptySet()
                }
                is Declaration -> {
                    /* For Declarations, we have to look up the last value written to it.
                     */
                    this.declarationsState.elements[node.memoryAddress]?.elements?.second?.elements
                        ?: setOf()
                }
                is Reference -> {
                    /* For References, we have to lookup the last value written to its declaration.
                     */
                    this.getAddress(node)
                        .flatMap {
                            this.declarationsState.elements[it]?.elements?.second?.elements
                                ?: setOf()
                        }
                        .toSet()
                }
                is CastExpression -> {
                    this.getValue(node.expression)
                }
                is UnaryOperator -> this.getValue(node.input)
                else -> setOf(node)
            }
        }

        fun getAddress(node: Node): Set<Node> {
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
                    PointerDerefences have as address the value of their input.
                    For example, the address of `*a` is the value of `a`
                     */
                    this.getValue(node.input)
                }
                is Reference -> {
                    /*
                    For references, the address is the same as for the declaration, AKA the refersTo
                    */
                    node.refersTo.let { refersTo ->
                        /* In some cases, the refersTo might not yet have an initialized memoryaddresse, for example if its a FunctionDeclaration. So let's to this here */
                        if (!refersTo!!.memoryAddressIsInitialized())
                            refersTo.memoryAddress = MemoryAddress(node.name)

                        this.declarationsState.elements[refersTo.memoryAddress]
                            ?.elements
                            ?.first
                            ?.elements ?: setOf()
                    }
                }
                is CastExpression -> {
                    /*
                    For CastExpressions we take the expression as the cast itself does not have any impact on the address
                     */
                    this.getAddress(node.expression)
                }
                else -> setOf()
            }
        }
    }
}

/*
typealias PointsToState = TupleLattice<MapLattice<Node?, TupleLattice<PowersetLattice<Node?>, PowersetLattice<Node?>>>,MapLattice<Node?, TupleLattice<PowersetLattice<Node?>, PowersetLattice<Node?>>>>
val PointsToState.generalState
    get() = this.elements.first
val PointsToState.declarationState
    get() = this.elements.second*/
