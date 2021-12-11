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
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.frontends.cpp2.CXXLanguageFrontend2.Companion.ts_node_child_by_field_name
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newBinaryOperator
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCallExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newDeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newLiteral
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMemberCallExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import java.lang.NumberFormatException
import org.bytedeco.treesitter.TSNode
import org.bytedeco.treesitter.global.treesitter.*

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
            "scoped_identifier" -> handleScopedIdentifier(node)
            "field_expression" -> handleFieldExpression(node)
            "assignment_expression" -> handleAssignmentExpression(node)
            "binary_expression" -> handleBinaryExpression(node)
            "call_expression" -> handleCallExpression(node)
            "number_literal" -> handleNumberLiteral(node)
            "char_literal" -> handleCharLiteral(node)
            "string_literal" -> handleStringLiteral(node)
            "concatenated_string" -> handleConcatenatedString(node)
            "null" -> handleNull(node)
            "false" -> handleFalseBooleanLiteral(node)
            "true" -> handleTrueBooleanLiteral(node)
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

    private fun handleScopedIdentifier(node: TSNode): Expression {
        // until we properly handle namespaces, we just forward it to handleIdentifier

        val ref = handleIdentifier(node)

        return ref
    }

    private fun handleFieldExpression(node: TSNode): Expression {
        // check for the base
        val base = handle(ts_node_child_by_field_name(node, "argument"))

        // check the field
        val field = ts_node_child_by_field_name(node, "field")
        val name =
            if (!ts_node_is_null(field)) {
                lang.getCodeFromRawNode(field)
            } else {
                ""
            }

        val symbol = lang.getCodeFromRawNode(ts_node_child(node, 1))

        val expression =
            newMemberExpression(
                base,
                UnknownType.getUnknownType(),
                name,
                symbol,
                lang.getCodeFromRawNode(node)
            )

        return expression
    }

    /** Handles a call expression. */
    private fun handleCallExpression(node: TSNode): Expression {
        // try to parse the "function" child
        val reference = handle(ts_node_child_by_field_name(node, "function"))

        val call =
            when (reference) {
                is MemberExpression -> {
                    // Pointer types contain * or []. We do not want that here.
                    val baseType: Type = reference.base.type.root
                    val baseTypename = baseType.typeName

                    val member =
                        newDeclaredReferenceExpression(
                            reference.name,
                            UnknownType.getUnknownType(),
                            reference.name
                        )
                    member.location = lang.getLocationFromRawNode<Expression>(reference)

                    // this is probably incomplete / invalid, but the type system will update this
                    // later if the type of the base is correctly determined
                    val fqn = baseTypename + "." + member.name

                    newMemberCallExpression(
                        member.name,
                        fqn,
                        reference.base,
                        member,
                        reference.operatorCode,
                        lang.getCodeFromRawNode(node)
                    )
                }
                is DeclaredReferenceExpression ->
                    newCallExpression(
                        reference.name,
                        reference.name,
                        lang.getCodeFromRawNode(node),
                        false
                    )
                else -> {
                    throw TranslationException(
                        "Trying to 'call' something which is not a reference"
                    )
                }
            }

        // parse arguments
        val arguments = ts_node_child_by_field_name(node, "arguments")
        if (!ts_node_is_null(arguments)) {
            for (i in 0 until ts_node_named_child_count(arguments)) {
                val expression = handle(ts_node_named_child(arguments, 0))
                call.addArgument(expression)
            }
        }

        return call
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

    private fun handleTrueBooleanLiteral(node: TSNode): Literal<*> {
        return newLiteral(true, TypeParser.createFrom("bool", true), lang.getCodeFromRawNode(node))
    }

    private fun handleFalseBooleanLiteral(node: TSNode): Literal<*> {
        return newLiteral(false, TypeParser.createFrom("bool", true), lang.getCodeFromRawNode(node))
    }

    /**
     * Handles an identifier and translates it into a [DeclaredReferenceExpression]. It will also
     * try to resolve this reference (locally). See comments in the function body for more details
     * about this resolution.
     */
    private fun handleIdentifier(node: TSNode): Expression {
        val name = lang.getCodeFromRawNode(node)

        val ref = newDeclaredReferenceExpression(name, UnknownType.getUnknownType(), name)

        // Note: in the long run, we might want to get rid of this resolution. The problem is that
        // certain parsing steps, for example the FQN of a MemberCallExpression depends on knowing
        // the (estimated) type of a reference. This can only be achieved if we attempt to to
        // resolve the reference using the local scope manager. This scope manager has only a
        // limited view on the overall source graph, i.e., it is limited to the declaration in the
        // current translation unit that have been parsed before. However, for most use cases this
        // is sufficient because mostly local variables are needed.
        val declaration = lang.scopeManager.resolveReference(ref)

        declaration?.let {
            /*ref.refersTo = it

            // update the type
            ref.setType(it.type, it)*/
        }

        return ref
    }

    /**
     * Handles number literals and tries to figure out the correct type int, float or double. We identify floats if
     * the number literal contains an f. By default, numeric values that are not covertible as int will be doubles.
     */
    private fun handleNumberLiteral(node: TSNode): Expression {
        val valueStr = lang.getCodeFromRawNode(node)
        try {
            val value = valueStr?.toInt()
            return newLiteral(
                value,
                TypeParser.createFrom("int", false),
                lang.getCodeFromRawNode(node)
            )
        } catch (e: NumberFormatException) {}

        if (valueStr != null && valueStr.contains("f")) {
            try {
                val value = lang.getCodeFromRawNode(node)?.toFloat()
                return newLiteral(
                    value,
                    TypeParser.createFrom("float", false),
                    lang.getCodeFromRawNode(node)
                )
            } catch (e: NumberFormatException) {}
        }

        try {
            val value = lang.getCodeFromRawNode(node)?.toDouble()
            return newLiteral(
                value,
                TypeParser.createFrom("double", false),
                lang.getCodeFromRawNode(node)
            )
        } catch (e: NumberFormatException) {}

        return newLiteral(null, UnknownType.getUnknownType(), lang.getCodeFromRawNode(node))
    }

    private fun handleCharLiteral(node: TSNode): Expression {
        return newLiteral(
            lang.getCodeFromRawNode(node)?.get(1),
            TypeParser.createFrom("char", false),
            lang.getCodeFromRawNode(node)
        )
    }

    private fun handleStringLiteral(node: TSNode): Expression {
        val stringContent = lang.getCodeFromRawNode(node)
        return newLiteral(
            stringContent?.substring(1, stringContent.length - 1),
            TypeParser.createFrom("char[]", false),
            lang.getCodeFromRawNode(node)
        )
    }

    private fun handleBinaryExpression(node: TSNode): Expression {
        val symbol = lang.getCodeFromRawNode(ts_node_child(node, 1))

        val expression = newBinaryOperator(symbol ?: "", lang.getCodeFromRawNode(node))

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
