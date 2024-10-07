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
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
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
     * This function delegates to the right handler depending on the next node.
     * This is the handler used in _iterateEOG_ to correctly handle complex statements.
     *
     * @param currentNode The current node
     * @param state The state for the current node
     * @param worklist The whole worklist to manually handle complex scenarios if necessary
     * @return The updated state after handling the current node
     */
    private fun handleNode(currentNode: Node, state: State<Node, LatticeInterval>, worklist: Worklist<Node, Node, LatticeInterval>): State<Node, LatticeInterval> {
        // TODO: handle the different cases
        return when (currentNode) {
            is ForStatement,
            is WhileStatement,
            is ForEachStatement,
            is DoStatement -> handleLoop(currentNode, state)
            is BranchingNode -> handleBranch(currentNode, state)
            else -> state.applyEffect(currentNode).first
        }
        // TODO: when the goal node is reached we must not return any more states in order to terminate!
        return state
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
        val (newInterval, hadEffect) = targetType.createInstance().applyEffect(this[node]!!.elements, node, targetName)
        this.push(node, IntervalLattice(newInterval))
        // Push all the next EOG nodes to the state with BOTTOM (unknown) value
        node.nextEOG.forEach {this.push(it, IntervalLattice(LatticeInterval.BOTTOM))}
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


    private fun handleNext(
        range: LatticeInterval,
        node: Node,
        name: String,
        type: KClass<out Value>,
        goalNode: Node
    ): Pair<LatticeInterval, Node> {

    }

    /**
     * Handles the analysis of a Looping statement. It does so by filtering out uninteresting
     * statements before applying widening and narrowing in each iteration. If the target node is
     * included in the body, the returned node will be the target node.
     *
     * @param range The previous size range
     * @param node The BranchingNode as head of the loop
     * @param name The name of the collection variable
     * @param type The type of the collection
     * @param goalNode The target node for the analysis
     * @return A Pair containing the new size range and the next node for the analysis
     */
    private fun handleLoop(
        range: LatticeInterval,
        node: Node,
        name: String,
        type: KClass<out Value>,
        goalNode: Node
    ): Pair<LatticeInterval, Node> {
        val afterLoop = node.nextEOG[1]
        // First try to determine whether the condition is applicable
        if (node is ForStatement && evaluateCondition(node.condition) == 1) {
            return range to afterLoop
        }
        val body = mutableListOf<Node>()
        var newRange = range
        val firstBodyStatement: Node? =
            when (node) {
                is ForStatement -> {
                    when (node.statement) {
                        // This cast is important! Otherwise, the wrong statements are returned
                        is Block -> (node.statement as Block).statements.firstOrNull()
                        null -> null
                        else -> node.statement
                    }
                }
                is WhileStatement -> {
                    when (node.statement) {
                        is Block -> (node.statement as Block).statements.firstOrNull()
                        null -> null
                        else -> node.statement
                    }
                }
                is ForEachStatement -> {
                    when (node.statement) {
                        is Block -> (node.statement as Block).statements.firstOrNull()
                        null -> null
                        else -> node.statement
                    }
                }
                is DoStatement -> {
                    when (node.statement) {
                        is Block -> (node.statement as Block).statements.firstOrNull()
                        null -> null
                        else -> node.statement
                    }
                }
                else -> throw NotImplementedException()
            }
        var current: Node? = firstBodyStatement
        // Preprocessing: filter for valid nodes
        while (current != null && current != afterLoop && current != node) {
            // Only add the Statement if it affects the range
            if (range.applyEffect(current, name, type).second) {
                body.add(current)
            }
            // get the next node, skipping nested structures
            // we assume that the last nextEOG always points to the node after the branch!
            current = current.nextEOG.last()
        }
        // Stop if the body contains no valid nodes
        if (body.isEmpty()) {
            return range to afterLoop
        }

        // Initialize the intervals for the previous loop iteration
        val prevBodyIntervals = Array<LatticeInterval>(body.size) { range }
        // WIDENING
        outer@ while (true) {
            for (index in body.indices) {
                // First apply the effect of the next node
                val (lRange, _) = handleNext(newRange, body[index], name, type, goalNode)
                newRange = lRange
                // Then widen using the previous iteration
                // Only widen for the first effective node in the loop (loop separator)
                if (index == 0) {
                    newRange = prevBodyIntervals[index].widen(newRange)
                }
                // If nothing changed we can abort
                if (newRange == prevBodyIntervals[index]) {
                    break@outer
                } else {
                    prevBodyIntervals[index] = newRange
                }
            }
        }
        // NARROWING
        outer@ while (true) {
            for (index in body.indices) {
                // First apply the effect of the next node
                val (lRange, _) = handleNext(newRange, body[index], name, type, goalNode)
                newRange = lRange
                // Then narrow using the previous iteration
                newRange = prevBodyIntervals[index].narrow(newRange)
                // If ALL loop ranges are stable we can abort
                if (index == body.size - 1 && newRange == prevBodyIntervals[index]) {
                    break@outer
                } else {
                    prevBodyIntervals[index] = newRange
                }
            }
        }

        // return goalNode as next node if it was in the loop to prevent skipping loop termination
        // condition
        if (body.contains(goalNode)) {
            return newRange to goalNode
        }
        return newRange to afterLoop
    }

    /**
     * Handles the analysis of a Branching statement. It does so by evaluating the final ranges of
     * each branch and taking the join over all of them. If the target node is included in any
     * branch, the evaluation only uses this branch.
     *
     * @param range The previous size range
     * @param node The BranchingNode as head of the branch
     * @param name The name of the collection variable
     * @param type The type of the collection
     * @param goalNode The target node for the analysis
     * @return A Pair containing the new size range and the next node for the analysis
     */
    private fun handleBranch(
        range: LatticeInterval,
        node: Node,
        name: String,
        type: KClass<out Value>,
        goalNode: Node
    ): Pair<LatticeInterval, Node> {
        val mergeNode = findMergeNode(node)
        val branchNumber = node.nextEOG.size
        val finalBranchRanges = Array<LatticeInterval>(branchNumber) { range }
        for (i in 0 until branchNumber) {
            var current = node.nextEOG[i]
            // if we arrive at the mergeNode we are done with this branch
            while (current != mergeNode) {
                // If at any point we find the goal node in a branch, we stop and ignore other all
                // branches
                if (current == goalNode) {
                    return finalBranchRanges[i] to current
                }
                val (nextRange, nextNode) =
                    handleNext(finalBranchRanges[i], current, name, type, goalNode)
                finalBranchRanges[i] = nextRange
                current = nextNode
            }
        }
        // Take the join of all branches since we do not know which was taken
        val finalMergedRange = finalBranchRanges.reduce { acc, r -> acc.join(r) }
        return finalMergedRange to mergeNode
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
//                        if (condition.lhs is Reference && condition.rhs is Literal<*> && condition.rhs.type is IntegerType) {
//                            val leftValue = evaluate(condition.lhs)
//                            if (
//                                leftValue is LatticeInterval.Bounded &&
//                                    leftValue.upper < LatticeInterval.Bound.Value((condition.rhs as Literal<*>).value as Int)
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
