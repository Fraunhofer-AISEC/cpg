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
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import org.treesitter.TSNode

/**
 * A [Handler] that translates Rust expressions into CPG [Expression] nodes. It currently supports
 * literals, binary and unary operations, function calls, assignments, and match expressions.
 */
class ExpressionHandler(frontend: RustLanguageFrontend) :
    RustHandler<Statement, TSNode>(::ProblemExpression, frontend) {

    override fun handleNode(node: TSNode): Statement {
        return when (node.type) {
            "integer_literal" -> handleIntegerLiteral(node)
            "string_literal" -> handleStringLiteral(node)
            "boolean_literal" -> handleBooleanLiteral(node)
            "identifier" -> handleIdentifier(node)
            "binary_expression" -> handleBinaryExpression(node)
            "call_expression" -> handleCallExpression(node)
            "field_expression" -> handleFieldExpression(node)
            "if_expression" -> frontend.statementHandler.handleNode(node)
            "block" -> frontend.statementHandler.handleBlock(node)
            "unary_expression" -> handleUnaryExpression(node)
            "assignment_expression" -> handleAssignmentExpression(node)
            "compound_assignment_expr" -> handleCompoundAssignmentExpression(node)
            "tuple_expression" -> handleTupleExpression(node)
            "array_expression" -> handleArrayExpression(node)
            "match_expression" -> handleMatchExpression(node)
            "macro_invocation" -> handleMacroInvocation(node)
            "let_condition" -> handleLetCondition(node)
            "break_expression" -> handleBreakExpression(node)
            "continue_expression" -> handleContinueExpression(node)
            "await_expression" -> handleAwaitExpression(node)
            "parenthesized_expression" -> {
                val child = node.getNamedChild(0)
                if (child != null) handle(child)
                else newProblemExpression("Empty parenthesized expression", rawNode = node)
            }
            else -> {
                newProblemExpression("Unknown expression type: ${node.type}", rawNode = node)
            }
        }
    }

    private fun handleIntegerLiteral(node: TSNode): Literal<Long> {
        val code = frontend.codeOf(node) ?: ""
        // Rust integers can have suffixes (e.g. 1u32) and underscores (e.g. 1_000)
        val valueStr = code.filter { it.isDigit() || it == '-' }
        val value = valueStr.toLongOrNull() ?: 0L
        return newLiteral(value, primitiveType("i32"), rawNode = node)
    }

    private fun handleStringLiteral(node: TSNode): Literal<String> {
        val code = frontend.codeOf(node) ?: ""
        val value = code.trim('"')
        return newLiteral(value, primitiveType("str"), rawNode = node)
    }

    private fun handleBooleanLiteral(node: TSNode): Literal<Boolean> {
        val code = frontend.codeOf(node) ?: ""
        val value = code == "true"
        return newLiteral(value, primitiveType("bool"), rawNode = node)
    }

    private fun handleIdentifier(node: TSNode): Reference {
        val name = frontend.codeOf(node) ?: ""
        return newReference(name, rawNode = node)
    }

    private fun handleBinaryExpression(node: TSNode): BinaryOperator {
        val left = node.getChildByFieldName("left")
        val right = node.getChildByFieldName("right")
        val operator = node.getChildByFieldName("operator")

        val op = newBinaryOperator(operator?.let { frontend.codeOf(it) } ?: "", rawNode = node)
        if (left != null)
            op.lhs = handle(left) as? Expression ?: newProblemExpression("LHS not an expression")
        if (right != null)
            op.rhs = handle(right) as? Expression ?: newProblemExpression("RHS not an expression")

        return op
    }

    private fun handleUnaryExpression(node: TSNode): UnaryOperator {
        val operator = node.getChild(0) // Usually anonymous
        val operand = node.getChildByFieldName("operand") ?: node.getNamedChild(0)

        val opCode = operator.let { frontend.codeOf(it) } ?: ""
        val op = newUnaryOperator(opCode, postfix = false, prefix = true, rawNode = node)
        if (operand != null)
            op.input =
                handle(operand) as? Expression ?: newProblemExpression("Operand not an expression")
        return op
    }

    private fun handleAssignmentExpression(node: TSNode): Statement {
        val left = node.getChildByFieldName("left")
        val right = node.getChildByFieldName("right")

        val lhs =
            handle(left ?: return newProblemExpression("Missing LHS in assignment")) as? Expression
                ?: return newProblemExpression("LHS not an expression")
        val rhs =
            handle(right ?: return newProblemExpression("Missing RHS in assignment")) as? Expression
                ?: return newProblemExpression("RHS not an expression")

        return newAssignExpression(
            operatorCode = "=",
            lhs = listOf(lhs),
            rhs = listOf(rhs),
            rawNode = node,
        )
    }

    private fun handleCompoundAssignmentExpression(node: TSNode): Statement {
        val left = node.getChildByFieldName("left")
        val operator = node.getChildByFieldName("operator")
        val right = node.getChildByFieldName("right")

        val lhs =
            handle(left ?: return newProblemExpression("Missing LHS in assignment")) as? Expression
                ?: return newProblemExpression("LHS not an expression")
        val rhs =
            handle(right ?: return newProblemExpression("Missing RHS in assignment")) as? Expression
                ?: return newProblemExpression("RHS not an expression")
        val opCode = operator?.let { frontend.codeOf(it) } ?: ""

        return newAssignExpression(
            operatorCode = opCode,
            lhs = listOf(lhs),
            rhs = listOf(rhs),
            rawNode = node,
        )
    }

    private fun handleTupleExpression(node: TSNode): InitializerListExpression {
        val ile = newInitializerListExpression(rawNode = node)
        ile.type = objectType("tuple")
        val list = mutableListOf<Expression>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.isNamed) {
                val expr = handle(child) as? Expression
                if (expr != null) list += expr
            }
        }
        ile.initializers = list
        return ile
    }

    private fun handleArrayExpression(node: TSNode): InitializerListExpression {
        val ile = newInitializerListExpression(rawNode = node)
        ile.type = objectType("array")
        val list = mutableListOf<Expression>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.isNamed) {
                val expr = handle(child) as? Expression
                if (expr != null) list += expr
            }
        }
        ile.initializers = list
        return ile
    }

    private fun handleCallExpression(node: TSNode): Expression {
        val function = node.getChildByFieldName("function")
        val arguments = node.getChildByFieldName("arguments")

        val callee =
            handle(
                function ?: return newProblemExpression("Missing function in call", rawNode = node)
            )
                as? Expression ?: newProblemExpression("Missing function in call", rawNode = node)

        val call =
            if (callee is MemberExpression) {
                newMemberCallExpression(callee, rawNode = node)
            } else {
                newCallExpression(callee, rawNode = node)
            }

        if (arguments != null) {
            for (i in 0 until arguments.childCount) {
                val arg = arguments.getChild(i)
                if (arg.isNamed) {
                    val expr = handle(arg) as? Expression
                    if (expr != null) call.addArgument(expr)
                }
            }
        }

        return call
    }

    private fun handleFieldExpression(node: TSNode): Statement {
        val value = node.getChildByFieldName("value")
        val field = node.getChildByFieldName("field")

        val base =
            handle(
                value
                    ?: return newProblemExpression(
                        "Missing value in field expression",
                        rawNode = node,
                    )
            )
                as? Expression
                ?: return newProblemExpression("Missing value in field expression", rawNode = node)
        val name = field?.let { frontend.codeOf(it) } ?: ""

        return newMemberExpression(name, base, rawNode = node)
    }

    private fun handleMatchExpression(node: TSNode): Statement {
        val value = node.getChildByFieldName("value")
        val body = node.getChildByFieldName("body")

        val switch = newSwitchStatement(rawNode = node)
        if (value != null) switch.selector = handle(value) as? Expression

        val block = newBlock().implicit()
        if (body != null) {
            for (i in 0 until body.childCount) {
                val arm = body.getChild(i)
                if (arm.type == "match_arm") {
                    val pattern = arm.getChildByFieldName("pattern")
                    val armValue = arm.getChildByFieldName("value")

                    var guardNode: TSNode? = null
                    for (k in 0 until arm.childCount) {
                        val c = arm.getChild(k)
                        if (c.type == "if_clause") {
                            guardNode = c
                            break
                        }
                    }

                    // Extract bindings from pattern
                    val bindings = frontend.statementHandler.extractBindings(pattern)

                    // Enter implicit scope for the arm (to support guard resolution)
                    val armScope = newBlock()
                    frontend.scopeManager.enterScope(armScope)
                    bindings.forEach { frontend.scopeManager.addDeclaration(it) }

                    val case = newCaseStatement(rawNode = arm)
                    var caseExpr = if (pattern != null) handle(pattern) as? Expression else null

                    if (guardNode != null) {
                        val condition = guardNode.getChildByFieldName("condition")
                        if (condition != null) {
                            val guardExpr = handle(condition) as? Expression
                            if (caseExpr != null && guardExpr != null) {
                                val op = newBinaryOperator("if", rawNode = guardNode)
                                op.lhs = caseExpr
                                op.rhs = guardExpr
                                caseExpr = op
                            }
                        }
                    }

                    case.caseExpression = caseExpr
                    block.statements += case

                    val stmt =
                        if (armValue != null) {
                            // handleBlockWithBindings will inject the declarations into the body
                            // block
                            // This ensures they are part of the AST
                            frontend.statementHandler.handleBlockWithBindings(armValue, bindings)
                        } else {
                            newEmptyStatement().implicit()
                        }
                    block.statements += stmt

                    // Add implicit break
                    block.statements += newBreakStatement().implicit()

                    frontend.scopeManager.leaveScope(armScope)
                }
            }
        }
        switch.statement = block

        return switch
    }

    private fun handleMacroInvocation(node: TSNode): Expression {
        val macroNode = node.getChildByFieldName("macro")
        val name = macroNode?.let { frontend.codeOf(it) } ?: ""

        // Treat macro call as a CallExpression for now
        val call =
            newCallExpression(newReference(name, rawNode = macroNode), fqn = name, rawNode = node)

        return call
    }

    private fun handleLetCondition(node: TSNode): Expression {
        val pattern = node.getChildByFieldName("pattern")
        val value = node.getChildByFieldName("value")

        val lhs =
            if (pattern != null) {
                // For now, treat pattern as an expression (e.g. Reference or specialized node)
                // We might need a PatternHandler later, but ExpressionHandler can handle basic
                // patterns usually
                handle(pattern) as? Expression
                    ?: newProblemExpression("Invalid pattern", rawNode = pattern)
            } else {
                newProblemExpression("Missing pattern", rawNode = node)
            }

        val rhs =
            if (value != null) {
                handle(value) as? Expression
                    ?: newProblemExpression("Invalid value", rawNode = value)
            } else {
                newProblemExpression("Missing value", rawNode = node)
            }

        val op = newBinaryOperator("let", rawNode = node)
        op.lhs = lhs
        op.rhs = rhs
        return op
    }

    private fun handleBreakExpression(node: TSNode): Statement {
        val breakStmt = newBreakStatement(rawNode = node)
        var label = node.getChildByFieldName("label")
        if (label == null || label.isNull) {
            for (i in 0 until node.childCount) {
                val c = node.getChild(i)
                if (c.type == "loop_label" || c.type == "label") {
                    label = c
                    break
                }
            }
        }

        if (label != null && !label.isNull) {
            val code = frontend.codeOf(label) ?: ""
            breakStmt.label = code.removePrefix("'")
        }
        return breakStmt
    }

    private fun handleContinueExpression(node: TSNode): Statement {
        val continueStmt = newContinueStatement(rawNode = node)
        var label = node.getChildByFieldName("label")
        if (label == null || label.isNull) {
            for (i in 0 until node.childCount) {
                val c = node.getChild(i)
                if (c.type == "loop_label" || c.type == "label") {
                    label = c
                    break
                }
            }
        }

        if (label != null && !label.isNull) {
            val code = frontend.codeOf(label) ?: ""
            continueStmt.label = code.removePrefix("'")
        }
        return continueStmt
    }

    private fun handleAwaitExpression(node: TSNode): Expression {
        var expr = node.getChildByFieldName("expression")
        if (expr == null || expr.isNull) {
            if (node.childCount > 0) {
                expr = node.getChild(0)
            }
        }

        val op = newUnaryOperator("await", postfix = true, prefix = false, rawNode = node)
        if (expr != null && !expr.isNull) {
            op.input =
                handle(expr) as? Expression
                    ?: newProblemExpression("Invalid await operand", rawNode = expr)
        } else {
            op.input = newProblemExpression("Missing await operand", rawNode = node)
        }
        return op
    }
}
