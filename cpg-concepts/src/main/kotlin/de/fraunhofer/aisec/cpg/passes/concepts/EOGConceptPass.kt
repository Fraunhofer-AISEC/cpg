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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.edges.flows.insertNodeAfterwardInEOGPath
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.MapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn

typealias NodeToOverlayStateElement = MapLattice.Element<Node, PowersetLattice.Element<OverlayNode>>

typealias NodeToOverlayState = MapLattice<Node, PowersetLattice.Element<OverlayNode>>

/**
 * An abstract pass that is used to identify and create [Concept] and [Operation] nodes in the
 * graph. It uses the fixpoint-iteration to traverse the graph and create the nodes. It accounts for
 * all possible EOG paths reaching a node. This is different to the [ConceptPass] which fails to
 * account for the fact that nodes may be reachable by different EOG paths.
 *
 * Important information for classes implementing this pass:
 * * The following methods can be overridden to handle specific nodes:
 *     - [handleCallExpression] (either the simplified version or the one with the lattice).
 *     - [handleMemberCallExpression] (either the simplified version or the one with the lattice).
 * * These methods must return a collection of [OverlayNode]s that are created for the given node.
 *   They must not create [OverlayNode]s for other nodes than the one passed as an argument!
 * * These methods must not connect the created [OverlayNode]s to the underlying node! This is done
 *   in the pass itself after having collected all overlays. Use a builder based with the flag
 *   `connect` set to `false` to do this.
 */
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(DFGPass::class)
@DependsOn(ControlFlowSensitiveDFGPass::class, softDependency = true)
open class EOGConceptPass(ctx: TranslationContext) :
    EOGStarterPass(ctx, sort = EOGStarterLeastTUImportCatchLastSorter) {

    /** Stores the current component in case we need it to look up some stuff. */
    var currentComponent: Component? = null

    override fun cleanup() {
        // Nothing to do
    }

    override fun finalCleanup() {
        val finalState = intermediateState ?: return

        // We set the underlying node based on the final state
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
                } else if (it is Concept) {
                    // Call the default DFG method for this operation.
                    it.setDFG()
                }
            }
        }
    }

    override fun accept(node: Node) {
        currentComponent = node.firstParentOrNull<Component>()

        ctx.currentComponent = node.component

        val lattice = NodeToOverlayState(PowersetLattice<OverlayNode>())
        val startState = intermediateState ?: getInitialState(lattice, node)

        val nextEog = node.nextEOGEdges.toList()
        intermediateState = lattice.iterateEOG(nextEog, startState, ::transfer)
    }

    /**
     * Generates [OverlayNode]s belonging to the given [node]. The [state] contains a map of nodes
     * to their respective [OverlayNode]s created by this instance of the pass.
     */
    open fun handleCallExpression(
        state: NodeToOverlayStateElement,
        node: CallExpression,
    ): Collection<OverlayNode> {
        return emptySet()
    }

    /**
     * Generates [OverlayNode]s belonging to the given [node]. The [state] contains a map of nodes
     * to their respective [OverlayNode]s created by this instance of the pass.
     *
     * This is the advanced version and passes the [lattice] in case the [state] should be
     * manipulated. We do not recommend using this!
     */
    open fun handleCallExpression(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: CallExpression,
    ): Collection<OverlayNode> {
        return emptySet()
    }

    /**
     * Generates [OverlayNode]s belonging to the given [node]. The [state] contains a map of nodes
     * to their respective [OverlayNode]s created by this instance of the pass.
     */
    open fun handleMemberCallExpression(
        state: NodeToOverlayStateElement,
        node: MemberCallExpression,
    ): Collection<OverlayNode> {
        return emptySet()
    }

    /**
     * Generates [OverlayNode]s belonging to the given [node]. The [state] contains a map of nodes
     * to their respective [OverlayNode]s created by this instance of the pass.
     *
     * This is the advanced version and passes the [lattice] in case the [state] should be
     * manipulated. We do not recommend using this!
     */
    open fun handleMemberCallExpression(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: MemberCallExpression,
    ): Collection<OverlayNode> {
        return emptySet()
    }

    /**
     * This function is called for each node in the graph. The specific nodes are always handled in
     * the same order. It calls the basic and advanced version of the handleX-methods.
     */
    // TODO: Once we use tasks, we iterate over all tasks registered to this pass.
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

    /** Generates the initial [NodeToOverlayStateElement] state. */
    // TODO: Provide an interface for Tasks, so each one can modify the state as needed before
    // starting with the iteration or make this method non-open.
    open fun getInitialState(lattice: NodeToOverlayState, node: Node): NodeToOverlayStateElement {
        return lattice.bottom
    }

    /** This function is called for each edge in the EOG until the fixpoint is computed. */
    fun transfer(
        lattice: Lattice<NodeToOverlayStateElement>,
        currentEdge: EvaluationOrder,
        currentState: NodeToOverlayStateElement,
    ): NodeToOverlayStateElement {
        val lattice = lattice as? NodeToOverlayState ?: return currentState
        val currentNode = currentEdge.end
        val addedOverlays = handleNode(lattice, currentState, currentNode).toSet()

        // This is some magic to filter out overlays that are already in the state (equal but not
        // identical) for the same Node. It also filters the nodes if they have already been created
        // by a previous iteration over the same code block. This happens if multiple EOG starters
        // reach
        // a certain piece of code (frequently happens with the code after catch clauses).
        val filteredAddedOverlays =
            addedOverlays.filter { added ->
                currentState[currentNode]?.none { existing -> added == existing } != false &&
                    currentNode.overlays.none { existing ->
                        (existing as? OverlayNode)?.equals(added) == true
                    }
            }

        // If we do not add any new concepts, we can keep the state the same
        if (filteredAddedOverlays.isEmpty()) {
            return currentState
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

    companion object {
        var intermediateState: NodeToOverlayStateElement? = null
    }
}
