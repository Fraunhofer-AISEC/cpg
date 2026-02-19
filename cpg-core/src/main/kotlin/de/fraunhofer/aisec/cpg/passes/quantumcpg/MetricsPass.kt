/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumCircuit
import de.fraunhofer.aisec.cpg.passes.ComponentPass

class Metrics {
    val width: Int? = null
    val depth: Int? = null
    val maxDens: Float? = null
    val avgDens: Float? = null
    val noPX: Int? = null
    val noPY: Int? = null
    val noPZ: Int? = null
    val tnoP: Int? = null
    val noH: Int? = null
    val spposQ: Float? = null
    val noOtherSG: Int? = null
    val numGates: Int? = null
}

class MetricsPass(ctx: TranslationContext) : ComponentPass(ctx) {
    override fun accept(t: Component) {
        val circuits =
            t.translationUnits
                .first()
                .additionalNodes
                .filterIsInstance<QuantumCircuit>() // TODO first
    }

    override fun cleanup() {
        // Nothing to do here
    }
}
