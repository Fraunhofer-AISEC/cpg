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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumCircuit
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.quantumcpg.QuantumBitType
import de.fraunhofer.aisec.cpg.graph.variables
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.CallResolver
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import de.fraunhofer.aisec.cpg.passes.order.RequiredFrontend

@RequiredFrontend(OpenQasmLanguageFrontend::class)
@DependsOn(VariableUsageResolver::class)
@DependsOn(CallResolver::class)
class OpenQASMPass : Pass() {
    private val quantumCircuitsMap = HashMap<Node, QuantumCircuit>()

    override fun accept(p0: TranslationResult) {

        val flatAST = SubgraphWalker.flattenAST(p0)

        var quBits = p0.variables { it.type is QuantumBitType}
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
