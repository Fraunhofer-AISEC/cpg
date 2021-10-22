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
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import org.bytedeco.treesitter.TSNode
import org.bytedeco.treesitter.global.treesitter.*

class CXXTreeSitterLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, "::") {

    lateinit var input: String

    override fun parse(file: File?): TranslationUnitDeclaration {
        val parser = ts_parser_new()

        ts_parser_set_language(parser, tree_sitter_cpp())

        val code =
            """
           int main(char** argv, int argc) {
             return 1;
           }
        """.trimIndent()
        input = code

        val tree = ts_parser_parse_string(parser, null, code, code.length)
        val root = ts_tree_root_node(tree)

        assert(ts_node_type(root).string == "translation_unit")

        handleTranslationUnit(root)

        return TranslationUnitDeclaration()
    }

    fun handleTranslationUnit(node: TSNode) {
        // loop through children
        for (i in 0 until ts_node_named_child_count(node)) {
            handleDeclaration(ts_node_named_child(node, i))
        }
    }

    fun handleDeclaration(node: TSNode) {
        when (val type = ts_node_type(node).string) {
            "function_definition" -> handleFunctionDefinition(node)
            "parameter_declaration" -> handleParameterDeclaration(node)
            else -> {
                log.error("Not handling type {} yet.", type)
            }
        }
    }

    private fun handleParameterDeclaration(node: TSNode) {
        println(ts_node_string(node).string)
    }

    private fun handleFunctionDefinition(node: TSNode): FunctionDeclaration {
        println(ts_node_string(node).string)

        val func =
            handleFunctionDeclarator(
                ts_node_child_by_field_name(node, "declarator", "declarator".length)
            )

        // update code to include the whole function
        func.code = getCodeFromRawNode(node)

        var type = ts_node_child_by_field_name(node, "type", "type".length)

        println(ts_node_string(type).string)

        println(func)

        return func
    }

    private fun handleFunctionDeclarator(node: TSNode): FunctionDeclaration {
        val id = ts_node_child_by_field_name(node, "declarator", "declarator".length)
        val name = getCodeFromRawNode(id)

        val func = NodeBuilder.newFunctionDeclaration(name, getCodeFromRawNode(node))

        var parameterList = ts_node_child_by_field_name(node, "parameters", "parameters".length)
        for (i in 0 until ts_node_named_child_count(parameterList)) {
            handleDeclaration(ts_node_named_child(parameterList, i))
        }

        return func
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
        TODO("Not yet implemented")
    }

    override fun <S : Any?, T : Any?> setComment(s: S, ctx: T) {
        TODO("Not yet implemented")
    }
}
