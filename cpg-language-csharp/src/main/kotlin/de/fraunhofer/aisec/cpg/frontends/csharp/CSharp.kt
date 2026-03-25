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
         * [`CSharpSyntaxNode`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.csharpsyntaxnode?view=roslyn-dotnet-5.0.0)
         */
        open class Node(p: Pointer? = Pointer.NULL) : CsharpObject(p) {
            val startLine: Int by lazy { INSTANCE.GetNodeStartLine(this) }
            val startColumn: Int by lazy { INSTANCE.GetNodeStartColumn(this) }
            val endLine: Int by lazy { INSTANCE.GetNodeEndLine(this) }
            val endColumn: Int by lazy { INSTANCE.GetNodeEndColumn(this) }
        }

        /**
         * Represents the Roslyn
         * [`TypeSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.typesyntax?view=roslyn-dotnet-5.0.0)
         * class.
         */
        class TypeSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val name: String by lazy { INSTANCE.GetTypeName(this) }
        }

        /**
         * Represents the Roslyn
         * [`CompilationUnitSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.compilationunitsyntax?view=roslyn-dotnet-5.0.0)
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
         * [MemberDeclarationSyntax](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.memberdeclarationsyntax?view=roslyn-dotnet-5.0.0)
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
         * [`NamespaceDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.namespacedeclarationsyntax?view=roslyn-dotnet-5.0.0)
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
         * [`ClassDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.classdeclarationsyntax?view=roslyn-dotnet-5.0.0)
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
         * [`BaseMethodDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.basemethoddeclarationsyntax?view=roslyn-dotnet-5.0.0)
         * class.
         */
        open class BaseMethodDeclarationSyntax(p: Pointer? = Pointer.NULL) :
            MemberDeclarationSyntax(p) {
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
         * [`MethodDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.methoddeclarationsyntax?view=roslyn-dotnet-5.0.0)
         * class.
         */
        class MethodDeclarationSyntax(p: Pointer? = Pointer.NULL) : BaseMethodDeclarationSyntax(p) {
            val identifier: String by lazy { INSTANCE.GetMethodDeclarationIdentifier(this) }
            val parameters: List<ParameterSyntax> by lazy {
                val count = INSTANCE.GetMethodDeclarationParameterCount(this)
                (0 until count).map { i -> INSTANCE.GetMethodDeclarationParameter(this, i) }
            }
            val body: BlockSyntax by lazy { INSTANCE.GetBaseMethodDeclarationBody(this) }
        }

        /**
         * Represents the Roslyn
         * [`BlockSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.blocksyntax?view=roslyn-dotnet-5.0.0)
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
         * [`StatementSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.statementsyntax?view=roslyn-dotnet-5.0.0)
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
                    else -> super.fromNative(nativeValue, context)
                }
            }
        }

        /**
         * Represents the Roslyn
         * [`ReturnStatementSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.returnstatementsyntax?view=roslyn-dotnet-5.0.0)
         * class.
         */
        class ReturnStatementSyntax(p: Pointer? = Pointer.NULL) : StatementSyntax(p) {
            val expression: ExpressionSyntax? by lazy {
                INSTANCE.GetReturnStatementExpression(this)
            }
        }

        /**
         * Represents the Roslyn
         * [`IfStatementSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.ifstatementsyntax?view=roslyn-dotnet-5.0.0)
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
         * [`ElseClauseSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.elseclausesyntax?view=roslyn-dotnet-5.0.0)
         * class. Note: not a [StatementSyntax] because it cannot appear on its own. It's always
         * attached to an [IfStatementSyntax].
         */
        class ElseClauseSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val statement: StatementSyntax by lazy { INSTANCE.GetElseClauseSyntaxStatement(this) }
        }

        /**
         * Represents the Roslyn
         * [`ConstructorDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.constructordeclarationsyntax?view=roslyn-dotnet-5.0.0)
         * class.
         */
        class ConstructorDeclarationSyntax(p: Pointer? = Pointer.NULL) :
            BaseMethodDeclarationSyntax(p) {
            val identifier: String by lazy { INSTANCE.GetConstructorDeclarationIdentifier(this) }
            val parameters: List<ParameterSyntax> by lazy {
                val count = INSTANCE.GetConstructorDeclarationParameterCount(this)
                (0 until count).map { i -> INSTANCE.GetConstructorDeclarationParameter(this, i) }
            }
            //            val body: StatementSyntax by lazy { StatementSyntax(p) }
        }

        /**
         * Represents the Roslyn
         * [`FieldDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.fielddeclarationsyntax?view=roslyn-dotnet-5.0.0)
         * class.
         */
        class FieldDeclarationSyntax(p: Pointer? = Pointer.NULL) : MemberDeclarationSyntax(p) {
            val variables: List<VariableDeclaratorSyntax> by lazy {
                val count = INSTANCE.GetFieldVariableCount(this)
                (0 until count).map { i -> INSTANCE.GetFieldVariable(this, i) }
            }
        }

        /**
         * Represents the Roslyn
         * [`VariableDeclaratorSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.variabledeclaratorsyntax?view=roslyn-dotnet-5.0.0)
         * class.
         */
        class VariableDeclaratorSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val identifier: String by lazy { INSTANCE.GetVariableDeclaratorIdentifier(this) }
        }

        /**
         * Represents the Roslyn
         * [`ParameterSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.parametersyntax?view=roslyn-dotnet-5.0.0)
         * class.
         */
        class ParameterSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val identifier: String by lazy { INSTANCE.GetParameterIdentifier(this) }
            val type: TypeSyntax by lazy { INSTANCE.GetParameterType(this) }
        }

        /**
         * Represents the Roslyn
         * [`ExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.expressionsyntax?view=roslyn-dotnet-5.0.0)
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
                    else -> super.fromNative(nativeValue, context)
                }
            }
        }

        /**
         * Represents the Roslyn
         * [`LiteralExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.literalexpressionsyntax?view=roslyn-dotnet-5.0.0)
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
         * [`BinaryExpressionSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.binaryexpressionsyntax?view=roslyn-dotnet-5.0.0)
         * class.
         */
        class BinaryExpressionSyntax(p: Pointer? = Pointer.NULL) : ExpressionSyntax(p) {
            val left: ExpressionSyntax by lazy { INSTANCE.GetBinaryExpressionLeft(this) }
            val operatorToken: String by lazy { INSTANCE.GetBinaryExpressionOperator(this) }
            val right: ExpressionSyntax by lazy { INSTANCE.GetBinaryExpressionRight(this) }
        }

        /**
         * Represents the Roslyn
         * [`IdentifierNameSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.identifiernamesyntax?view=roslyn-dotnet-5.0.0)
         * class.
         */
        class IdentifierNameSyntax(p: Pointer? = Pointer.NULL) : ExpressionSyntax(p) {
            val identifier: String by lazy { INSTANCE.GetIdentifierNameSyntaxIdentifier(this) }
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

    fun GetFieldVariableCount(handle: AST.FieldDeclarationSyntax): Int

    fun GetFieldVariable(
        handle: AST.FieldDeclarationSyntax,
        index: Int,
    ): AST.VariableDeclaratorSyntax

    fun GetVariableDeclaratorIdentifier(handle: AST.VariableDeclaratorSyntax): String

    fun GetMethodDeclarationIdentifier(handle: AST.MethodDeclarationSyntax): String

    fun GetMethodDeclarationParameterCount(handle: AST.MethodDeclarationSyntax): Int

    fun GetMethodDeclarationParameter(
        handle: AST.MethodDeclarationSyntax,
        index: Int,
    ): AST.ParameterSyntax

    fun GetParameterIdentifier(handle: AST.ParameterSyntax): String

    fun GetParameterType(handle: AST.ParameterSyntax): AST.TypeSyntax

    fun GetTypeName(handle: AST.TypeSyntax): String

    fun GetConstructorDeclarationIdentifier(handle: AST.ConstructorDeclarationSyntax): String

    fun GetConstructorDeclarationParameterCount(handle: AST.ConstructorDeclarationSyntax): Int

    fun GetConstructorDeclarationParameter(
        handle: AST.ConstructorDeclarationSyntax,
        index: Int,
    ): AST.ParameterSyntax

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
