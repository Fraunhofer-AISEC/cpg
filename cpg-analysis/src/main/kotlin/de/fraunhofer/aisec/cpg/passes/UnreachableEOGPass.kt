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
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.statements.DoStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.LoopStatement
import de.fraunhofer.aisec.cpg.graph.statements.WhileStatement
import de.fraunhofer.aisec.cpg.helpers.*
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.MapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.Order
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import kotlinx.coroutines.runBlocking

/**
 * A [Pass] which uses a simple logic to determine constant values and mark unreachable code regions
 * by setting the [EvaluationOrder.unreachable] property to true.
 */
@DependsOn(ControlFlowSensitiveDFGPass::class, softDependency = true)
@DependsOn(PointsToPass::class, softDependency = true)
@DependsOn(DFGPass::class, softDependency = true)
open class UnreachableEOGPass(ctx: TranslationContext) : EOGStarterPass(ctx) {

    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(node: Node) {
        val walker = SubgraphWalker.IterativeGraphWalker(strategy = Strategy::AST_FORWARD)
        walker.registerOnNodeVisit(::handle)

        if (node is AstNode) {
            walker.iterate(node)
        }
    }

    /**
     * We perform the actions for each [FunctionDeclaration].
     *
     * @param node every node in the TranslationResult
     */
    protected fun handle(node: Node, parent: Node?) {
        val unreachabilityState = UnreachabilityState(ReachabilityLattice())
        var startState = unreachabilityState.bottom
        for (firstEdge in node.nextEOGEdges) {
            startState = unreachabilityState.push(startState, firstEdge, Reachability.REACHABLE)
        }

        val nextEog = node.nextEOGEdges.toList()
        val finalStateNew = runBlocking {
            unreachabilityState.iterateEOG(nextEog, startState, ::transfer)
        }

        for ((key, value) in finalStateNew) {
            if (value.reachability == Reachability.UNREACHABLE) {
                key.unreachable = true
            }
        }
    }

    /**
     * This method is executed for each EOG edge which is in the worklist. [currentEdge] is the edge
     * to process, [currentState] contains the state which was observed before arriving here.
     *
     * This method modifies the state for the next eog edge as follows:
     * - If the next node in the eog is an [IfStatement], the condition is evaluated and if it is
     *   either always true or false, the else or then branch receives set to
     *   [Reachability.UNREACHABLE].
     * - If the next node in the eog is a [WhileStatement], the condition is evaluated and if it's
     *   always true or false, either the EOG edge to the loop body or out of the loop body is set
     *   to [Reachability.UNREACHABLE].
     * - For all other nodes, we simply propagate the state which led us here.
     *
     * Returns the updated state and true because we always expect an update of the state.
     */
    suspend fun transfer(
        lattice: Lattice<UnreachabilityStateElement>,
        currentEdge: EvaluationOrder,
        currentState: UnreachabilityStateElement,
    ): UnreachabilityStateElement {
        val lattice = lattice as? UnreachabilityState ?: return currentState
        var newState = currentState
        when (val currentNode = currentEdge.end) {
            is IfStatement -> {
                newState = handleIfStatement(lattice, currentEdge, currentNode, newState)
            }

            is LoopStatement -> {
                newState = handleLoopStatement(lattice, currentEdge, currentNode, newState)
            }
            // TODO: Add handling of SwitchStatement once we have a good way to follow the EOG edges
            //  for them (e.g. based on the branching condition or similar).
            else -> {
                // For all other edges, we simply propagate the reachability property of the edge
                // which made us come here.
                currentNode.nextEOGEdges.forEach {
                    newState =
                        lattice.push(
                            newState,
                            it,
                            newState[currentEdge]?.reachability ?: Reachability.BOTTOM,
                        )
                }
            }
        }

        return newState
    }

    /**
     * Evaluates the condition of the [IfStatement] [n] (which is the end node of [enteringEdge]).
     * If it is always true, then the else-branch receives the [state] [Reachability.UNREACHABLE].
     * If the condition is always false, then the then-branch receives the [state]
     * [Reachability.UNREACHABLE]. All other cases simply copy the state which led us here.
     */
    private fun handleIfStatement(
        lattice: UnreachabilityState,
        enteringEdge: EvaluationOrder,
        n: IfStatement,
        state: UnreachabilityStateElement,
    ): UnreachabilityStateElement {
        val evalResult = n.language.evaluator.evaluate(n.condition)

        val (unreachableEdges, remainingEdges) =
            if (evalResult == true) {
                // If the condition is always true, the "false" branch is always unreachable
                Pair(
                    n.nextEOGEdges.filter { e -> e.branch == false },
                    n.nextEOGEdges.filter { e -> e.branch != false },
                )
            } else if (evalResult == false) {
                // If the condition is always false, the "true" branch is always unreachable
                Pair(
                    n.nextEOGEdges.filter { e -> e.branch == true },
                    n.nextEOGEdges.filter { e -> e.branch != true },
                )
            } else {
                Pair(listOf(), n.nextEOGEdges)
            }

        return propagateState(
            unreachableEdges = unreachableEdges,
            remainingEdges = remainingEdges,
            enteringEdge = enteringEdge,
            state = state,
            lattice = lattice,
        )
    }

    /**
     * Evaluates the condition of the [LoopStatement] [n] (which is the end node of [enteringEdge]).
     * If it is always false, then the edge to the code inside the loop receives the [state]
     * [Reachability.UNREACHABLE]. If the condition is always true, then the edge after the loop
     * body receives the [state] [Reachability.UNREACHABLE]. All other cases simply copy the state
     * which led us here.
     */
    @Suppress("KotlinConstantConditions")
    private fun handleLoopStatement(
        lattice: UnreachabilityState,
        enteringEdge: EvaluationOrder,
        n: LoopStatement,
        state: UnreachabilityStateElement,
    ): UnreachabilityStateElement {
        val condition =
            when (n) {
                is WhileStatement -> n.condition
                is DoStatement -> n.condition
                is ForStatement -> n.condition
                else -> return state
            }
        val evalResult = n.language.evaluator.evaluate(condition)

        val (unreachableEdges, remainingEdges) =
            if (evalResult is Boolean && evalResult == true) {
                Pair(
                    n.nextEOGEdges.filter { e -> e.branch == false },
                    n.nextEOGEdges.filter { e -> e.branch != false },
                )
            } else if (evalResult is Boolean && evalResult == false) {
                Pair(
                    n.nextEOGEdges.filter { e -> e.branch == true },
                    n.nextEOGEdges.filter { e -> e.branch != true },
                )
            } else {
                Pair(listOf(), n.nextEOGEdges)
            }
        return propagateState(
            unreachableEdges = unreachableEdges,
            remainingEdges = remainingEdges,
            enteringEdge = enteringEdge,
            state = state,
            lattice = lattice,
        )
    }

    private fun propagateState(
        unreachableEdges: List<EvaluationOrder>,
        remainingEdges: List<EvaluationOrder>,
        enteringEdge: EvaluationOrder,
        state: UnreachabilityStateElement,
        lattice: UnreachabilityState,
    ): UnreachabilityStateElement {
        var newState = state
        // These edges are definitely unreachable
        unreachableEdges.forEach { newState = lattice.push(newState, it, Reachability.UNREACHABLE) }

        // For all other edges, we simply propagate the reachability property of the edge which
        // made us come here.
        remainingEdges.forEach {
            newState =
                lattice.push(
                    newState,
                    it,
                    newState[enteringEdge]?.reachability ?: Reachability.BOTTOM,
                )
        }
        return newState
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

class ReachabilityLattice() : Lattice<ReachabilityLattice.Element> {
    class Element(var reachability: Reachability) : Lattice.Element {
        override fun equals(other: Any?): Boolean {
            return other is Element && this@Element.compare(other) == Order.EQUAL
        }

        override fun compare(other: Lattice.Element): Order {
            return when {
                other !is Element ->
                    throw IllegalArgumentException(
                        "$other should be of type ReachabilityLattice2.Element but is ${other.javaClass}"
                    )
                this.reachability == other.reachability -> Order.EQUAL
                this.reachability < other.reachability -> Order.LESSER
                this.reachability > other.reachability -> Order.GREATER
                else -> Order.UNEQUAL
            }
        }

        override fun duplicate(): Element {
            return Element(this.reachability)
        }

        override fun hashCode(): Int {
            return reachability.hashCode()
        }
    }

    override var elements =
        setOf(
            Element(Reachability.BOTTOM),
            Element(Reachability.UNREACHABLE),
            Element(Reachability.REACHABLE),
        )

    override val bottom: Element
        get() = Element(Reachability.BOTTOM)

    override suspend fun lub(
        one: Element,
        two: Element,
        allowModify: Boolean,
        widen: Boolean,
        concurrencyCounter: Int,
    ): Element {
        return if (allowModify) {
            val ret = compare(one, two)
            when (ret) {
                Order.EQUAL -> one
                Order.GREATER -> one
                Order.LESSER -> {
                    one.reachability = two.reachability
                    one
                }
                Order.UNEQUAL -> {
                    one.reachability = Reachability.REACHABLE
                    one
                } // Top of the lattice
            }
        } else Element(maxOf(one.reachability, two.reachability))
    }

    override suspend fun glb(one: Element, two: Element): Element {
        return Element(minOf(one.reachability, two.reachability))
    }

    override fun compare(one: Element, two: Element): Order {
        return one.compare(two)
    }

    override fun duplicate(one: Element): Element {
        return one.duplicate()
    }
}

typealias UnreachabilityStateElement =
    MapLattice.Element<EvaluationOrder, ReachabilityLattice.Element>

typealias UnreachabilityState = MapLattice<EvaluationOrder, ReachabilityLattice.Element>

fun UnreachabilityState.push(
    currentState: UnreachabilityStateElement,
    newEdge: EvaluationOrder,
    newReachability: Reachability,
): UnreachabilityStateElement {
    return runBlocking {
        this@push.lub(
            currentState,
            UnreachabilityStateElement(newEdge to ReachabilityLattice.Element(newReachability)),
            true,
        )
    }
}
