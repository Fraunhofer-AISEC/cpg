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
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.quantumcpg.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.passes.ComponentPass
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn

@DependsOn(QuantumEOGPass::class)
class QuantumDFGPass(ctx: TranslationContext) : ComponentPass(ctx) {

    val worklist: MutableList<QuantumOperation> = mutableListOf()

    override fun accept(p0: Component) {
        val allQuantumCircuits =
            p0.translationUnits
                .first()
                .additionalNodes
                .filterIsInstance<QuantumCircuit>() // TODO first

        for (circuit in allQuantumCircuits) {

            connectDFGWithinOps(circuit)

            // connect gates with each other
            val firstQuantumGate: QuantumOperation =
                circuit.operations.first {
                    when (it) {
                        is QuantumGate -> it.prevEOG.isEmpty()
                        is QuantumMeasure -> it.prevEOG.isEmpty()
                        else -> TODO()
                    }
                }

            for (qubit in circuit.quantumBits ?: arrayOf()) {

                if (operationIsRelevantForQubit(firstQuantumGate, qubit)) {
                    worklist.add(firstQuantumGate)
                } else {
                    advanceGateUntilRelevant(qubit, firstQuantumGate).forEach { worklist.add(it) }
                }

                // add DFG from declaration to first use in gate
                worklist.firstOrNull()?.let { connectQubitFirstUse(it, qubit) }

                while (worklist.isNotEmpty()) {
                    val currentOperation = worklist.removeFirst()
                    val nextOperation = advanceGateUntilRelevant(qubit, currentOperation)
                    connectOperationsForQubit(currentOperation, nextOperation, qubit)

                    // advance one step
                    nextOperation.forEach { worklist.add(it) }
                }
            }
        }
    }

    /*
    Connect the [currentOperation] with the [nextOperation] with respect to the provided [qubit]
     */
    private fun connectOperationsForQubit(
        currentOperation: QuantumOperation,
        nextOperation: List<QuantumOperation>,
        qubit: QuantumBit,
    ) {
        when (currentOperation) {
            is QuantumGateH -> {
                connectQubitWithNextOperation(currentOperation.quantumBit0, nextOperation)
            }
            is QuantumPauliGate -> {
                connectQubitWithNextOperation(currentOperation.quantumBit0, nextOperation)
            }
            is QuantumGateCX -> {
                if (currentOperation.quBit0.refersToQubit == qubit) {
                    connectQubitWithNextOperation(currentOperation.quBit0, nextOperation)
                }
                if (currentOperation.quBit1.refersToQubit == qubit) {
                    connectQubitWithNextOperation(currentOperation.quBit1, nextOperation)
                }
            }
            is QuantumRotationGate -> {
                connectQubitWithNextOperation(currentOperation.quantumBit, nextOperation)
            }
            is QuantumMeasure -> {
                ((currentOperation.cBit as? ClassicBitReference)?.refersToClassicBit as? ClassicBit)
                    ?.references
                    ?.forEach {
                        // Really ugly hack to account for having multiple refs to the bit
                        currentOperation.quBit.nextDFG += it
                    }
            }
            is ClassicIf -> {
                val thenStmt = currentOperation.thenStatement
                if (thenStmt != null) {
                    when (thenStmt) {
                        is QuantumPauliGate -> {
                            currentOperation.nextDFG += thenStmt.quantumBit0
                        }
                        else -> TODO()
                    }
                }
            }
            else -> TODO("not implemented: $currentOperation")
        }
    }

    /*
    Connect an (incoming) [QuantumBitReference] with a [QuantumOperation].
     */
    private fun connectQubitWithNextOperation(
        qubit: QuantumBitReference,
        nextOperation: List<QuantumOperation>,
    ) {
        for (nextOp in nextOperation) {
            when (nextOp) {
                is QuantumGateH -> {
                    qubit.nextDFG += (nextOp.quantumBit0)
                }
                is QuantumGateCX -> {
                    if (nextOp.quBit0.refersToQubit == qubit.refersToQubit) {
                        qubit.nextDFG += (nextOp.quBit0)
                    }
                    if (nextOp.quBit1.refersToQubit == qubit.refersToQubit) {
                        qubit.nextDFG += (nextOp.quBit1)
                    }
                }
                is QuantumPauliGate -> {
                    qubit.nextDFG += (nextOp.quantumBit0)
                }
                is QuantumMeasure -> {
                    qubit.nextDFG += (nextOp.quBit)
                }
                is ClassicIf -> {
                    connectQubitWithNextOperation(
                        qubit,
                        listOf(nextOp.thenStatement as? QuantumOperation ?: TODO()),
                    )
                }
                else -> {
                    TODO("connectQubitWithNextOperation not implemented: $nextOperation")
                }
            }
        }
    }

    private fun connectQubitFirstUse(firstOp: QuantumOperation, qubit: QuantumBit) {
        when (firstOp) {
            is QuantumGateH -> {
                qubit.nextDFG += (firstOp.quantumBit0)
            }
            is QuantumPauliGate -> {
                qubit.nextDFG += (firstOp.quantumBit0)
            }
            is QuantumGateCX -> {
                if (firstOp.quBit0.refersToQubit == qubit) {
                    qubit.nextDFG += (firstOp.quBit0)
                }
                if (firstOp.quBit1.refersToQubit == qubit) {
                    qubit.nextDFG += (firstOp.quBit1)
                }
            }
            is QuantumMeasure -> {
                qubit.nextDFG += (firstOp.quBit)
            }
            is ClassicIf -> {
                // TODO?
            }
        }
    }

    /*
    Checks whether a provided [QuantumOperation] is relevant for a given [QunatumBit] (i.e. the qubit is used in the operation).
     */
    private fun operationIsRelevantForQubit(op: QuantumOperation, qubit: QuantumBit): Boolean {
        return when (op) {
            is QuantumGateH -> {
                op.quantumBit0.refersToQubit == qubit
            }
            is QuantumPauliGate -> {
                op.quantumBit0.refersToQubit == qubit
            }
            is QuantumGateCX -> {
                op.quBit0.refersToQubit == qubit || op.quBit1.refersToQubit == qubit
            }
            is QuantumMeasure -> {
                op.quBit.refersToQubit == qubit
            }
            is QuantumRotationGate -> {
                op.quantumBit.refersToQubit == qubit
            }
            is ClassicIf -> {
                // Check condition
                val thenStmt = op.thenStatement as? QuantumOperation ?: TODO()
                // TODO LHS RHS are classic bit refs (not qubit)
                operationIsRelevantForQubit(thenStmt, qubit)
            }
            else -> TODO()
        }
    }

    /** Add DFG edges within all [QuantumOperation]s */
    private fun connectDFGWithinOps(circuit: QuantumCircuit) {
        // connect qubits input/output for gates
        for (op in circuit.operations) {
            when (op) {
                is QuantumGateCX -> {
                    // data flow from control bit to other bit
                    op.quBit0.nextDFG += (op.quBit1)
                }
                is QuantumPauliGate -> {
                    // nothing to do
                }
                is QuantumGateH -> {
                    // nothing to do
                }
                is QuantumRotationGate -> {
                    op.theta.nextDFG += (op.quantumBit)
                }
                is QuantumMeasure -> {
                    // data flow from qubit to classic bit
                    op.quBit.nextDFG += (op.cBit)
                }
                is ClassicIf -> {
                    // TODO: very hacky, but needed
                    val binOp = op.condition as? BinaryOperator ?: continue
                    binOp.prevDFG += (binOp.rhs)
                    binOp.prevDFG += (binOp.lhs)

                    op.prevDFG += (binOp)
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
        startNode: QuantumOperation?,
    ): List<QuantumOperation> {
        val candidates = nextOpEOG(startNode)
        val result: MutableList<QuantumOperation> = mutableListOf()
        for (nextNode in candidates) {
            if (operationIsRelevantForQubit(nextNode, qubit)) {
                result += nextNode
            } else {
                result += advanceGateUntilRelevant(qubit, nextNode)
            }
        }
        return result
    }

    /** Get the next QuantumOperation according to the EOG */
    private fun nextOpEOG(startOp: QuantumOperation?): List<QuantumOperation> {
        if (startOp == null) return emptyList()
        val result: MutableList<QuantumOperation> = mutableListOf()
        val workinglist: MutableList<Node> = mutableListOf()

        (startOp as? Node)?.nextEOG?.forEach { workinglist.add(it) }

        while (workinglist.isNotEmpty()) {
            when (val currentOperation = workinglist.removeFirst()) {
                is QuantumOperation -> {
                    result.add(currentOperation)
                }
                else -> {
                    (currentOperation as? Node)?.nextEOG?.forEach { workinglist.add(it) }
                }
            }
        }
        return result
    }

    override fun cleanup() {
        // nothing to do
    }
}
