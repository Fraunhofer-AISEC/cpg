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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.NamespaceScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.helpers.Util

abstract class GoHandler<ResultNode : AstNode?, HandlerNode : GoStandardLibrary.Ast.Node>(
    frontend: GoLanguageFrontend
) : Handler<ResultNode, HandlerNode, GoLanguageFrontend>(frontend) {
    /**
     * We intentionally override the logic of [Handler.handle] because we do not want the map-based
     * logic, but rather want to make use of the Kotlin-when syntax.
     *
     * We also want to support nullable results for some handlers, e.g., when ignoring private
     * declarations in dependencies.
     */
    override fun handle(ctx: HandlerNode): ResultNode {
        val node = handleNode(ctx)
        if (node == null) {
            return node
        }

        frontend.setComment(node, ctx)
        frontend.process(ctx, node)

        lastNode = node

        return node
    }

    abstract fun handleNode(node: HandlerNode): ResultNode

    /**
     * This function should be called by classes that derive from [GoHandler] to denote, that the
     * supplied node (type) is not supported.
     */
    protected fun handleNotSupported(node: HandlerNode, name: String): ResultNode {
        Util.errorWithFileLocation(
            frontend,
            node,
            log,
            "Parsing of type $name is not supported (yet)",
        )

        return this.problemConstructor("Parsing of type $name is not supported (yet)", node)
    }

    /**
     * This virtual property returns the name of the import package when imported without any
     * aliases.
     */
    val GoStandardLibrary.Ast.ImportSpec.importName: String
        get() {
            // We set the filename of the include declaration to the package path, i.e., its full
            // path including any module identifiers. This way we can match the include declaration
            // back to the namespace's path and name
            val filename = path.value.removeSurrounding("\"").removeSurrounding("`")

            // While it is convention that the respective Go package is named after the last path in
            // the import name, this is purely a convention. We therefore need to find out the real
            // package name. Currently, we do not support parallel parsing, so the order of imports
            // might be suitable to look up the specific package in the scope manager. In the
            // future, we might need a better solution.
            val namespace =
                this@GoHandler.frontend.scopeManager
                    .filterScopes {
                        it is NamespaceScope &&
                            (it.astNode as? NamespaceDeclaration)?.path == filename
                    }
                    .firstOrNull()
                    ?.astNode as? NamespaceDeclaration

            if (namespace != null) {
                // We can directly take the "real" namespace name then
                return namespace.name.localName
            } else {
                val path =
                    this@GoHandler.frontend.expressionHandler.handle(this.path) as? Literal<*>
                val paths = (path?.value as? String)?.split("/") ?: listOf()

                // Return the last name in the path as the import name. However, if the last name is
                // a module version (e.g., v5), then we need to return the second-to-last
                var last = paths.lastOrNull()
                last =
                    if (last != null && last.length >= 2 && last[0] == 'v' && last[1].isDigit()) {
                        paths.getOrNull(paths.size - 2)
                    } else {
                        last
                    }

                return last ?: ""
            }
        }
}
