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
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Construction
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
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
 * dirty for [EvaluationOrderGraphPass]. In addition, for every such declaration we check whether an
 * inferred stub with the same symbol already exists in the live [ScopeManager] (i.e., a previous
 * call to this name could not be resolved and [de.fraunhofer.aisec.cpg.passes.inference.Inference]
 * created a placeholder [Function] for it). If so, every
 * [de.fraunhofer.aisec.cpg.graph.expressions.Call] that still invokes that stub has its stale
 * [de.fraunhofer.aisec.cpg.graph.expressions.Call.invokes] edge (and the DFG edges that
 * [de.fraunhofer.aisec.cpg.passes.DFGPass] attached because of it) removed, and its enclosing
 * function is marked dirty for [SymbolResolver] and [DFGPass] too. Once the stub is no longer
 * invoked by anyone, it is detached from the graph entirely.
 */
internal fun TranslationManager.updateIncrementally(
    result: TranslationResult,
    tu: TranslationUnit,
) {
    // Never changes within a single call, so compute it once and thread it through instead of
    // recomputing it per new function/call in markDfgRelatedPassesDirty/cleanUpStaleInferredStubs.
    val registeredPasses = result.finalCtx.config.registeredPasses.flatten()
    var detachedAnyStub = false

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

    val newFunctions = tu.allChildren<Function>()
    for (newFunction in newFunctions) {
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
        if (cleanUpStaleInferredStubs(result, newFunction, registeredPasses)) {
            detachedAnyStub = true
        }
    }

    // Invalidate once for the whole call instead of once per detached stub, and skip entirely on
    // the common path where nothing stale was found.
    if (detachedAnyStub) {
        result.finalCtx.scopeManager.invalidateSymbolLookupCache()
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
 * Looks for a pre-existing inferred [Function] stub that matches [newFunction]'s symbol (and
 * signature) in the same lookup scope and, if found, re-points/cleans-up everything that used to
 * reference the stub. Returns `true` if at least one stale stub was detached from the graph.
 */
private fun cleanUpStaleInferredStubs(
    result: TranslationResult,
    newFunction: Function,
    registeredPasses: List<KClass<out Pass<out Node>>>,
): Boolean {
    val scopeManager = result.finalCtx.scopeManager
    // Unqualified lookup only walks *up* the scope chain from the start scope (it never descends
    // into child scopes), so we must start at newFunction's own scope (e.g. its enclosing
    // RecordScope/NamespaceScope) rather than unconditionally at the global scope. Otherwise stubs
    // registered in a class/namespace scope would never be found.
    val candidates =
        scopeManager.lookupSymbolByName(
            newFunction.name,
            newFunction.language,
            startScope = newFunction.scope ?: scopeManager.globalScope,
        )

    val stubs =
        candidates.filterIsInstance<Function>().filter { stub ->
            stub.isInferred &&
                stub !== newFunction &&
                // Only treat same-named stubs as stale if newFunction is actually a viable
                // resolution target for the call(s) that produced this stub.
                //
                // [Function.matchesSignature] is designed to be called as
                // `candidate.matchesSignature(callArgumentTypes)` (see
                // SymbolResolver.resolveWithArguments): it walks the *candidate's* parameters and
                // requires them to consume the entire `signature` list, only tolerating a
                // candidate with MORE parameters than `signature` if `useDefaultArguments` is set
                // and the extra ones have defaults.
                //
                // A stub's parameters are typed 1:1 from the original call's argument types (see
                // Inference.createInferredParameters), so stub.parameters.map { it.type } is
                // exactly that original call's argument-type signature. The previous version of
                // this check called it backwards -- `stub.matchesSignature(newFunction's
                // parameter types)` -- which fails whenever the real function has MORE parameters
                // than the call the stub was inferred from (e.g. trailing default/optional
                // parameters), since the stub's (few) parameters could never consume the real
                // function's (more) parameter types. Calling it in the correct direction,
                // `newFunction.matchesSignature(stub's parameter types)`, mirrors how
                // SymbolResolver itself would resolve that original call against newFunction, and
                // correctly allows newFunction to have extra trailing default parameters that the
                // call simply didn't supply.
                newFunction.matchesSignature(
                    stub.parameters.map { it.type },
                    useDefaultArguments = true,
                ) is SignatureMatches
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

    var detachedAnyStub = false
    for (stub in stubs) {
        // Tearing down the function-summary-derived reverse edges (arg <- stub-parameter, plus
        // READWRITE access flips) correctly would require knowing whether some other, still-valid
        // edge also justifies them, which we cannot determine soundly here. We therefore skip only
        // the DFG-edge teardown below for this stub, leaving those (and the other, "standard") DFG
        // edges in place rather than risk leaving a partially/incorrectly cleaned-up graph.
        //
        // We must NOT also skip updating `call.invokes` and marking the caller dirty, though: the
        // real function now exists, so leaving `call.invokes` pointed at the dead stub would strand
        // the call there forever -- nothing would ever mark it dirty again, so a later
        // runDirtyPasses would never revisit it. So we always remove the stale invokes edge and
        // always mark the caller dirty for re-resolution, and only conditionally skip the DFG-edge
        // surgery.
        val skipDfgTeardown =
            connectInferredCallArgumentsMayHaveRun && stub.functionSummary.isNotEmpty()
        if (skipDfgTeardown) {
            log.warn(
                "Not tearing down DFG edges for inferred stub {} because it has " +
                    "function-summary-derived DFG edges that cannot be soundly removed here. The " +
                    "stale invokes edge is still removed and the caller still marked dirty.",
                stub.name,
            )
        }

        // Take a snapshot: we are about to mutate stub.calledBy (via the mirrored invokes edge)
        // while iterating over it.
        val staleCalls = stub.calledBy.toList()

        for (call in staleCalls) {
            if (!skipDfgTeardown) {
                tearDownStandardDfgEdges(stub, call)
            }

            // Remove the stale invokes edge itself. This also removes the mirrored entry in
            // stub.calledByEdges.
            call.invokeEdges.removeIf { it.end === stub }

            // Construction.constructor is a separate backing field that the setter forwards
            // one-directionally to `invokes` (setting `constructor` also sets `invokes`, but not
            // the other way around) -- so clearing `invokes` above leaves
            // `construction.constructor`
            // still dangling at the now-stale stub until SymbolResolver happens to rerun. Reset it
            // explicitly here so there is no window of inconsistency between the two. Note: when
            // `anonymousClass` is set, the getter ignores this backing field entirely, making this
            // a no-op for that path; correctness there instead comes from detachInferredDeclaration
            // removing the stub from the anonymous class's `constructors` a few lines below.
            if (call is Construction && call.constructor === stub) {
                call.constructor = null
            }

            // The caller is where SymbolResolver needs to re-resolve the call, and where DFGPass
            // needs to re-attach the argument/parameter and invoked-function/call edges once the
            // call resolves to the new, real function. Some calls (e.g. top-level/module-scope
            // statements) have no enclosing Function -- TranslationUnit is itself an
            // EOGStarterHolder ("to catch any static statements in the TU"), so fall back to
            // marking the enclosing TranslationUnit dirty instead of silently doing nothing; the
            // stale invokes/DFG edges above were already unconditionally removed, so leaving this
            // case unmarked would strand the call worse off than before cleanup ran.
            val enclosingFunction = call.firstParentOrNull<Function>()
            if (enclosingFunction != null) {
                enclosingFunction.markDirty<SymbolResolver>()
                enclosingFunction.markDfgRelatedPassesDirty(registeredPasses)
            } else {
                call.firstParentOrNull<TranslationUnit>()?.let {
                    it.markDirty<SymbolResolver>()
                    it.markDfgRelatedPassesDirty(registeredPasses)
                }
            }
        }

        // If nobody invokes the stub anymore, it is dead weight: detach it from the AST and from
        // the scope's symbol table so it doesn't linger as a phantom declaration.
        if (stub.calledBy.isEmpty()) {
            if (skipDfgTeardown) {
                // staleCalls was a full snapshot of stub.calledBy taken before this loop, and
                // every one of those calls just had its invokes edge to stub removed above, so
                // stub.calledBy being empty here means none of them (or anyone else) can still
                // need the DFG edges we left alone earlier -- it is now safe to tear those down
                // too, without the "some other call might still justify them" risk that justified
                // skipping this at the time.
                for (call in staleCalls) {
                    tearDownStandardDfgEdges(stub, call)
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
            // methods, and mirrors this same is-Constructor-before-is-Method order.
            if (declaration is Constructor) {
                parent.constructors.remove(declaration)
            } else if (declaration is Method) {
                parent.methods.remove(declaration)
            }
        is DeclarationHolder -> {
            @Suppress("UNCHECKED_CAST")
            (parent.declarations as? MutableList<Declaration>)?.remove(declaration)
        }
        else -> {}
    }

    declaration.scope?.symbols?.get(declaration.symbol)?.remove(declaration)
}
