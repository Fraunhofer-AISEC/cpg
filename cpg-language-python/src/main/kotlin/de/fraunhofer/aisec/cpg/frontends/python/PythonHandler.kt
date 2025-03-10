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
import de.fraunhofer.aisec.cpg.graph.AstNode

abstract class PythonHandler<ResultNode : AstNode, HandlerNode : Python.AST.AST>(
    frontend: PythonLanguageFrontend
) : Handler<ResultNode, HandlerNode, PythonLanguageFrontend>(frontend) {
    /**
     * We intentionally override the logic of [Handler.handle] because we do not want the map-based
     * logic, but rather want to make use of the Kotlin-when syntax.
     *
     * We also want non-nullable result handlers
     */
    override fun handle(ctx: HandlerNode): ResultNode {
        val node = handleNode(ctx)

        frontend.setComment(node, ctx)
        frontend.process(ctx, node)

        return node
    }

    abstract fun handleNode(node: HandlerNode): ResultNode

    companion object {
        /**
         * A prefix to add to random names when having to add implicit assignments. Used when
         * handling loops with multiple variables.
         */
        const val LOOP_VAR_PREFIX = "loopMultiVarHelperVar"
        /**
         * A prefix to add to random names representing implicit context managers in `with`
         * statements.
         */
        const val CONTEXT_MANAGER = "contextManager"
        /**
         * A prefix to add to random names representing implicit `tmpVal` nodes in `with`
         * statements.
         */
        const val WITH_TMP_VAL = "withTmpVal"
    }
}
