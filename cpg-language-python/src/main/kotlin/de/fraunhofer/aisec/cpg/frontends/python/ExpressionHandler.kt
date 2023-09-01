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
    PythonHandler<Expression, PythonAST.ExprBase>(::ProblemExpression, frontend) {
    override fun handleNode(node: PythonAST.ExprBase): Expression {
        return when (node) {
            is PythonAST.Name -> handleName(node)
            is PythonAST.Call -> handleCall(node)
            is PythonAST.Constant -> handleConstant(node)
            is PythonAST.Attribute -> handleAttribute(node)
            is PythonAST.BinOp -> handleBinOp(node)
            is PythonAST.Compare -> handleCompare(node)
            else -> TODO()
        }
    }

    private fun handleCompare(node: PythonAST.Compare): Expression {
        if (node.comparators.size != 1 || node.ops.size != 1) {
            return newProblemExpression("Multi compare is not (yet) supported.", rawNode = node)
        }
        val op =
            when (node.ops.first()) {
                is PythonAST.Eq -> "=="
                is PythonAST.NotEq -> "!="
                is PythonAST.Lt -> "<"
                is PythonAST.LtE -> "<="
                is PythonAST.Gt -> ">"
                is PythonAST.GtE -> ">="
                is PythonAST.Is -> "is"
                is PythonAST.IsNot -> "is not"
                is PythonAST.In -> "in"
                is PythonAST.NotIn -> "not in"
                else -> TODO()
            }
        val ret = newBinaryOperator(op, rawNode = node)
        ret.lhs = handle(node.left)
        ret.rhs = handle(node.comparators.first())
        return ret
    }

    private fun handleBinOp(node: PythonAST.BinOp): Expression {
        val op =
            when (node.op) {
                is PythonAST.Add -> "+"
                is PythonAST.Sub -> "-"
                is PythonAST.Mult -> "*"
                is PythonAST.MatMult -> "*"
                is PythonAST.Div -> "/"
                is PythonAST.Mod -> "%"
                is PythonAST.Pow -> "**"
                is PythonAST.LShift -> "<<"
                is PythonAST.RShift -> ">>"
                is PythonAST.BitOr -> "|"
                is PythonAST.BitXor -> "^"
                is PythonAST.BitAnd -> "&"
                is PythonAST.FloorDiv -> "//"
                else -> TODO()
            }
        val ret = newBinaryOperator(operatorCode = op, rawNode = node)
        ret.lhs = handle(node.left)
        ret.rhs = handle(node.right)
        return ret
    }

    private fun handleAttribute(node: PythonAST.Attribute): Expression {
        return newMemberExpression(name = node.attr, base = handle(node.value), rawNode = node)
    }

    private fun handleConstant(node: PythonAST.Constant): Expression {
        // TODO check and add missing types
        val tpe =
            when (node.value) {
                is String -> primitiveType("str")
                is Boolean -> primitiveType("bool")
                is Int,
                is Long -> primitiveType("int")
                is Float,
                is Double -> primitiveType("float")
                null -> objectType("None")
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
        val ret =
            when (node.func) {
                is PythonAST.Attribute -> {
                    newMemberCallExpression(
                        frontend.expressionHandler.handle(node.func),
                        rawNode = node
                    )
                }
                else -> {
                    val func = handle(node.func)

                    // try to resolve -> [ConstructExpression]
                    val currentScope = frontend.scopeManager.currentScope
                    val record =
                        currentScope?.let { frontend.scopeManager.getRecordForName(it, func.name) }

                    if (record != null) {
                        // construct expression
                        val constructExpr =
                            newConstructExpression(
                                (node.func as? PythonAST.Name)?.id,
                                rawNode = node
                            )
                        constructExpr.type = record.toType()
                        constructExpr
                    } else {
                        newCallExpression(func, rawNode = node)
                    }
                }
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
