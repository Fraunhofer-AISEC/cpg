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
import de.fraunhofer.aisec.cpg.graph.quantumcpg.*
import de.fraunhofer.aisec.cpg.passes.order.DependsOn

@DependsOn(QuantumEOGPass::class)
class QuantumDFGPass : Pass() {

    override fun accept(p0: TranslationResult) {

        val allQuantumCircuits = p0.additionalNodes.filterIsInstance<QuantumCircuit>()

        for (circuit in allQuantumCircuits) {

            // connect qubits input/output for gates
            for (gate in circuit.gates) {
                when (gate) {
                    is QuantumGateCX -> {
                        gate.quBit0.addNextDFG(gate.quBit1)
                    }
                    is QuantumGateH -> {
                        // nothing to do
                    }
                    else -> TODO()
                }
            }

            // connect gates with each other
            val firstQuantumGate = circuit.gates.first { it.prevEOGQuantumGate == null }
            for (qubit in circuit.quantumBits!!) {
                var currentGate = advanceGateUntilRelevant(qubit, firstQuantumGate)
                var nextGate = advanceGateUntilRelevant(qubit, currentGate?.nextEOGQuantumGate)

                // add DFG from declaration to first use in gate
                when (currentGate) {
                    is QuantumGateH -> {
                        qubit.addNextDFG(currentGate.quantumBit0)
                    }
                    is QuantumGateCX -> {
                        if (currentGate.quBit0.refersToQubit == qubit) {
                            qubit.addNextDFG(currentGate.quBit0)
                        }
                        if (currentGate.quBit1.refersToQubit == qubit) {
                            qubit.addNextDFG(currentGate.quBit1)
                        }
                    }
                    else -> TODO()
                }

                // connect with following gates
                while (nextGate != null) {
                    when (currentGate) {
                        is QuantumGateH -> {
                            when (nextGate) {
                                is QuantumGateH -> {
                                    currentGate.quantumBit0.addNextDFG(nextGate.quantumBit0)
                                }
                                is QuantumGateCX -> {
                                    if (nextGate.quBit0.refersToQubit == qubit) {
                                        currentGate.quantumBit0.addNextDFG(nextGate.quBit0)
                                    }
                                    if (nextGate.quBit1.refersToQubit == qubit) {
                                        currentGate.quantumBit0.addNextDFG(nextGate.quBit1)
                                    }
                                }
                                else -> TODO()
                            }
                        }
                        is QuantumGateCX -> {
                            if (currentGate.quBit0.refersToQubit == qubit) {
                                when (nextGate) {
                                    is QuantumGateH -> {
                                        currentGate.quBit0.addNextDFG(nextGate.quantumBit0)
                                    }
                                    is QuantumGateCX -> {
                                        TODO()
                                    }
                                    else -> TODO()
                                }
                            }
                            if (currentGate.quBit1.refersToQubit == qubit) {
                                when (nextGate) {
                                    is QuantumGateH -> {
                                        currentGate.quBit1.addNextDFG(nextGate.quantumBit0)
                                    }
                                    is QuantumGateCX -> {
                                        TODO()
                                    }
                                    else -> TODO()
                                }
                            }
                        }
                        else -> TODO()
                    }

                    // advance one step
                    currentGate = nextGate
                    nextGate = advanceGateUntilRelevant(qubit, currentGate.nextEOGQuantumGate)
                }
            }
        }
    }

    private fun advanceGateUntilRelevant(qubit: QuantumBit, startGate: QuantumGate?): QuantumGate? {
        var currentGate: QuantumGate? = startGate
        while (currentGate != null) {
            when (currentGate) {
                is QuantumGateH -> {
                    if (currentGate.quantumBit0.refersToQubit == qubit) {
                        return currentGate
                    }
                }
                is QuantumGateCX -> {
                    if (
                        currentGate.quBit0.refersToQubit == qubit ||
                            currentGate.quBit1.refersToQubit == qubit
                    ) {
                        return currentGate
                    }
                }
                else -> TODO()
            }
            currentGate = currentGate.nextEOGQuantumGate
        }
        return null
    }

    override fun cleanup() {
        // nothing to do
    }
}
