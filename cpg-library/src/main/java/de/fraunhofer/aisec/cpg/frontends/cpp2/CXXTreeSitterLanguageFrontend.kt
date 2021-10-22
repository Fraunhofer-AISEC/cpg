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
import de.fraunhofer.aisec.cpg.graph.HasInitializer
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.*
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import org.bytedeco.treesitter.TSNode
import org.bytedeco.treesitter.global.treesitter.*

private val TSNode.type: String
    get() {
        return ts_node_type(this).string
    }

class CXXTreeSitterLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, "::") {

    lateinit var input: String

    override fun parse(file: File): TranslationUnitDeclaration {
        TypeManager.getInstance().setLanguageFrontend(this)

        val parser = ts_parser_new()

        ts_parser_set_language(parser, tree_sitter_cpp())

        input = file.readText()

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
            val declaration = handleDeclaration(ts_node_named_child(node, i))

            scopeManager.addDeclaration(declaration)
        }

        return tu
    }

    fun handleDeclaration(node: TSNode): Declaration {
        return when (val type = node.type) {
            "function_definition" -> handleFunctionDefinition(node)
            "parameter_declaration" -> handleParameterDeclaration(node)
            else -> {
                log.error("Not handling type {} yet.", type)
                Declaration()
            }
        }
    }

    private fun handleParameterDeclaration(node: TSNode): ParamVariableDeclaration {
        println(ts_node_string(node).string)

        val startType = handleType(ts_node_child_by_field_name(node, "type", "type".length))
        val param = newMethodParameterIn("", startType, false, getCodeFromRawNode(node))

        // process the declarator to adjust name and type of this declaration
        processDeclarator(
            ts_node_child_by_field_name(node, "declarator", "declarator".length),
            param
        )

        return param
    }

    private fun handleStatement(node: TSNode): Statement {
        return when (val type = node.type) {
            "compound_statement" -> handleCompoundStatement(node)
            "declaration" -> handleDeclarationStatement(node)
            else -> {
                log.error("Not handling declarator of type {} yet", type)
                newStatement(getCodeFromRawNode(node))
            }
        }
    }

    private fun handleDeclarationStatement(node: TSNode): Statement {
        val stmt = newDeclarationStatement(getCodeFromRawNode(node))

        val type = handleType(ts_node_child_by_field_name(node, "type", "type".length))
        val declaration = newVariableDeclaration("", type, getCodeFromRawNode(node), false)

        processDeclarator(
            ts_node_child_by_field_name(node, "declarator", "declarator".length),
            declaration
        )

        scopeManager.addDeclaration(declaration)

        stmt.singleDeclaration = declaration

        return stmt
    }

    private fun handleCompoundStatement(node: TSNode): Statement {
        val stmt = newCompoundStatement(getCodeFromRawNode(node))

        scopeManager.enterScope(stmt)

        for (i in 0 until ts_node_named_child_count(node)) {
            handleStatement(ts_node_named_child(node, i))
        }

        scopeManager.leaveScope(stmt)

        return stmt
    }

    private fun processDeclarator(node: TSNode, declaration: ValueDeclaration) {
        when (node.type) {
            "identifier" -> {
                declaration.name = getCodeFromRawNode(node) ?: ""
            }
            "init_declarator" -> {
                processInitDeclarator(node, declaration)
            }
            "pointer_declarator" -> {
                processPointerDeclarator(node, declaration)
            }
            "function_declarator" ->
                processFunctionDeclarator(node, declaration as FunctionDeclaration)
            else -> {
                log.error("Not handling declarator of type {} yet", node.type)
            }
        }
    }

    private fun processInitDeclarator(node: TSNode, declaration: ValueDeclaration) {
        var hasInitializer = declaration as? HasInitializer
        hasInitializer?.let {
            hasInitializer.initializer =
                handleExpression(
                    ts_node_child_by_field_name(node, "declarator", "declarator".length)
                )
        }
    }

    private fun handleExpression(node: TSNode): Expression {
        return when (node.type) {
            "identifier" -> {
                newLiteral(1, UnknownType.getUnknownType(), "1")
            }
            else -> {
                println(ts_node_string(node).string)
                log.error(
                    "Not handling expression of type {} yet: {}",
                    node.type,
                    getCodeFromRawNode(node)
                )
                Expression()
            }
        }
    }

    private fun processPointerDeclarator(node: TSNode, declaration: ValueDeclaration) {
        processDeclarator(
            ts_node_child_by_field_name(node, "declarator", "declarator".length),
            declaration
        )

        // reference the type using a pointer
        declaration.type = declaration.type.reference(PointerType.PointerOrigin.POINTER)
    }

    private fun handleFunctionDefinition(node: TSNode): FunctionDeclaration {
        println(ts_node_string(node).string)

        val nonPointerType = handleType(ts_node_child_by_field_name(node, "type", "type".length))

        // name will be filled later by handleDeclarator
        val func = newFunctionDeclaration("", getCodeFromRawNode(node))
        func.type = nonPointerType

        processDeclarator(
            ts_node_child_by_field_name(node, "declarator", "declarator".length),
            func
        )

        // update code to include the whole function
        func.code = getCodeFromRawNode(node)

        println(func)

        func.body = handleStatement(ts_node_child_by_field_name(node, "body", "body".length))

        return func
    }

    private fun processFunctionDeclarator(node: TSNode, func: FunctionDeclaration) {
        processDeclarator(
            ts_node_child_by_field_name(node, "declarator", "declarator".length),
            func
        )

        scopeManager.enterScope(func)

        val parameterList = ts_node_child_by_field_name(node, "parameters", "parameters".length)
        for (i in 0 until ts_node_named_child_count(parameterList)) {
            val declaration = handleDeclaration(ts_node_named_child(parameterList, i))

            scopeManager.addDeclaration(declaration)
        }

        scopeManager.leaveScope(func)
    }

    private fun handleType(node: TSNode): Type {
        return when (node.type) {
            "primitive_type" -> getCodeFromRawNode(node)?.let { TypeParser.createFrom(it, false) }
                    ?: UnknownType.getUnknownType()
            "type_identifier" -> getCodeFromRawNode(node)?.let { TypeParser.createFrom(it, false) }
                    ?: UnknownType.getUnknownType()
            else -> {
                log.error("Not handling type of type {} yet", node.type)
                return UnknownType.getUnknownType()
            }
        }
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
