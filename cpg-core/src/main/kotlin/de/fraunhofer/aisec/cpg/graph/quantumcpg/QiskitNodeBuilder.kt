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
import org.slf4j.LoggerFactory

/** Builder for construction code property graph nodes. */
object QiskitNodeBuilder {
    private val LOGGER = LoggerFactory.getLogger(NodeBuilder::class.java)

    @JvmStatic
    @JvmOverloads
    fun newQuantumCircuit(
        cpgNode: Node? = null,
        quantumBits: Int = 0,
        classicBits: Int = 0
    ): QuantumCircuit {
        val node = QuantumCircuit(cpgNode)
        node.quantumBits = Array(quantumBits) { newQuantumBit(cpgNode, node) }
        for (qbitIdx in node.quantumBits!!.indices) {
            node.quantumBits!![qbitIdx].name = Name("QBit $qbitIdx")
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
    fun newQuantumBit(cpgNode: Node? = null, quantumCircuit: QuantumCircuit? = null): QuantumBit {
        val node = QuantumBit(cpgNode)
        node.quantumCircuit = quantumCircuit
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newClassicBit(cpgNode: Node? = null, quantumCircuit: QuantumCircuit? = null): ClassicBit {
        val node = ClassicBit(cpgNode)
        node.quantumCircuit = quantumCircuit
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumGateH(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit? = null,
        quantumBit0: QuantumBit? = null
    ): QuantumGateH {
        val node = QuantumGateH(cpgNode)
        node.quantumCircuit = quantumCircuit
        node.quantumBit0 = quantumBit0
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumGateCX(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit? = null,
        quantumBit0: QuantumBit? = null,
        quantumBit1: QuantumBit? = null
    ): QuantumGateCX {
        val node = QuantumGateCX(cpgNode)
        node.quantumCircuit = quantumCircuit
        node.quantumBit0 = quantumBit0
        node.quantumBit1 = quantumBit1
        NodeBuilder.log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newQuantumMeasure(
        cpgNode: Node? = null,
        quantumCircuit: QuantumCircuit? = null
    ): QuantumMeasure {
        val node = QuantumMeasure(cpgNode)
        node.quantumCircuit = quantumCircuit
        node.measurements = ArrayList()
        NodeBuilder.log(node)
        return node
    }
}
