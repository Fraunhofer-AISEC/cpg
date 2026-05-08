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
package de.fraunhofer.aisec.cpg.frontends.php

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ProblemNode
import de.fraunhofer.aisec.cpg.helpers.Util
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Base handler for all PHP ANTLR4 context nodes. Subclasses override [handleNode] to implement
 * dispatch over concrete grammar rule contexts via Kotlin `when`.
 */
abstract class PHPHandler<ResultNode : Node, HandlerNode : ParserRuleContext>(
    configConstructor: () -> ResultNode,
    frontend: PHPLanguageFrontend,
) : Handler<ResultNode, HandlerNode, PHPLanguageFrontend>(configConstructor, frontend) {

    /**
     * We bypass the map-based dispatch from [Handler.handle] and use the Kotlin-`when` dispatch in
     * each subclass instead.
     */
    override fun handle(ctx: HandlerNode): ResultNode {
        val node = handleNode(ctx)
        frontend.setComment(node, ctx)
        frontend.process(ctx, node)
        lastNode = node
        return node
    }

    abstract fun handleNode(node: HandlerNode): ResultNode

    protected fun handleNotSupported(node: HandlerNode, name: String): ResultNode {
        Util.errorWithFileLocation(
            frontend,
            node,
            log,
            "Parsing of PHP node type $name is not yet supported",
        )
        val cpgNode = this.configConstructor.get()
        if (cpgNode is ProblemNode) {
            cpgNode.problem = "Parsing of PHP node type $name is not yet supported"
        }
        return cpgNode
    }
}
