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
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import kotlin.math.min

/**
 * This pass implements Tarjan's algorithm (the original
 * [paper](https://epubs.siam.org/doi/10.1137/0201010)) to find strongly connected components (SCCs)
 * in the Evaluation Order Graph (EOG) of a program. SCCs are subgraphs where every node is
 * reachable from every other node within the same subgraph (i.e., loops). In addition, we remove
 * the exit nodes of the SCC so that we can also detect nested loops. The pass labels the EOG edges
 * that are part of an SCC with the same identifier.
 */
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(BasicBlockCollectorPass::class, softDependency = true)
class SccPass(ctx: TranslationContext) : EOGStarterPass(ctx) {
    data class TarjanInfo(val blackList: List<Node>) {
        var blockCounter = 0
        var stack = mutableListOf<Node>()
        var visited = mutableSetOf<Node>()
        var blockIDs = mutableMapOf<Node, Int>()
        var lowLinkValues = mutableMapOf<Node, Int>()
    }

    val tarjanInfoMap = mutableMapOf<Int, TarjanInfo>()

    override fun cleanup() {
        // Nothing to clean up
    }

    fun tarjan(bb: Node, level: Int) {
        val currentInfo = tarjanInfoMap.computeIfAbsent(level) { TarjanInfo(emptyList()) }
        currentInfo.blockIDs[bb] = currentInfo.blockCounter
        currentInfo.lowLinkValues[bb] = currentInfo.blockCounter
        currentInfo.blockCounter++
        currentInfo.visited.add(bb)
        currentInfo.stack.add(0, bb)

        for (next in bb.nextEOG) {
            // To detect inner loops, we put some nodes on a blacklist and see if we can still find
            // a loop
            if (next in currentInfo.blackList) {
                break
            }
            if (next !in currentInfo.visited) {
                tarjan(next, level)
            }
            // If the node we came from is on the stack, we min its lowLinkValue with the one of bb
            if (next in currentInfo.stack) {
                currentInfo.lowLinkValues[bb] =
                    min(
                        currentInfo.lowLinkValues.getValue(bb),
                        currentInfo.lowLinkValues.getValue(next),
                    )
            }
        }

        if (currentInfo.blockIDs[bb] == currentInfo.lowLinkValues[bb]) {
            // Set the lowLinkValues of all nodes on the stack to the same, as they belong to the
            // same SCC
            // If bb is the first element on the stack, it's a trivial SCC (AKA an isolated node),
            // so we simply remove it again without setting any properties
            if (
                currentInfo.stack.first() == bb
                // Also consider SCCs that consist of a single element, in those, the bb points to
                // itself
                && bb.nextEOG.none { it == bb }
            ) {
                currentInfo.stack.remove(bb)
            } else {
                // Otherwise, we found a loop, so we set the scc-property of the respective edges
                // First, let's collect all the nodes that are part of the SCC from the stack
                // Note: This is not necessarily equal to all nodes on the stack, we only pop from
                // the stack until we are back at our initial node [bb]
                log.trace("Found a SCC (Level $level): ")
                // Can't iterate over the stack and remove items from it, so we iterate over a clone
                val stackClone = currentInfo.stack.toList()
                val sccElements = mutableListOf<Node>()

                for (it in stackClone) {
                    currentInfo.lowLinkValues[it] = currentInfo.blockIDs[bb]!!
                    log.trace("{} ({}); ", it.location, currentInfo.lowLinkValues[it])
                    // pop the node from the real stack
                    currentInfo.stack.remove(it)
                    sccElements.add(it)
                    // once we are back at our starting node [bb], we stop
                    if (it == bb) break
                }
                log.trace("Done with stack clone iteration.")

                val loopEntryElements = sccElements.filter { it.prevEOG.any { it !in sccElements } }
                loopEntryElements.forEach { loopEntryElement ->
                    loopEntryElement.prevEOGEdges
                        .filter { edge -> edge.start in sccElements }
                        .forEach { edge ->
                            edge.scc = level
                            // also label the respective edges between node
                            (edge.start as? BasicBlock)
                                ?.endNode
                                ?.nextEOGEdges
                                // in case of multiple nextEOGEdges, make sure we take the one
                                // pointing to a node that's part an sccElements-block
                                ?.filter { it.end.basicBlock.single() in sccElements }
                                ?.forEach { nodeEdge -> nodeEdge.scc = level }
                        }
                }

                // label the edge of elements that exit the SCC
                // Find elements that have edges pointing to the outside of the SCC
                val loopExitElements =
                    sccElements.filter { it.nextEOG.any { nextEOG -> nextEOG !in sccElements } }

                // get the edges that are part of the SCC (AKA points to an element on the
                // stack) and label them
                loopExitElements.forEach { loopExitElement ->
                    loopExitElement.nextEOGEdges
                        .filter { edge -> edge.end in sccElements }
                        .forEach { edge ->
                            edge.scc = level
                            // also label the respective edges between node
                            (edge.end as? BasicBlock)?.startNode?.prevEOGEdges?.forEach { nodeEdge
                                ->
                                nodeEdge.scc = level
                            }
                        }
                }

                // Additionally label edges when a block has 2 outgoing edges, both going to other
                // sccElements
                // This ensures that don't end up in the nextBranchEdgesList during EOG iteration
                // but have a higher priority
                // Note: If sccElements have only one nextEOG edge, this is not necessary, as these
                // edges will end up in the currentBBEdgesList list, which has the highes priority
                // anyway
                sccElements.forEach { sccElement ->
                    val nextSCCEdges =
                        sccElement.nextEOGEdges.filter { nextEOGEdge ->
                            nextEOGEdge.end in sccElements
                        }
                    if (nextSCCEdges.size > 1) {
                        nextSCCEdges.forEach { nextSCCEdge ->
                            nextSCCEdge.scc = level
                            // Do the same for the underlying edges
                            // There should be exacltly one edge from the endNode of the BB-Edge's
                            // start to a node that's in the BB of the BB-Edge's end
                            // Let's find this one and also label it
                            (nextSCCEdge.start as? BasicBlock)
                                ?.endNode
                                ?.nextEOGEdges
                                ?.single { it.end.basicBlock.single() == nextSCCEdge.end }
                                ?.scc = level
                        }
                    }
                }

                // find nested loops
                if (loopEntryElements.isNotEmpty() && loopExitElements.isNotEmpty()) {
                    // We should always first find the outer loop. Now let's see if the elements in
                    // the
                    // SCC again contain a loop if we remove the exit node
                    val blackList = currentInfo.blackList.toMutableList()
                    val innerLevel = level + 1
                    // blacklist the exitElement that comes last in the code (hoping that this is
                    // the very end) and see if we still have a loop, that would be an
                    // inner loop
                    val eliminatedElement =
                        loopEntryElements.sortedBy { it.location?.region?.startLine }.last()
                    blackList.add(eliminatedElement)
                    sccElements.remove(eliminatedElement)
                    val innerInfo =
                        tarjanInfoMap.computeIfAbsent(innerLevel) { TarjanInfo(blackList) }
                    sccElements.forEach { element ->
                        if (element !in innerInfo.visited) {
                            tarjan(element, innerLevel)
                        }
                    }
                }
            }
        }
    }

    override fun accept(node: Node) {
        if (node.basicBlock.isEmpty()) return
        val bb = node.basicBlock.single() as BasicBlock
        val entry = tarjanInfoMap.computeIfAbsent(0) { TarjanInfo(emptyList()) }
        if (bb !in entry.visited) {
            tarjan(bb, 1)
        }
    }
}
