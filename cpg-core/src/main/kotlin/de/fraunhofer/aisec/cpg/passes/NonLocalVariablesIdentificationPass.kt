/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.EOGStarterHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionTemplate
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.declarations.Tuple
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import java.util.IdentityHashMap

val isNonLocalVariablesMap = IdentityHashMap<ValueDeclaration, Boolean>()

val Variable.isNonLocal: Boolean
    get() = isNonLocalVariablesMap[this] ?: false

@DependsOn(SymbolResolver::class)
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(BasicBlockCollectorPass::class, softDependency = true)
@Description(
    "Pass that identifies which variables are global but require special handling by the DFG passes."
)
class NonLocalVariablesIdentificationPass(ctx: TranslationContext) : EOGStarterPass(ctx) {
    override fun cleanup() {
        // Nothing to do here
    }

    override fun accept(t: Node) {
        val node = t as? AstNode ?: return

        val allChildrenOfFunction =
            node.allChildren<Node>(
                stopAtNode = {
                    it is FunctionTemplate ||
                        (it is Variable && it.prevEOG.isEmpty() && !it.isImplicit) ||
                        (it is EOGStarterHolder && it.prevEOG.isEmpty() && it != node)
                }
            )
        for (varDecl in
            allChildrenOfFunction.filter {
                (it is Variable && !it.isGlobal && it !is Field && it !is Tuple) || it is Parameter
            }) {
            (varDecl as? ValueDeclaration)?.let {
                isNonLocalVariablesMap[it] = it.usages.any { it !in allChildrenOfFunction }
            }
        }
    }
}
