/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.graph.EOGStarterHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.overlays.BasicBlock
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ShortCircuitOperator
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import java.util.IdentityHashMap
import kotlin.collections.set

@DependsOn(EvaluationOrderGraphPass::class)
class BasicBlockCollectorPass(ctx: TranslationContext) : EOGStarterPass(ctx) {

    override fun cleanup() {
        // Nothing to clean up
    }

    override fun accept(t: Node) {
        val (firstBasicBlock, _, _) = collectBasicBlocks(t, false)
        (t as? EOGStarterHolder)?.firstBasicBlock = firstBasicBlock
    }

    /**
     * Collects all basic blocks starting at the given [startNode]. We identify basic blocks by
     * iterating through the EOG in O(n) and collecting all nodes which are reachable from the
     * [startNode]. We identify basic blocks by merges and branches in the EOG but may not consider
     * [ShortCircuitOperator]s as EOG-splitting nodes if we set [splitOnShortCircuitOperator] to
     * `false`..
     *
     * It returns a triple containing:
     * 1) The first basic block, which is the one starting at the [startNode].
     * 2) A collection of all basic blocks which were collected.
     * 3) A map from [Node] to the [BasicBlock] it belongs to. This is used to find out which basic
     *    block a node belongs to, which is important when constructing the CDG.
     *
     * @param startNode The node to start the collection from, usually a [FunctionDeclaration].
     * @param splitOnShortCircuitOperator If true, the basic blocks will be split on short-circuit
     *   operators (e.g., `&&` or `||`). If false, the short-circuit operators will not be
     *   considered, and the basic blocks will be collected as if they were not splitting up the
     *   EOG.
     */
    fun collectBasicBlocks(
        startNode: Node,
        splitOnShortCircuitOperator: Boolean,
    ): Triple<BasicBlock, Collection<BasicBlock>, Map<Node, BasicBlock>> {
        val allBasicBlocks = mutableSetOf<BasicBlock>()
        val firstBB = BasicBlock(startNode = startNode)
        allBasicBlocks.add(firstBB)
        val worklist =
            mutableListOf<Triple<Node, EvaluationOrder?, BasicBlock>>(
                Triple(startNode, null, firstBB)
            )
        val alreadySeen = IdentityHashMap<Node, BasicBlock>()

        while (worklist.isNotEmpty()) {
            var (currentStartNode, reachingEOGEdge, basicBlock) = worklist.removeFirst()
            // If we have already seen this node, we can skip it.
            if (currentStartNode in alreadySeen) {
                val oldBB = alreadySeen[currentStartNode]
                // There must be some sort of merge point to arrive here twice, so we add the
                // reaching basic block to the BB this node belongs to.
                oldBB?.prevEOG += basicBlock
                continue
            }

            if (currentStartNode.prevEOG.size > 1 && currentStartNode != basicBlock.startNode) {
                // If the currentStartNode is reachable from multiple paths, it starts a new basic
                // block. currentStartNode is part of the new basic block, so we add it after this
                // if statement.
                // Set the end node of the old basic block to the last node on the path
                basicBlock =
                    BasicBlock(startNode = currentStartNode).apply {
                        // ingoingEOGEdges.addAll(currentStartNode.prevEOGEdges)
                        // Save the relationships between the two basic blocks.
                        prevEOGEdges.add(basicBlock) {
                            this.branch = reachingEOGEdge?.branch
                            this.unreachable = reachingEOGEdge?.unreachable ?: false
                        }
                    }

                // Add the newly created basic block to the allBasicBlocks set
                allBasicBlocks.add(basicBlock)
            }
            // Add the basic block to the already seen map for this node
            alreadySeen[currentStartNode] = basicBlock

            basicBlock.nodes.add(currentStartNode)

            val shortCircuit = currentStartNode.astParent as? ShortCircuitOperator
            val language = shortCircuit?.language
            val nextRelevantEOGEdges =
                if (
                    shortCircuit != null &&
                        language is HasShortCircuitOperators &&
                        !splitOnShortCircuitOperator &&
                        currentStartNode.nextEOGEdges.size > 1
                ) {
                    // For ShortCircuitOperators, we select only the branch which is not a shortcut
                    // because it's not really a CDG-relevant node, and we want to save the branches
                    // it introduces.
                    currentStartNode.nextEOGEdges.filter {
                        shortCircuit.operatorCode in language.conjunctiveOperators == it.branch ||
                            it.branch == null
                    }
                } else {
                    currentStartNode.nextEOGEdges
                }

            if (nextRelevantEOGEdges.size > 1) {
                // If the currentStartNode splits up into multiple paths, the next nodes start a new
                // basic block. We already generate this here. But currentStartNode is still part of
                // the current basic block, so we add it before this if statement.
                // basicBlock.outgoingEOGEdges.addAll(nextRelevantEOGEdges)
                worklist.addAll(
                    nextRelevantEOGEdges.mapNotNull {
                        if (it.end in alreadySeen) {
                            alreadySeen[it.end]?.prevEOGEdges?.add(basicBlock) {
                                this.branch = it.branch
                                this.unreachable = it.unreachable
                            }
                            null
                        } else {
                            Triple(
                                it.end,
                                it,
                                BasicBlock(startNode = it.end).apply {
                                    // ingoingEOGEdges.add(it)
                                    // Save the relationships between the two basic blocks.
                                    prevEOGEdges.add(basicBlock) {
                                        this.branch = it.branch
                                        this.unreachable = it.unreachable
                                    }
                                    // Add the newly created basic block to the allBasicBlocks set
                                    allBasicBlocks.add(this)
                                },
                            )
                        }
                    }
                )
            } else if (nextRelevantEOGEdges.size == 1) {
                // If there's max. 1 incoming and max. 1 outgoing path, we can add the
                // currentStartNode to the current basic block.
                // If the currentStartNode has only one outgoing path, we can continue with this
                // path.
                val nextEdge = nextRelevantEOGEdges.single()
                worklist.add(Triple(nextEdge.end, nextEdge, basicBlock))
            } else {
                // List is empty, nothing to do.
            }
        }
        return Triple(firstBB, allBasicBlocks, alreadySeen)
    }
}
