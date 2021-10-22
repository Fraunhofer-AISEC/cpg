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
            "expression_statement" -> handleExpressionStatement(node)
            else -> {
                log.error("Not handling statement of type {} yet", type)
                newStatement(getCodeFromRawNode(node))
            }
        }
    }

    private fun handleExpressionStatement(node: TSNode): Statement {
        // forward the first (and only child) to the expression handler
        return handleExpression(ts_node_named_child(node, 0))
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
        val compoundStatement = newCompoundStatement(getCodeFromRawNode(node))

        scopeManager.enterScope(compoundStatement)

        for (i in 0 until ts_node_named_child_count(node)) {
            val statement = handleStatement(ts_node_named_child(node, i))

            compoundStatement.addStatement(statement)
        }

        scopeManager.leaveScope(compoundStatement)

        return compoundStatement
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
        // going forward in the declarator chain
        processDeclarator(
            ts_node_child_by_field_name(node, "declarator", "declarator".length),
            declaration
        )

        val hasInitializer = declaration as? HasInitializer
        hasInitializer?.let {
            // the value is nested in the init declarator
            val expression =
                handleExpression(ts_node_child_by_field_name(node, "value", "value".length))

            hasInitializer.initializer = expression
        }
    }

    private fun handleExpression(node: TSNode): Expression {
        return when (node.type) {
            "identifier" -> handleIdentifier(node)
            "assignment_expression" -> handleAssignmentExpression(node)
            "binary_expression" -> handleBinaryExpression(node)
            "number_literal" -> handleNumberLiteral(node)
            else -> {
                log.error(
                    "Not handling expression of type {} yet: {}",
                    node.type,
                    getCodeFromRawNode(node)
                )
                Expression()
            }
        }
    }

    private fun handleIdentifier(node: TSNode): Expression {
        val name = getCodeFromRawNode(node)

        val ref = newDeclaredReferenceExpression(name, UnknownType.getUnknownType(), name)

        return ref
    }

    private fun handleNumberLiteral(node: TSNode): Expression {
        val value = getCodeFromRawNode(node)?.toInt()
        val literal =
            newLiteral(value, TypeParser.createFrom("int", false), getCodeFromRawNode(node))

        return literal
    }

    private fun handleBinaryExpression(node: TSNode): Expression {
        val symbol = getCodeFromRawNode(ts_node_child(node, 1))

        val expression = newBinaryOperator(symbol, getCodeFromRawNode(node))

        expression.lhs = handleExpression(ts_node_child_by_field_name(node, "left", "left".length))
        expression.rhs =
            handleExpression(ts_node_child_by_field_name(node, "right", "right".length))

        return expression
    }

    private fun handleAssignmentExpression(node: TSNode): Expression {
        val expression = newBinaryOperator("=", getCodeFromRawNode(node))

        expression.lhs = handleExpression(ts_node_child_by_field_name(node, "left", "left".length))
        expression.rhs =
            handleExpression(ts_node_child_by_field_name(node, "right", "right".length))

        return expression
    }

    private fun processPointerDeclarator(node: TSNode, declaration: ValueDeclaration) {
        processDeclarator(
            ts_node_child_by_field_name(node, "declarator", "declarator".length),
            declaration
        )

        log.debug("Type was: {}", declaration.type)

        var type =
            TypeParser.createFrom(declaration.type.typeName, false)
                .reference(PointerType.PointerOrigin.POINTER)

        log.debug("Type should be: {}", type)

        // reference the type using a pointer
        declaration.type = type

        log.debug("Type is: {}", type)
    }

    private fun handleFunctionDefinition(node: TSNode): FunctionDeclaration {
        println(ts_node_string(node).string)

        val nonPointerType = handleType(ts_node_child_by_field_name(node, "type", "type".length))

        // name will be filled later by handleDeclarator
        val func = newFunctionDeclaration("", getCodeFromRawNode(node))
        func.type = nonPointerType

        scopeManager.enterScope(func)

        processDeclarator(
            ts_node_child_by_field_name(node, "declarator", "declarator".length),
            func
        )

        // update code to include the whole function
        func.code = getCodeFromRawNode(node)

        println(func)

        func.body = handleStatement(ts_node_child_by_field_name(node, "body", "body".length))

        scopeManager.leaveScope(func)

        return func
    }

    private fun processFunctionDeclarator(node: TSNode, func: FunctionDeclaration) {
        processDeclarator(
            ts_node_child_by_field_name(node, "declarator", "declarator".length),
            func
        )

        val parameterList = ts_node_child_by_field_name(node, "parameters", "parameters".length)
        for (i in 0 until ts_node_named_child_count(parameterList)) {
            val declaration = handleDeclaration(ts_node_named_child(parameterList, i))

            scopeManager.addDeclaration(declaration)
        }
    }

    private fun handleType(node: TSNode): Type {
        return when (node.type) {
            "primitive_type" -> getCodeFromRawNode(node)?.let { TypeParser.createFrom(it, false) }
                    ?: UnknownType.getUnknownType()
            "type_identifier" -> getCodeFromRawNode(node)?.let { TypeParser.createFrom(it, false) }
                    ?: UnknownType.getUnknownType()
            "scoped_type_identifier" ->
                getCodeFromRawNode(node)?.let { TypeParser.createFrom(it, false) }
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
