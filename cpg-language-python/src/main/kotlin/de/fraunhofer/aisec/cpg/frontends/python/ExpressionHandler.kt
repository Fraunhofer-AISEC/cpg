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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression

class ExpressionHandler(frontend: PythonLanguageFrontend) :
    PythonHandler<Expression, PythonAST.expr>(::ProblemExpression, frontend) {
    override fun handleNode(node: PythonAST.expr): Expression {
        return when (node) {
            is PythonAST.Name -> handleName(node)
            is PythonAST.Call -> handleCall(node)
            else -> TODO()
        }
    }

    /**
     * Handles an `ast.Call` Python node. This can be one of
     * - [MemberCallExpression]
     * - [ConstructExpression]
     * - [CastExpression]
     * - [CallExpression]
     *
     * ast.Call = class Call(expr) | Call(expr func, expr* args, keyword* keywords)
     */
    private fun handleCall(node: PythonAST.Call): Expression {
        val func = handle(node.func)

        TODO()
    }

    private fun handleName(node: PythonAST.Name): Expression {
        return newDeclaredReferenceExpression(
            name = node.id,
            code = frontend.codeOf(node),
            rawNode = node
        )
    }
}
