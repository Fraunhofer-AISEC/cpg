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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.array
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.newProblemType
import de.fraunhofer.aisec.cpg.graph.newTranslationUnit
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.parseName
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File

class CSharpLanguageFrontend(ctx: TranslationContext, language: Language<CSharpLanguageFrontend>) :
    LanguageFrontend<Csharp.AST.Node, Csharp.AST.Node>(ctx, language) {

    val declarationHandler = DeclarationHandler(this)
    val statementHandler = StatementHandler(this)
    val expressionHandler = ExpressionHandler(this)

    private var currentFile: File? = null

    override fun parse(file: File): TranslationUnit {
        currentFile = file
        val source = file.readText()
        val root = Csharp.CSharpSyntaxTree.parseText(source)
        val tu = newTranslationUnit(file.name, rawNode = root)

        scopeManager.resetToGlobal(tu)
        currentTU = tu
        scopeManager.enterScope(tu)

        for (using in root.usings) {
            val import = declarationHandler.handleUsingDirective(using)
            scopeManager.addDeclaration(import)
            tu.addDeclaration(import)
        }

        for (member in root.members) {
            val decl = declarationHandler.handle(member)
            scopeManager.addDeclaration(decl)
            tu.addDeclaration(decl)
        }

        scopeManager.leaveScope(tu)
        return tu
    }

    override fun typeOf(type: Csharp.AST.Node): Type {
        return when (type) {
            is Csharp.AST.ArrayTypeSyntax -> typeOf(type.elementType).array()
            // `string?` or `int?`
            is Csharp.AST.NullableTypeSyntax -> typeOf(type.elementType)
            // `scoped Span<byte>`. The `scoped` modifier promises that the reference does not
            // escape the current method. That is a lifetime constraint the compiler checks, so we
            // simply continue with the type it is
            // attached to.
            is Csharp.AST.ScopedTypeSyntax -> typeOf(type.type)
            // The empty spot in an unbound generic name such as `List<>`.
            is Csharp.AST.OmittedTypeArgumentSyntax -> unknownType()
            // `List<int>`. The type arguments must not become part of the name, because a type
            // named `List<int>` would never be matched with the declaration of `List`. They become
            // the generics of the type instead, which is what [Csharp.AST.TypeSyntax.name] does.
            is Csharp.AST.GenericNameSyntax ->
                objectType(type.identifier, type.typeArguments.map { typeOf(it) })
            // `System.Collections.Generic.List<int>`. Only the right half states the type and can
            // carry type arguments, the left half is the namespace it lives in and becomes the
            // parent of the name.
            is Csharp.AST.QualifiedNameSyntax -> {
                val right = typeOf(type.right)
                objectType(
                    Name(right.name.localName, parseName(type.left.name)),
                    (right as? ObjectType)?.generics ?: listOf(),
                )
            }
            // `Foo` and `int`. These are the only two kinds whose source text really is a name, so
            // they are the only ones we may take [Csharp.AST.TypeSyntax.name] from as it is.
            is Csharp.AST.IdentifierNameSyntax,
            is Csharp.AST.PredefinedTypeSyntax -> objectType(type.name)
            // Every other kind of type still writes its modifiers, punctuation and type arguments
            // into its source text, so using that as a name would invent a type such as `byte*` or
            // `(int, string)` that can never be matched with any declaration. We report that
            // instead of inventing a new type.
            else -> {
                Util.warnWithFileLocation(
                    locationOf(type),
                    log,
                    "Cannot determine a type from a {}, since we do not handle it yet",
                    type.csharpType,
                )
                newProblemType(type)
            }
        }
    }

    override fun codeOf(astNode: Csharp.AST.Node): String = Csharp.INSTANCE.GetCode(astNode)

    override fun locationOf(astNode: Csharp.AST.Node): PhysicalLocation? {
        val file = currentFile ?: return null
        return PhysicalLocation(
            file.toURI(),
            Region(
                startLine = astNode.startLine,
                startColumn = astNode.startColumn,
                endLine = astNode.endLine,
                endColumn = astNode.endColumn,
            ),
        )
    }

    override fun setComment(node: Node, astNode: Csharp.AST.Node) {}
}
