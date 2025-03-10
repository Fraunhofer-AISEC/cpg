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
package de.fraunhofer.aisec.cpg.frontends.ruby

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.helpers.Util

abstract class RubyHandler<ResultNode : AstNode, HandlerNode : org.jruby.ast.Node>(
    frontend: RubyLanguageFrontend
) : Handler<ResultNode, HandlerNode, RubyLanguageFrontend>(frontend) {
    /**
     * We intentionally override the logic of [Handler.handle] because we do not want the map-based
     * logic, but rather want to make use of the Kotlin-when syntax.
     */
    override fun handle(ctx: HandlerNode): ResultNode {
        val node = handleNode(ctx)

        frontend.setComment(node, ctx)
        frontend.process(ctx, node)

        lastNode = node

        return node
    }

    abstract fun handleNode(node: HandlerNode): ResultNode

    /**
     * This function should be called by classes that derive from [RubyHandler] to denote, that the
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
}
