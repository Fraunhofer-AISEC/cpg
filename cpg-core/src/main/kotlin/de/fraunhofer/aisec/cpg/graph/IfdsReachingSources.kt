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
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edges.flows.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import java.util.Collections
import java.util.IdentityHashMap

/**
 * A demand-driven, context-sensitive, **field-insensitive** IFDS / tabulation "reaching-sources"
 * solver over the CPG graph. It is a *recursion-complete* alternative to [followXUntilHitNodes] for
 * the MAY / node-set regime.
 *
 * Starting from [this] node and traversing along the requested [direction], it returns the set of
 * *frontier* nodes satisfying [predicate]: the FIRST predicate-satisfying node reached along each
 * path (predicate nodes are treated as absorbing sinks, i.e. once a node satisfies the predicate we
 * record it and do **not** expand past it).
 *
 * ## Why IFDS
 * The classic engine ([followXUntilHitNodes]) uses a visit-once BFS keyed on `(node, callStack)`
 * with a same-call-site recursion cut (see `contextExplosion`). On *recursive* interprocedural
 * graphs that cut makes it sound-but-INCOMPLETE (it can under-report reachable sources). This
 * solver instead performs single-fact IFDS tabulation with **summary edges**: once a callee
 * (entered at a call site) reaches a relevant exit, the result is recorded as a summary and reused
 * instead of re-entering the callee. Summary reuse bounds recursion and yields completeness +
 * termination in polynomial time *without* an unbounded call stack.
 *
 * ## Single fact = balanced-parenthesis (Dyck) reachability
 * Our IFDS instance is the degenerate single-fact one ("reaches a source"), so it collapses to
 * context-sensitive graph reachability where call (push) and return (pop) steps must be BALANCED: a
 * return may only pop back to the matching call site.
 *
 * ## k-limited call strings ([k], Sharir–Pnueli)
 * * `k = Int.MAX_VALUE` (default): full context sensitivity — a return goes only to the matching
 *   call site.
 * * `k = 0`: IGNORE the pushdown entirely = context-INSENSITIVE. A return may go to ANY call site,
 *   producing an OVER-approximation / SUPERSET. This is implemented by dropping the token match on
 *   returns (NOT by dropping interprocedural edges). Monotone: `result(0) ⊇ result(1) ⊇ … ⊇
 *   result(∞)`.
 * * `0 < k < Int.MAX_VALUE`: folds to full sensitivity by design. CPG call/return tokens are
 *   single-frame and the solver's context is the callee-entry node (a *summary*-based tabulation),
 *   which already yields full context sensitivity in polynomial time — strictly more precise than
 *   finite-k call-string limiting. Only the two endpoints k=0 and k=∞ are therefore distinct; the
 *   parameter is threaded through the token match so the ordering stays monotone (see
 *   [tokensMatch]). Genuine tunable k-CFL would require call-string-valued contexts and is
 *   intentionally not implemented (summaries dominate it on precision).
 *
 * ## Call/return derivation (differs by graph)
 * The recognizer is pluggable by [GraphToFollow], mirroring [Forward]/[Backward].
 * * **DFG**: context lives in edge labels — [ContextSensitiveDataflow] carries a [CallingContextIn]
 *   or [CallingContextOut] with a `calls` list; matched by object identity. For [Backward] the
 *   roles swap: [CallingContextOut] = push, [CallingContextIn] = pop.
 * * **EOG**: no calling-context labels exist; context is via `Invoke` edges (Call→Function) plus
 *   node kinds. Push: at a [Call] with invokes, enter the invoked [Function] (token = the [Call]).
 *   Pop: at a [Function] (Backward) or [Return]/EOG-leaf (Forward), return through the invoking
 *   [Call], resuming at the call site's intraprocedural neighbours.
 *
 * @param direction Which sub-graph ([GraphToFollow]) and which direction to follow.
 * @param k Call-string k-limiting bound (see above). Defaults to full context sensitivity.
 * @param predicate Identifies the target/source nodes (absorbing sinks).
 */
fun Node.ifdsReachingSources(
    direction: AnalysisDirection,
    k: Int = Int.MAX_VALUE,
    predicate: (Node) -> Boolean,
): Set<Node> {
    // Opt-in cross-query acceleration: if a pass installed an [IfdsSummaryCache] on this node's
    // [Component] (see [Component.ifdsSummaryCache]), reuse its predicate-independent pure
    // summaries.
    // When absent (the default, e.g. in tests or non-tagging queries) the solver behaves exactly as
    // the uncached tabulation. See [IfdsSummaryCache] for the soundness contract.
    val cache = this.component?.ifdsSummaryCache?.takeIf { it.direction.sameShapeAs(direction) }
    return IfdsReachingSourcesSolver(this, direction, k, predicate, cache).solve()
}

/** DFG-flavoured convenience entry point for [ifdsReachingSources]. Defaults to [Backward] DFG. */
fun Node.ifdsReachingSourcesDFG(
    direction: AnalysisDirection = Backward(GraphToFollow.DFG),
    k: Int = Int.MAX_VALUE,
    predicate: (Node) -> Boolean,
): Set<Node> = ifdsReachingSources(direction, k, predicate)

/** EOG-flavoured convenience entry point for [ifdsReachingSources]. Defaults to [Backward] EOG. */
fun Node.ifdsReachingSourcesEOG(
    direction: AnalysisDirection = Backward(GraphToFollow.EOG),
    k: Int = Int.MAX_VALUE,
    predicate: (Node) -> Boolean,
): Set<Node> = ifdsReachingSources(direction, k, predicate)

/** A single interprocedural step produced by the successor oracle. */
internal sealed interface IfdsStep

/** An intraprocedural move to [target] (no push/pop). */
internal class IntraStep(val target: Node) : IfdsStep

/**
 * A call (push) step: from [callSite] we enter a callee whose entry node is [calleeEntry], pushing
 * [token] (the call site(s), matched by identity) onto the conceptual call stack.
 */
internal class CallStep(val callSite: Node, val calleeEntry: Node, val token: List<Call>) :
    IfdsStep

/**
 * A return (pop) step: we leave the current procedure by popping [token]; the caller resumes at
 * each node in [targets].
 */
internal class ReturnStep(val token: List<Call>, val targets: List<Node>) : IfdsStep

/**
 * Identity-based key over a call-string token (a list of [Call]s). Two tokens are equal iff they
 * reference the same [Call] objects in the same order (object identity), so structurally-equal but
 * distinct call sites are never conflated.
 */
internal class TokenKey(val calls: List<Call>) {
    override fun equals(other: Any?): Boolean {
        if (other !is TokenKey) return false
        if (calls.size != other.calls.size) return false
        for (i in calls.indices) if (calls[i] !== other.calls[i]) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        for (call in calls) result = 31 * result + System.identityHashCode(call)
        return result
    }
}

/** A tabulated summary of a callee: popping [token] out of the callee resumes at [target]. */
internal class SummaryRecord(val token: TokenKey, val target: Node) {
    override fun equals(other: Any?): Boolean {
        return other is SummaryRecord && token == other.token && target === other.target
    }

    override fun hashCode(): Int = 31 * token.hashCode() + System.identityHashCode(target)
}

/**
 * A caller waiting on a callee's summary: the caller lived in context [callerCtx] (its own entry
 * node, or `null` for the empty-stack ROOT region), pushed at [callSite] with [token].
 */
internal class CallerRecord(val callerCtx: Node?, val callSite: Node, val token: List<Call>)

/**
 * A tabulated path edge: node [node] is reachable within the procedure context identified by [ctx]
 * (its entry node, or `null` for the empty-stack ROOT region). Equality is by object identity of
 * both components.
 */
internal class PathEdge(val ctx: Node?, val node: Node) {
    override fun equals(other: Any?): Boolean {
        return other is PathEdge && ctx === other.ctx && node === other.node
    }

    override fun hashCode(): Int = 31 * System.identityHashCode(ctx) + System.identityHashCode(node)
}

/** The actual tabulation. See [ifdsReachingSources] for the high-level contract. */
private class IfdsReachingSourcesSolver(
    val start: Node,
    val direction: AnalysisDirection,
    val k: Int,
    val predicate: (Node) -> Boolean,
    /**
     * Optional cross-query cache of predicate-independent pure summaries; see [IfdsSummaryCache].
     */
    val cache: IfdsSummaryCache? = null,
) {
    // A single solver instance analyzes exactly one AnalysisDirection: mixing forward and backward
    // steps within the same query (or genuinely bidirectional analysis) is not supported and would
    // require running two separate solves. No current caller needs this.
    private val backward = direction is Backward
    private val graph = direction.graphToFollow

    /** Frontier hits, deduplicated by object identity. */
    private val hits: MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap())

    /** Path edges already discovered (visit-once), keyed by [PathEdge] identity semantics. */
    private val visited = HashSet<PathEdge>()
    private val worklist = ArrayDeque<PathEdge>()

    /** Callers waiting on a callee, keyed by the callee's entry node (identity). */
    private val callers: MutableMap<Node, MutableList<CallerRecord>> = IdentityHashMap()

    /** Summaries of a callee, keyed by the callee's entry node (identity). */
    private val summaries: MutableMap<Node, MutableSet<SummaryRecord>> = IdentityHashMap()

    fun solve(): Set<Node> {
        if (predicate(start)) {
            hits.add(start)
            return hits
        }
        addPathEdge(null, start)
        while (worklist.isNotEmpty()) {
            val pe = worklist.removeFirst()
            expand(pe.ctx, pe.node)
        }
        return hits
    }

    private fun addPathEdge(ctx: Node?, node: Node) {
        if (predicate(node)) {
            // Frontier pruning: record the hit and do NOT expand past it.
            hits.add(node)
            return
        }
        val pe = PathEdge(ctx, node)
        if (visited.add(pe)) worklist.addLast(pe)
    }

    private fun expand(ctx: Node?, node: Node) {
        for (step in stepsOf(node)) {
            when (step) {
                is IntraStep -> addPathEdge(ctx, step.target)
                is CallStep -> handleCall(ctx, step)
                is ReturnStep -> handleReturn(ctx, step)
            }
        }
    }

    private fun handleCall(ctx: Node?, step: CallStep) {
        val calleeEntry = step.calleeEntry
        // Fast path: a "clean" callee (its transitive callee-subgraph contains no sink for any
        // query, so [predicate] can never fire inside it) has a predicate-independent pure summary.
        // We replay that summary and SKIP descending into the callee entirely: no frontier hit can
        // be lost (there is none inside), and the pure pass-through summaries capture every caller
        // resume target. This is where the cross-query speedup comes from. Only valid at full
        // context sensitivity (the pure summaries are tabulated at k=inf); k=0 queries (which never
        // carry a cache in practice) fall through to the exact slow path.
        if (cache != null && k == Int.MAX_VALUE && !cache.isDirty(calleeEntry)) {
            cache.replayCleanCallee(calleeEntry, step.token, k) { target ->
                addPathEdge(ctx, target)
            }
            return
        }
        // Slow path: a "dirty" callee may contain a sink, so we must tabulate it WITH the predicate
        // (frontier pruning inside the callee correctly shadows everything reachable only past a
        // sink). Nested clean callees encountered during this descent still take the fast path.
        val cr = CallerRecord(ctx, step.callSite, step.token)
        callers.getOrPut(calleeEntry) { mutableListOf() }.add(cr)
        // Seed the callee's own context (shared across all call sites of the same callee).
        addPathEdge(calleeEntry, calleeEntry)
        // Reuse any summary already computed for this callee.
        summaries[calleeEntry]?.toList()?.forEach { sum ->
            if (ifdsTokensMatch(cr.token, sum.token.calls, k)) {
                addPathEdge(cr.callerCtx, sum.target)
            }
        }
    }

    private fun handleReturn(ctx: Node?, step: ReturnStep) {
        if (ctx == null) {
            // ROOT / empty-stack region: a return here is the engine's permissive empty-stack pop,
            // i.e. it may reach ANY caller. Stay in the ROOT region.
            for (t in step.targets) addPathEdge(null, t)
            return
        }
        // Callee context: record summaries and resume matching callers.
        val tokenKey = TokenKey(step.token)
        for (t in step.targets) {
            val sr = SummaryRecord(tokenKey, t)
            if (summaries.getOrPut(ctx) { HashSet() }.add(sr)) {
                callers[ctx]?.toList()?.forEach { cr ->
                    if (ifdsTokensMatch(cr.token, step.token, k)) {
                        addPathEdge(cr.callerCtx, t)
                    }
                }
            }
        }
    }

    /** The successor oracle: typed interprocedural steps out of [node], per graph and direction. */
    private fun stepsOf(node: Node): List<IfdsStep> = ifdsStepsOf(node, backward, graph)
}

/**
 * The successor oracle, shared by the query-time [IfdsReachingSourcesSolver] and the predicate-free
 * pure tabulation in [IfdsSummaryCache]. Produces the typed interprocedural steps out of [node] for
 * the given traversal direction ([backward]) and sub-[graph]. It depends only on the graph
 * structure, never on a predicate, which is exactly why the resulting summaries can be cached.
 */
internal fun ifdsStepsOf(node: Node, backward: Boolean, graph: GraphToFollow): List<IfdsStep> {
    return when (graph) {
        GraphToFollow.DFG -> dfgSteps(node, backward)
        GraphToFollow.EOG -> eogSteps(node, backward)
    }
}

private fun dfgSteps(node: Node, backward: Boolean): List<IfdsStep> {
    val edges = if (backward) node.prevDFGEdges else node.nextDFGEdges
    return edges.map { edge ->
        val target = if (backward) edge.start else edge.end
        if (edge is ContextSensitiveDataflow) {
            val cc = edge.callingContext
            val isPush = if (backward) cc is CallingContextOut else cc is CallingContextIn
            val isPop = if (backward) cc is CallingContextIn else cc is CallingContextOut
            when {
                isPush -> CallStep(node, target, cc.calls.toList())
                isPop -> ReturnStep(cc.calls.toList(), listOf(target))
                else -> IntraStep(target)
            }
        } else {
            IntraStep(target)
        }
    }
}

private fun eogSteps(node: Node, backward: Boolean): List<IfdsStep> {
    return if (backward) eogStepsBackward(node) else eogStepsForward(node)
}

/**
 * Backward EOG successor oracle: a [Call] with invokes always pushes into the callee; otherwise a
 * [Function] node is treated as the callee's boundary and always pops back to its callers; any
 * other node just walks its own [Node.prevEOGEdges].
 */
private fun eogStepsBackward(node: Node): List<IfdsStep> {
    if (node is Call && node.invokes.isNotEmpty()) {
        val steps = node.invokeEdges.map { inv -> CallStep(node, inv.end, listOf(node)) }
        if (steps.isNotEmpty()) return steps
    } else if (node is Function) {
        val steps =
            node.calledByEdges.mapNotNull { inv ->
                val call = inv.start as? Call ?: return@mapNotNull null
                ReturnStep(listOf(call), call.prevEOG.toList())
            }
        if (steps.isNotEmpty()) return steps
    }
    return node.prevEOGEdges.map { IntraStep(it.start) }
}

/**
 * Forward EOG successor oracle: a [Call] with invokes always pushes into the callee; a [Return] or
 * a leaf node (no further [Node.nextEOG]) pops back through every call site invoking its enclosing
 * [Function]; any other node just walks its own [Node.nextEOGEdges].
 */
private fun eogStepsForward(node: Node): List<IfdsStep> {
    if (node is Call && node.invokes.isNotEmpty()) {
        val steps = node.invokeEdges.map { inv -> CallStep(node, inv.end, listOf(node)) }
        if (steps.isNotEmpty()) return steps
    } else if (node is Return || node.nextEOG.isEmpty()) {
        val fn = (node as? Function) ?: node.firstParentOrNull<Function>()
        val steps =
            fn?.calledByEdges?.mapNotNull { inv ->
                val call = inv.start as? Call ?: return@mapNotNull null
                ReturnStep(listOf(call), call.nextEOG.toList())
            } ?: emptyList()
        if (steps.isNotEmpty()) return steps
    }
    return node.nextEOGEdges.map { IntraStep(it.end) }
}

/**
 * Balanced-matching predicate under k-limiting. `k <= 0` ignores the pushdown (context insensitive,
 * superset). Otherwise the (single-frame) tokens must reference the same call site(s) by identity.
 * Shared by the query solver and the pure summary cache.
 */
internal fun ifdsTokensMatch(callerToken: List<Call>, returnToken: List<Call>, k: Int): Boolean {
    if (k <= 0) return true
    if (callerToken.size != returnToken.size) return false
    // Compare the top min(k, size) frames by identity. Tokens are single-frame in practice, so any
    // k >= 1 is a full match; k == 0 is handled above.
    val compare = minOf(k, callerToken.size)
    for (i in 0 until compare) {
        if (callerToken[i] !== returnToken[i]) return false
    }
    return true
}

/**
 * True iff two directions select the same traversal (same Forward/Backward class and sub-graph).
 */
private fun AnalysisDirection.sameShapeAs(other: AnalysisDirection): Boolean =
    this::class == other::class && this.graphToFollow == other.graphToFollow
