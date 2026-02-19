/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.Node

/** A gate representing a rotation around the axis [axis] with the angle [theta]. */
abstract class QuantumRotationGate(
    cpgNode: Node?,
    quantumCircuit: QuantumCircuit,
    val axis: Axis,
    val theta: Node,
    val quantumBit: QuantumBitReference,
) : QuantumGate(cpgNode, quantumCircuit)

class QuantumRotationXGate(
    cpgNode: Node?,
    quantumCircuit: QuantumCircuit,
    theta: Node,
    quantumBit: QuantumBitReference,
) : QuantumRotationGate(cpgNode, quantumCircuit, Axis.X, theta, quantumBit) {
    override val fidelity: Float
        get() = TODO("Not yet implemented")
}

class QuantumRotationYGate(
    cpgNode: Node?,
    quantumCircuit: QuantumCircuit,
    theta: Node,
    quantumBit: QuantumBitReference,
) : QuantumRotationGate(cpgNode, quantumCircuit, Axis.Y, theta, quantumBit) {
    override val fidelity: Float
        get() = TODO("Not yet implemented")
}

class QuantumRotationZGate(
    cpgNode: Node?,
    quantumCircuit: QuantumCircuit,
    theta: Node,
    quantumBit: QuantumBitReference,
) : QuantumRotationGate(cpgNode, quantumCircuit, Axis.Z, theta, quantumBit) {
    override val fidelity: Float
        get() = TODO("Not yet implemented")
}

enum class Axis {
    /** The X-axis */
    X,
    /** The Y-axis */
    Y,
    /** The Z-axis */
    Z,
}
