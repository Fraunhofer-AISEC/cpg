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
package de.fraunhofer.aisec.cpg.analysis.collectioneval

import de.fraunhofer.aisec.cpg.analysis.collectioneval.collection.Array
import de.fraunhofer.aisec.cpg.analysis.collectioneval.collection.Collection
import de.fraunhofer.aisec.cpg.analysis.collectioneval.collection.MutableList
import de.fraunhofer.aisec.cpg.graph.BranchingNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import org.apache.commons.lang3.NotImplementedException

// We assume that we only work with lists in this operator
class CollectionSizeEvaluator {
    fun evaluate(node: Node): LatticeInterval {
        val name = node.name.toString()
        val type = getType(node)
        val initializer = getInitializerOf(node, type)!!
        var range = getInitialRange(initializer, type)
        // evaluate effect of each operation on the list until we reach "node"
        var current = initializer
        // TODO: do preprocessing: remove all node types that we do not need
        do {
            val (newRange, newCurrent) = handleNext(range, current, name, type, node)
            range = newRange
            current = newCurrent
        } while (current != node)

        return range
    }

    private fun getInitializerOf(node: Node, type: KClass<out Collection>): Node? {
        return type.createInstance().getInitializer(node)
    }

    private fun getInitialRange(initializer: Node, type: KClass<out Collection>): LatticeInterval {
        return type.createInstance().getInitialRange(initializer)
    }

    private fun LatticeInterval.applyEffect(
        node: Node,
        name: String,
        type: KClass<out Collection>
    ): Pair<LatticeInterval, Boolean> {
        return type.createInstance().applyEffect(this, node, name)
    }

    private fun getType(node: Node): KClass<out Collection> {
        if (node !is Reference) {
            throw NotImplementedException()
        }
        val name = node.type.name.toString()
        return when {
            // TODO: could be linkedList, arrayList, ...
            name.startsWith("java.util.List") -> MutableList::class
            name.endsWith("[]") -> Array::class
            else -> MutableList::class // throw NotImplementedException()
        }
    }

    private fun handleNext(
        range: LatticeInterval,
        node: Node,
        name: String,
        type: KClass<out Collection>,
        goalNode: Node
    ): Pair<LatticeInterval, Node> {
        return when (node) {
            is ForStatement,
            is WhileStatement,
            is ForEachStatement,
            is DoStatement -> handleLoop(range, node, name, type, goalNode)
            is BranchingNode -> handleBranch(range, node, name, type, goalNode)
            else -> range.applyEffect(node, name, type).first to node.nextEOG.first()
        }
    }

    private fun handleLoop(
        range: LatticeInterval,
        node: Node,
        name: String,
        type: KClass<out Collection>,
        goalNode: Node
    ): Pair<LatticeInterval, Node> {
        val afterLoop = node.nextEOG[1]
        val body: kotlin.Array<Statement>
        var newRange = range
        when (node) {
            is ForStatement -> {
                body =
                    when (node.statement) {
                        is Block -> node.statement.statements.toTypedArray()
                        null -> arrayOf()
                        else -> arrayOf(node.statement!!)
                    }
            }
            is WhileStatement -> {
                body =
                    when (node.statement) {
                        is Block -> node.statement.statements.toTypedArray()
                        null -> arrayOf()
                        else -> arrayOf(node.statement!!)
                    }
            }
            is ForEachStatement -> {
                body =
                    when (node.statement) {
                        is Block -> node.statement.statements.toTypedArray()
                        null -> arrayOf()
                        else -> arrayOf(node.statement!!)
                    }
            }
            is DoStatement -> {
                body =
                    when (node.statement) {
                        is Block -> node.statement.statements.toTypedArray()
                        null -> arrayOf()
                        else -> arrayOf(node.statement!!)
                    }
            }
            else -> throw NotImplementedException()
        }

        // Initialize the intervals for the previous loop iteration
        val prevBodyIntervals = Array<LatticeInterval>(body.size) { LatticeInterval.BOTTOM }
        // WIDENING
        // TODO: get max amount of iterations for the loop!
        // TODO: maybe only widen at one point (loop separator) for better results
        outer@ while (true) {
            for (index in body.indices) {
                // First apply the effect
                val (lRange, effect) = newRange.applyEffect(body[index], name, type)
                if (effect) {
                    newRange = lRange
                    // Then widen using the previous iteration
                    newRange = prevBodyIntervals[index].widen(newRange)
                    // If nothing changed we can abort
                    if (newRange == prevBodyIntervals[index]) {
                        break@outer
                    } else {
                        prevBodyIntervals[index] = newRange
                    }
                }
            }
        }
        // NARROWING
        // TODO: what is the right termination condition?
        outer@ while (true) {
            for (index in body.indices) {
                // First apply the effect
                val (lRange, effect) = newRange.applyEffect(body[index], name, type)
                if (effect) {
                    newRange = lRange
                    // Then widen using the previous iteration
                    newRange = prevBodyIntervals[index].narrow(newRange)
                    // If nothing changed we can abort
                    if (newRange == prevBodyIntervals[index]) {
                        break@outer
                    } else {
                        prevBodyIntervals[index] = newRange
                    }
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

    private fun handleBranch(
        range: LatticeInterval,
        node: Node,
        name: String,
        type: KClass<out Collection>,
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
        val finalMergedRange = finalBranchRanges.reduce { acc, r -> acc.join(r) }
        return finalMergedRange to mergeNode
    }

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
}
