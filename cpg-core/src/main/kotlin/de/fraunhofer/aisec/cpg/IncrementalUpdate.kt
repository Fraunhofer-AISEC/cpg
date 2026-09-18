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
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.PointsToPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
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
 * Every new [Function]-like declaration in [tu] is marked dirty for [SymbolResolver] and [DFGPass].
 * In addition, for every such declaration we check whether an inferred stub with the same symbol
 * already exists in the live [ScopeManager] (i.e., a previous call to this name could not be
 * resolved and [de.fraunhofer.aisec.cpg.passes.inference.Inference] created a placeholder
 * [Function] for it). If so, every [de.fraunhofer.aisec.cpg.graph.expressions.Call] that still
 * invokes that stub has its stale [de.fraunhofer.aisec.cpg.graph.expressions.Call.invokes] edge
 * (and the DFG edges that [de.fraunhofer.aisec.cpg.passes.DFGPass] attached because of it) removed,
 * and its enclosing function is marked dirty for [SymbolResolver] and [DFGPass] too. Once the stub
 * is no longer invoked by anyone, it is detached from the graph entirely.
 */
internal fun TranslationManager.updateIncrementally(
    result: TranslationResult,
    tu: TranslationUnit,
) {
    // Never changes within a single call, so compute it once and thread it through instead of
    // recomputing it per new function/call in markDfgRelatedPassesDirty/cleanUpStaleInferredStubs.
    val registeredPasses = result.finalCtx.config.registeredPasses.flatten()
    var detachedAnyStub = false

    val newFunctions = tu.allChildren<Function>()
    for (newFunction in newFunctions) {
        newFunction.markDirty<SymbolResolver>()
        // DFGPass connects a resolved call's arguments/return value to the invoked function's
        // parameters based on `Call.invokes`; since SymbolResolver has not run on this new
        // function yet (it is not invoked by anyone so far), DFGPass has nothing to do for it yet
        // either -- but it must be re-run once it starts being called (see below).
        newFunction.markDfgRelatedPassesDirty(registeredPasses)
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
 * [DFGPass.handleCall]'s own gating, whichever of [PointsToPass] / [ControlFlowSensitiveDFGPass] is
 * actually registered -- one of these (rather than [DFGPass] itself) attaches the
 * argument-to-parameter DFG edges once either is registered (the default).
 */
private fun Function.markDfgRelatedPassesDirty(registeredPasses: List<KClass<out Pass<out Node>>>) {
    markDirty<DFGPass>()
    when {
        registeredPasses.contains(PointsToPass::class) -> markDirty<PointsToPass>()
        registeredPasses.contains(ControlFlowSensitiveDFGPass::class) ->
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

    val newSignature = newFunction.parameters.map { it.type }
    val stubs =
        candidates.filterIsInstance<Function>().filter {
            it.isInferred &&
                it !== newFunction &&
                // Only treat same-named stubs as stale if their (inferred) signature is actually
                // compatible with the new declaration's signature; otherwise we might rip out
                // edges that still belong to a different, still-unresolved overload.
                it.matchesSignature(newSignature) is SignatureMatches
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
                // Remove the argument -> parameter DFG edges that DFGPass/Util.attachCallParameters
                // added specifically because of this call invoking the stub. We must only touch
                // edges whose source is one of THIS call's arguments, since other calls may share
                // the same stub (and thus the same parameter nodes).
                //
                // Note: Util.detachCallParameters looks like the "obvious" inverse of
                // Util.attachCallParameters, but it is pre-existing, unused, dead code with a bug
                // (it removes from param.nextDFGEdges after searching param.prevDFGEdges, which are
                // different mirrored collections) -- do not consolidate onto it without fixing that
                // first.
                for (param in stub.parameters) {
                    param.prevDFGEdges.removeIf { it.start in call.arguments }
                }

                // Remove the receiver -> stub.receiver DFG edge for member calls.
                if (stub is Method && call is MemberCall) {
                    stub.receiver?.let { receiver ->
                        call.base?.nextDFGEdges?.removeIf { it.end == receiver }
                    }
                }

                // Remove the "invoked function flows into the call" DFG edge that DFGPass adds for
                // every entry of call.invokes.
                call.prevDFGEdges.removeIf { it.start === stub }
            }

            // Remove the stale invokes edge itself. This also removes the mirrored entry in
            // stub.calledByEdges.
            call.invokeEdges.removeIf { it.end === stub }

            // The caller is where SymbolResolver needs to re-resolve the call, and where DFGPass
            // needs to re-attach the argument/parameter and invoked-function/call edges once the
            // call resolves to the new, real function.
            call.firstParentOrNull<Function>()?.let {
                it.markDirty<SymbolResolver>()
                it.markDfgRelatedPassesDirty(registeredPasses)
            }
        }

        // If nobody invokes the stub anymore, it is dead weight: detach it from the AST and from
        // the scope's symbol table so it doesn't linger as a phantom declaration.
        if (stub.calledBy.isEmpty()) {
            detachInferredDeclaration(stub)
            detachedAnyStub = true
        }
    }
    return detachedAnyStub
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
