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
package de.fraunhofer.aisec.cpg.graph.quantumcpg

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import org.slf4j.LoggerFactory

/** Builder for construction code property graph nodes. */
object QuantumNodeBuilder {
    private val LOGGER = LoggerFactory.getLogger(NodeBuilder::class.java)

    @JvmStatic
    @JvmOverloads
    fun newQuantumCircuit(
        cpgNode: Node? = null,
        quantumBits: Int = 0,
        classicBits: Int = 0,
    ): QuantumCircuit {
        val node = QuantumCircuit(cpgNode)
        node.quantumBits = Array(quantumBits) { newQuantumBit(cpgNode, node) }
        for (qubitIdx in node.quantumBits!!.indices) {
            node.quantumBits!![qubitIdx].name = Name("Qubit $qubitIdx")
        }

        node.classicBits = Array(classicBits) { newClassicBit(cpgNode, node) }
        for (cbitIdx in node.classicBits!!.indices) {
            node.classicBits!![cbitIdx].name = Name("Bit $cbitIdx")
        }
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumBit(cpgNode: Node? = null, quantumCircuit: QuantumCircuit): QuantumBit {
        val node = QuantumBit(cpgNode, quantumCircuit)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumBitRef(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        quBit: QuantumBit,
    ): QuantumBitReference {
        val node = QuantumBitReference(cpgNode, quantumCircuit, quBit)
        NodeBuilder.log(node)
        quBit.references.add(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newClassicBitRef(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        classicBit: Declaration,
    ): ClassicBitReference {
        val node = ClassicBitReference(cpgNode, quantumCircuit, classicBit)
        NodeBuilder.log(node)
        (classicBit as? ClassicBit)?.references?.add(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newClassicBit(cpgNode: Node? = null, quantumCircuit: QuantumCircuit): ClassicBit {
        val node = ClassicBit(cpgNode, quantumCircuit)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newClassicIf(cpgNode: Node? = null, quantumCircuit: QuantumCircuit? = null): ClassicIf {
        val node = ClassicIf(cpgNode)
        node.quantumCircuit = quantumCircuit
        quantumCircuit?.operations?.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumGateH(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        quantumBit0: QuantumBitReference,
    ): QuantumGateH {
        val node = QuantumGateH(cpgNode, quantumCircuit, quantumBit0)
        quantumBit0.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumGateS(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        quantumBit0: QuantumBitReference,
    ): QuantumGateS {
        val node = QuantumGateS(cpgNode, quantumCircuit, quantumBit0)
        quantumBit0.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumGateSdg(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        quantumBit0: QuantumBitReference,
    ): QuantumGateSdg {
        val node = QuantumGateSdg(cpgNode, quantumCircuit, quantumBit0)
        quantumBit0.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumGateT(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        quantumBit0: QuantumBitReference,
    ): QuantumGateT {
        val node = QuantumGateT(cpgNode, quantumCircuit, quantumBit0)
        quantumBit0.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumGateTdg(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        quantumBit0: QuantumBitReference,
    ): QuantumGateTdg {
        val node = QuantumGateTdg(cpgNode, quantumCircuit, quantumBit0)
        quantumBit0.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumGateX(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        quantumBit0: QuantumBitReference,
    ): QuantumGateX {
        val node = QuantumGateX(cpgNode, quantumCircuit, quantumBit0)
        quantumBit0.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumGateY(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        quantumBit0: QuantumBitReference,
    ): QuantumGateY {
        val node = QuantumGateY(cpgNode, quantumCircuit, quantumBit0)
        quantumBit0.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumGateZ(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        quantumBit0: QuantumBitReference,
    ): QuantumGateZ {
        val node = QuantumGateZ(cpgNode, quantumCircuit, quantumBit0)
        quantumBit0.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumRotationXGate(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        theta: Node,
        quantumBit0: QuantumBitReference,
    ): QuantumRotationXGate {
        val node = QuantumRotationXGate(cpgNode, quantumCircuit, theta, quantumBit0)
        quantumBit0.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumRotationYGate(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        theta: Node,
        quantumBit0: QuantumBitReference,
    ): QuantumRotationYGate {
        val node = QuantumRotationYGate(cpgNode, quantumCircuit, theta, quantumBit0)
        quantumBit0.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumRotationZGate(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        theta: Node,
        quantumBit0: QuantumBitReference,
    ): QuantumRotationZGate {
        val node = QuantumRotationZGate(cpgNode, quantumCircuit, theta, quantumBit0)
        quantumBit0.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumGateCX(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        quantumBit0: QuantumBitReference,
        quantumBit1: QuantumBitReference,
    ): QuantumGateCX {
        val node = QuantumGateCX(cpgNode, quantumCircuit, quantumBit0, quantumBit1)
        quantumBit0.refersToQubit.relevantForGates.add(node)
        quantumBit1.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumGateToffoli(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        quantumBit0: QuantumBitReference,
        quantumBit1: QuantumBitReference,
        quantumBit2: QuantumBitReference,
    ): QuantumToffoliGate {
        val node =
            QuantumToffoliGate(cpgNode, quantumCircuit, quantumBit0, quantumBit1, quantumBit2)
        quantumBit0.refersToQubit.relevantForGates.add(node)
        quantumBit1.refersToQubit.relevantForGates.add(node)
        quantumBit2.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumMeasurement(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit,
        qubit: QuantumBitReference,
        classicBit: Expression,
    ): QuantumMeasure {
        val node = QuantumMeasure(cpgNode, quantumCircuit, qubit, classicBit)
        qubit.refersToQubit.relevantForGates.add(node)
        quantumCircuit.operations.add(node)
        NodeBuilder.log(node)
        return node
    }
}
