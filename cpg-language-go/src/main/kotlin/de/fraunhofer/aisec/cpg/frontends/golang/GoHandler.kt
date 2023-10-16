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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ProblemNode
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.helpers.Util
import java.util.function.Supplier

abstract class GoHandler<ResultNode : Node?, HandlerNode : GoStandardLibrary.Ast.Node>(
    configConstructor: Supplier<ResultNode?>,
    lang: GoLanguageFrontend
) : Handler<ResultNode?, HandlerNode, GoLanguageFrontend>(configConstructor, lang) {
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

        // The language frontend might set a location, which we should respect. Otherwise, we will
        // set the location here.
        if (node.location == null) {
            frontend.setCodeAndLocation(node, ctx)
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
            "Parsing of type $name is not supported (yet)"
        )

        val cpgNode = this.configConstructor.get()
        if (cpgNode is ProblemNode) {
            cpgNode.problem = "Parsing of type $name is not supported (yet)"
        }

        return cpgNode!!
    }

    /**
     * This virtual property returns the name of the import package when imported without any
     * aliases.
     */
    val GoStandardLibrary.Ast.ImportSpec.importName: String
        get() {
            val path = frontend.expressionHandler.handle(this.path) as? Literal<*>
            val paths = (path?.value as? String)?.split("/") ?: listOf()

            // Return the last name in the path as the import name. However, if the last name is a
            // module version (e.g., v5), then we need to return the second-to-last
            var last = paths.lastOrNull()
            last =
                if (last?.startsWith("v") == true) {
                    paths.getOrNull(paths.size - 2)
                } else {
                    last
                }

            return last ?: ""
        }
}
