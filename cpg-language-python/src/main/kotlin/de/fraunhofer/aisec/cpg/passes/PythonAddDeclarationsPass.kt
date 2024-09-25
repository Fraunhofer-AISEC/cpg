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
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.InitializerTypePropagation
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.RequiredFrontend

@DependsOn(TypeResolver::class)
@ExecuteBefore(SymbolResolver::class)
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
     * - [VariableDeclaration] otherwise
     *
     * TODO: loops
     */
    private fun handle(node: Node?) {
        when (node) {
            is AssignExpression -> handleAssignExpression(node)
            is ForEachStatement -> handleForEach(node)
            else -> {
                // Nothing to do for all other types of nodes
            }
        }
    }

    /*
     * Return null when not creating a new decl
     */
    private fun handleReference(node: Reference): VariableDeclaration? {
        if (node.resolutionHelper is CallExpression) {
            return null
        }

        // TODO(oxisto): Actually the logic here is far more complex in reality, taking into account
        //  local and global variables, but for now we just resolve it using the scope manager
        val resolved = scopeManager.resolveReference(node)

        // Nothing to create
        if (resolved != null) return null

        val decl =
            if (scopeManager.isInRecord) {
                if (scopeManager.isInFunction) {
                    if (
                        node is MemberExpression &&
                            node.base.name ==
                                (scopeManager.currentFunction as? MethodDeclaration)?.receiver?.name
                    ) {
                        // We need to temporarily jump into the scope of the current record to
                        // add the field
                        val field =
                            scopeManager.withScope(scopeManager.currentRecord?.scope) {
                                newFieldDeclaration(node.name)
                            }
                        field
                    } else {
                        val v = newVariableDeclaration(node.name)
                        v
                    }
                } else {
                    val field =
                        scopeManager.withScope(scopeManager.currentRecord?.scope) {
                            newFieldDeclaration(node.name)
                        }
                    field
                }
            } else {
                newVariableDeclaration(node.name)
            }

        decl.code = node.code
        decl.location = node.location
        decl.isImplicit = true

        if (decl is FieldDeclaration) {
            scopeManager.currentRecord?.addField(decl)
            scopeManager.withScope(scopeManager.currentRecord?.scope) {
                scopeManager.addDeclaration(decl)
            }
        } else {
            scopeManager.addDeclaration(decl)
        }
        return decl
    }

    private fun handleAssignExpression(assignExpression: AssignExpression) {
        for (target in assignExpression.lhs) {
            (target as? Reference)?.let {
                val handled = handleReference(target)
                if (handled is Declaration) {
                    // We cannot assign an initializer here because this will lead to duplicate
                    // DFG edges, but we need to propagate the type information from our value
                    // to the declaration. We therefore add the declaration to the observers of
                    // the value's type, so that it gets informed and can change its own type
                    // accordingly.
                    assignExpression
                        .findValue(target)
                        ?.registerTypeObserver(InitializerTypePropagation(handled))

                    // Add it to our assign expression, so that we can find it in the AST
                    assignExpression.declarations += handled
                }
            }
        }
    }

    // TODO document why this is necessary and implement for other possible places
    private fun handleForEach(node: ForEachStatement) {
        when (val forVar = node.variable) {
            is Reference -> {
                val handled = handleReference(forVar)
                (handled as? Declaration)?.let { forVar.addDeclaration(it) }
            }
        }
    }
}
