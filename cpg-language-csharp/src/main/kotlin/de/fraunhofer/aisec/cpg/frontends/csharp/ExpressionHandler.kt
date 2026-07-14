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
import de.fraunhofer.aisec.cpg.graph.newUnaryOperator
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
            is Csharp.AST.PrefixUnaryExpressionSyntax -> handlePrefixUnaryExpression(node)
            is Csharp.AST.PostfixUnaryExpressionSyntax -> handlePostfixUnaryExpression(node)
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
     * Translates a [PrefixUnaryExpressionSyntax][Csharp.AST.PrefixUnaryExpressionSyntax] (e.g.
     * `++a`) into a [UnaryOperator].
     *
     * C# spec:
     * [Unary operators](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#129-unary-operators)
     */
    private fun handlePrefixUnaryExpression(
        node: Csharp.AST.PrefixUnaryExpressionSyntax
    ): UnaryOperator {
        return newUnaryOperator(
                operatorCode = node.operatorToken,
                postfix = false,
                prefix = true,
                rawNode = node,
            )
            .apply { this.input = handle(node.operand) }
    }

    /**
     * Translates a [PostfixUnaryExpressionSyntax][Csharp.AST.PostfixUnaryExpressionSyntax] (e.g.
     * `a++`, `a--`) into a [UnaryOperator].
     *
     * C# spec:
     * [Postfix increment and decrement operators](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#12816-postfix-increment-and-decrement-operators)
     */
    private fun handlePostfixUnaryExpression(
        node: Csharp.AST.PostfixUnaryExpressionSyntax
    ): UnaryOperator {
        return newUnaryOperator(
                operatorCode = node.operatorToken,
                postfix = true,
                prefix = false,
                rawNode = node,
            )
            .apply { this.input = handle(node.operand) }
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
     * [Call]. If the callee is a [MemberAccess] (e.g. `member.Method()`), a `MemberCall` is
     * created. Otherwise (e.g. `Foo()`), a [Call] is created.
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
            return when (initializer) {
                is Csharp.AST.ObjectInitializerExpressionSyntax ->
                    handleObjectInitializer(initializer, newExpression, type)
                is Csharp.AST.CollectionInitializerExpressionSyntax ->
                    handleCollectionInitializer(initializer, newExpression, type)
                else -> newExpression
            }
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
        val declStmt = newDeclarationStatement().implicit()
        declStmt.addDeclaration(tmpVar)
        exprList.expressions += declStmt

        for (expr in initializer.expressions) {
            if (expr is Csharp.AST.AssignmentExpressionSyntax) {
                val memberName = (expr.left as? Csharp.AST.IdentifierNameSyntax)?.identifier
                val baseRef = newReference(name = tmpName).implicit()
                baseRef.refersTo = tmpVar
                val memberAccess =
                    newMemberAccess(name = memberName, base = baseRef)
                        .implicit(code = newExpression.code, location = newExpression.location)

                if (expr.right is Csharp.AST.ObjectInitializerExpressionSyntax) {
                    // Nested object initializer: member = { X = 0, Y = 1 }
                    // -> __tmp.member.X = 0, __tmp.member.Y = 1
                    exprList.expressions +=
                        handleNestedObjectInitializer(
                            expr.right as Csharp.AST.ObjectInitializerExpressionSyntax,
                            memberAccess,
                            newExpression,
                        )
                } else if (expr.right is Csharp.AST.CollectionInitializerExpressionSyntax) {
                    // Nested collection initializer: member = { "a", "b" }
                    // -> __tmp.member.Add("a"), __tmp.member.Add("b")
                    exprList.expressions +=
                        handleNestedCollectionInitializer(
                            expr.right as Csharp.AST.CollectionInitializerExpressionSyntax,
                            memberAccess,
                            newExpression,
                        )
                } else {
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
        }

        // Add a reference to the temporary variable
        exprList.expressions +=
            newReference(name = tmpName).implicit().apply { this.refersTo = tmpVar }
        return exprList
    }

    /**
     * Handles a nested object initializer where no new object is created. Instead, the members are
     * only initialized. For example:
     * ```csharp
     * Rectangle r = new Rectangle { P1 = { X = 0, Y = 1 } };
     * ```
     *
     * is equivalent to:
     * ```csharp
     * Rectangle __tmp = new Rectangle();
     * __tmp.P1.X = 0;
     * __tmp.P1.Y = 1;
     * Rectangle r = __tmp;
     * ```
     *
     * C# spec:
     * [Object initializers](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#1281722-object-initializers)
     */
    private fun handleNestedObjectInitializer(
        initializer: Csharp.AST.ObjectInitializerExpressionSyntax,
        memberAccess: MemberAccess,
        newExpression: New,
    ): List<Assign> {
        val assigns = mutableListOf<Assign>()
        for (innerExpr in initializer.expressions) {
            if (innerExpr is Csharp.AST.AssignmentExpressionSyntax) {
                val innerName = (innerExpr.left as? Csharp.AST.IdentifierNameSyntax)?.identifier
                val innerAccess =
                    newMemberAccess(name = innerName, base = memberAccess)
                        .implicit(code = newExpression.code, location = newExpression.location)
                val assign =
                    newAssign(
                            operatorCode = "=",
                            lhs = listOf(innerAccess),
                            rhs = listOf(handle(innerExpr.right)),
                        )
                        .implicit(code = newExpression.code, location = newExpression.location)
                assigns += assign
            }
        }
        return assigns
    }

    /**
     * Handles a [Csharp.AST.CollectionInitializerExpressionSyntax] by wrapping the expressions in
     * an [ExpressionList]. Each element in the initializer is translated into an implicit `Add`
     * call on a temporary variable. For example:
     * ```csharp
     * var list = new List<int> { 0, 1, 2 };
     * ```
     *
     * is equivalent to:
     * ```csharp
     * List<int> __tmp = new List<int>();
     * __tmp.Add(0);
     * __tmp.Add(1);
     * __tmp.Add(2);
     * var list = __tmp;
     * ```
     *
     * For [Csharp.AST.ComplexElementInitializerExpressionSyntax], each `{ key, value }` element is
     * translated into an `Add(key, value)` call:
     * ```csharp
     * var map = new Map<string, int> { { "a", 1 }, { "b", 2 } };
     * ```
     *
     * is equivalent to:
     * ```csharp
     * Map<string, int> __tmp = new Map<string, int>();
     * __tmp.Add("a", 1);
     * __tmp.Add("b", 2);
     * var map = __tmp;
     * ```
     *
     * C# spec:
     * [Collection initializers](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#1281723-collection-initializers)
     */
    private fun handleCollectionInitializer(
        initializer: Csharp.AST.CollectionInitializerExpressionSyntax,
        newExpression: New,
        type: de.fraunhofer.aisec.cpg.graph.types.Type,
    ): ExpressionList {
        val exprList = newExpressionList()
        val tmpName = Name.temporary(prefix = type.name.localName, separatorChar = '_', exprList)
        val tmpVar = newVariable(name = tmpName, type = type).implicit()
        tmpVar.initializer = newExpression
        val declStmt = newDeclarationStatement().implicit()
        declStmt.addDeclaration(tmpVar)
        exprList.expressions += declStmt

        for (expr in initializer.expressions) {
            val baseRef = newReference(name = tmpName).implicit()
            baseRef.refersTo = tmpVar
            val addCall =
                newMemberCall(
                        newMemberAccess(name = "Add", base = baseRef)
                            .implicit(code = newExpression.code, location = newExpression.location)
                    )
                    .implicit(code = newExpression.code, location = newExpression.location)
            if (expr is Csharp.AST.ComplexElementInitializerExpressionSyntax) {
                // For example { "a", 1 } -> Add("a", 1)
                for (arg in expr.expressions) {
                    addCall.addArgument(handle(arg))
                }
            } else {
                // Simple element, e.g. 0 -> Add(0)
                addCall.addArgument(handle(expr))
            }
            exprList.expressions += addCall
        }

        exprList.expressions +=
            newReference(name = tmpName).implicit().apply { this.refersTo = tmpVar }
        return exprList
    }

    /**
     * Handles a nested collection initializer where elements are added to an already existing
     * member via implicit `Add` calls. For example:
     * ```csharp
     * new Foo { Items = { 0, 1, 2 } }
     * ```
     *
     * is equivalent to:
     * ```csharp
     * __tmp.Items.Add(0);
     * __tmp.Items.Add(1);
     * __tmp.Items.Add(2);
     * ```
     *
     * C# spec:
     * [Collection initializers](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#1281723-collection-initializers)
     */
    private fun handleNestedCollectionInitializer(
        initializer: Csharp.AST.CollectionInitializerExpressionSyntax,
        memberAccess: MemberAccess,
        newExpression: New,
    ): List<MemberCall> {
        val calls = mutableListOf<MemberCall>()
        for (expr in initializer.expressions) {
            val addCall =
                newMemberCall(
                        newMemberAccess(name = "Add", base = memberAccess)
                            .implicit(code = newExpression.code, location = newExpression.location)
                    )
                    .implicit(code = newExpression.code, location = newExpression.location)
            if (expr is Csharp.AST.ComplexElementInitializerExpressionSyntax) {
                for (arg in expr.expressions) {
                    addCall.addArgument(handle(arg))
                }
            } else {
                addCall.addArgument(handle(expr))
            }
            calls += addCall
        }
        return calls
    }
}
