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

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HandlerInterface
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParamVariableDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ProblemDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import java.util.function.Supplier
import java.util.stream.Collectors
import org.eclipse.cdt.core.dom.ast.IASTArrayModifier
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTArrayDeclarator
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTInitializerList
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTParameterDeclaration

class ParameterDeclarationHandler(lang: CXXLanguageFrontend) :
    Handler<Declaration?, IASTParameterDeclaration, CXXLanguageFrontend?>(
        Supplier { ProblemDeclaration() },
        lang
    ) {

    init {
        map[CPPASTParameterDeclaration::class.java] =
            HandlerInterface { ctx: IASTParameterDeclaration ->
                handleParameterDeclaration(ctx as CPPASTParameterDeclaration)
            }
    }

    private fun handleParameterDeclaration(
        ctx: CPPASTParameterDeclaration
    ): ParamVariableDeclaration {
        // The logic of type adjustment computation was copied over from handleDeclarator, it is not
        // clear if it will be necessary but the usage of handleDeclarator had to be avoided because
        // of
        // side effects
        var typeAdjustment =
            mutableListOf(*ctx.declarator.pointerOperators)
                .stream()
                .map { obj: IASTPointerOperator -> obj.rawSignature }
                .collect(Collectors.joining())
        if (ctx.declarator is CPPASTArrayDeclarator &&
                ctx.declarator.initializer is CPPASTInitializerList
        ) {
            // narrow down array type to size of initializer list expression
            typeAdjustment +=
                ("[" +
                    (ctx.declarator.initializer as CPPASTInitializerList).initializers.size +
                    "]")
        } else if (ctx.declarator is CPPASTArrayDeclarator &&
                ctx.declarator.initializer is Literal<*> &&
                (ctx.declarator.initializer as Literal<*>).value is String
        ) {
            // narrow down array type to length of string literal
            typeAdjustment +=
                ("[" +
                    (((ctx.declarator.initializer as Literal<*>).value as String).length + 1) +
                    "]")
        } else if (ctx.declarator is CPPASTArrayDeclarator) {
            typeAdjustment +=
                mutableListOf(*(ctx.declarator as CPPASTArrayDeclarator).arrayModifiers)
                    .stream()
                    .map { obj: IASTArrayModifier -> obj.rawSignature }
                    .collect(Collectors.joining())
        }
        val paramVariableDeclaration =
            NodeBuilder.newMethodParameterIn(
                ctx.declarator.name.toString(),
                UnknownType.getUnknownType(),
                false,
                ctx.rawSignature
            )
        if (typeAdjustment.isNotEmpty()) {
            paramVariableDeclaration.type =
                TypeParser.createFrom(ctx.declSpecifier.toString() + typeAdjustment, true, lang)
        } else {
            paramVariableDeclaration.type =
                TypeParser.createFrom(ctx.declSpecifier.toString(), true, lang)
        }

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
