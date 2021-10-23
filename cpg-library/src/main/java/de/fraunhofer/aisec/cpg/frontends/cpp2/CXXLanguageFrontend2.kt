/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cpp2

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.*
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import org.bytedeco.treesitter.TSNode
import org.bytedeco.treesitter.global.treesitter.*

val TSNode.type: String
    get() {
        return ts_node_type(this).string
    }

/**
 * An (experimental) language frontend for the [TypeManager.Language.CXX] language based on
 * [Tree-sitter](https://tree-sitter.github.io).
 */
class CXXLanguageFrontend2(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, "::") {

    lateinit var input: String
    lateinit var currentFile: File

    var expressionHandler = ExpressionHandler(this)
    var statementHandler = StatementHandler(this)
    var declarationHandler = DeclarationHandler(this)

    override fun parse(file: File): TranslationUnitDeclaration {
        TypeManager.getInstance().setLanguageFrontend(this)

        val parser = ts_parser_new()

        ts_parser_set_language(parser, tree_sitter_cpp())

        input = file.readText()
        currentFile = file

        val tree = ts_parser_parse_string(parser, null, input, input.length)
        val root = ts_tree_root_node(tree)

        assert(ts_node_type(root).string == "translation_unit")

        return handleTranslationUnit(root)
    }

    fun handleTranslationUnit(node: TSNode): TranslationUnitDeclaration {
        val tu = newTranslationUnitDeclaration("", getCodeFromRawNode(node))

        scopeManager.resetToGlobal(tu)

        // loop through children
        for (i in 0 until ts_node_named_child_count(node)) {
            val declaration = declarationHandler.handle(ts_node_named_child(node, i))

            scopeManager.addDeclaration(declaration)
        }

        return tu
    }

    fun handleType(node: TSNode): Type {
        return when (node.type) {
            "primitive_type" -> getCodeFromRawNode(node)?.let { TypeParser.createFrom(it, false) }
                    ?: UnknownType.getUnknownType()
            "type_identifier" -> getCodeFromRawNode(node)?.let { TypeParser.createFrom(it, false) }
                    ?: UnknownType.getUnknownType()
            "scoped_type_identifier" ->
                getCodeFromRawNode(node)?.let { TypeParser.createFrom(it, false) }
                    ?: UnknownType.getUnknownType()
            "class_specifier" -> handleClassSpecifier(node)
            else -> {
                log.error(
                    "Not handling type of type {} yet: {}",
                    node.type,
                    getCodeFromRawNode(node)
                )
                return UnknownType.getUnknownType()
            }
        }
    }

    private fun handleClassSpecifier(node: TSNode): Type {
        val recordDeclaration = declarationHandler.handle(node) as? RecordDeclaration

        return recordDeclaration?.toType() ?: UnknownType.getUnknownType()
    }

    override fun <T : Any?> getCodeFromRawNode(astNode: T): String? {
        if (astNode is TSNode) {
            val start = ts_node_start_byte(astNode)
            val end = ts_node_end_byte(astNode)

            return input.substring(start, end)
        }

        return null
    }

    override fun <T : Any?> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        if (astNode is TSNode) {
            val start = ts_node_start_point(astNode)
            val end = ts_node_end_point(astNode)

            return PhysicalLocation(
                currentFile.toURI(),
                Region(start.row(), start.column(), end.row(), end.column())
            )
        }

        return null
    }

    override fun <S : Any?, T : Any?> setComment(s: S, ctx: T) {}

    companion object {
        @JvmStatic
        fun ts_node_child_by_field_name(node: TSNode, field: String): TSNode {
            return ts_node_child_by_field_name(node, field, field.length)
        }
    }
}
