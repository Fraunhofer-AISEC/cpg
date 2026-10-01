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
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.min

/** A [MutableSet] that tests membership by reference identity rather than [Any.equals]. */
private fun <T> identitySetOf(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap())

/**
 * This pass implements Tarjan's algorithm (the original
 * [paper](https://epubs.siam.org/doi/10.1137/0201010)) to find strongly connected components (SCCs)
 * in the Evaluation Order Graph (EOG) of a program. SCCs are subgraphs where every node is
 * reachable from every other node within the same subgraph (i.e., loops). In addition, we remove
 * the entry nodes of the SCC so that we can also detect nested loops. The pass labels the EOG edges
 * that are part of an SCC with the same identifier.
 *
 * Algorithm: [tarjan] runs the DFS with an explicit [WorkItem] stack instead of recursion, since
 * EOGs can be deeper than the JVM call stack allows. A [DfsFrame] is one DFS call frame - a node
 * plus its not-yet-visited successors - pushed on descent and popped on return, updating lowlink
 * values the usual Tarjan way. When a popped frame's node turns out to be an SCC root,
 * [handleSccRoot] labels that SCC's edges with the current [WorkItem.level], then - to find loops
 * nested inside it - strips all of the SCC's entry nodes at once (a loop can have more than one
 * distinct entry point) and re-decomposes the remaining elements one `level` deeper, using a
 * [DecompDriver] to drive that re-run. This repeats until no further nested loop remains, leaving
 * concentric loops labeled from outermost to innermost.
 *
 * Each [WorkItem] carries both a [WorkItem.level] (the nesting depth reported on labeled edges) and
 * a [WorkItem.mapKey] (which [TarjanInfo] scratch space it reads/writes). These are *not* the same
 * value: two unrelated SCCs can legitimately decompose to the same depth (e.g. two independent
 * sibling loops, each one level "deep") without being related at all, and giving them the same
 * scratch space would corrupt each other's blacklist/visited state - see [nextScratchKey].
 */
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(BasicBlockCollectorPass::class, softDependency = true)
@Description("Pass that finds strongly connected components in the EOG using Tarjan's algorithm.")
class SccPass(ctx: TranslationContext) : EOGStarterPass(ctx) {
    /**
     * `stack`/`blackList`/`visited`/`blockIDs`/`lowLinkValues` all key or test membership by [Node]
     * *identity* - never by [Node.equals], which for
     * [BasicBlock][de.fraunhofer.aisec.cpg.graph.overlays.BasicBlock] is a full structural
     * comparison (including a `location` recomputed from scratch on every call). [identitySetOf]
     * and [IdentityHashMap] keep every one of these O(1) regardless of how expensive `equals()` or
     * `hashCode()` happen to be for the node type involved. [stack] is the one exception that stays
     * an ordinary [MutableList]: its order is load-bearing (LIFO push/pop, `last()` as "top of
     * stack"), so [stackSet] mirrors its contents purely for O(1) `in` checks, kept in sync at
     * every push/pop site.
     */
    data class TarjanInfo(val blackList: Set<Node>) {
        var blockCounter = 0
        var stack = mutableListOf<Node>()
        var stackSet = identitySetOf<Node>()
        var visited = identitySetOf<Node>()
        var blockIDs: MutableMap<Node, Int> = IdentityHashMap()
        var lowLinkValues: MutableMap<Node, Int> = IdentityHashMap()
    }

    val tarjanInfoMap = mutableMapOf<Int, TarjanInfo>()

    /**
     * Generates a fresh, never-reused key into [tarjanInfoMap] for each nested-decomposition
     * attempt (see [handleSccRoot]). Deliberately disjoint from the positive `level`/depth values
     * (always < 0) so it can never collide with a real depth, and disjoint from every other
     * decomposition's key so two unrelated decompositions that happen to land at the same *depth*
     * (e.g. two independent sibling loops, each one level deep) never share [TarjanInfo].
     */
    private var nextScratchKey = -1

    /**
     * One item on [tarjan]'s explicit work-stack: [mapKey] identifies which [TarjanInfo] in
     * [tarjanInfoMap] this item's DFS run reads/writes (see [nextScratchKey] - *not* necessarily
     * the same as [level]), while [level] is purely the nesting depth reported on
     * [EvaluationOrder.scc][de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder.scc].
     */
    private sealed class WorkItem(val mapKey: Int, val level: Int)

    /**
     * One DFS call frame for [node]: pulls its not-yet-visited successors lazily from [iterator].
     */
    private class DfsFrame(val node: Node, mapKey: Int, level: Int, val iterator: Iterator<Node>) :
        WorkItem(mapKey, level)

    /**
     * Drives the nested-loop re-decomposition over `sccElements`, pushing a [DfsFrame] only for an
     * element that is still unvisited *at the moment its turn comes up* - visiting one element can
     * visit others in the same decomposition, so pushing frames for all of them up front would use
     * a stale visited-snapshot.
     */
    private class DecompDriver(val iterator: Iterator<Node>, mapKey: Int, level: Int) :
        WorkItem(mapKey, level)

    override fun cleanup() {
        // Nothing to clean up
    }

    private fun initNode(node: Node, info: TarjanInfo) {
        info.blockIDs[node] = info.blockCounter
        info.lowLinkValues[node] = info.blockCounter
        info.blockCounter++
        info.visited.add(node)
        // Pushed at the end, not the front: appending is O(1) amortized, whereas add(0, ...)
        // would shift every existing element on every single push.
        info.stack.add(node)
        info.stackSet.add(node)
    }

    /**
     * Runs Tarjan's algorithm from [bb], the root of a top-level (not nested) decomposition; see
     * the class doc for the algorithm. Always starts at depth 1 - nested loops found along the way
     * are decomposed one level deeper by [handleSccRoot] itself, within the same call, not by
     * calling [tarjan] again.
     */
    fun tarjan(bb: Node) {
        val level = 1
        val workStack = ArrayDeque<WorkItem>()
        val startInfo = tarjanInfoMap.computeIfAbsent(level) { TarjanInfo(emptySet()) }
        initNode(bb, startInfo)
        workStack.addLast(DfsFrame(bb, level, level, bb.nextEOG.iterator()))

        while (workStack.isNotEmpty()) {
            when (val item = workStack.last()) {
                is DfsFrame -> {
                    val info = tarjanInfoMap.computeIfAbsent(item.mapKey) { TarjanInfo(emptySet()) }
                    if (item.iterator.hasNext()) {
                        val next = item.iterator.next()
                        // To detect inner loops, we put some nodes on a blacklist and see if we
                        // can still find a loop
                        if (next in info.blackList) {
                            continue
                        }
                        if (next !in info.visited) {
                            initNode(next, info)
                            workStack.addLast(
                                DfsFrame(next, item.mapKey, item.level, next.nextEOG.iterator())
                            )
                        } else if (next in info.stackSet) {
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
                        // same decomposition run (same mapKey, i.e. same TarjanInfo - level alone
                        // is not a reliable enough identity check, since two unrelated
                        // decompositions can legitimately share the same depth). No-op if
                        // handleSccRoot just ran for v, since that always removes v from the
                        // stack.
                        val parent = workStack.lastOrNull()
                        if (
                            parent is DfsFrame && parent.mapKey == item.mapKey && v in info.stackSet
                        ) {
                            info.lowLinkValues[parent.node] =
                                min(
                                    info.lowLinkValues.getValue(parent.node),
                                    info.lowLinkValues.getValue(v),
                                )
                        }
                    }
                }
                is DecompDriver -> {
                    val innerInfo = tarjanInfoMap.getValue(item.mapKey)
                    if (item.iterator.hasNext()) {
                        val element = item.iterator.next()
                        if (element !in innerInfo.visited) {
                            initNode(element, innerInfo)
                            workStack.addLast(
                                DfsFrame(
                                    element,
                                    item.mapKey,
                                    item.level,
                                    element.nextEOG.iterator(),
                                )
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
        // Identity (===), not equals(): "is this the exact node we're looking for", never "an
        // equal-looking one" - also sidesteps BasicBlock's expensive structural equals().
        if (currentInfo.stack.last() === bb && bb.nextEOG.none { it === bb }) {
            currentInfo.stack.removeAt(currentInfo.stack.lastIndex)
            currentInfo.stackSet.remove(bb)
            return
        }

        log.trace("Found a SCC (Level $level): ")
        // Not necessarily all nodes on the stack - only the suffix pushed after bb (now the
        // topmost element still below them). Collect that suffix, then pop it in a single range
        // clear instead of one remove() per element.
        val bbIndex = currentInfo.stack.indexOfLast { it === bb }
        val sccElements = identitySetOf<Node>()
        for (i in currentInfo.stack.lastIndex downTo bbIndex) {
            val element = currentInfo.stack[i]
            currentInfo.lowLinkValues[element] = currentInfo.blockIDs.getValue(bb)
            if (log.isTraceEnabled) {
                log.trace("{} ({}); ", element.location, currentInfo.lowLinkValues[element])
            }
            sccElements.add(element)
            currentInfo.stackSet.remove(element)
        }
        currentInfo.stack.subList(bbIndex, currentInfo.stack.size).clear()
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

        // Find nested loops: strip all of this level's loop-entry elements at once and
        // re-decompose the remaining elements one level deeper - if that still contains a loop,
        // it's a nested one.
        //
        // All entries are stripped together, not one at a time: a loop can have more than one
        // distinct entry point (e.g. a `goto` into the middle of a loop, or a switch/labeled break
        // into a loop body - see testFullyConnectedLoopWithThreeEntriesStaysAtOneLevel). Removing
        // them one at a time and re-decomposing after each would cost O(k * n) for k entries, and
        // could relabel edges at a deeper level than the loop actually has, since the remaining
        // entries can still keep the same elements connected in a cycle. For a single-entry loop
        // (loopEntryElements.size == 1), stripping "all" is the same as stripping the one.
        //
        // Each decomposition attempt gets its own never-reused mapKey (see [nextScratchKey]) into
        // tarjanInfoMap, kept separate from level/depth: two unrelated SCCs can legitimately
        // decompose to the same depth (e.g. two independent sibling loops, each with a single
        // entry==exit node), and sharing a TarjanInfo between them would let one's
        // blackList/visited/stack silently corrupt the other's.
        if (loopEntryElements.isNotEmpty() && loopExitElements.isNotEmpty()) {
            val blackList = identitySetOf<Node>()
            blackList.addAll(currentInfo.blackList)
            val innerLevel = level + 1
            val innerMapKey = nextScratchKey--
            blackList.addAll(loopEntryElements)
            sccElements.removeAll(loopEntryElements)
            tarjanInfoMap[innerMapKey] = TarjanInfo(blackList)
            workStack.addLast(DecompDriver(sccElements.iterator(), innerMapKey, innerLevel))
        }
    }

    // Note: no need to guard against processing the same basic block twice - EOGStarterPass
    // creates a fresh SccPass instance per starter node and calls accept() on it exactly once
    // (see the top-level consumeTarget() in Pass.kt), so tarjanInfoMap is never shared across
    // multiple accept() calls in the first place.
    override fun accept(node: Node) {
        if (node.basicBlock.isEmpty()) return
        val bb = node.basicBlock.single() as BasicBlock
        tarjan(bb)
    }
}
