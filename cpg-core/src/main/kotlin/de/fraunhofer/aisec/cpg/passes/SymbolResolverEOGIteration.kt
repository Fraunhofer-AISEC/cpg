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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log

/**
 * This function resolves symbols for the given EOG starter [t] (see
 * [de.fraunhofer.aisec.cpg.graph.EOGStarterHolder] - e.g. a [Function], but also a translation
 * unit, record, namespace, or a field/variable with an initializer) by driving
 * [SymbolResolver.handle] with [Lattice.iterateEOG] - the same worklist/fixpoint engine used by
 * [PointsToPass], [ControlDependenceGraphPass] and [UnreachableEOGPass] - instead of the
 * [de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker]-based linear traversal that the
 * default (non-experimental) code path in [SymbolResolver.accept] uses.
 *
 * Importantly, this does *not* reimplement resolution: [SymbolResolver.handle] and everything it
 * dispatches to (member/call/construction/operator-overload resolution, access control, implicit
 * receivers, etc.) is reused verbatim and resolves purely from the (already fully populated by the
 * frontends before any pass runs) [de.fraunhofer.aisec.cpg.graph.scopes.Scope] tree and from
 * already-propagated [de.fraunhofer.aisec.cpg.graph.types.HasType] information - exactly like the
 * default traversal. Only the *traversal mechanism* changes here; nothing about resolution is
 * flow-sensitive (yet).
 *
 * We don't need any genuine flow-sensitive state for that: [Lattice.iterateEOG] is used purely to
 * get a termination-safe, EOG-respecting traversal (correctly handling loops via its fixpoint
 * machinery), not to compute anything. The [PowersetLattice] state threaded through [transfer] is
 * therefore never actually populated; it exists only because [Lattice.iterateEOG] requires *some*
 * lattice element to track convergence, and an always-equal, always-bottom element is sufficient to
 * guarantee that every reachable edge is still visited at least once (see
 * [de.fraunhofer.aisec.cpg.helpers.functional.Lattice.iterateEogInternal]'s `oldGlobalIt == null`
 * case) while loop back-edges still converge instead of being reprocessed forever.
 *
 * A node can be reached via more than one incoming [EvaluationOrder] edge - not just a loop back-
 * edge (which the engine's fixpoint machinery already only re-processes until convergence), but
 * also a genuine, non-cyclic merge, e.g. the first node after an `if`/`else` where neither branch
 * terminates. [transfer] is offered such a node once per incoming edge, each with its own, separate
 * slice of lattice state - so tracking "already handled" *in* that per-edge state would only catch
 * the loop-reconvergence case, not this one. We therefore track already-handled nodes in
 * [handledNodes], a plain, closure-captured set shared across every [transfer] call for this
 * starter (safe without synchronization, since the engine drives them strictly sequentially), so
 * that [SymbolResolver.handle] is only ever *applied* once per node no matter how many incoming
 * edges it has.
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

    val lattice = PowersetLattice<Node>()
    val startState = PowersetLattice.Element<Node>()
    val (_, timeout) =
        lattice.iterateEOG(
            t.nextEOGEdges,
            startState,
            transformation = { l, edge, state ->
                transfer(l, edge, state, handledNodes, deferredOperatorNodes)
            },
        )
    if (timeout) {
        log.warn("Could not compute final state for EOG starter {} (due to timeout)", t.name)
    }

    deferredOperatorNodes.forEach {
        jumpToScope(it)
        handle(it)
    }
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
 * The state-transfer function used by [acceptWithIterateEOG]. Applies [SymbolResolver.handle] to
 * [EvaluationOrder.end] the first time it is reached (tracked in [handledNodes], not [state] - see
 * the KDoc on [acceptWithIterateEOG]). [BinaryOperator]/[UnaryOperator] nodes are special-cased:
 * their type is propagated immediately (see [propagateOperatorType]), but the AST-mutating
 * [SymbolResolver.handleOverloadedOperator] is deferred by appending them to
 * [deferredOperatorNodes] instead of calling [SymbolResolver.handle] on them right away.
 */
private suspend fun SymbolResolver.transfer(
    lattice: Lattice<PowersetLattice.Element<Node>>,
    currentEdge: EvaluationOrder,
    state: PowersetLattice.Element<Node>,
    handledNodes: IdentitySet<Node>,
    deferredOperatorNodes: MutableList<Node>,
): PowersetLattice.Element<Node> {
    val node = currentEdge.end
    if (!handledNodes.add(node)) {
        return state
    }

    jumpToScope(node)
    if (node is BinaryOperator || node is UnaryOperator) {
        propagateOperatorType(node)
        deferredOperatorNodes += node
    } else {
        handle(node)
    }

    return state
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
