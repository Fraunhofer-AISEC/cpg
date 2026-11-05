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
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.helpers.functional.ConcurrentMapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import java.util.IdentityHashMap
import kotlinx.coroutines.runBlocking

typealias NodeToOverlayStateElement =
    ConcurrentMapLattice.Element<Node, PowersetLattice.Element<OverlayNode>>

/**
 * The lattice of the states of one execution of [EOGConceptPass.accept]. It also remembers the
 * [OverlayNode]s created per node, so that a node which is evaluated more than once (e.g. after a
 * loop, with the states of different EOG paths) gets the same instances again. Otherwise, the
 * states would hold equal but not identical overlays, and an overlay built upon one of them (e.g.
 * an [Operation] of a [Concept]) could refer to an instance that is never attached.
 *
 * Overlays are added to a state with [addOverlays].
 */
class NodeToOverlayState(innerLattice: PowersetLattice<OverlayNode> = PowersetLattice()) :
    ConcurrentMapLattice<Node, PowersetLattice.Element<OverlayNode>>(innerLattice) {

    /**
     * The overlays created so far per node. They are compared with `==` (see [OverlayNode.hashCode]
     * why not in a hash-based collection).
     */
    private val created = IdentityHashMap<Node, MutableList<OverlayNode>>()

    /**
     * Returns the instances to use for [overlays] of [node]: for each overlay, an equal one that is
     * attached to [node] already, else an equal one created for [node] before (also earlier in
     * [overlays]), else the overlay itself, which is remembered from now on.
     *
     * A new [Operation] refers to the [Concept] it was built with. If that concept is one of
     * [overlays], it is therefore kept as it is, even if an equal one exists.
     */
    fun intern(node: Node, overlays: Collection<OverlayNode>): List<OverlayNode> {
        val attached = node.overlays.filterIsInstance<OverlayNode>()
        val known = created.getOrPut(node) { mutableListOf() }
        val canonical = IdentityHashMap<OverlayNode, OverlayNode>()
        for (overlay in overlays) {
            canonical[overlay] =
                if (attached.any { it === overlay } || known.any { it === overlay }) {
                    overlay
                } else {
                    attached.firstOrNull { it == overlay }
                        ?: known.firstOrNull { it == overlay }
                        ?: overlay.also { known += it }
                }
        }
        for (overlay in overlays) {
            if (overlay !is Operation || canonical[overlay] !== overlay) continue
            val concept = overlay.concept
            val conceptInstance = canonical[concept]
            if (conceptInstance != null && conceptInstance !== concept) {
                canonical[concept] = concept
                known += concept
            }
        }

        val result = mutableListOf<OverlayNode>()
        for (overlay in overlays) {
            val instance = canonical[overlay] ?: overlay
            if (result.none { it === instance }) {
                result += instance
            }
        }
        return result
    }

    /**
     * Adds the [intern]ed [overlays] of [node] to [state], which is modified, and returns the ones
     * that [state] did not contain yet.
     */
    fun addOverlays(
        state: NodeToOverlayStateElement,
        node: Node,
        overlays: Collection<OverlayNode>,
    ): List<OverlayNode> {
        val added =
            intern(node, overlays).filter { overlay ->
                state[node]?.none { it === overlay } != false
            }
        if (added.isNotEmpty()) {
            runBlocking {
                lub(
                    state,
                    NodeToOverlayStateElement(
                        node to PowersetLattice.Element(*added.toTypedArray())
                    ),
                    true,
                )
            }
        }
        return added
    }
}

/**
 * An abstract pass that is used to identify and create [Concept] and [Operation] nodes in the
 * graph. It uses the fixpoint-iteration to traverse the graph and create the nodes. It accounts for
 * all possible EOG paths reaching a node. This is different to the [ConceptPass] which fails to
 * account for the fact that nodes may be reachable by different EOG paths.
 *
 * Important information for classes implementing this pass:
 * * The following methods can be overridden to handle specific nodes:
 *     - [handleCall] (either the simplified version or the one with the lattice).
 *     - [handleMemberCall] (either the simplified version or the one with the lattice).
 * * These methods must return a collection of [OverlayNode]s that are created for the given node.
 *   They must not create [OverlayNode]s for other nodes than the one passed as an argument!
 * * These methods must not connect the created [OverlayNode]s to the underlying node! This is done
 *   in the pass itself after having collected all overlays. Use a builder based with the flag
 *   `connect` set to `false` to do this.
 * * If you require the [OverlayNode]s to be created in a specific EOG order, you have to return an
 *   ordered collection.
 */
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(DFGPass::class)
@DependsOn(ControlFlowSensitiveDFGPass::class, softDependency = true)
@Description("Creates Concept and Operation overlay nodes based on EOG traversal.")
open class EOGConceptPass(ctx: TranslationContext) :
    EOGStarterPass(ctx, sort = EOGStarterLeastTUImportCatchLastSorter) {

    /** Stores the current component in case we need it to look up some stuff. */
    var currentComponent: Component? = null

    override fun cleanup() {
        // Nothing to do
    }

    override fun finalCleanup() {
        // Nothing to do
    }

    override fun accept(node: Node) {
        ctx.currentComponent = node.component
        currentComponent = ctx.currentComponent

        val lattice = NodeToOverlayState(PowersetLattice())
        val startState = getInitialState(lattice, node)

        val nextEog = node.nextEOGEdges
        val finalState = runBlocking {
            lattice.iterateEOG(nextEog, startState, ::transfer).first.let { tmpFinalState ->
                lattice.lub(tmpFinalState, startState, true)
            } ?: startState
        }

        // We set the underlying node based on the final state
        for ((underlyingNode, overlayNodes) in finalState) {
            attachOverlays(underlyingNode, overlayNodes)
        }
    }

    /**
     * Attaches [overlays] (from the final state) to [underlyingNode] and returns the ones that were
     * attached.
     *
     * The state only holds the instances returned by [NodeToOverlayState.intern], which may be
     * attached to [underlyingNode] already (e.g. by an earlier execution of this pass). These are
     * skipped.
     */
    internal fun attachOverlays(
        underlyingNode: Node,
        overlays: Collection<OverlayNode>,
    ): List<OverlayNode> {
        val attached =
            overlays.filter { overlay -> underlyingNode.overlays.none { it === overlay } }

        // Keep the (optional) IFDS summary cache honest: a node that just gained overlays may be
        // a sink for later reaching-sources queries, so its callee must not be short-circuited.
        if (attached.isNotEmpty()) {
            ctx.currentComponent?.ifdsSummaryCache?.markSink(underlyingNode)
        }
        attached.forEach {
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
        return attached
    }

    /**
     * Generates [OverlayNode]s belonging to the given [node]. The [state] contains a map of nodes
     * to their respective [OverlayNode]s created by this instance of the pass.
     *
     * Note: see the class documentation for more information about creating [OverlayNode]s.
     */
    open fun handleCall(state: NodeToOverlayStateElement, node: Call): Collection<OverlayNode> {
        return emptySet()
    }

    /**
     * Generates [OverlayNode]s belonging to the given [node]. The [state] contains a map of nodes
     * to their respective [OverlayNode]s created by this instance of the pass.
     *
     * This is the advanced version and passes the [lattice] in case the [state] should be
     * manipulated. We do not recommend using this!
     *
     * Note: see the class documentation for more information about creating [OverlayNode]s.
     */
    open fun handleCall(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: Call,
    ): Collection<OverlayNode> {
        return emptySet()
    }

    /**
     * Generates [OverlayNode]s belonging to the given [node]. The [state] contains a map of nodes
     * to their respective [OverlayNode]s created by this instance of the pass.
     *
     * Note: see the class documentation for more information about creating [OverlayNode]s.
     */
    open fun handleMemberCall(
        state: NodeToOverlayStateElement,
        node: MemberCall,
    ): Collection<OverlayNode> {
        return emptySet()
    }

    /**
     * Generates [OverlayNode]s belonging to the given [node]. The [state] contains a map of nodes
     * to their respective [OverlayNode]s created by this instance of the pass.
     *
     * This is the advanced version and passes the [lattice] in case the [state] should be
     * manipulated. We do not recommend using this!
     *
     * Note: see the class documentation for more information about creating [OverlayNode]s.
     */
    open fun handleMemberCall(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: MemberCall,
    ): Collection<OverlayNode> {
        return emptySet()
    }

    /**
     * This function is called for each node in the graph. The specific nodes are always handled in
     * the same order. It calls the basic and advanced version of the handleX-methods.
     *
     * Note: see the class documentation for more information about creating [OverlayNode]s.
     */
    // TODO: Once we use tasks, we iterate over all tasks registered to this pass.
    open fun handleNode(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: Node,
    ): Collection<OverlayNode> {
        return when (node) {
            is MemberCall -> handleMemberCall(lattice, state, node) + handleMemberCall(state, node)
            is Call -> handleCall(lattice, state, node) + handleCall(state, node)
            else -> emptySet()
        }
    }

    /**
     * Generates the initial [NodeToOverlayStateElement] state for the current execution of this
     * pass, which is currently [ConcurrentMapLattice.bottom] where some stuff based on [node] is
     * already added.
     */
    open fun getInitialState(lattice: NodeToOverlayState, node: Node): NodeToOverlayStateElement {
        return overlayStateForNode(lattice, lattice.bottom, node)
    }

    /** This function is called for each edge in the EOG until the fixpoint is computed. */
    suspend fun transfer(
        lattice: Lattice<NodeToOverlayStateElement>,
        currentEdge: EvaluationOrder,
        currentState: NodeToOverlayStateElement,
    ): NodeToOverlayStateElement {
        val lattice = lattice as? NodeToOverlayState ?: return currentState
        val currentNode = currentEdge.end
        return overlayStateForNode(lattice, currentState, currentNode)
    }

    /**
     * Returns and modifies the (new) state which contains all [OverlayNode]s that should be added
     * for the given [currentNode]. This is done by calling the [handleNode] method and adding the
     * result to the state with [NodeToOverlayState.addOverlays].
     */
    private fun overlayStateForNode(
        lattice: NodeToOverlayState,
        currentState: NodeToOverlayStateElement,
        currentNode: Node,
    ): NodeToOverlayStateElement {
        val added =
            lattice.addOverlays(
                currentState,
                currentNode,
                handleNode(lattice, currentState, currentNode).toSet(),
            )
        if (added.isNotEmpty()) {
            // Keep the (optional) IFDS summary cache honest: this node just gained state overlays
            // and may be a sink for later reaching-sources queries, so its callee must stay
            // expandable.
            ctx.currentComponent?.ifdsSummaryCache?.markSink(currentNode)
        }
        return currentState
    }
}

/**
 * Returns a list of nodes of type [T] fulfilling the [predicate] that are reachable from this node
 * via the backwards DFG.
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
        .flatMap {
            val last = it.nodes.last()
            // collect all "overlay" nodes
            stateElement[last] ?: setOf(last, *last.overlays.toTypedArray())
        }
        .filterIsInstance<T>() // discard not-relevant overlays
}

/**
 * This interfaces describes a generic structure that "collects" a list of [OverlayNode]s that
 * should be pushed to the state based on the current node in the EOG iteration.
 */
interface OverlayCollector {
    /**
     * This function needs to return a list of [OverlayNode]s that are considered to be added to the
     * [state], given the current [node] in the EOG iteration.
     *
     * In order to safe some memory, instead of an [emptyList], a null object can also be returned
     * if no overlay nodes are suitable for the given [node].
     */
    fun collect(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: Node,
    ): List<OverlayNode>?
}
