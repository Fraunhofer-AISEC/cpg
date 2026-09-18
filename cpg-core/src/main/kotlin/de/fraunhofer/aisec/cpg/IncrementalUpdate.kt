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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.Constructor
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Construction
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.PointerDereference
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.graph.scopes.FunctionScope
import de.fraunhofer.aisec.cpg.graph.scopes.LocalScope
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.passes.BasicBlockCollectorPass
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.PointsToPass
import de.fraunhofer.aisec.cpg.passes.SccPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.TypeHierarchyResolver
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import de.fraunhofer.aisec.cpg.passes.markDirty
import kotlin.reflect.KClass
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("de.fraunhofer.aisec.cpg.IncrementalUpdate")

/**
 * Runs after a new [TranslationUnit] has been parsed and merged into [result] by
 * [TranslationManager.addSource]. This does not run any [de.fraunhofer.aisec.cpg.passes.Pass] -- it
 * only (1) seeds [TranslationResult.dirtyNodes] so that a later, dirty-marking-aware pass re-run
 * (see [de.fraunhofer.aisec.cpg.runDirtyPasses]) knows what to look at, and (2) performs graph
 * surgery to remove stale edges that a re-run would otherwise leave dangling alongside newly
 * resolved ones.
 *
 * Every new [Function]-like declaration in [tu] is marked dirty for [SymbolResolver] and the
 * DFG-family/EOG-family passes described in [markDfgRelatedPassesDirty], and [tu] itself is marked
 * dirty for [EvaluationOrderGraphPass].
 *
 * In addition, [reconcileExistingUsagesWithNewSymbols] reconciles every new, non-local declaration
 * in [tu] (see [collectNewNonLocalSymbols]) against the *whole* live graph in a single batched
 * scan: pre-existing [de.fraunhofer.aisec.cpg.graph.expressions.Call]s that were unresolved,
 * resolved to an [de.fraunhofer.aisec.cpg.passes.inference.Inference]-created stub, or even already
 * resolved to some other real declaration, get the new declaration added to their
 * [de.fraunhofer.aisec.cpg.graph.expressions.Call.invokes] if it is a viable candidate; a stale
 * inferred stub still present in `invokes` once a real candidate exists is removed and, once fully
 * orphaned, detached from the graph. Pre-existing [Reference]s (e.g. variable/field accesses) are
 * handled more conservatively, since only one resolution can exist at a time -- see
 * [reconcileReferences] for the exact cases handled (and the one deliberately left alone).
 */
internal fun TranslationManager.updateIncrementally(
    result: TranslationResult,
    tu: TranslationUnit,
) {
    // Never changes within a single call, so compute it once and thread it through instead of
    // recomputing it per new function/call.
    val registeredPasses = result.finalCtx.config.registeredPasses.flatten()

    // EvaluationOrderGraphPass is purely intraprocedural/AST-driven (no @DependsOn at all -- it
    // builds the EOG straight from the fresh AST and only touches the ScopeManager for label
    // lookups, not symbol resolution) and tu is a brand-new TranslationUnit no pass has ever seen,
    // so there are no pre-existing EOG edges to roll back here -- this is purely additive. Like the
    // DFG-family marking below, this is gated on actual registration so we never schedule a pass
    // that a full analyze() would not have run either.
    if (registeredPasses.contains(EvaluationOrderGraphPass::class)) {
        tu.markDirty<EvaluationOrderGraphPass>()
    }
    // TypeResolver/TypeHierarchyResolver/ImportResolver never ran on this new subtree either, so
    // e.g. a new function's parameter/return ObjectTypes never get `recordDeclaration` resolved,
    // and imports referenced from the new code are never resolved -- both of which SymbolResolver
    // relies on for member-call/constructor resolution. These are ComponentPass-granularity (see
    // PartialPassExecution.kt), so marking them dirty triggers a whole-component rerun; that
    // inherent lack of finer granularity is a pre-existing, accepted limitation, not something to
    // fix here.
    if (registeredPasses.contains(TypeResolver::class)) {
        tu.markDirty<TypeResolver>()
    }
    if (registeredPasses.contains(TypeHierarchyResolver::class)) {
        tu.markDirty<TypeHierarchyResolver>()
    }
    if (registeredPasses.contains(ImportResolver::class)) {
        tu.markDirty<ImportResolver>()
    }

    for (newFunction in tu.allChildren<Function>()) {
        newFunction.markDirty<SymbolResolver>()
        // DFGPass connects a resolved call's arguments/return value to the invoked function's
        // parameters based on `Call.invokes`; since SymbolResolver has not run on this new
        // function yet (it is not invoked by anyone so far), DFGPass has nothing to do for it yet
        // either -- but it must be re-run once it starts being called (see below).
        newFunction.markDfgRelatedPassesDirty(registeredPasses)
        // BasicBlockCollectorPass/ControlDependenceGraphPass/SccPass are all per-EOG-starter
        // passes that only depend on EvaluationOrderGraphPass's output for the same starter, so,
        // like the DFG-family marking above, this is purely additive: newFunction has never been
        // visited by any of them before, so there is nothing to roll back. resolveEOGStarterTargets
        // in PartialPassExecution.kt resolves a dirty node to the nearest enclosing/contained
        // EOGStarterHolder with no incoming EOG edges -- newFunction (a Function, hence an
        // EOGStarterHolder with empty prevEOG until EvaluationOrderGraphPass runs) already *is*
        // that target, so marking it directly lines up exactly with what runDirtyPasses looks for.
        // Each is gated on actual registration -- ControlDependenceGraphPass in particular is NOT
        // part of defaultPasses(), so unconditionally marking it dirty would make an
        // addSource()+runDirtyPasses() pipeline run a pass a full analyze() never would.
        if (registeredPasses.contains(BasicBlockCollectorPass::class)) {
            newFunction.markDirty<BasicBlockCollectorPass>()
        }
        if (registeredPasses.contains(ControlDependenceGraphPass::class)) {
            newFunction.markDirty<ControlDependenceGraphPass>()
        }
        if (registeredPasses.contains(SccPass::class)) {
            newFunction.markDirty<SccPass>()
        }
    }

    // A single batched scan over the whole live graph (see reconcileExistingUsagesWithNewSymbols),
    // done exactly once per addSource/updateIncrementally call -- not once per new symbol -- rather
    // than one scope-lookup per new function like the previous, narrower implementation.
    val newSymbols = tu.collectNewNonLocalSymbols()
    if (
        newSymbols.isNotEmpty() &&
            reconcileExistingUsagesWithNewSymbols(result, tu, newSymbols, registeredPasses)
    ) {
        result.finalCtx.scopeManager.invalidateSymbolLookupCache()
    }
}

/**
 * Declarations in this [TranslationUnit] that are visible from outside their own declaration site
 * -- i.e. that a pre-existing [Call] or [Reference] elsewhere in the graph could plausibly target
 * now that they exist. [Function]s (including [Method]s and [Constructor]s) and [Record]s are
 * always included, since both are callable/referenceable from wherever their scope/visibility
 * allows. A [Variable] (which also covers [de.fraunhofer.aisec.cpg.graph.declarations.Field]) is
 * only included if it is declared outside a function body: a function-local variable or [Parameter]
 * lives in a [FunctionScope] or [LocalScope] and can never be referenced from outside the function
 * it was declared in, so it can never be the target of a pre-existing, already-parsed reference --
 * including it in the scan below would be pure overhead.
 */
private fun TranslationUnit.collectNewNonLocalSymbols(): List<Declaration> =
    allChildren<Declaration> { declaration ->
        when (declaration) {
            is Function -> true
            is Record -> true
            is Variable -> declaration.scope !is FunctionScope && declaration.scope !is LocalScope
            else -> false
        }
    }

/**
 * Marks [DFGPass] dirty (it always attaches the "invoked function flows into the call" DFG edge for
 * every entry of [de.fraunhofer.aisec.cpg.graph.expressions.Call.invokes]), plus, mirroring
 * [DFGPass.handleCall]'s own gating, every one of [PointsToPass] / [ControlFlowSensitiveDFGPass]
 * that is actually registered -- one of these (rather than [DFGPass] itself) attaches the
 * argument-to-parameter DFG edges once either is registered (the default). Normally at most one of
 * the two is registered (mutually exclusive in [TranslationConfiguration.Builder.defaultPasses]),
 * but a manually-assembled pass list could register both, so we must not stop after the first
 * match.
 */
private fun Node.markDfgRelatedPassesDirty(registeredPasses: List<KClass<out Pass<out Node>>>) {
    markDirty<DFGPass>()
    if (registeredPasses.contains(PointsToPass::class)) {
        markDirty<PointsToPass>()
    }
    if (registeredPasses.contains(ControlFlowSensitiveDFGPass::class)) {
        markDirty<ControlFlowSensitiveDFGPass>()
    }
}

/**
 * Implements steps 2-5 of the class-level design described on [updateIncrementally]: a single
 * [TranslationResult.allChildren] scan over the whole live graph (the cheap filter, phase 1)
 * collects every pre-existing [Call]/[Reference] whose name matches one of [newSymbols], and only
 * that (much smaller) candidate list is then checked against the actual signature/scope of each
 * matching new declaration (the precise check, phase 2, split across [reconcileCalls] and
 * [reconcileReferences]). This structure is deliberate: the full-graph walk below must execute
 * exactly once per [updateIncrementally] call no matter how many new symbols [tu] introduces, and
 * the expensive per-candidate checks (signature matching, scope lookups) must never run against the
 * full graph.
 *
 * Returns `true` if at least one inferred stub was fully detached from the graph as a result.
 */
private fun reconcileExistingUsagesWithNewSymbols(
    result: TranslationResult,
    tu: TranslationUnit,
    newSymbols: List<Declaration>,
    registeredPasses: List<KClass<out Pass<out Node>>>,
): Boolean {
    val byName: Map<Symbol, List<Declaration>> = newSymbols.groupBy { it.symbol }

    // Phase 1 (cheap filter): exactly one scan over the whole graph, matching only on name and
    // language -- no signature/scope inspection happens here.
    val matches =
        result.allChildren<Node> { node ->
            node.language == tu.language && node.symbolName in byName
        }
    val candidateCalls = matches.filterIsInstance<Call>()
    val candidateReferences =
        matches.filterIsInstance<Reference>().filter {
            // A Call's callee (whether a plain Reference or, for a MemberCall, a MemberAccess) is
            // handled via reconcileCalls/Call.invokes instead -- SubgraphWalker sets
            // resolutionHelper to the owning Call for exactly these nodes (see
            // ExpressionBuilder.kt), so this is the same check SymbolResolver.handleReference
            // itself uses to recognize a callee.
            it.resolutionHelper !is Call
        }

    // DFGPass.connectInferredCallArguments consults Function.functionSummary to attach extra
    // reverse edges (arg <- stub-parameter, plus arg.access flipped to READWRITE for by-reference
    // parameters) for calls to inferred functions -- but ONLY if it actually runs, which DFGPass
    // itself gates on neither ControlFlowSensitiveDFGPass nor PointsToPass being registered (see
    // DFGPass.runsPointsToPassOrCfsDFG). If either of those passes IS registered (the default),
    // connectInferredCallArguments never runs, so functionSummary being non-empty is irrelevant
    // here (PointsToPass/ControlFlowSensitiveDFGPass populate it for their own, unrelated reasons).
    val connectInferredCallArgumentsMayHaveRun =
        registeredPasses.none {
            it == ControlFlowSensitiveDFGPass::class || it == PointsToPass::class
        }

    val detachedByCalls =
        reconcileCalls(
            candidateCalls,
            byName,
            result.finalCtx.scopeManager,
            registeredPasses,
            connectInferredCallArgumentsMayHaveRun,
        )
    val detachedByReferences =
        reconcileReferences(
            candidateReferences,
            byName,
            result.finalCtx.scopeManager,
            registeredPasses,
        )

    return detachedByCalls || detachedByReferences
}

/**
 * The cheap, name-only key used by phase 1's candidate scan: [Node.name]'s local name for
 * everything except a [Construction], which -- unlike a regular [Call] -- has no meaningful
 * `callee` (its inherited [Call.name] resolves to whatever placeholder its unused `callee` edge
 * defaults to, not the type being constructed). [Construction] is matched by the local name of the
 * type it instantiates instead, mirroring how
 * [de.fraunhofer.aisec.cpg.passes.SymbolResolver.handleConstruction] itself resolves it -- via
 * [Construction.type], not [Call.name]/[Call.callee].
 */
private val Node.symbolName: Symbol
    get() =
        if (this is Construction) {
            (type.root as? ObjectType)?.name?.localName ?: name.localName
        } else {
            name.localName
        }

/** The [Record] this [Type] resolves to, if it is (or wraps) an [ObjectType]. */
private fun Type.recordOrNull(): Record? = (root as? ObjectType)?.recordDeclaration

/**
 * The [Record] that owns the member [source] statically accesses -- determined via the *static type
 * of the receiver expression*, exactly like
 * [de.fraunhofer.aisec.cpg.passes.SymbolResolver.resolveMemberByName]/`handleMemberAccess` resolve
 * a member call/field access, and completely independent of where [source] is lexically written.
 *
 * Returns `null` for anything that is not an *explicit* member access with its own receiver
 * expression (a [MemberCall] or a [MemberAccess] used as a plain field reference) -- in particular
 * for a plain [Call]/[Reference] resolved via an *implicit* receiver (e.g. an unqualified `foo()`
 * inside a method, resolved against `this`). [isReachableFrom] handles that case correctly via the
 * lexical scope-chain walk instead, which mirrors [SymbolResolver]'s own `HasImplicitReceiver`
 * fallback in `getPossibleContainingTypes`/`handleReference` -- genuinely lexical there, unlike
 * member access via an explicit receiver.
 */
private fun memberReceiverRecord(source: Node): Record? {
    val base =
        when (source) {
            is MemberCall -> source.base
            is MemberAccess -> source.base
            else -> return null
        } ?: return null
    val baseType = (base as? PointerDereference)?.input?.type ?: base.type
    return baseType.recordOrNull()
}

/**
 * The [Record] that declares this member, mirroring [SymbolResolver]'s own (private)
 * `declaringRecord` extension: a [Method] may be defined out-of-line (e.g. C++ `void C::foo() {}`),
 * where its AST parent is the enclosing namespace/translation unit rather than the record, so we
 * prefer its explicitly-tracked [Method.recordDeclaration]; everything else (e.g. a [Field]) is
 * looked up via the closest enclosing [Record] in the AST.
 */
private val Declaration.declaringRecordOrNull: Record?
    get() = (this as? Method)?.recordDeclaration ?: firstParentOrNull<Record>()

/**
 * Whether [declaringRecord] is [receiverRecord] itself, or one of its (transitive) ancestors --
 * mirroring [SymbolResolver.resolveMemberByName]/its (private) `ancestorRecords` extension, which
 * walks [receiverRecord]'s supertype chain via [de.fraunhofer.aisec.cpg.ancestors] (not just an
 * exact match), so that a member inherited from a base class resolves too.
 *
 * Uses `===` (identity), like every other node-identity check in this file, rather than `==`
 * (structural equality): [Record.equals] is fully structural, so two distinct, structurally
 * identical (e.g. empty/placeholder) `Record`s could otherwise be wrongly treated as the same type,
 * linking a member of one to a receiver of the other.
 */
private fun isSameOrAncestorRecord(receiverRecord: Record, declaringRecord: Record): Boolean {
    return receiverRecord.toType().ancestors.any { it.type.recordOrNull() === declaringRecord }
}

/**
 * Whether [declaration] is actually visible from [source]'s point of resolution -- the
 * "scope/visibility check" half of the precise, phase-2 check. Two deliberately different
 * mechanisms are used, matching how [SymbolResolver] itself resolves each case:
 * - If [source] is an *explicit* member access (a [MemberCall] or a [MemberAccess] field reference
 *   with its own receiver expression), reachability is decided by the receiver's static type -> its
 *   [Record] and that [Record]'s ancestor chain (see
 *   [memberReceiverRecord]/[isSameOrAncestorRecord]), completely independent of where [source] is
 *   lexically written. A lexical scope-chain walk would be wrong here: e.g. `other.foo()` on an
 *   object of an unrelated class must never resolve against a same-named, same-signature free
 *   function or a different class's method just because that declaration happens to be lexically
 *   visible from the call site.
 * - Otherwise (a plain [Call]/[Reference], including one resolved via an *implicit* receiver),
 *   reachability is a lexical scope-chain walk via [ScopeManager.lookupSymbolByName] -- the same
 *   mechanism [SymbolResolver] itself uses to resolve a [Reference]/[Call.callee].
 *
 * Note: if an explicit member access's receiver type cannot be resolved to a [Record] at all (e.g.
 * its base type is still unknown), [memberReceiverRecord] returns `null` and we fall back to the
 * lexical walk, which may then fail to find an otherwise-valid candidate. This residual gap (a stub
 * that could theoretically still have such an unresolvable-base caller) is accepted as a rare,
 * pathological edge case rather than something to build further machinery for.
 */
private fun ScopeManager.isReachableFrom(source: Node, declaration: Declaration): Boolean {
    val receiverRecord = memberReceiverRecord(source)
    if (receiverRecord != null) {
        val declaringRecord = declaration.declaringRecordOrNull ?: return false
        return isSameOrAncestorRecord(receiverRecord, declaringRecord)
    }

    val candidates =
        lookupSymbolByName(
            declaration.name,
            declaration.language,
            startScope = source.scope ?: globalScope,
        )
    return candidates.any { it === declaration }
}

/**
 * Phase 2 for [Call]/[de.fraunhofer.aisec.cpg.graph.expressions.Construction] candidates: for every
 * candidate whose name matched some new declaration in [byName] (phase 1), checks whether that new
 * declaration is actually a viable resolution target (right kind of declaration -- [Constructor]
 * for a Construction, any other [Function] otherwise --, a matching signature via
 * [Function.matchesSignature], and reachable per [isReachableFrom]).
 *
 * Per the class-level design: unlike [Reference.refersTo] (a single edge), [Call.invokes] already
 * tolerates multiple simultaneous targets -- [SymbolResolver]'s own
 * [de.fraunhofer.aisec.cpg.passes.decideInvokesBasedOnCandidates] leaves more than one candidate in
 * `invokes` whenever resolution is ambiguous or "problematic" (e.g. overload resolution finding
 * several equally-viable functions). We mirror that exact mechanism here: a viable new declaration
 * is simply *added* to `invokes` (idempotently) regardless of whether the call was previously
 * unresolved, resolved to an inferred stub, or already resolved to some other real declaration --
 * there is no need to decide which resolution should "win".
 *
 * The one exception: once a real (non-inferred) candidate exists for a call, any *inferred*
 * function stub still present in that call's `invokes` is removed (it "was probably only there as a
 * bridge"). Once a stub is no longer invoked by anyone at all ([Function.calledBy] empty), it is
 * detached from the graph entirely by [detachInferredDeclaration].
 *
 * Returns `true` if at least one stub was fully detached.
 */
private fun reconcileCalls(
    candidateCalls: List<Call>,
    byName: Map<Symbol, List<Declaration>>,
    scopeManager: ScopeManager,
    registeredPasses: List<KClass<out Pass<out Node>>>,
    connectInferredCallArgumentsMayHaveRun: Boolean,
): Boolean {
    // Calls (possibly several) whose invokes edge to a given stub was just removed, so we can
    // decide -- once every candidate call has been processed -- whether the stub is now fully
    // orphaned, and if so, tear down its function-summary-derived DFG edges for exactly those
    // calls (mirroring the old, per-stub cleanup's deferred teardown).
    val callsByRemovedStub = mutableMapOf<Function, MutableList<Call>>()

    for (call in candidateCalls) {
        val declarations = byName[call.symbolName] ?: continue
        val isConstruction = call is Construction
        val functionCandidates =
            declarations.filterIsInstance<Function>().filter { candidate ->
                (candidate is Constructor) == isConstruction &&
                    (!isConstruction ||
                        matchesConstructionTarget(call as Construction, candidate as Constructor))
            }

        var invokesChanged = false
        for (candidate in functionCandidates) {
            if (
                candidate.matchesSignature(
                    call.arguments.map { it.type },
                    call.arguments,
                    useDefaultArguments = true,
                ) !is SignatureMatches
            ) {
                continue
            }
            if (!scopeManager.isReachableFrom(call, candidate)) {
                continue
            }

            if (call.invokes.none { it === candidate }) {
                // Use the edge list's own `add`, not a whole-property reassignment: assigning
                // `call.invokes = ...` goes through EdgeCollection.resetTo, which discards and
                // rebuilds EVERY edge from scratch -- silently resetting Invoke.dynamicInvoke back
                // to false for every PRE-EXISTING entry too (corrupting static-vs-dynamic call
                // tracking for calls previously resolved via DynamicInvokeResolver/PointsToPass),
                // not just the newly-added one. `invokeEdges.add` only ever creates the one new
                // edge and leaves every existing edge (and its flags) untouched.
                call.invokeEdges.add(candidate)
                invokesChanged = true
            }

            // A real, non-inferred candidate now exists for this call -- remove any inferred stub
            // still sitting in `invokes` (see the function doc).
            val staleStubs = call.invokes.filterIsInstance<Function>().filter { it.isInferred }
            for (stub in staleStubs) {
                if (!connectInferredCallArgumentsMayHaveRun || stub.functionSummary.isEmpty()) {
                    tearDownStandardDfgEdges(stub, call)
                }
                // In-place removal (like the rest of this file), not a property reassignment --
                // see the `add` comment above for why that distinction matters here too.
                call.invokeEdges.removeIf { it.end === stub }
                if (isConstruction && (call as Construction).constructor === stub) {
                    // Safe: Construction.constructor's setter only replaces `invokes` wholesale
                    // when assigned a non-null value (see the resync comment below); assigning
                    // null is a plain field write with no such side effect.
                    call.constructor = null
                }
                callsByRemovedStub.getOrPut(stub) { mutableListOf() }.add(call)
                invokesChanged = true
            }
        }

        if (isConstruction) {
            val constructionCall = call as Construction
            val constructorCandidates = constructionCall.invokes.filterIsInstance<Constructor>()
            // Construction.constructor is a separate backing field whose setter forwards a
            // non-null assignment to `invokes` *wholesale*
            // (`invokes = mutableListOf(value)`) -- syncing it unconditionally after every edit
            // would silently collapse a legitimate multi-candidate (ambiguous) `invokes` list down
            // to a single entry, exactly the kind of destructive replace `reconcileCalls` otherwise
            // goes out of its way to avoid for plain Calls. Only sync when there is EXACTLY one
            // candidate -- there, the setter's wholesale replace is a no-op (invokes already
            // contains just that one entry). With zero candidates it was already reset to null
            // above (if applicable); with more than one, we deliberately leave both `invokes` and
            // `constructor` untouched.
            if (
                constructorCandidates.size == 1 &&
                    constructionCall.constructor !== constructorCandidates.single()
            ) {
                constructionCall.constructor = constructorCandidates.single()
            }
        }

        if (invokesChanged) {
            // Mirrors the tail of SymbolResolver.decideInvokesBasedOnCandidates, which also keeps
            // the callee reference's refersTo in sync with `invokes`. Only done if it was not
            // already resolved -- e.g. to a Variable/Parameter for a dynamic/function-pointer
            // invoke -- which we must not overwrite.
            (call.callee as? Reference)?.let { callee ->
                if (callee.refersTo == null) {
                    callee.refersTo = call.invokes.firstOrNull()
                }
            }
            markCallerDirty(call, registeredPasses)
        }
    }

    var detachedAnyStub = false
    for ((stub, calls) in callsByRemovedStub) {
        // Every call that could possibly still invoke this stub shares its name, so it must
        // already be among candidateCalls -- but Function.calledBy is an exact, edge-backed
        // reverse list, so we use that (rather than re-deriving orphanhood from the candidate
        // list) to decide whether the stub is now fully orphaned.
        if (stub.calledBy.isEmpty()) {
            val skipDfgTeardown =
                connectInferredCallArgumentsMayHaveRun && stub.functionSummary.isNotEmpty()
            if (skipDfgTeardown) {
                log.warn(
                    "Not tearing down function-summary-derived DFG edges for inferred stub {} " +
                        "because they cannot be soundly removed here.",
                    stub.name,
                )
            } else {
                for (call in calls) {
                    tearDownFunctionSummaryDerivedDfgEdges(stub, call)
                }
            }
            detachInferredDeclaration(stub)
            detachedAnyStub = true
        }
    }
    return detachedAnyStub
}

/**
 * Whether [candidate] is actually a viable constructor for [construction] -- i.e. declared in the
 * record [construction] instantiates, if that record is already known. If it is not yet known (e.g.
 * [de.fraunhofer.aisec.cpg.passes.SymbolResolver.handleConstruction] has not run on this particular
 * Construction yet), we fall back to allowing the match; [isReachableFrom] still guards against
 * completely unrelated same-named constructors.
 */
private fun matchesConstructionTarget(construction: Construction, candidate: Constructor): Boolean {
    val recordDeclaration = construction.instantiates as? Record ?: construction.type.recordOrNull()
    return recordDeclaration == null || candidate.recordDeclaration == recordDeclaration
}

/**
 * Phase 2 for [Reference] candidates (variable/field accesses, i.e. explicitly NOT [Call] callees
 * -- see [reconcileExistingUsagesWithNewSymbols]). Unlike [Call.invokes], [Reference.refersTo] is a
 * single edge, so we cannot simply "add" a viable candidate the way [reconcileCalls] does; only one
 * resolution can exist at a time. Three cases, per the class-level design:
 * - Currently unresolved (`refersTo == null`): resolved to a viable new [Variable]/
 *   [de.fraunhofer.aisec.cpg.graph.declarations.Field], if one exists.
 * - Currently resolved to an *inferred* declaration: replaced by a viable new, real declaration,
 *   mirroring the stub-is-a-bridge behavior for calls. The old inferred declaration's DFG edges to
 *   this reference are torn down, and if it turns out to be referenced from nowhere else in the
 *   graph, it is detached entirely.
 * - Currently resolved to a real, non-inferred declaration: deliberately left untouched. Silently
 *   swapping an already-correctly-resolved reference for a new, same-named declaration based on a
 *   name/scope heuristic would be unsound -- shadowing and visibility rules differ per language,
 *   and there is no principled way to decide here whether the existing resolution or the new
 *   declaration is the "correct" one. This mirrors the deferred function-overload-selection
 *   concerns already documented for [reconcileCalls]'s ambiguity handling, just without the
 *   multi-edge escape hatch a [Call] has.
 *
 * Returns `true` if at least one stub was fully detached.
 */
private fun reconcileReferences(
    candidateReferences: List<Reference>,
    byName: Map<Symbol, List<Declaration>>,
    scopeManager: ScopeManager,
    registeredPasses: List<KClass<out Pass<out Node>>>,
): Boolean {
    var detachedAnyStub = false

    for (ref in candidateReferences) {
        val variableCandidates = byName[ref.symbolName]?.filterIsInstance<Variable>() ?: continue
        if (variableCandidates.isEmpty()) continue

        val existingTarget = ref.refersTo
        if (existingTarget != null && (existingTarget !is Variable || !existingTarget.isInferred)) {
            // Case 3, deliberately left alone -- see the function doc. This also conservatively
            // skips any already-resolved target that is not itself a Variable/Field (e.g. an
            // inferred Function reached via a function-pointer-style reference), which is out of
            // scope for this Reference-specific path.
            continue
        }
        // At this point existingTarget is either null, or an inferred Variable/Field.
        val inferredTarget = existingTarget as? Variable

        val realCandidate =
            variableCandidates.firstOrNull { candidate ->
                !candidate.isInferred && scopeManager.isReachableFrom(ref, candidate)
            } ?: continue

        if (inferredTarget != null) {
            tearDownReferenceDfgEdges(inferredTarget, ref)
            // Reference.refersTo's setter only ever APPENDS `this` to the new target's
            // usageEdges on (re)assignment -- it never removes the reverse edge from the OLD
            // target, which would otherwise leave a dangling usage edge on inferredTarget (and
            // make it look "still referenced" by the orphan check below, even after we just
            // repointed `ref` away from it).
            inferredTarget.usageEdges.removeIf { it.end === ref }
        }

        ref.refersTo = realCandidate
        markCallerDirty(ref, registeredPasses)

        if (inferredTarget != null && inferredTarget.usages.isEmpty()) {
            // ValueDeclaration.usages is the authoritative, edge-backed reverse list of every
            // Reference resolving to this declaration -- unlike a hand-built list derived from
            // candidateReferences (phase 1's cheap-filter shortlist), this stays correct even for
            // referrers phase 1 excludes (e.g. a dynamic/function-pointer call callee, filtered
            // out via `resolutionHelper !is Call`).
            detachInferredDeclaration(inferredTarget)
            detachedAnyStub = true
        }
    }

    return detachedAnyStub
}

/**
 * The caller-side counterpart of [reconcileCalls]/[reconcileReferences]'s edge surgery: marks the
 * nearest enclosing [Function] (or, if there is none -- e.g. a top-level/module-scope statement --
 * the enclosing [TranslationUnit], itself an EOGStarterHolder "to catch any static statements in
 * the TU") dirty for [SymbolResolver] and the DFG-family passes, so a later
 * [de.fraunhofer.aisec.cpg.runDirtyPasses] actually revisits [node].
 */
private fun markCallerDirty(node: Node, registeredPasses: List<KClass<out Pass<out Node>>>) {
    val enclosingFunction = node.firstParentOrNull<Function>()
    if (enclosingFunction != null) {
        enclosingFunction.markDirty<SymbolResolver>()
        enclosingFunction.markDfgRelatedPassesDirty(registeredPasses)
    } else {
        node.firstParentOrNull<TranslationUnit>()?.let {
            it.markDirty<SymbolResolver>()
            it.markDfgRelatedPassesDirty(registeredPasses)
        }
    }
}

/**
 * Removes the DFG edges [de.fraunhofer.aisec.cpg.passes.DFGPass.handleReference] attaches for a
 * [Reference] resolved to [stub] (see there): the read edge (`stub -> ref`) and/or the write edge
 * (`ref -> stub`), depending on [Reference.access]. Safe to call even if neither edge was ever
 * attached (e.g. because DFGPass's own `isGlobal`/`runsPointsToPassOrCfsDFG` gating skipped it) --
 * `removeIf` on an edge list that does not contain a matching edge is simply a no-op.
 */
private fun tearDownReferenceDfgEdges(stub: Declaration, ref: Reference) {
    ref.nextDFGEdges.removeIf { it.end === stub }
    ref.prevDFGEdges.removeIf { it.start === stub }
}

/**
 * Removes the "standard" DFG edges that DFGPass attached because [call] invoked [stub]: the
 * argument -> parameter edges (from [de.fraunhofer.aisec.cpg.helpers.Util.attachCallParameters]),
 * the receiver -> stub.receiver edge for member calls, and the "invoked function flows into the
 * call" edge. We must only touch edges whose source is one of THIS call's arguments/this stub,
 * since other calls may share the same stub (and thus the same parameter nodes).
 *
 * Note: [de.fraunhofer.aisec.cpg.helpers.Util.detachCallParameters] looks like the "obvious"
 * inverse of `attachCallParameters`, but it is pre-existing, unused, dead code with a bug (it
 * removes from `param.nextDFGEdges` after searching `param.prevDFGEdges`, which are different
 * mirrored collections) -- do not consolidate onto it without fixing that first.
 */
private fun tearDownStandardDfgEdges(stub: Function, call: Call) {
    for (param in stub.parameters) {
        param.prevDFGEdges.removeIf { it.start in call.arguments }
    }

    // Uses `===` (identity), like every other edge-removal check in this file, rather than `==`
    // (structural equality): two distinct receivers can be structurally equal per Node.equals
    // (e.g. synthetic "this" receivers sharing a placeholder location), and we must only ever
    // remove the edge that actually points at *this* stub's receiver.
    if (stub is Method && call is MemberCall) {
        stub.receiver?.let { receiver -> call.base?.nextDFGEdges?.removeIf { it.end === receiver } }
    }

    call.prevDFGEdges.removeIf { it.start === stub }
}

/**
 * Mirrors and undoes exactly the edges
 * [de.fraunhofer.aisec.cpg.passes.DFGPass.connectInferredCallArguments] adds for [call] based on
 * [stub]'s [Function.functionSummary]: the reverse arg <- stub-parameter edge (or, for a receiver
 * parameter, the reverse call.base <- stub.receiver edge). Only ever called once [stub] is fully
 * orphaned (see the caller), since some other call sharing the same stub could otherwise still
 * depend on these edges. Deliberately does not attempt to revert the `arg.access =
 * AccessValues.READWRITE` flip or the `arg.refersTo`-derived write-back edge that
 * `connectInferredCallArguments` also adds for by-reference parameters -- those encode "this
 * argument's underlying variable was (possibly) written by the call" and cannot be soundly reverted
 * without knowing whether some other, still-valid data flow also justifies them; leaving them in
 * place is a conservative over-approximation, not a dangling/leaked edge.
 */
private fun tearDownFunctionSummaryDerivedDfgEdges(stub: Function, call: Call) {
    for ((param, _) in stub.functionSummary) {
        if (param === (stub as? Method)?.receiver) {
            (call as? MemberCall)?.base?.prevDFGEdges?.removeIf { it.start === param }
        } else if (param is Parameter) {
            call.arguments.getOrNull(param.argumentIndex)?.prevDFGEdges?.removeIf {
                it.start === param
            }
        }
    }
}

/**
 * Detaches [declaration] from its AST parent and from the scope it was declared in. Does not
 * invalidate the symbol lookup cache itself -- callers that detach multiple declarations in one
 * pass should invalidate it once afterwards instead.
 */
private fun detachInferredDeclaration(declaration: Declaration) {
    when (val parent = declaration.astParent) {
        is Record ->
            // Constructor extends Method, so this must be checked first -- Record.addDeclaration
            // stores constructors in a separate constructorEdges/constructors collection, not in
            // methods, and mirrors this same is-Constructor-before-is-Method order. Record also
            // implements DeclarationHolder, but its `declarations` getter (unlike a plain
            // DeclarationHolder's) aggregates fields/methods/constructors/records into a freshly
            // built, read-only List rather than exposing a single backing MutableList, so we must
            // handle Record explicitly here instead of falling through to the generic
            // `is DeclarationHolder` branch below (which would silently no-op on it).
            if (declaration is Constructor) {
                parent.constructors.remove(declaration)
            } else if (declaration is Method) {
                parent.methods.remove(declaration)
            } else if (declaration is Field) {
                parent.fields.remove(declaration)
            }
        is DeclarationHolder -> {
            @Suppress("UNCHECKED_CAST")
            (parent.declarations as? MutableList<Declaration>)?.remove(declaration)
        }
        else -> {}
    }

    declaration.scope?.symbols?.get(declaration.symbol)?.remove(declaration)
}
