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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.QiskitNodeBuilder.newQuantumCircuit
import de.fraunhofer.aisec.cpg.graph.QiskitNodeBuilder.newQuantumGateCX
import de.fraunhofer.aisec.cpg.graph.QiskitNodeBuilder.newQuantumGateH
import de.fraunhofer.aisec.cpg.graph.QiskitNodeBuilder.newQuantumMeasure
import de.fraunhofer.aisec.cpg.graph.QuantumCircuit
import de.fraunhofer.aisec.cpg.graph.QuantumGate
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.order.RequiredFrontend

@RequiredFrontend(PythonLanguageFrontend::class)
class QiskitPass : Pass() {
    private val quantumCircuitsMap = HashMap<Node, QuantumCircuit>()

    override fun accept(p0: TranslationResult) {

        val flatAST = SubgraphWalker.flattenAST(p0)

        val allQuantumCircuits =
            flatAST.filter { n ->
                n is VariableDeclaration &&
                    n.initializer != null &&
                    n.initializer is CallExpression &&
                    (n.initializer as CallExpression).name.localName == "QuantumCircuit"
            }

        for (circuit in allQuantumCircuits) {
            var quantumBits = 0
            var classicBits = 0

            // circuit = QuantumCircuit(1, 2)
            if (
                circuit is VariableDeclaration &&
                    circuit.initializer != null &&
                    circuit.initializer is CallExpression &&
                    (circuit.initializer as CallExpression).invokes.isNotEmpty() &&
                    (circuit.initializer as CallExpression).invokes[0]?.name?.localName ==
                        "QuantumCircuit" &&
                    (circuit.initializer as CallExpression).arguments.size == 2
            ) {
                val arg0 =
                    (circuit.initializer as CallExpression).arguments[0] as? Literal<Long> // TODO
                val arg1 =
                    (circuit.initializer as CallExpression).arguments[1] as? Literal<Long> // TODO
                if (arg0 != null && arg1 != null) {
                    quantumBits = arg0.value!!.toInt() // TODO
                    classicBits = arg1.value!!.toInt() // TODO
                }
            }
            val graphNode = newQuantumCircuit(circuit, quantumBits, classicBits)
            quantumCircuitsMap[circuit] = graphNode
        }

        val allQuantumExpressions =
            flatAST.filter { n ->
                n is MemberCallExpression &&
                    n.base is DeclaredReferenceExpression &&
                    quantumCircuitsMap.containsKey(
                        ((n.base as? DeclaredReferenceExpression)?.refersTo) as? Node
                    )
            }

        for (expr in allQuantumExpressions) {
            val currentCircuit =
                quantumCircuitsMap[
                    ((expr as? MemberCallExpression)?.base as? DeclaredReferenceExpression)
                        ?.refersTo as? Node]
            var newGate: QuantumGate? = null
            when (expr.name.localName) {
                "h" -> {

                    val idx = getArgAsInt(expr as CallExpression, 0)
                    newGate =
                        newQuantumGateH(expr, currentCircuit, currentCircuit?.quantumBits?.get(idx))
                }
                "cx" -> {
                    val idx0 = getArgAsInt(expr as MemberCallExpression, 0)
                    val idx1 = getArgAsInt(expr, 1)
                    newGate =
                        newQuantumGateCX(
                            expr,
                            currentCircuit,
                            currentCircuit?.quantumBits?.get(idx0),
                            currentCircuit?.quantumBits?.get(idx1)
                        )
                }
                "measure" -> {
                    val args = (expr as? MemberCallExpression)?.arguments
                    if (args?.size == 2) {
                        val arg0 = args[0]
                        val arg1 = args[1]
                        if (
                            arg0 != null &&
                                arg0 is InitializerListExpression &&
                                arg1 != null &&
                                arg1 is InitializerListExpression
                        ) {
                            // e.g. measure([0, 1], [0, 1])

                            if (arg0.initializers.size != arg1.initializers.size) {
                                TODO()
                            } else {
                                val newMeasureNode = newQuantumMeasure(expr, currentCircuit)
                                for (i in arg0.initializers.indices) {
                                    val qbitIdx =
                                        getIntFromInitializer(arg0.initializers[i] as Literal<*>)
                                    val cbitIdx =
                                        getIntFromInitializer(arg1.initializers[i] as Literal<*>)
                                    newMeasureNode.addMeasurement(
                                        qbitIdx?.let { currentCircuit?.getQbitByIdx(it) },
                                        cbitIdx?.let { currentCircuit?.getCbitByIdx(it) }
                                    )
                                }
                                p0.additionalNodes.add(newMeasureNode)
                            }
                        } else TODO()
                    } else {
                        TODO()
                    }
                }
                "draw" -> {}
                else -> TODO("not implemented")
            }

            // save the gate to the graph
            if (newGate != null) {
                p0.additionalNodes.add(newGate)
            }
        }

        // save the new nodes
        for (node in quantumCircuitsMap.values) {
            p0.additionalNodes.add(node)
        }
    }

    private fun getArgAsInt(call: CallExpression, idx: Int): Int {
        val args = call.arguments
        val arg = args[idx] as? Literal<*> ?: TODO()
        // TODO localName
        if (arg.type.name.localName != "int") {
            TODO()
        }
        return (arg.value as? Long)?.toInt() ?: TODO()
    }

    private fun getIntFromInitializer(a: Literal<*>): Int? {

        return (a.value as? Number)?.toInt()
    }

    override fun cleanup() {
        // nothing to do
    }
}
