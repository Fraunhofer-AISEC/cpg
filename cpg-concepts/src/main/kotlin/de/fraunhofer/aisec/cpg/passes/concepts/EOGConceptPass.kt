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
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.component
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.edges.flows.insertNodeAfterwardInEOGPath
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.MapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.passes.EOGStarterLeastTUImportSorterWithCatchAfterTry
import de.fraunhofer.aisec.cpg.passes.EOGStarterPass

typealias NodeToOverlayStateElement = MapLattice.Element<Node, PowersetLattice.Element<OverlayNode>>

typealias NodeToOverlayState = MapLattice<Node, PowersetLattice.Element<OverlayNode>>

/**
 * An abstract pass that is used to identify and create [Concept] and [Operation] nodes in the
 * graph.
 */
abstract class EOGConceptPass(ctx: TranslationContext) :
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
}
