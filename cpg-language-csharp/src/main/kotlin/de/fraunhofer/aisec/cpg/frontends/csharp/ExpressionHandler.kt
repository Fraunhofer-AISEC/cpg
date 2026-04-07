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

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.graph.implicit
import de.fraunhofer.aisec.cpg.graph.newAssign
import de.fraunhofer.aisec.cpg.graph.newBinaryOperator
import de.fraunhofer.aisec.cpg.graph.newCall
import de.fraunhofer.aisec.cpg.graph.newConstruction
import de.fraunhofer.aisec.cpg.graph.newDeclarationStatement
import de.fraunhofer.aisec.cpg.graph.newExpressionList
import de.fraunhofer.aisec.cpg.graph.newLiteral
import de.fraunhofer.aisec.cpg.graph.newMemberAccess
import de.fraunhofer.aisec.cpg.graph.newMemberCall
import de.fraunhofer.aisec.cpg.graph.newNew
import de.fraunhofer.aisec.cpg.graph.newProblemExpression
import de.fraunhofer.aisec.cpg.graph.newReference
import de.fraunhofer.aisec.cpg.graph.newVariable
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.unknownType

class ExpressionHandler(frontend: CSharpLanguageFrontend) :
    CSharpHandler<Expression, Csharp.AST.ExpressionSyntax>(::ProblemExpression, frontend) {
    override fun handleNode(node: Csharp.AST.ExpressionSyntax): Expression {
        return when (node) {
            is Csharp.AST.IdentifierNameSyntax -> handleIdentifierName(node)
            is Csharp.AST.LiteralExpressionSyntax -> handleLiteralExpression(node)
            is Csharp.AST.BinaryExpressionSyntax -> handleBinaryExpression(node)
            is Csharp.AST.AssignmentExpressionSyntax -> handleAssignmentExpression(node)
            is Csharp.AST.InvocationExpressionSyntax -> handleInvocationExpression(node)
            is Csharp.AST.MemberAccessExpressionSyntax -> handleMemberAccessExpression(node)
            is Csharp.AST.ThisExpressionSyntax -> handleThisExpression(node)
            is Csharp.AST.BaseObjectCreationExpressionSyntax -> handleObjectCreationExpression(node)
            else -> ProblemExpression("Not supported: ${node.csharpType}")
        }
    }

    /**
     * Translates an [IdentifierNameSyntax][Csharp.AST.IdentifierNameSyntax] into a [Reference].
     *
     * C# spec:
     * [Simple names](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#1284-simple-names)
     */
    private fun handleIdentifierName(node: Csharp.AST.IdentifierNameSyntax): Reference {
        return newReference(name = node.identifier, rawNode = node)
    }

    /**
     * Translates a [BinaryExpressionSyntax][Csharp.AST.BinaryExpressionSyntax] into a
     * [BinaryOperator].
     *
     * C# spec:
     * [Binary operator](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#1245-binary-operator-overload-resolution)
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
     * [Assignment operators](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#1223-assignment-operators)
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
            // TODO: Return unknownType()?
            else -> newProblemExpression("Unknown literal type: ${node.csharpType}", rawNode = node)
        }
    }

    /**
     * Translates an [InvocationExpressionSyntax][Csharp.AST.InvocationExpressionSyntax] into a
     * [Call]. If the callee is a [MemberAccess] (e.g. `obj.Method()`), a `MemberCall` is created.
     * Otherwise (e.g. `Foo()`), a plain [Call] is created.
     *
     * C# spec:
     * [Invocation expressions](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#12810-invocation-expressions)
     */
    private fun handleInvocationExpression(node: Csharp.AST.InvocationExpressionSyntax): Call {
        val callee = handle(node.expression)
        val call =
            if (callee is MemberAccess) {
                newMemberCall(callee, rawNode = node)
            } else {
                newCall(callee, rawNode = node)
            }
        for (arg in node.argumentList.arguments) {
            call.addArgument(handle(arg.expression))
        }
        return call
    }

    /**
     * Translates a [MemberAccessExpressionSyntax][Csharp.AST.MemberAccessExpressionSyntax] into a
     * [MemberAccess].
     *
     * C# spec:
     * [Member access](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#1287-member-access)
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

    /**
     * Translates a [ThisExpressionSyntax][Csharp.AST.ThisExpressionSyntax] into a [Reference] with
     * the name `this`.
     *
     * C# spec:
     * [This access](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#12814-this-access)
     */
    private fun handleThisExpression(node: Csharp.AST.ThisExpressionSyntax): Reference {
        val type = frontend.scopeManager.currentRecord?.toType() ?: unknownType()
        return newReference(name = "this", type = type, rawNode = node)
    }

    /**
     * Translates a
     * [BaseObjectCreationExpressionSyntax][Csharp.AST.BaseObjectCreationExpressionSyntax] into a
     * [New] with a [Construction][Construction] initializer. Handles both explicit (`new Foo()`)
     * and implicit (`new()`) object creations. For implicit cases, the type is [unknownType] and
     * will be resolved later.
     *
     * If the expression has an object initializer (e.g. `new Foo(1) { X = 2, Y = 3 }`), the result
     * is wrapped in an [ExpressionList] via [handleObjectInitializer].
     *
     * C# spec:
     * [Object creation expressions](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#128172-object-creation-expressions)
     */
    private fun handleObjectCreationExpression(
        node: Csharp.AST.BaseObjectCreationExpressionSyntax
    ): Expression {
        val type =
            if (node is Csharp.AST.ObjectCreationExpressionSyntax) {
                frontend.typeOf(node.type)
            } else {
                unknownType()
            }
        val newExpression = newNew(type, rawNode = node)
        val construction = newConstruction(type.name.localName, rawNode = node)
        construction.type = type
        val argumentList = node.argumentList
        if (argumentList != null) {
            for (arg in argumentList.arguments) {
                construction.addArgument(handle(arg.expression))
            }
        }
        newExpression.initializer = construction

        val initializer = node.initializer
        if (initializer != null) {
            return handleObjectInitializer(initializer, newExpression, type)
        }
        return newExpression
    }

    /**
     * Handles an object initializer by wrapping the expressions in an [ExpressionList]. Since we do
     * not have a direct representation for object initializers, we destructure them into an
     * equivalent form. For example:
     * ```csharp
     * var p = new Point(1) { X = 0, Y = 1 };
     * ```
     *
     * is equivalent to:
     * ```csharp
     * Point __tmp = new Point(1);
     * __tmp.X = 0;
     * __tmp.Y = 1;
     * var p = __tmp;
     * ```
     *
     * The [ExpressionList] contains:
     * 1. A [DeclarationStatement] with an implicit temporary variable: `__tmp = new Point(1)`
     * 2. Implicit [Assign] statements for each member: `__tmp.X = 0`, `__tmp.Y = 1`
     * 3. A [Reference] to the temporary variable
     *
     * C# spec:
     * [Object initializers](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#1281722-object-initializers)
     */
    private fun handleObjectInitializer(
        initializer: Csharp.AST.InitializerExpressionSyntax,
        newExpression: New,
        type: de.fraunhofer.aisec.cpg.graph.types.Type,
    ): ExpressionList {
        val exprList = newExpressionList()
        // Create an implicit temporary variable to hold the new object
        val tmpName = Name.temporary(prefix = type.name.localName, separatorChar = '_', exprList)
        val tmpVar = newVariable(name = tmpName, type = type).implicit()
        tmpVar.initializer = newExpression
        frontend.scopeManager.addDeclaration(tmpVar)
        val declStmt = newDeclarationStatement().implicit()
        declStmt.addDeclaration(tmpVar)
        exprList.expressions += declStmt

        // Each assignment in the initializer is translated to a member access on the tmp variable
        for (expr in initializer.expressions) {
            if (expr is Csharp.AST.AssignmentExpressionSyntax) {
                val memberAccess =
                    newMemberAccess(
                            name =
                                (expr.left as? Csharp.AST.IdentifierNameSyntax)?.identifier
                                    ?: "obj",
                            base = newReference(name = tmpName).implicit(),
                        )
                        .implicit(code = newExpression.code, location = newExpression.location)
                val assign =
                    newAssign(
                            operatorCode = "=",
                            lhs = listOf(memberAccess),
                            rhs = listOf(handle(expr.right)),
                        )
                        .implicit(code = newExpression.code, location = newExpression.location)
                exprList.expressions += assign
            }
        }

        // Add a reference to the temporary variable
        exprList.expressions += newReference(name = tmpName).implicit()
        return exprList
    }

    //    /**
    //     * Translates an [InitializerExpressionSyntax][Csharp.AST.InitializerExpressionSyntax]
    // into an
    //     * [InitializerList].
    //     *
    //     * C# spec:
    //     * [Object
    // initializers](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#1281722-object-initializers)
    //     */
    //    // For arrays
    //    private fun handleInitializerExpression(
    //        node: Csharp.AST.InitializerExpressionSyntax
    //    ): InitializerList {
    //        val initializerList = newInitializerList(rawNode = node)
    //        for (expr in node.expressions) {
    //            initializerList.addArgument(handle(expr))
    //        }
    //        return initializerList
    //    }
}
