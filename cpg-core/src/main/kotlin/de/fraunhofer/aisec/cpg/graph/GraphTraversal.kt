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

import de.fraunhofer.aisec.cpg.assumptions.addAssumptionDependence
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.helpers.mapFilteredTo
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/**
 * Returns an instance of [FulfilledAndFailedPaths] where [FulfilledAndFailedPaths.fulfilled]
 * contains all possible paths (with [x] specifying how to fetch more nodes) between the starting
 * node [this] and the end node fulfilling [predicate]. The paths are represented as lists of nodes.
 * Paths which do not end at such a node are included in [FulfilledAndFailedPaths.failed].
 *
 * Hence, if "fulfilled" is a non-empty list, a path from [this] to such a node is **possible but
 * not mandatory**. If the list "failed" is empty, the path is mandatory.
 *
 * @param x A function that, given the current node, the current [Context], the current path and the
 *   list of already detected looping paths, returns the collection of next `(Node, Context)` pairs
 *   to be explored. This is where the actual graph-traversal logic lives (e.g. following DFG or EOG
 *   edges).
 * @param collectFailedPaths If `true` (the default), paths that reach a dead end without satisfying
 *   [predicate] – as well as paths stopped by [earlyTermination] – are collected in
 *   [FulfilledAndFailedPaths.failed]. Set to `false` to skip collecting failed paths for better
 *   performance when only fulfilled paths are of interest.
 * @param findAllPossiblePaths If `true` (the default), every possible path through the graph is
 *   explored, even if a node has already been visited via a different path. Set to `false` to visit
 *   each `(Node, Context)` pair only once, which is faster but may miss some paths.
 * @param continueAfterHit Only relevant for a **MAY** analysis (`findAllPossiblePaths == false`).
 *   If `true` (the default), the search continues after a hit and reports one (shortest) witness
 *   per distinct reachable target node. If `false`, the whole traversal stops as soon as the
 *   *first* target is reached and returns exactly that single (shortest) witness. This is a pure
 *   reachability query ("is any node satisfying [predicate] reachable?") and can be dramatically
 *   faster on large graphs, but must be used with care: [FulfilledAndFailedPaths.failed] is left
 *   empty in this mode, and only [FulfilledAndFailedPaths.fulfilled] is meaningful. The flag is
 *   ignored for a MUST analysis (`findAllPossiblePaths == true`), which must enumerate every path
 *   to decide inevitability.
 * @param ctx The initial [Context] for the traversal (index stack, call stack, step counter).
 *   Usually the default value suffices; supply a custom context e.g. when the analysis should start
 *   inside a specific call stack.
 * @param earlyTermination A predicate called on each *next* node and the current [Context] before
 *   the node is added to the worklist. If it returns `true`, the path is immediately recorded as
 *   failed with reason [FailureReason.HIT_EARLY_TERMINATION] and traversal of that branch stops.
 *   This is typically used to enforce analysis boundaries, for example to stop at the border of the
 *   current function: ```kotlin node.followDFGEdgesUntilHit( scope = Interprocedural(),
 *   earlyTermination = { nextNode, _ -> nextNode is FunctionDeclaration }, ) { it is Literal<*>
 *   } ```
 * @param predicate A predicate that marks the *target* of the path search. When a node satisfying
 *   [predicate] is reached, the current path is added to [FulfilledAndFailedPaths.fulfilled] and
 *   that branch of the traversal is stopped.
 */
fun Node.followXUntilHit(
    x:
        (
            Node, Context, List<Triple<Node, Edge<Node>?, Context>>, MutableSet<NodePath>,
        ) -> Collection<Triple<Node, Edge<Node>, Context>>,
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    continueAfterHit: Boolean = true,
    ctx: Context = Context(steps = 0),
    earlyTermination: (Node, Context) -> Boolean,
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths {
    // This traversal runs one of two regimes depending on [findAllPossiblePaths]. Both regimes use
    // the same decision for when a successor state must NOT be expanded ([contextExplosion] and,
    // for
    // MUST, [isNodeWithCallStackInPath]); they differ only in scheduling and bookkeeping:
    //
    //  * MAY  (findAllPossiblePaths == false): a visit-once breadth-first search over the precise
    //    (node, callStack, indexStack) state. BFS visits each reachable state at most once and
    //    reaches it via its shortest path first, so the shortest witness is found without ever
    //    diving into (possibly unbounded) recursion before shallower paths are exhausted. This is
    //    exactly what the MAY consumers need (`fulfilled.any` / `fulfilled.minByOrNull { size }`),
    //    and it is linear in the reachable state space and in memory: only parent pointers are
    //    kept, and full paths are rebuilt lazily just for the (few) recorded results.
    //
    //  * MUST (findAllPossiblePaths == true): a depth-first faithful enumeration of every maximal
    //    path, so that `failed` contains exactly the maximal paths that do not hit `predicate`
    //    (MUST/inevitability holds iff `failed` is empty). On an acyclic graph this enumerates
    //    every distinct path (preserving the exact fulfilled/failed counts the tests pin down);
    //    loops and interprocedural recursion are cut by the shared decision above.
    val fulfilledPaths = mutableListOf<NodePath>()
    // failedPaths: All the paths which do not satisfy "predicate"
    val failedPaths = mutableListOf<Pair<FailureReason, NodePath>>()
    val loopingPaths: MutableSet<NodePath> = ConcurrentHashMap.newKeySet()

    // First check if the current node satisfies the predicate.
    // If it does, we consider this path fulfilled and skip further traversal.
    if (predicate(this)) {
        fulfilledPaths.add(NodePath(mutableListOf(this), emptyList()).addAssumptionDependence(this))
        return FulfilledAndFailedPaths(fulfilledPaths, failedPaths)
    }

    if (!findAllPossiblePaths) {
        // ===== MAY regime: visit-once breadth-first search with parent pointers =====
        // For every discovered state we remember how we reached it (previous state + edge) and the
        // context/node we had there, so a witness can be rebuilt lazily. BFS (FIFO) guarantees the
        // first time we reach a state is via a shortest path.
        val parentOf = HashMap<TraversalStateKey, Pair<TraversalStateKey, Edge<Node>?>?>()
        val ctxOf = HashMap<TraversalStateKey, Context>()
        val nodeOf = HashMap<TraversalStateKey, Node>()
        val queue = ArrayDeque<TraversalStateKey>()
        // We only need one (shortest) witness per target node. Dedup by object identity so two
        // *distinct* target nodes that happen to be structurally equal (same name/location/class)
        // are both reported.
        val recordedHits = identitySetOf<Node>()
        val startKey = ctx.stateKey(this)
        parentOf[startKey] = null
        ctxOf[startKey] = ctx
        nodeOf[startKey] = this
        queue.addLast(startKey)

        // Rebuilds the path from the start back to [key] (inclusive) in forward order, optionally
        // extended by one final (node, edge, context) hop.
        fun witness(
            key: TraversalStateKey,
            extraNode: Node? = null,
            extraEdge: Edge<Node>? = null,
            extraCtx: Context? = null,
        ): NodePath {
            val nodes = ArrayDeque<Node>()
            val edges = ArrayDeque<Edge<Node>>()
            val contexts = ArrayDeque<Context>()
            var k: TraversalStateKey? = key
            while (k != null) {
                nodes.addFirst(nodeOf.getValue(k))
                contexts.addFirst(ctxOf.getValue(k))
                val parent = parentOf[k]
                parent?.second?.let { edges.addFirst(it) }
                k = parent?.first
            }
            extraNode?.let { nodes.addLast(it) }
            extraEdge?.let { edges.addLast(it) }
            extraCtx?.let { contexts.addLast(it) }
            return NodePath(nodes.toList(), edges.toList())
                .addAssumptionDependence(contexts.toList())
        }

        while (queue.isNotEmpty()) {
            if (parentOf.size > MAX_VISITED_STATES) {
                // Pathological state-space blow-up: bail out with the hits collected so far rather
                // than exhaust the heap. A MAY result is allowed to be incomplete.
                followXLog.warn(
                    "MAY traversal from {} reached the {}-state backstop; returning the {} hit(s) " +
                        "found so far. The result may be incomplete.",
                    this,
                    MAX_VISITED_STATES,
                    recordedHits.size,
                )
                break
            }
            val currentKey = queue.removeFirst()
            val currentNode = nodeOf.getValue(currentKey)
            val currentContext = ctxOf.getValue(currentKey)
            // `x`/`followEdge` only reads the path to build looping-path records, which the MAY
            // regime does not consume, so a light single-element path suffices and keeps this O(1).
            val lightPath =
                listOf(Triple<Node, Edge<Node>?, Context>(currentNode, null, currentContext))
            val nextNodes = x(currentNode, currentContext, lightPath, loopingPaths)

            if (nextNodes.isEmpty() && collectFailedPaths) {
                failedPaths.add(FailureReason.PATH_ENDED to witness(currentKey))
                continue
            }

            for ((nextNode, edge, newContext) in nextNodes) {
                if (predicate(nextNode)) {
                    // Only keep the first (shortest, thanks to BFS) witness per target node.
                    if (recordedHits.add(nextNode)) {
                        fulfilledPaths.add(witness(currentKey, nextNode, edge, newContext))
                        if (!continueAfterHit) {
                            // Reachability mode: a single reachable target answers the query, so
                            // stop the whole BFS immediately. `failed` is intentionally left empty
                            // here (see the [continueAfterHit] doc); callers that opt into early
                            // termination only consume `fulfilled`.
                            return FulfilledAndFailedPaths(fulfilledPaths, emptyList())
                        }
                    }
                    continue
                }
                if (earlyTermination(nextNode, currentContext)) {
                    if (collectFailedPaths) {
                        failedPaths.add(
                            FailureReason.HIT_EARLY_TERMINATION to
                                witness(currentKey, nextNode, edge, newContext)
                        )
                    }
                    continue
                }
                if (contextExplosion(newContext)) {
                    // Recursion / stack explosion: cut this branch.
                    loopingPaths.add(witness(currentKey, nextNode, edge, newContext))
                    continue
                }
                // `stateKey` ignores `steps`, so we can compute it before incrementing and only
                // advance the step counter for a genuinely new state we are about to enqueue.
                val nextKey = newContext.stateKey(nextNode)
                if (nextKey !in parentOf) {
                    newContext.inc()
                    parentOf[nextKey] = currentKey to edge
                    ctxOf[nextKey] = newContext
                    nodeOf[nextKey] = nextNode
                    queue.addLast(nextKey)
                }
            }
        }
    } else {
        // ===== MUST regime: depth-first faithful path enumeration =====
        // A LIFO worklist (stack) gives depth-first order with O(1) push/pop; the enumeration is
        // exhaustive, so the order does not affect the result, only memory (frontier-sized).
        val worklist = ArrayDeque<List<Triple<Node, Edge<Node>?, Context>>>()
        worklist.addLast(listOf(Triple(this, null, ctx))) // We start only with the "from" node.

        while (worklist.isNotEmpty()) {
            val currentPath = worklist.removeLast()
            val currentNode = currentPath.last().first
            val currentContext = currentPath.last().third
            val currentPathNodes = currentPath.map { it.first }
            val currentPathEdges = currentPath.mapNotNull { it.second }
            val nextNodes = x(currentNode, currentContext, currentPath, loopingPaths)

            // No further nodes in the path and the path criteria are not satisfied.
            if (nextNodes.isEmpty() && collectFailedPaths) {
                // TODO: How to determine if this path is really at the end or if it exceeded the
                // number of steps?
                failedPaths.add(
                    FailureReason.PATH_ENDED to
                        NodePath(currentPathNodes, currentPathEdges)
                            .addAssumptionDependence(currentPath.map { it.third })
                )
            }

            for ((nextNode, edge, newContext) in nextNodes) {
                if (predicate(nextNode)) {
                    // We ended up in the node fulfilling "predicate", so we're done for this path.
                    fulfilledPaths.add(
                        NodePath(currentPathNodes + nextNode, currentPathEdges + edge)
                            .addAssumptionDependence(currentPath.map { it.third } + newContext)
                    )
                    continue // Don't add this path anymore. The requirement is satisfied.
                }
                if (earlyTermination(nextNode, currentContext)) {
                    failedPaths.add(
                        FailureReason.HIT_EARLY_TERMINATION to
                            NodePath(currentPathNodes + nextNode, currentPathEdges + edge)
                                .addAssumptionDependence(currentPath.map { it.third } + newContext)
                    )
                    continue // Don't add this path anymore. We already failed.
                }
                // Extend the path unless continuing would loop or blow up the stacks.
                // `isNodeWithCallStackInPath` catches loops where the (node, callStack) state
                // repeats on the current path (intraprocedural loops and recursion via the same
                // call site). `contextExplosion` additionally catches interprocedural recursion
                // whose call stack keeps growing so that no (node, callStack) state ever repeats,
                // and unbounded index/call-stack growth in general.
                if (
                    !isNodeWithCallStackInPath(nextNode, newContext, currentPath) &&
                        !contextExplosion(newContext)
                ) {
                    worklist.addLast(currentPath + Triple(nextNode, edge, newContext.inc()))
                } else {
                    // There's a loop.
                    loopingPaths.add(
                        NodePath(currentPathNodes + nextNode, currentPathEdges + edge)
                            .addAssumptionDependence(currentPath.map { it.third } + newContext)
                    )
                }
            }
        }
    }

    val failedLoops =
        loopingPaths.mapFilteredTo(
            mutableSetOf(),
            { path ->
                fulfilledPaths.none {
                    it.nodes.size > path.nodes.size &&
                        it.nodes.subList(0, path.nodes.size - 1) == path.nodes
                } &&
                    failedPaths.none {
                        it.second.nodes.size > path.nodes.size &&
                            it.second.nodes.subList(0, path.nodes.size - 1) == path.nodes
                    }
            },
        ) {
            FailureReason.PATH_ENDED to it
        }

    return FulfilledAndFailedPaths(
        fulfilledPaths,
        (failedPaths + failedLoops).toSet().map { Pair(it.first, it.second) },
    )
}

/**
 * A **path-free** variant of the MAY branch of [followXUntilHit]. Instead of rebuilding a witness
 * [NodePath] for every hit, it returns only the *set* of predicate-satisfying frontier nodes
 * reachable from [this] without passing through an earlier match.
 *
 * This reproduces the `findAllPossiblePaths = false`, `continueAfterHit = true` regime of
 * [followXUntilHit] exactly:
 * * visit-once dedup over the precise `(node, callStack)` MAY state (via [TraversalStateKey] /
 *   [Context.stateKey] – the key deliberately excludes the index stack),
 * * AT-hit pruning: a node satisfying [predicate] is recorded and *not* expanded past (frontier
 *   semantics),
 * * all frontier hits are collected (equivalent to `continueAfterHit = true`),
 * * the same [earlyTermination], [contextExplosion] and [MAX_VISITED_STATES] safety bounds.
 *
 * It skips all parent/edge/context witness bookkeeping: the worklist carries the `(node, context)`
 * pair directly. This exists for the very common caller pattern
 * `followDFGEdgesUntilHit(findAllPossiblePaths = false, ...).fulfilled.map { it.nodes.last() }`,
 * which throws the reconstructed paths away and keeps only the endpoint nodes.
 *
 * The returned set is deduplicated by object identity (like [followXUntilHit]'s `recordedHits`), so
 * two *distinct* target nodes that happen to be structurally equal are both reported, while a node
 * reachable via several paths (e.g. through a diamond) is reported exactly once.
 *
 * @param x The next-step callback, identical to the one taken by [followXUntilHit].
 * @param ctx The initial [Context] (index stack, call stack, step counter).
 * @param earlyTermination A predicate called on each *next* candidate node and the current
 *   [Context]. If it returns `true`, that branch is abandoned. Unlike [followXUntilHit] this
 *   variant never collects failed paths, so no record is kept.
 * @param predicate A predicate that identifies the target node(s). A node satisfying it is added to
 *   the result set and its successors are not explored.
 */
fun Node.followXUntilHitNodes(
    x:
        (
            Node, Context, List<Triple<Node, Edge<Node>?, Context>>, MutableSet<NodePath>,
        ) -> Collection<Triple<Node, Edge<Node>, Context>>,
    ctx: Context = Context(steps = 0),
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): Set<Node> {
    // Dedup reported hits by object identity, exactly like [followXUntilHit]'s `recordedHits`.
    val hits = identitySetOf<Node>()

    // If the start node already satisfies the predicate, it is the only frontier node.
    if (predicate(this)) {
        hits.add(this)
        return hits
    }

    // Visit-once dedup over the precise `(node, callStack)` MAY state (see [TraversalStateKey]).
    val visited = HashSet<TraversalStateKey>()
    // The worklist carries the `(node, context)` pair directly: no parent/edge/witness maps needed.
    val queue = ArrayDeque<Pair<Node, Context>>()
    // `x` may append looping records to this set, but the MAY regime never consumes them, so a
    // throwaway sink keeps the callback happy without any bookkeeping cost.
    val loopingSink: MutableSet<NodePath> = mutableSetOf()

    visited.add(ctx.stateKey(this))
    queue.addLast(this to ctx)

    while (queue.isNotEmpty()) {
        if (visited.size > MAX_VISITED_STATES) {
            // Pathological state-space blow-up: bail out with the hits found so far rather than
            // exhaust the heap. A MAY result is allowed to be incomplete.
            followXLog.warn(
                "MAY node traversal from {} reached the {}-state backstop; returning the {} hit(s) " +
                    "found so far. The result may be incomplete.",
                this,
                MAX_VISITED_STATES,
                hits.size,
            )
            break
        }
        val (currentNode, currentContext) = queue.removeFirst()
        // A light single-element path suffices: `x` only reads it to build looping records, which
        // we discard (see [loopingSink]).
        val lightPath =
            listOf(Triple<Node, Edge<Node>?, Context>(currentNode, null, currentContext))
        val nextNodes = x(currentNode, currentContext, lightPath, loopingSink)

        for ((nextNode, _, newContext) in nextNodes) {
            if (predicate(nextNode)) {
                // Frontier pruning: record the hit and do NOT expand past it.
                hits.add(nextNode)
                continue
            }
            if (earlyTermination(nextNode, currentContext)) {
                continue
            }
            if (contextExplosion(newContext)) {
                // Recursion / stack explosion: cut this branch.
                continue
            }
            // `stateKey` ignores `steps`, so compute it before incrementing and only advance the
            // step counter for a genuinely new state we are about to enqueue.
            val nextKey = newContext.stateKey(nextNode)
            if (visited.add(nextKey)) {
                newContext.inc()
                queue.addLast(nextNode to newContext)
            }
        }
    }

    return hits
}

/**
 * Shared default no-op [earlyTermination] used by the node-set MAY wrappers. Hoisting it to a named
 * value (instead of an inline lambda per call site) lets those wrappers detect that the caller did
 * NOT supply a custom early-termination predicate via reference identity (`===
 * noEarlyTermination`), which is a precondition for delegating to the field-insensitive
 * [ifdsReachingSources] solver.
 */
internal val noEarlyTermination: (Node, Context) -> Boolean = { _, _ -> false }

/**
 * Hard backstops that bound the traversal state space even when the graph contains recursion whose
 * call stack never repeats a state (e.g. every recursion level goes through a *fresh* call site) or
 * an unbounded chain of indexed data flows. Real programs stay far below these; they exist only to
 * guarantee termination in pathological cases. The primary, precise recursion cut is the
 * same-call-site detection in [contextExplosion] and the on-path (node, callStack) repeat detection
 * in [isNodeWithCallStackInPath].
 */
private const val MAX_CALL_STACK_BACKSTOP = 1000

private const val MAX_INDEX_STACK_BACKSTOP = 1000

/**
 * Hard cap on the number of distinct MAY states (`(node, callStack)` keys) the visit-once search
 * will retain. A MAY search keeps parent/context/node bookkeeping for every discovered state for
 * the whole traversal (to rebuild witness paths), so an unexpectedly huge state space would
 * otherwise exhaust the heap. When this cap is reached the search stops expanding and returns the
 * hits found so far: a MAY result is allowed to be incomplete, and a bounded-but-incomplete answer
 * is strictly better than an [OutOfMemoryError]. This is a backstop for pathological graphs; the
 * primary bound is the `(node, callStack)` dedup itself.
 */
private const val MAX_VISITED_STATES = 1_000_000

/** Logger for [followXUntilHit] diagnostics (e.g. the [MAX_VISITED_STATES] backstop). */
private val followXLog = LoggerFactory.getLogger("de.fraunhofer.aisec.cpg.graph.FollowXUntilHit")

/**
 * An immutable traversal state used by the MAY search to visit each state at most once.
 *
 * The key is deliberately `(node, callStack)` only and does **not** include the index stack: on a
 * field-sensitive interprocedural analysis many distinct index-stack contents reach the same
 * `(node, callStack)`, and partitioning the visited set by the full index stack makes it explode
 * combinatorially (and, on large real programs, exhausts the heap). Dropping the index stack from
 * the key restores the historically bounded MAY behaviour: each `(node, callStack)` is expanded
 * once, keeping the first-arriving (shortest) index-stack context. This can merge states that a
 * fully index-sensitive dedup would keep apart, but a MAY analysis is explicitly allowed to be
 * incomplete, and the memory bound matters more than that extra precision here.
 */
private data class TraversalStateKey(val node: Node, val callStack: List<Call>)

/** Builds the [TraversalStateKey] for reaching [node] under this [Context]. */
private fun Context.stateKey(node: Node): TraversalStateKey =
    TraversalStateKey(node, callStack.toList())

/**
 * Decides whether the state described by [ctx] must not be expanded any further because continuing
 * would (or is very likely to) diverge. This is the crucial guard for interprocedural recursion,
 * which the legacy `(node, callStack)`-repeat check cannot catch on its own: recursion through a
 * fresh call site grows the call stack forever without any state ever repeating.
 *
 * We cut a branch if either stack has grown past its hard backstop, or if the *same call site*
 * appears more than once on the call stack. The latter is compared by reference identity so that
 * two syntactically identical but distinct call sites are never mistaken for a recursion cycle.
 */
private fun contextExplosion(ctx: Context): Boolean {
    if (ctx.indexStack.depth > MAX_INDEX_STACK_BACKSTOP) return true
    val callDepth = ctx.callStack.depth
    if (callDepth > MAX_CALL_STACK_BACKSTOP) return true
    // Only a call stack of depth >= 2 can contain a repeated call site (a recursion cycle); avoid
    // the snapshot + map allocation in the common shallow/intraprocedural case.
    if (callDepth >= 2) {
        val seen = java.util.IdentityHashMap<Call, Boolean>()
        for (call in ctx.callStack.toList()) {
            if (seen.put(call, true) != null) return true
        }
    }
    return false
}
