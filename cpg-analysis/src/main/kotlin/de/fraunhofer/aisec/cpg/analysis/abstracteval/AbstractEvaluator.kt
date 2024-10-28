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

import de.fraunhofer.aisec.cpg.analysis.abstracteval.value.Array
import de.fraunhofer.aisec.cpg.analysis.abstracteval.value.Integer
import de.fraunhofer.aisec.cpg.analysis.abstracteval.value.Value
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.DoStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.WhileStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import org.apache.commons.lang3.NotImplementedException

/**
 * An evaluator performing abstract evaluation for a singular [Value]. It takes a target [Node] and
 * walks back to its Declaration. From there it uses the [Worklist] to traverse the EOG graph until
 * it reaches the node. All statements encountered may influence the result as implemented in the
 * respective [Value] class. The result is a [LatticeInterval] defining both a lower and upper bound
 * for the final value.
 */
class AbstractEvaluator {
    // The node for which we want to get the value
    private lateinit var targetNode: Node
    // The name of the value we are analyzing
    private lateinit var targetName: String
    // The type of the value we are analyzing
    private lateinit var targetType: KClass<out Value>

    /**
     * Takes a node (e.g. Reference) and tries to evaluate its value at this point in the program.
     */
    fun evaluate(node: Node): LatticeInterval {
        return evaluate(
            node.name.localName,
            getInitializerOf(node)!!,
            node,
            getType(node),
            IntervalLattice(LatticeInterval.BOTTOM)
        )
    }

    /**
     * Takes a manual configuration and tries to evaluate the value of the node at the end.
     *
     * @param name The name of the target node
     * @param start The beginning of the analysis, usually the start of the target's life
     * @param end The place at which we want to know the target's value
     * @param type The Type of the target
     * @param interval The starting value of the analysis, optional
     */
    fun evaluate(
        name: String,
        start: Node,
        end: Node,
        type: KClass<out Value>,
        interval: IntervalLattice? = null
    ): LatticeInterval {
        targetNode = end
        targetName = name
        targetType = type

        // evaluate effect of each operation on the list until we reach "node"
        val startState = IntervalState()
        startState.push(start, interval)
        val finalState = iterateEOG(start, startState, ::handleNode, targetNode)
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
        worklist: Worklist<Node, Node, LatticeInterval>
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
        // Only do this if we have not yet reached the goal node
        if (currentNode != targetNode) {
            currentNode.nextEOG.forEach { newState.push(it, newState[currentNode]) }
        }

        return newState
    }

    private fun getInitializerOf(node: Node): Node? {
        return Value.getInitializer(node)
    }

    private fun State<Node, LatticeInterval>.calculateEffect(node: Node): LatticeInterval {
        return targetType.createInstance().applyEffect(this[node]!!.elements, node, targetName)
    }

    /**
     * Tries to determine the type of the target Node by parsing the type name.
     *
     * @param node The target node
     * @return A [Value] class that models the effects on the node type
     */
    private fun getType(node: Node): KClass<out Value> {
        if (node !is Reference) {
            throw NotImplementedException()
        }
        val name = node.type.name.toString()
        return when {
            name.endsWith("[]") -> Array::class
            name == "int" -> Integer::class
            else -> throw NotImplementedException()
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
