/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.BranchingNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.cyclomaticComplexity
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConditionalExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ShortCircuitOperator
import de.fraunhofer.aisec.cpg.helpers.*
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import java.util.*
import kotlin.system.measureTimeMillis

/** This pass builds the Control Dependence Graph (CDG) by iterating through the EOG. */
@DependsOn(EvaluationOrderGraphPass::class)
open class ControlDependenceGraphPass(ctx: TranslationContext) : EOGStarterPass(ctx) {

    class Configuration(
        /**
         * This specifies the maximum complexity (as calculated per
         * [Statement.cyclomaticComplexity]) a [FunctionDeclaration] must have in order to be
         * considered.
         */
        var maxComplexity: Int? = null
    ) : PassConfiguration()

    override fun cleanup() {
        // Nothing to do
    }

    /**
     * Computes the CDG for the given [startNode]. It performs the following steps:
     * 1) Compute the "parent branching node" for each node and through which path the node is
     *    reached
     * 2) Find out which branch of a [BranchingNode] is actually conditional. The other ones aren't.
     * 3) For each node: 3.a) Check if the node is reachable through an unconditional path of its
     *    parent [BranchingNode] or through all the conditional paths. 3.b) Move the node "one layer
     *    up" by finding the parent node of the current [BranchingNode] and changing it to this
     *    parent node and the path(s) through which the [BranchingNode] node is reachable. 3.c)
     *    Repeat step 3) until you cannot move the node upwards in the CDG anymore.
     */
    override fun accept(startNode: Node) {
        // For now, we only execute this for function declarations, we will support all EOG starters
        // in the future.
        if (startNode !is FunctionDeclaration) {
            return
        }
        val max = passConfig<Configuration>()?.maxComplexity
        val c = startNode.body?.cyclomaticComplexity ?: 0
        if (max != null && c > max) {
            log.info(
                "Ignoring function ${startNode.name} because its complexity (${c}) is greater than the configured maximum (${max})"
            )
            return
        }
        log.info("[CDG] Analyzing function ${startNode.name}. Complexity: $c")

        // Maps nodes to their "cdg parent" (i.e. the dominator) and also has the information
        // through which path it is reached. If all outgoing paths of the node's dominator result in
        // the node, we use the dominator's state instead (i.e., we move the node one layer upwards)
        val startState = PrevEOGState()
        val identityMap = IdentityHashMap<Node, IdentitySet<Node>>()
        identityMap[startNode] = identitySetOf(startNode)
        startState.push(startNode, PrevEOGLattice(identityMap))
        var finalState: State<Node, IdentityHashMap<Node, IdentitySet<Node>>>
        val executionTime = measureTimeMillis {
            finalState = iterateEOG(startNode.nextEOGEdges, startState, ::handleEdge) ?: return
        }
        log.info("[CDG] iterated EOG for ${startNode.name}. Time: $executionTime")
        val branchingNodeConditionals = getBranchingNodeConditions(startNode)

        // Collect the information, identify merge points, etc. This is not really efficient yet :(
        for ((node, dominatorPaths) in finalState) {
            val dominatorsList =
                dominatorPaths.elements.entries
                    .map { (k, v) -> Pair(k, v.toMutableSet()) }
                    .toMutableList()
            val finalDominators = mutableListOf<Pair<Node, MutableSet<Node>>>()
            val conditionKeys =
                dominatorPaths.elements.entries
                    .filter { (k, _) ->
                        (k as? BranchingNode)?.branchedBy == node ||
                            node in
                                ((k as? BranchingNode)?.branchedBy?.allChildren<Node>() ?: listOf())
                    }
                    .map { (k, _) -> k }
            if (conditionKeys.isNotEmpty()) {
                // The node is part of the condition. For loops, it happens that these nodes are
                // somehow put in the CDG of the surrounding statement (e.g. the loop) but we don't
                // want this. Move it one layer up.
                for (k1 in conditionKeys) {
                    dominatorsList.removeIf { k1 == it.first }
                    finalState[k1]?.elements?.forEach { (newK, newV) ->
                        val entry = dominatorsList.firstOrNull { it.first == newK }
                        entry?.let {
                            dominatorsList.remove(entry)
                            val update = entry.second.addAll(newV)
                            if (update) dominatorsList.add(entry) else finalDominators.add(entry)
                        } ?: dominatorsList.add(Pair(newK, newV.toMutableSet()))
                    }
                }
            }
            val alreadySeen = mutableSetOf<Int>()

            while (dominatorsList.isNotEmpty()) {
                val (k, v) = dominatorsList.removeFirst()
                if (!alreadySeen.add(Pair(k, v).hashCode())) {
                    continue
                }
                if (k != startNode && v.containsAll(branchingNodeConditionals[k] ?: setOf())) {
                    // We are reachable from all the branches of a branching node. Add this parent
                    // to the worklist or update an existing entry. Also consider already existing
                    // entries in finalDominators list and update it (if necessary)
                    val newDominatorMap = finalState[k]?.elements
                    newDominatorMap?.forEach { (newK, newV) ->
                        when {
                            dominatorsList.any { it.first == newK } -> {
                                // Entry exists => update it
                                dominatorsList.first { it.first == newK }.second.addAll(newV)
                            }
                            finalDominators.any { it.first == newK } -> {
                                // Entry in final dominators => Delete it and add it to the worklist
                                // (but only if something changed)
                                val entry = finalDominators.first { it.first == newK }
                                finalDominators.remove(entry)
                                val update = entry.second.addAll(newV)
                                if (
                                    update &&
                                        Pair(entry.first, entry.second).hashCode() !in alreadySeen
                                ) {
                                    dominatorsList.add(entry)
                                } else finalDominators.add(entry)
                            }
                            alreadySeen.none {
                                // it.first == newK && it.second == newV
                                it == Pair(newK, newV.toMutableSet()).hashCode()
                            } -> {
                                // We don't have an entry yet => add a new one
                                val newEntry = Pair(newK, newV.toMutableSet())
                                dominatorsList.add(newEntry)
                            }
                            else -> {
                                // Not sure what to do, there seems to be a cycle but this entry is
                                // not in finalDominators for some reason. Add to finalDominators
                                // now.
                                finalDominators.add(Pair(newK, newV.toMutableSet()))
                            }
                        }
                    }
                } else {
                    // Node is not reachable from all branches => k dominates node. Add to
                    // finalDominators.
                    finalDominators.add(Pair(k, v))
                }
            }

            // We have all the dominators of this node and potentially traversed the graph
            // "upwards". Add the CDG edges
            // log.info("[CDG] iterating through the finalDomniators")
            finalDominators
                .filter { (k, _) -> k != node }
                .forEach { (k, v) ->
                    val branchesSet =
                        k.nextEOGEdges
                            .filter { edge -> edge.end in v }
                            .mapNotNull { it.branch }
                            .toSet()

                    node.prevCDGEdges.add(k) {
                        branches =
                            when {
                                branchesSet.isNotEmpty() -> {
                                    branchesSet
                                }
                                k is IfStatement &&
                                    (branchingNodeConditionals[k]?.size ?: 0) >
                                        1 -> { // Note: branchesSet must be empty here The if
                                    // statement has only a then branch but there's a way
                                    // to "jump out" of this branch. In this case, we
                                    // want to set the false property here.
                                    setOf(false)
                                }
                                else -> setOf()
                            }
                    }
                }
        }
    }

    /*
     * For a branching node, we identify which path(s) have to be found to be in a "merging point".
     * There are two options:
     *   1) There's a path which is executed independent of the branch (e.g. this is the case for an if-statement without an else-branch).
     *   2) A node can be reached from all conditional branches.
     *
     * This method collects the merging points. It also includes the function declaration itself.
     */
    private fun getBranchingNodeConditions(functionDeclaration: FunctionDeclaration) =
        mapOf(
            // For the function declaration, there's only the path through the function declaration
            // itself.
            Pair(functionDeclaration, setOf(functionDeclaration)),
            *functionDeclaration
                .allChildren<BranchingNode>()
                .filterIsInstance<Node>()
                .map { branchingNode ->
                    val mergingPoints =
                        if (branchingNode.nextEOGEdges.any { !it.isConditionalBranch() }) {
                            // There's an unconditional path (case 1), so when reaching this branch,
                            // we're done. Collect all (=1) unconditional branches.
                            branchingNode.nextEOGEdges
                                .filter { !it.isConditionalBranch() }
                                .map { it.end }
                                .toSet()
                        } else {
                            // All branches are executed based on some condition (case 2), so we
                            // collect all these branches.
                            branchingNode.nextEOGEdges.map { it.end }.toSet()
                        }
                    // Map this branching node to its merging points
                    Pair(branchingNode, mergingPoints)
                }
                .toTypedArray()
        )
}

/**
 * This method is executed for each EOG edge which is in the worklist. [currentEdge] is the edge to
 * process, [currentState] contains the state which was observed before arriving here.
 *
 * This method modifies the state for the next eog edge as follows:
 * - If [currentEdge] starts in a [BranchingNode], the end node depends on the start node. We modify
 *   the state to express that "the end node depends on the start node and is reachable through the
 *   path starting at the end node".
 * - For all other starting nodes, we copy the state of the start node to the end node.
 *
 * Returns the updated state and true because we always expect an update of the state.
 */
fun handleEdge(
    currentEdge: Edge<Node>,
    currentState: State<Node, IdentityHashMap<Node, IdentitySet<Node>>>
): State<Node, IdentityHashMap<Node, IdentitySet<Node>>> {
    // Check if we start in a branching node and if this edge leads to the conditional
    // branch. In this case, the next node will move "one layer downwards" in the CDG.
    if (currentEdge.start is BranchingNode) { // && currentEdge.isConditionalBranch()) {
        // We start in a branching node and end in one of the branches, so we have the
        // following state:
        // for the branching node "start", we have a path through "end".
        val prevPathLattice =
            PrevEOGLattice(
                IdentityHashMap(
                    currentState[currentEdge.start]?.elements?.filter { (k, _) ->
                        k == currentEdge.start
                    }
                )
            )
        val map = IdentityHashMap<Node, IdentitySet<Node>>()
        map[currentEdge.start] = identitySetOf(currentEdge.end)
        val newPath = PrevEOGLattice(map).lub(prevPathLattice) as PrevEOGLattice
        currentState.push(currentEdge.end, newPath)
    } else {
        // We did not start in a branching node, so for the next node, we have the same path
        // (last branching + first end node) as for the start node of this edge.
        // If there is no state for the start node (most likely, this is the case for the
        // first edge in a function), we generate a new state where we start in "start" end
        // have "end" as the first node in the "branch".
        val state =
            PrevEOGLattice(
                currentState[currentEdge.start]?.elements
                    ?: IdentityHashMap(
                        mutableMapOf(Pair(currentEdge.start, identitySetOf(currentEdge.end)))
                    )
            )
        currentState.push(currentEdge.end, state)
    }
    return currentState
}

/**
 * For all types I've seen so far, the "true" branch is executed conditionally.
 *
 * For if-statements, the BRANCH property is set to "false" for the "else" branch (which is also
 * executed conditionally) and is not set in the code after an if-statement if there's no else
 * branch (which is also always executed). For all other nodes, the "false" branch is the code after
 * the loop or so (i.e., the unconditionally executed path).
 *
 * Note: This method does not account for return statements in the conditional part or endless loops
 * where the other branch is actually also conditionally executed (or not). It should be easy to
 * change this if we do not want this behavior (just remove the condition on the start node of the
 * "false" branch).
 */
private fun EvaluationOrder.isConditionalBranch(): Boolean {
    return if (branch == true) {
        true
    } else
        (this.start is IfStatement ||
            this.start is ConditionalExpression ||
            this.start is ShortCircuitOperator) && branch == false ||
            (this.start is IfStatement &&
                !(this.start as IfStatement).allBranchesFromMyThenBranchGoThrough(
                    (this.start as IfStatement).nextUnconditionalNode
                ))
}

private val IfStatement.nextUnconditionalNode: Node?
    get() = this.nextEOGEdges.firstOrNull { it.branch == null }?.end

private fun IfStatement.allBranchesFromMyThenBranchGoThrough(node: Node?): Boolean {
    if (this.thenStatement.allChildren<ReturnStatement>().isNotEmpty()) return false

    if (node == null) return true

    val alreadySeen = mutableSetOf<Node>()
    val nextNodes = this.nextEOGEdges.filter { it.branch == true }.map { it.end }.toMutableList()

    while (nextNodes.isNotEmpty()) {
        val nextNode = nextNodes.removeFirst()
        if (nextNode == node) {
            continue
        } else if (nextNode.nextEOG.isEmpty()) {
            // We're at the end of the EOG but didn't see "node" on this path. Fail
            return false
        }
        alreadySeen.add(nextNode)
        nextNodes.addAll(nextNode.nextEOG.filter { it !in alreadySeen })
    }

    return true
}

/**
 * Implements the [LatticeElement] over a set of nodes and their set of "nextEOG" nodes which reach
 * this node.
 */
class PrevEOGLattice(override val elements: IdentityHashMap<Node, IdentitySet<Node>>) :
    LatticeElement<IdentityHashMap<Node, IdentitySet<Node>>>(elements) {

    override fun lub(
        other: LatticeElement<IdentityHashMap<Node, IdentitySet<Node>>>
    ): LatticeElement<IdentityHashMap<Node, IdentitySet<Node>>> {
        val newMap = IdentityHashMap(other.elements.mapValues { (_, v) -> v.toIdentitySet() })
        for ((key, value) in this.elements) {
            newMap.computeIfAbsent(key, ::identitySetOf).addAll(value)
        }
        return PrevEOGLattice(newMap)
    }

    override fun duplicate() = PrevEOGLattice(IdentityHashMap(this.elements))

    override fun compareTo(other: LatticeElement<IdentityHashMap<Node, IdentitySet<Node>>>): Int {
        return if (
            this.elements.keys.containsAll(other.elements.keys) &&
                this.elements.all { (k, v) -> v.containsAll(other.elements[k] ?: identitySetOf()) }
        ) {
            if (
                this.elements.keys.size > (other.elements.keys.size) ||
                    this.elements.any { (k, v) -> v.size > (other.elements[k]?.size ?: 0) }
            )
                1
            else 0
        } else {
            -1
        }
    }
}

/**
 * A state which actually holds a state for all [Edge]s. It maps the node to its
 * [BranchingNode]-parent and the path through which it is reached.
 */
class PrevEOGState : State<Node, IdentityHashMap<Node, IdentitySet<Node>>>()
