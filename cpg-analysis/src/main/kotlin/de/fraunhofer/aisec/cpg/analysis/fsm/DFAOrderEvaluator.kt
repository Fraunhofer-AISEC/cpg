/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis.fsm

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.passes.astParent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class uses a [DFA] to evaluate if the order of statements in the CPG is correct. It needs
 * the following inputs:
 * - [consideredBases]: A set of the IDs of nodes (typically the [VariableDeclaration]) which are
 * considered.
 * - [nodeToRelevantMethod]: A mapping between CPG nodes and their operators used by the respective
 * edges in the DFA. Currently, we only consider [CallExpression]s. If a node is not contained in
 * this list, it is not considered by the evaluation as we assume that the method is not relevant.
 * - [thisPositionOfNode]: If a non-object oriented language was used, this is a map from CPG nodes
 * (i.e., the [CallExpression]) to the argument position serving as base of the operation.
 */
open class DFAOrderEvaluator(
    var consideredBases: Set<Long>,
    var nodeToRelevantMethod: Map<Node, String>,
    var thisPositionOfNode: Map<Node, Int> = mapOf()
) {
    private val nodeIDtoEOGPathSet = mutableMapOf<Long, MutableSet<String>>()
    private val log: Logger = LoggerFactory.getLogger(DFAOrderEvaluator::class.java)

    /**
     * Contains the functionality which is executed if the DFA does not contain a suitable
     * transition for the given [node]. The evaluation ensures that the [node] is relevant, i.e.,
     * its operator is considered by the DFA and its base is subject to analysis. This means that
     * the order is broken at [node].
     */
    open fun actionMissingTransitionForNode(node: Node, fsm: DFA) {
        log.error("There was a failure in the order of statements at node: ${node.id}")
        log.error(
            fsm.executionTrace
                .fold("") { r, t -> "$r${t.first}${t.third} (node id: ${t.second.id})\n" }
                .toString()
        )
    }

    /**
     * Contains the functionality which is executed if the DFA did not terminate in an accepting
     * state for the given [base]. This means that not all required statements have been executed
     * for [base] so far. The [fsm] holds the execution trace found by the analysis.
     */
    open fun actionNonAcceptingTermination(base: String, fsm: DFA) {
        log.error("Base $base did not terminate in an accepting state")
        log.error(
            fsm.executionTrace.fold("") { r, t ->
                "$r${t.first}${t.third} (node id: ${t.second.id})\n"
            }
        )
    }

    /**
     * Checks if a sequence of [Node]s/statemets starting from [startNode] follows the sequence
     * given by the [dfa]. If the sequence of statements violates the rules, the method returns
     * `false`, if it is correct, the method returns `true`. The flag [stopOnWrongBase] makes the
     * FSM stop evaluation of a base if an unexpected operation was observed for that base.
     */
    fun evaluateOrder(dfa: DFA, startNode: Node, stopOnWrongBase: Boolean = true): Boolean {
        // First dummy edge to simulate that we are in the start state.
        dfa.executionTrace.add(
            Triple(dfa.currentState!!, startNode, BaseOpEdge(DFA.EPSILON, "", dfa.currentState!!))
        )

        // Stores the current markings in the FSM (i.e., which base is at which FSM-node).
        val baseToFSM = mutableMapOf<String, DFA>()
        // Stores the states (i.e., nodes and their states in the fsm) to avoid endless loops.
        val seenStates = mutableSetOf<String>()
        // Maps a node to all the paths which were followed to reach the node.
        startNode.addEogPath("")
        // Collect bases (with their eogPath) which have already been found to be incorrect due to
        // an out-of-order call.
        val wrongBases = mutableSetOf<String>()

        var isValidOrder = true

        val worklist = mutableListOf(startNode) // Keeps the nodes which have to be processed.
        while (worklist.isNotEmpty()) {
            // Pop the next item from the worklist.
            val node = worklist.removeFirst()
            // Add the node to be processed together with an encoding of the path/fsm-state
            // to the list of already processed states.
            val currentState = getStateSnapshot(node.id!!, baseToFSM)
            seenStates.add(currentState)

            val eogPathSet = node.getEogPaths()
            if (eogPathSet == null) {
                log.debug("Error during order-evaluation, no path set for node ${node.id}")
                continue
            }

            // Iterate through the paths which can reach the current node and
            // try to make the transition in the DFA and retrieve the next nodes
            // to process for each of the paths.
            for (eogPath in eogPathSet) {
                // Currently, we only handle CallExpressions as "operation".
                // Check if the current node is of interest for the DFA.
                // This is the case if the map nodesToOp contains the node.
                if (node is CallExpression && nodeToRelevantMethod.contains(node)) {
                    val baseAndOp = getBaseAndOpOfNode(node, eogPath)

                    if (baseAndOp != null &&
                            (!stopOnWrongBase ||
                                wrongBases.none { wb ->
                                    wb.endsWith(baseAndOp.first.split("|")[1]) &&
                                        baseAndOp.first.startsWith(wb.split("|")[0])
                                })
                    ) {
                        // Make a transition in the DFA. In case, it is not possible,
                        // there was an error in the order of statements and allOk is
                        // set to false. If this is the first time we use the base (i.e.,
                        // we're at the starting node of the DFA), we clone the fsm and
                        // start the analysis for that base from scratch.
                        val allOk =
                            baseToFSM
                                .computeIfAbsent(baseAndOp.first) { dfa.clone() }
                                .makeTransitionWithOp(baseAndOp.second, node)

                        if (!allOk) {
                            actionMissingTransitionForNode(
                                node,
                                baseToFSM.computeIfAbsent(baseAndOp.first) { dfa.clone() }
                            )
                            wrongBases.add(baseAndOp.first)
                            isValidOrder = false
                        }
                    }
                }

                // Retrieve the nodes which have to be processed later and add them at the
                // end of the worklist.
                worklist.addAll(getNextNodes(node, eogPath, baseToFSM, seenStates))
            }
            // The current node has been analyzed with all its eogPaths.
            // Remove from map, if we visit it in another iteration
            nodeIDtoEOGPathSet.remove(node.id)
        }

        // Check if all the FSM are in an accepting state
        for (e in baseToFSM.entries) {
            log.info("Checking fsm in current state ${e.value.currentState}")
            if (!e.value.isAccepted()) {
                actionNonAcceptingTermination(e.key, e.value)
                isValidOrder = false
            }
        }

        return isValidOrder
    }

    /**
     * Returns a [Pair] holding the "base" and the "operator" of the function/method call happening
     * in [node]. The operator is retrieved from the map [nodeToRelevantMethod] and is probably the
     * name of the function called. If the call is neither a [MemberCallExpression] nor a
     * [ConstructExpression], it probably calls a function which does not have a "base" (as it is
     * the case for C). In that case, we try to look up the base in the map [thisPositionOfNode].
     *
     * The base is prefixed with [eogPath] in order to differentiate between different paths of
     * execution in the control flow.
     *
     * If the base cannot be retrieved, or if the [node] is not considered by the analysis (i.e., no
     * entry for [node] exists in the map [consideredBases]), the method returns `null`.
     */
    private fun getBaseAndOpOfNode(node: CallExpression, eogPath: String): Pair<String, String>? {
        // The "base" node, on which the DFA is based on. Ideally, this is a variable declaration in
        // the end.
        var base =
            when {
                node is MemberCallExpression -> node.base
                node is ConstructExpression -> node.astParent?.getSuitableDFGTarget()
                node.thisPosition != null ->
                    node.getBaseOfCallExpressionUsingArgument(node.thisPosition!!)
                else -> {
                    val dfgTarget = node.getSuitableDFGTarget()
                    if (dfgTarget != null && dfgTarget is ConstructExpression) {
                        dfgTarget.getSuitableDFGTarget()
                    } else {
                        dfgTarget
                    }
                }
            }

        if (base is DeclaredReferenceExpression && base.refersTo != null) {
            base = base.refersTo
        }

        if (base != null && consideredBases.contains(base.id)) {
            // We add the path as prefix to the base in order to differentiate between
            // the different paths of execution which both can use the same base.
            val prefixedBase = "$eogPath|${base.name}.${base.id}"
            return Pair(prefixedBase, nodeToRelevantMethod[node]!!)
        }

        if (base == null) {
            log.warn("The base of a call expression must not be null.")
        } else if (!consideredBases.contains(base.id)) {
            log.info("Skipping call because the base $[base.id} is not considered.")
        }

        return null
    }

    private fun Node.addEogPath(path: String) {
        nodeIDtoEOGPathSet.computeIfAbsent(this.id!!) { mutableSetOf() }.add(path)
    }

    private fun Node.getEogPaths(): Set<String>? {
        return nodeIDtoEOGPathSet[this.id]
    }

    /**
     * If it's not an object-oriented language, we need to retrieve the base in a different way.
     * Usually, it's the argument at index 0 but here, it's more obscure.
     */
    private val Node.thisPosition: Int?
        get() {
            return thisPositionOfNode[this]
        }

    /** Get the argument of a function call at index [argumentIndex]. */
    private fun CallExpression.getBaseOfCallExpressionUsingArgument(argumentIndex: Int): Node? {
        val list = this.arguments.filter { it.argumentIndex == argumentIndex }
        if (list.size != 1) return null

        var node: Node = list.first()
        // if the node refers to another node, return the node it refers to
        (node as? DeclaredReferenceExpression)?.refersTo?.let { node = it }
        return node
    }

    /**
     * Get the first next DFG node. First, the options are filtered by some "interesting" types of
     * statements
     *
     * TODO: The idea of returning only one of multiple elements looks quite dangerous! Why are
     * exactly those expressions "interesting"?
     */
    private fun Node.getSuitableDFGTarget(): Node? {
        return this.nextDFG
            .filter {
                it is DeclaredReferenceExpression ||
                    it is ReturnStatement ||
                    it is ConstructExpression ||
                    it is VariableDeclaration
            }
            .sortedWith(Comparator.comparing(Node::name))
            .firstOrNull()
    }

    /**
     * Traverses the graph to identify the next node in the evaluation order. Returns the nodes
     * which have to be analyzed later.
     *
     * If the graph contains a single next statement, we use that node. We do not need to change the
     * [eogPath] and simply add it to [nodeIDtoEOGPathSet] for the respective node.
     *
     * If the graph contains multiple next statements, we copy the current DFA for each of the paths
     * and generate a unique base for each path. The different FSMs are kept in [baseToFSM]. In the
     * case of multiple possible next statements, we also generate a different eogPath for each
     * statement and add the path ([eogPath]) to [nodeIDtoEOGPathSet].
     */
    private fun getNextNodes(
        node: Node,
        eogPath: String,
        baseToFSM: MutableMap<String, DFA>,
        seenStates: MutableSet<String>
    ): List<Node> {
        val outNodes = mutableListOf<Node>()
        outNodes += node.nextEOG

        if (outNodes.size == 1) {
            // We only have one node following this node, so we
            // simply propagate the current eogPath to the next node.
            outNodes[0].addEogPath(eogPath)
        } else if (outNodes.size > 1) {
            // We have multiple outgoing nodes, so we generate multiple new entries:
            //  - Each node gets its own eogPath which is split up
            //  - Each node gets a copy of the current DFA
            val newBases = mutableMapOf<String, DFA>()
            // Remove all the entries from baseToFSM which are now replaced with multiple new ones.
            // We store these entries without the eogPath prefix and update them in (1)
            val iterator = baseToFSM.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.key.startsWith(eogPath)) {
                    // keep the "|" before the real base, as we need it later anyway
                    newBases[entry.key.substring(eogPath.length)] = entry.value
                    // Remove the old entry as it will be replaced with 2 new ones.
                    iterator.remove()
                }
            }

            // (1) Update all entries previously removed from the baseToFSM map with
            // the new eog-path as prefix to the base
            for (i in outNodes.indices.reversed()) {
                val stateOfNext: String = getStateSnapshot(outNodes[i].id!!, baseToFSM)
                if (seenStates.contains(stateOfNext)) {
                    log.debug(
                        "Node/FSM state already visited: ${stateOfNext}. Remove from next nodes."
                    )
                    outNodes.removeAt(i)
                } else {
                    val newEOGPath = "$eogPath$i"
                    // Clone the FSM for each of the paths in the baseToFSM map.
                    newBases.forEach { (k: String, v: DFA) ->
                        baseToFSM[newEOGPath + k] = v.clone()
                    }
                    // Update the eog-path directly in the map of paths for the respective node.
                    outNodes[i].addEogPath(newEOGPath)
                }
            }
        }
        return outNodes
    }

    /**
     * Returns a String representation of all paths that have been observed so far together with the
     * [nodeId]. It is used to keep track of the states which have already been analyzed and to
     * avoid getting stuck in loops.
     */
    private fun getStateSnapshot(nodeId: Long, baseToFSM: Map<String, DFA>): String {
        val grouped =
            baseToFSM
                .entries
                .groupBy { e -> e.key.split("|")[1] }
                .map { x ->
                    "${x.key}(${x.value.map({ y -> y.value.currentState!! }).toSet().joinToString(",")})"
                }
                .sorted()
                .joinToString(",")

        return "$nodeId $grouped"
    }
}
