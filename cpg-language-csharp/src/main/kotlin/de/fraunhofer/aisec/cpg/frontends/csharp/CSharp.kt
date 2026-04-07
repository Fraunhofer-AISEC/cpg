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

import com.sun.jna.FromNativeContext
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.PointerType
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException

/** This interface encapsulates C# <-> Kotlin translation objects. */
interface Csharp : Library {
    /** Base class for all JNA pointer wrappers in this interface. */
    open class CsharpObject(p: Pointer? = Pointer.NULL) : PointerType(p) {
        val csharpType: String
            get() {
                return INSTANCE.GetType(this.pointer)
            }
    }

    /**
     * This interface represents the `Microsoft.CodeAnalysis.CSharp.Syntax` namespace in Roslyn. It
     * contains classes representing the syntax nodes of the C# AST as returned by Roslyn's parser.
     */
    interface AST {
        /**
         * Base class for all C# syntax nodes. Represents Roslyn's
         * [`CSharpSyntaxNode`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.csharpsyntaxnode)
         */
        open class Node(p: Pointer? = Pointer.NULL) : CsharpObject(p) {
            val startLine: Int by lazy { INSTANCE.GetNodeStartLine(this) }
            val startColumn: Int by lazy { INSTANCE.GetNodeStartColumn(this) }
            val endLine: Int by lazy { INSTANCE.GetNodeEndLine(this) }
            val endColumn: Int by lazy { INSTANCE.GetNodeEndColumn(this) }
        }

        /**
         * Represents the Roslyn
         * [`TypeSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.typesyntax)
         * class.
         */
        open class TypeSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val name: String by lazy { INSTANCE.GetTypeName(this) }

            override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any {
                if (nativeValue !is Pointer) {
                    return super.fromNative(nativeValue, context)
                }
                return when (INSTANCE.GetType(nativeValue)) {
                    "ArrayTypeSyntax" -> ArrayTypeSyntax(nativeValue)
                    else -> TypeSyntax(nativeValue)
                }
            }
        }

        /**
         * Represents the Roslyn
         * [`ArrayTypeSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.arraytypesyntax)
         * class.
         */
        class ArrayTypeSyntax(p: Pointer? = Pointer.NULL) : TypeSyntax(p) {
            val elementType: TypeSyntax by lazy { INSTANCE.GetArrayTypeElementType(this) }
        }

        /**
         * Represents the Roslyn
         * [`CompilationUnitSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.compilationunitsyntax)
         * class.
         */
        class CompilationUnitSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val members: List<MemberDeclarationSyntax> by lazy {
                val count = INSTANCE.GetCompilationUnitMembersCount(this)
                (0 until count).map { i -> INSTANCE.GetCompilationUnitMember(this, i) }
            }
        }

        /**
         * Represents the Roslyn
         * [MemberDeclarationSyntax](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.memberdeclarationsyntax)
         * class.
         */
        open class MemberDeclarationSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            /**
             * JNA calls this method automatically when converting a native pointer which is
             * returned by a JNA function into a [MemberDeclarationSyntax] object. We override it to
             * return the concrete subtype (e.g. [NamespaceDeclarationSyntax]) based on the Roslyn
             * kind string.
             */
            override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any {
                if (nativeValue !is Pointer) {
                    return super.fromNative(nativeValue, context)
                }
                return when (INSTANCE.GetType(nativeValue)) {
                    "NamespaceDeclarationSyntax",
                    "FileScopedNamespaceDeclarationSyntax" ->
                        NamespaceDeclarationSyntax(nativeValue)
                    "ClassDeclarationSyntax" -> ClassDeclarationSyntax(nativeValue)
                    "FieldDeclarationSyntax" -> FieldDeclarationSyntax(nativeValue)
                    "MethodDeclarationSyntax" -> MethodDeclarationSyntax(nativeValue)
                    "ConstructorDeclarationSyntax" -> ConstructorDeclarationSyntax(nativeValue)
                    else -> super.fromNative(nativeValue, context)
                }
            }
        }

        /**
         * Represents the Roslyn
         * [`NamespaceDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.namespacedeclarationsyntax)
         * class.
         */
        class NamespaceDeclarationSyntax(p: Pointer? = Pointer.NULL) : MemberDeclarationSyntax(p) {
            val name: String by lazy { INSTANCE.GetNamespaceDeclarationName(this) }
            val members: List<MemberDeclarationSyntax> by lazy {
                val count = INSTANCE.GetNamespaceDeclarationMembersCount(this)
                (0 until count).map { i -> INSTANCE.GetNamespaceDeclarationMember(this, i) }
            }
        }

        /**
         * Represents the Roslyn
         * [`ClassDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.classdeclarationsyntax)
         * class.
         */
        class ClassDeclarationSyntax(p: Pointer? = Pointer.NULL) : MemberDeclarationSyntax(p) {
            val identifier: String by lazy { INSTANCE.GetClassDeclarationIdentifier(this) }
            val members: List<MemberDeclarationSyntax> by lazy {
                val count = INSTANCE.GetClassDeclarationMembersCount(this)
                (0 until count).map { i -> INSTANCE.GetClassDeclarationMember(this, i) }
            }
        }

        /**
         * Represents the Roslyn
         * [`BaseMethodDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.basemethoddeclarationsyntax)
         * class.
         */
        open class BaseMethodDeclarationSyntax(p: Pointer? = Pointer.NULL) :
            MemberDeclarationSyntax(p) {
            val parameterList: ParameterListSyntax by lazy {
                INSTANCE.GetBaseMethodDeclarationParameterList(this)
            }

            override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any {
                if (nativeValue !is Pointer) {
                    return super.fromNative(nativeValue, context)
                }
                return when (INSTANCE.GetType(nativeValue)) {
                    else -> super.fromNative(nativeValue, context)
                }
            }
        }

        /**
         * Represents the Roslyn
         * [`MethodDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.methoddeclarationsyntax)
         * class.
         */
        class MethodDeclarationSyntax(p: Pointer? = Pointer.NULL) : BaseMethodDeclarationSyntax(p) {
            val identifier: String by lazy { INSTANCE.GetMethodDeclarationIdentifier(this) }
            val body: BlockSyntax by lazy { INSTANCE.GetBaseMethodDeclarationBody(this) }
        }

        /**
         * Represents the Roslyn
         * [`BlockSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.blocksyntax)
         * class.
         */
        class BlockSyntax(p: Pointer? = Pointer.NULL) : StatementSyntax(p) {
            val statements: List<StatementSyntax> by lazy {
                val count = INSTANCE.GetBlockStatementCount(this)
                (0 until count).map { i -> INSTANCE.GetBlockStatement(this, i) }
            }
        }

        /**
         * Represents the Roslyn
         * [`StatementSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.statementsyntax)
         * class.
         */
        open class StatementSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any {
                if (nativeValue !is Pointer) {
                    return super.fromNative(nativeValue, context)
                }
                return when (INSTANCE.GetType(nativeValue)) {
                    "ReturnStatementSyntax" -> ReturnStatementSyntax(nativeValue)
                    "BlockSyntax" -> BlockSyntax(nativeValue)
                    "IfStatementSyntax" -> IfStatementSyntax(nativeValue)
                    "LocalDeclarationStatementSyntax" ->
                        LocalDeclarationStatementSyntax(nativeValue)
                    "ExpressionStatementSyntax" -> ExpressionStatementSyntax(nativeValue)
                    "WhileStatementSyntax" -> WhileStatementSyntax(nativeValue)
                    "DoStatementSyntax" -> DoStatementSyntax(nativeValue)
                    "ForStatementSyntax" -> ForStatementSyntax(nativeValue)
                    "ForEachStatementSyntax" -> ForEachStatementSyntax(nativeValue)
                    else -> super.fromNative(nativeValue, context)
                }
            }
        }

        /**
         * Represents the Roslyn
         * [`ReturnStatementSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.returnstatementsyntax)
         * class.
         */
        class ReturnStatementSyntax(p: Pointer? = Pointer.NULL) : StatementSyntax(p) {
            val expression: ExpressionSyntax? by lazy {
                INSTANCE.GetReturnStatementExpression(this)
            }
        }

        /**
         * Represents the Roslyn
         * [`IfStatementSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.ifstatementsyntax)
         * class.
         */
        class IfStatementSyntax(p: Pointer? = Pointer.NULL) : StatementSyntax(p) {
            val condition: ExpressionSyntax by lazy { INSTANCE.GetIfStatementSyntaxCondition(this) }
            /** The statement that is executed when the condition is true. */
            val statement: StatementSyntax by lazy { INSTANCE.GetIfStatementSyntaxStatement(this) }
            val elseClause: ElseClauseSyntax? by lazy { INSTANCE.GetElseClauseSyntax(this) }
        }

        /**
         * Represents the Roslyn
         * [`ElseClauseSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.elseclausesyntax)
         * class. Note: not a [StatementSyntax] because it cannot appear on its own. It's always
         * attached to an [IfStatementSyntax].
         */
        class ElseClauseSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val statement: StatementSyntax by lazy { INSTANCE.GetElseClauseSyntaxStatement(this) }
        }

        /**
         * Represents the Roslyn
         * [`WhileStatementSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.whilestatementsyntax)
         * class.
         */
        class WhileStatementSyntax(p: Pointer? = Pointer.NULL) : StatementSyntax(p) {
            val condition: ExpressionSyntax by lazy { INSTANCE.GetWhileStatementCondition(this) }
            val statement: StatementSyntax by lazy { INSTANCE.GetWhileStatementStatement(this) }
        }

        /**
         * Represents the Roslyn
         * [`DoStatementSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.dostatementsyntax)
         * class.
         */
        class DoStatementSyntax(p: Pointer? = Pointer.NULL) : StatementSyntax(p) {
            val condition: ExpressionSyntax by lazy { INSTANCE.GetDoStatementCondition(this) }
            val statement: StatementSyntax by lazy { INSTANCE.GetDoStatementStatement(this) }
        }

        /**
         * Represents the Roslyn
         * [`ForStatementSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.forstatementsyntax)
         * class.
         */
        class ForStatementSyntax(p: Pointer? = Pointer.NULL) : StatementSyntax(p) {
            /** Variable declaration in the initializer (e.g. `int i = 0`) */
            val declaration: VariableDeclarationSyntax? by lazy {
                INSTANCE.GetForStatementDeclaration(this)
            }
            /** Expression initializers (e.g. `i = 0`), used when there is no declaration */
            val initializerExpressions: List<ExpressionSyntax> by lazy {
                val count = INSTANCE.GetForStatementInitializerExpressionCount(this)
                (0 until count).map { i -> INSTANCE.GetForStatementInitializerExpression(this, i) }
            }
            val condition: ExpressionSyntax? by lazy { INSTANCE.GetForStatementCondition(this) }
            /** The incrementors (e.g. `i++, j--`) */
            val incrementors: List<ExpressionSyntax> by lazy {
                val count = INSTANCE.GetForStatementIncrementorCount(this)
                (0 until count).map { i -> INSTANCE.GetForStatementIncrementor(this, i) }
            }
            val statement: StatementSyntax by lazy { INSTANCE.GetForStatementStatement(this) }
        }

        /**
         * Represents the Roslyn
         * [`ForEachStatementSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.foreachstatementsyntax)
         * class.
         */
        class ForEachStatementSyntax(p: Pointer? = Pointer.NULL) : StatementSyntax(p) {
            val identifier: String by lazy { INSTANCE.GetForEachStatementIdentifier(this) }
            val type: TypeSyntax by lazy { INSTANCE.GetForEachStatementType(this) }
            val expression: ExpressionSyntax by lazy {
                INSTANCE.GetForEachStatementExpression(this)
            }
            val statement: StatementSyntax by lazy { INSTANCE.GetForEachStatementStatement(this) }
        }

        /**
         * Represents the Roslyn
         * [`ConstructorDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.constructordeclarationsyntax)
         * class.
         */
        class ConstructorDeclarationSyntax(p: Pointer? = Pointer.NULL) :
            BaseMethodDeclarationSyntax(p) {
            val identifier: String by lazy { INSTANCE.GetConstructorDeclarationIdentifier(this) }
        }

        /**
         * Represents the Roslyn
         * [`FieldDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.fielddeclarationsyntax)
         * class.
         */
        class FieldDeclarationSyntax(p: Pointer? = Pointer.NULL) : MemberDeclarationSyntax(p) {
            val declaration: VariableDeclarationSyntax by lazy {
                INSTANCE.GetFieldDeclaration(this)
            }
        }

        /**
         * Represents the Roslyn
         * [`LocalDeclarationStatementSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.localdeclarationstatementsyntax)
         * class.
         */
        class LocalDeclarationStatementSyntax(p: Pointer? = Pointer.NULL) : StatementSyntax(p) {
            val declaration: VariableDeclarationSyntax by lazy {
                INSTANCE.GetVariableDeclarationSyntax(this)
            }
        }

        /**
         * Represents the Roslyn
         * [`ExpressionStatementSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.expressionstatementsyntax)
         * class.
         */
        class ExpressionStatementSyntax(p: Pointer? = Pointer.NULL) : StatementSyntax(p) {
            val expression: ExpressionSyntax by lazy {
                INSTANCE.GetExpressionStatementExpression(this)
            }
        }

        /**
         * Represents the Roslyn
         * [`VariableDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.variabledeclarationsyntax)
         * class.
         */
        class VariableDeclarationSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val type: TypeSyntax by lazy { INSTANCE.GetVariableDeclarationType(this) }
            val variables: List<VariableDeclaratorSyntax> by lazy {
                val count = INSTANCE.GetLocalVariableCount(this)
                (0 until count).map { i -> INSTANCE.GetLocalVariable(this, i) }
            }
        }

        /**
         * Represents the Roslyn
         * [`VariableDeclaratorSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.variabledeclaratorsyntax)
         * class.
         */
        class VariableDeclaratorSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val identifier: String by lazy { INSTANCE.GetVariableDeclaratorIdentifier(this) }
            val initializer: ExpressionSyntax? by lazy {
                INSTANCE.GetVariableDeclaratorInitializer(this)
            }
        }

        /**
         * Represents the Roslyn
         * [`ExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.expressionsyntax)
         * class.
         */
        open class ExpressionSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any? {
                if (nativeValue !is Pointer) {
                    return super.fromNative(nativeValue, context)
                }
                return when (INSTANCE.GetType(nativeValue)) {
                    // All literals share the same Roslyn type (LiteralExpressionSyntax),
                    // so we need to use 'GetKind' to get the underlying type.
                    "LiteralExpressionSyntax" ->
                        when (INSTANCE.GetKind(nativeValue)) {
                            "NumericLiteralExpression" ->
                                NumericLiteralExpressionSyntax(nativeValue)
                            "StringLiteralExpression" -> StringLiteralExpressionSyntax(nativeValue)
                            "TrueLiteralExpression",
                            "FalseLiteralExpression" -> BooleanLiteralExpressionSyntax(nativeValue)
                            "CharacterLiteralExpression" ->
                                CharacterLiteralExpressionSyntax(nativeValue)
                            "NullLiteralExpression" -> NullLiteralExpressionSyntax(nativeValue)
                            else -> LiteralExpressionSyntax(nativeValue)
                        }
                    "BinaryExpressionSyntax" -> BinaryExpressionSyntax(nativeValue)
                    "IdentifierNameSyntax" -> IdentifierNameSyntax(nativeValue)
                    "AssignmentExpressionSyntax" -> AssignmentExpressionSyntax(nativeValue)
                    "MemberAccessExpressionSyntax" -> MemberAccessExpressionSyntax(nativeValue)
                    "InvocationExpressionSyntax" -> InvocationExpressionSyntax(nativeValue)
                    "ThisExpressionSyntax" -> ThisExpressionSyntax(nativeValue)
                    "ObjectCreationExpressionSyntax" -> ObjectCreationExpressionSyntax(nativeValue)
                    "ImplicitObjectCreationExpressionSyntax" ->
                        ImplicitObjectCreationExpressionSyntax(nativeValue)
                    "InitializerExpressionSyntax" -> InitializerExpressionSyntax(nativeValue)
                    else -> super.fromNative(nativeValue, context)
                }
            }
        }

        /**
         * Represents the Roslyn
         * [`LiteralExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.literalexpressionsyntax)
         * class.
         */
        open class LiteralExpressionSyntax(p: Pointer? = Pointer.NULL) : ExpressionSyntax(p) {
            val value: String by lazy { INSTANCE.GetLiteralExpressionValue(this) }
        }

        class NumericLiteralExpressionSyntax(p: Pointer? = Pointer.NULL) :
            LiteralExpressionSyntax(p)

        class StringLiteralExpressionSyntax(p: Pointer? = Pointer.NULL) :
            LiteralExpressionSyntax(p)

        class BooleanLiteralExpressionSyntax(p: Pointer? = Pointer.NULL) :
            LiteralExpressionSyntax(p)

        class CharacterLiteralExpressionSyntax(p: Pointer? = Pointer.NULL) :
            LiteralExpressionSyntax(p)

        class NullLiteralExpressionSyntax(p: Pointer? = Pointer.NULL) : LiteralExpressionSyntax(p)

        /**
         * Represents the Roslyn
         * [`BinaryExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.binaryexpressionsyntax)
         * class.
         */
        class BinaryExpressionSyntax(p: Pointer? = Pointer.NULL) : ExpressionSyntax(p) {
            val left: ExpressionSyntax by lazy { INSTANCE.GetBinaryExpressionLeft(this) }
            val operatorToken: String by lazy { INSTANCE.GetBinaryExpressionOperator(this) }
            val right: ExpressionSyntax by lazy { INSTANCE.GetBinaryExpressionRight(this) }
        }

        /**
         * Represents the Roslyn
         * [`AssignmentExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.assignmentexpressionsyntax)
         * class.
         */
        class AssignmentExpressionSyntax(p: Pointer? = Pointer.NULL) : ExpressionSyntax(p) {
            val left: ExpressionSyntax by lazy { INSTANCE.GetAssignmentExpressionLeft(this) }
            val operatorToken: String by lazy { INSTANCE.GetAssignmentExpressionOperator(this) }
            val right: ExpressionSyntax by lazy { INSTANCE.GetAssignmentExpressionRight(this) }
        }

        /**
         * Represents the Roslyn
         * [`IdentifierNameSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.identifiernamesyntax)
         * class.
         */
        class IdentifierNameSyntax(p: Pointer? = Pointer.NULL) : ExpressionSyntax(p) {
            val identifier: String by lazy { INSTANCE.GetIdentifierNameSyntaxIdentifier(this) }
        }

        /**
         * Represents the Roslyn
         * [`ThisExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.thisexpressionsyntax)
         * class.
         */
        class ThisExpressionSyntax(p: Pointer? = Pointer.NULL) : ExpressionSyntax(p)

        /**
         * Represents the Roslyn
         * [`InvocationExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.invocationexpressionsyntax)
         * class.
         */
        class InvocationExpressionSyntax(p: Pointer? = Pointer.NULL) : ExpressionSyntax(p) {
            val expression: ExpressionSyntax by lazy {
                INSTANCE.GetInvocationExpressionExpression(this)
            }
            val argumentList: ArgumentListSyntax by lazy {
                INSTANCE.GetInvocationExpressionArgumentList(this)
            }
        }

        /**
         * Represents the Roslyn
         * [`ArgumentListSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.argumentlistsyntax)
         * class.
         */
        class ArgumentListSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val arguments: List<ArgumentSyntax> by lazy {
                val count = INSTANCE.GetArgumentListCount(this)
                (0 until count).map { i -> INSTANCE.GetArgumentListArgument(this, i) }
            }
        }

        /**
         * Represents the Roslyn
         * [`ArgumentSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.argumentsyntax)
         * class.
         */
        class ArgumentSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val expression: ExpressionSyntax by lazy { INSTANCE.GetArgumentExpression(this) }
        }

        /**
         * Represents the Roslyn
         * [`ParameterListSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.parameterlistsyntax)
         * class.
         */
        class ParameterListSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val parameters: List<ParameterSyntax> by lazy {
                val count = INSTANCE.GetParameterListCount(this)
                (0 until count).map { i -> INSTANCE.GetParameterListParameter(this, i) }
            }
        }

        /**
         * Represents the Roslyn
         * [`ParameterSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.parametersyntax)
         * class.
         */
        class ParameterSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val identifier: String by lazy { INSTANCE.GetParameterIdentifier(this) }
            val type: TypeSyntax by lazy { INSTANCE.GetParameterType(this) }
        }

        /**
         * Represents the Roslyn
         * [`MemberAccessExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.memberaccessexpressionsyntax)
         * class.
         */
        class MemberAccessExpressionSyntax(p: Pointer? = Pointer.NULL) : ExpressionSyntax(p) {
            val expression: ExpressionSyntax by lazy {
                INSTANCE.GetMemberAccessExpressionExpression(this)
            }
            val name: String by lazy { INSTANCE.GetMemberAccessExpressionName(this) }
            val operatorToken: String by lazy {
                INSTANCE.GetMemberAccessExpressionOperatorToken(this)
            }
        }

        /**
         * Represents the Roslyn
         * [`BaseObjectCreationExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.baseobjectcreationexpressionsyntax)
         * class. This is the common base for [ObjectCreationExpressionSyntax] (explicit type, e.g.
         * `new Foo()`) and [ImplicitObjectCreationExpressionSyntax] (target-typed, e.g. `new()`).
         */
        open class BaseObjectCreationExpressionSyntax(p: Pointer? = Pointer.NULL) :
            ExpressionSyntax(p) {
            val argumentList: ArgumentListSyntax? by lazy {
                INSTANCE.GetBaseObjectCreationExpressionArgumentList(this)
            }
            val initializer: InitializerExpressionSyntax? by lazy {
                INSTANCE.GetBaseObjectCreationExpressionInitializer(this)
            }
        }

        /**
         * Represents the Roslyn
         * [`ObjectCreationExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.objectcreationexpressionsyntax)
         * class.
         */
        class ObjectCreationExpressionSyntax(p: Pointer? = Pointer.NULL) :
            BaseObjectCreationExpressionSyntax(p) {
            val type: TypeSyntax by lazy { INSTANCE.GetObjectCreationExpressionType(this) }
        }

        class ImplicitObjectCreationExpressionSyntax(p: Pointer? = Pointer.NULL) :
            BaseObjectCreationExpressionSyntax(p)

        /**
         * Represents the Roslyn
         * [`InitializerExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.initializerexpressionsyntax)
         * class.
         */
        class InitializerExpressionSyntax(p: Pointer? = Pointer.NULL) : ExpressionSyntax(p) {
            val expressions: List<ExpressionSyntax> by lazy {
                val count = INSTANCE.GetInitializerExpressionExpressionsCount(this)
                (0 until count).map { i -> INSTANCE.GetInitializerExpressionExpression(this, i) }
            }
        }
    }

    /**
     * Represents the Roslyn `CSharpSyntaxTree` class.
     *
     * See: TODO(link)
     */
    object CSharpSyntaxTree {
        fun parseText(source: String): AST.CompilationUnitSyntax {
            return AST.CompilationUnitSyntax(INSTANCE.CSharpRoslynSyntaxTreeParseText(source))
        }
    }

    fun CSharpRoslynSyntaxTreeParseText(source: String): Pointer

    fun GetKind(handle: Pointer): String

    fun GetType(handle: Pointer): String

    fun GetCompilationUnitMembersCount(handle: AST.CompilationUnitSyntax): Int

    fun GetCompilationUnitMember(
        handle: AST.CompilationUnitSyntax,
        index: Int,
    ): AST.MemberDeclarationSyntax

    fun GetNamespaceDeclarationName(handle: AST.NamespaceDeclarationSyntax): String

    fun GetNamespaceDeclarationMembersCount(handle: AST.NamespaceDeclarationSyntax): Int

    fun GetNamespaceDeclarationMember(
        handle: AST.NamespaceDeclarationSyntax,
        index: Int,
    ): AST.MemberDeclarationSyntax

    fun GetClassDeclarationIdentifier(handle: AST.ClassDeclarationSyntax): String

    fun GetClassDeclarationMembersCount(handle: AST.ClassDeclarationSyntax): Int

    fun GetClassDeclarationMember(
        handle: AST.ClassDeclarationSyntax,
        index: Int,
    ): AST.MemberDeclarationSyntax

    fun GetFieldDeclaration(handle: AST.FieldDeclarationSyntax): AST.VariableDeclarationSyntax

    fun GetVariableDeclaratorIdentifier(handle: AST.VariableDeclaratorSyntax): String

    fun GetMethodDeclarationIdentifier(handle: AST.MethodDeclarationSyntax): String

    fun GetBaseMethodDeclarationParameterList(
        handle: AST.BaseMethodDeclarationSyntax
    ): AST.ParameterListSyntax

    fun GetParameterListCount(handle: AST.ParameterListSyntax): Int

    fun GetParameterListParameter(handle: AST.ParameterListSyntax, index: Int): AST.ParameterSyntax

    fun GetParameterIdentifier(handle: AST.ParameterSyntax): String

    fun GetParameterType(handle: AST.ParameterSyntax): AST.TypeSyntax

    fun GetTypeName(handle: AST.TypeSyntax): String

    fun GetArrayTypeElementType(handle: AST.ArrayTypeSyntax): AST.TypeSyntax

    fun GetConstructorDeclarationIdentifier(handle: AST.ConstructorDeclarationSyntax): String

    fun GetBaseMethodDeclarationBody(handle: AST.MethodDeclarationSyntax): AST.BlockSyntax

    fun GetBlockStatementCount(handle: AST.StatementSyntax): Int

    fun GetBlockStatement(handle: AST.StatementSyntax, index: Int): AST.StatementSyntax

    fun GetLiteralExpressionValue(handle: AST.LiteralExpressionSyntax): String

    fun GetReturnStatementExpression(handle: AST.StatementSyntax): AST.ExpressionSyntax?

    fun GetIfStatementSyntaxCondition(handle: AST.StatementSyntax): AST.ExpressionSyntax

    fun GetBinaryExpressionLeft(handle: AST.BinaryExpressionSyntax): AST.ExpressionSyntax

    fun GetBinaryExpressionOperator(handle: AST.BinaryExpressionSyntax): String

    fun GetBinaryExpressionRight(handle: AST.BinaryExpressionSyntax): AST.ExpressionSyntax

    fun GetCode(handle: AST.Node): String

    fun GetNodeStartLine(handle: AST.Node): Int

    fun GetNodeStartColumn(handle: AST.Node): Int

    fun GetNodeEndLine(handle: AST.Node): Int

    fun GetNodeEndColumn(handle: AST.Node): Int

    fun GetIdentifierNameSyntaxIdentifier(handle: AST.IdentifierNameSyntax): String

    fun GetIfStatementSyntaxStatement(handle: AST.IfStatementSyntax): AST.StatementSyntax

    fun GetElseClauseSyntax(handle: AST.IfStatementSyntax): AST.ElseClauseSyntax?

    fun GetElseClauseSyntaxStatement(handle: AST.ElseClauseSyntax): AST.StatementSyntax

    fun GetLocalVariableCount(handle: AST.VariableDeclarationSyntax): Int

    fun GetLocalVariable(
        handle: AST.VariableDeclarationSyntax,
        index: Int,
    ): AST.VariableDeclaratorSyntax

    fun GetVariableDeclarationSyntax(
        handle: AST.LocalDeclarationStatementSyntax
    ): AST.VariableDeclarationSyntax

    fun GetVariableDeclarationType(handle: AST.VariableDeclarationSyntax): AST.TypeSyntax

    fun GetVariableDeclaratorInitializer(
        handle: AST.VariableDeclaratorSyntax
    ): AST.ExpressionSyntax?

    fun GetExpressionStatementExpression(
        handle: AST.ExpressionStatementSyntax
    ): AST.ExpressionSyntax

    fun GetAssignmentExpressionLeft(handle: AST.AssignmentExpressionSyntax): AST.ExpressionSyntax

    fun GetAssignmentExpressionRight(handle: AST.AssignmentExpressionSyntax): AST.ExpressionSyntax

    fun GetAssignmentExpressionOperator(handle: AST.AssignmentExpressionSyntax): String

    fun GetWhileStatementCondition(handle: AST.WhileStatementSyntax): AST.ExpressionSyntax

    fun GetWhileStatementStatement(handle: AST.WhileStatementSyntax): AST.StatementSyntax

    fun GetDoStatementCondition(handle: AST.DoStatementSyntax): AST.ExpressionSyntax

    fun GetDoStatementStatement(handle: AST.DoStatementSyntax): AST.StatementSyntax

    fun GetForStatementDeclaration(handle: AST.ForStatementSyntax): AST.VariableDeclarationSyntax?

    fun GetForStatementInitializerExpressionCount(handle: AST.ForStatementSyntax): Int

    fun GetForStatementInitializerExpression(
        handle: AST.ForStatementSyntax,
        index: Int,
    ): AST.ExpressionSyntax

    fun GetForStatementCondition(handle: AST.ForStatementSyntax): AST.ExpressionSyntax?

    fun GetForStatementIncrementorCount(handle: AST.ForStatementSyntax): Int

    fun GetForStatementIncrementor(handle: AST.ForStatementSyntax, index: Int): AST.ExpressionSyntax

    fun GetForStatementStatement(handle: AST.ForStatementSyntax): AST.StatementSyntax

    fun GetForEachStatementIdentifier(handle: AST.ForEachStatementSyntax): String

    fun GetForEachStatementType(handle: AST.ForEachStatementSyntax): AST.TypeSyntax

    fun GetForEachStatementExpression(handle: AST.ForEachStatementSyntax): AST.ExpressionSyntax

    fun GetForEachStatementStatement(handle: AST.ForEachStatementSyntax): AST.StatementSyntax

    fun GetInvocationExpressionExpression(
        handle: AST.InvocationExpressionSyntax
    ): AST.ExpressionSyntax

    fun GetInvocationExpressionArgumentList(
        handle: AST.InvocationExpressionSyntax
    ): AST.ArgumentListSyntax

    fun GetArgumentListCount(handle: AST.ArgumentListSyntax): Int

    fun GetArgumentListArgument(handle: AST.ArgumentListSyntax, index: Int): AST.ArgumentSyntax

    fun GetArgumentExpression(handle: AST.ArgumentSyntax): AST.ExpressionSyntax

    fun GetMemberAccessExpressionExpression(
        handle: AST.MemberAccessExpressionSyntax
    ): AST.ExpressionSyntax

    fun GetMemberAccessExpressionName(handle: AST.MemberAccessExpressionSyntax): String

    fun GetMemberAccessExpressionOperatorToken(handle: AST.MemberAccessExpressionSyntax): String

    fun GetObjectCreationExpressionType(handle: AST.ObjectCreationExpressionSyntax): AST.TypeSyntax

    fun GetBaseObjectCreationExpressionArgumentList(
        handle: AST.BaseObjectCreationExpressionSyntax
    ): AST.ArgumentListSyntax?

    fun GetBaseObjectCreationExpressionInitializer(
        handle: AST.BaseObjectCreationExpressionSyntax
    ): AST.InitializerExpressionSyntax?

    fun GetInitializerExpressionExpressionsCount(handle: AST.InitializerExpressionSyntax): Int

    fun GetInitializerExpressionExpression(
        handle: AST.InitializerExpressionSyntax,
        index: Int,
    ): AST.ExpressionSyntax

    companion object {
        val INSTANCE: Csharp by lazy {
            try {
                val osName = System.getProperty("os.name")
                val os = if (osName.startsWith("Mac")) "osx" else "linux"
                val arch =
                    System.getProperty("os.arch")
                        .replace("aarch64", "arm64")
                        .replace("amd64", "x64")
                val rid = "$os-$arch"
                val ext = if (osName.startsWith("Mac")) ".dylib" else ".so"

                val dylib =
                    java.io.File(
                        "src/main/csharp/NativeParser/bin/Release/net8.0/$rid/publish/NativeParser$ext"
                    )

                LanguageFrontend.log.info("Loading NativeParser from ${dylib.absolutePath}")

                Native.load(dylib.absolutePath, Csharp::class.java)
            } catch (ex: Exception) {
                throw TranslationException("Error loading C# native library: $ex")
            }
        }
    }
}
