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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.frontends.Handler
import java.util.function.Supplier
import org.eclipse.cdt.internal.core.dom.parser.ASTNode

abstract class CXXHandler<S, T>(configConstructor: Supplier<S>, lang: CXXLanguageFrontend) :
    Handler<S, T, CXXLanguageFrontend>(configConstructor, lang) {
    /**
     * We intentionally override the logic of [Handler.handle] because we do not want the map-based
     * logic, but rather want to make use of the Kotlin-when syntax.
     */
    override fun handle(ctx: T): S? {
        // If we do not want to load includes into the CPG and the current fileLocation was included

        // If we do not want to load includes into the CPG and the current fileLocation was included
        if (!lang.config.loadIncludes && ctx is ASTNode) {
            val astNode = ctx as ASTNode
            if (astNode.fileLocation != null &&
                    astNode.fileLocation.contextInclusionStatement != null
            ) {
                log.debug("Skip parsing include file" + astNode.containingFilename)
                return null
            }
        }

        return handleNode(ctx)
    }

    abstract fun handleNode(node: T): S
}
