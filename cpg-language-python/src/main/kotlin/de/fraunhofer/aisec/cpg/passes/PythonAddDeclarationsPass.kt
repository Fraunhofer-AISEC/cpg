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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.order.ExecuteFirst
import de.fraunhofer.aisec.cpg.passes.order.RequiredFrontend

@ExecuteFirst
@RequiredFrontend(PythonLanguageFrontend::class)
class PythonAddDeclarationsPass(ctx: TranslationContext) : ComponentPass(ctx), NamespaceProvider {
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
     * - [VariableDeclaration] otherwise
     *
     * TODO: loops
     */
    private fun handle(node: Node?) {
        when (node) {
            is AssignExpression -> handleAssignExpression(node)
            is Reference -> handleReference(node)
            else -> {}
        }
    }

    /*
     * Return null when not creating a new decl
     */
    private fun handleReference(node: Reference): VariableDeclaration? {
        if (node.resolutionHelper is CallExpression) {
            return null
        }
        val resolved = scopeManager.resolveReference(node)
        if (resolved == null) {
            val decl =
                if (scopeManager.isInRecord) {
                    if (scopeManager.isInFunction) {
                        if (
                            node is MemberExpression &&
                                node.base.name ==
                                    (scopeManager.currentFunction as? MethodDeclaration)
                                        ?.receiver
                                        ?.name
                        ) {
                            val field =
                                newFieldDeclaration(
                                    node.name,
                                    code = node.code,
                                    rawNode = node,
                                    location = node.location
                                )
                            scopeManager.currentRecord?.addField(field) // TODO why do we need this?
                            field
                        } else {
                            val v = newVariableDeclaration(node.name, code = node.code)
                            v.location = node.location
                            scopeManager.currentFunction?.addDeclaration(v)
                            v
                        }
                    } else {
                        val field = newFieldDeclaration(node.name, code = node.code)
                        field.location = node.location
                        scopeManager.currentRecord?.addField(field) // TODO why do we need this?
                        field
                    }
                } else {
                    if (scopeManager.isInFunction) {
                        val v = newVariableDeclaration(node.name, code = node.code)
                        v.location = node.location
                        scopeManager.currentFunction
                            ?.body
                            ?.addDeclaration(v) // TODO why do we need this?
                        v
                    } else {
                        val v = newVariableDeclaration(node.name, code = node.code)
                        v.location = node.location
                        v
                    }
                }

            decl.isImplicit = true

            if (decl is FieldDeclaration) {
                decl.scope = scopeManager.currentRecord?.scope
                scopeManager.currentRecord?.addField(decl)
                scopeManager.withScope(scopeManager.currentRecord?.scope) {
                    scopeManager.addDeclaration(decl)
                }
            } else {
                decl.scope = scopeManager.currentScope // TODO why do we need this?
                scopeManager.addDeclaration(decl)
            }
            return decl
        } else {
            return null
        }
    }

    private fun handleAssignExpression(assignExpression: AssignExpression) {
        for (target in assignExpression.lhs) {
            (target as? Reference)?.let {
                val resolved = scopeManager.resolveReference(it)
                if (resolved == null) {
                    val decl = handleReference(it)
                    assignExpression.findValue(it)?.let { value ->
                        decl?.type = value.type
                    } // TODO why do we need this (testCtor test case for example)?

                    decl?.let { d -> assignExpression.declarations += d }
                }
            }
        }
    }

    override val namespace: Name?
        get() = scopeManager.currentNamespace
}
