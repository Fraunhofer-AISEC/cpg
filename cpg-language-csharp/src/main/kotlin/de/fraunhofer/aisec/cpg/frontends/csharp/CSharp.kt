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
    interface Ast {
        /**
         * Base class for all C# syntax nodes. Represents Roslyn's
         * `Microsoft.CodeAnalysis.CSharp.CSharpSyntaxNode`.
         *
         * See
         * [CSharpSyntaxNode](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.csharpsyntaxnode?view=roslyn-dotnet-5.0.0)
         */
        open class Node(p: Pointer? = Pointer.NULL) : CsharpObject(p)

        /**
         * Represents the Roslyn `CompilationUnitSyntax` class.
         *
         * See
         * [CompilationUnitSyntax](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.compilationunitsyntax?view=roslyn-dotnet-5.0.0)
         */
        class CompilationUnitSyntax(p: Pointer? = Pointer.NULL) : Node(p)

        /**
         * Represents the Roslyn `NamespaceDeclarationSyntax` class.
         *
         * See
         * [NamespaceDeclarationSyntax](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.namespacedeclarationsyntax?view=roslyn-dotnet-5.0.0)
         */
        class NamespaceDeclarationSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val name: String by lazy { INSTANCE.GetNamespaceDeclarationName(this) }
        }

        /**
         * Represents the Roslyn `ClassDeclarationSyntax` class.
         *
         * See
         * [ClassDeclarationSyntax](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.classdeclarationsyntax?view=roslyn-dotnet-5.0.0)
         */
        class ClassDeclarationSyntax(p: Pointer? = Pointer.NULL) : Node(p) {
            val identifier: String by lazy { INSTANCE.GetClassDeclarationIdentifier(this) }
        }
    }

    /**
     * Represents the Roslyn `CSharpSyntaxTree` class.
     *
     * See: TODO(link)
     */
    object CSharpSyntaxTree {
        fun parseText(source: String): Ast.CompilationUnitSyntax {
            return Ast.CompilationUnitSyntax(INSTANCE.CSharpRoslynSyntaxTreeParseText(source))
        }
    }

    fun CSharpRoslynSyntaxTreeParseText(source: String): Pointer

    fun GetKind(handle: Pointer): String

    fun GetNamespaceDeclarationName(handle: Ast.NamespaceDeclarationSyntax): String

    fun GetClassDeclarationIdentifier(handle: Ast.ClassDeclarationSyntax): String

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
