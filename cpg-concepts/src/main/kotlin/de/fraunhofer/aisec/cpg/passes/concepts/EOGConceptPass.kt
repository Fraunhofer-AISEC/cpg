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
import de.fraunhofer.aisec.cpg.TranslationResult
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
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import java.util.Collections
import java.util.WeakHashMap
import kotlinx.coroutines.runBlocking

typealias NodeToOverlayStateElement =
    ConcurrentMapLattice.Element<Node, PowersetLattice.Element<OverlayNode>>

typealias NodeToOverlayState = ConcurrentMapLattice<Node, PowersetLattice.Element<OverlayNode>>

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

    /**
     * Whether this pass is executed again as long as an execution attaches new overlay nodes, i.e.,
     * until a fixpoint is reached. The overlays of an EOG starter are only attached to their
     * underlying nodes after it has been processed, so an overlay that builds upon another one
     * (e.g., via [getOverlaysByPrevDFG]) misses it if the other one belongs to an EOG starter that
     * is processed later, e.g., a function declared further down. The next execution finds it.
     *
     * The executions are triggered with [markDirty] and bounded by
     * [de.fraunhofer.aisec.cpg.TranslationConfiguration.maxPassExecutions], so this only takes
     * effect if the passes are executed sequentially.
     */
    protected open val iterateToFixpoint: Boolean
        get() = false

    override fun cleanup() {
        // Nothing to do
    }

    /**
     * Schedules the next execution if [iterateToFixpoint] is set and this execution attached new
     * overlay nodes. This runs once after all EOG starters of an execution (each of which gets its
     * own instance of the pass) and before the dirty nodes are evaluated.
     */
    override fun finalCleanup() {
        val execution = fixpointExecutions.remove(ctx) ?: return
        val result = execution.result ?: return

        // These marks of the previous execution have already scheduled this one
        result.dirtyNodes.values.forEach { passes -> passes.removeAll { it == this::class } }
        execution.newlyTagged.forEach { result.markDirty(it, this::class) }
    }

    override fun accept(node: Node) {
        val execution =
            if (iterateToFixpoint) {
                fixpointExecutions
                    .getOrPut(ctx) { FixpointExecution() }
                    .also { it.result = it.result ?: node.translationResult }
            } else {
                null
            }

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
            // Keep the (optional) IFDS summary cache honest: a node that just gained overlays may
            // be
            // a sink for later reaching-sources queries, so its callee must not be short-circuited.
            if (overlayNodes.isNotEmpty()) {
                ctx.currentComponent?.ifdsSummaryCache?.markSink(underlyingNode)
                execution?.newlyTagged?.add(underlyingNode)
            }
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
     * for the given [currentNode]. This is done by calling the [handleNode] method and filtering
     * the result based on the current state.
     */
    private fun overlayStateForNode(
        lattice: NodeToOverlayState,
        currentState: NodeToOverlayStateElement,
        currentNode: Node,
    ): NodeToOverlayStateElement {
        val addedOverlays = handleNode(lattice, currentState, currentNode).toSet()

        // This is some magic to filter out overlays that are already in the state (equal but not
        // identical) for the same Node. It also filters the nodes if they have already been created
        // by a previous iteration over the same code block. This happens if multiple EOG starters
        // reach a certain piece of code (frequently happens with the code after catch clauses).
        val filteredAddedOverlays = filterDuplicates(currentState, currentNode, addedOverlays)

        return if (filteredAddedOverlays.isEmpty()) {
            currentState
        } else {
            // Keep the (optional) IFDS summary cache honest: this node just gained state overlays
            // and
            // may be a sink for later reaching-sources queries, so its callee must stay expandable.
            ctx.currentComponent?.ifdsSummaryCache?.markSink(currentNode)
            runBlocking {
                lattice.lub(
                    currentState,
                    NodeToOverlayStateElement(
                        currentNode to
                            PowersetLattice.Element(*filteredAddedOverlays.toTypedArray())
                    ),
                    true,
                )
            }
        }
    }

    /**
     * What one execution of a pass with [iterateToFixpoint] collects across its EOG starters: the
     * nodes that got new overlay nodes, to be marked dirty in [finalCleanup].
     */
    private class FixpointExecution {
        var result: TranslationResult? = null
        val newlyTagged: MutableSet<Node> = Collections.synchronizedSet(identitySetOf())
    }

    companion object {
        /** The current [FixpointExecution] of each [TranslationContext]. */
        private val fixpointExecutions: MutableMap<TranslationContext, FixpointExecution> =
            Collections.synchronizedMap(WeakHashMap())

        /**
         * This is some magic to filter out overlays from [newOverlays] that are already in the
         * [currentState] (equal but not identical) for the same [node]. It also filters the
         * [OverlayNode]s if they have already been created by a previous iteration over the same
         * code block. This happens if multiple EOG starters reach a certain piece of code
         * (frequently happens with the code after catch clauses). Returns a new list of the
         * remaining [OverlayNode]s and does not modify [newOverlays].
         */
        fun filterDuplicates(
            currentState: NodeToOverlayStateElement,
            node: Node,
            newOverlays: Collection<OverlayNode>,
        ): Collection<OverlayNode> {
            return newOverlays.filter { new ->
                currentState[node]?.none { existing -> new == existing } != false &&
                    node.overlays.none { existing ->
                        (existing as? OverlayNode)?.equals(new) == true
                    }
            }
        }
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
