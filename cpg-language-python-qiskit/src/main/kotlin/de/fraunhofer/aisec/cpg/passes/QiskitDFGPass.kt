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
import de.fraunhofer.aisec.cpg.graph.quantumcpg.*
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import de.fraunhofer.aisec.cpg.passes.order.RequiredFrontend

@RequiredFrontend(PythonLanguageFrontend::class)
@DependsOn(QiskitEOGPass::class)
class QiskitDFGPass : Pass() {

    override fun accept(p0: TranslationResult) {

        val allQuantumCircuits = p0.additionalNodes.filterIsInstance<QuantumCircuit>()

        for (circuit in allQuantumCircuits) {

            // connect qubits input/output for gates
            for (gate in circuit.gates) {
                when (gate) {
                    is QuantumGateCX -> {
                        gate.quBit0.nextDFGQuantum = gate.quBit1
                        gate.quBit1.prevDFGQuantum = gate.quBit0
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
                        qubit.nextDFGQuantum = currentGate.quantumBit0
                        // currentGate.quantumBit0.prevDFGQuantum = qubit
                    }
                    is QuantumGateCX -> {
                        if (currentGate.quBit0.refersTo == qubit) {
                            qubit.nextDFGQuantum = currentGate.quBit0
                            // currentGate.quBit0.prevDFGQuantum = qubit
                        }
                        if (currentGate.quBit1.refersTo == qubit) {
                            qubit.nextDFGQuantum = currentGate.quBit1
                            // currentGate.quBit1.prevDFGQuantum = qubit
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
                                    currentGate.quantumBit0.nextDFGQuantum = nextGate.quantumBit0
                                    nextGate.quantumBit0.prevDFGQuantum = currentGate.quantumBit0
                                }
                                is QuantumGateCX -> {
                                    if (nextGate.quBit0.refersTo == qubit) {
                                        currentGate.quantumBit0.nextDFGQuantum = nextGate.quBit0
                                        nextGate.quBit0.prevDFGQuantum = currentGate.quantumBit0
                                    }
                                    if (nextGate.quBit1.refersTo == qubit) {
                                        currentGate.quantumBit0.nextDFGQuantum = nextGate.quBit1
                                        nextGate.quBit1.prevDFGQuantum = currentGate.quantumBit0
                                    }
                                }
                                else -> TODO()
                            }
                        }
                        is QuantumGateCX -> {
                            if (currentGate.quBit0.refersTo == qubit) {
                                when (nextGate) {
                                    is QuantumGateH -> {
                                        currentGate.quBit0.nextDFGQuantum = nextGate.quantumBit0
                                        nextGate.quantumBit0.prevDFGQuantum = currentGate.quBit0
                                    }
                                    is QuantumGateCX -> {
                                        TODO()
                                    }
                                    else -> TODO()
                                }
                            }
                            if (currentGate.quBit1.refersTo == qubit) {
                                when (nextGate) {
                                    is QuantumGateH -> {
                                        currentGate.quBit1.nextDFGQuantum = nextGate.quantumBit0
                                        nextGate.quantumBit0.prevDFGQuantum = currentGate.quBit1
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
                    if (currentGate.quantumBit0.refersTo == qubit) {
                        return currentGate
                    }
                }
                is QuantumGateCX -> {
                    if (
                        currentGate.quBit0.refersTo == qubit || currentGate.quBit1.refersTo == qubit
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
