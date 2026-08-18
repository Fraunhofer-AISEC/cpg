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
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
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
 * Because a node can be reached via more than one incoming [EvaluationOrder] edge (e.g., the first
 * node after an `if`/`else` merges), [SymbolResolver.transfer] is offered every such node once per
 * incoming edge. We track the set of already-handled nodes in a [PowersetLattice] so that
 * [SymbolResolver.handle] is still only ever *applied* once per node.
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
    // Nodes that may replace themselves in the AST/EOG (see the KDoc above) are deferred here and
    // only handled once the EOG traversal itself is done.
    val deferredOperatorNodes = mutableListOf<Node>()

    val lattice = PowersetLattice<Node>()
    val startState = PowersetLattice.Element<Node>()
    val (_, timeout) =
        lattice.iterateEOG(
            t.nextEOGEdges,
            startState,
            transformation = { l, edge, state -> transfer(l, edge, state, deferredOperatorNodes) },
        )
    if (timeout) {
        log.warn("Could not compute final state for EOG starter {} (due to timeout)", t.name)
    }

    deferredOperatorNodes.forEach { handle(it) }
}

/**
 * The state-transfer function used by [acceptWithIterateEOG]. Applies [SymbolResolver.handle] to
 * [EvaluationOrder.end] the first time it is reached (tracked in [state]), then records it as
 * handled. [BinaryOperator]/[UnaryOperator] nodes are special-cased: their type is propagated
 * immediately (see [propagateOperatorType]), but the AST-mutating
 * [SymbolResolver.handleOverloadedOperator] is deferred by appending them to
 * [deferredOperatorNodes] instead of calling [SymbolResolver.handle] on them right away.
 */
private suspend fun SymbolResolver.transfer(
    lattice: Lattice<PowersetLattice.Element<Node>>,
    currentEdge: EvaluationOrder,
    state: PowersetLattice.Element<Node>,
    deferredOperatorNodes: MutableList<Node>,
): PowersetLattice.Element<Node> {
    val lattice = lattice as? PowersetLattice<Node> ?: return state
    val node = currentEdge.end
    if (node in state) {
        return state
    }

    if (node is BinaryOperator || node is UnaryOperator) {
        propagateOperatorType(node)
        deferredOperatorNodes += node
    } else {
        handle(node)
    }

    return lattice.lub(state, PowersetLattice.Element(node), true)
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
