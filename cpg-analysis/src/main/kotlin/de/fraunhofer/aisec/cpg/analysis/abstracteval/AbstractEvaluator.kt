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
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.State
import de.fraunhofer.aisec.cpg.helpers.Worklist
import de.fraunhofer.aisec.cpg.helpers.iterateEOG
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import org.apache.commons.lang3.NotImplementedException

class AbstractEvaluator {
    // The node for which we want to get the value
    lateinit var goalNode: Node

    // The name of the value we are analyzing
    lateinit var targetName: String

    // The type of the value we are analyzing
    lateinit var targetType: KClass<out Value>

    // The call stack to memorize the starting points of nested analysis
    var callStack = mutableListOf<Node>()

    // A Barrier node that blocks the EOG iteration
    var pathBarrier: Node? = null

    fun evaluate(node: Node): LatticeInterval {
        goalNode = node
        targetName = node.name.toString()
        targetType = getType(node)
        val initializer = getInitializerOf(node, targetType)!!

        // evaluate effect of each operation on the list until we reach "node"
        val startState = IntervalState()
        startState.push(initializer, IntervalLattice(LatticeInterval.BOTTOM))
        // TODO: terminates too early since it already knows the state of the first node
        //  -> mark declarations as node with effect in Integer and start with BOTTOM node!
        val finalState = iterateEOG(initializer, startState, ::handleNode, goalNode)
        // TODO: null-safety
        return finalState!![node]!!.elements
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
        // TODO: we must not override the current state before they are checked by the worklist!
        //  otherwise it will seem as if nothing changed
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

        // If it was already seen exactly once or is known to need widening
        if (worklist.needsWidening(currentNode)) {
            // Widen the interval
            val widenedInterval = previousInterval!!.widen(newInterval)
            // Check if the widening caused a change
            if (widenedInterval != previousInterval) {
                // YES: mark next nodes as needs widening, add them to worklist
                // Overwrite current interval, mark this node as needs narrowing
                state[currentNode] = IntervalLattice(widenedInterval)
                currentNode.nextEOG.forEach {
                    worklist.evaluationStateMap[it] = Worklist.EvaluationState.WIDENING
                    worklist.push(it, state)
                }
                worklist.evaluationStateMap[currentNode] = Worklist.EvaluationState.NARROWING
            } else {
                // NO: mark the current node as DONE
                worklist.evaluationStateMap[currentNode] = Worklist.EvaluationState.DONE
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
                state[currentNode] = IntervalLattice(narrowedInterval)
                currentNode.nextEOG.forEach {
                    worklist.evaluationStateMap[it] = Worklist.EvaluationState.NARROWING
                    worklist.push(it, state)
                }
            } else {
                // NO: mark the node as DONE
                worklist.evaluationStateMap[currentNode] = Worklist.EvaluationState.DONE
            }
        }

        // If it was seen for the first time apply the effect and mark it as "NEEDS WIDENING"
        // We cannot use the "already_seen" field as it is set before this handler is called
        else {
            state[currentNode] = IntervalLattice(newInterval)
            worklist.evaluationStateMap[currentNode] = Worklist.EvaluationState.WIDENING
        }

        // If the current node is not DONE we need to push it to the worklist again
        if (!worklist.isDone(currentNode)) {
            worklist.push(currentNode, state)
        }

        // We propagate the current Interval to all successors which are empty
        // Push all the next EOG nodes to the state with BOTTOM (unknown) value
        // Only do this if we have not reached the goal node
        if (currentNode != goalNode) {
            currentNode.nextEOG.forEach {
                if (state[it]?.elements == null) {
                    state.push(it, state[currentNode])
                }
            }
        }

        return state
    }

    private fun getInitializerOf(node: Node, type: KClass<out Value>): Node? {
        return type.createInstance().getInitializer(node)
    }

    private fun getInitialRange(initializer: Node, type: KClass<out Value>): LatticeInterval {
        return type.createInstance().getInitialRange(initializer)
    }

    private fun State<Node, LatticeInterval>.calculateEffect(
        node: Node,
    ): LatticeInterval {
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
}
