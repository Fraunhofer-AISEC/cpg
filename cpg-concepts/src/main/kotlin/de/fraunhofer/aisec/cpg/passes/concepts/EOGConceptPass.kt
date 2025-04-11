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
import de.fraunhofer.aisec.cpg.graph.concepts.newConceptNoConnect
import de.fraunhofer.aisec.cpg.graph.concepts.newOperationNoConnect
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.edges.flows.insertNodeAfterwardInEOGPath
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.graph.followDFGEdgesUntilHit
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.MapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.EOGIteratorTask
import de.fraunhofer.aisec.cpg.passes.EOGStarterLeastTUImportSorterWithCatchAfterTry
import de.fraunhofer.aisec.cpg.passes.EOGStarterPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.TaskBasedPass
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import kotlin.collections.set

typealias NodeToOverlayStateElement = MapLattice.Element<Node, PowersetLattice.Element<OverlayNode>>

typealias NodeToOverlayState = MapLattice<Node, PowersetLattice.Element<OverlayNode>>

/**
 * An abstract pass that is used to identify and create [Concept] and [Operation] nodes in the
 * graph. It uses the fixpoint-iteration to traverse the graph and create the nodes.
 *
 * Important information for classes implementing this pass:
 * * The following methods can be overridden to handle specific nodes:
 *     - [handleCallExpression] (either the simplified version or the one with the lattice).
 *     - [handleMemberCallExpression] (either the simplified version or the one with the lattice).
 * * These methods must return a collection of [OverlayNode]s that are created for the given node.
 *   They must not create [OverlayNode]s for other nodes than the one passed as an argument!
 * * These methods must not connect the created [OverlayNode]s to the underlying node! This is done
 *   in the pass itself after having collected all overlays. Use a builder based on
 *   [newOperationNoConnect] or [newConceptNoConnect] to do this.
 */
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(DFGPass::class)
@DependsOn(ControlFlowSensitiveDFGPass::class, softDependency = true)
open class EOGConceptPass(ctx: TranslationContext) :
    EOGStarterPass(ctx, sort = EOGStarterLeastTUImportSorterWithCatchAfterTry), TaskBasedPass {

    /** Stores the current component in case we need it to look up some stuff. */
    var currentComponent: Component? = null

    val tasks: List<EOGIteratorTask<Node, EOGConceptPass, OverlayNode>> =
        this.getTasks().filterIsInstance<EOGIteratorTask<Node, EOGConceptPass, OverlayNode>>()

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
        val addedOverlays =
            tasks.fold(mutableSetOf<OverlayNode>()) { set, task ->
                set.addAll(task.handleNode(lattice, currentState, currentNode))
                set
            }

        // This is some magic to filter out overlays that are already in the state (equal but not
        // identical) for the same Node. It also filters the nodes if they have already been created
        // by a previous pass over the same code block. This happens if multiple EOG starters reach
        // a certain piece of code (frequently happens with the code after catch clauses).
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
