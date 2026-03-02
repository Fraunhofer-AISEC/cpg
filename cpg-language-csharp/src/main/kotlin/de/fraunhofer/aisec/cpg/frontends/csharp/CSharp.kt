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
                return INSTANCE.GetKind(this.pointer)
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
                return when (INSTANCE.GetKind(nativeValue)) {
                    "NamespaceDeclaration" -> NamespaceDeclarationSyntax(nativeValue)
                    "ClassDeclaration" -> ClassDeclarationSyntax(nativeValue)
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
            val members: List<BaseMethodDeclarationSyntax> by lazy {
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
                return when (INSTANCE.GetKind(nativeValue)) {
                    "MethodDeclaration" -> MethodDeclarationSyntax(nativeValue)
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
    ): AST.BaseMethodDeclarationSyntax

    fun GetMethodDeclarationIdentifier(handle: AST.MethodDeclarationSyntax): String

    fun GetNodeStartLine(handle: AST.Node): Int

    fun GetNodeStartColumn(handle: AST.Node): Int

    fun GetNodeEndLine(handle: AST.Node): Int

    fun GetNodeEndColumn(handle: AST.Node): Int

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
