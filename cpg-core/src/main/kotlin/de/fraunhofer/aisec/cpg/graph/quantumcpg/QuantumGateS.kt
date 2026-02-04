/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.quantumcpg

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression

/** The S gate (Phase gate S, square root of Z, P(π/2)). */
class QuantumGateS(
    cpgNode: Node?,
    quantumCircuit: QuantumCircuit,
    quantumBit0: QuantumBitReference,
) : QuantumPauliGate(cpgNode, quantumCircuit, quantumBit0) {

    init {
        (cpgNode as? CallExpression)?.let { this.callee = it.callee }
    }

    override val fidelity: Float = 0.0f
}

/** The adjoint of the S gate (a square root of Z, P(-π/2)). */
class QuantumGateSdg(
    cpgNode: Node?,
    quantumCircuit: QuantumCircuit,
    quantumBit0: QuantumBitReference,
) : QuantumPauliGate(cpgNode, quantumCircuit, quantumBit0) {

    init {
        (cpgNode as? CallExpression)?.let { this.callee = it.callee }
    }

    override val fidelity: Float = 0.0f
}
