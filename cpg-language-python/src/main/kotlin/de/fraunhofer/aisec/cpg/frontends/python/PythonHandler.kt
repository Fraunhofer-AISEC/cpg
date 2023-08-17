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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ProblemNode
import de.fraunhofer.aisec.cpg.helpers.Util
import java.util.function.Supplier

abstract class PythonHandler<ResultNode : Node, HandlerNode : PythonAST.Node>(
    configConstructor: Supplier<ResultNode>,
    lang: PythonLanguageFrontend
) : Handler<ResultNode, HandlerNode, PythonLanguageFrontend>(configConstructor, lang) {
    /**
     * We intentionally override the logic of [Handler.handle] because we do not want the map-based
     * logic, but rather want to make use of the Kotlin-when syntax.
     *
     * We also want non-nullable result handlers
     */
    override fun handle(ctx: HandlerNode): ResultNode {
        val node = handleNode(ctx)

        // The language frontend might set a location, which we should respect. Otherwise, we will
        // set the location here.
        if (node.location == null) {
            frontend.setCodeAndLocation(node, ctx)
        }

        frontend.setComment(node, ctx)
        frontend.process(ctx, node)

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

        return cpgNode
    }
}
