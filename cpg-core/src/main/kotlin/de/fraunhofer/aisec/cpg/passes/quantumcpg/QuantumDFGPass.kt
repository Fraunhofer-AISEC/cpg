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
import de.fraunhofer.aisec.cpg.graph.followNextEOG
import de.fraunhofer.aisec.cpg.graph.quantumcpg.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.order.DependsOn

@DependsOn(QuantumEOGPass::class)
class QuantumDFGPass : Pass() {

    val worklist: MutableList<QuantumOperation> = mutableListOf()

    override fun accept(p0: TranslationResult) {
        val allQuantumCircuits = p0.additionalNodes.filterIsInstance<QuantumCircuit>()

        for (circuit in allQuantumCircuits) {

            connectDFGWithinOp(circuit)

            // connect gates with each other
            val firstQuantumGate: QuantumOperation =
                circuit.operations.first {
                    when (it) {
                        is QuantumGate -> it.prevEOG.isEmpty()
                        is QuantumMeasure -> it.prevEOG.isEmpty()
                        else -> TODO()
                    }
                }

            for (qubit in circuit.quantumBits!!) {

                if (operationIsRelevantForQubit(firstQuantumGate, qubit)) {
                    worklist.add(firstQuantumGate as? QuantumOperation ?: TODO())
                } else {
                    advanceGateUntilRelevant(qubit, firstQuantumGate)?.let { worklist.add(it) }
                }

                // add DFG from declaration to first use in gate
                worklist.firstOrNull()?.let { connectQubitFirstUse(it, qubit) }

                while (worklist.isNotEmpty()) {
                    val currentOperation = worklist.removeFirst()
                    var nextOperation = advanceGateUntilRelevant(qubit, currentOperation)
                    if (nextOperation != null) {
                        when (currentOperation) {
                            is QuantumGateH -> {
                                when (nextOperation) {
                                    is QuantumGateH -> {
                                        currentOperation.quantumBit0.addNextDFG(
                                            nextOperation.quantumBit0
                                        )
                                    }
                                    is QuantumGateX -> {
                                        currentOperation.quantumBit0.addNextDFG(
                                            nextOperation.quantumBit0
                                        )
                                    }
                                    is QuantumGateCX -> {
                                        if (nextOperation.quBit0.refersToQubit == qubit) {
                                            currentOperation.quantumBit0.addNextDFG(
                                                nextOperation.quBit0
                                            )
                                        }
                                        if (nextOperation.quBit1.refersToQubit == qubit) {
                                            currentOperation.quantumBit0.addNextDFG(
                                                nextOperation.quBit1
                                            )
                                        }
                                    }
                                    is QuantumMeasurement -> {
                                        currentOperation.quantumBit0.addNextDFG(nextOperation.quBit)
                                    }
                                    else -> {
                                        TODO()
                                    }
                                }
                            }
                            is QuantumGateX -> {
                                when (nextOperation) {
                                    is QuantumGateH -> {
                                        currentOperation.quantumBit0.addNextDFG(
                                            nextOperation.quantumBit0
                                        )
                                    }
                                    is QuantumGateX -> {
                                        currentOperation.quantumBit0.addNextDFG(
                                            nextOperation.quantumBit0
                                        )
                                    }
                                    is QuantumGateCX -> {
                                        if (nextOperation.quBit0.refersToQubit == qubit) {
                                            currentOperation.quantumBit0.addNextDFG(
                                                nextOperation.quBit0
                                            )
                                        }
                                        if (nextOperation.quBit1.refersToQubit == qubit) {
                                            currentOperation.quantumBit0.addNextDFG(
                                                nextOperation.quBit1
                                            )
                                        }
                                    }
                                    is QuantumMeasurement -> {
                                        currentOperation.quantumBit0.addNextDFG(nextOperation.quBit)
                                    }
                                    else -> {
                                        TODO()
                                    }
                                }
                            }
                            is QuantumGateCX -> {
                                if (currentOperation.quBit0.refersToQubit == qubit) {
                                    when (nextOperation) {
                                        is QuantumGateH -> {
                                            currentOperation.quBit0.addNextDFG(
                                                nextOperation.quantumBit0
                                            )
                                        }
                                        is QuantumGateCX -> {
                                            TODO()
                                        }
                                        is QuantumMeasurement -> {
                                            currentOperation.quBit0.addNextDFG(nextOperation.quBit)
                                        }
                                        else -> TODO()
                                    }
                                }
                                if (currentOperation.quBit1.refersToQubit == qubit) {
                                    when (nextOperation) {
                                        is QuantumGateH -> {
                                            currentOperation.quBit1.addNextDFG(
                                                nextOperation.quantumBit0
                                            )
                                        }
                                        is QuantumGateX -> {
                                            currentOperation.quBit1.addNextDFG(
                                                nextOperation.quantumBit0
                                            )
                                        }
                                        is QuantumGateCX -> {
                                            TODO()
                                        }
                                        is QuantumMeasurement -> {
                                            currentOperation.quBit1.addNextDFG(nextOperation.quBit)
                                        }
                                        else -> TODO()
                                    }
                                }
                            }
                            is QuantumMeasurement -> {
                                // TODO add edges back to the original CPG here
                            }
                            else -> TODO()
                        }

                        // advance one step
                        worklist.add(nextOperation)
                    }
                }
            }
        }
    }

    private fun connectQubitFirstUse(firstOp: QuantumOperation, qubit: QuantumBit) {
        when (firstOp) {
            is QuantumGateH -> {
                qubit.addNextDFG(firstOp.quantumBit0)
            }
            is QuantumGateX -> {
                qubit.addNextDFG(firstOp.quantumBit0)
            }
            is QuantumGateCX -> {
                if (firstOp.quBit0.refersToQubit == qubit) {
                    qubit.addNextDFG(firstOp.quBit0)
                }
                if (firstOp.quBit1.refersToQubit == qubit) {
                    qubit.addNextDFG(firstOp.quBit1)
                }
            }
            is QuantumMeasurement -> {
                qubit.addNextDFG(firstOp.quBit)
            }
            is ClassicIf -> {
                // TODO?
            }
        }
    }

    private fun operationIsRelevantForQubit(op: QuantumOperation, qubit: QuantumBit): Boolean {
        return when (op) {
            is QuantumGateH -> {
                op.quantumBit0.refersToQubit == qubit
            }
            is QuantumGateX -> {
                op.quantumBit0.refersToQubit == qubit
            }
            is QuantumGateCX -> {
                op.quBit0.refersToQubit == qubit || op.quBit1.refersToQubit == qubit
            }
            is QuantumMeasurement -> {
                op.quBit.refersToQubit == qubit
            }
            is QuantumMeasure -> {
                // we only care about the measurements and not the enclosing "measure"
                false
            }
            is ClassicIf -> {
                // Check condition
                val binOp = op.condition as? BinaryOperator
                (binOp?.lhs as? DeclaredReferenceExpression)?.refersTo == qubit ||
                    (binOp?.rhs as? DeclaredReferenceExpression)?.refersTo == qubit
            }
            else -> TODO()
        }
    }

    /** Add DFG edges within a QuantumOperation */
    private fun connectDFGWithinOp(circuit: QuantumCircuit) {
        // connect qubits input/output for gates
        for (op in circuit.operations) {
            when (op) {
                is QuantumGateCX -> {
                    // data flow from control bit to other bit
                    op.quBit0.addNextDFG(op.quBit1)
                }
                is QuantumGateX -> {
                    // data flows to itself?
                }
                is QuantumGateH -> {
                    // nothing to do
                }
                is QuantumMeasure -> {
                    // data flow from qubit to classic bit
                    for (m in op.measurements) {
                        m.quBit.addNextDFG(m.cBit)
                    }
                }
                is ClassicIf -> {
                    // TODO: very hacky, but needed
                    val binOp = op.condition as? BinaryOperator ?: continue
                    binOp.addPrevDFG(binOp.rhs)
                    binOp.addPrevDFG(binOp.lhs)

                    op.addPrevDFG(binOp)
                }
                else -> TODO("not implemented: $op")
            }
        }
    }

    /**
     * Advance the current node according to the EOG until the next gate which uses the provided
     * [qubit]
     */
    private fun advanceGateUntilRelevant(
        qubit: QuantumBit,
        startNode: QuantumOperation?
    ): QuantumOperation? {
        var currentNode: QuantumOperation? = nextOpEOG(startNode)
        while (currentNode != null) {
            if (operationIsRelevantForQubit(currentNode, qubit)) {
                return currentNode
            } else {
                currentNode = nextOpEOG(currentNode)
            }
        }
        return null
    }

    /** Get the next QuantumOperation according to the EOG */
    private fun nextOpEOG(startOp: QuantumOperation?): QuantumOperation? {
        val path = (startOp as? Node)?.followNextEOG { it.end is QuantumOperation }
        val result = path?.lastOrNull()?.end as? QuantumOperation
        return when (path == result) {
            true -> result
            else -> null
        }
    }

    override fun cleanup() {
        // nothing to do
    }
}
