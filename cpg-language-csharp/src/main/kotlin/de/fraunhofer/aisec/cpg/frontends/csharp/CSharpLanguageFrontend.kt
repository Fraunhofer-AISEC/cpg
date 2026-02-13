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
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.newNamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.newRecordDeclaration
import de.fraunhofer.aisec.cpg.graph.newTranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File

class CSharpLanguageFrontend(ctx: TranslationContext, language: Language<CSharpLanguageFrontend>) :
    LanguageFrontend<CSharpSyntaxNode, CSharpSyntaxNode>(ctx, language) {

    companion object {
        internal val nativeLib: CSharpNativeParser by lazy {
            val libFile =
                File(
                    "src/main/csharp/NativeParser/bin/Release/net8.0/osx-arm64/publish/NativeParser.dylib"
                )
            if (!libFile.exists()) {
                throw TranslationException("nothing found")
            }
            Native.load(libFile.absolutePath, CSharpNativeParser::class.java)
        }

        internal fun nativeString(ptr: Pointer): String {
            return try {
                ptr.getString(0, "UTF-8")
            } finally {
                nativeLib.freeString(ptr)
            }
        }

        fun wrapHandle(handle: Pointer): CSharpSyntaxNode {
            val kind = nativeString(nativeLib.getKind(handle))
            return when (kind) {
                "CompilationUnit" -> CompilationUnitSyntax(handle)
                "NamespaceDeclaration" -> NamespaceDeclarationSyntax(handle)
                "ClassDeclaration" -> ClassDeclarationSyntax(handle)
                else -> CSharpSyntaxNode(handle)
            }
        }
    }

    override fun parse(file: File): TranslationUnitDeclaration {
        val source = file.readText()
        val rootHandle = nativeLib.parseCsharp(source)
        val root = wrapHandle(rootHandle)

        val tu = newTranslationUnitDeclaration(file.name, rawNode = root)
        scopeManager.resetToGlobal(tu)
        scopeManager.enterScope(tu)

        for (child in root.children) {
            handleNode(child, tu)
        }

        scopeManager.leaveScope(tu)
        return tu
    }

    private fun handleNode(node: CSharpSyntaxNode, parent: DeclarationHolder) {
        when (node) {
            is NamespaceDeclarationSyntax -> {
                val ns = newNamespaceDeclaration(node.name, rawNode = node)
                scopeManager.enterScope(ns)

                for (child in node.children) {
                    handleNode(child, ns)
                }

                scopeManager.leaveScope(ns)
                scopeManager.addDeclaration(ns)
                parent.addDeclaration(ns)
            }
            is ClassDeclarationSyntax -> {
                val record = newRecordDeclaration(node.identifier, "class", rawNode = node)
                scopeManager.enterScope(record)
                scopeManager.leaveScope(record)
                scopeManager.addDeclaration(record)
                parent.addDeclaration(record)
            }
        }
    }

    override fun typeOf(type: CSharpSyntaxNode): Type = unknownType()

    override fun codeOf(astNode: CSharpSyntaxNode): String = astNode.code

    override fun locationOf(astNode: CSharpSyntaxNode): PhysicalLocation? = null

    override fun setComment(node: Node, astNode: CSharpSyntaxNode) {}
}

interface CSharpNativeParser : Library {
    fun parseCsharp(source: String): Pointer

    fun getKind(handle: Pointer): Pointer

    fun getCode(handle: Pointer): Pointer

    fun getNumChildren(handle: Pointer): Int

    fun getChild(handle: Pointer, index: Int): Pointer

    fun getIdentifier(handle: Pointer): Pointer

    fun getName(handle: Pointer): Pointer

    fun freeString(ptr: Pointer)
}

// https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.csharpsyntaxnode?view=roslyn-dotnet-5.0.0
open class CSharpSyntaxNode(val handle: Pointer) {
    val code: String by lazy {
        CSharpLanguageFrontend.nativeString(CSharpLanguageFrontend.nativeLib.getCode(handle))
    }
    val children: List<CSharpSyntaxNode> by lazy {
        val n = CSharpLanguageFrontend.nativeLib.getNumChildren(handle)
        (0 until n).map {
            val childHandle = CSharpLanguageFrontend.nativeLib.getChild(handle, it)
            CSharpLanguageFrontend.wrapHandle(childHandle)
        }
    }
}

// https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.compilationunitsyntax?view=roslyn-dotnet-5.0.0
class CompilationUnitSyntax(handle: Pointer) : CSharpSyntaxNode(handle)

class NamespaceDeclarationSyntax(handle: Pointer) : CSharpSyntaxNode(handle) {
    val name: String by lazy {
        CSharpLanguageFrontend.nativeString(CSharpLanguageFrontend.nativeLib.getName(handle))
    }
}

class ClassDeclarationSyntax(handle: Pointer) : CSharpSyntaxNode(handle) {
    val identifier: String by lazy {
        CSharpLanguageFrontend.nativeString(CSharpLanguageFrontend.nativeLib.getIdentifier(handle))
    }
}
