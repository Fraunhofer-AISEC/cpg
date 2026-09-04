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
package de.fraunhofer.aisec.cpg.analysis.string

import de.fraunhofer.aisec.cpg.assumptions.AssumptionType
import de.fraunhofer.aisec.cpg.assumptions.assume
import de.fraunhofer.aisec.cpg.graph.Backward
import de.fraunhofer.aisec.cpg.graph.Context
import de.fraunhofer.aisec.cpg.graph.ContextSensitive
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.Interprocedural
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.NodePath
import de.fraunhofer.aisec.cpg.graph.OnlyFullDFG
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Cast
import de.fraunhofer.aisec.cpg.graph.expressions.Conditional
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.Subscription
import de.fraunhofer.aisec.cpg.graph.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import java.util.IdentityHashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Bounds the fixpoint loop in [StringEvaluator.evaluateWithFixpoint] (see its KDoc). */
private const val MAX_FIXPOINT_ITERATIONS = 20

/**
 * A structurally-hashable substitute for using [Context] directly as a `HashMap` key - see
 * [StringEvaluator]'s `threadLocalCache` KDoc for why [Context]'s own `hashCode` cannot be used for
 * this. Derived from exactly the two fields [Context.equals] itself compares
 * (`indexStack`/`callStack`), deliberately excluding `steps`.
 *
 * Stack elements ([de.fraunhofer.aisec.cpg.graph.expressions.Call] for `callStack`,
 * [de.fraunhofer.aisec.cpg.graph.edges.flows.IndexedDataflowGranularity] for `indexStack`) are
 * compared/hashed by identity, not by `Node`'s structural `equals`/`hashCode`, for the same reason
 * [StringEvaluator]'s `threadLocalPath` does: two different call sites (or two different
 * indexed-access edges) that happen to be structurally equal must not collapse into one cache
 * entry, which would silently merge results that D6 requires to stay separate.
 */
private class ContextKey(ctx: Context) {
    private val callStack = ctx.callStack.toList()
    private val indexStack = ctx.indexStack.toList()

    override fun equals(other: Any?): Boolean =
        other is ContextKey &&
            callStack.size == other.callStack.size &&
            indexStack.size == other.indexStack.size &&
            callStack.indices.all { callStack[it] === other.callStack[it] } &&
            indexStack.indices.all { indexStack[it] === other.indexStack[it] }

    override fun hashCode(): Int {
        var result = 1
        for (call in callStack) {
            result = 31 * result + System.identityHashCode(call)
        }
        for (granularity in indexStack) {
            result = 31 * result + System.identityHashCode(granularity)
        }
        return result
    }
}

/**
 * A demand-driven, backward, interprocedural evaluator for [StringPattern]s. See
 * `docs/docs/CPG/impl/string-analysis.md` for the design rationale.
 *
 * Structurally this mirrors [de.fraunhofer.aisec.cpg.evaluation.ValueEvaluator]: a `when` over node
 * types in [dispatch], with `open` handler methods so that languages/consumers can override
 * individual cases. The key differences (also documented in the design doc) are:
 * 1. Joins over multiple predecessors produce a [StringPattern.Union] via [union] instead of
 *    aborting (contrast with `ValueEvaluator.handlePrevDFG`, which gives up whenever a node has
 *    more than one incoming DFG edge).
 * 2. Predecessors are chosen context-sensitively and interprocedurally by delegating to [Backward]
 *    (`GraphToFollow.DFG`) `.pickNextStep`, instead of reading [Node.prevFullDFG] directly - this
 *    is what makes cross-function resolution (D6) work "for free".
 * 3. Cycles in the backward DFG (e.g. a variable that depends on its own previous value through a
 *    loop) are resolved via a small fixpoint loop using [StringLattice.widen], see
 *    [evaluateWithFixpoint].
 * 4. Unknowns are first-class: an unresolvable node becomes a [StringPattern.Unknown] carrying its
 *    origin and a [StringPattern.Reason], not a failure.
 *
 * A single instance is expected to be shared and invoked concurrently (e.g. from parallel query
 * evaluation), so all mutable per-evaluation state is kept in [ThreadLocal]s, mirroring
 * `ValueEvaluator.path`.
 */
open class StringEvaluator(
    val config: StringEvaluatorConfig = StringEvaluatorConfig(),
    /**
     * Consulted, in order, by [handleCall] before falling back to the generic predecessor-following
     * behaviour. Empty in this phase - see [StringOperationHandler], this is the extension point
     * for Phase 3 (language-specific call handling).
     */
    val operationHandlers: List<StringOperationHandler> = emptyList(),
) {
    open val log: Logger
        get() = LoggerFactory.getLogger(StringEvaluator::class.java)

    private val lattice =
        StringLattice(config.maxTermSize, config.maxTermDepth, config.maxUnionSize)

    /**
     * The current recursion stack, by object identity. Used for cycle detection: if we are asked to
     * evaluate a node that is already on this stack, we have found a cycle in the backward DFG.
     * `Node` overrides `equals`/`hashCode` structurally, so identity (`===`) is used explicitly
     * rather than relying on [List.contains].
     */
    private val threadLocalPath = ThreadLocal.withInitial { mutableListOf<Node>() }

    /**
     * The current fixpoint assumption for a node that is being widened because it is part of a
     * cycle (see [evaluateWithFixpoint]). Keyed by identity via [IdentityHashMap] for the same
     * reason as [threadLocalPath].
     */
    private val threadLocalAssumed =
        ThreadLocal.withInitial { IdentityHashMap<Node, StringPattern>() }

    /** Nodes that were found to be part of a cycle while they were being evaluated. */
    private val threadLocalCyclic = ThreadLocal.withInitial { identitySetOf<Node>() }

    /**
     * Memoizes the result of [evaluateInternal] for a `(Node, Context)` pair, scoped to a single
     * top-level [evaluate] call (cleared at the start of [evaluate], just like
     * [path]/[assumed]/[cyclic]).
     *
     * Without this, [evaluateInternal] re-descends into shared predecessors on every branch of
     * every join with no memoization: a chain of N sequential joins that each depend on the
     * previous one (e.g. `x = x + "a"` repeated inside if/else branches) triggers genuine `2^N`
     * re-evaluation, because [followPredecessors] evaluates *every* incoming branch independently
     * before taking their [union].
     *
     * Writes only ever happen once [evaluateWithFixpoint] has returned for `node` *and* no cycle is
     * still being widened anywhere on the current path (`cyclic.isEmpty()`) - see the comment at
     * the write site in [evaluateInternal] for why the latter condition is required in addition to
     * the former: a node that is merely a *dependency* of a still-converging cyclic node returns
     * normally on every fixpoint round, long before the cycle converges, and caching that
     * intermediate value would freeze in a too-precise result that later fixpoint rounds would read
     * back stale instead of recomputing - this is what makes the unmemoized evaluator sound today
     * (anything depending on a cyclic node always sees its final, widened value), and is exactly
     * the property [testLoopBuiltString] guards.
     *
     * Keying: the outer [IdentityHashMap] uses [Node] identity (not the structural
     * `equals`/`hashCode` `Node` defines), for the same reason [threadLocalAssumed] does - two
     * structurally-equal-but-distinct nodes must not collapse.
     *
     * The inner map is keyed by [ContextKey], **not** by [Context] directly, even though
     * conceptually we want to key on "everything about `ctx` that can change the result for the
     * same node" - which is exactly `indexStack` and `callStack` (not `steps`: that is a per-path
     * step counter that changes on every hop, and folding it into the key would defeat memoization
     * entirely by making almost every key unique; deliberately mirrors `Context.equals`, which also
     * excludes it). Using `callStack` as part of the key is what keeps this sound rather than just
     * imprecise: a [Parameter] reached via two different call sites has two different `callStack`s
     * and therefore two different cache entries, so per-call-site precision (D6, see the design
     * doc) is preserved - caching by `Node` identity alone would incorrectly collapse those into a
     * single answer.
     *
     * The reason we cannot just use `Context` itself as the `HashMap` key: `Context.hashCode()`
     * (`Extensions.kt`) is `Objects.hash(super.hashCode(), indexStack, callStack)`, and
     * `super.hashCode()` is `Any`'s *identity* hash code, since `Context` does not otherwise
     * override it - i.e. `Context.hashCode()` is (almost always) different for every instance even
     * when `Context.equals()` says two instances are equal. `HashMap` requires equal hashCodes for
     * equal keys, so using `Context` directly makes lookups miss the cache almost every time
     * (confirmed by measurement: near-zero hit rate), even though `equals()` itself is correct.
     * [ContextKey] recomputes a proper structural hash from the same two fields instead.
     */
    private val threadLocalCache =
        ThreadLocal.withInitial { IdentityHashMap<Node, MutableMap<ContextKey, StringPattern>>() }

    /**
     * Required by [Backward.pickNextStep]'s signature but not otherwise consulted by our logic:
     * [de.fraunhofer.aisec.cpg.graph.Interprocedural.followEdge] only ever *adds* to this set (to
     * report a detected call-recursion loop for callers that inspect it), it never reads it back to
     * make a decision. We do our own, simpler, node-identity-based cycle detection via
     * [threadLocalPath], so this is a throwaway sink.
     */
    private val threadLocalLoopingPaths = ThreadLocal.withInitial { mutableSetOf<NodePath>() }

    /** The node passed to the public [evaluate] entry point, used as the scope of assumptions. */
    private val threadLocalRoot = ThreadLocal<Node?>()

    private val path: MutableList<Node>
        get() = threadLocalPath.get()

    private val assumed: IdentityHashMap<Node, StringPattern>
        get() = threadLocalAssumed.get()

    private val cyclic: MutableSet<Node>
        get() = threadLocalCyclic.get()

    private val loopingPaths: MutableSet<NodePath>
        get() = threadLocalLoopingPaths.get()

    private val cache: IdentityHashMap<Node, MutableMap<ContextKey, StringPattern>>
        get() = threadLocalCache.get()

    private val rootNode: Node
        get() = threadLocalRoot.get() ?: error("evaluate() must be called before evaluateInternal")

    /** Evaluates [node], returning everything we know about the strings it may evaluate to. */
    fun evaluate(node: Node): StringPattern {
        path.clear()
        assumed.clear()
        cyclic.clear()
        loopingPaths.clear()
        cache.clear()
        threadLocalRoot.set(node)
        return evaluateInternal(node, Context())
    }

    /**
     * The single recursive gateway: serves cached results (see [threadLocalCache]), detects cycles
     * (a node already on [path]) and otherwise pushes [node] and delegates to
     * [evaluateWithFixpoint] `->` [dispatch].
     */
    protected open fun evaluateInternal(node: Node, ctx: Context): StringPattern {
        val key = ContextKey(ctx)
        cache[node]?.get(key)?.let {
            return it
        }

        if (path.any { it === node }) {
            // We are in the middle of evaluating `node` further up the call stack: this is a
            // genuine cycle in the backward DFG (e.g. a loop-carried variable). Report it to the
            // enclosing evaluateWithFixpoint call for `node` (which is guaranteed to be on the
            // stack) and return our current best guess for it - Bottom until a first guess exists.
            cyclic.add(node)
            return assumed[node] ?: StringPattern.Bottom
        }

        path.add(node)
        try {
            val result = evaluateWithFixpoint(node, ctx)
            // Only cache once there is no cycle still being widened anywhere on the current path.
            // `evaluateWithFixpoint` removes `node` from `cyclic` only once *its own* fixpoint has
            // converged, so `cyclic.isEmpty()` here means neither `node` itself nor any ancestor
            // still on `path` is mid-fixpoint. This is necessary, not just the "cache only after
            // evaluateWithFixpoint returns" rule from the KDoc above: a node like the `+` in
            // `x = x + "a"` (a *dependency* of the cyclic node `x = ...`, not the cyclic node
            // itself) has its own `evaluateWithFixpoint` return normally on every fixpoint round,
            // long before the outer cycle converges - caching its result the first time it returns
            // would freeze in a value computed against a not-yet-widened `assumed[x]`, and every
            // later fixpoint round would then read that stale cached value back out instead of
            // recomputing it against the newly widened assumption, silently breaking convergence.
            // Skipping the cache write while `cyclic` is non-empty defers caching for such
            // dependencies indefinitely (they are simply recomputed every time, as before this
            // change) - sound, if less optimal, and confirmed necessary by `testLoopBuiltString`,
            // which fails with a too-precise, non-widened result if this check is removed.
            if (cyclic.isEmpty()) {
                cache.getOrPut(node) { mutableMapOf() }[key] = result
            }
            return result
        } finally {
            path.removeAt(path.size - 1)
        }
    }

    /**
     * If evaluating [node] did not recurse back into [node] itself, this is just [dispatch]. If it
     * did (i.e. [node] was added to [cyclic] by the nested [evaluateInternal] call), we have a
     * cycle and need a fixpoint: re-[dispatch] with an increasingly refined assumption for what
     * [node] evaluates to, [StringLattice.widen]ing between rounds, until two consecutive rounds
     * agree (a real fixpoint) or [MAX_FIXPOINT_ITERATIONS] is hit (a safety backstop; [widen] is
     * proven to terminate on its own, see [StringLattice.widen]'s KDoc, so this bound should never
     * actually bite).
     *
     * This never under-approximates: every round only replaces the assumption by `widen(old, new)`,
     * which by construction represents a language at least as large as either operand, so even an
     * early, non-fixpoint exit is a sound (if not maximally precise) answer.
     */
    private fun evaluateWithFixpoint(node: Node, ctx: Context): StringPattern {
        var result = dispatch(node, ctx)
        if (node !in cyclic) {
            return result
        }

        var iterations = 0
        while (iterations < MAX_FIXPOINT_ITERATIONS) {
            val prior = assumed[node]
            val widened = if (prior == null) result else lattice.widen(prior, result)
            if (widened == prior) {
                result = widened
                break
            }
            assumed[node] = widened
            result = dispatch(node, ctx)
            iterations++
        }
        assumed.remove(node)
        cyclic.remove(node)
        return result
    }

    /** The node-type dispatch table, see the design doc's "Handlers to implement" table. */
    protected open fun dispatch(node: Node, ctx: Context): StringPattern =
        when (node) {
            is Literal<*> -> handleLiteral(node)
            is BinaryOperator -> handleBinaryOperator(node, ctx)
            is Assign -> handleAssign(node, ctx)
            is Conditional -> handleConditional(node, ctx)
            is Cast -> handleCast(node, ctx)
            is Call -> handleCall(node, ctx)
            is Subscription -> handleSubscription(node, ctx)
            is UnaryOperator -> handleUnaryOperator(node, ctx)
            // Reference, Variable, Field, Parameter and everything else: context-sensitive
            // predecessors.
            else -> followPredecessors(node, ctx)
        }

    /**
     * `Const(value.toString())` for a string/char literal, `Bottom` for a `null` literal.
     *
     * For any other literal type (numbers, booleans, ...) we also fall back to
     * `Const(value.toString())` rather than `Unknown`: this makes constructs like `"x" + 5` (a
     * `BinaryOperator` `+` whose rhs is a numeric literal) resolve to `Const("x5")` instead of
     * losing precision, at the cost of not distinguishing "was genuinely a string" from "was
     * stringified" - a distinction [StringPattern] has no room for anyway, and one `ValueEvaluator`
     * does not make either (see `ValueEvaluator.handlePlus`).
     */
    protected open fun handleLiteral(node: Literal<*>): StringPattern {
        val value = node.value
        return if (value == null) StringPattern.Bottom else const(value.toString())
    }

    /**
     * `+`/`+=` become [concat]; every other operator falls back to [followPredecessors] (in
     * practice this almost always yields [StringPattern.Unknown], since arithmetic/comparison
     * operators do not usually have an incoming DFG edge of their own).
     */
    protected open fun handleBinaryOperator(node: BinaryOperator, ctx: Context): StringPattern {
        return when (node.operatorCode) {
            "+",
            "+=" ->
                concat(
                    evaluateInternal(node.lhs, ctx),
                    evaluateInternal(node.rhs, ctx),
                    maxTermSize = config.maxTermSize,
                    maxTermDepth = config.maxTermDepth,
                    maxUnionSize = config.maxUnionSize,
                )
            else -> followPredecessors(node, ctx)
        }
    }

    /**
     * The rhs for a simple assignment, or the [concat] of the current lhs value and the rhs for a
     * compound (`+=`-like) assignment - mirroring
     * `ValueEvaluator.handleAssign`/`computeBinaryOpEffect`.
     */
    protected open fun handleAssign(node: Assign, ctx: Context): StringPattern {
        val lhs = node.lhs.singleOrNull()
        val rhs = node.rhs.singleOrNull()
        return if (lhs != null && rhs != null && node.isCompoundAssignment) {
            concat(
                evaluateInternal(lhs, ctx),
                evaluateInternal(rhs, ctx),
                maxTermSize = config.maxTermSize,
                maxTermDepth = config.maxTermDepth,
                maxUnionSize = config.maxUnionSize,
            )
        } else {
            rhs?.let { evaluateInternal(it, ctx) } ?: followPredecessors(node, ctx)
        }
    }

    /**
     * `union(then, else)`. Unlike `ValueEvaluator.handleConditional`, we do not currently
     * constant-fold the condition to pick a single branch - both branches are always joined. This
     * costs some precision on conditions that are actually always-true/always-false, but keeps this
     * phase simple; refining this is listed as a possible improvement in the design doc ("refined
     * by a constant-folded condition where possible") rather than a hard requirement.
     */
    protected open fun handleConditional(node: Conditional, ctx: Context): StringPattern {
        val then = node.thenExpression?.let { evaluateInternal(it, ctx) } ?: StringPattern.Bottom
        val els = node.elseExpression?.let { evaluateInternal(it, ctx) } ?: StringPattern.Bottom
        return union(
            listOf(then, els),
            maxTermSize = config.maxTermSize,
            maxTermDepth = config.maxTermDepth,
            maxUnionSize = config.maxUnionSize,
        )
    }

    /** Transparent: recurses into the cast's inner expression. */
    protected open fun handleCast(node: Cast, ctx: Context): StringPattern {
        val expression = node.expression
        return if (expression != null) evaluateInternal(expression, ctx)
        else followPredecessors(node, ctx)
    }

    /**
     * Default behaviour: no language-specific [StringOperationHandler] recognises [node], so we
     * fall back to the predecessors of the call's own DFG (i.e. whatever the call's return value
     * flows from - which, via [followPredecessors]'s use of [Backward]/[ContextSensitive], already
     * includes flowing into the callee and back out through its `return`, see the design doc's D6).
     *
     * `open` so that Phase 3 can add its own handling without touching [dispatch].
     */
    protected open fun handleCall(node: Call, ctx: Context): StringPattern {
        for (handler in operationHandlers) {
            val result = handler.handleCall(node) { evaluateInternal(it, ctx) }
            if (result != null) {
                return result
            }
        }
        return followPredecessors(node, ctx)
    }

    /**
     * Best-effort: a single character of a known [StringPattern.Const] at a known constant index,
     * otherwise [StringPattern.Unknown]. Deliberately does not attempt general slicing.
     */
    protected open fun handleSubscription(node: Subscription, ctx: Context): StringPattern {
        val array = node.arrayExpression?.let { evaluateInternal(it, ctx) }
        val index = ((node.subscriptExpression as? Literal<*>)?.value as? Number)?.toInt()
        return if (array is StringPattern.Const && index != null && index in array.value.indices) {
            const(array.value[index].toString())
        } else {
            StringPattern.Unknown(origin = node, reason = StringPattern.Reason.UNSUPPORTED)
        }
    }

    /** `*`/`&` are transparent for value purposes, as in `ValueEvaluator.handleUnaryOp`. */
    protected open fun handleUnaryOperator(node: UnaryOperator, ctx: Context): StringPattern {
        return when (node.operatorCode) {
            "*",
            "&" -> node.input?.let { evaluateInternal(it, ctx) } ?: followPredecessors(node, ctx)
            else -> followPredecessors(node, ctx)
        }
    }

    /**
     * The core of D6/D7: rather than reading [Node.prevFullDFG] directly, chooses predecessors of
     * [node] via [Backward] (`GraphToFollow.DFG`) `.pickNextStep`, which reuses the machinery
     * `followPrevFullDFGEdgesUntilHit` is built on and gives us context-sensitive, interprocedural
     * predecessor selection "for free": `ContextSensitiveDataflow` edges with
     * `CallingContextIn`/`CallingContextOut` already connect arguments to parameters and returns to
     * call sites, and [Context.callStack] keeps us from returning into the wrong caller.
     *
     * We call [de.fraunhofer.aisec.cpg.graph.AnalysisDirection.pickNextStep] directly, one step at
     * a time, rather than building on the higher-level `followPrevFullDFGEdgesUntilHit`/
     * `followDFGEdgesUntilHit` wrappers: those wrappers are built to enumerate whole *paths* to a
     * target predicate, which is not what we want here - we need, at each node, only its immediate
     * predecessor set, so that we can recurse into each one (through this same evaluator, so that
     * every node type gets its own handler again) and [union] the results. Collecting full paths
     * just to discard everything but the one-hop predecessor set would be wasteful and would not
     * even give us the right recursion shape (we need to interleave *our* node dispatch between
     * hops, not just filter/terminate on a predicate).
     * - Zero next steps: [node] is a leaf. A [Parameter] with no reachable call site becomes
     *   `Unknown(reason = PARAMETER)`; anything else becomes `Unknown(reason = UNSUPPORTED)`.
     * - Exactly one next step: recurse and return that result directly - no join needed, which
     *   keeps the common case as precise as `ValueEvaluator`.
     * - More than one next step: recurse into every branch and [union] the results - this is D7,
     *   the core improvement over `ValueEvaluator.handlePrevDFG`, which just aborts here.
     *
     * Budget exhaustion (`AnalysisScope.maxSteps`) is checked proactively (rather than inferred
     * from an empty result, which would be indistinguishable from a genuine dead end) and yields
     * `Unknown(reason = BUDGET_EXCEEDED)` plus a [AssumptionType.SoundnessAssumption] recorded on
     * the node passed to the public [evaluate] entry point (the "root node"), since [StringPattern]
     * itself has no assumptions slot (it is a pure value type, not a `Node`/`HasAssumptions`) - see
     * the design doc's discussion of this trade-off in `QueryHelpers.kt`'s `mustMatch`.
     *
     * `Interprocedural.maxCallDepth`, in contrast, cannot be checked proactively for an arbitrary
     * node: [de.fraunhofer.aisec.cpg.graph.Interprocedural.followEdge] only ever cuts off
     * *interprocedural* edges once `ctx.callStack.depth >= maxCallDepth`, not the node itself, and
     * a node can have a perfectly reachable same-function predecessor whose availability has
     * nothing to do with the call-depth budget. Proactively labelling every node reached at max
     * depth as budget-exceeded would therefore mislabel nodes that would have gotten a real answer
     * regardless (this used to be checked, incorrectly, only `if (node is Parameter)`, which missed
     * every other node type `followEdge` can cut off, e.g. a `Return`/call-boundary node in a plain
     * deep call chain). Instead we call
     * [de.fraunhofer.aisec.cpg.graph.AnalysisDirection.pickNextStep] as normal, and only *after* it
     * comes back empty do we ask whether we are at/beyond `maxCallDepth` - if so, the empty result
     * is plausibly `followEdge` cutting off the only escape route, so we label it
     * `BUDGET_EXCEEDED`; otherwise it is a genuine leaf and we fall back to [leafUnknown] (e.g.
     * `PARAMETER` for a parameter that truly has no caller at all).
     */
    protected open fun followPredecessors(node: Node, ctx: Context): StringPattern {
        val scope = config.scope
        val maxSteps = scope.maxSteps
        if (maxSteps != null && ctx.steps >= maxSteps) {
            return budgetExceeded(node)
        }

        val steps =
            Backward(GraphToFollow.DFG)
                .pickNextStep(
                    node,
                    scope,
                    ctx,
                    emptyList(),
                    loopingPaths,
                    OnlyFullDFG,
                    ContextSensitive,
                )
                .filter { (next, _, _) -> config.enterInferredFunctions || !isInferred(next) }

        if (steps.isEmpty()) {
            val maxCallDepth = (scope as? Interprocedural)?.maxCallDepth
            return if (maxCallDepth != null && ctx.callStack.depth >= maxCallDepth) {
                budgetExceeded(node)
            } else {
                leafUnknown(node)
            }
        }

        val results =
            steps.map { (next, _, newCtx) ->
                newCtx.inc()
                evaluateInternal(next, newCtx)
            }

        return if (results.size == 1) {
            results[0]
        } else {
            union(
                results,
                maxTermSize = config.maxTermSize,
                maxTermDepth = config.maxTermDepth,
                maxUnionSize = config.maxUnionSize,
            )
        }
    }

    private fun isInferred(node: Node): Boolean =
        node.isInferred || node.firstParentOrNull<Function>()?.isInferred == true

    private fun leafUnknown(node: Node): StringPattern =
        if (node is Parameter) {
            StringPattern.Unknown(origin = node, reason = StringPattern.Reason.PARAMETER)
        } else {
            StringPattern.Unknown(origin = node, reason = StringPattern.Reason.UNSUPPORTED)
        }

    private fun budgetExceeded(node: Node): StringPattern {
        rootNode.assume(
            AssumptionType.SoundnessAssumption,
            "We assume that the value of `$node` is over-approximated because the string " +
                "evaluator's budget (maxSteps/maxCallDepth, see StringEvaluatorConfig.scope) was " +
                "exhausted before reaching a base case. To verify this assumption, we need to check " +
                "whether increasing the budget changes the result.",
            scope = node,
        )
        return StringPattern.Unknown(origin = node, reason = StringPattern.Reason.BUDGET_EXCEEDED)
    }
}

/** Evaluates [this], see [StringEvaluator]. */
fun Node.evaluateString(config: StringEvaluatorConfig = StringEvaluatorConfig()): StringPattern =
    StringEvaluator(config).evaluate(this)
