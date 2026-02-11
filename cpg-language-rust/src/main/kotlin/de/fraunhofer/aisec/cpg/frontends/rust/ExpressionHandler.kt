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
 * A [Handler] that translates Rust expressions into CPG [Expression] nodes. It supports literals,
 * binary and unary operations, function calls, assignments, match expressions, index expressions,
 * range expressions, type cast expressions, closure expressions, and more.
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
            "return_expression" -> frontend.statementHandler.handleNode(node)
            "while_expression" -> frontend.statementHandler.handleNode(node)
            "loop_expression" -> frontend.statementHandler.handleNode(node)
            "for_expression" -> frontend.statementHandler.handleNode(node)
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
            "reference_expression" -> handleReferenceExpression(node)
            "struct_expression" -> handleStructExpression(node)
            "method_call_expression" -> handleMethodCallExpression(node)
            "index_expression" -> handleIndexExpression(node)
            "range_expression" -> handleRangeExpression(node)
            "try_expression" -> handleTryExpression(node)
            "type_cast_expression" -> handleTypeCastExpression(node)
            "closure_expression" -> handleClosureExpression(node)
            "negative_literal" -> handleNegativeLiteral(node)
            "float_literal" -> handleFloatLiteral(node)
            "char_literal" -> handleCharLiteral(node)
            "scoped_identifier" -> handleScopedIdentifier(node)
            "generic_function" -> handleGenericFunctionReference(node)
            "unsafe_block" -> handleUnsafeBlock(node)
            "async_block" -> handleAsyncBlock(node)
            "tuple_index_expression" -> handleTupleIndexExpression(node)
            "raw_string_literal" -> handleRawStringLiteral(node)
            "unit_expression" -> newLiteral(null, objectType("()"), rawNode = node)
            "self" -> newReference("self", rawNode = node)
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
        return when {
            code.startsWith("b\"") -> {
                val value = code.removePrefix("b\"").removeSuffix("\"")
                newLiteral(value, objectType("&[u8]"), rawNode = node)
            }
            code.startsWith("c\"") -> {
                val value = code.removePrefix("c\"").removeSuffix("\"")
                newLiteral(value, objectType("&CStr"), rawNode = node)
            }
            else -> {
                val value = code.trim('"')
                newLiteral(value, primitiveType("str"), rawNode = node)
            }
        }
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
        val function =
            node.getChildByFieldName("function")
                ?: return newProblemExpression("Missing function in call", rawNode = node)
        val arguments = node.getChildByFieldName("arguments")

        // Detect turbofish / generic function call: identity::<i32>(42)
        // Tree-sitter AST: call_expression > function: generic_function
        if (function.type == "generic_function") {
            return handleGenericCallExpression(node, function, arguments)
        }

        val callee =
            handle(function) as? Expression
                ?: newProblemExpression("Missing function in call", rawNode = node)

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

    private fun handleGenericCallExpression(
        node: TSNode,
        genericFunction: TSNode,
        arguments: TSNode?,
    ): Expression {
        // generic_function has children: function (identifier/scoped_identifier) + type_arguments
        val innerFunction = genericFunction.getChildByFieldName("function")
        val typeArguments = genericFunction.getChildByFieldName("type_arguments")

        val callee =
            if (innerFunction != null && !innerFunction.isNull) {
                handle(innerFunction) as? Expression
                    ?: newProblemExpression("Invalid generic function", rawNode = genericFunction)
            } else {
                newProblemExpression("Missing function in generic call", rawNode = genericFunction)
            }

        val call = newCallExpression(callee, template = true, rawNode = node)

        // Add type arguments as template parameters
        if (typeArguments != null && !typeArguments.isNull) {
            for (i in 0 until typeArguments.childCount) {
                val typeArg = typeArguments.getChild(i)
                if (typeArg.isNamed) {
                    val typeName = frontend.codeOf(typeArg) ?: ""
                    val type = frontend.typeHandler.handle(typeArg)
                    val typeExpr = newTypeExpression(typeName, type, rawNode = typeArg)
                    call.addTemplateParameter(typeExpr)
                }
            }
        }

        // Add arguments
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

    private fun handleStructExpression(node: TSNode): Expression {
        val nameNode = node.getChildByFieldName("name")
        val name = if (nameNode != null && !nameNode.isNull) frontend.codeOf(nameNode) ?: "" else ""

        val construct = newConstructExpression(name, rawNode = node)
        construct.type = objectType(name)

        val body = node.getChildByFieldName("body")
        if (body != null && !body.isNull) {
            for (i in 0 until body.childCount) {
                val child = body.getChild(i)
                when (child.type) {
                    "field_initializer" -> {
                        val fieldName = child.getChildByFieldName("field")
                        val value = child.getChildByFieldName("value")
                        if (
                            fieldName != null && value != null && !fieldName.isNull && !value.isNull
                        ) {
                            val valExpr =
                                handle(value) as? Expression
                                    ?: newProblemExpression("Invalid field value", rawNode = value)
                            construct.addArgument(valExpr, frontend.codeOf(fieldName) ?: "")
                        }
                    }
                    "shorthand_field_initializer" -> {
                        // { x } is shorthand for { x: x }
                        val fieldName = frontend.codeOf(child) ?: ""
                        val ref = newReference(fieldName, rawNode = child)
                        construct.addArgument(ref, fieldName)
                    }
                    "base_field_initializer" -> {
                        // ..other_struct spread
                        val expr = child.getNamedChild(0)
                        if (expr != null && !expr.isNull) {
                            construct.addArgument(
                                handle(expr) as? Expression
                                    ?: newProblemExpression("Invalid base", rawNode = expr),
                                "..",
                            )
                        }
                    }
                }
            }
        }

        return construct
    }

    private fun handleMethodCallExpression(node: TSNode): Expression {
        val receiver = node.getNamedChild(0)
        val methodName = node.getChildByFieldName("name")
        val arguments = node.getChildByFieldName("arguments")

        val base =
            if (receiver != null && !receiver.isNull) {
                handle(receiver) as? Expression
                    ?: newProblemExpression("Invalid receiver", rawNode = receiver)
            } else {
                newProblemExpression("Missing receiver", rawNode = node)
            }

        val name =
            if (methodName != null && !methodName.isNull) frontend.codeOf(methodName) ?: "" else ""
        val member = newMemberExpression(name, base, rawNode = node)
        val call = newMemberCallExpression(member, rawNode = node)

        if (arguments != null && !arguments.isNull) {
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

    private fun handleIndexExpression(node: TSNode): Expression {
        val ase = newSubscriptExpression(rawNode = node)
        val base = node.getNamedChild(0)
        val index = node.getNamedChild(1)
        if (base != null && !base.isNull) {
            ase.arrayExpression =
                handle(base) as? Expression
                    ?: newProblemExpression("Invalid array base", rawNode = base)
        }
        if (index != null && !index.isNull) {
            ase.subscriptExpression =
                handle(index) as? Expression
                    ?: newProblemExpression("Invalid index", rawNode = index)
        }
        return ase
    }

    private fun handleRangeExpression(node: TSNode): Expression {
        // Range can have 0, 1, or 2 named children depending on form (..end, start.., start..end)
        var floor: Expression? = null
        var ceiling: Expression? = null

        // Tree-sitter-rust range_expression: children are [start], operator (.., ..=), [end]
        // We need to determine which children are start/end based on position relative to operator
        var operatorIdx = -1
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val code = frontend.codeOf(child) ?: ""
            if (code == ".." || code == "..=") {
                operatorIdx = i
                break
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.isNamed) {
                val expr = handle(child) as? Expression
                if (i < operatorIdx) {
                    floor = expr
                } else {
                    ceiling = expr
                }
            }
        }

        return newRangeExpression(floor, ceiling, rawNode = node)
    }

    private fun handleTryExpression(node: TSNode): Expression {
        // x? is modeled as a postfix unary operator "?"
        val op = newUnaryOperator("?", postfix = true, prefix = false, rawNode = node)
        val operand = node.getNamedChild(0)
        if (operand != null && !operand.isNull) {
            op.input =
                handle(operand) as? Expression
                    ?: newProblemExpression("Invalid try operand", rawNode = operand)
        }
        return op
    }

    private fun handleTypeCastExpression(node: TSNode): Expression {
        val cast = newCastExpression(rawNode = node)
        val value = node.getChildByFieldName("value")
        val type = node.getChildByFieldName("type")
        if (value != null && !value.isNull) {
            cast.expression =
                handle(value) as? Expression
                    ?: newProblemExpression("Invalid cast operand", rawNode = value)
        }
        if (type != null && !type.isNull) {
            cast.castType = frontend.typeOf(type)
        }
        return cast
    }

    private fun handleClosureExpression(node: TSNode): Expression {
        val lambda = newLambdaExpression(rawNode = node)

        // Create an anonymous function for the closure body
        val func = newFunctionDeclaration("", rawNode = node)
        frontend.scopeManager.enterScope(func)

        // Parse closure parameters
        val params = node.getChildByFieldName("parameters")
        if (params != null && !params.isNull) {
            for (i in 0 until params.childCount) {
                val child = params.getChild(i)
                if (child.type == "parameter") {
                    val pattern = child.getChildByFieldName("pattern")
                    val pName =
                        if (pattern != null && !pattern.isNull) frontend.codeOf(pattern) ?: ""
                        else ""
                    val typeNode = child.getChildByFieldName("type")
                    val param =
                        newParameterDeclaration(pName, frontend.typeOf(typeNode), rawNode = child)
                    frontend.scopeManager.addDeclaration(param)
                    func.parameters += param
                } else if (child.isNamed && child.type == "identifier") {
                    // Simple closure param without type: |x| x + 1
                    val pName = frontend.codeOf(child) ?: ""
                    val param = newParameterDeclaration(pName, unknownType(), rawNode = child)
                    frontend.scopeManager.addDeclaration(param)
                    func.parameters += param
                }
            }
        }

        // Parse return type if present
        val returnType = node.getChildByFieldName("return_type")
        if (returnType != null && !returnType.isNull) {
            func.returnTypes = listOf(frontend.typeOf(returnType))
        }

        // Parse body - can be a block or a single expression
        val body = node.getChildByFieldName("body")
        if (body != null && !body.isNull) {
            func.body =
                if (body.type == "block") {
                    frontend.statementHandler.handle(body)
                } else {
                    frontend.expressionHandler.handle(body)
                }
        }

        frontend.scopeManager.leaveScope(func)
        lambda.function = func
        return lambda
    }

    private fun handleNegativeLiteral(node: TSNode): Expression {
        // negative_literal wraps a numeric literal with unary minus
        val code = frontend.codeOf(node) ?: ""
        if (code.contains('.')) {
            val valueStr = code.filter { it.isDigit() || it == '-' || it == '.' }
            val value = valueStr.toDoubleOrNull() ?: 0.0
            return newLiteral(value, primitiveType("f64"), rawNode = node)
        } else {
            val valueStr = code.filter { it.isDigit() || it == '-' }
            val value = valueStr.toLongOrNull() ?: 0L
            return newLiteral(value, primitiveType("i32"), rawNode = node)
        }
    }

    private fun handleFloatLiteral(node: TSNode): Expression {
        val code = frontend.codeOf(node) ?: ""
        val valueStr =
            code.filter { it.isDigit() || it == '.' || it == '-' || it == 'e' || it == 'E' }
        val value = valueStr.toDoubleOrNull() ?: 0.0
        return newLiteral(value, primitiveType("f64"), rawNode = node)
    }

    private fun handleCharLiteral(node: TSNode): Expression {
        val code = frontend.codeOf(node) ?: ""
        val value = code.removeSurrounding("'")
        return newLiteral(value, primitiveType("char"), rawNode = node)
    }

    private fun handleScopedIdentifier(node: TSNode): Expression {
        // path::to::item - model as a Reference with full qualified name
        val name = frontend.codeOf(node) ?: ""
        return newReference(name, rawNode = node)
    }

    private fun handleReferenceExpression(node: TSNode): Expression {
        val value = node.getChildByFieldName("value")

        // Check for mutable reference (&mut x)
        var isMut = false
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.type == "mutable_specifier") {
                isMut = true
                break
            }
        }

        val opCode = if (isMut) "&mut" else "&"
        val op = newUnaryOperator(opCode, postfix = false, prefix = true, rawNode = node)
        if (value != null && !value.isNull) {
            op.input =
                handle(value) as? Expression
                    ?: newProblemExpression("Invalid reference operand", rawNode = value)
        }
        return op
    }

    private fun handleUnsafeBlock(node: TSNode): Expression {
        // unsafe_block has an inner "block" named child
        val blockNode = node.getChildByFieldName("block")
        val block =
            if (blockNode != null && !blockNode.isNull && blockNode.type == "block") {
                frontend.statementHandler.handleBlock(blockNode)
            } else {
                // Fallback: look for first named child that is a block
                var innerBlock: Block? = null
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child.type == "block") {
                        innerBlock = frontend.statementHandler.handleBlock(child)
                        break
                    }
                }
                innerBlock ?: frontend.statementHandler.handleBlock(node)
            }
        block.annotations += newAnnotation("unsafe", rawNode = node)
        return block
    }

    private fun handleAsyncBlock(node: TSNode): Expression {
        // async_block has an inner "block" named child
        val blockNode = node.getChildByFieldName("block")
        val block =
            if (blockNode != null && !blockNode.isNull && blockNode.type == "block") {
                frontend.statementHandler.handleBlock(blockNode)
            } else {
                // Fallback: look for first named child that is a block
                var innerBlock: Block? = null
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child.type == "block") {
                        innerBlock = frontend.statementHandler.handleBlock(child)
                        break
                    }
                }
                innerBlock ?: frontend.statementHandler.handleBlock(node)
            }
        block.annotations += newAnnotation("async", rawNode = node)
        return block
    }

    private fun handleTupleIndexExpression(node: TSNode): Expression {
        // tuple_index_expression: value.index (e.g., t.0)
        // First named child is the tuple expression
        val tuple = node.getNamedChild(0)

        val base =
            if (tuple != null && !tuple.isNull) {
                handle(tuple) as? Expression
                    ?: newProblemExpression("Invalid tuple base", rawNode = tuple)
            } else {
                newProblemExpression("Missing tuple", rawNode = node)
            }

        // Try to get the index via field name first, then fall back to extracting from code
        val index = node.getChildByFieldName("index")
        val indexName =
            if (index != null && !index.isNull) {
                frontend.codeOf(index) ?: "0"
            } else {
                // Fallback: extract the digit after the dot from the full code
                val code = frontend.codeOf(node) ?: ""
                val dotIdx = code.lastIndexOf('.')
                if (dotIdx >= 0) code.substring(dotIdx + 1).trim() else "0"
            }
        return newMemberExpression(indexName, base, rawNode = node)
    }

    private fun handleRawStringLiteral(node: TSNode): Expression {
        val code = frontend.codeOf(node) ?: ""
        // Strip r#"..."# delimiters: remove leading r, then strip matching # and " pairs
        val withoutR = code.removePrefix("r")
        val hashes = withoutR.takeWhile { it == '#' }.length
        val delimiter = "#".repeat(hashes) + "\""
        val value = withoutR.removePrefix(delimiter).removeSuffix("\"" + "#".repeat(hashes))
        return newLiteral(value, primitiveType("str"), rawNode = node)
    }

    private fun handleGenericFunctionReference(node: TSNode): Expression {
        // Fallback for generic_function outside of a call context
        val innerFunction = node.getChildByFieldName("function")
        val name =
            if (innerFunction != null && !innerFunction.isNull) frontend.codeOf(innerFunction) ?: ""
            else frontend.codeOf(node) ?: ""
        return newReference(name, rawNode = node)
    }
}
