/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cpp2

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.cpp2.CXXLanguageFrontend2.Companion.ts_node_child_by_field_name
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import org.bytedeco.treesitter.TSNode
import org.bytedeco.treesitter.global.treesitter

/**
 * This handler takes care of parsing
 * [expressions](https://en.cppreference.com/w/cpp/language/expressions).
 */
class ExpressionHandler(lang: CXXLanguageFrontend2) :
    Handler<Expression, TSNode, CXXLanguageFrontend2>(::Expression, lang) {
    init {
        map.put(TSNode::class.java, ::handleExpression)
    }

    private fun handleExpression(node: TSNode): Expression {
        return when (node.type) {
            "identifier" -> handleIdentifier(node)
            "assignment_expression" -> handleAssignmentExpression(node)
            "binary_expression" -> handleBinaryExpression(node)
            "number_literal" -> handleNumberLiteral(node)
            "concatenated_string" -> handleConcatenatedString(node)
            "null" -> handleNull(node)
            else -> {
                LanguageFrontend.log.error(
                    "Not handling expression of type {} yet: {}",
                    node.type,
                    lang.getCodeFromRawNode(node)
                )
                configConstructor.get()
            }
        }
    }

    private fun handleConcatenatedString(node: TSNode): Expression {
        val code = lang.getCodeFromRawNode(node)

        val value = code?.replace("\"", "")

        return newLiteral(value, TypeParser.createFrom("const char*", false), code)
    }

    private fun handleNull(node: TSNode): Literal<*> {
        return newLiteral(
            null,
            TypeParser.createFrom("std::nullptr_t", false),
            lang.getCodeFromRawNode(node)
        )
    }

    private fun handleIdentifier(node: TSNode): Expression {
        val name = lang.getCodeFromRawNode(node)

        return newDeclaredReferenceExpression(name, UnknownType.getUnknownType(), name)
    }

    private fun handleNumberLiteral(node: TSNode): Expression {
        val value = lang.getCodeFromRawNode(node)?.toInt()

        return newLiteral(value, TypeParser.createFrom("int", false), lang.getCodeFromRawNode(node))
    }

    private fun handleBinaryExpression(node: TSNode): Expression {
        val symbol = lang.getCodeFromRawNode(treesitter.ts_node_child(node, 1))

        val expression = newBinaryOperator(symbol, lang.getCodeFromRawNode(node))

        expression.lhs = handleExpression(ts_node_child_by_field_name(node, "left"))
        expression.rhs = handleExpression(ts_node_child_by_field_name(node, "right"))

        return expression
    }

    /**
     * Handles an
     * [assignment expression](https://en.cppreference.com/w/cpp/language/operator_assignment). It
     * is parsed as a [BinaryOperator].
     */
    private fun handleAssignmentExpression(node: TSNode): BinaryOperator {
        val expression = newBinaryOperator("=", lang.getCodeFromRawNode(node))

        expression.lhs = handleExpression(ts_node_child_by_field_name(node, "left"))
        expression.rhs = handleExpression(ts_node_child_by_field_name(node, "right"))

        return expression
    }
}
