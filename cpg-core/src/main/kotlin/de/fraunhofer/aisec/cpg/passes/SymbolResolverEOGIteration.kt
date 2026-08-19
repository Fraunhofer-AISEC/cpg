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

import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.scopes.LocalScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.functional.ConcurrentMapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log
import kotlinx.coroutines.runBlocking

/**
 * A mapping of [LocalScope]s (block/loop/catch/comprehension scopes - see
 * [de.fraunhofer.aisec.cpg.ScopeManager.enterScope]'s dispatch) to the set of [Declaration]s that
 * have been reached, along the specific EOG path represented by this lattice element, by the
 * program point it is associated with. This is the actual flow-sensitive state threaded through
 * [Lattice.iterateEOG]: unlike every other kind of [Scope] (global/namespace/record/function, whose
 * [Scope.symbols] is fully known before any pass runs and is therefore read directly, unaffected by
 * EOG position), a [LocalScope]'s visible declarations genuinely depend on how far the EOG has been
 * traversed - most simply, "used before declared" within the same block.
 */
typealias LocalDeclarationLattice =
    ConcurrentMapLattice<LocalScope, PowersetLattice.Element<Declaration>>

typealias LocalDeclarationElement =
    ConcurrentMapLattice.Element<LocalScope, PowersetLattice.Element<Declaration>>

/**
 * This function resolves symbols for the given EOG starter [t] (see
 * [de.fraunhofer.aisec.cpg.graph.EOGStarterHolder] - e.g. a [Function], but also a translation
 * unit, record, namespace, or a field/variable with an initializer) by driving
 * [SymbolResolver.handle] with [Lattice.iterateEOG] - the same worklist/fixpoint engine used by
 * [PointsToPass], [ControlDependenceGraphPass] and [UnreachableEOGPass] - instead of the
 * [de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker]-based linear traversal that the
 * default (non-experimental) code path in [SymbolResolver.accept] uses.
 *
 * Resolution is reused verbatim from [SymbolResolver.handle] and everything it dispatches to
 * (member/call/construction/operator-overload resolution, access control, implicit receivers,
 * etc.); only the visibility of [LocalScope]-declared symbols is genuinely flow-sensitive, via
 * [LocalDeclarationLattice] and [SymbolResolver.localSymbolsOverride]. Every other [Scope] kind is
 * still read directly from [Scope.symbols], exactly like the default traversal - both because
 * that's correct (a global/record/namespace symbol's visibility never depends on the EOG) and
 * because most of it (parameters, fields, globals) is never reached by any EOG walk at all.
 *
 * A node can be reached via more than one incoming [EvaluationOrder] edge - not just a loop back-
 * edge (which the engine's fixpoint machinery already only re-processes until convergence), but
 * also a genuine, non-cyclic merge, e.g. the first node after an `if`/`else` where neither branch
 * terminates. [transfer] is offered such a node once per incoming edge, each with its own, separate
 * slice of lattice state. [LocalDeclarationLattice]'s own `lub` (a union, at both the map and the
 * inner-set level) is exactly the merge semantics we want for the *flow state* here - a declaration
 * reached along *either* incoming path is considered reached (matching this tool's general lean
 * towards best-effort resolution over strict soundness) - but applying [SymbolResolver.handle]
 * itself needs a *separate*, non-lattice, all-or-nothing "already handled" marker: tracking that in
 * the per-edge lattice state would only catch the loop-reconvergence case, not a true merge, since
 * neither incoming edge's own state has a record of the other having already processed their shared
 * successor. [handledNodes] is that marker: a plain, closure-captured identity set shared across
 * every [transfer] call for this starter, safe without synchronization since the engine drives them
 * strictly sequentially.
 *
 * [SymbolResolver.handleOverloadedOperator] additionally physically replaces the
 * [BinaryOperator]/[UnaryOperator] node with an
 * [de.fraunhofer.aisec.cpg.graph.expressions.OperatorCall], rewiring its EOG edges in the process.
 * [Lattice.iterateEOG] determines how to continue the traversal by reading
 * [EvaluationOrder.end]`.nextEOGEdges` of the edge it just processed - but a replaced node is
 * disconnected and has no outgoing EOG edges of its own anymore, so mutating the graph mid-
 * traversal would make the engine think the EOG ends right there and abandon everything after it.
 * We therefore only run [SymbolResolver.handle] on [BinaryOperator]/[UnaryOperator] nodes (which is
 * the only way [SymbolResolver.handleOverloadedOperator] is reached, since [SymbolResolver.handle]
 * dispatches [de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess] and
 * [de.fraunhofer.aisec.cpg.graph.expressions.Call] - the only other
 * [de.fraunhofer.aisec.cpg.graph.HasOverloadedOperation] implementers - to their own handlers
 * first) *after* the EOG traversal has fully finished, in the order they were encountered.
 */
fun SymbolResolver.acceptWithIterateEOG(t: Node) {
    // Nodes for which SymbolResolver.handle has already been applied, tracked globally across the
    // whole traversal (see the KDoc above for why this can't live in the per-edge lattice state).
    val handledNodes = identitySetOf<Node>()

    // Nodes that may replace themselves in the AST/EOG (see the KDoc above) are deferred here and
    // only handled once the EOG traversal itself is done.
    val deferredOperatorNodes = mutableListOf<Node>()

    // The scope t itself introduces (e.g. a Function's FunctionScope), used to tell apart a
    // LocalScope that belongs to this starter's own traversal (and is therefore genuinely
    // flow-sensitive here) from one belonging to an *enclosing*, already fully-resolved starter
    // (e.g. a captured variable in an outer function, when t is a nested function/lambda) - the
    // latter must fall back to the ordinary, static Scope.symbols lookup instead of appearing
    // "not yet declared".
    val starterScope = ctx.scopeManager.lookupScope(t)

    val lattice = LocalDeclarationLattice(PowersetLattice<Declaration>())
    var startState = LocalDeclarationElement()

    // Some declarations belonging to a LocalScope are never themselves the *target* of an EOG
    // edge - e.g. a CatchClause's exception parameter, the individual target Variables of a
    // tuple/multiple declaration, or a locally-declared function prototype (itself its own,
    // separate EOG starter, possibly with a nonempty nextEOGEdges of its own for evaluating a
    // default argument value - but never reached *from* this starter's own traversal, since
    // nothing has an edge pointing into it) - exactly like a Function's Parameters are never
    // reached via EOG either. Since these are declared "atomically" as part of a larger construct
    // rather than as an ordinary, sequential statement, they must be visible from the very start
    // rather than waiting to be "reached" by a transfer() call that will never come. The relevant
    // criterion is having no *incoming* EOG edge; an unrelated, nonempty outgoing chain of its own
    // doesn't change that nothing in *this* traversal will ever visit it.
    val preseeded =
        (t as? AstNode)
            ?.allChildren<Declaration>()
            ?.filter { it.scope is LocalScope && it.prevEOGEdges.isEmpty() }
            ?.groupBy { it.scope as LocalScope } ?: emptyMap()
    if (preseeded.isNotEmpty()) {
        val preseededElement =
            LocalDeclarationElement(
                preseeded.map { (scope, decls) ->
                    scope to PowersetLattice.Element(*decls.toTypedArray())
                }
            )
        startState = runBlocking { lattice.lub(startState, preseededElement, true) }
    }

    val (_, timeout) =
        lattice.iterateEOG(
            t.nextEOGEdges,
            startState,
            transformation = { l, edge, state ->
                transfer(l, edge, state, handledNodes, deferredOperatorNodes, starterScope)
            },
        )
    if (timeout) {
        log.warn("Could not compute final state for EOG starter {} (due to timeout)", t.name)
    }

    deferredOperatorNodes.forEach {
        jumpToScope(it)
        handle(it)
    }

    localSymbolsOverride = null
}

/**
 * Whether [scope] is (transitively) nested within [starterScope], i.e. whether it belongs to the
 * EOG starter currently being processed rather than to an enclosing one. If [starterScope] is
 * `null` (the starter itself introduces no scope, e.g. a bare field/variable initializer), we
 * conservatively treat every [LocalScope] as belonging to it - such starters are simple enough (and
 * any nested comprehension/lambda-with-outer-capture inside one is rare enough) that this is an
 * acceptable simplification for now.
 */
private fun isWithinStarter(scope: Scope, starterScope: Scope?): Boolean {
    if (starterScope == null) {
        return true
    }
    var current: Scope? = scope
    while (current != null) {
        if (current === starterScope) {
            return true
        }
        current = current.parent
    }
    return false
}

/**
 * The state-transfer function used by [acceptWithIterateEOG]. If [EvaluationOrder.end] is a
 * [Declaration] in a [LocalScope], it is pushed into [state]. [SymbolResolver.handle] is then
 * applied to the node the first time it is reached (tracked in [handledNodes], not [state] - see
 * the KDoc on [acceptWithIterateEOG]), with [SymbolResolver.localSymbolsOverride] pointed at
 * [state] so [SymbolResolver.handleReference] resolves flow-sensitively for [LocalScope]s.
 * [BinaryOperator]/[UnaryOperator] nodes are special-cased: their type is propagated immediately
 * (see [propagateOperatorType]), but the AST-mutating [SymbolResolver.handleOverloadedOperator] is
 * deferred by appending them to [deferredOperatorNodes] instead of calling [SymbolResolver.handle]
 * on them right away.
 */
private suspend fun SymbolResolver.transfer(
    lattice: Lattice<LocalDeclarationElement>,
    currentEdge: EvaluationOrder,
    state: LocalDeclarationElement,
    handledNodes: IdentitySet<Node>,
    deferredOperatorNodes: MutableList<Node>,
    starterScope: Scope?,
): LocalDeclarationElement {
    val lattice = lattice as? LocalDeclarationLattice ?: return state
    val node = currentEdge.end

    var newState = state
    val declarationScope = (node as? Declaration)?.scope
    if (declarationScope is LocalScope) {
        newState =
            lattice.lub(
                newState,
                LocalDeclarationElement(declarationScope to PowersetLattice.Element(node)),
                true,
            )
    }

    if (!handledNodes.add(node)) {
        return newState
    }

    jumpToScope(node)
    localSymbolsOverride = { scope, symbol ->
        if (scope is LocalScope && isWithinStarter(scope, starterScope)) {
            newState[scope]?.filter { it.name.localName == symbol } ?: emptyList()
        } else {
            // Not a LocalScope we're tracking flow-sensitively (either not a LocalScope at all,
            // or one belonging to an enclosing, already-resolved starter): fall back to the
            // default, static Scope.symbols[symbol] lookup.
            null
        }
    }

    if (node is BinaryOperator || node is UnaryOperator) {
        propagateOperatorType(node)
        deferredOperatorNodes += node
    } else {
        handle(node)
    }

    return newState
}

/**
 * Several of the handlers reached through [SymbolResolver.handle] (e.g.
 * [SymbolResolver.handleReference]'s implicit-receiver fallback, which reads
 * [de.fraunhofer.aisec.cpg.ScopeManager.currentRecord]) rely on
 * [de.fraunhofer.aisec.cpg.ScopeManager.currentScope] reflecting [node]'s own scope, exactly like
 * [de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker] keeps it in sync while walking.
 * [Lattice.iterateEOG] has no notion of "current scope", so we have to update it ourselves before
 * handling each node.
 */
private fun SymbolResolver.jumpToScope(node: Node) {
    if (scopeManager.currentScope != node.scope) {
        scopeManager.jumpTo(node.scope)
    }
}

/**
 * [SymbolResolver.handle] does not compute the type of a [BinaryOperator] or [UnaryOperator]
 * itself; normally, it relies on [de.fraunhofer.aisec.cpg.graph.types.HasType]'s reactive
 * [de.fraunhofer.aisec.cpg.graph.types.HasType.TypeObserver] mechanism to propagate the type of
 * [BinaryOperator.lhs]/[BinaryOperator.rhs] (or [UnaryOperator.input]) once they are resolved. That
 * mechanism can be switched off entirely via
 * [de.fraunhofer.aisec.cpg.TranslationConfiguration.Builder.disableTypeObserver], in which case
 * nothing else computes these types. Since the EOG guarantees [node]'s operands were already
 * handled (and thus have their final type) by the time [node] itself is reached, we can compute the
 * type here directly, exactly mirroring what the reactive path would have done.
 */
private fun SymbolResolver.propagateOperatorType(node: Node) {
    when (node) {
        is BinaryOperator ->
            node.type =
                node.language.propagateTypeOfBinaryOperation(
                    node.operatorCode,
                    node.lhs.type,
                    node.rhs.type,
                    node,
                )
        is UnaryOperator ->
            node.type =
                node.language.propagateTypeOfUnaryOperation(node.operatorCode, node.input.type)
    }
}
