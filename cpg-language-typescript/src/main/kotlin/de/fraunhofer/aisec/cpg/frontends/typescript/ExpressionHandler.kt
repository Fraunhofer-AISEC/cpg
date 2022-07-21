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

import de.fraunhofer.aisec.cpg.ExperimentalTypeScript
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newLambdaExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newLiteral
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType

@ExperimentalTypeScript
class ExpressionHandler(lang: TypeScriptLanguageFrontend) :
    Handler<Expression, TypeScriptNode, TypeScriptLanguageFrontend>(::ProblemExpression, lang) {
    init {
        map.put(TypeScriptNode::class.java, ::handleNode)
    }

    private fun handleNode(node: TypeScriptNode): Expression {
        when (node.type) {
            "CallExpression" -> return handleCallExpression(node)
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

    private fun handleJsxAttribute(node: TypeScriptNode): KeyValueExpression {
        val key = this.handle(node.children?.first())
        val value = this.handle(node.children?.last())

        val keyValue =
            NodeBuilder.newKeyValueExpression(key, value, this.lang.getCodeFromRawNode(node))

        return keyValue
    }

    private fun handleJsxClosingElement(node: TypeScriptNode): Expression {
        // this basically represents an HTML tag with attributes
        val tag = NodeBuilder.newExpressionList(this.lang.getCodeFromRawNode(node))

        // it contains an Identifier node, we map this into the name
        this.lang.getIdentifierName(node).let { tag.name = "</$it>" }

        return tag
    }

    private fun handleJsxExpression(node: TypeScriptNode): Expression {
        // for now, we just treat this as a wrapper and directly return the first node
        return this.handle(node.children?.first())
    }

    private fun handleJsxOpeningElement(node: TypeScriptNode): ExpressionList {
        // this basically represents an HTML tag with attributes
        val tag = NodeBuilder.newExpressionList(this.lang.getCodeFromRawNode(node))

        // it contains an Identifier node, we map this into the name
        this.lang.getIdentifierName(node).let { tag.name = "<$it>" }

        // and a container named JsxAttributes, with JsxAttribute nodes
        tag.expressions =
            node.firstChild("JsxAttributes")?.children?.map { this.handle(it) } ?: emptyList()

        return tag
    }

    private fun handeJsxElement(node: TypeScriptNode): ExpressionList {
        val jsx = NodeBuilder.newExpressionList(this.lang.getCodeFromRawNode(node))

        jsx.expressions = node.children?.map { this.handle(it) }

        return jsx
    }

    private fun handleArrowFunction(node: TypeScriptNode): Expression {
        // parse as a function
        val func = lang.declarationHandler.handle(node) as? FunctionDeclaration

        // the function will (probably) not have a defined return type, so we try to deduce this
        // from a return statement
        if (func?.type == UnknownType.getUnknownType()) {
            val returnValue = func.bodyOrNull<ReturnStatement>()?.returnValue

            /*if (returnValue == null) {
                // we have a void function
                func.type = TypeParser.createFrom("void", false)
            } else {*/

            val returnType = returnValue?.type ?: UnknownType.getUnknownType()

            func.type = returnType
            // }
        }

        // we cannot directly return a function declaration as an expression, so we
        // wrap it into a lambda expression
        val lambda = newLambdaExpression(lang.getCodeFromRawNode(node))
        lambda.function = func

        return lambda
    }

    private fun handlePropertyAssignment(node: TypeScriptNode): KeyValueExpression {
        val key = this.handle(node.children?.first())
        val value = this.handle(node.children?.last())

        val keyValue =
            NodeBuilder.newKeyValueExpression(key, value, this.lang.getCodeFromRawNode(node))

        return keyValue
    }

    private fun handleObjectLiteralExpression(node: TypeScriptNode): InitializerListExpression {
        val ile = NodeBuilder.newInitializerListExpression(this.lang.getCodeFromRawNode(node))

        ile.initializers = node.children?.map { this.handle(it) } ?: emptyList()

        return ile
    }

    private fun handleStringLiteral(node: TypeScriptNode): Literal<String> {
        // for now, we also simply parse template expressions as string literals. we
        // might need a special literal type for that in the future. See
        // https://github.com/Fraunhofer-AISEC/cpg/issues/463
        val value =
            this.lang
                .getCodeFromRawNode(node)
                ?.trim()
                ?.replace("\"", "")
                ?.replace("`", "")
                ?.replace("'", "")
                ?: ""

        return newLiteral(
            value,
            TypeParser.createFrom("String", false),
            lang.getCodeFromRawNode(node)
        )
    }

    private fun handleIdentifier(node: TypeScriptNode): Expression {
        val name = this.lang.getCodeFromRawNode(node)?.trim()

        val ref =
            NodeBuilder.newDeclaredReferenceExpression(
                name,
                UnknownType.getUnknownType(),
                this.lang.getCodeFromRawNode(node)
            )

        return ref
    }

    private fun handlePropertyAccessExpression(node: TypeScriptNode): Expression {
        val base = this.handle(node.children?.first())

        val name = this.lang.getCodeFromRawNode(node.children?.last())

        val memberExpression =
            NodeBuilder.newMemberExpression(
                base,
                UnknownType.getUnknownType(),
                name,
                ".",
                this.lang.getCodeFromRawNode(node)
            )

        return memberExpression
    }

    private fun handleCallExpression(node: TypeScriptNode): Expression {
        val call: CallExpression

        // peek at the children, to check whether it is a call expression or member call expression
        val propertyAccess = node.firstChild("PropertyAccessExpression")

        if (propertyAccess != null) {
            val memberExpression = this.handle(propertyAccess) as MemberExpression

            // we need to build a declared reference expression out of the MemberExpression, because
            // member calls are not really handled well in the CPG. See
            // https://github.com/Fraunhofer-AISEC/cpg/issues/298
            val member =
                NodeBuilder.newDeclaredReferenceExpression(
                    memberExpression.name,
                    memberExpression.type,
                    memberExpression.name
                )

            // TODO: fqn - how?
            val fqn = memberExpression.name
            call =
                NodeBuilder.newMemberCallExpression(
                    memberExpression.name,
                    fqn,
                    memberExpression.base,
                    member,
                    ".",
                    this.lang.getCodeFromRawNode(node)
                )
        } else {
            val name = this.lang.getIdentifierName(node)

            // TODO: fqn - how?
            val fqn = name
            // regular function call
            call =
                NodeBuilder.newCallExpression(name, fqn, this.lang.getCodeFromRawNode(node), false)
        }

        // parse the arguments. the first node is the identifier, so we skip that
        val remainingNodes = node.children?.drop(1)

        remainingNodes?.forEach { call.addArgument(this.handle(it)) }

        return call
    }
}
