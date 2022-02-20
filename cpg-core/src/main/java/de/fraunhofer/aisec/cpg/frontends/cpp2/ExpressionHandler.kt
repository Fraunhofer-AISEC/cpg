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
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newBinaryOperator
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCallExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newDeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newLiteral
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMemberCallExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMemberExpression
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import io.github.oxisto.kotlintree.jvm.*
import java.util.ArrayList

/**
 * This handler takes care of parsing
 * [expressions](https://en.cppreference.com/w/cpp/language/expressions).
 */
class ExpressionHandler(lang: CXXLanguageFrontend2) :
    Handler<Expression, Node, CXXLanguageFrontend2>(::Expression, lang) {
    init {
        map.put(Node::class.java, ::handleExpression)
    }

    private fun handleExpression(node: Node): Expression {
        return when (node.type) {
            "cast_expression" -> handleCastExpression(node)
            "identifier" -> handleIdentifier(node)
            "scoped_identifier" -> handleScopedIdentifier(node)
            "field_expression" -> handleFieldExpression(node)
            "assignment_expression" -> handleAssignmentExpression(node)
            "binary_expression" -> handleBinaryExpression(node)
            "update_expression" -> handleUnaryExpression(node)
            "unary_expression" -> handleUnaryExpression(node)
            "sizeof_expression" -> handleSizeOfExpression(node)
            "pointer_expression" -> handleUnaryExpression(node)
            "parenthesized_expression" -> handleParenthesizedExpression(node)
            "new_expression" -> handleNewExpression(node)
            "call_expression" -> handleCallExpression(node)
            "number_literal" -> handleNumberLiteral(node)
            "char_literal" -> handleCharLiteral(node)
            "string_literal" -> handleStringLiteral(node)
            "concatenated_string" -> handleConcatenatedString(node)
            "initializer_list" -> handleInitializerList(node)
            "subscript_expression" -> handleSubscriptExpression(node)
            "null" -> handleNull(node)
            "false" -> handleFalseBooleanLiteral(node)
            "true" -> handleTrueBooleanLiteral(node)
            "template_function" -> {
                return when (lang.getCodeFromRawNode(node.childByFieldName("name"))) {
                    "dynamic_cast" -> handleCastExpressionWithOperator(node, 1)
                    "static_cast" -> handleCastExpressionWithOperator(node, 2)
                    "reinterpret_cast" -> handleCastExpressionWithOperator(node, 3)
                    "const_cast" -> handleCastExpressionWithOperator(node, 4)
                    else -> {
                        LanguageFrontend.log.error("Not handling templates yet")
                        configConstructor.get()
                    }
                }

                /*val name = lang.getCodeFromRawNode(node.childByFieldName("name"))
                if (name.equals("static_cast") || name.equals("const_cast") || name.equals()) {
                    return handleCastExpression(node)
                } else {
                    return configConstructor.get()
                }*/
            }
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

    private fun handleScopedIdentifier(node: Node): Expression {
        // until we properly handle namespaces, we just forward it to handleIdentifier

        val ref = handleIdentifier(node)

        return ref
    }

    private fun handleFieldExpression(node: Node): Expression {
        // check for the base
        val base = handle(node.childByFieldName("argument"))

        // check the field
        val field = node.childByFieldName("field")
        val name =
            if (!field.isNull) {
                lang.getCodeFromRawNode(field)
            } else {
                ""
            }

        val symbol = lang.getCodeFromRawNode(node.child(1))

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

    private fun handleArgumentList(node: Node): List<Expression> {
        var argumentList: MutableList<Expression> = arrayListOf()

        for (namedChild in 1.rangeTo(node.namedChildCount)) {
            argumentList.add(handle(node.child(namedChild)))
        }

        return argumentList
    }

    private fun handleTemplateArgumentList(node: Node): List<de.fraunhofer.aisec.cpg.graph.Node> {
        var argumentList: MutableList<de.fraunhofer.aisec.cpg.graph.Node> = arrayListOf()

        for (namedChild in 1.rangeTo(node.namedChildCount)) {
            var child = node.child(namedChild)
            if (child.type.equals("type_descriptor")) {
                argumentList.add(lang.handleType(child))
            } else {
                argumentList.add(handle(child))
            }
        }

        return argumentList
    }

    private fun handleCastExpressionWithOperator(node: Node, castOperator: Int): Expression {
        val castExpression = NodeBuilder.newCastExpression(lang.getCodeFromRawNode(node))
        castExpression.setCastOperator(castOperator)
        castExpression.castType =
            handleTemplateArgumentList(node.childByFieldName("arguments"))[0] as Type?
        return castExpression
    }

    private fun handleCastExpression(node: Node): Expression {
        val castExpression = NodeBuilder.newCastExpression(lang.getCodeFromRawNode(node))
        castExpression.expression = handle(node.childByFieldName("value"))
        castExpression.setCastOperator(0)
        castExpression.castType = lang.handleType(node.childByFieldName("type"))

        if (TypeManager.getInstance().isPrimitive(castExpression.castType)) {
            castExpression.type = castExpression.castType
        } else {
            castExpression.expression.registerTypeListener(castExpression)
        }

        return castExpression
    }

    private fun handleSubscriptExpression(node: Node): Expression {
        val expression = NodeBuilder.newArraySubscriptionExpression(lang.getCodeFromRawNode(node))
        expression.arrayExpression = handle(node.childByFieldName("argument"))
        expression.subscriptExpression = handle(node.childByFieldName("index"))
        return expression
    }

    private fun handleInitializerList(node: Node): Expression {
        val expression = NodeBuilder.newInitializerListExpression(lang.getCodeFromRawNode(node))
        val initializers: MutableList<Expression?> = ArrayList()

        for (i in 0 until node.namedChildCount) {
            initializers.add(handle(node.namedChild(i)))
        }
        expression.initializers = initializers

        return expression
    }

    private fun handleNewExpression(node: Node): Expression {
        val name = node.childByFieldName("type")
        val type = TypeParser.createFrom(lang.getCodeFromRawNode(name)!!, true, lang)

        val argumentList = handleArgumentList(node.childByFieldName("arguments"))
        val initializer =
            NodeBuilder.newConstructExpression(
                lang.getCodeFromRawNode(node.childByFieldName("arguments"))
            )
        if (!argumentList.isEmpty()) {
            for (argument in argumentList) {
                initializer.addArgument(argument)
            }
        } else {
            initializer.isInferred = true
        }

        initializer.type = type

        val new =
            NodeBuilder.newNewExpression(
                lang.getCodeFromRawNode(node),
                type.reference(PointerType.PointerOrigin.POINTER)
            )

        new.initializer = initializer

        return new
    }

    /** Handles a call expression. */
    private fun handleCallExpression(node: Node): Expression {
        // try to parse the "function" child
        val reference = handle(node.childByFieldName("function"))

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
                is CastExpression -> {
                    reference.code = lang.getCodeFromRawNode(node)
                    reference.expression = handleArgumentList(node.childByFieldName("arguments"))[0]
                    if (TypeManager.getInstance().isPrimitive(reference.castType)) {
                        reference.type = reference.castType
                    } else {
                        reference.expression.registerTypeListener(reference)
                    }
                    reference
                }
                else -> {
                    throw TranslationException(
                        "Trying to 'call' something which is not a reference"
                    )
                }
            }

        if (call is CallExpression) {
            // parse arguments
            val arguments = "arguments" of node
            if (!arguments.isNull) {
                for (i in 0 until arguments.namedChildCount) {
                    val expression = handle(arguments.namedChild(i))

                    call.addArgument(expression)
                }
            }
        }

        return call
    }

    private fun handleConcatenatedString(node: Node): Expression {
        val code = lang.getCodeFromRawNode(node)

        val value = code?.replace("\"", "")

        return newLiteral(value, TypeParser.createFrom("const char*", false), code)
    }

    private fun handleNull(node: Node): Literal<*> {
        return newLiteral(
            null,
            TypeParser.createFrom("std::nullptr_t", false),
            lang.getCodeFromRawNode(node)
        )
    }

    private fun handleTrueBooleanLiteral(node: Node): Literal<*> {
        return newLiteral(true, TypeParser.createFrom("bool", true), lang.getCodeFromRawNode(node))
    }

    private fun handleFalseBooleanLiteral(node: Node): Literal<*> {
        return newLiteral(false, TypeParser.createFrom("bool", true), lang.getCodeFromRawNode(node))
    }

    /**
     * Handles an identifier and translates it into a [DeclaredReferenceExpression]. It will also
     * try to resolve this reference (locally). See comments in the function body for more details
     * about this resolution.
     */
    private fun handleIdentifier(node: Node): Expression {
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
     * Handles number literals and tries to figure out the correct type int, float or double. We
     * identify floats if the number literal contains an f. By default, numeric values that are not
     * covertible as int will be doubles.
     */
    private fun handleNumberLiteral(node: Node): Expression {
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

    private fun handleCharLiteral(node: Node): Expression {
        return newLiteral(
            lang.getCodeFromRawNode(node)?.get(1),
            TypeParser.createFrom("char", false),
            lang.getCodeFromRawNode(node)
        )
    }

    private fun handleStringLiteral(node: Node): Expression {
        val stringContent = lang.getCodeFromRawNode(node)
        return newLiteral(
            stringContent?.substring(1, stringContent.length - 1),
            TypeParser.createFrom("char[]", false),
            lang.getCodeFromRawNode(node)
        )
    }

    private fun handleBinaryExpression(node: Node): Expression {
        val symbol = lang.getCodeFromRawNode(node.child(1))

        val expression = newBinaryOperator(symbol ?: "", lang.getCodeFromRawNode(node))

        expression.lhs = handleExpression("left" of node)
        expression.rhs = handleExpression("right" of node)

        return expression
    }

    private fun handleUnaryExpression(node: Node): Expression {
        val identifier = lang.getCodeFromRawNode(node.childByFieldName("argument"))
        var prefix = true
        var operator: String? = null
        if (identifier == lang.getCodeFromRawNode(node.child(0))) {
            prefix = false
            operator = lang.getCodeFromRawNode(node.child(1))
        } else {
            operator = lang.getCodeFromRawNode(node.child(0))
        }

        val expression =
            NodeBuilder.newUnaryOperator(operator, !prefix, prefix, lang.getCodeFromRawNode(node))

        expression.input = handleExpression(node.childByFieldName("argument"))

        return expression
    }

    private fun handleSizeOfExpression(node: Node): Expression {
        val expression =
            NodeBuilder.newUnaryOperator("sizeof", false, true, lang.getCodeFromRawNode(node))

        expression.input = handleExpression(node.childByFieldName("value"))

        return expression
    }

    private fun handleParenthesizedExpression(node: Node): Expression {
        return handleExpression(node.child(1))
    }

    /**
     * Handles an
     * [assignment expression](https://en.cppreference.com/w/cpp/language/operator_assignment). It
     * is parsed as a [BinaryOperator].
     */
    private fun handleAssignmentExpression(node: Node): BinaryOperator {
        val expression = newBinaryOperator("=", lang.getCodeFromRawNode(node))

        expression.lhs = handleExpression("left" of node)
        expression.rhs = handleExpression("right" of node)

        return expression
    }
}
