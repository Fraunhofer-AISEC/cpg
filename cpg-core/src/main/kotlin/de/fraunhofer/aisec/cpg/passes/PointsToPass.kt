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
import de.fraunhofer.aisec.cpg.graph.HasInitializer
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.cyclomaticComplexity
import de.fraunhofer.aisec.cpg.graph.edges.Edge
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
                    key.memoryAddress.addAll(
                        value.elements.first.elements.filterIsInstance<MemoryAddress>()
                    )
                    key.memoryValue.addAll(value.elements.second.elements)
                }
                is ValueDeclaration -> {
                    key.memoryValue.addAll(value.elements.second.elements)
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
                is Reference -> TODO()
                is AssignExpression -> TODO()
                else -> doubleState
            }

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
            doubleState.pushToDeclarationsState(
                currentNode,
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
                is PointerReference -> {
                    /* For PointerReferences, the value is the address of the input
                     * For example, the value of `&i` is the address of `i`
                     * */
                    this.getAddress(node.input)
                }
                /*
                 PointerDereferences are handeld by the Reference case. The correct address is identified based on the input but getAddress takes care of this
                */
                is Declaration -> {
                    /* For Declarations, we have to lookup the last value written to it.
                     */
                    this.declarationsState.elements[node]?.elements?.second?.elements ?: setOf()
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
                else -> setOf(node)
            }
        }

        fun getAddress(node: Node): Set<Node> {
            return when (node) {
                is Declaration -> {
                    /*
                     * For declarations, we created a new MemoryAddress node, so that's the one we use here
                     */
                    if (!node.memoryAddressIsInitialized()) node.memoryAddress = MemoryAddress()
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
                    node.refersTo?.let {
                        this.declarationsState.elements[it]?.elements?.first?.elements
                    } ?: setOf()
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
