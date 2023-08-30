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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression

class ExpressionHandler(frontend: PythonLanguageFrontend) :
    PythonHandler<Expression, PythonAST.expr>(::ProblemExpression, frontend) {
    override fun handleNode(node: PythonAST.expr): Expression {
        return when (node) {
            is PythonAST.Name -> handleName(node)
            is PythonAST.Call -> handleCall(node)
            is PythonAST.Constant -> handleConstant(node)
            else -> TODO()
        }
    }

    private fun handleConstant(node: PythonAST.Constant): Expression {
        // TODO check and add missing types
        val tpe =
            when (node.value) {
                is String -> primitiveType("str")
                is Boolean -> primitiveType("bool")
                is Int -> primitiveType("int")
                is Float -> primitiveType("float")
                null -> objectType("None") // TODO
                else -> {
                    unknownType()
                }
            }
        return newLiteral(node.value, type = tpe, rawNode = node)
    }

    /**
     * Handles an `ast.Call` Python node. This can be one of
     * - [MemberCallExpression]
     * - [ConstructExpression]
     * - [CastExpression]
     * - [CallExpression]
     *
     * TODO: cast, memberexpression, magic
     */
    private fun handleCall(node: PythonAST.Call): Expression {
        val func = handle(node.func)
        if (func is MemberExpression) TODO("OLD PYTHON CODE")

        // try to resolve -> [ConstructExpression]
        val currentScope = frontend.scopeManager.currentScope
        val record = currentScope?.let { frontend.scopeManager.getRecordForName(it, func.name) }
        val ret =
            if (record != null) {
                // construct expression
                val constructExpr =
                    newConstructExpression((node.func as? PythonAST.Name)?.id, rawNode = node)
                constructExpr.type = record.toType()
                constructExpr
            } else {
                newCallExpression(func, rawNode = node)
            }

        for (arg in node.args) {
            ret.addArgument(handle(arg))
        }

        for (keyword in node.keywords) {
            ret.addArgument(handle(keyword.value), keyword.arg)
        }

        return ret
    }

    private fun handleName(node: PythonAST.Name): Expression {
        return newDeclaredReferenceExpression(name = node.id, rawNode = node)
    }
}
