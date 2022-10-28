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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParamVariableDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ProblemDeclaration
import java.util.function.Supplier
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration
import org.eclipse.cdt.internal.core.dom.parser.c.CASTParameterDeclaration
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTParameterDeclaration

class ParameterDeclarationHandler(lang: CXXLanguageFrontend) :
    CXXHandler<Declaration?, IASTParameterDeclaration>(Supplier(::ProblemDeclaration), lang) {

    override fun handleNode(node: IASTParameterDeclaration): Declaration? {
        return when (node) {
            is CPPASTParameterDeclaration -> handleParameterDeclaration(node)
            is CASTParameterDeclaration -> handleParameterDeclaration(node)
            else -> {
                return handleNotSupported(node, node.javaClass.name)
            }
        }
    }

    private fun handleParameterDeclaration(
        ctx: IASTParameterDeclaration
    ): ParamVariableDeclaration {
        // Parse the type
        val type = lang.typeOf(ctx.declarator, ctx.declSpecifier)

        val paramVariableDeclaration =
            NodeBuilder.newMethodParameterIn(
                ctx.declarator.name.toString(),
                type,
                false,
                lang.language,
                ctx.rawSignature
            )

        // Add default values
        if (ctx.declarator.initializer != null) {
            paramVariableDeclaration.default =
                lang.initializerHandler.handle(ctx.declarator.initializer)
        }

        // Add default values
        if (ctx.declarator.initializer != null) {
            paramVariableDeclaration.default =
                lang.initializerHandler.handle(ctx.declarator.initializer)
        }

        return paramVariableDeclaration
    }
}
