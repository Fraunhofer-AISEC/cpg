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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import org.treesitter.TSNode

class StatementHandler(frontend: RustLanguageFrontend) :
    RustHandler<Statement, TSNode>(::ProblemExpression, frontend) {

    override fun handleNode(node: TSNode): Statement {
        return when (node.type) {
            "block" -> handleBlock(node)
            "let_declaration" -> handleLetDeclaration(node)
            "return_expression" -> handleReturnExpression(node)
            "if_expression" -> handleIfExpression(node)
            "expression_statement" -> handleExpressionStatement(node)
            else -> {
                newProblemExpression("Unknown statement type: ${node.type}", rawNode = node)
            }
        }
    }

    internal fun handleBlock(node: TSNode): Block {
        val block = newBlock(rawNode = node)
        frontend.scopeManager.enterScope(block)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.isNamed && child.type != "{" && child.type != "}") {
                block.statements += handle(child)
            }
        }

        frontend.scopeManager.leaveScope(block)
        return block
    }

    private fun handleLetDeclaration(node: TSNode): DeclarationStatement {
        val declStmt = newDeclarationStatement(rawNode = node)

        val patternNode = node.getChildByFieldName("pattern")
        val name =
            if (patternNode != null) {
                frontend.codeOf(patternNode) ?: ""
            } else {
                // Fallback: first identifier
                var foundName = ""
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child.isNamed && child.type == "identifier") {
                        foundName = frontend.codeOf(child) ?: ""
                        break
                    }
                }
                foundName
            }

        val variable = newVariableDeclaration(name, rawNode = node)

        val valueNode = node.getChildByFieldName("value")
        if (valueNode != null) {
            variable.initializer = frontend.expressionHandler.handle(valueNode)
        } else {
            // Fallback: search for a node after '='
            var foundEqual = false
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child.type == "=") {
                    foundEqual = true
                } else if (foundEqual && child.isNamed && child.type != ";") {
                    variable.initializer = frontend.expressionHandler.handle(child)
                    break
                }
            }
        }

        frontend.scopeManager.addDeclaration(variable)
        declStmt.addDeclaration(variable)

        return declStmt
    }

    private fun handleReturnExpression(node: TSNode): ReturnStatement {
        val ret = newReturnStatement(rawNode = node)
        // In Rust return_expression, the value is often a child
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.isNamed && child.type != "return") {
                ret.returnValue = frontend.expressionHandler.handle(child)
                break
            }
        }
        return ret
    }

    private fun handleIfExpression(node: TSNode): IfStatement {
        val ifStmt = newIfStatement(rawNode = node)

        val condition = node.getChildByFieldName("condition")
        if (condition != null) {
            ifStmt.condition = frontend.expressionHandler.handle(condition)
        } else {
            // Fallback: search for a named child that isn't 'if' or '{' or 'block'
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (
                    child.isNamed &&
                        child.type != "if" &&
                        child.type != "block" &&
                        child.type != "else_clause"
                ) {
                    ifStmt.condition = frontend.expressionHandler.handle(child)
                    break
                }
            }
        }

        val consequence = node.getChildByFieldName("consequence")
        if (consequence != null) {
            ifStmt.thenStatement = handle(consequence)
        } else {
            // Fallback: the block is usually the consequence
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child.type == "block") {
                    ifStmt.thenStatement = handle(child)
                    break
                }
            }
        }

        val alternative = node.getChildByFieldName("alternative")
        if (alternative != null) {
            // Alternative can be another if_expression or a block
            // In Rust Tree-sitter, alternative is often an 'else_clause' node
            val elseNode =
                if (alternative.type == "else_clause") {
                    // Search for a named child in else_clause
                    var found: TSNode? = null
                    for (j in 0 until alternative.childCount) {
                        val c = alternative.getChild(j)
                        if (c.isNamed && c.type != "else") {
                            found = c
                            break
                        }
                    }
                    found
                } else {
                    alternative
                }
            if (elseNode != null) {
                ifStmt.elseStatement = handle(elseNode)
            }
        }

        return ifStmt
    }

    private fun handleExpressionStatement(node: TSNode): Statement {
        val child = node.getNamedChild(0) ?: return newEmptyStatement(rawNode = node)
        return if (child.type == "if_expression" || child.type == "block") {
            handle(child)
        } else {
            frontend.expressionHandler.handle(child)
        }
    }
}
