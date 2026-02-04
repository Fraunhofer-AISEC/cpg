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
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.quantumcpg.*
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newClassicBitRef
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumBitRef
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumCircuit
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.quantumcpg.ClassicBitType
import de.fraunhofer.aisec.cpg.graph.types.quantumcpg.QuantumBitType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.RequiredFrontend
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
                cBits.size,
            )
        p0.additionalNodes.add(quantumCircuit)

        val allQuantumExpressions = flatAST.filterIsInstance<CallExpression>()
        for (expr in allQuantumExpressions) {
            var newOperation: QuantumOperation? = null
            when (expr.name.localName) {
                "h" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    quBit?.let {
                        val quBitRef =
                            newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                        newOperation =
                            QuantumNodeBuilder.newQuantumGateH(expr, quantumCircuit, quBitRef)
                    }
                }
                "s" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    quBit?.let {
                        val quBitRef =
                            newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                        newOperation =
                            QuantumNodeBuilder.newQuantumGateS(expr, quantumCircuit, quBitRef)
                    }
                }
                "sdg" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    quBit?.let {
                        val quBitRef =
                            newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                        newOperation =
                            QuantumNodeBuilder.newQuantumGateSdg(expr, quantumCircuit, quBitRef)
                    }
                }
                "t" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    quBit?.let {
                        val quBitRef =
                            newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                        newOperation =
                            QuantumNodeBuilder.newQuantumGateT(expr, quantumCircuit, quBitRef)
                    }
                }
                "tdg" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    quBit?.let {
                        val quBitRef =
                            newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                        newOperation =
                            QuantumNodeBuilder.newQuantumGateTdg(expr, quantumCircuit, quBitRef)
                    }
                }
                "x" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    quBit?.let {
                        val quBitRef =
                            newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                        newOperation =
                            QuantumNodeBuilder.newQuantumGateX(expr, quantumCircuit, quBitRef)
                    }
                }
                "y" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    quBit?.let {
                        val quBitRef =
                            newQuantumBitRef(expr.arguments.first(), quantumCircuit, quBit)
                        newOperation =
                            QuantumNodeBuilder.newQuantumGateY(expr, quantumCircuit, quBitRef)
                    }
                }
                "z" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[0])

                    quBit?.let {
                        val quBitRef = newQuantumBitRef(expr.arguments.first(), quantumCircuit, it)
                        newOperation =
                            QuantumNodeBuilder.newQuantumGateZ(expr, quantumCircuit, quBitRef)
                    }
                }
                "rx" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[1])

                    quBit?.let {
                        val quBitRef = newQuantumBitRef(expr.arguments.first(), quantumCircuit, it)
                        newOperation =
                            QuantumNodeBuilder.newQuantumRotationXGate(
                                expr,
                                quantumCircuit,
                                expr.arguments[0],
                                quBitRef,
                            )
                    }
                }
                "ry" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[1])

                    quBit?.let {
                        val quBitRef = newQuantumBitRef(expr.arguments.first(), quantumCircuit, it)
                        newOperation =
                            QuantumNodeBuilder.newQuantumRotationYGate(
                                expr,
                                quantumCircuit,
                                expr.arguments[0],
                                quBitRef,
                            )
                    }
                }
                "rz" -> {
                    val quBit = getArgAsQubit(quantumCircuit, expr.arguments[1])

                    quBit?.let {
                        val quBitRef = newQuantumBitRef(expr.arguments.first(), quantumCircuit, it)
                        newOperation =
                            QuantumNodeBuilder.newQuantumRotationZGate(
                                expr,
                                quantumCircuit,
                                expr.arguments[0],
                                quBitRef,
                            )
                    }
                }
                "cx" -> {
                    val quBit0 = getArgAsQubit(quantumCircuit, expr.arguments[0])
                    val quBit1 = getArgAsQubit(quantumCircuit, expr.arguments[1])

                    val quBitRef0 =
                        quBit0?.let { newQuantumBitRef(expr.arguments[0], quantumCircuit, it) }

                    val quBitRef1 =
                        quBit1?.let { newQuantumBitRef(expr.arguments[1], quantumCircuit, it) }

                    newOperation =
                        quBitRef0?.let {
                            quBitRef1?.let { it1 ->
                                QuantumNodeBuilder.newQuantumGateCX(expr, quantumCircuit, it, it1)
                            }
                        }
                }
                "ccx" -> {
                    val quBit0 = getArgAsQubit(quantumCircuit, expr.arguments[0])
                    val quBit1 = getArgAsQubit(quantumCircuit, expr.arguments[1])
                    val quBit2 = getArgAsQubit(quantumCircuit, expr.arguments[2])

                    val quBitRef0 =
                        quBit0?.let { newQuantumBitRef(expr.arguments[0], quantumCircuit, it) }

                    val quBitRef1 =
                        quBit1?.let { newQuantumBitRef(expr.arguments[1], quantumCircuit, it) }
                    val quBitRef2 =
                        quBit2?.let { newQuantumBitRef(expr.arguments[2], quantumCircuit, it) }

                    newOperation =
                        quBitRef0?.let { it0 ->
                            quBitRef1?.let { it1 ->
                                quBitRef2?.let { it2 ->
                                    QuantumNodeBuilder.newQuantumGateToffoli(
                                        expr,
                                        quantumCircuit,
                                        it0,
                                        it1,
                                        it2,
                                    )
                                }
                            }
                        }
                }
                "draw" -> {}
                "measure" -> {
                    val qubit = getArgAsQubit(quantumCircuit, expr.arguments[0])
                    if (expr.arguments[1] !is Reference) TODO()
                    val name = "Bit " + expr.arguments[1].name
                    val classicBitList =
                        quantumCircuit.classicBits?.filter { it.name == Name(name) }
                    val classicBit: Declaration =
                        classicBitList?.singleOrNull()
                            ?: newProblemDeclaration(
                                "Measuring to Bit with name ${name} led to ${classicBitList?.size} results"
                            )

                    qubit?.let {
                        newOperation =
                            QuantumNodeBuilder.newQuantumMeasurement(
                                expr,
                                quantumCircuit,
                                newQuantumBitRef(expr, quantumCircuit, it),
                                newClassicBitRef(expr, quantumCircuit, classicBit),
                            )
                    }
                }
                else -> {
                    val argQuBits =
                        expr.arguments.mapNotNull { arg ->
                            getArgAsQubit(quantumCircuit, arg)?.let { qubit ->
                                newQuantumBitRef(arg, quantumCircuit, qubit)
                            }
                        }

                    UnknownQuantumGate(expr, quantumCircuit, *argQuBits.toTypedArray())
                }
            }

            // save the gate to the graph
            if (newOperation != null) {
                (newOperation as? Statement)?.let { quantumCircuit.statements += it }
                p0.additionalNodes.add(newOperation as? Node ?: TODO())
            }
        }
    }

    private fun getArgAsQubit(circuit: QuantumCircuit, ref: Node): QuantumBit? {
        if (ref !is Reference) TODO()
        val name =
            "Qubit " + ref.name[2] // TODO: Ideally, the name should be some FQN and differ between
        // different
        // custom gates.
        val qubit = circuit.quantumBits?.singleOrNull { it.name == Name(name) }
        return qubit
    }

    override fun cleanup() {
        // nothing to do
    }
}
