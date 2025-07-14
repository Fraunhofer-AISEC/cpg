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
package de.fraunhofer.aisec.cpg.analysis.abstracteval

import de.fraunhofer.aisec.cpg.analysis.abstracteval.value.*
import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.statements.DoStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.WhileStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * An evaluator performing abstract evaluation for a singular [Value]. It takes a target [Node] and
 * walks back to its Declaration. From there it uses the [Worklist] to traverse the EOG graph until
 * it reaches the node. All statements encountered may influence the result as implemented in the
 * respective [Value] class. The result is a [LatticeInterval] defining both a lower and upper bound
 * for the final value.
 */
class AbstractIntervalEvaluator {
    /** The name of the value we are analyzing */
    private lateinit var targetName: String
    /** The type of the value we are analyzing */
    private lateinit var targetType: KClass<out Value<LatticeInterval>>

    /**
     * Takes a node (e.g. Reference) and tries to evaluate its value at this point in the program.
     */
    fun evaluate(node: Node): LatticeInterval {
        return evaluate(
            getInitializerOf(node)!!,
            node,
            getType(node),
            IntervalLattice(LatticeInterval.BOTTOM),
        )
    }

    /**
     * Takes a manual configuration and tries to evaluate the value of the node at the end.
     *
     * @param start The beginning of the analysis, usually the start of the target's life
     * @param targetNode The place at which we want to know the target's value
     * @param type The Type of the target
     * @param interval The starting value of the analysis, optional
     */
    fun evaluate(
        start: Node,
        targetNode: Node,
        type: KClass<out Value<LatticeInterval>>,
        interval: IntervalLattice? = null,
    ): LatticeInterval {
        targetType = type

        // evaluate effect of each operation on the list until we reach "node"
        val startState = IntervalState()
        startState.push(start, interval)
        val finalState = iterateEOG(start, startState, ::handleNode)
        return finalState?.get(targetNode)?.elements ?: LatticeInterval.BOTTOM
    }

    /**
     * This function changes the state depending on the current node. This is the handler used in
     * _iterateEOG_ to correctly handle complex statements.
     *
     * @param currentNode The current node
     * @param state The state for the current node
     * @param worklist The whole worklist to manually handle complex scenarios
     * @return The updated state after handling the current node
     */
    private fun handleNode(
        currentNode: Node,
        state: State<Node, LatticeInterval>,
        worklist: Worklist<Node, Node, LatticeInterval>,
    ): State<Node, LatticeInterval> {
        // Check if the current node is already DONE
        // (prevents infinite loop and unnecessary double-checking)
        if (worklist.isDone(currentNode)) {
            // Mark following nodes as DONE if they only have this as previousEOG or all are DONE
            // In other cases, converging branches may still change the node
            currentNode.nextEOG.forEach { next ->
                if (
                    next.prevEOG.singleOrNull() == currentNode ||
                        next.prevEOG.all { worklist.isDone(it) }
                ) {
                    worklist.evaluationStateMap[next] = Worklist.EvaluationState.DONE
                }
            }
            return state
        }

        // Calculate the effect of the currentNode
        val previousInterval = state[currentNode]?.elements
        val newInterval = state.calculateEffect(currentNode)
        val newState = state.duplicate()

        // Check if it is marked as in need of widening
        if (worklist.needsWidening(currentNode)) {
            // Widen the interval
            val widenedInterval = previousInterval!!.widen(newInterval)
            // Check if the widening caused a change
            if (widenedInterval != previousInterval) {
                // YES: mark next nodes as needs widening, add them to worklist
                // Overwrite current interval, mark this node as needs narrowing
                newState[currentNode] = IntervalLattice(widenedInterval)
                currentNode.nextEOG.forEach {
                    if (!isLoopEnd(it)) {
                        worklist.evaluationStateMap[it] = Worklist.EvaluationState.WIDENING
                    }
                }
                worklist.evaluationStateMap[currentNode] = Worklist.EvaluationState.NARROWING
            } else {
                // NO: mark the current node as DONE
                // We never mark loop heads as DONE as they prevent loop entering otherwise
                if (!isLoopHead(currentNode)) {
                    worklist.evaluationStateMap[currentNode] = Worklist.EvaluationState.DONE
                }
            }
        }

        // Otherwise, check if it is marked as in need of narrowing
        else if (worklist.needsNarrowing(currentNode)) {
            // Narrow the interval
            val narrowedInterval = previousInterval!!.narrow(newInterval)
            // Check if the narrowing caused a change
            if (narrowedInterval != previousInterval) {
                // YES: overwrite and keep this node marked
                // Mark next nodes as need narrowing and add to worklist
                newState[currentNode] = IntervalLattice(narrowedInterval)
                currentNode.nextEOG.forEach {
                    worklist.evaluationStateMap[it] = Worklist.EvaluationState.NARROWING
                }
            } else {
                // NO: mark the node as DONE
                // We never mark loop heads as DONE as they prevent loop entering otherwise
                if (!isLoopHead(currentNode)) {
                    worklist.evaluationStateMap[currentNode] = Worklist.EvaluationState.DONE
                }
            }
        }

        // Otherwise, if it was seen for the first time directly apply the effect.
        // If it is within a loop mark it as "NEEDS WIDENING".
        // We cannot use the "already_seen" field as it is set before this handler is called
        else {
            newState[currentNode] = IntervalLattice(newInterval)
            // We mark the node as needs widening if it is either a loop head or any previous node
            // is marked, but not if the current node ends a loop
            if (
                isLoopHead(currentNode) ||
                    (currentNode.prevEOG.any { worklist.needsWidening(it) } &&
                        !isLoopEnd(currentNode))
            ) {
                worklist.evaluationStateMap[currentNode] = Worklist.EvaluationState.WIDENING
            }
        }

        // Finally, we propagate the current Interval to all successor nodes which are empty.
        // If the next EOG already has a value we need to join them.
        // This is implemented in IntervalState.push.
        currentNode.nextEOG.forEach { newState.push(it, newState[currentNode]) }

        return newState
    }

    private fun getInitializerOf(node: Node): Node? {
        return Value.getInitializer(node)
    }

    private fun State<Node, LatticeInterval>.calculateEffect(node: Node): LatticeInterval {
        val currentInterval =
            if (
                node is Reference &&
                    (node.access == AccessValues.READ || node.access == AccessValues.READWRITE)
            ) {
                val prevDFGs =
                    node.prevDFGEdges
                        .filter { it.granularity is FullDataflowGranularity }
                        .map { it.start }
                prevDFGs.fold(LatticeInterval.BOTTOM) { acc: LatticeInterval, prevNode ->
                    acc.meet(this[prevNode]?.elements ?: LatticeInterval.BOTTOM)
                }
            } else {
                this[node]?.elements
                    ?: LatticeInterval.Bounded(
                        LatticeInterval.Bound.NEGATIVE_INFINITE,
                        LatticeInterval.Bound.INFINITE,
                    )
            }
        return targetType
            .createInstance()
            .applyEffect(
                currentInterval,
                TupleState<Node>(
                    DeclarationState(NewIntervalLattice()),
                    NewIntervalState(NewIntervalLattice()),
                ),
                TupleStateElement(DeclarationStateElement(), NewIntervalStateElement()),
                node,
                targetName,
            )
    }

    /**
     * Tries to determine the type of the target Node by parsing the type name.
     *
     * @param node The target node
     * @return A [Value] class that models the effects on the node type
     */
    private fun getType(node: Node): KClass<out Value<LatticeInterval>> {
        if (node !is Reference) {
            TODO()
        }
        val name = node.type.name.toString()
        return when {
            name.endsWith("[]") -> ArrayValue::class
            name == "int" -> IntegerValue::class
            else -> TODO()
        }
    }

    /** This method checks for known loop heads such as For, While or Do. */
    private fun isLoopHead(node: Node): Boolean {
        return when (node) {
            is ForStatement,
            is WhileStatement,
            is ForEachStatement,
            is DoStatement -> true
            else -> false
        }
    }

    /**
     * This method checks if the current node marks the end of a loop. It assumes that nextEOG[1] of
     * the loop head always points to the loop end.
     */
    private fun isLoopEnd(node: Node): Boolean {
        val head = node.prevEOG.firstOrNull { isLoopHead(it) } ?: return false
        return head.nextEOG[1] == node
    }
}
