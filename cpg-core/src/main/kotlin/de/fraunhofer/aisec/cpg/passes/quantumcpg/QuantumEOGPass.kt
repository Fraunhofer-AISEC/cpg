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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumCircuit
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumGate
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumMeasure
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.order.DependsOn

// @DependsOn(QiskitPass::class) or @DependsOn(OpenQASMPass::class)
@DependsOn(EvaluationOrderGraphPass::class)
class QuantumEOGPass(ctx: TranslationContext) : EvaluationOrderGraphPass(ctx) {
    private val nodeTracker = HashSet<Node>()

    init {
        map[QuantumMeasure::class.java] = { handleQuantumMeasure(it as QuantumMeasure) }
        map[QuantumGate::class.java] = { handleQuantumGate(it as QuantumGate) }
        map[QuantumCircuit::class.java] = { handleQuantumCircuit(it as QuantumCircuit) }
    }

    private fun handleQuantumCircuit(node: QuantumCircuit) {
        // Analyze the contained statements. Since we analyzed the operations in the statement order
        // of the original source and we do not allow branching conditions of the underlying source
        // code, we can just iterate over the operations.
        for (child in node.statements) {
            createEOG(child as Node)
        }

        // pushToEOG(node)
    }

    private fun handleQuantumGate(node: QuantumGate) {
        pushToEOG(node)
    }

    private fun handleQuantumMeasure(node: QuantumMeasure) {
        pushToEOG(node)
    }

    override fun accept(tu: TranslationUnitDeclaration) {
        // We only want to start at the circuit(s)
        val circuits = tu.additionalNodes.filterIsInstance<QuantumCircuit>()

        for (circuit in circuits) {
            createEOG(circuit)
        }
    }

    override fun cleanup() {
        // nothing to do
    }
}
