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

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.cpp2.CXXLanguageFrontend2.Companion.ts_node_child_by_field_name
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newDeclarationStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import org.bytedeco.treesitter.TSNode
import org.bytedeco.treesitter.global.treesitter
import org.bytedeco.treesitter.global.treesitter.*

class StatementHandler(lang: CXXLanguageFrontend2) :
    Handler<Statement, TSNode, CXXLanguageFrontend2>(::Statement, lang) {
    init {
        map.put(TSNode::class.java, ::handleStatement)
    }

    private fun handleStatement(node: TSNode): Statement {
        return when (val type = node.type) {
            "compound_statement" -> handleCompoundStatement(node)
            "declaration" -> handleDeclarationStatement(node)
            "expression_statement" -> handleExpressionStatement(node)
            "return_statement" -> handleReturnStatement(node)
            else -> {
                log.error("Not handling statement of type {} yet", type)
                configConstructor.get()
            }
        }
    }

    private fun handleReturnStatement(node: TSNode): Statement {
        val returnStatement = newReturnStatement(lang.getCodeFromRawNode(node))

        if (ts_node_child_count(node) > 0) {
            val child = ts_node_named_child(node, 0)

            val expression = lang.expressionHandler.handle(child)

            returnStatement.returnValue = expression
        }

        return returnStatement
    }

    private fun handleExpressionStatement(node: TSNode): Statement {
        // forward the first (and only child) to the expression handler
        return lang.expressionHandler.handle(treesitter.ts_node_named_child(node, 0))
    }

    private fun handleDeclarationStatement(node: TSNode): Statement {
        val stmt = newDeclarationStatement(lang.getCodeFromRawNode(node))

        var type = lang.handleType(ts_node_child_by_field_name(node, "type"))

        // if the type also declared something, we add it to the declaration statement
        (type as? ObjectType)?.recordDeclaration?.let {
            // lang.scopeManager.addDeclaration(it)
            stmt.addToPropertyEdgeDeclaration(it)
        }

        var declarator = ts_node_child_by_field_name(node, "declarator")
        // loop through the declarators
        do {
            val declaration =
                NodeBuilder.newVariableDeclaration("", type, lang.getCodeFromRawNode(node), false)

            lang.declarationHandler.processDeclarator(declarator, declaration)

            // update the type for the rest of the declarations
            type = declaration.type

            lang.scopeManager.addDeclaration(declaration)
            stmt.addToPropertyEdgeDeclaration(declaration)
            declarator = treesitter.ts_node_next_named_sibling(declarator)
        } while (!ts_node_is_null(declarator))

        return stmt
    }

    private fun handleCompoundStatement(node: TSNode): Statement {
        val compoundStatement = NodeBuilder.newCompoundStatement(lang.getCodeFromRawNode(node))

        lang.scopeManager.enterScope(compoundStatement)

        for (i in 0 until treesitter.ts_node_named_child_count(node)) {
            val statement = handleStatement(treesitter.ts_node_named_child(node, i))

            compoundStatement.addStatement(statement)
        }

        lang.scopeManager.leaveScope(compoundStatement)

        return compoundStatement
    }
}
