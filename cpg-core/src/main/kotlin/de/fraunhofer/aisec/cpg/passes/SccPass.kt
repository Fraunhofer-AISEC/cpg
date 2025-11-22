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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.overlays.BasicBlock
import de.fraunhofer.aisec.cpg.graph.overlays.StronglyConnectedComponent
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import kotlin.math.min

@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(BasicBlockCollectorPass::class, softDependency = true)
class SccPass(ctx: TranslationContext) : EOGStarterPass(ctx) {
    var blockCounter = 0
    var stack = mutableListOf<BasicBlock>()
    var visited = mutableSetOf<BasicBlock>()
    var blockIDs = mutableMapOf<BasicBlock, Int>()
    var lowLinkValues = mutableMapOf<BasicBlock, Int>()

    override fun cleanup() {
        // Nothing to clean up
    }

    fun tarjan(bb: BasicBlock) {
        blockIDs[bb] = blockCounter
        lowLinkValues[bb] = blockCounter
        blockCounter++
        visited.add(bb)
        stack.add(0, bb)

        for (next in bb.nextEOG) {
            if (next !in visited) tarjan(next as BasicBlock)
            // If the node we came from is on the stack, we min its lowlinkValue with the one of bb
            if (next in stack) {
                lowLinkValues[bb] = min(lowLinkValues[bb] as Int, lowLinkValues[next] as Int)
            }
        }

        if (blockIDs[bb] == lowLinkValues[bb]) {
            // Set the lowLinkValues of all nodes on the stack to the same, as they belong to the
            // same SCC
            // If bb is the first element on the stack, that's no SCC, so we simply remove it again
            if (stack.first() == bb) stack.remove(bb)
            // Otherwise, we found a loop, so we add the SCC edges
            else {
                println("Found a SCC: ")
                val scc = StronglyConnectedComponent()
                val stackClone = stack.toList()
                for (it in stackClone) {
                    print("${it.location} (${lowLinkValues[it]}); ")
                    lowLinkValues[it] = blockIDs[bb]!!
                    // pop the node from the real stack
                    stack.remove(it)
                    // Label the EOG-Edges wit the SCC-Information
                    it.nextEOGEdges
                        .filter { edge -> edge.end in stackClone }
                        .forEach { edge ->
                            edge.scc = scc
                            // also label the respective edges between node
                            (edge.start as? BasicBlock)
                                ?.endNode
                                ?.nextEOGEdges
                                ?.filter { it.end == (edge.end as? BasicBlock)?.startNode }
                                ?.forEach { nodeEdge -> nodeEdge.scc = scc }
                        }
                    if (it == bb) break
                    println()
                }
            }
        }
    }

    override fun accept(node: Node) {
        val bb = node.basicBlock.single() as BasicBlock
        if (bb !in visited) {
            tarjan(bb)
        }
    }
}
