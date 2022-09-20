/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.IterativeGraphWalker

/**
 * This pass tracks VariableDeclarations and values that are included in the graph by the DFG edge.
 * For this pass we traverse the EOG as it represents our understanding of the execution order and
 * can be used to remove the DFG edges that are not feasible.
 *
 * Control Flow Sensitivity in the DFG is only performed on VariableDeclarations and not on
 * FieldDeclarations. The reason for this being the fact, that the value of a field might be
 * modified to a value that is not present in the method, thus it is not detected by our variable
 * tracking.
 *
 * This pass will split up at every branch in the EOG. Because of the existence of loops and
 * multiple paths being able to run to through the same Declared reference expression we have to
 * keep track of the set of values (assignments) associated to a variable at JoinPoints. If the set
 * reaching a Joinpoint is not adding new values to one variable the path does not have to be
 * further explored. This ensures that the algorithm terminates and scales with the number of
 * different paths in the program finally reaching a fixpoint.
 *
 * This is only feasible because the values associate to a variable at fix-points is the location
 * assignment and not its symbolically executed value, in which case we could not ensure termination
 * for the algorithm.
 *
 * We here do not solve the problem of Exception-Handling, for this we will need additional
 * semantics on Edges.
 */
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(DFGPass::class)
open class ControlFlowSensitiveDFGPass : Pass() {
    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(translationResult: TranslationResult) {
        val walker = IterativeGraphWalker()
        walker.registerOnNodeVisit(::handle)
        for (tu in translationResult.translationUnits) {
            walker.iterate(tu)
        }
    }

    /**
     * ControlFlowSensitiveDFG Pass is performed on every method.
     *
     * @param node every node in the TranslationResult
     */
    protected fun handle(node: Node) {
        if (node is FunctionDeclaration || node is StatementHolder) {
            FunctionLevelFixpointIterator().handle(node)
        }
    }

    protected inner class FunctionLevelFixpointIterator {
        /**
         * A Node with refined DFG edges (key) is mapped to a set of nodes that were the previous
         * (unrefined) DFG edges and need to be removed later on
         */
        var removes = mutableMapOf<Node, MutableSet<Node>>()
        private val joinPoints = JoinPoints()

        fun handle(functionRoot: Node) {
            iterateTillFixpoint(functionRoot, VariableToPrevMap(), null, false)
            // Reset removes. TODO: Ideally don't even put stuff there in iterateTillFixpoint
            removes.clear()

            // Iterate over all join-points and propagates the state that is valid for the sum of
            // incoming eog-Paths to refine the dfg edges at variable usage points.
            for ((key, value) in joinPoints) {
                propagateFromJoinPoints(key, value, null, true)
            }

            // Remove unrefined DFG edges
            for ((currNode, prevNodes) in removes) {
                for (prev in prevNodes) {
                    currNode.removePrevDFG(prev)
                }
            }
        }

        /**
         * Stores the prevDFG to a VariableDeclaration of a node in the removes map and adds the
         * values of the VariableDeclaration as prevDFGs to the node
         *
         * @param currNode node that is analyzed
         */
        private fun setIngoingDFG(currNode: Node, variables: VariableToPrevMap) {
            for (prev in HashSet(currNode.prevDFG)) {
                if (prev is VariableDeclaration && prev in variables) {
                    currNode.addAllPrevDFG(variables[prev]!!)
                    removes.computeIfAbsent(currNode, ::mutableSetOf).add(prev)
                }
            }
        }

        /**
         * If a Node has a DFG to a VariableDeclaration we need to remove the nextDFG and store the
         * value of the node in our tracking map
         *
         * @param currNode Node that is being analyzed
         */
        private fun registerOutgoingDFG(currNode: Node, variables: VariableToPrevMap) {
            for (next in currNode.nextDFG) {
                (next as? VariableDeclaration)?.let {
                    variables.replace(it, LinkedHashSet(currNode.prevDFG))
                }
            }
        }

        /**
         * Get node of the BinaryOperator where the assignment is finished (last node of the
         * Assignment)
         *
         * @param node start node of the assignment LHS DeclaredReferenceExpression
         * @return return the last (in eog order) node of the assignment
         *
         * TODO: I don't know what "LAST" means here. We take the first node (before, it was
         * actually a random node).
         */
        private fun obtainAssignmentNode(node: DeclaredReferenceExpression): Node? {
            // TODO: What about AccessValues.READWRITE?
            // WRITE access is required if the node is written to
            if (node.access != AccessValues.WRITE) return null

            val alreadyVisited = mutableSetOf<Node>()
            val worklist = mutableListOf<Node>()
            worklist.addAll(node.nextEOG)
            while (worklist.isNotEmpty()) {
                val n = worklist.removeFirst()
                if (n is BinaryOperator && n.lhs == node) return n
                worklist.addAll(n.nextEOG.filter { it !in worklist && it !in alreadyVisited })
                alreadyVisited.add(n)
            }

            return null
        }

        /**
         * Perform the actual modification of the DFG edges based on the values that are recorded in
         * the variables map for every VariableDeclaration
         *
         * @param currNode node whose dfg edges have to be replaced
         */
        private fun modifyDFGEdges(currNode: Node, variables: VariableToPrevMap) {
            // A DeclaredReferenceExpression makes use of one of the VariableDeclaration we are
            // tracking. Therefore, we must modify the outgoing and ingoing DFG edges
            // Check for outgoing DFG edges
            registerOutgoingDFG(currNode, variables)

            // Check for ingoing DFG edges
            setIngoingDFG(currNode, variables)
        }

        /**
         * This function collects the set of variable definitions valid for the VariableDeclarations
         * defined in the program when reaching a join-point, a node reached by more than one
         * incoming EOG-Edge. The state is computed whenever a write access to a variable is
         * encountered. A node may be passed by multiple paths and therefore the states have to be
         * merged at these join-points. However, the number of paths is finite and scales well
         * enough to make a fixpoint iteration of states at join-points is therefore terminating and
         * feasible.
         *
         * This function iterates over the entire EOG starting at a fixed node. If the execution of
         * this function is started with the function-Node which also represents the EOG-Root-Node
         * all Nodes that are part of a valid execution path will be traversed.
         *
         * @param node
         * - Start of the fixpoint iteration
         * @param variables
         * - The state, composed of a mapping from variables to expression that were prior written
         * to it.
         * @param endNode
         * - A node where the iteration shall stop, if null the iteration stops at a point once a
         * fix-point no outgoing eog edge is reached
         * @param stopBefore
         * - denotes whether the iteration shall stop before or after processing the reached node.
         * @return The state after reaching on of the terminating conditions
         */
        private fun iterateTillFixpoint(
            node: Node?,
            variables: VariableToPrevMap,
            endNode: Node?,
            stopBefore: Boolean
        ): VariableToPrevMap {
            if (node == null) {
                return variables
            }
            var currentNode: Node = node
            var currentVariables = variables

            while (currentNode.nextEOG.isNotEmpty()) {
                if (currentNode.prevEOG.size != 1) {
                    // We only want to keep track of variables where they can change due to multiple
                    // incoming EOG-Edges or at the root of an EOG-path
                    val mergeResult = joinPoints.mergeVariablesToNode(currentNode, currentVariables)
                    currentVariables = mergeResult.second
                    if (!mergeResult.first) return currentVariables
                }
                if (currentNode == endNode && stopBefore) {
                    return currentVariables
                }
                currentNode =
                    updateIfDeclaredReferenceExpression(
                        currentNode,
                        currentVariables,
                        ::iterateTillFixpoint
                    )
                if (currentNode == endNode && !stopBefore) {
                    return currentVariables
                }

                // We use recursion when an eog path splits, if we can find a non-recursive
                // variation of this algorithm it may avoid some problems with scaling
                if (currentNode.nextEOG.size > 1) {
                    return mergeStatesOnJoinpoint(
                        currentVariables,
                        currentNode,
                        endNode,
                        stopBefore
                    )
                } else if (currentNode.nextEOG.isEmpty()) {
                    // There are no more nextEOG nodes, so we return the current state
                    return currentVariables
                }

                // Only one nextEOG node, so we continue with that path.
                val next = currentNode.nextEOG[0]
                currentVariables.updateIfDFGExists(next, currentNode)
                currentNode = next
            }
            return currentVariables
        }

        /**
         * Method that handles the updates for DeclaredReferenceExpressions when the EOG is
         * traversed
         *
         * @param currNode DeclaredReferenceExpression that is found in
         * @return Node where the EOG traversal should continue
         */
        private fun updateIfDeclaredReferenceExpression(
            currNode: Node,
            variables: VariableToPrevMap,
            iterationFunction:
                (
                    node: Node?,
                    variables: VariableToPrevMap,
                    endNode: Node?,
                    stopBefore: Boolean
                ) -> VariableToPrevMap
        ): Node {
            if (currNode !is DeclaredReferenceExpression) {
                return currNode
            }

            val assignmentNode = obtainAssignmentNode(currNode)
            if (assignmentNode != null) {
                // This is an assignment -> DeclaredReferenceExpression + Write Access
                // Search for = BinaryOperator as it marks the end of the assignment

                // There's exactly one outgoing eog edge from an assignment
                // DeclaredReferenceExpression
                val next = currNode.nextEOG.first()
                val updatedVariables =
                    iterationFunction(next, variables.copy(), assignmentNode, false)

                // necessary to return the updated variables state to the calling function as
                // the return value returns the next node
                variables.clear()
                variables.putAll(updatedVariables)

                // Perform Delayed DFG modifications (after having processed the entire
                // assignment)
                modifyDFGEdges(currNode, variables)

                // Update values of DFG Pass until the end of the assignment
                return assignmentNode
            }
            // Other DeclaredReferenceExpression that do not have a write-assignment we do not have
            // to delay the replacement of the value in the VariableDeclaration
            modifyDFGEdges(currNode, variables)
            return currNode // It is necessary for it to return the already processed node
        }

        /**
         * Propagates the state from the join-point until the next join-point is reached.
         *
         * @param node
         * - Start of the propagation
         * @param variables
         * - The state, composed of a mapping from variables to expression that were prior written
         * to it collected at some join-point.
         * @param endNode
         * - A node where the iteration shall stop, if null the iteration stops at a point once a
         * fix-point no outgoing eog edge is reached
         * @param stopBefore
         * - denotes whether the iteration shall stop before or after processing the reached node.
         * @return The state after reaching on of the terminating conditions
         */
        private fun propagateFromJoinPoints(
            node: Node?,
            variables: VariableToPrevMap,
            endNode: Node?,
            stopBefore: Boolean
        ): VariableToPrevMap {
            if (node == null) return variables
            var currentNode = node

            do {
                if (currentNode == endNode && stopBefore) {
                    return variables
                }
                currentNode =
                    updateIfDeclaredReferenceExpression(
                        currentNode!!,
                        variables,
                        ::propagateFromJoinPoints
                    )
                if (currentNode == endNode && !stopBefore) {
                    return variables
                }

                // We use recursion when an eog path splits, if we can find a non-recursive
                // variation  of this algorithm it may avoid some problems with scaling
                if (currentNode.nextEOG.size > 1) {
                    return mergeStatesOnJoinpoint(variables, currentNode, endNode, stopBefore)
                } else if (currentNode.nextEOG.isEmpty()) {
                    // No more nodes reachable. Done.
                    return variables
                }

                // One next node, so we continue in the eog
                val next = currentNode.nextEOG[0]
                variables.updateIfDFGExists(next, currentNode)
                currentNode = next
            } while (currentNode?.nextEOG?.isNotEmpty()!! || currentNode in joinPoints)
            return variables
        }

        /**
         * On a joinpoint (=end of a branch), we compute the effects for each branch separately and
         * merge together what we've seen in the different branches.
         */
        private fun mergeStatesOnJoinpoint(
            variables: VariableToPrevMap,
            currentNode: Node,
            endNode: Node?,
            stopBefore: Boolean
        ): VariableToPrevMap {
            val updatedVariables = VariableToPrevMap()
            for (next in currentNode.nextEOG) {
                variables.updateIfDFGExists(next, currentNode)
                // if (next !in joinPoints) { // Only in propagateFromJoinPoints but it should never
                // happen!
                // As we are propagating from joinpoints we stop when we reach the next joinpoint
                updatedVariables.merge(
                    iterateTillFixpoint(next, variables.copy(), endNode, stopBefore)
                )
                // }
            }
            // We explored everything in the different branches. done.
            return updatedVariables
        }
    }
}

class JoinPoints : LinkedHashMap<Node, VariableToPrevMap>() {
    /**
     * Adds the [variables] to the joinpoint at [node]. The return value checks if the state has
     * been updated.
     */
    fun mergeVariablesToNode(
        node: de.fraunhofer.aisec.cpg.graph.Node,
        variables: VariableToPrevMap
    ): Pair<Boolean, VariableToPrevMap> {
        if (node !in this) {
            this[node] = variables.copy()
            return Pair(true, variables)
        } else {
            val currentJoinpoint = this[node]!!
            if (!currentJoinpoint.merge(variables)) {
                return Pair(
                    false,
                    currentJoinpoint
                ) // Stop when we get to a joinpoint that does not
                // get a broader state through this path this ensures termination
            }
            // Progress execution with the updated JoinPoint set
            return Pair(true, currentJoinpoint.copy())
        }
    }
}

class VariableToPrevMap : LinkedHashMap<VariableDeclaration, MutableSet<Node>>() {
    /**
     * Merges the mapping of [other] into this map assuming that both mappings come from valid
     * paths. All the definition for variables are collected into the current state represented by
     * this map
     *
     * @return whether the merging resulted into an update
     */
    fun merge(other: VariableToPrevMap): Boolean {
        var changed = false
        for ((key, newAssignments) in other) {
            changed = computeIfAbsent(key, ::mutableSetOf).addAll(newAssignments) || changed
        }
        return changed
    }

    /**
     * Creates a shallow copy to the depth of the nodes. References to nodes are not copied as new
     * objects. Only the collections are created sd new Objects.
     */
    fun copy(): VariableToPrevMap {
        val shallowCopy = VariableToPrevMap()
        for ((key, value) in this) {
            shallowCopy[key] = HashSet(value)
        }
        return shallowCopy
    }

    /**
     * Adds the [value] to the state of the variable declaration [key] if [key] is a
     * [VariableDeclaration] and data flows from the value to the key.
     */
    fun updateIfDFGExists(
        key: de.fraunhofer.aisec.cpg.graph.Node,
        value: de.fraunhofer.aisec.cpg.graph.Node
    ) {
        if (key is VariableDeclaration && value in key.prevDFG) {
            this.computeIfAbsent(key, ::mutableSetOf).add(value)
        }
    }
}
