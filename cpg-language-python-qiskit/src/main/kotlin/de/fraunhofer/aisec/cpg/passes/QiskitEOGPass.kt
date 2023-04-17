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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumCircuit
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumGate
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import de.fraunhofer.aisec.cpg.passes.order.RequiredFrontend

@RequiredFrontend(PythonLanguageFrontend::class)
@DependsOn(QiskitPass::class)
@DependsOn(EvaluationOrderGraphPass::class)
class QiskitEOGPass : Pass() {

    override fun accept(p0: TranslationResult) {

        val allQuantumCircuits = p0.additionalNodes.filterIsInstance<QuantumCircuit>()

        for (circuit in allQuantumCircuits) {
            val circuitCPG = (circuit as? QuantumCircuit)?.cpgNode
            if (circuitCPG !is VariableDeclaration) {
                TODO()
            }
            val nextEOGs = circuitCPG.nextEOG.filterIsInstance<DeclarationStatement>()
            if (nextEOGs.size != 1) {
                TODO()
            }
            val declStmt = nextEOGs.first()
            var currentNode: Node? = declStmt
            var lastQuantumGateSeen: QuantumGate? = null
            while (currentNode != null) {
                if (currentNode is MemberCallExpression) {
                    val quantumCPGNode =
                        p0.additionalNodes.firstOrNull {
                            (it as? QuantumGate)?.cpgNode == currentNode
                        } as? QuantumGate
                    if (quantumCPGNode != null) {
                        if (lastQuantumGateSeen != null) {
                            // add Q-EOG edges to the quantum graph
                            lastQuantumGateSeen.nextEOGQuantumGate = quantumCPGNode
                            quantumCPGNode.prevEOGQuantumGate = lastQuantumGateSeen
                        }
                        lastQuantumGateSeen = quantumCPGNode
                    }
                } else {}
                currentNode = currentNode.nextEOG.firstOrNull()
            }
        }
    }

    override fun cleanup() {
        // nothing to do
    }
}
