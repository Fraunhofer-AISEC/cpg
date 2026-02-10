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
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import org.treesitter.TSNode

/**
 * A [Handler] that translates Rust statements into CPG [Statement] nodes. It currently supports
 * blocks, let declarations, return statements, and if expressions.
 */
class StatementHandler(frontend: RustLanguageFrontend) :
    RustHandler<Statement, TSNode>(::ProblemExpression, frontend) {

    override fun handleNode(node: TSNode): Statement {
        return when (node.type) {
            "block" -> handleBlock(node)
            "let_declaration" -> handleLetDeclaration(node)
            "return_expression" -> handleReturnExpression(node)
            "if_expression" -> handleIfExpression(node)
            "while_expression" -> handleWhileExpression(node)
            "loop_expression" -> handleLoopExpression(node)
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

        val typeNode = node.getChildByFieldName("type")
        if (typeNode != null) {
            variable.type = frontend.typeOf(typeNode)
        }

        val valueNode = node.getChildByFieldName("value")
        if (valueNode != null) {
            variable.initializer = frontend.expressionHandler.handle(valueNode) as? Expression
        } else {
            // Fallback: search for a node after '='
            var foundEqual = false
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child.type == "=") {
                    foundEqual = true
                } else if (foundEqual && child.isNamed && child.type != ";") {
                    variable.initializer = frontend.expressionHandler.handle(child) as? Expression
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
                ret.returnValue = frontend.expressionHandler.handle(child) as? Expression
                break
            }
        }
        return ret
    }

    private fun handleIfExpression(node: TSNode): IfStatement {
        val ifStmt = newIfStatement(rawNode = node)

        var condition = node.getChildByFieldName("condition")
        if (condition != null && condition.isNull) condition = null

        if (condition != null) {
            ifStmt.condition = frontend.expressionHandler.handle(condition) as? Expression
        } else {
            // Fallback: search for a named child that isn't 'if' or '{' or 'block'
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (
                    !child.isNull &&
                        child.isNamed &&
                        child.type != "if" &&
                        child.type != "block" &&
                        child.type != "else_clause"
                ) {
                    ifStmt.condition = frontend.expressionHandler.handle(child) as? Expression
                    condition = child
                    break
                }
            }
        }

        // Check for bindings (if let)
        val bindings =
            if (condition != null && condition.type == "let_condition") {
                extractBindings(condition.getChildByFieldName("pattern"))
            } else {
                emptyList<VariableDeclaration>()
            }

        val consequence = node.getChildByFieldName("consequence")
        if (consequence != null && !consequence.isNull) {
            ifStmt.thenStatement =
                if (bindings.isNotEmpty()) {
                    handleBlockWithBindings(consequence, bindings)
                } else {
                    handle(consequence)
                }
        } else {
            // Fallback: the block is usually the consequence
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (!child.isNull && child.type == "block") {
                    ifStmt.thenStatement =
                        if (bindings.isNotEmpty()) {
                            handleBlockWithBindings(child, bindings)
                        } else {
                            handle(child)
                        }
                    break
                }
            }
        }

        val alternative = node.getChildByFieldName("alternative")
        if (alternative != null && !alternative.isNull) {
            // Alternative can be another if_expression or a block
            // In Rust Tree-sitter, alternative is often an 'else_clause' node
            val elseNode =
                if (alternative.type == "else_clause") {
                    // Search for a named child in else_clause
                    var found: TSNode? = null
                    for (j in 0 until alternative.childCount) {
                        val c = alternative.getChild(j)
                        if (!c.isNull && c.isNamed && c.type != "else") {
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

    private fun handleWhileExpression(node: TSNode): Statement {
        val whileStmt = newWhileStatement(rawNode = node)

        var condition = node.getChildByFieldName("condition")
        if (condition != null && condition.isNull) condition = null

        if (condition != null) {
            whileStmt.condition = frontend.expressionHandler.handle(condition) as? Expression
        }

        // Check for bindings (while let)
        val bindings =
            if (condition != null && condition.type == "let_condition") {
                extractBindings(condition.getChildByFieldName("pattern"))
            } else {
                emptyList<VariableDeclaration>()
            }

        val body = node.getChildByFieldName("body")
        if (body != null && !body.isNull) {
            whileStmt.statement =
                if (bindings.isNotEmpty()) {
                    handleBlockWithBindings(body, bindings)
                } else {
                    handle(body)
                }
        }

        var label = node.getChildByFieldName("label")
        if (label == null || label.isNull) {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child.type == "loop_label" || child.type == "label") {
                    label = child
                    break
                }
            }
        }

        return if (label != null && !label.isNull) {
            val labelStmt = newLabelStatement(rawNode = node)
            val code = frontend.codeOf(label) ?: ""
            labelStmt.label = code.removePrefix("'")
            labelStmt.subStatement = whileStmt
            labelStmt
        } else {
            whileStmt
        }
    }

    private fun handleLoopExpression(node: TSNode): Statement {
        val loop = newWhileStatement(rawNode = node)
        // Infinite loop: while(true)
        loop.condition = newLiteral(true, primitiveType("bool"), rawNode = node).implicit()

        val body = node.getChildByFieldName("body")
        if (body != null && !body.isNull) {
            loop.statement = handle(body)
        }

        var label = node.getChildByFieldName("label")
        if (label == null || label.isNull) {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child.type == "loop_label" || child.type == "label") {
                    label = child
                    break
                }
            }
        }

        return if (label != null && !label.isNull) {
            val labelStmt = newLabelStatement(rawNode = node)
            val code = frontend.codeOf(label) ?: ""
            labelStmt.label = code.removePrefix("'")
            labelStmt.subStatement = loop
            labelStmt
        } else {
            loop
        }
    }

    internal fun handleBlockWithBindings(node: TSNode, bindings: List<VariableDeclaration>): Block {
        val block = newBlock(rawNode = node)
        frontend.scopeManager.enterScope(block)

        bindings.forEach {
            frontend.scopeManager.addDeclaration(it)
            val decl = newDeclarationStatement(rawNode = null)
            decl.location = it.location
            decl.addDeclaration(it)
            block.statements.add(decl)
        }

        if (node.type == "block") {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child.isNamed && child.type != "{" && child.type != "}") {
                    block.statements += handle(child)
                }
            }
        } else {
            block.statements += handle(node)
        }

        frontend.scopeManager.leaveScope(block)
        return block
    }

    internal fun extractBindings(pattern: TSNode?): List<VariableDeclaration> {
        val vars = mutableListOf<VariableDeclaration>()
        if (pattern == null) return vars

        when (pattern.type) {
            "identifier" -> {
                val name = frontend.codeOf(pattern) ?: ""
                vars += newVariableDeclaration(name, rawNode = pattern)
            }
            "tuple_struct_pattern" -> {
                val typeChild = pattern.getChildByFieldName("type")
                for (i in 0 until pattern.childCount) {
                    val child = pattern.getChild(i)
                    // Skip the type name (child field "type")
                    val isType =
                        if (typeChild != null && !typeChild.isNull) {
                            child.startByte == typeChild.startByte &&
                                child.endByte == typeChild.endByte
                        } else {
                            false
                        }

                    if (!isType && child.isNamed) {
                        vars += extractBindings(child)
                    }
                }
            }
            "tuple_pattern" -> {
                for (i in 0 until pattern.childCount) {
                    val child = pattern.getChild(i)
                    if (child.isNamed && child.type != "(" && child.type != ")") {
                        vars += extractBindings(child)
                    }
                }
            }
            else -> {
                // Generic fallback for other patterns (struct_pattern, etc.)
                // This might over-capture (e.g. enum variant names) if not careful,
                // but strictly pattern matching usually only binds variables or matches consts.
                // Assuming lowercase = var, uppercase = const/type is a heuristic we might need
                // later.
                // For now, recurse.
                for (i in 0 until pattern.childCount) {
                    val child = pattern.getChild(i)
                    if (child.isNamed) vars += extractBindings(child)
                }
            }
        }
        return vars
    }

    private fun handleExpressionStatement(node: TSNode): Statement {
        val child = node.getNamedChild(0) ?: return newEmptyStatement(rawNode = node)
        return if (
            child.type == "if_expression" ||
                child.type == "block" ||
                child.type == "return_expression" ||
                child.type == "while_expression" ||
                child.type == "loop_expression"
        ) {
            handle(child)
        } else {
            frontend.expressionHandler.handle(child)
        }
    }
}
