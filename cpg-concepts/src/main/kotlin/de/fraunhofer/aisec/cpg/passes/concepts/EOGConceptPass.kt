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
package de.fraunhofer.aisec.cpg.passes.concepts

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Backward
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.component
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.edges.flows.insertNodeAfterwardInEOGPath
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.graph.followDFGEdgesUntilHit
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.MapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.passes.EOGStarterLeastTUImportSorterWithCatchAfterTry
import de.fraunhofer.aisec.cpg.passes.EOGStarterPass
import kotlin.collections.set

typealias NodeToOverlayStateElement = MapLattice.Element<Node, PowersetLattice.Element<OverlayNode>>

typealias NodeToOverlayState = MapLattice<Node, PowersetLattice.Element<OverlayNode>>

/**
 * An abstract pass that is used to identify and create [Concept] and [Operation] nodes in the
 * graph.
 */
open class EOGConceptPass(ctx: TranslationContext) :
    EOGStarterPass(ctx, sort = EOGStarterLeastTUImportSorterWithCatchAfterTry) {

    /** Stores the current component in case we need it to look up some stuff. */
    var currentComponent: Component? = null

    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(node: Node) {
        currentComponent = node.firstParentOrNull<Component>()

        ctx.currentComponent = node.component
        val lattice = NodeToOverlayState(PowersetLattice<OverlayNode>())
        val startState = getInitialState(lattice, node)

        val nextEog = node.nextEOGEdges.toList()
        val finalState = lattice.iterateEOG(nextEog, startState, ::transfer)

        // We do not need to use the finalState here as generating the new objects in the iteration
        // already connects them to the underlying nodes.
        for ((underlyingNode, overlayNodes) in finalState) {
            overlayNodes.forEach {
                it.underlyingNode = underlyingNode
                if (it is Operation) {
                    // Connect with the EOG
                    underlyingNode.insertNodeAfterwardInEOGPath(it)
                    // Call the default DFG method for this operation.
                    it.setDFG()
                    // Add the operation to the concept.
                    it.concept.ops += it
                }
            }
        }
    }

    open fun handleCallExpression(
        state: NodeToOverlayStateElement,
        node: CallExpression,
    ): Collection<OverlayNode> {
        return emptySet()
    }

    open fun handleCallExpression(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: CallExpression,
    ): Collection<OverlayNode> {
        return emptySet()
    }

    open fun handleMemberCallExpression(
        state: NodeToOverlayStateElement,
        node: MemberCallExpression,
    ): Collection<OverlayNode> {
        return emptySet()
    }

    open fun handleMemberCallExpression(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: MemberCallExpression,
    ): Collection<OverlayNode> {
        return emptySet()
    }

    /**
     * This function is called for each node in the graph. It needs to be overridden by subclasses
     * to handle the specific node.
     */
    open fun handleNode(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: Node,
    ): Collection<OverlayNode> {
        return when (node) {
            is MemberCallExpression ->
                handleMemberCallExpression(lattice, state, node) +
                    handleMemberCallExpression(state, node)
            is CallExpression ->
                handleCallExpression(lattice, state, node) + handleCallExpression(state, node)
            else -> emptySet()
        }
    }

    open fun getInitialState(lattice: NodeToOverlayState, node: Node): NodeToOverlayStateElement {
        return lattice.bottom
    }

    fun transfer(
        lattice: Lattice<NodeToOverlayStateElement>,
        currentEdge: EvaluationOrder,
        currentState: NodeToOverlayStateElement,
    ): NodeToOverlayStateElement {
        val lattice = lattice as? NodeToOverlayState ?: return currentState
        val currentNode = currentEdge.end
        val addedOverlays = handleNode(lattice, currentState, currentNode).toSet()

        val filteredAddedOverlays =
            addedOverlays.filter { added ->
                currentState[currentNode]?.none { existing -> added == existing } != false &&
                    currentNode.overlays.none { existing ->
                        (existing as? OverlayNode)?.equalWithoutUnderlying(added) == true
                    }
            }

        return lattice.lub(
            currentState,
            NodeToOverlayStateElement(
                currentNode to
                    PowersetLattice.Element<OverlayNode>(*filteredAddedOverlays.toTypedArray())
            ),
        )
    }

    /**
     * Returns a list of nodes of type [T] fulfilling the [predicate] that are reachable from this
     * node via the backwards DFG.
     */
    inline fun <reified T : OverlayNode> Node.getOverlaysByPrevDFG(
        stateElement: NodeToOverlayStateElement,
        crossinline predicate: (T) -> Boolean = { true },
    ): List<T> {
        return this.followDFGEdgesUntilHit(
                collectFailedPaths = false,
                findAllPossiblePaths = false,
                direction = Backward(GraphToFollow.DFG),
            ) { node ->
                // find all nodes on a prev DFG path which an overlay node matching the predicate
                // either in the state, they are this node already or they have it in their
                // overlays. We do these three things because nodes may be added to the DFG after
                // running the pass (and are available only in the state) or they may have been
                // added before (so they aren't in the state but connected by the DFG or the overlay
                // edge).
                stateElement[node]?.filterIsInstance<T>()?.any(predicate) == true ||
                    node is T && predicate(node) ||
                    node.overlays.filterIsInstance<T>().any(predicate)
            }
            .fulfilled
            // The last nodes on the path are the ones we are interested in.
            .map { it.last() }
            .flatMap {
                // collect all "overlay" nodes
                stateElement[it] ?: setOf(it, *it.overlays.toTypedArray())
            }
            .filterIsInstance<T>() // discard not-relevant overlays
    }
}
