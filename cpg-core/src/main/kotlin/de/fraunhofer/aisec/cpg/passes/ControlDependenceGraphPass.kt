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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.BranchingNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.helpers.EOGWorklist
import de.fraunhofer.aisec.cpg.helpers.Lattice
import de.fraunhofer.aisec.cpg.helpers.State
import de.fraunhofer.aisec.cpg.helpers.Worklist
import de.fraunhofer.aisec.cpg.passes.order.DependsOn

@DependsOn(EvaluationOrderGraphPass::class)
class ControlDependenceGraphPass : Pass() {
    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(t: TranslationResult) {
        t.functions.forEach(::handle)
    }

    private fun handle(functionDecl: FunctionDeclaration) {
        // Maps nodes to their "cdg parent" (i.e. the dominator) and also has the information
        // through which path it is reached. If all outgoing paths of the node's dominator result in
        // the node, we use the dominator's state instead (i.e., we move the node one layer upwards)
        val startState = PrevEOGState()
        startState.push(
            functionDecl,
            PrevEOGLattice(mapOf(Pair(functionDecl, setOf(functionDecl))))
        )
        val finalState =
            EOGWorklist().iterateEOGEN(functionDecl.nextEOGEdges, startState, ::handleEdge)

        val branchingNodeConditionals = getBranchingNodeConditions(functionDecl)

        // Collect the information, identify merge points, etc. This is not really efficient yet :(
        for ((node, dominatorPaths) in finalState) {
            val dominatorsList =
                dominatorPaths.elements.entries
                    .map { (k, v) -> Pair(k, v.toMutableSet()) }
                    .toMutableList()
            val finalDominators = mutableListOf<Pair<Node, MutableSet<Node>>>()
            while (dominatorsList.isNotEmpty()) {
                val (k, v) = dominatorsList.removeFirst()
                if (k != functionDecl && v.containsAll(branchingNodeConditionals[k] ?: setOf())) {
                    // We are reachable from all the branches of branch. Add this parent to the
                    // worklist or update an existing entry. Also consider already existing entries
                    // in finalDominators list and update it (if necessary)
                    val newDominatorMap = finalState[k]?.elements
                    newDominatorMap?.forEach { (newK, newV) ->
                        if (dominatorsList.any { it.first == newK }) {
                            // Entry exists => update it
                            dominatorsList.first { it.first == newK }.second.addAll(newV)
                        } else if (finalDominators.any { it.first == newK }) {
                            // Entry in final dominators => Delete it and add it to the worklist
                            // (but only if something changed)
                            val entry = finalDominators.first { it.first == newK }
                            finalDominators.remove(entry)
                            val update = entry.second.addAll(newV)
                            if (update) dominatorsList.add(entry) else finalDominators.add(entry)
                        } else {
                            // We don't have an entry yet => add a new one
                            dominatorsList.add(Pair(newK, newV.toMutableSet()))
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
            finalDominators.forEach { (k, _) -> node.addPrevCDG(k) }
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
    private fun getBranchingNodeConditions(functionDecl: FunctionDeclaration) =
        mapOf(
            // For the function declaration, there's only the path through the function declaration
            // itself.
            Pair(functionDecl, setOf(functionDecl)),
            *functionDecl
                .allChildren<BranchingNode>()
                .map { branchingNode ->
                    val mergingPoints =
                        if (
                            (branchingNode as? Node)?.nextEOGEdges?.any {
                                !it.isConditionalBranch()
                            } == true
                        ) {
                            // There's an unconditional path (case 1), so when reaching this branch,
                            // we're done. Collect all (=1) unconditional branches.
                            (branchingNode as? Node)
                                ?.nextEOGEdges
                                ?.filter { !it.isConditionalBranch() }
                                ?.map { it.end }
                                ?.toSet()
                        } else {
                            // All branches are executed based on some condition (case 2), so we
                            // collect all these branches.
                            (branchingNode as Node).nextEOGEdges.map { it.end }.toSet()
                        }
                    // Map this branching node to its merging points
                    Pair(branchingNode as Node, mergingPoints)
                }
                .toTypedArray()
        )

    companion object {
        @JvmStatic
        fun handleEdge(
            currentEdge: PropertyEdge<Node>,
            currentState: State<Node, Map<Node, Set<Node>>>,
            currentWorklist: Worklist<PropertyEdge<Node>, Node, Map<Node, Set<Node>>>
        ): Pair<State<Node, Map<Node, Set<Node>>>, Boolean> {
            // Check if we start in a branching node and if this edge leads to the conditional
            // branch. In this case, the next node will move "one layer downwards" in the CDG.
            if (currentEdge.start is BranchingNode) { // && currentEdge.isConditionalBranch()) {
                // We start in a branching node and end in one of the branches, so we have the
                // following state:
                // for the branching node "start", we have a path through "end".
                currentState.push(
                    currentEdge.end,
                    PrevEOGLattice(mapOf(Pair(currentEdge.start, setOf(currentEdge.end))))
                )
            } else {
                // We did not start in a branching node, so for the next node, we have the same path
                // (last branching + first end node) as for the start node of this edge.
                // If there is no state for the start node (most likely, this is the case for the
                // first edge in a function), we generate a new state where we start in "start" end
                // have "end" as the first node in the "branch".
                val state =
                    PrevEOGLattice(
                        currentState[currentEdge.start]?.elements
                            ?: mapOf(Pair(currentEdge.start, setOf(currentEdge.end)))
                    )
                currentState.push(currentEdge.end, state)
            }
            return Pair(currentState, true)
        }
    }
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
private fun <T : Node> PropertyEdge<T>.isConditionalBranch(): Boolean {
    return if (this.getProperty(Properties.BRANCH) == true) {
        true
    } else this.start is IfStatement && this.getProperty(Properties.BRANCH) == false
}

/**
 * Implements the [Lattice] over a set of nodes and their set of "nextEOG" nodes which reach this
 * node.
 */
class PrevEOGLattice(override val elements: Map<Node, Set<Node>>) :
    Lattice<Map<Node, Set<Node>>>(elements) {
    override fun lub(other: Lattice<Map<Node, Set<Node>>>?): Lattice<Map<Node, Set<Node>>> {
        val newMap =
            (other?.elements ?: mapOf()).mapValues { (_, v) -> v.toMutableSet() }.toMutableMap()
        for ((key, value) in this.elements) {
            newMap.computeIfAbsent(key, ::mutableSetOf).addAll(value)
        }
        return PrevEOGLattice(newMap)
    }
    override fun duplicate() = PrevEOGLattice(this.elements.toMap())
    override fun compareTo(other: Lattice<Map<Node, Set<Node>>>?): Int {
        return if (
            this.elements.keys.containsAll(other?.elements?.keys ?: setOf()) &&
                this.elements.all { (k, v) -> v.containsAll(other?.elements?.get(k) ?: setOf()) }
        ) {
            if (
                this.elements.keys.size > (other?.elements?.keys?.size ?: 0) ||
                    this.elements.any { (k, v) ->
                        v.size > (other?.elements?.get(k) ?: setOf()).size
                    }
            )
                1
            else 0
        } else {
            -1
        }
    }
}

/**
 * A state which actually holds a state for all [PropertyEdge]s, one only for declarations and one
 * for ReturnStatements.
 */
class PrevEOGState : State<Node, Map<Node, Set<Node>>>()
