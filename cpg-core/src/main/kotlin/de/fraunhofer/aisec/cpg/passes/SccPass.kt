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
 *
 * Algorithm: [tarjan] runs the DFS with an explicit [WorkItem] stack instead of recursion, since
 * EOGs can be deeper than the JVM call stack allows. A [DfsFrame] is one DFS call frame - a node
 * plus its not-yet-visited successors - pushed on descent and popped on return, updating lowlink
 * values the usual Tarjan way. When a popped frame's node turns out to be an SCC root,
 * [handleSccRoot] labels that SCC's edges with the current [WorkItem.level], then - to find loops
 * nested inside it - strips the SCC's exit node and re-decomposes the remaining elements one
 * `level` deeper, using a [DecompDriver] to drive that re-run. This repeats until no further nested
 * loop remains, leaving concentric loops labeled from outermost to innermost.
 */
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(BasicBlockCollectorPass::class, softDependency = true)
@Description("Pass that finds strongly connected components in the EOG using Tarjan's algorithm.")
class SccPass(ctx: TranslationContext) : EOGStarterPass(ctx) {
    data class TarjanInfo(val blackList: List<Node>) {
        var blockCounter = 0
        var stack = mutableListOf<Node>()
        var visited = mutableSetOf<Node>()
        var blockIDs = mutableMapOf<Node, Int>()
        var lowLinkValues = mutableMapOf<Node, Int>()
    }

    val tarjanInfoMap = mutableMapOf<Int, TarjanInfo>()

    /**
     * One item on [tarjan]'s explicit work-stack, tagged with the decomposition [level] it's for.
     */
    private sealed class WorkItem(val level: Int)

    /**
     * One DFS call frame for [node]: pulls its not-yet-visited successors lazily from [iterator].
     */
    private class DfsFrame(val node: Node, level: Int, val iterator: Iterator<Node>) :
        WorkItem(level)

    /**
     * Drives the nested-loop re-decomposition over `sccElements`, pushing a [DfsFrame] only for an
     * element that is still unvisited *at the moment its turn comes up* - visiting one element can
     * visit others in the same decomposition, so pushing frames for all of them up front would use
     * a stale visited-snapshot.
     */
    private class DecompDriver(val iterator: Iterator<Node>, level: Int) : WorkItem(level)

    override fun cleanup() {
        // Nothing to clean up
    }

    private fun initNode(node: Node, info: TarjanInfo) {
        info.blockIDs[node] = info.blockCounter
        info.lowLinkValues[node] = info.blockCounter
        info.blockCounter++
        info.visited.add(node)
        info.stack.add(0, node)
    }

    /**
     * Runs Tarjan's algorithm from [bb] at decomposition [level]; see the class doc for the
     * algorithm.
     */
    fun tarjan(bb: Node, level: Int) {
        val workStack = ArrayDeque<WorkItem>()
        val startInfo = tarjanInfoMap.computeIfAbsent(level) { TarjanInfo(emptyList()) }
        initNode(bb, startInfo)
        workStack.addLast(DfsFrame(bb, level, bb.nextEOG.iterator()))

        while (workStack.isNotEmpty()) {
            when (val item = workStack.last()) {
                is DfsFrame -> {
                    val info = tarjanInfoMap.computeIfAbsent(item.level) { TarjanInfo(emptyList()) }
                    if (item.iterator.hasNext()) {
                        val next = item.iterator.next()
                        // To detect inner loops, we put some nodes on a blacklist and see if we
                        // can still find a loop
                        if (next in info.blackList) {
                            continue
                        }
                        if (next !in info.visited) {
                            initNode(next, info)
                            workStack.addLast(DfsFrame(next, item.level, next.nextEOG.iterator()))
                        } else if (next in info.stack) {
                            // If the node we came from is on the stack, we min its lowLinkValue
                            // with the one of item.node
                            info.lowLinkValues[item.node] =
                                min(
                                    info.lowLinkValues.getValue(item.node),
                                    info.lowLinkValues.getValue(next),
                                )
                        }
                    } else {
                        workStack.removeLast()
                        val v = item.node
                        if (info.blockIDs[v] == info.lowLinkValues[v]) {
                            handleSccRoot(v, info, item.level, workStack)
                        }
                        // Propagate v's lowLinkValue up to the parent frame, but only within the
                        // same decomposition level. No-op if handleSccRoot just ran for v, since
                        // that always removes v from the stack.
                        val parent = workStack.lastOrNull()
                        if (parent is DfsFrame && parent.level == item.level && v in info.stack) {
                            info.lowLinkValues[parent.node] =
                                min(
                                    info.lowLinkValues.getValue(parent.node),
                                    info.lowLinkValues.getValue(v),
                                )
                        }
                    }
                }
                is DecompDriver -> {
                    val innerInfo = tarjanInfoMap.getValue(item.level)
                    if (item.iterator.hasNext()) {
                        val element = item.iterator.next()
                        if (element !in innerInfo.visited) {
                            initNode(element, innerInfo)
                            workStack.addLast(
                                DfsFrame(element, item.level, element.nextEOG.iterator())
                            )
                        }
                    } else {
                        workStack.removeLast()
                    }
                }
            }
        }
    }

    /**
     * Handles an SCC root [bb] found at [level] (`blockIDs[bb] == lowLinkValues[bb]`): labels the
     * SCC's edges and, if applicable, kicks off the nested-loop decomposition by pushing a
     * [DecompDriver] onto [workStack].
     */
    private fun handleSccRoot(
        bb: Node,
        currentInfo: TarjanInfo,
        level: Int,
        workStack: ArrayDeque<WorkItem>,
    ) {
        // A trivial SCC (isolated node, or a single node with a self-loop) needs no labeling.
        if (currentInfo.stack.first() == bb && bb.nextEOG.none { it == bb }) {
            currentInfo.stack.remove(bb)
            return
        }

        log.trace("Found a SCC (Level $level): ")
        // Not necessarily all nodes on the stack - pop only down to bb. Clone since we can't
        // iterate the stack while removing from it.
        val stackClone = currentInfo.stack.toList()
        val sccElements = mutableListOf<Node>()
        for (it in stackClone) {
            currentInfo.lowLinkValues[it] = currentInfo.blockIDs[bb]!!
            log.trace("{} ({}); ", it.location, currentInfo.lowLinkValues[it])
            currentInfo.stack.remove(it)
            sccElements.add(it)
            if (it == bb) break
        }
        log.trace("Done with stack clone iteration.")

        // Mark the SCC's incoming edge with an scc-flag, to tell it apart from a mergePoint
        // (which also has 2 incoming EOG edges).
        val loopEntryElements = sccElements.filter { it.prevEOG.any { it !in sccElements } }
        loopEntryElements.forEach { loopEntryElement ->
            loopEntryElement.prevEOGEdges
                .filter { edge -> edge.start in sccElements }
                .forEach { edge ->
                    edge.scc = level
                    // also label the corresponding BasicBlock-level edge
                    (edge.start as? BasicBlock)
                        ?.endNode
                        ?.nextEOGEdges
                        ?.filter { it.end.basicBlock.all { it in sccElements } }
                        ?.forEach { nodeEdge -> nodeEdge.scc = level }
                }
        }

        // Mark the SCC's outgoing (exit) edges the same way.
        val loopExitElements =
            sccElements.filter { it.nextEOG.any { nextEOG -> nextEOG !in sccElements } }
        loopExitElements.forEach { loopExitElement ->
            loopExitElement.nextEOGEdges
                .filter { edge -> edge.end in sccElements }
                .forEach { edge ->
                    edge.scc = level
                    (edge.end as? BasicBlock)
                        ?.startNode
                        ?.prevEOGEdges
                        ?.filter { it.start.basicBlock.all { it in sccElements } }
                        ?.forEach { nodeEdge -> nodeEdge.scc = level }
                }
        }

        // A block with 2 outgoing edges that both stay inside the SCC needs labeling too, so it
        // takes priority over the nextBranchEdgesList during EOG iteration (a single such edge
        // would already land in the higher-priority currentBBEdgesList without this).
        sccElements.forEach { sccElement ->
            val nextSCCEdges =
                sccElement.nextEOGEdges.filter { nextEOGEdge -> nextEOGEdge.end in sccElements }
            if (nextSCCEdges.size > 1) {
                nextSCCEdges.forEach { nextSCCEdge ->
                    nextSCCEdge.scc = level
                    // There should be exactly one matching BasicBlock-level edge to label too.
                    val bbNextEdges =
                        (nextSCCEdge.start as? BasicBlock)?.endNode?.nextEOGEdges?.filter {
                            it.end.basicBlock.single() == nextSCCEdge.end
                        }
                    if ((bbNextEdges?.size ?: 0) > 1) {
                        log.error("Found more than one EOG Edge matching criteria")
                    }
                    bbNextEdges?.forEach { bbNextEdge -> bbNextEdge.scc = level }
                }
            }
        }

        // Find nested loops: strip the last loop-entry element (by source order, hoping it's the
        // outermost entry) and re-decompose the remaining elements one level deeper - if that
        // still contains a loop, it's a nested one.
        if (loopEntryElements.isNotEmpty() && loopExitElements.isNotEmpty()) {
            val blackList = currentInfo.blackList.toMutableList()
            val innerLevel = level + 1
            val eliminatedElement =
                loopEntryElements.sortedBy { it.location?.region?.startLine }.last()
            blackList.add(eliminatedElement)
            sccElements.remove(eliminatedElement)
            tarjanInfoMap.computeIfAbsent(innerLevel) { TarjanInfo(blackList) }
            workStack.addLast(DecompDriver(sccElements.iterator(), innerLevel))
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
