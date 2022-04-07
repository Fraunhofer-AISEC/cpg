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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.function.Supplier
import org.eclipse.cdt.core.dom.ast.IASTInitializer
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTConstructorInitializer
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTEqualsInitializer
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTInitializerList

class InitializerHandler(lang: CXXLanguageFrontend) :
    Handler<Expression?, IASTInitializer, CXXLanguageFrontend?>(Supplier { Expression() }, lang) {

    init {
        map[CPPASTConstructorInitializer::class.java] = HandlerInterface { ctx: IASTInitializer ->
            handleConstructorInitializer(ctx as CPPASTConstructorInitializer)
        }
        map[CPPASTEqualsInitializer::class.java] = HandlerInterface { ctx: IASTInitializer ->
            handleEqualsInitializer(ctx as CPPASTEqualsInitializer)
        }

        /* Todo Initializer List is handled in ExpressionsHandler that actually handles InitializerClauses often used where
            one expects an expression.
        */ map[CPPASTInitializerList::class.java] =
            HandlerInterface { ctx: IASTInitializer ->
                lang.expressionHandler.handle(ctx as CPPASTInitializerList)
            }
    }

    private fun handleConstructorInitializer(ctx: CPPASTConstructorInitializer): Expression {
        val constructExpression = NodeBuilder.newConstructExpression(ctx.rawSignature)
        for ((i, argument) in ctx.arguments.withIndex()) {
            val arg = lang.expressionHandler.handle(argument)
            arg!!.argumentIndex = i
            constructExpression.addArgument(arg)
        }
        return constructExpression
    }

    private fun handleEqualsInitializer(ctx: CPPASTEqualsInitializer): Expression? {
        return lang.expressionHandler.handle(ctx.initializerClause)
    }
}
