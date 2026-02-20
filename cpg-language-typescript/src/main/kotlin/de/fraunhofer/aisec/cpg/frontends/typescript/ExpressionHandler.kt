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
package de.fraunhofer.aisec.cpg.frontends.typescript

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*

class ExpressionHandler(lang: TypeScriptLanguageFrontend) :
    Handler<Expression, TypeScriptNode, TypeScriptLanguageFrontend>(::ProblemExpression, lang) {
    init {
        map.put(TypeScriptNode::class.java, ::handleNode)
    }

    private fun handleNode(node: TypeScriptNode): Expression {
        when (node.type) {
            "Call" -> return handleCall(node)
            "PropertyAccessExpression" -> return handlePropertyAccessExpression(node)
            "Identifier" -> return handleIdentifier(node)
            "FirstTemplateToken" -> return handleStringLiteral(node)
            "TemplateExpression" -> return handleStringLiteral(node)
            "NoSubstitutionTemplateLiteral" -> return handleStringLiteral(node)
            "StringLiteral" -> return handleStringLiteral(node)
            "ObjectLiteralExpression" -> return handleObjectLiteralExpression(node)
            "PropertyAssignment" -> return handlePropertyAssignment(node)
            "ArrowFunction" -> return handleArrowFunction(node)
            "FunctionExpression" -> return handleArrowFunction(node)
            "JsxElement" -> return handeJsxElement(node)
            "JsxOpeningElement" -> return handleJsxOpeningElement(node)
            "JsxAttribute" -> return handleJsxAttribute(node)
            "JsxText" -> return handleStringLiteral(node)
            "JsxExpression" -> return handleJsxExpression(node)
            "JsxClosingElement" -> return handleJsxClosingElement(node)
        }

        return ProblemExpression("No handler was implemented for nodes of type " + node.type)
    }

    private fun handleJsxAttribute(node: TypeScriptNode): KeyValue {
        val key =
            node.children?.first()?.let { this.handle(it) } ?: newProblemExpression("missing key")
        val value =
            node.children?.last()?.let { this.handle(it) } ?: newProblemExpression("missing value")

        return newKeyValue(key, value, rawNode = node)
    }

    private fun handleJsxClosingElement(node: TypeScriptNode): Expression {
        // this basically represents an HTML tag with attributes
        val tag = newExpressionList(rawNode = node)

        // it contains an Identifier node, we map this into the name
        this.frontend.getIdentifierName(node).let { tag.name = Name("</$it>") }

        return tag
    }

    private fun handleJsxExpression(node: TypeScriptNode): Expression {
        // for now, we just treat this as a wrapper and directly return the first node
        return node.children?.first()?.let { this.handle(it) }
            ?: ProblemExpression("problem parsing expression")
    }

    private fun handleJsxOpeningElement(node: TypeScriptNode): ExpressionList {
        // this basically represents an HTML tag with attributes
        val tag = newExpressionList(rawNode = node)

        // it contains an Identifier node, we map this into the name
        this.frontend.getIdentifierName(node).let { tag.name = Name("<$it>") }

        // and a container named JsxAttributes, with JsxAttribute nodes
        tag.expressions =
            node
                .firstChild("JsxAttributes")
                ?.children
                ?.mapNotNull { this.handle(it) }
                ?.toMutableList() ?: mutableListOf()

        return tag
    }

    private fun handeJsxElement(node: TypeScriptNode): ExpressionList {
        val jsx = newExpressionList(rawNode = node)

        jsx.expressions =
            node.children?.mapNotNull { this.handle(it) }?.toMutableList() ?: mutableListOf()

        return jsx
    }

    private fun handleArrowFunction(node: TypeScriptNode): Expression {
        // parse as a function
        val func = frontend.declarationHandler.handle(node) as? Function

        // the function will (probably) not have a defined return type, so we try to deduce this
        // from a return statement
        if (func?.type == unknownType()) {
            val returnValue = func.bodyOrNull<ReturnStatement>()?.returnValue

            /*if (returnValue == null) {
                // we have a void function
                func.type = TypeParser.createFrom("void", false)
            } else {*/

            val returnType = returnValue?.type ?: unknownType()

            func.type = returnType
            // }
        }

        // we cannot directly return a function declaration as an expression, so we
        // wrap it into a lambda expression
        val lambda = newLambda(rawNode = node)
        lambda.function = func

        return lambda
    }

    private fun handlePropertyAssignment(node: TypeScriptNode): KeyValue {
        val key =
            node.children?.first()?.let { this.handle(it) } ?: newProblemExpression("missing key")
        val value =
            node.children?.last()?.let { this.handle(it) } ?: newProblemExpression("missing value")

        return newKeyValue(key, value, rawNode = node)
    }

    private fun handleObjectLiteralExpression(node: TypeScriptNode): InitializerList {
        val ile = newInitializerList(unknownType(), rawNode = node)

        ile.initializers =
            node.children?.mapNotNull { this.handle(it) }?.toMutableList() ?: mutableListOf()

        return ile
    }

    private fun handleStringLiteral(node: TypeScriptNode): Literal<String> {
        // for now, we also simply parse template expressions as string literals. we
        // might need a special literal type for that in the future. See
        // https://github.com/Fraunhofer-AISEC/cpg/issues/463
        val value =
            this.frontend
                .codeOf(node)
                ?.trim()
                ?.replace("\"", "")
                ?.replace("`", "")
                ?.replace("'", "") ?: ""

        return newLiteral(value, primitiveType("string"), rawNode = node)
    }

    private fun handleIdentifier(node: TypeScriptNode): Expression {
        val name = this.frontend.codeOf(node)?.trim() ?: ""

        return newReference(name, unknownType(), rawNode = node)
    }

    private fun handlePropertyAccessExpression(node: TypeScriptNode): Expression {
        val base =
            node.children?.first()?.let { this.handle(it) }
                ?: ProblemExpression("problem parsing base")

        val name = node.children?.last()?.let { this.frontend.codeOf(it) } ?: ""

        return newMemberAccess(name, base, unknownType(), ".", rawNode = node)
    }

    private fun handleCall(node: TypeScriptNode): Expression {
        val call: Call

        // peek at the children, to check whether it is a call expression or member call expression
        val propertyAccess = node.firstChild("PropertyAccessExpression")

        call =
            if (propertyAccess != null) {
                val memberExpressionExpression =
                    this.handle(propertyAccess) as? MemberAccess
                        ?: return ProblemExpression("node is not a member expression")

                newMemberCall(memberExpressionExpression, rawNode = node)
            } else {
                // TODO: fqn - how?
                val fqn = this.frontend.getIdentifierName(node)
                // regular function call

                val ref = newReference(fqn)

                newCall(ref, fqn, false, rawNode = node)
            }

        // parse the arguments. the first node is the identifier, so we skip that
        val remainingNodes = node.children?.drop(1)

        remainingNodes?.forEach { this.handle(it)?.let { it1 -> call.addArgument(it1) } }

        return call
    }
}
