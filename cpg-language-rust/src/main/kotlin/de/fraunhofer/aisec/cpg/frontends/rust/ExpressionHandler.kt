/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import org.treesitter.TSNode

class ExpressionHandler(frontend: RustLanguageFrontend) :
    RustHandler<Expression, TSNode>(::ProblemExpression, frontend) {

    override fun handleNode(node: TSNode): Expression {
        return when (node.type) {
            "integer_literal" -> handleIntegerLiteral(node)
            "string_literal" -> handleStringLiteral(node)
            "boolean_literal" -> handleBooleanLiteral(node)
            "identifier" -> handleIdentifier(node)
            "binary_expression" -> handleBinaryExpression(node)
            "call_expression" -> handleCallExpression(node)
            "field_expression" -> handleFieldExpression(node)
            "if_expression" -> {
                val stmt = frontend.statementHandler.handleNode(node)
                (stmt as? Expression)
                    ?: newProblemExpression(
                        "Could not translate if_expression to Expression",
                        rawNode = node,
                    )
            }
            "block" -> frontend.statementHandler.handleBlock(node)
            else -> {
                newProblemExpression("Unknown expression type: ${node.type}", rawNode = node)
            }
        }
    }

    private fun handleIntegerLiteral(node: TSNode): Literal<Long> {
        val code = frontend.codeOf(node) ?: ""
        // Rust integers can have suffixes (e.g. 1u32) and underscores (e.g. 1_000)
        val valueStr = code.filter { it.isDigit() || it == '-' }
        val value = valueStr.toLongOrNull() ?: 0L
        return newLiteral(value, primitiveType("i32"), rawNode = node)
    }

    private fun handleStringLiteral(node: TSNode): Literal<String> {
        val code = frontend.codeOf(node) ?: ""
        val value = code.trim('"')
        return newLiteral(value, primitiveType("str"), rawNode = node)
    }

    private fun handleBooleanLiteral(node: TSNode): Literal<Boolean> {
        val code = frontend.codeOf(node) ?: ""
        val value = code == "true"
        return newLiteral(value, primitiveType("bool"), rawNode = node)
    }

    private fun handleIdentifier(node: TSNode): Reference {
        val name = frontend.codeOf(node) ?: ""
        return newReference(name, rawNode = node)
    }

    private fun handleBinaryExpression(node: TSNode): BinaryOperator {
        val left = node.getChildByFieldName("left")
        val right = node.getChildByFieldName("right")
        val operator = node.getChildByFieldName("operator")

        val op = newBinaryOperator(operator?.let { frontend.codeOf(it) } ?: "", rawNode = node)
        if (left != null) op.lhs = handle(left)
        if (right != null) op.rhs = handle(right)

        return op
    }

    private fun handleCallExpression(node: TSNode): Expression {
        val function = node.getChildByFieldName("function")
        val arguments = node.getChildByFieldName("arguments")

        val callee =
            function?.let { handle(it) }
                ?: newProblemExpression("Missing function in call", rawNode = node)

        val call =
            if (callee is MemberExpression) {
                newMemberCallExpression(callee, rawNode = node)
            } else {
                newCallExpression(callee, rawNode = node)
            }

        if (arguments != null) {
            for (i in 0 until arguments.childCount) {
                val arg = arguments.getChild(i)
                if (arg.isNamed) {
                    call.addArgument(handle(arg))
                }
            }
        }

        return call
    }

    private fun handleFieldExpression(node: TSNode): MemberExpression {
        val value = node.getChildByFieldName("value")
        val field = node.getChildByFieldName("field")

        val base =
            value?.let { handle(it) }
                ?: newProblemExpression("Missing value in field expression", rawNode = node)
        val name = field?.let { frontend.codeOf(it) } ?: ""

        return newMemberExpression(name, base, rawNode = node)
    }
}
