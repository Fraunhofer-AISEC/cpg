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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.openqasm.OpenQasmLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumBit
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumCircuit
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newClassicBitRef
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumBitRef
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumCircuit
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumOperation
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.quantumcpg.ClassicBitType
import de.fraunhofer.aisec.cpg.graph.types.quantumcpg.QuantumBitType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import de.fraunhofer.aisec.cpg.passes.order.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.order.RequiredFrontend
import de.fraunhofer.aisec.cpg.passes.quantumcpg.QuantumDFGPass
import de.fraunhofer.aisec.cpg.passes.quantumcpg.QuantumEOGPass

@RequiredFrontend(OpenQasmLanguageFrontend::class)
@DependsOn(SymbolResolver::class)
@ExecuteBefore(QuantumEOGPass::class)
@ExecuteBefore(QuantumDFGPass::class)
class OpenQASMPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {

    override fun accept(p0: TranslationUnitDeclaration) {

        val flatAST = SubgraphWalker.flattenAST(p0)

        val quBits =
            flatAST.filterIsInstance<VariableDeclaration>().filter { it.type is QuantumBitType }
        val cBits =
            flatAST.filterIsInstance<VariableDeclaration>().filter { it.type is ClassicBitType }

        val quantumCircuit: QuantumCircuit =
            newQuantumCircuit(
                flatAST[0] as? TranslationUnitDeclaration ?: TODO(),
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

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                    newOperation =
                        QuantumNodeBuilder.newQuantumGateH(expr, quantumCircuit, quBitRef)
                }
                "s" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                    newOperation =
                        QuantumNodeBuilder.newQuantumGateS(expr, quantumCircuit, quBitRef)
                }
                "sdg" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                    newOperation =
                        QuantumNodeBuilder.newQuantumGateSdg(expr, quantumCircuit, quBitRef)
                }
                "t" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                    newOperation =
                        QuantumNodeBuilder.newQuantumGateT(expr, quantumCircuit, quBitRef)
                }
                "tdg" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                    newOperation =
                        QuantumNodeBuilder.newQuantumGateTdg(expr, quantumCircuit, quBitRef)
                }
                "x" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                    newOperation =
                        QuantumNodeBuilder.newQuantumGateX(expr, quantumCircuit, quBitRef)
                }
                "y" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                    newOperation =
                        QuantumNodeBuilder.newQuantumGateY(expr, quantumCircuit, quBitRef)
                }
                "z" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                    newOperation =
                        QuantumNodeBuilder.newQuantumGateZ(expr, quantumCircuit, quBitRef)
                }
                "cx" -> {
                    val quBit0 = getArgAsQubit(quantumCircuit, expr.arguments[0])
                    val quBit1 = getArgAsQubit(quantumCircuit, expr.arguments[1])

                    val quBitRef0 = newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit0)

                    val quBitRef1 = newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit1)

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
                    if (expr.arguments[1] !is Reference) TODO()
                    val name = "Bit " + expr.arguments[1].name[2]
                    val classicBitList =
                        quantumCircuit.classicBits?.filter { it.name == Name(name) }
                    if (classicBitList?.size != 1) TODO()
                    val classicBit = classicBitList.first()

                    newOperation =
                        QuantumNodeBuilder.newQuantumMeasurement(
                            expr,
                            quantumCircuit,
                            newQuantumBitRef(expr, quantumCircuit, qubit),
                            newClassicBitRef(expr, quantumCircuit, classicBit)
                        )
                }
                else -> TODO("not implemented")
            }

            // save the gate to the graph
            if (newOperation != null) {
                (newOperation as? Statement)?.let { quantumCircuit.statements += it }
                p0.additionalNodes.add(newOperation as? Node ?: TODO())
            }
        }
    }

    private fun getArgAsQubit(circuit: QuantumCircuit, ref: Node): QuantumBit {
        // TODO not really correct.... Find a solution for the naming problem...
        if (ref !is Reference) TODO()
        val name = "Qubit " + ref.name[2]
        val qubit = circuit.quantumBits?.filter { it.name == Name(name) }
        if (qubit?.size != 1) TODO()
        return qubit.first()
    }

    override fun cleanup() {
        // nothing to do
    }
}
