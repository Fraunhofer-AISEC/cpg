/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Function

/**
 * This functional interface specifies how a handler class should translate raw AST nodes into CPG
 * nodes.
 *
 * @param S the type of the translated CPG node
 * @param T the type of the raw AST node
 */
fun interface HandlerInterface<S, T> {
    /**
     * The returned CPG node type [S] should be as specific as possible to make it easier for the
     * caller. For example, if a handler parses functions and methods, it should at least return a
     * [Function] and not just a [Declaration].
     *
     * Furthermore, this function *can* return `null`, even though it should be avoided. A valid
     * use-case is that some nodes might be not relevant to be directly into CPG nodes at this
     * stage, for example comments.
     */
    fun handle(expr: T): S?
}
