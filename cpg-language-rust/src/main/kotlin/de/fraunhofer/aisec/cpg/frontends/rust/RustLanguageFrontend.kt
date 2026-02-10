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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.net.URI
import org.treesitter.*

/** A [LanguageFrontend] for Rust using Tree-sitter. */
class RustLanguageFrontend(ctx: TranslationContext, language: Language<RustLanguageFrontend>) :
    LanguageFrontend<TSNode, TSNode?>(ctx, language) {

    private lateinit var content: String
    private lateinit var uri: URI

    internal val declarationHandler = DeclarationHandler(this)
    internal val statementHandler = StatementHandler(this)
    internal val expressionHandler = ExpressionHandler(this)

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        content = file.readText()
        uri = file.toURI()

        val parser = TSParser()
        parser.setLanguage(TreeSitterRust())

        val tree = parser.parseString(null, content)
        val rootNode = tree.rootNode

        val tud = newTranslationUnitDeclaration(file.absolutePath, rawNode = rootNode)
        tud.location = locationOf(rootNode)

        scopeManager.resetToGlobal(tud)

        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            if (child.isNamed) {
                val decl = declarationHandler.handle(child)
                if (decl != null) {
                    tud.addDeclaration(decl)
                }
            }
        }

        return tud
    }

    override fun typeOf(type: TSNode?): Type {
        if (type == null) return unknownType()
        val code = codeOf(type) ?: return unknownType()
        return objectType(code)
    }

    override fun codeOf(astNode: TSNode): String? {
        return content.substring(astNode.startByte, astNode.endByte)
    }

    override fun locationOf(astNode: TSNode): PhysicalLocation? {
        val startPoint = astNode.startPoint
        val endPoint = astNode.endPoint

        return PhysicalLocation(
            uri,
            Region(startPoint.row + 1, startPoint.column + 1, endPoint.row + 1, endPoint.column + 1),
        )
    }

    override fun setComment(node: Node, astNode: TSNode) {
        // TODO
    }
}
