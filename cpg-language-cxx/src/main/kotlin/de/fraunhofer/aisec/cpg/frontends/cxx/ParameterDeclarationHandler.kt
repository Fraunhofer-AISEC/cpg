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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.declarations.ProblemDeclaration
import de.fraunhofer.aisec.cpg.graph.newParameter
import java.util.function.Supplier
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration
import org.eclipse.cdt.internal.core.dom.parser.c.CASTParameterDeclaration
import org.eclipse.cdt.internal.core.dom.parser.c.CASTSimpleDeclSpecifier
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTParameterDeclaration

class ParameterDeclarationHandler(lang: CXXLanguageFrontend) :
    CXXHandler<Declaration, IASTParameterDeclaration>(Supplier(::ProblemDeclaration), lang) {

    override fun handleNode(node: IASTParameterDeclaration): Declaration {
        return when (node) {
            is CPPASTParameterDeclaration -> handleParameter(node)
            is CASTParameterDeclaration -> handleParameter(node)
            else -> {
                handleNotSupported(node, node.javaClass.name)
            }
        }
    }

    private fun handleParameter(ctx: IASTParameterDeclaration): Parameter {
        var name = ctx.declarator.name.toString()
        val specifier = ctx.declSpecifier

        // Parse the type. If we are running into the situation where the declSpecifier is
        // "unspecified" and the name is not, then this is an unnamed parameter of an unknown type
        // and CDT is not able to handle this correctly
        val type =
            if (
                specifier is CASTSimpleDeclSpecifier &&
                    specifier.type == IASTDeclSpecifier.sc_unspecified
            ) {
                name = ""
                frontend.typeOf(ctx.declarator.name)
            } else {
                frontend.typeOf(ctx.declarator, ctx.declSpecifier)
            }

        val paramVariableDeclaration = newParameter(name, type, false, rawNode = ctx)

        // We cannot really model "const" as part of the type, but we can model it as part of the
        // parameter, so we can use it later
        if (ctx.declSpecifier.isConst) {
            paramVariableDeclaration.modifiers += CONST
        }

        // Add default values
        if (ctx.declarator.initializer != null) {
            paramVariableDeclaration.default =
                frontend.initializerHandler.handle(ctx.declarator.initializer)
        }

        // Add default values
        if (ctx.declarator.initializer != null) {
            paramVariableDeclaration.default =
                frontend.initializerHandler.handle(ctx.declarator.initializer)
        }

        return paramVariableDeclaration
    }
}
