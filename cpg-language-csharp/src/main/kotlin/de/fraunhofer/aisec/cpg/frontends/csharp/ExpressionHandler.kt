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

import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.newAssign
import de.fraunhofer.aisec.cpg.graph.newBinaryOperator
import de.fraunhofer.aisec.cpg.graph.newLiteral
import de.fraunhofer.aisec.cpg.graph.newMemberAccess
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
            is Csharp.AST.AssignmentExpressionSyntax -> handleAssignmentExpression(node)
            is Csharp.AST.MemberAccessExpressionSyntax -> handleMemberAccessExpression(node)
            else ->
                newProblemExpression(
                    "The expression of class ${node.javaClass} is not supported yet",
                    rawNode = node,
                )
        }
    }

    /**
     * Translates an [IdentifierNameSyntax][Csharp.AST.IdentifierNameSyntax] into a [Reference].
     *
     * C# spec:
     * [SimpleNames](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#12842-simple-names)
     */
    private fun handleIdentifierName(node: Csharp.AST.IdentifierNameSyntax): Reference {
        return newReference(name = node.identifier, rawNode = node)
    }

    /**
     * Translates a [BinaryExpressionSyntax][Csharp.AST.BinaryExpressionSyntax] into a
     * [BinaryOperator].
     *
     * C# spec:
     * [BinaryOperator](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#12104-binary-operators)
     */
    private fun handleBinaryExpression(node: Csharp.AST.BinaryExpressionSyntax): BinaryOperator {
        return newBinaryOperator(operatorCode = node.operatorToken, rawNode = node).apply {
            this.lhs = handle(node.left)
            this.rhs = handle(node.right)
        }
    }

    /**
     * Translates an [AssignmentExpressionSyntax][Csharp.AST.AssignmentExpressionSyntax] into an
     * [Assign].
     *
     * C# spec:
     * [AssignmentOperators](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#12214-assignment-operators)
     */
    private fun handleAssignmentExpression(node: Csharp.AST.AssignmentExpressionSyntax): Assign {
        return newAssign(
            operatorCode = node.operatorToken,
            lhs = listOf(handle(node.left)),
            rhs = listOf(handle(node.right)),
            rawNode = node,
        )
    }

    /**
     * Translates a [LiteralExpressionSyntax][Csharp.AST.LiteralExpressionSyntax] into a [Literal].
     * The concrete literal subclass is determined by the Roslyn SyntaxKind.
     *
     * Note: [NumericLiteralExpressionSyntax][Csharp.AST.NumericLiteralExpressionSyntax] does not
     * distinguish between numeric types (e.g. int vs long). Instead, the .NET runtime type of
     * `Token.Value` (e.g. `System.Int32` vs `System.Int64`) can be used, but this requires an
     * additional mapping from .NET types to C# keywords.
     *
     * C# spec:
     * [Literals](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/lexical-structure#645-literals)
     */
    private fun handleLiteralExpression(node: Csharp.AST.LiteralExpressionSyntax): Expression {
        val builtInTypes = frontend.language.builtInTypes
        return when (node) {
            is Csharp.AST.NumericLiteralExpressionSyntax ->
                newLiteral(node.value.toInt(), builtInTypes.getValue("int"), rawNode = node)
            is Csharp.AST.StringLiteralExpressionSyntax ->
                newLiteral(node.value, builtInTypes.getValue("string"), rawNode = node)
            is Csharp.AST.BooleanLiteralExpressionSyntax ->
                newLiteral(node.value.toBoolean(), builtInTypes.getValue("bool"), rawNode = node)
            is Csharp.AST.CharacterLiteralExpressionSyntax ->
                newLiteral(node.value.single(), builtInTypes.getValue("char"), rawNode = node)
            is Csharp.AST.NullLiteralExpressionSyntax ->
                newLiteral(null, objectType("null"), rawNode = node)
            else -> newProblemExpression("Unknown literal type: ${node.csharpType}", rawNode = node)
        }
    }

    /**
     * Translates a [MemberAccessExpressionSyntax][Csharp.AST.MemberAccessExpressionSyntax] into a
     * [MemberAccess].
     *
     * C# spec:
     * [MemberAccess](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#12813-member-access)
     */
    private fun handleMemberAccessExpression(
        node: Csharp.AST.MemberAccessExpressionSyntax
    ): MemberAccess {
        val base = handle(node.expression)
        return newMemberAccess(
            name = node.name,
            base = base,
            operatorCode = node.operatorToken,
            rawNode = node,
        )
    }
}
