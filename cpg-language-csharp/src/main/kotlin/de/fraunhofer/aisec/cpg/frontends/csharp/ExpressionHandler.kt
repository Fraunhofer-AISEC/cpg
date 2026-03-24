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

import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.newBinaryOperator
import de.fraunhofer.aisec.cpg.graph.newLiteral
import de.fraunhofer.aisec.cpg.graph.newProblemExpression
import de.fraunhofer.aisec.cpg.graph.newReference
import de.fraunhofer.aisec.cpg.graph.objectType

class ExpressionHandler(frontend: CSharpLanguageFrontend) :
    CSharpHandler<Expression, Csharp.AST.ExpressionSyntax>(::ProblemExpression, frontend) {
    override fun handleNode(node: Csharp.AST.ExpressionSyntax): Expression {
        return when (node) {
            is Csharp.AST.IdentifierNameSyntax -> handleIdentifierName(node)
            is Csharp.AST.LiteralExpressionSyntax -> handleLiteralExpression(node)
            is Csharp.AST.BinaryExpressionSyntax -> handleBinaryExpression(node)
            else ->
                newProblemExpression(
                    "The expression of class ${node.javaClass} is not supported yet",
                    rawNode = node,
                )
        }
    }

    private fun handleIdentifierName(node: Csharp.AST.IdentifierNameSyntax): Reference {
        return newReference(name = node.identifier, rawNode = node)
    }

    private fun handleBinaryExpression(node: Csharp.AST.BinaryExpressionSyntax): BinaryOperator {
        val binOp = newBinaryOperator(operatorCode = node.operatorToken, rawNode = node)
        binOp.lhs = handle(node.left)
        binOp.rhs = handle(node.right)
        return binOp
    }

    /**
     * Translates a C#
     * [`LiteralExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.literalexpressionsyntax?view=roslyn-dotnet-5.0.0)
     * into a [Literal].
     */
    private fun handleLiteralExpression(node: Csharp.AST.LiteralExpressionSyntax): Expression {
        val builtInTypes = frontend.language.builtInTypes
        return when (node.kind) {
            "int" -> newLiteral(node.value.toInt(), builtInTypes.getValue("int"), rawNode = node)
            "string" -> newLiteral(node.value, builtInTypes.getValue("string"), rawNode = node)
            "bool" ->
                newLiteral(node.value.toBoolean(), builtInTypes.getValue("bool"), rawNode = node)
            "char" -> newLiteral(node.value.single(), builtInTypes.getValue("char"), rawNode = node)
            "null" -> newLiteral(null, objectType("null"), rawNode = node)
            else -> newProblemExpression("Unknown literal kind: ${node.kind}", rawNode = node)
        }
    }
}
