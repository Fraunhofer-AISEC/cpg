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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumCircuit
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumGate
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newClassicBitRef
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newClassicIf
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumBitRef
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumCircuit
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumGateCX
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumGateH
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumGateS
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumGateSdg
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumGateT
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumGateTdg
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumGateX
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumGateY
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumGateZ
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumMeasurement
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumRotationXGate
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumRotationYGate
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newQuantumRotationZGate
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.RequiredFrontend
import de.fraunhofer.aisec.cpg.passes.quantumcpg.QuantumDFGPass
import de.fraunhofer.aisec.cpg.passes.quantumcpg.QuantumEOGPass

@RequiredFrontend(PythonLanguageFrontend::class)
@DependsOn(SymbolResolver::class)
@DependsOn(EvaluationOrderGraphPass::class)
@ExecuteBefore(QuantumEOGPass::class)
@ExecuteBefore(QuantumDFGPass::class)
class QiskitPass(ctx: TranslationContext) : ComponentPass(ctx) {
    private val quantumCircuitsMap = HashMap<Node, QuantumCircuit>()

    override fun accept(comp: Component) {

        val flatAST = SubgraphWalker.flattenAST(comp.translationUnits.first()) // TODO first

        val allQuantumCircuits2 =
            flatAST
                .mapNotNull {
                    if (it !is AssignExpression) {
                        return@mapNotNull null
                    }
                    it.rhs
                        .filterIsInstance<CallExpression>()
                        .filter { call ->
                            call.name.localName == "QuantumCircuit" &&
                                (call.arguments.size == 2 || call.arguments.size == 1)
                        }
                        .flatMap { call ->
                            it.findTargets(call).mapNotNull { lhs ->
                                if (call.arguments.size == 1 && call.arguments[0] is Literal<*>) {
                                    Triple(
                                        (lhs as? Reference)?.refersTo,
                                        (call.arguments[0].evaluate() as? Number)?.toInt(),
                                        0,
                                    )
                                } else if (
                                    call.arguments.size == 2 &&
                                        call.arguments[0] is Literal<*> &&
                                        call.arguments[1] is Literal<*>
                                ) {
                                    Triple(
                                        (lhs as? Reference)?.refersTo,
                                        (call.arguments[0].evaluate() as? Number)?.toInt(),
                                        (call.arguments[1].evaluate() as? Number)?.toInt(),
                                    )
                                } else {
                                    log.warn(
                                        "Cannot handle call to QuantumCircuit with arguments ${call.arguments.map { it.type }}! Please implement this feature in the QiskitPass."
                                    )
                                    null
                                }
                            }
                        }
                }
                .flatten()

        for ((circuit, quantumBits, classicBits) in allQuantumCircuits2) {
            if (circuit != null && quantumBits != null && classicBits != null) {
                // circuit = QuantumCircuit(1, 2)
                val graphNode = newQuantumCircuit(circuit, quantumBits, classicBits)
                quantumCircuitsMap[circuit] = graphNode
            } else {
                log.warn("Found circuit $circuit but something is null. This shouldn't happen...")
            }
        }

        val allQuantumExpressions =
            flatAST.filter { n ->
                n is MemberCallExpression &&
                    n.base is Reference &&
                    quantumCircuitsMap.containsKey(((n.base as? Reference)?.refersTo) as? Node)
            }

        for (expr in allQuantumExpressions) {
            val currentCircuit: QuantumCircuit =
                quantumCircuitsMap[
                    ((expr as? MemberCallExpression)?.base as? Reference)?.refersTo as? Node]
                    ?: TODO()
            var newGate: QuantumGate? = null

            when (expr.name.localName) {
                "h" -> {
                    val idx = getArgAsInt(expr as CallExpression, 0)
                    val quBit = currentCircuit.quantumBits?.get(idx) ?: continue
                    val quBitRef = newQuantumBitRef(expr.arguments.first(), currentCircuit, quBit)
                    newGate = newQuantumGateH(expr, currentCircuit, quBitRef)
                }
                "s" -> {
                    val idx = getArgAsInt(expr as MemberCallExpression, 0)
                    val quBit = currentCircuit.quantumBits?.get(idx) ?: continue

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), currentCircuit, quBit)
                    newGate = newQuantumGateS(expr, currentCircuit, quBitRef)
                }
                "sdg" -> {
                    val idx = getArgAsInt(expr as MemberCallExpression, 0)
                    val quBit = currentCircuit.quantumBits?.get(idx) ?: continue

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), currentCircuit, quBit)
                    newGate = newQuantumGateSdg(expr, currentCircuit, quBitRef)
                }
                "t" -> {
                    val idx = getArgAsInt(expr as MemberCallExpression, 0)
                    val quBit = currentCircuit.quantumBits?.get(idx) ?: continue

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), currentCircuit, quBit)
                    newGate = newQuantumGateT(expr, currentCircuit, quBitRef)
                }
                "tdg" -> {
                    val idx = getArgAsInt(expr as MemberCallExpression, 0)
                    val quBit = currentCircuit.quantumBits?.get(idx) ?: continue

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), currentCircuit, quBit)
                    newGate = newQuantumGateTdg(expr, currentCircuit, quBitRef)
                }
                "x" -> {
                    val idx = getArgAsInt(expr as MemberCallExpression, 0)
                    val quBit = currentCircuit.quantumBits?.get(idx) ?: continue

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), currentCircuit, quBit)
                    newGate = newQuantumGateX(expr, currentCircuit, quBitRef)
                }
                "y" -> {
                    val idx = getArgAsInt(expr as MemberCallExpression, 0)
                    val quBit = currentCircuit.quantumBits?.get(idx) ?: continue

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), currentCircuit, quBit)
                    newGate = newQuantumGateY(expr, currentCircuit, quBitRef)
                }
                "z" -> {
                    val idx = getArgAsInt(expr as MemberCallExpression, 0)
                    val quBit = currentCircuit.quantumBits?.get(idx) ?: continue

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), currentCircuit, quBit)
                    newGate = newQuantumGateZ(expr, currentCircuit, quBitRef)
                }
                "rx" -> {
                    val idx = getArgAsInt(expr as MemberCallExpression, 1)
                    val quBit = currentCircuit.quantumBits?.get(idx) ?: continue

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), currentCircuit, quBit)
                    newGate =
                        newQuantumRotationXGate(expr, currentCircuit, expr.arguments[0], quBitRef)
                }
                "ry" -> {
                    val idx = getArgAsInt(expr as MemberCallExpression, 1)
                    val quBit = currentCircuit.quantumBits?.get(idx) ?: continue

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), currentCircuit, quBit)
                    newGate =
                        newQuantumRotationYGate(expr, currentCircuit, expr.arguments[0], quBitRef)
                }
                "rz" -> {
                    val idx = getArgAsInt(expr as MemberCallExpression, 1)
                    val quBit = currentCircuit.quantumBits?.get(idx) ?: continue

                    val quBitRef = newQuantumBitRef(expr.arguments.first(), currentCircuit, quBit)
                    newGate =
                        newQuantumRotationZGate(expr, currentCircuit, expr.arguments[0], quBitRef)
                }
                "cx" -> {
                    val idx0 = getArgAsInt(expr as MemberCallExpression, 0)
                    val idx1 = getArgAsInt(expr, 1)
                    val quBit0 = currentCircuit.quantumBits?.get(idx0) ?: continue
                    val quBitRef0 = newQuantumBitRef(expr.arguments.first(), currentCircuit, quBit0)

                    val quBit1 = currentCircuit.quantumBits?.get(idx1) ?: continue
                    val quBitRef1 = newQuantumBitRef(expr.arguments.first(), currentCircuit, quBit1)

                    newGate = newQuantumGateCX(expr, currentCircuit, quBitRef0, quBitRef1)
                }
                "measure" -> {
                    val args = (expr as? MemberCallExpression)?.arguments
                    if (args?.size == 2) {
                        val arg0 = args[0]
                        val arg1 = args[1]
                        if (
                            arg0 is InitializerListExpression && arg1 is InitializerListExpression
                        ) {
                            // e.g. measure([0, 1], [0, 1])

                            if (arg0.initializers.size != arg1.initializers.size) {
                                TODO()
                            } else {

                                for (i in arg0.initializers.indices) {
                                    val qubitIdx =
                                        getIntFromInitializer(arg0.initializers[i] as Literal<*>)
                                    val cbitIdx =
                                        getIntFromInitializer(arg1.initializers[i] as Literal<*>)
                                    val newMeasureNode =
                                        newQuantumMeasurement(
                                            expr,
                                            currentCircuit,
                                            newQuantumBitRef(
                                                expr,
                                                currentCircuit,
                                                qubitIdx?.let { currentCircuit.getQubitByIdx(it) }
                                                    ?: TODO(),
                                            ),
                                            newClassicBitRef(
                                                expr,
                                                currentCircuit,
                                                cbitIdx?.let { currentCircuit.getCbitByIdx(it) }
                                                    ?: TODO(),
                                            ),
                                        )
                                    comp.translationUnits
                                        .first()
                                        .additionalNodes
                                        .add(newMeasureNode) // TODO first
                                    currentCircuit.statements += newMeasureNode
                                }
                            }
                        } else TODO()
                    } else {
                        TODO()
                    }
                }
                "draw" -> {}
                "initialize" -> {
                    // TODO
                }
                else -> TODO("not implemented: ${expr.name.localName}")
            }

            // save the gate to the graph
            if (newGate != null) {
                // before we save it, we need to check, whether that this gate is "wrapped" in an
                // ClassicalIf. For now this is a quite simple heuristic. We just check whether the
                // next EOG is a member call to c_if.
                val next = expr.nextEOG.firstOrNull()
                if (next?.name?.localName == "c_if" && next is MemberExpression) {
                    // For some reason astParent doesn't work properly, so we need to follow the EOG
                    // until we hit the call expression of this member expression
                    val path =
                        next.followNextEOG {
                            it.end is MemberCallExpression &&
                                (it.end as MemberCallExpression).callee == next
                        }
                    val call = path?.lastOrNull()?.end as? CallExpression ?: continue
                    val cIf = newClassicIf(call, newGate.quantumCircuit)
                    val binOp = next.newBinaryOperator("==")
                    // mark it as part of the quantum graph
                    binOp.additionalLabels = mutableListOf("QuantumNode")
                    val idx = getArgAsInt(call, 0)
                    val cBit = currentCircuit.classicBits?.get(idx) ?: continue
                    val quBitRef = newClassicBitRef(call.arguments.first(), currentCircuit, cBit)
                    binOp.lhs = quBitRef
                    // only works with literals for now
                    val lit = (call.arguments[1] as Literal<*>).duplicate(true)
                    lit.disconnectFromGraph()
                    // mark it as part of the quantum graph
                    lit.additionalLabels = mutableListOf("QuantumNode")
                    binOp.rhs = lit
                    cIf.condition = binOp
                    cIf.thenStatement = newGate
                    comp.translationUnits.first().additionalNodes.add(cIf) // TODO first
                    comp.translationUnits
                        .first()
                        .additionalNodes
                        .add(newGate) // not sure if we want it this way // TODO first
                    currentCircuit.statements += cIf
                } else {
                    currentCircuit.statements += newGate
                    comp.translationUnits.first().additionalNodes.add(newGate) // TODO first
                }
            }
        }

        // save the new nodes
        for (node in quantumCircuitsMap.values) {
            comp.translationUnits.first().additionalNodes.add(node) // TODO first
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
