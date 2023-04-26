/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes.quantumcpg

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumCircuit
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumGate
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumMeasure
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import java.util.HashSet

// @DependsOn(QiskitPass::class) or @DependsOn(OpenQASMPass::class)
@DependsOn(EvaluationOrderGraphPass::class)
class QuantumEOGPass : Pass() {
    private val nodeTracker = HashSet<Node>()

    override fun accept(p0: TranslationResult) {

        val allQuantumCircuits = p0.additionalNodes.filterIsInstance<QuantumCircuit>()

        for (circuit in allQuantumCircuits) {
            val circuitCPG = (circuit as? QuantumCircuit)?.cpgNode ?: TODO()

            val nextEOGs = circuitCPG.nextEOG.filterIsInstance<DeclarationStatement>()

            for (next in nextEOGs) {
                if (next in nodeTracker) {
                    continue
                }
                handleNode(p0, next, null)
                nodeTracker.add(next)
            }
        }
    }

    private fun handleNode(p0: TranslationResult, node: Node, last: Node?) {
        if (node in nodeTracker) {
            return
        }
        nodeTracker.add(node)
        var lastQuantumGateSeen: Node? = last

        if (node is MemberCallExpression) {
            val quantumCPGNode =
                p0.additionalNodes.firstOrNull { (it as? QuantumGate)?.cpgNode == node }
                    as? QuantumGate
                    ?: p0.additionalNodes.firstOrNull { (it as? QuantumMeasure)?.cpgNode == node }
                        as? QuantumMeasure
                        ?: null

            when (quantumCPGNode) {
                is QuantumGate -> {
                    if (lastQuantumGateSeen != null) {
                        // add Q-EOG edges to the quantum graph
                        addEOGEdge(lastQuantumGateSeen, quantumCPGNode)
                    }
                    lastQuantumGateSeen = quantumCPGNode
                }
                is QuantumMeasure -> {
                    if (lastQuantumGateSeen != null) {
                        addEOGEdge(lastQuantumGateSeen, quantumCPGNode)
                        lastQuantumGateSeen = quantumCPGNode
                        for (m in quantumCPGNode.measurements) {
                            addEOGEdge(lastQuantumGateSeen!!, m)
                            lastQuantumGateSeen = m
                        }
                    } else {
                        TODO()
                    }
                }
                else -> {}
            }
        }

        when (node.nextEOG.size) {
            0 -> {
                // finished :)
            }
            else -> {
                for (next in node.nextEOG) {
                    handleNode(p0, next, lastQuantumGateSeen)
                }
            }
        }
    }

    /* TODO: copy & paste from EOG pass */
    /**
     * Builds an EOG edge from prev to next. 'eogDirection' defines how the node instances save the
     * references constituting the edge. 'FORWARD': only the nodes nextEOG member contains
     * references, an points to the next nodes. 'BACKWARD': only the nodes prevEOG member contains
     * references and points to the previous nodes. 'BIDIRECTIONAL': nextEOG and prevEOG contain
     * references and point to the previous and the next nodes.
     *
     * @param prev the previous node
     * @param next the next node
     */
    private fun addEOGEdge(prev: Node, next: Node) {
        val propertyEdge = PropertyEdge(prev, next)
        // propertyEdge.addProperties(nextEdgeProperties)
        propertyEdge.addProperty(Properties.INDEX, prev.nextEOG.size)
        propertyEdge.addProperty(Properties.UNREACHABLE, false)
        prev.addNextEOG(propertyEdge)
        next.addPrevEOG(propertyEdge)
    }

    override fun cleanup() {
        // nothing to do
    }
}
