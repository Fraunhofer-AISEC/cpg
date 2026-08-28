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
package de.fraunhofer.aisec.cpg.frontends.csharp

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.Node
import java.util.function.Supplier

/** Base class for all C# handlers. */
abstract class CSharpHandler<ResultNode : Node, HandlerNode : Csharp.AST.Node>(
    configConstructor: Supplier<ResultNode>,
    frontend: CSharpLanguageFrontend,
) :
    Handler<ResultNode, HandlerNode, CSharpLanguageFrontend>(
        configConstructor = configConstructor,
        frontend = frontend,
    ) {

    override fun handle(ctx: HandlerNode): ResultNode {
        val node = handleNode(ctx)

        frontend.setComment(node, ctx)
        frontend.process(ctx, node)

        lastNode = node
        return node
    }

    abstract fun handleNode(node: HandlerNode): ResultNode
}
