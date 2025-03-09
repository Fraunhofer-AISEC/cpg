/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.helpers.Util
import org.eclipse.cdt.core.dom.ast.IASTNode
import org.eclipse.cdt.internal.core.dom.parser.ASTNode

abstract class CXXHandler<S : AstNode, T : IASTNode>(frontend: CXXLanguageFrontend) :
    Handler<S, T, CXXLanguageFrontend>(frontend) {

    /**
     * We intentionally override the logic of [Handler.handle] because we do not want the map-based
     * logic, but rather want to make use of the Kotlin-when syntax.
     */
    override fun handle(ctx: T): S? {
        // If we do not want to load includes into the CPG and the current fileLocation was included
        if (!this@CXXHandler.frontend.config.loadIncludes && ctx is ASTNode) {
            val astNode = ctx as ASTNode
            if (
                astNode.fileLocation != null &&
                    astNode.fileLocation.contextInclusionStatement != null
            ) {
                log.debug("Skip parsing include file" + astNode.containingFilename)
                return null
            }
        }

        val node = handleNode(ctx)

        this@CXXHandler.frontend.process(ctx, node)

        this.lastNode = node

        return node
    }

    abstract fun handleNode(node: T): S

    /**
     * This function should be called by classes that derive from [CXXHandler] to denote, that the
     * supplied node (type) is not supported.
     */
    protected fun handleNotSupported(node: T, name: String): S {
        Util.errorWithFileLocation(
            this@CXXHandler.frontend,
            node,
            log,
            "Parsing of type $name is not supported (yet)",
        )

        return this.problemConstructor("Parsing of type $name is not supported (yet)", node)
    }
}
