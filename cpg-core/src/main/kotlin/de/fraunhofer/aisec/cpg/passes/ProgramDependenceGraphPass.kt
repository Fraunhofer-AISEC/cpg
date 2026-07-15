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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.DependenceType
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.util.IdentityHashMap

/**
 * This pass collects the dependence information of each node into a Program Dependence Graph (PDG)
 * by traversing through the AST.
 */
@DependsOn(ControlDependenceGraphPass::class)
@DependsOn(DFGPass::class)
@DependsOn(PointsToPass::class, softDependency = true)
@DependsOn(ControlFlowSensitiveDFGPass::class, softDependency = true)
@DependsOn(DynamicInvokeResolver::class)
@Description(
    "Combines the Data Flow Graph (DFG) and Control Dependence Graph (CDG) into a Program Dependence Graph (PDG), providing a comprehensive view of both data and control dependencies within the program."
)
class ProgramDependenceGraphPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {
    private val visitor =
        object : IVisitor<AstNode>() {
            /**
             * Collects the data and control dependence edges of a node and adds them to the program
             * dependence edges
             */
            override fun visit(t: AstNode) {
                if (t is Reference) {
                    // We filter all prevDFGEdges if the condition affects the variable of t and
                    // if there's a flow from the prevDFGEdge's node through the condition to t.
                    val prevDFGToConsider = mutableListOf<Dataflow>()
                    t.prevDFGEdges.forEach { prevDfgEdge ->
                        val prevDfgNode = prevDfgEdge.start
                        // The prevDfgNode also flows into the condition. This is suspicious because
                        // if the condition is on each path between prevDfgNode and t, the condition
                        // is more relevant.
                        if (prevDfgNode is Reference || prevDfgNode is ValueDeclaration) {
                            val cdgConditionChildren =
                                t.prevCDG.flatMap {
                                    (it as? BranchingNode)?.branchedBy?.allChildren<Reference> { c
                                        ->
                                        c in prevDfgNode.nextDFG
                                    } ?: listOf()
                                }
                            if (
                                cdgConditionChildren.isNotEmpty() &&
                                    cdgConditionChildren.all {
                                        it != t && allEOGsFromToFlowThrough(prevDfgNode, t, it)
                                    }
                            ) {
                                // All data flows from the prevDfgNode to t flow through all the
                                // relevant CDG condition's children. This means that we can safely
                                // avoid the prevDfgNode in the PDG because it will be added
                                // transitively.
                            } else {
                                prevDFGToConsider.add(prevDfgEdge)
                            }
                        } else {
                            prevDFGToConsider.add(prevDfgEdge)
                        }
                    }

                    prevDFGToConsider.forEach {
                        it.dependence = DependenceType.DATA
                        t.prevPDGEdges.add(it)
                    }
                    t.prevPDGEdges += t.prevCDGEdges
                } else {
                    t.prevDFGEdges.forEach {
                        it.dependence = DependenceType.DATA
                        t.prevPDGEdges.add(it)
                    }
                    t.prevPDGEdges += t.prevCDGEdges
                }
            }
        }

    /**
     * Caches, per `from` node, a map from a `through` node to the set of nodes reachable from
     * `from` via [Node.nextEOG] without ever stepping into `through` (see
     * [computeReachableExcluding]). This lets [allEOGsFromToFlowThrough] answer the question for
     * every `to` with a single O(1) set lookup, instead of running a fresh EOG traversal per
     * `(from, to, through)` triple. The same `(from, through)` pair recurs across all references
     * that share a data-flow predecessor and a control-dependence condition, so this is a
     * substantial saving on functions with many references.
     *
     * Keys use reference identity ([IdentityHashMap]). The cache is cleared per translation unit in
     * [accept] to bound its memory footprint.
     */
    private val reachableExcludingCache =
        IdentityHashMap<Node, IdentityHashMap<Node, IdentitySet<Node>>>()

    /**
     * Returns `true` if every EOG path from [from] to [to] flows through [through]. Equivalently,
     * it returns `false` iff [to] is reachable from [from] following [Node.nextEOG] edges without
     * ever stepping into [through].
     *
     * The reachable set depends only on `(from, through)` and is memoized in
     * [reachableExcludingCache], so repeated queries with different [to] values are answered by a
     * set membership check.
     */
    private fun allEOGsFromToFlowThrough(from: Node, to: Node, through: Node): Boolean {
        val reachable =
            reachableExcludingCache
                .getOrPut(from) { IdentityHashMap() }
                .getOrPut(through) { computeReachableExcluding(from, through) }
        return to !in reachable
    }

    /**
     * Computes the set of nodes that are reachable as a [Node.nextEOG] successor of any node
     * reachable from [from], where paths never step into [through]. A node `n` is in the result iff
     * there is an EOG path from [from] to `n` that does not pass through [through]; this is exactly
     * the set of `to` values for which [allEOGsFromToFlowThrough] returns `false`.
     */
    private fun computeReachableExcluding(from: Node, through: Node): IdentitySet<Node> {
        val worklist = mutableListOf(from)
        val alreadySeenNodes = identitySetOf<Node>()
        val reachable = identitySetOf<Node>()

        while (worklist.isNotEmpty()) {
            val currentStatus = worklist.removeFirst()
            if (!alreadySeenNodes.add(currentStatus)) {
                continue
            }
            for (next in currentStatus.nextEOG) {
                if (next == through) {
                    continue
                }
                reachable.add(next)
                if (next !in alreadySeenNodes) {
                    worklist.add(next)
                }
            }
        }
        return reachable
    }

    override fun accept(tu: TranslationUnit) {
        reachableExcludingCache.clear()
        tu.statements.forEach(::handle)
        tu.namespaces.forEach(::handle)
        tu.declarations.forEach(::handle)
        reachableExcludingCache.clear()
    }

    override fun cleanup() {
        // Nothing to do
    }

    private fun handle(node: Node) {
        if (node is AstNode) {
            node.accept<AstNode>(Strategy::AST_FORWARD, visitor)
        }
    }
}
