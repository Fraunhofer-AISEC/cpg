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
package de.fraunhofer.aisec.cpg.frontends.openqasm.passes

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.openqasm.OpenQasmLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumBit
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumCircuit
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumCircuit
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumOperation
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.quantumcpg.ClassicBitType
import de.fraunhofer.aisec.cpg.graph.types.quantumcpg.QuantumBitType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.CallResolver
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import de.fraunhofer.aisec.cpg.passes.order.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.order.RequiredFrontend
import de.fraunhofer.aisec.cpg.passes.quantumcpg.QuantumDFGPass
import de.fraunhofer.aisec.cpg.passes.quantumcpg.QuantumEOGPass

@RequiredFrontend(OpenQasmLanguageFrontend::class)
@DependsOn(VariableUsageResolver::class)
@DependsOn(CallResolver::class)
@ExecuteBefore(QuantumEOGPass::class)
@ExecuteBefore(QuantumDFGPass::class)
class OpenQASMPass : Pass() {

    override fun accept(p0: TranslationResult) {

        val flatAST = SubgraphWalker.flattenAST(p0)

        val quBits =
            flatAST.filterIsInstance<VariableDeclaration>().filter { it.type is QuantumBitType }
        val cBits =
            flatAST.filterIsInstance<VariableDeclaration>().filter { it.type is ClassicBitType }

        val quantumCircuit =
            newQuantumCircuit(
                flatAST[2] as? TranslationUnitDeclaration ?: TODO(),
                quBits.size,
                cBits.size
            )
        p0.additionalNodes.add(quantumCircuit)

        val allQuantumExpressions = flatAST.filterIsInstance<CallExpression>()
        for (expr in allQuantumExpressions) {
            var newOperation: QuantumOperation? = null
            when (expr.name.localName) {
                "h" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    val quBitRef =
                        QuantumNodeBuilder.newQuantumBitRef(
                            expr.arguments.first(),
                            quantumCircuit,
                            quBit
                        )
                    newOperation =
                        QuantumNodeBuilder.newQuantumGateH(expr, quantumCircuit, quBitRef)
                }
                "cx" -> {
                    val quBit0 = getArgAsQubit(quantumCircuit, expr.arguments[0])
                    val quBit1 = getArgAsQubit(quantumCircuit, expr.arguments[1])

                    val quBitRef0 =
                        QuantumNodeBuilder.newQuantumBitRef(
                            expr.arguments.first(),
                            quantumCircuit,
                            quBit0
                        )

                    val quBitRef1 =
                        QuantumNodeBuilder.newQuantumBitRef(
                            expr.arguments.first(),
                            quantumCircuit,
                            quBit1
                        )

                    newOperation =
                        QuantumNodeBuilder.newQuantumGateCX(
                            expr,
                            quantumCircuit,
                            quBitRef0,
                            quBitRef1
                        )
                }
                "draw" -> {}
                "measure" -> {
                    val qubit = getArgAsQubit(quantumCircuit, expr.arguments[0])
                    if (expr.arguments[1] !is DeclaredReferenceExpression) TODO()
                    val name = "Bit " + expr.arguments[1].name[2]
                    val cbits = quantumCircuit.classicBits?.filter { it.name.equals(Name(name)) }
                    if (cbits?.size != 1) TODO()
                    val cBit = cbits.first()

                    newOperation = QuantumNodeBuilder.newQuantumMeasure(expr, quantumCircuit)
                    newOperation.addMeasurement(qubit, cBit)
                }
                else -> TODO("not implemented")
            }

            // save the gate to the graph
            if (newOperation != null) {
                quantumCircuit.operations.add(newOperation)
                p0.additionalNodes.add(newOperation as? Node ?: TODO())
            }
        }
    }

    private fun getArgAsQubit(circuit: QuantumCircuit, ref: Node): QuantumBit {
        // TODO not really correct.... Find a solution for the naming problem...
        if (ref !is DeclaredReferenceExpression) TODO()
        val name = "Qubit " + ref.name[2]
        val qubit = circuit.quantumBits?.filter { it.name.equals(Name(name)) }
        if (qubit?.size != 1) TODO()
        return qubit.first()
    }

    private fun getIntFromInitializer(a: Literal<*>): Int? {

        return (a.value as? Number)?.toInt()
    }

    override fun cleanup() {
        // nothing to do
    }
}
