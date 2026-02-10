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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.Node
import java.util.function.Supplier
import org.treesitter.TSNode

/**
 * An abstract base class for handlers in the Rust frontend. It provides a common [handle] method
 * that takes care of null-node checks, comment assignment, and metadata processing.
 */
abstract class RustHandler<ResultNode : Node, HandlerNode : TSNode>(
    configConstructor: Supplier<ResultNode>,
    lang: RustLanguageFrontend,
) : Handler<ResultNode, HandlerNode, RustLanguageFrontend>(configConstructor, lang) {
    /**
     * We intentionally override the logic of [Handler.handle] because we do not want the map-based
     * logic, but rather want to make use of the Kotlin-when syntax.
     */
    override fun handle(ctx: HandlerNode): ResultNode {
        if (ctx.isNull()) {
            val node = configConstructor.get()
            frontend.process(ctx, node)
            return node
        }
        val node = handleNode(ctx)

        frontend.setComment(node, ctx)
        frontend.process(ctx, node)

        return node
    }

    abstract fun handleNode(node: HandlerNode): ResultNode
}
