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
package de.fraunhofer.aisec.cpg.frontends.experimental.rust

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
            "raw_string_literal" -> handleRawStringLiteral(node)
            "unit_expression" -> newLiteral(null, objectType("()"), rawNode = node)
            "self" -> newReference("self", rawNode = node)
            "match_pattern" -> {
                // match_pattern wraps the actual pattern in a match arm
                val child = node.getNamedChild(0)
                if (child != null) handle(child)
                else newProblemExpression("Empty match pattern", rawNode = node)
            }

            "or_pattern" -> {
                // 1 | 2 | 3 — model as a list of alternatives
                val children = mutableListOf<Expression>()
                for (child in node.children) {
                    if (child.isNamed) {
                        val expr = handle(child) as? Expression
                        if (expr != null) children += expr
                    }
                }
                if (children.size == 1) {
                    children.first()
                } else {
                    // Use a binary operator chain with "|"
                    children.reduce { acc, expr ->
                        val op = newBinaryOperator("|", rawNode = node)
                        op.lhs = acc
                        op.rhs = expr
                        op
                    }
                }
            }

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
        // Rust integers can have suffixes (e.g. 1u32) and underscores (e.g. 1_000)
        val code = node.text()
        // Strip underscores and type suffixes
        val cleaned =
            code
                .replace("_", "")
                .removeSuffix("i8")
                .removeSuffix("i16")
                .removeSuffix("i32")
                .removeSuffix("i64")
                .removeSuffix("i128")
                .removeSuffix("isize")
                .removeSuffix("u8")
                .removeSuffix("u16")
                .removeSuffix("u32")
                .removeSuffix("u64")
                .removeSuffix("u128")
                .removeSuffix("usize")
        val value =
            when {
                cleaned.startsWith("0x") || cleaned.startsWith("0X") ->
                    cleaned.drop(2).toLongOrNull(16) ?: 0L

                cleaned.startsWith("0o") || cleaned.startsWith("0O") ->
                    cleaned.drop(2).toLongOrNull(8) ?: 0L

                cleaned.startsWith("0b") || cleaned.startsWith("0B") ->
                    cleaned.drop(2).toLongOrNull(2) ?: 0L

                else -> cleaned.toLongOrNull() ?: 0L
            }
        val typeName =
            when {
                code.endsWith("u8") -> "u8"
                code.endsWith("u16") -> "u16"
                code.endsWith("u32") -> "u32"
                code.endsWith("u64") -> "u64"
                code.endsWith("u128") -> "u128"
                code.endsWith("usize") -> "usize"
                code.endsWith("i8") -> "i8"
                code.endsWith("i16") -> "i16"
                code.endsWith("i64") -> "i64"
                code.endsWith("i128") -> "i128"
                code.endsWith("isize") -> "isize"
                else -> "i32"
            }
        return newLiteral(value, primitiveType(typeName), rawNode = node)
    }

    private fun handleStringLiteral(node: TSNode): Literal<String> {
        val code = node.text()
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
        val code = node.text()
        val value = code == "true"
        return newLiteral(value, primitiveType("bool"), rawNode = node)
    }

    private fun handleIdentifier(node: TSNode): Reference {
        val name = node.text()
        return newReference(name, rawNode = node)
    }

    /** Translates a Rust `binary_expression` (e.g., `a + b`, `x == y`) into a [BinaryOperator]. */
    private fun handleBinaryExpression(node: TSNode): BinaryOperator {
        val left = node["left"]
        val right = node["right"]
        val operator = node["operator"]

        val op = newBinaryOperator(operator.text(), rawNode = node)
        if (left != null) op.lhs = handle(left) as Expression
        if (right != null) op.rhs = handle(right) as Expression

        return op
    }

    private fun handleUnaryExpression(node: TSNode): UnaryOperator {
        val operator = node.getChild(0) // Usually anonymous
        val operand = node["operand"] ?: node.getNamedChild(0)

        val opCode = operator.text()
        val op = newUnaryOperator(opCode, postfix = false, prefix = true, rawNode = node)
        if (operand != null)
            op.input =
                handle(operand) as? Expression ?: newProblemExpression("Operand not an expression")
        return op
    }

    private fun handleAssignmentExpression(node: TSNode): Statement {
        val left = node["left"]
        val right = node["right"]

        val lhs =
            handle(left ?: return newProblemExpression("Missing LHS in assignment")) as Expression
        val rhs =
            handle(right ?: return newProblemExpression("Missing RHS in assignment")) as Expression

        return newAssignExpression(
            operatorCode = "=",
            lhs = listOf(lhs),
            rhs = listOf(rhs),
            rawNode = node,
        )
    }

    private fun handleCompoundAssignmentExpression(node: TSNode): Statement {
        val left = node["left"]
        val operator = node["operator"]
        val right = node["right"]

        val lhs =
            handle(left ?: return newProblemExpression("Missing LHS in assignment")) as Expression
        val rhs =
            handle(right ?: return newProblemExpression("Missing RHS in assignment")) as Expression
        val opCode = operator.text()

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
        ile.initializers =
            node.children
                .filter { it.isNamed }
                .mapNotNull { handle(it) as? Expression }
                .toMutableList()
        return ile
    }

    private fun handleArrayExpression(node: TSNode): InitializerListExpression {
        val ile = newInitializerListExpression(rawNode = node)
        ile.type = objectType("array")
        val list = mutableListOf<Expression>()
        for (child in node.children) {
            if (child.isNamed) {
                val expr = handle(child) as? Expression
                if (expr != null) list += expr
            }
        }
        ile.initializers = list
        return ile
    }

    /**
     * Translates a Rust `call_expression` into a [CallExpression] or [MemberCallExpression]. If the
     * callee is a `generic_function` (turbofish syntax like `foo::<T>()`), delegates to
     * [handleGenericCallExpression].
     */
    private fun handleCallExpression(node: TSNode): Expression {
        val function =
            node["function"]
                ?: return newProblemExpression("Missing function in call", rawNode = node)
        val arguments = node["arguments"]

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
            for (arg in arguments.children) {
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
        // generic_function always has "function" and "type_arguments" fields per grammar
        val innerFunction = genericFunction["function"]!!
        val typeArguments = genericFunction["type_arguments"]!!

        val callee =
            handle(innerFunction) as? Expression
                ?: newProblemExpression("Invalid generic function", rawNode = genericFunction)

        val call = newCallExpression(callee, template = true, rawNode = node)

        // Add type arguments as template parameters
        for (typeArg in typeArguments.children) {
            if (typeArg.isNamed) {
                val typeName = typeArg.text()
                val type = frontend.typeHandler.handle(typeArg)
                val typeExpr = newTypeExpression(typeName, type, rawNode = typeArg)
                call.addTemplateParameter(typeExpr)
            }
        }

        // Add arguments
        if (arguments != null) {
            for (arg in arguments.children) {
                if (arg.isNamed) {
                    val expr = handle(arg) as? Expression
                    if (expr != null) call.addArgument(expr)
                }
            }
        }

        return call
    }

    private fun handleFieldExpression(node: TSNode): Statement {
        val value = node["value"]
        val field = node["field"]

        val base =
            handle(
                value
                    ?: return newProblemExpression(
                        "Missing value in field expression",
                        rawNode = node,
                    )
            )
                as Expression
        val name = field.text()

        return newMemberExpression(name, base, rawNode = node)
    }

    /**
     * Translates a Rust `match_expression` into a [SwitchStatement]. Each match arm becomes a
     * [CaseStatement] with its pattern as the case expression. Match guards are modeled as binary
     * `"if"` operators combining the pattern and guard condition.
     */
    private fun handleMatchExpression(node: TSNode): Statement {
        val value = node["value"]
        val body = node["body"]

        val switch = newSwitchStatement(rawNode = node)
        if (value != null) switch.selector = handle(value) as? Expression

        val block = newBlock().implicit()
        if (body != null) {
            for (arm in body.children) {
                if (arm.type == "match_arm") {
                    val pattern = arm["pattern"]
                    val armValue = arm["value"]

                    // Check for match guard: first as a separate child node,
                    // then as an embedded "if" inside the match_pattern
                    var guardNode: TSNode? = null
                    for (c in arm.children) {
                        if (c.type == "if_clause" || c.type == "match_guard") {
                            guardNode = c
                            break
                        }
                    }

                    // In tree-sitter-rust, the guard is embedded inside match_pattern:
                    // match_pattern -> [pattern, "if", guard_expression]
                    var embeddedGuard: TSNode? = null
                    if (guardNode == null && pattern != null && !pattern.isNull) {
                        var foundIf = false
                        for (k in 0 until pattern.childCount) {
                            val pc = pattern.getChild(k)
                            if (pc.type == "if") {
                                foundIf = true
                            } else if (foundIf && pc.isNamed) {
                                embeddedGuard = pc
                                break
                            }
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
                        val condition = guardNode.getNamedChild(0)
                        if (condition != null && !condition.isNull) {
                            val guardExpr = handle(condition) as? Expression
                            if (caseExpr != null && guardExpr != null) {
                                val op = newBinaryOperator("if", rawNode = guardNode)
                                op.lhs = caseExpr
                                op.rhs = guardExpr
                                caseExpr = op
                            }
                        }
                    } else if (embeddedGuard != null) {
                        val guardExpr = handle(embeddedGuard) as? Expression
                        if (caseExpr != null && guardExpr != null) {
                            val op = newBinaryOperator("if", rawNode = arm)
                            op.lhs = caseExpr
                            op.rhs = guardExpr
                            caseExpr = op
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
        val macroNode = node["macro"]
        val name = macroNode.text()

        val call =
            newCallExpression(newReference(name, rawNode = macroNode), fqn = name, rawNode = node)

        // Extract arguments from the token_tree (tree-sitter doesn't parse macro bodies,
        // but we can extract top-level named children as arguments)
        for (child in node.children) {
            if (child.type == "token_tree") {
                for (arg in child.children) {
                    if (arg.isNamed && arg.type != "token_tree") {
                        val expr = handle(arg) as? Expression
                        if (expr != null) call.addArgument(expr)
                    }
                }
                break
            }
        }

        return call
    }

    private fun handleLetCondition(node: TSNode): Expression {
        // let_condition always has "pattern" and "value" fields per grammar
        val pattern = node["pattern"]!!
        val value = node["value"]!!

        val lhs = handle(pattern) as Expression
        val rhs = handle(value) as Expression

        val op = newBinaryOperator("let", rawNode = node)
        op.lhs = lhs
        op.rhs = rhs
        return op
    }

    private fun handleBreakExpression(node: TSNode): Statement {
        val breakStmt = newBreakStatement(rawNode = node)
        val label = findLabel(node)

        if (label != null) {
            val code = label.text()
            breakStmt.label = code.removePrefix("'")
        }
        return breakStmt
    }

    private fun handleContinueExpression(node: TSNode): Statement {
        val continueStmt = newContinueStatement(rawNode = node)
        val label = findLabel(node)

        if (label != null) {
            val code = label.text()
            continueStmt.label = code.removePrefix("'")
        }
        return continueStmt
    }

    private fun handleAwaitExpression(node: TSNode): Expression {
        // await_expression always has the awaited expression as its first named child
        val expr = node.getNamedChild(0)
        val op = newUnaryOperator("await", postfix = true, prefix = false, rawNode = node)
        op.input =
            handle(expr) as? Expression
                ?: newProblemExpression("Invalid await operand", rawNode = node)
        return op
    }

    /**
     * Translates a Rust `struct_expression` (e.g., `Point { x: 1, y: 2 }`) into a
     * [ConstructExpression]. Supports field initializers, shorthand initializers (`{ x }`), and
     * base field initializers (`..other`).
     */
    private fun handleStructExpression(node: TSNode): Expression {
        val nameNode = node["name"]
        val name = nameNode.text()

        val construct = newConstructExpression(name, rawNode = node)
        construct.type = objectType(name)

        // struct_expression always has "body" (field_initializer_list) per grammar
        val body = node["body"]!!
        for (child in body.children) {
            when (child.type) {
                "field_initializer" -> {
                    // field_initializer always has "field" and "value" per grammar
                    val fieldName = child["field"]!!
                    val value = child["value"]!!
                    val valExpr =
                        handle(value) as? Expression
                            ?: newProblemExpression("Invalid field value", rawNode = value)
                    construct.addArgument(valExpr, fieldName.text())
                }

                "shorthand_field_initializer" -> {
                    // { x } is shorthand for { x: x }
                    val fieldName = child.text()
                    val ref = newReference(fieldName, rawNode = child)
                    construct.addArgument(ref, fieldName)
                }

                "base_field_initializer" -> {
                    // ..other_struct spread — always has the spread expression
                    val expr = child.getNamedChild(0)
                    construct.addArgument(
                        handle(expr) as? Expression
                            ?: newProblemExpression("Invalid base", rawNode = expr),
                        "..",
                    )
                }
            }
        }

        return construct
    }

    private fun handleIndexExpression(node: TSNode): Expression {
        // index_expression always has exactly 2 named children: base and index
        val ase = newSubscriptExpression(rawNode = node)
        val base = node.getNamedChild(0)
        val index = node.getNamedChild(1)
        ase.arrayExpression =
            handle(base) as? Expression
                ?: newProblemExpression("Invalid array base", rawNode = base)
        ase.subscriptExpression =
            handle(index) as? Expression ?: newProblemExpression("Invalid index", rawNode = index)
        return ase
    }

    private fun handleRangeExpression(node: TSNode): Expression {
        // Range can have 0, 1, or 2 named children depending on form (..end, start.., start..end)
        var floor: Expression? = null
        var ceiling: Expression? = null

        // Tree-sitter-rust range_expression: children are [start], operator (.., ..=), [end]
        // We need to determine which children are start/end based on position relative to operator
        var operatorIdx = -1
        for (child in node.children) {
            val code = child.text()
            if (code == ".." || code == "..=") {
                operatorIdx = node.children.indexOf(child)
                break
            }
        }

        for (child in node.children) {
            if (child.isNamed) {
                val expr = handle(child) as? Expression
                if (node.children.indexOf(child) < operatorIdx) {
                    floor = expr
                } else {
                    ceiling = expr
                }
            }
        }

        return newRangeExpression(floor, ceiling, rawNode = node)
    }

    private fun handleTryExpression(node: TSNode): Expression {
        // try_expression always has the operand as its first named child: expr '?'
        val op = newUnaryOperator("?", postfix = true, prefix = false, rawNode = node)
        val operand = node.getNamedChild(0)
        op.input =
            handle(operand) as? Expression
                ?: newProblemExpression("Invalid try operand", rawNode = node)
        return op
    }

    private fun handleTypeCastExpression(node: TSNode): Expression {
        // type_cast_expression always has "value" and "type" fields per grammar
        val cast = newCastExpression(rawNode = node)
        val value = node["value"]!!
        val type = node["type"]!!
        cast.expression =
            handle(value) as? Expression
                ?: newProblemExpression("Invalid cast operand", rawNode = value)
        cast.castType = frontend.typeOf(type)
        return cast
    }

    /**
     * Translates a Rust `closure_expression` (e.g., `|x| x + 1` or `move |x: i32| -> i32 { x }`)
     * into a [LambdaExpression] wrapping an anonymous [FunctionDeclaration].
     */
    private fun handleClosureExpression(node: TSNode): Expression {
        val lambda = newLambdaExpression(rawNode = node)

        // Create an anonymous function for the closure body
        val func = newFunctionDeclaration("", rawNode = node)
        frontend.scopeManager.enterScope(func)

        // Parse closure parameters
        val params = node["parameters"]
        if (params != null && !params.isNull) {
            for (child in params.children) {
                if (child.type == "parameter") {
                    val pattern = child["pattern"]
                    val pName = pattern.text()
                    val typeNode = child["type"]
                    val param =
                        newParameterDeclaration(pName, frontend.typeOf(typeNode), rawNode = child)
                    frontend.scopeManager.addDeclaration(param)
                    func.parameters += param
                } else if (child.isNamed && child.type == "identifier") {
                    // Simple closure param without type: |x| x + 1
                    val pName = child.text()
                    val param = newParameterDeclaration(pName, unknownType(), rawNode = child)
                    frontend.scopeManager.addDeclaration(param)
                    func.parameters += param
                }
            }
        }

        // Parse return type if present
        val returnType = node["return_type"]
        if (returnType != null && !returnType.isNull) {
            func.returnTypes = listOf(frontend.typeOf(returnType))
        }

        // Parse body - can be a block or a single expression
        val body = node["body"]
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
        val code = node.text()
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
        val code = node.text()
        val valueStr =
            code.filter { it.isDigit() || it == '.' || it == '-' || it == 'e' || it == 'E' }
        val value = valueStr.toDoubleOrNull() ?: 0.0
        return newLiteral(value, primitiveType("f64"), rawNode = node)
    }

    private fun handleCharLiteral(node: TSNode): Expression {
        val code = node.text()
        val value = code.removeSurrounding("'")
        return newLiteral(value, primitiveType("char"), rawNode = node)
    }

    private fun handleScopedIdentifier(node: TSNode): Expression {
        // path::to::item - model as a Reference with full qualified name
        val name = node.text()
        return newReference(name, rawNode = node)
    }

    private fun handleReferenceExpression(node: TSNode): Expression {
        // reference_expression always has a "value" field per grammar
        val value = node["value"]!!

        // Check for mutable reference (&mut x)
        var isMut = false
        for (child in node.children) {
            if (child.type == "mutable_specifier") {
                isMut = true
                break
            }
        }

        val opCode = if (isMut) "&mut" else "&"
        val op = newUnaryOperator(opCode, postfix = false, prefix = true, rawNode = node)
        op.input =
            handle(value) as? Expression
                ?: newProblemExpression("Invalid reference operand", rawNode = node)
        return op
    }

    private fun handleUnsafeBlock(node: TSNode): Expression {
        // unsafe_block: 'unsafe' block — no field name, so we pass the whole node
        // to handleBlock which iterates named children and finds the inner block
        val block = frontend.statementHandler.handleBlock(node)
        block.annotations += newAnnotation("unsafe", rawNode = node)
        return block
    }

    private fun handleAsyncBlock(node: TSNode): Expression {
        // async_block: 'async' [move] block — no field name, so we pass the whole node
        val block = frontend.statementHandler.handleBlock(node)
        block.annotations += newAnnotation("async", rawNode = node)
        return block
    }

    private fun handleRawStringLiteral(node: TSNode): Expression {
        val code = node.text()
        // Strip r#"..."# delimiters: remove leading r, then strip matching # and " pairs
        val withoutR = code.removePrefix("r")
        val hashes = withoutR.takeWhile { it == '#' }.length
        val delimiter = "#".repeat(hashes) + "\""
        val value = withoutR.removePrefix(delimiter).removeSuffix("\"" + "#".repeat(hashes))
        return newLiteral(value, primitiveType("str"), rawNode = node)
    }

    /**
     * Finds a `loop_label` or `label` child in the given node. Tree-sitter-rust does not expose
     * labels as a named field for break/continue, so we search through children directly.
     */
    private fun findLabel(node: TSNode): TSNode? {
        for (child in node.children) {
            if (child.type == "loop_label" || child.type == "label") {
                return child
            }
        }
        return null
    }

    private fun handleGenericFunctionReference(node: TSNode): Expression {
        // Fallback for generic_function outside of a call context
        val innerFunction = node["function"]
        val name = innerFunction.text().ifEmpty { node.text() }
        return newReference(name, rawNode = node)
    }
}
