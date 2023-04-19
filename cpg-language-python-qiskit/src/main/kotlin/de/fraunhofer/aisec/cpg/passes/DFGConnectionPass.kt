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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumCircuit
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumMeasure
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import de.fraunhofer.aisec.cpg.passes.quantumcpg.QuantumDFGPass

@DependsOn(QuantumDFGPass::class)
@DependsOn(ControlFlowSensitiveDFGPass::class)
class DFGConnectionPass : Pass() {
    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(t: TranslationResult) {
        val transpileCalls = t.calls("transpile")
        for (call in transpileCalls) {
            val circuit = call.arguments.first() as DeclaredReferenceExpression
            val measure =
                (t.additionalNodes.firstOrNull {
                        it is QuantumCircuit && it.cpgNode == circuit.refersTo
                    } as QuantumCircuit?)
                    ?.operations
                    ?.filterIsInstance<QuantumMeasure>()
            val jobs =
                call
                    .followNextDFGEdgesUntilHit {
                        (it as? CallExpression)?.name?.localName == "run"
                    }
                    .fulfilled
                    .map { it.last() }
            jobs.forEach {
                val results =
                    it.followNextDFGEdgesUntilHit {
                            (it as? CallExpression)?.name?.localName == "result"
                        }
                        .fulfilled
                        .map { it.last() }

                measure?.forEach { measure ->
                    results.forEach { result ->
                        measure.measurements.forEach { it.cBit.addNextDFG(result) }
                    }
                }
            }
        }
    }
}
