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
    data class tarjanInfo(val blackList: List<Node>) {
        var blockCounter = 0
        var stack = mutableListOf<Node>()
        var visited = mutableSetOf<Node>()
        var blockIDs = mutableMapOf<Node, Int>()
        var lowLinkValues = mutableMapOf<Node, Int>()
    }

    val tarjanInfoMap = mutableMapOf<Int, tarjanInfo>()

    override fun cleanup() {
        // Nothing to clean up
    }

    fun tarjan(bb: Node, level: Int) {
        tarjanInfoMap[level]?.blockIDs[bb] = tarjanInfoMap[level]!!.blockCounter
        tarjanInfoMap[level]?.lowLinkValues[bb] = tarjanInfoMap[level]!!.blockCounter
        tarjanInfoMap[level]?.blockCounter++
        tarjanInfoMap[level]?.visited!!.add(bb)
        tarjanInfoMap[level]?.stack!!.add(0, bb)

        for (next in bb.nextEOG) {
            // To detect inner loops, we put some nodes on a blacklist and see if we can still find
            // a loop
            if (next in tarjanInfoMap[level]!!.blackList) {
                break
            }
            if (next !in tarjanInfoMap[level]?.visited!!) {
                tarjan(next, level)
            }
            // If the node we came from is on the stack, we min its lowlinkValue with the one of bb
            if (next in tarjanInfoMap[level]?.stack!!) {
                tarjanInfoMap[level]?.lowLinkValues[bb] =
                    min(
                        tarjanInfoMap[level]?.lowLinkValues[bb] as Int,
                        tarjanInfoMap[level]?.lowLinkValues[next] as Int,
                    )
            }
        }

        if (tarjanInfoMap[level]?.blockIDs[bb] == tarjanInfoMap[level]?.lowLinkValues[bb]) {
            // Set the lowLinkValues of all nodes on the stack to the same, as they belong to the
            // same SCC
            // If bb is the first element on the stack, that's no SCC, so we simply remove it again
            if (tarjanInfoMap[level]?.stack?.first() == bb) tarjanInfoMap[level]?.stack?.remove(bb)
            // Otherwise, we found a loop, so we add the SCC edges
            else {
                println("Found a SCC (Level $level): ")
                val scc = StronglyConnectedComponent(level)
                // Can't iterate over the stack and remove items from it, so we create a clone
                val stackClone = tarjanInfoMap[level]?.stack!!.toList()
                val sccElements = mutableListOf<Node>()

                for (it in stackClone) {
                    tarjanInfoMap[level]?.lowLinkValues[it] = tarjanInfoMap[level]?.blockIDs[bb]!!
                    print("${it.location} (${tarjanInfoMap[level]?.lowLinkValues[it]}); ")
                    // pop the node from the real stack
                    tarjanInfoMap[level]?.stack?.remove(it)
                    sccElements.add(it)
                    if (it == bb) break
                }
                println()

                // label the edge of the element that exits the SCC
                // Find the element that points to the outside of the SCC
                val loopExitElement =
                    sccElements.single { it.nextEOG.any { nextEOG -> nextEOG !in sccElements } }

                // get the edge that's part of the SCC (AKA points to an element on the
                // stack) and label it
                loopExitElement.nextEOGEdges
                    .single { edge -> edge.end in sccElements }
                    .let { edge ->
                        edge.scc = scc
                        // also label the respective edges between node
                        (edge.start as? BasicBlock)
                            ?.endNode
                            ?.nextEOGEdges
                            ?.single { it.end == (edge.end as? BasicBlock)?.startNode }
                            ?.let { nodeEdge -> nodeEdge.scc = scc }
                    }

                //// find nested loops
                // We should always first find the outer loop. Now let's see if the elements in the
                // SCC again contain a loop if we remove the exit node
                val blackList = tarjanInfoMap[level]!!.blackList.toMutableList()
                val innerLevel = level + 1
                // blacklist the exitElement and see if we still have a loop, that would be an
                // inner loop
                blackList.add(loopExitElement)
                sccElements.remove(loopExitElement)
                tarjanInfoMap.computeIfAbsent(innerLevel) { tarjanInfo(blackList) }
                sccElements.forEach { element ->
                    if (element !in tarjanInfoMap[innerLevel]?.visited!!) {
                        tarjan(element, innerLevel)
                    }
                }
            }
        }
    }

    override fun accept(node: Node) {
        val bb = node.basicBlock.single() as BasicBlock
        tarjanInfoMap.computeIfAbsent(0) { tarjanInfo(emptyList()) }
        if (bb !in tarjanInfoMap[0]?.visited!!) {
            tarjan(bb, 0)
        }
    }
}
