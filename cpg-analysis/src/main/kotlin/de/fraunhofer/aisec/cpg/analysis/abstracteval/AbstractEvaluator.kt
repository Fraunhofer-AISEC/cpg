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
import de.fraunhofer.aisec.cpg.graph.BranchingNode
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
    // A barrier that is used to block the evaluation from progressing until it is cleared
    var pathBarrier: Node? = null

    fun evaluate(node: Node): LatticeInterval {
        goalNode = node
        targetName = node.name.toString()
        targetType = getType(node)
        val initializer = getInitializerOf(node, targetType)!!
        val initialRange = getInitialRange(initializer, targetType)

        // evaluate effect of each operation on the list until we reach "node"
        val startState = IntervalState(null)
        startState.push(initializer, IntervalLattice(initialRange))
        val finalState = iterateEOG(initializer, startState, ::handleNode)
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
        // Do not add perform any operation or add new nodes if we reached a barrier
        if (currentNode == pathBarrier) {
            return state
        }
        // TODO: handle the different cases
        return when (currentNode) {
            is ForStatement,
            is WhileStatement,
            is ForEachStatement,
            is DoStatement -> handleLoop(currentNode, state, worklist)
            is BranchingNode -> handleBranch(currentNode, state, worklist)
            else -> state.applyEffect(currentNode).first
        }
    }

    private fun getInitializerOf(node: Node, type: KClass<out Value>): Node? {
        return type.createInstance().getInitializer(node)
    }

    private fun getInitialRange(initializer: Node, type: KClass<out Value>): LatticeInterval {
        return type.createInstance().getInitialRange(initializer)
    }

    private fun State<Node, LatticeInterval>.applyEffect(
        node: Node,
    ): Pair<State<Node, LatticeInterval>, Boolean> {
        // TODO: do we really need the knowledge if it had an effect from this method?
        val (newInterval, hadEffect) =
            targetType.createInstance().applyEffect(this[node]!!.elements, node, targetName)
        this.push(node, IntervalLattice(newInterval))
        // Push all the next EOG nodes to the state with BOTTOM (unknown) value
        // Only do this if we have not reached the goal node
        if (node != goalNode) {
            node.nextEOG.forEach { this.push(it, IntervalLattice(LatticeInterval.BOTTOM)) }
        }
        return this to hadEffect
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

    /**
     * Handles the analysis of a Looping statement. It does so by iterating over the loop twice,
     * widening the arrays the first time and narrowing them the second time. Only the value of the
     * head node ond the goal node (if it is in the loop) are updated. If the goal node is not in
     * the loop, the first node after the loop will be pushed to the worklist to continue.
     *
     * @param node The BranchingNode as head of the loop
     * @param state The current analysis state
     * @param worklist The current worklist used for blocking off the loop from the outer analysis
     * @return The new state after incorporating the loop analysis
     */
    private fun handleLoop(
        node: Node,
        state: State<Node, LatticeInterval>,
        worklist: Worklist<Node, Node, LatticeInterval>
    ): IntervalState {
        // TODO: create a new micro-evaluation of the value! To do this:
        //  1) Determine the EOG that ends the loop
        //  2) Create a new State with the current value as initial value
        //  3) Set the mode of the state to WIDEN
        //  4) Make sure the worklist terminates at the end of the loop
        //  5) When the worklist terminates, set the mode to NARROW and start again
        //  6) After the second termination, copy important result to the outer state

        val afterLoop = node.nextEOG[1]

        // TODO: check condition

        // TODO: apply widening until the current node does not change and stop then
        // Create new state using widening and iterate over it
        val operatingState = IntervalState(IntervalState.Mode.WIDEN)
        operatingState.push(node, state[node])
        node.nextEOG.forEach { operatingState.push(it, IntervalLattice(LatticeInterval.BOTTOM)) }
        // We set the barrier before iterating to prevent the iteration from leaving the loop
        pathBarrier = afterLoop
        iterateEOG(node, operatingState, ::handleNode)
        pathBarrier = null

        // -- At this point the worklist should be exhausted so that iterateEOG returned --

        // TODO: apply narrowing until no more nodes change and stop then
        // Change the state to narrowing and push the loop start to the worklist again
        operatingState.changeMode(IntervalState.Mode.NARROW)
        worklist.push(node, operatingState)
        node.nextEOG.forEach { operatingState.push(it, operatingState[node]) }
        // We set the barrier before iterating to prevent the iteration from leaving the loop
        pathBarrier = afterLoop
        iterateEOG(node, operatingState, ::handleNode)
        pathBarrier = null

        // -- At this point the worklist should be exhausted so that iterateEOG returned --

        // overwrite the entry node and goal node if it exists in the loop
        // else push the end of the loop to the worklist
        state[node] = operatingState[node]!!
        if (goalNode in operatingState.keys) {
            state[goalNode] = operatingState[node]!!
        } else {
            worklist.push(afterLoop, state)
        }

        // -- we can return the now overwritten state --

        return state as IntervalState
    }

    /**
     * Handles the analysis of a Branching statement. It does so by evaluating the final ranges of
     * each branch and taking the join over all of them. If the target node is included in any
     * branch, the evaluation only uses this branch.
     *
     * @param node The BranchingNode as head of the loop
     * @param state The current analysis state
     * @param worklist The current worklist used for blocking off the loop from the outer analysis
     * @return The new state after incorporating the branch analysis
     */
    private fun handleBranch(
        node: Node,
        state: State<Node, LatticeInterval>,
        worklist: Worklist<Node, Node, LatticeInterval>
    ): IntervalState {
        val mergeNode = findMergeNode(node)

        // TODO: optimization -> if goal node is in one of the branches abort immediately and report
        // its value

        // push all branches as new states
        val operatingState = IntervalState(IntervalState.Mode.OVERWRITE)
        node.nextEOG.forEach { operatingState.push(it, IntervalLattice(LatticeInterval.BOTTOM)) }

        // iterate over all branches without passing the merge node
        pathBarrier = mergeNode
        iterateEOG(node, operatingState, ::handleNode)
        pathBarrier = null

        // overwrite the entry node and goal node if it exists in the loop
        // else push the end of the loop to the worklist
        state[node] = operatingState[node]!!
        if (goalNode in operatingState.keys) {
            state[goalNode] = operatingState[node]!!
        } else {
            worklist.push(mergeNode, state)
        }

        return state as IntervalState
    }

    /**
     * Finds the "MergeNode" as the first common node of all branches.
     *
     * @param node The BranchingNode that is the head of the branching statement
     * @return The Node that is the end of the branching statement
     */
    private fun findMergeNode(node: Node): Node {
        if (node !is BranchingNode) {
            return node.nextEOG.first()
        }

        val branchNumber = node.nextEOG.size
        val branches = Array(branchNumber) { Node() }
        val visited = Array(branchNumber) { mutableSetOf<Node>() }
        for (index in 0 until branchNumber) {
            branches[index] = node.nextEOG[index]
        }
        while (true) {
            for (index in 0 until branchNumber) {
                val current = branches[index]
                if (current in visited[index]) {
                    continue
                }
                visited[index].add(current)
                // If all paths contain the current node it merges all branches
                if (visited.all { it.contains(current) }) {
                    return current
                }
                val next = current.nextEOG.firstOrNull() ?: break
                branches[index] = next
            }
        }
    }

    /**
     * Returns 0 if the condition evaluates to True and 1 if it evaluates to false. If the outcome
     * cannot be deduced it returns -1. This method is always best-effort and cannot guarantee that
     * the outcome can be determined, but must not return a wrong result.
     *
     * @param condition The Expression used as branch condition
     * @return 0, 1 or -1 depending on the Boolean evaluation
     */
    private fun evaluateCondition(condition: Expression?): Int {
        // TODO: this method needs to try and evaluate branch conditions to predict the outcome
        //        when (condition) {
        //            is BinaryOperator -> {
        //                when (condition.operatorCode) {
        //                    "<" -> {
        //                        if (condition.lhs is Reference && condition.rhs is Literal<*> &&
        // condition.rhs.type is IntegerType) {
        //                            val leftValue = evaluate(condition.lhs)
        //                            if (
        //                                leftValue is LatticeInterval.Bounded &&
        //                                    leftValue.upper <
        // LatticeInterval.Bound.Value((condition.rhs as Literal<*>).value as Int)
        //                            ) {
        //                                return 0
        //                            }
        //                        }
        //                    }
        //                }
        //            }
        //        }
        return -1
    }
}
