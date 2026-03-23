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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.newTranslationUnit
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File

class CSharpLanguageFrontend(ctx: TranslationContext, language: Language<CSharpLanguageFrontend>) :
    LanguageFrontend<Csharp.AST.Node, Csharp.AST.Node>(ctx, language) {

    val declarationHandler = DeclarationHandler(this)
    val statementHandler = StatementHandler(this)

    private var currentFile: File? = null

    override fun parse(file: File): TranslationUnit {
        currentFile = file
        val source = file.readText()
        val root = Csharp.CSharpSyntaxTree.parseText(source)
        val tu = newTranslationUnit(file.name, rawNode = root)

        scopeManager.resetToGlobal(tu)
        currentTU = tu
        scopeManager.enterScope(tu)

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
            is Csharp.AST.TypeSyntax -> objectType(type.name)
            else -> unknownType()
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
