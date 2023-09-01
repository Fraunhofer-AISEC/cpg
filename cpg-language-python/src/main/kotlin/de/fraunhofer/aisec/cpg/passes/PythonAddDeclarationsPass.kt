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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.newFieldDeclaration
import de.fraunhofer.aisec.cpg.graph.newVariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.order.ExecuteFirst
import de.fraunhofer.aisec.cpg.passes.order.RequiredFrontend

@ExecuteFirst
@RequiredFrontend(PythonLanguageFrontend::class)
class PythonAddDeclarationsPass(ctx: TranslationContext) : ComponentPass(ctx) {
    override fun cleanup() {
        // nothing to do
    }

    override fun accept(p0: Component) {
        val walker = SubgraphWalker.ScopedWalker(ctx.scopeManager)
        walker.registerHandler { _, _, currNode -> handle(currNode) }

        for (tu in p0.translationUnits) {
            walker.iterate(tu)
        }
    }

    /**
     * This function checks for each [AssignExpression] whether there is already a matching variable
     * or not. New variables can be one of:
     * - [FieldDeclaration] if we are currently in a record
     * - [VariableDeclaratrion] otherwise
     *
     * TODO: loops
     */
    private fun handle(assignExpression: Node?) {
        if (assignExpression !is AssignExpression) {
            return
        }

        for (target in assignExpression.lhs) {
            (target as? DeclaredReferenceExpression)?.let {
                val resolved = scopeManager.resolveReference(it)

                if (resolved == null) {
                    val decl =
                        if (scopeManager.isInRecord && !scopeManager.isInFunction) {
                            val field = newFieldDeclaration(it.name, code = it.code)
                            field.location = it.location
                            scopeManager.currentRecord?.addField(field) // TODO why do we need this?
                            field
                        } else {
                            val v = newVariableDeclaration(it.name, code = it.code)
                            v.location = it.location
                            v
                        }
                    if (scopeManager.isInFunction) {
                        scopeManager.currentFunction?.addDeclaration(
                            decl
                        ) // TODO why do we need this?
                    }
                    decl.isImplicit = true
                    assignExpression.findValue(it)?.let { value ->
                        decl.type = value.type
                    } // TODO why do we need this (testCtor test case for example)?
                    assignExpression.declarations += decl
                    decl.scope = scopeManager.currentScope // TODO why do we need this?
                    scopeManager.addDeclaration(decl)
                }
            }
        }
    }
}
