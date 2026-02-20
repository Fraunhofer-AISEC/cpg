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

import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.newConstruct
import de.fraunhofer.aisec.cpg.graph.newInitializerList
import de.fraunhofer.aisec.cpg.graph.newProblem
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerList
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Problem
import de.fraunhofer.aisec.cpg.graph.unknownType
import java.util.function.Supplier
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer
import org.eclipse.cdt.core.dom.ast.IASTInitializer
import org.eclipse.cdt.core.dom.ast.IASTInitializerList
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTConstructorInitializer

class InitializerHandler(lang: CXXLanguageFrontend) :
    CXXHandler<Expression, IASTInitializer>(Supplier(::Problem), lang) {

    override fun handleNode(node: IASTInitializer): Expression {
        return when (node) {
            is IASTEqualsInitializer -> handleEqualsInitializer(node)
            is IASTInitializerList -> handleInitializerList(node)
            is CPPASTConstructorInitializer -> handleConstructorInitializer(node)
            else -> {
                handleNotSupported(node, node.javaClass.name)
            }
        }
    }

    private fun handleConstructorInitializer(ctx: CPPASTConstructorInitializer): Expression {
        val constructExpression = newConstruct(rawNode = ctx)
        constructExpression.type =
            (frontend.declaratorHandler.lastNode as? Variable)?.type ?: unknownType()

        for ((i, argument) in ctx.arguments.withIndex()) {
            val arg = frontend.expressionHandler.handle(argument)
            arg?.let {
                it.argumentIndex = i
                constructExpression.addArgument(it)
            }
        }

        return constructExpression
    }

    private fun handleInitializerList(ctx: IASTInitializerList): InitializerList {
        // Because an initializer list expression is used for many different things, it is important
        // for us to know which kind of variable (or rather of which kind), we are initializing.
        // This information can be found in the lastNode property of our declarator handler.
        val targetType =
            (frontend.declaratorHandler.lastNode as? ValueDeclaration)?.type ?: unknownType()

        val expression = newInitializerList(targetType, rawNode = ctx)

        for (clause in ctx.clauses) {
            frontend.expressionHandler.handle(clause)?.let { expression.initializerEdges.add(it) }
        }

        return expression
    }

    private fun handleEqualsInitializer(ctx: IASTEqualsInitializer): Expression {
        return frontend.expressionHandler.handle(ctx.initializerClause)
            ?: return newProblem("could not parse initializer clause")
    }
}
