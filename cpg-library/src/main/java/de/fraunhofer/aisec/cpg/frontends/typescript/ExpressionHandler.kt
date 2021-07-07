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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType

@ExperimentalTypeScript
class ExpressionHandler(lang: TypeScriptLanguageFrontend) :
    Handler<Expression, TypeScriptNode, TypeScriptLanguageFrontend>(::Expression, lang) {
    init {
        map.put(TypeScriptNode::class.java, ::handleNode)
    }

    fun handleNode(node: TypeScriptNode): Expression {
        when (node.type) {
            "CallExpression" -> return handleCallExpression(node)
            "PropertyAccessExpression" -> return handlePropertyAccessExpression(node)
            "Identifier" -> return handleIdentifier(node)
            "StringLiteral" -> return handleStringLiteral(node)
        }

        return Expression()
    }

    private fun handleStringLiteral(node: TypeScriptNode): Literal<String> {
        val value = this.lang.getCodeFromRawNode(node)?.trim()?.replace("\"", "")

        return NodeBuilder.newLiteral(
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
