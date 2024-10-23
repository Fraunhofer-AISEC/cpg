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
import de.fraunhofer.aisec.cpg.analysis.abstracteval.value.MutableList
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

class AbstractEvaluator {
    // The node for which we want to get the value
    private lateinit var targetNode: Node
    // The name of the value we are analyzing
    private lateinit var targetName: String
    // The type of the value we are analyzing
    private lateinit var targetType: KClass<out Value>

    fun evaluate(node: Node): LatticeInterval {
        return evaluate(
            node.name.localName,
            getInitializerOf(node)!!,
            node,
            getType(node),
            IntervalLattice(LatticeInterval.BOTTOM)
        )
    }

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
        // TODO: null-safety
        return finalState!![targetNode]!!.elements
    }

    /**
     * This function delegates to the right handler depending on the next node. This is the handler
     * used in _iterateEOG_ to correctly handle complex statements.
     *
     * @param currentNode The current node
     * @param state The state for the current node
     * @param worklist The whole worklist to manually handle complex scenarios if necessary
     * @return The updated state after handling the current node
     */
    private fun handleNode(
        currentNode: Node,
        state: State<Node, LatticeInterval>,
        worklist: Worklist<Node, Node, LatticeInterval>
    ): State<Node, LatticeInterval> {
        // If the current node is already done
        // (prevents infinite loop and unnecessary double-checking)
        if (worklist.isDone(currentNode)) {
            // Mark following nodes as DONE if they only have this as previous EOG or all are DONE
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

        // First calculate the effect
        val previousInterval = state[currentNode]?.elements
        val newInterval = state.calculateEffect(currentNode)
        val newState = state.duplicate()

        // If it was already seen exactly once or is known to need widening
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

        // If it is marked as in need of narrowing
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

        // If it was seen for the first time apply the effect and maybe mark it as "NEEDS WIDENING"
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

        // We propagate the current Interval to all successor nodes which are empty
        // If the next EOG already has a value we need to join them
        // This is implemented in IntervalState.push
        // Only do this if we have not reached the goal node
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
     * Tries to determine the Collection type of the target Node by parsing the type name.
     *
     * @param node The target node
     * @return A Kotlin class representing the collection that contains the necessary analysis
     *   functions
     */
    private fun getType(node: Node): KClass<out Value> {
        if (node !is Reference) {
            throw NotImplementedException()
        }
        val name = node.type.name.toString()
        return when {
            // TODO: could be linkedList, arrayList, ...
            name.startsWith("java.util.List") -> MutableList::class
            name.endsWith("[]") -> Array::class
            name == "int" -> Integer::class
            else -> MutableList::class // throw NotImplementedException()
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
