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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.analysis.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.WhileStatement
import de.fraunhofer.aisec.cpg.helpers.*
import de.fraunhofer.aisec.cpg.helpers.functional.LatticeElement
import de.fraunhofer.aisec.cpg.helpers.functional.MapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.iterateEOGClean
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn

/**
 * A [Pass] which uses a simple logic to determine constant values and mark unreachable code regions
 * by setting the [EvaluationOrder.unreachable] property to true.
 */
@DependsOn(ControlFlowSensitiveDFGPass::class)
class UnreachableEOGPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {
    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(tu: TranslationUnitDeclaration) {
        val walker = SubgraphWalker.IterativeGraphWalker()
        walker.registerOnNodeVisit(::handle)
        walker.iterate(tu)
    }

    /**
     * We perform the actions for each [FunctionDeclaration].
     *
     * @param node every node in the TranslationResult
     */
    protected fun handle(node: Node, parent: Node?) {
        if (node is FunctionDeclaration) {
            var startState = UnreachabilityState()
            for (firstEdge in node.nextEOGEdges) {
                startState = startState.push(firstEdge, Reachability.REACHABLE)
            }

            val nextEog = node.nextEOGEdges.toList()
            val finalState = iterateEOGClean(nextEog, startState, ::transfer)

            for ((key, value) in finalState.elements) {
                if (value.elements == Reachability.UNREACHABLE) {
                    key.unreachable = true
                }
            }
        }
    }
}

/**
 * This method is executed for each EOG edge which is in the worklist. [currentEdge] is the edge to
 * process, [currentState] contains the state which was observed before arriving here.
 *
 * This method modifies the state for the next eog edge as follows:
 * - If the next node in the eog is an [IfStatement], the condition is evaluated and if it is either
 *   always true or false, the else or then branch receives set to [Reachability.UNREACHABLE].
 * - If the next node in the eog is a [WhileStatement], the condition is evaluated and if it's
 *   always true or false, either the EOG edge to the loop body or out of the loop body is set to
 *   [Reachability.UNREACHABLE].
 * - For all other nodes, we simply propagate the state which led us here.
 *
 * Returns the updated state and true because we always expect an update of the state.
 */
fun transfer(
    currentEdge: EvaluationOrder,
    currentState: LatticeElement<Map<EvaluationOrder, LatticeElement<Reachability>>>
): LatticeElement<Map<EvaluationOrder, LatticeElement<Reachability>>> {
    var newState = currentState as? UnreachabilityState ?: return currentState
    when (val currentNode = currentEdge.end) {
        is IfStatement -> {
            newState = handleIfStatement(currentEdge, currentNode, newState)
        }
        is WhileStatement -> {
            newState = handleWhileStatement(currentEdge, currentNode, newState)
        }
        else -> {
            // For all other edges, we simply propagate the reachability property of the edge
            // which made us come here.
            currentNode.nextEOGEdges.forEach {
                newState =
                    newState.push(
                        it,
                        newState.elements[currentEdge]?.elements ?: Reachability.BOTTOM
                    )
            }
        }
    }

    return newState
}

/**
 * Evaluates the condition of the [IfStatement] [n] (which is the end node of [enteringEdge]). If it
 * is always true, then the else-branch receives the [state] [Reachability.UNREACHABLE]. If the
 * condition is always false, then the then-branch receives the [state] [Reachability.UNREACHABLE].
 * All other cases simply copy the state which led us here.
 */
private fun handleIfStatement(
    enteringEdge: Edge<Node>,
    n: IfStatement,
    state: UnreachabilityState
): UnreachabilityState {
    var newState = state
    val evalResult = ValueEvaluator().evaluate(n.condition)

    val (unreachableEdges, remainingEdges) =
        if (evalResult == true) {
            // If the condition is always true, the "false" branch is always unreachable
            Pair(
                n.nextEOGEdges.filter { e -> e.branch == false },
                n.nextEOGEdges.filter { e -> e.branch != false }
            )
        } else if (evalResult == false) {
            // If the condition is always false, the "true" branch is always unreachable
            Pair(
                n.nextEOGEdges.filter { e -> e.branch == true },
                n.nextEOGEdges.filter { e -> e.branch != true }
            )
        } else {
            Pair(listOf(), n.nextEOGEdges)
        }

    // These edges are definitely unreachable
    unreachableEdges.forEach { newState = newState.push(it, Reachability.UNREACHABLE) }

    // For all other edges, we simply propagate the reachability property of the edge which
    // made us come here.
    remainingEdges.forEach {
        newState =
            newState.push(it, newState.elements[enteringEdge]?.elements ?: Reachability.BOTTOM)
    }
    return newState
}

/**
 * Evaluates the condition of the [WhileStatement] [n] (which is the end node of [enteringEdge]). If
 * it is always true, then the edge to the code after the loop receives the [state]
 * [Reachability.UNREACHABLE]. If the condition is always false, then the edge to the loop body
 * receives the [state] [Reachability.UNREACHABLE]. All other cases simply copy the state which led
 * us here.
 */
private fun handleWhileStatement(
    enteringEdge: Edge<Node>,
    n: WhileStatement,
    state: UnreachabilityState
): UnreachabilityState {
    var newState = state
    /*
     * Note: It does not understand that code like
     * x = true; while(x) {...; x = false;}
     * makes the loop execute at least once.
     * Apparently, the CPG does not offer the required functionality to
     * differentiate between the first and subsequent evaluations of the
     * condition.
     */
    val evalResult = ValueEvaluator().evaluate(n.condition)

    val (unreachableEdges, remainingEdges) =
        if (evalResult is Boolean && evalResult == true) {
            Pair(
                n.nextEOGEdges.filter { e -> e.index == 1 },
                n.nextEOGEdges.filter { e -> e.index != 1 }
            )
        } else if (evalResult is Boolean && evalResult == false) {
            Pair(
                n.nextEOGEdges.filter { e -> e.index == 0 },
                n.nextEOGEdges.filter { e -> e.index != 0 }
            )
        } else {
            Pair(listOf(), n.nextEOGEdges)
        }

    // These edges are definitely unreachable
    unreachableEdges.forEach { newState = newState.push(it, Reachability.UNREACHABLE) }

    // For all other edges, we simply propagate the reachability property of the edge which
    // made us come here.
    remainingEdges.forEach {
        newState =
            newState.push(it, newState.elements[enteringEdge]?.elements ?: Reachability.BOTTOM)
    }
    return newState
}

/**
 * Implements the [LatticeElement] over reachability properties: REACHABLE | UNREACHABLE | BOTTOM
 */
class ReachabilityLattice(elements: Reachability) : LatticeElement<Reachability>(elements) {
    override fun lub(other: LatticeElement<Reachability>) =
        ReachabilityLattice(maxOf(this.elements, other.elements))

    override fun duplicate() = ReachabilityLattice(this.elements)

    override fun compareTo(other: LatticeElement<Reachability>) =
        this.elements.compareTo(other.elements)

    override fun equals(other: Any?): Boolean {
        return other is ReachabilityLattice && this.elements == other.elements
    }

    override fun hashCode(): Int {
        return super.hashCode() * 31 + elements.hashCode()
    }
}

/**
 * The ordering will be as follows: BOTTOM (no information) < UNREACHABLE < REACHABLE (= Top of the
 * lattice)
 */
enum class Reachability {
    BOTTOM,
    UNREACHABLE,
    REACHABLE,
}

/**
 * A state which actually holds a state for all [Edge]s, one only for declarations and one for
 * ReturnStatements.
 */
class UnreachabilityState(elements: Map<EvaluationOrder, ReachabilityLattice> = mapOf()) :
    MapLattice<EvaluationOrder, Reachability>(elements) {
    override fun lub(
        other: LatticeElement<Map<EvaluationOrder, LatticeElement<Reachability>>>
    ): UnreachabilityState {
        return UnreachabilityState(
            this.elements.entries.fold(
                other.elements.mapValues { ReachabilityLattice(it.value.elements) }
            ) { current, (thisKey, thisValue) ->
                val mutableMap = current.toMutableMap()
                mutableMap.compute(thisKey) { k, v ->
                    ReachabilityLattice(thisValue.elements)
                        .lub(v ?: ReachabilityLattice(Reachability.BOTTOM))
                }
                mutableMap
            }
        )
    }

    fun push(newEdge: EvaluationOrder, newReachability: Reachability): UnreachabilityState {
        return this.lub(MapLattice(mapOf(Pair(newEdge, ReachabilityLattice(newReachability)))))
    }
}
