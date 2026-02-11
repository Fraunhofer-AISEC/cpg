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

/**
 * A [LanguageFrontend] for Rust using Tree-sitter. It utilizes the `tree-sitter-rust` grammar via
 * the `io.github.bonede:tree-sitter-ng` bindings to translate Rust source code into the common CPG
 * representation.
 */
class RustLanguageFrontend(ctx: TranslationContext, language: Language<RustLanguageFrontend>) :
    LanguageFrontend<TSNode, TSNode?>(ctx, language) {

    private lateinit var content: String
    private lateinit var uri: URI

    internal val declarationHandler = DeclarationHandler(this)
    internal val statementHandler = StatementHandler(this)
    internal val expressionHandler = ExpressionHandler(this)
    internal val typeHandler = TypeHandler(this)

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

        handleChildren(rootNode, tud)

        return tud
    }

    internal fun handleChildren(parent: TSNode, tud: TranslationUnitDeclaration) {
        val pendingAnnotations = mutableListOf<de.fraunhofer.aisec.cpg.graph.Annotation>()

        for (i in 0 until parent.childCount) {
            val child = parent.getChild(i)
            if (!child.isNamed) continue

            if (child.type == "attribute_item") {
                pendingAnnotations += parseAttribute(child)
                continue
            }

            val decl = declarationHandler.handle(child)
            if (pendingAnnotations.isNotEmpty()) {
                decl.annotations += pendingAnnotations
                pendingAnnotations.clear()
            }

            // Check for visibility_modifier as a child of the declaration node
            // (e.g., "pub" in "pub fn foo()")
            for (j in 0 until child.childCount) {
                val grandchild = child.getChild(j)
                if (grandchild.type == "visibility_modifier") {
                    val visibilityText = codeOf(grandchild) ?: "pub"
                    decl.annotations += newAnnotation(visibilityText, rawNode = grandchild)
                    break
                }
            }

            tud.addDeclaration(decl)
        }
    }

    internal fun parseAttribute(node: TSNode): de.fraunhofer.aisec.cpg.graph.Annotation {
        // attribute_item has children: #, [, attribute, ]
        // The "attribute" child contains the actual content like "derive(Clone, Debug)"
        var attrContent = ""
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.type == "attribute") {
                attrContent = codeOf(child) ?: ""
                break
            }
        }
        return newAnnotation(attrContent, rawNode = node)
    }

    override fun typeOf(type: TSNode?): Type {
        return if (type != null) typeHandler.handle(type) else unknownType()
    }

    override fun codeOf(astNode: TSNode): String? {
        if (astNode.isNull()) return null
        return content.substring(astNode.startByte, astNode.endByte)
    }

    override fun locationOf(astNode: TSNode): PhysicalLocation? {
        if (astNode.isNull()) return null
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
