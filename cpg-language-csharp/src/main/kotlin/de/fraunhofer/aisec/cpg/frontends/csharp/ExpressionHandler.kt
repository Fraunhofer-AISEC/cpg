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
package de.fraunhofer.aisec.cpg.frontends.csharp

import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.newLiteral
import de.fraunhofer.aisec.cpg.graph.newProblemExpression
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.primitiveType

class ExpressionHandler(frontend: CSharpLanguageFrontend) :
    CSharpHandler<Expression, Csharp.AST.ExpressionSyntax>(::ProblemExpression, frontend) {
    override fun handleNode(node: Csharp.AST.ExpressionSyntax): Expression {
        return when (node) {
            is Csharp.AST.LiteralExpressionSyntax -> handleLiteral(node)
            else ->
                newProblemExpression(
                    "The expression of class ${node.javaClass} is not supported yet",
                    rawNode = node,
                )
        }
    }

    private fun handleLiteral(node: Csharp.AST.LiteralExpressionSyntax): Expression {
        return when (node.csharpType) {
            "NumericLiteralExpression" ->
                newLiteral(node.value.toInt(), primitiveType("int"), rawNode = node)
            "StringLiteralExpression" ->
                newLiteral(node.value, primitiveType("string"), rawNode = node)
            "TrueLiteralExpression" -> newLiteral(true, primitiveType("bool"), rawNode = node)
            "FalseLiteralExpression" -> newLiteral(false, primitiveType("bool"), rawNode = node)
            "NullLiteralExpression" -> newLiteral(null, objectType("null"), rawNode = node)
            "CharacterLiteralExpression" ->
                newLiteral(node.value.single(), primitiveType("char"), rawNode = node)
            else ->
                // TODO: Return unknowntype() instead?
                newProblemExpression("Unknown type: ${node.csharpType}", rawNode = node)
        }
    }
}
