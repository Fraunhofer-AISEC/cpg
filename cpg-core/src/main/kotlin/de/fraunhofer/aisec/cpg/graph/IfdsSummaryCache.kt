/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import java.util.Collections
import java.util.IdentityHashMap

/**
 * A per-pass, cross-query cache for [ifdsReachingSources]. It exploits two facts that hold while a
 * pass runs:
 * 1. The DFG/EOG **graph is immutable** during the pass, so *predicate-free* balanced callee
 *    summaries ("enter this callee, pop this token, resume at this node") are stable graph facts
 *    that can be tabulated once and reused by every query.
 * 2. The only thing a query's [predicate] changes is where the frontier stops. A callee whose
 *    transitive callee-subgraph contains **no sink** can therefore never trigger frontier pruning,
 *    so its pure summary is exactly what any query would compute — we replay it and skip the
 *    callee.
 *
 * A callee is **dirty** if its function (transitively, up the call graph) contains a sink. Sinks
 * are registered by the owning pass via [markSink] as overlays / state entries are added to nodes.
 * Dirtiness is monotonic (sinks are only ever added) and propagated to all callers, so once a
 * callee is dirty it stays dirty and the solver falls back to exact, predicate-aware tabulation for
 * it — preserving results bit-for-bit.
 *
 * ## Soundness contract
 * The cache is only sound for queries whose sink set is a subset of the nodes reported through
 * [markSink]. Callers guarantee this by installing the cache on a [Component] (see
 * [Component.ifdsSummaryCache]) **only** for the duration of a pass whose queries have exactly that
 * shape, and by [markSink]-ing every node that could satisfy such a query's predicate (including a
 * one-time scan of pre-existing overlays). Any query that finds no cache runs the exact uncached
 * tabulation.
 *
 * Instances are used from a single pass that executes its queries sequentially; no internal locking
 * is required.
 *
 * @param direction The traversal this cache's pure summaries were tabulated for. A query with a
 *   different [AnalysisDirection] shape must not reuse them (enforced in [ifdsReachingSources]).
 */
class IfdsSummaryCache(val direction: AnalysisDirection) {
    private val backward = direction is Backward
    private val graph = direction.graphToFollow

    // ---- predicate-free ("pure") tabulation state, shared/incremental across all queries ----
    private val pureSummaries: MutableMap<Node, MutableSet<SummaryRecord>> = IdentityHashMap()
    private val pureCallers: MutableMap<Node, MutableList<CallerRecord>> = IdentityHashMap()
    private val pureVisited = HashSet<PathEdge>()
    private val pureWorklist = ArrayDeque<PathEdge>()

    /** Callee entries whose pure summary set is fully tabulated (complete). */
    private val pureComplete: MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap())

    /** Callee entries seeded during the in-progress drain; all become complete once it finishes. */
    private val seededThisDrain: MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap())

    // ---- dirtiness ----
    private val dirty: MutableSet<Function> = Collections.newSetFromMap(IdentityHashMap())
    private val functionOfCache = IdentityHashMap<Node, Function?>()

    /**
     * Replays the complete pure summary of the clean callee entered at [calleeEntry], invoking
     * [emit] with each caller-resume target whose token balances [callerToken] under [k]. Computes
     * the pure summary on first use.
     */
    internal fun replayCleanCallee(
        calleeEntry: Node,
        callerToken: List<Call>,
        k: Int,
        emit: (Node) -> Unit,
    ) {
        ensurePure(calleeEntry)
        pureSummaries[calleeEntry]?.toList()?.forEach { sum ->
            if (ifdsTokensMatch(callerToken, sum.token.calls, k)) emit(sum.target)
        }
    }

    /** Ensures [calleeEntry] has a complete pure summary set, tabulating it on demand. */
    private fun ensurePure(calleeEntry: Node) {
        if (calleeEntry in pureComplete) return
        seededThisDrain.clear()
        seedPure(calleeEntry, calleeEntry)
        seededThisDrain.add(calleeEntry)
        while (pureWorklist.isNotEmpty()) {
            val pe = pureWorklist.removeFirst()
            expandPure(pe.ctx, pe.node)
        }
        // A full drain makes every callee context it seeded complete (recursion is bounded by
        // summary reuse, exactly as in the query solver).
        pureComplete.addAll(seededThisDrain)
    }

    private fun seedPure(ctx: Node?, node: Node) {
        // No predicate: the pure tabulation treats no node as a sink.
        if (pureVisited.add(PathEdge(ctx, node))) pureWorklist.addLast(PathEdge(ctx, node))
    }

    /** Advances the predicate-free tabulation one step past [node] in context [ctx]. */
    private fun expandPure(ctx: Node?, node: Node) {
        for (step in ifdsStepsOf(node, backward, graph)) {
            when (step) {
                is IntraStep -> seedPure(ctx, step.target)
                is CallStep -> {
                    val callee = step.calleeEntry
                    seededThisDrain.add(callee)
                    pureCallers
                        .getOrPut(callee) { mutableListOf() }
                        .add(CallerRecord(ctx, step.callSite, step.token))
                    seedPure(callee, callee)
                    pureSummaries[callee]?.toList()?.forEach { sum ->
                        if (ifdsTokensMatch(step.token, sum.token.calls, Int.MAX_VALUE)) {
                            seedPure(ctx, sum.target)
                        }
                    }
                }
                is ReturnStep -> {
                    if (ctx == null) {
                        for (t in step.targets) seedPure(null, t)
                        continue
                    }
                    val tokenKey = TokenKey(step.token)
                    for (t in step.targets) {
                        val sr = SummaryRecord(tokenKey, t)
                        if (pureSummaries.getOrPut(ctx) { HashSet() }.add(sr)) {
                            pureCallers[ctx]?.toList()?.forEach { cr ->
                                if (ifdsTokensMatch(cr.token, step.token, Int.MAX_VALUE)) {
                                    seedPure(cr.callerCtx, t)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Registers that [node] is (now) a sink for the queries this cache serves, marking its function
     * — and, transitively, every caller — dirty so their pure summaries are no longer reused.
     */
    fun markSink(node: Node) {
        val f = functionOf(node) ?: return
        if (!dirty.add(f)) return
        val work = ArrayDeque<Function>()
        work.addLast(f)
        while (work.isNotEmpty()) {
            val g = work.removeFirst()
            for (call in g.calledBy) {
                val caller = call.firstParentOrNull<Function>() ?: continue
                if (dirty.add(caller)) work.addLast(caller)
            }
        }
    }

    /**
     * True if the callee entered at [calleeEntry] may contain a sink (so its pure summary must not
     * be reused). Unknown-function callees are treated as dirty (conservative).
     */
    fun isDirty(calleeEntry: Node): Boolean {
        val f = functionOf(calleeEntry) ?: return true
        return f in dirty
    }

    /**
     * The [Function] a node belongs to (via AST for program nodes, via underlying for overlays).
     */
    private fun functionOf(node: Node): Function? {
        if (functionOfCache.containsKey(node)) return functionOfCache[node]
        val f =
            when (node) {
                is Function -> node
                is OverlayNode ->
                    node.underlyingNode?.let { it as? Function ?: it.firstParentOrNull<Function>() }
                else -> node.firstParentOrNull<Function>()
            }
        functionOfCache[node] = f
        return f
    }
}
