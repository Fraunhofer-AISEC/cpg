/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.analysis.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.WhileStatement
import de.fraunhofer.aisec.cpg.helpers.*
import de.fraunhofer.aisec.cpg.passes.order.DependsOn

/**
 * A [Pass] which uses a simple logic to determine constant values and mark unreachable code regions
 * by setting the [Properties.UNREACHABLE] property of an eog-edge to true.
 */
@DependsOn(ControlFlowSensitiveDFGPass::class)
class UnreachableEOGPass : Pass() {
    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(translationResult: TranslationResult) {
        val walker = SubgraphWalker.IterativeGraphWalker()
        walker.registerOnNodeVisit(::handle)
        for (tu in translationResult.translationUnits) {
            walker.iterate(tu)
        }
    }

    /**
     * We perform the actions for each [FunctionDeclaration].
     *
     * @param node every node in the TranslationResult
     */
    protected fun handle(node: Node) {
        if (node is FunctionDeclaration) {
            val startState = UnreachabilityState()
            for (firstEdge in node.nextEOGEdges) {
                startState.push(firstEdge, ReachabilityLattice(Reachability.REACHABLE))
            }
            val finalState = EOGWorklist().iterateEOG(node.nextEOGEdges, startState, ::transfer)

            for ((key, value) in finalState) {
                if (value.elements == Reachability.UNREACHABLE) {
                    key.addProperty(Properties.UNREACHABLE, true)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun transfer(
            currentEdge: PropertyEdge<Node>,
            currentState: State<PropertyEdge<Node>, Reachability>,
            currentWorklist: Worklist<PropertyEdge<Node>, Reachability>
        ): Pair<State<PropertyEdge<Node>, Reachability>, Boolean> {
            val currentNode = currentEdge.end
            if (currentNode is IfStatement) {
                handleIfStatement(currentEdge, currentNode, currentState)
            } else if (currentNode is WhileStatement) {
                handleWhileStatement(currentEdge, currentNode, currentState)
            } else {
                // For all other edges, we simply propagate the reachability property of the edge
                // which made us come here.
                currentNode.nextEOGEdges.forEach {
                    currentState.push(it, currentState[currentEdge])
                }
            }

            return Pair(currentState, true)
        }

        private fun handleIfStatement(
            enteringEdge: PropertyEdge<Node>,
            n: IfStatement,
            state: State<PropertyEdge<Node>, Reachability>
        ) {
            val evalResult = ValueEvaluator().evaluate(n.condition)

            val (unreachableEdge, remainingEdges) =
                if (evalResult is Boolean && evalResult == true) {
                    Pair(
                        n.nextEOGEdges.firstOrNull { e -> e.getProperty(Properties.INDEX) == 1 },
                        n.nextEOGEdges.filter { e -> e.getProperty(Properties.INDEX) != 1 }
                    )
                } else if (evalResult is Boolean && evalResult == false) {
                    Pair(
                        n.nextEOGEdges.firstOrNull { e -> e.getProperty(Properties.INDEX) == 0 },
                        n.nextEOGEdges.filter { e -> e.getProperty(Properties.INDEX) != 0 }
                    )
                } else {
                    Pair(null, n.nextEOGEdges)
                }

            if (unreachableEdge != null) {
                // This edge is definitely unreachable
                state.push(unreachableEdge, ReachabilityLattice(Reachability.UNREACHABLE))
            }

            // For all other edges, we simply propagate the reachability property of the edge which
            // made us come here.
            remainingEdges.forEach { state.push(it, state[enteringEdge]) }
        }

        private fun handleWhileStatement(
            enteringEdge: PropertyEdge<Node>,
            n: WhileStatement,
            state: State<PropertyEdge<Node>, Reachability>
        ) {
            /*
             * Note: It does not understand that code like
             * x = true; while(x) {...; x = false;}
             * makes the loop execute at least once.
             * Apparently, the CPG does not offer the required functionality to
             * differentiate between the first and subsequent evaluations of the
             * condition.
             */
            val evalResult = ValueEvaluator().evaluate(n.condition)

            val (unreachableEdge, remainingEdges) =
                if (evalResult is Boolean && evalResult == true) {
                    Pair(
                        n.nextEOGEdges.firstOrNull { e -> e.getProperty(Properties.INDEX) == 1 },
                        n.nextEOGEdges.filter { e -> e.getProperty(Properties.INDEX) != 1 }
                    )
                } else if (evalResult is Boolean && evalResult == false) {
                    Pair(
                        n.nextEOGEdges.firstOrNull { e -> e.getProperty(Properties.INDEX) == 0 },
                        n.nextEOGEdges.filter { e -> e.getProperty(Properties.INDEX) != 0 }
                    )
                } else {
                    Pair(null, n.nextEOGEdges)
                }

            if (unreachableEdge != null) {
                // This edge is definitely unreachable
                state.push(unreachableEdge, ReachabilityLattice(Reachability.UNREACHABLE))
            }

            // For all other edges, we simply propagate the reachability property of the edge which
            // made us come here.
            remainingEdges.forEach { state.push(it, state[enteringEdge]) }
        }
    }
}

/** Implements the [Lattice] over reachability properties: TOP | REACHABLE | UNREACHABLE | BOTTOM */
class ReachabilityLattice(override val elements: Reachability) : Lattice<Reachability>(elements) {
    override fun lub(other: Lattice<Reachability>) =
        ReachabilityLattice(maxOf(this.elements, other.elements))
    override fun duplicate() = ReachabilityLattice(this.elements)
    override fun compareTo(other: Lattice<Reachability>) = this.elements.compareTo(other.elements)
}

enum class Reachability {
    BOTTOM,
    UNREACHABLE,
    REACHABLE,
    TOP
}

/**
 * A state which actually holds a state for all [PropertyEdge]s, one only for declarations and one
 * for ReturnStatements.
 */
class UnreachabilityState : State<PropertyEdge<Node>, Reachability>()
