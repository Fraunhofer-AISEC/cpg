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
import de.fraunhofer.aisec.cpg.graph.declarations.TupleDeclaration
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
            "block" -> {
                val block = handleBlock(node)
                wrapWithLabel(node, block)
            }
            "let_declaration" -> handleLetDeclaration(node)
            "return_expression" -> handleReturnExpression(node)
            "if_expression" -> {
                // Delegate to the expression handler which will produce a
                // ConditionalExpression when an else clause is present, or fall back
                // to handleIfExpression (IfStatement) when there is no else clause.
                frontend.expressionHandler.handleNode(node)
            }
            "while_expression" -> handleWhileExpression(node)
            "loop_expression" -> handleLoopExpression(node)
            "for_expression" -> handleForExpression(node)
            "expression_statement" -> handleExpressionStatement(node)
            "empty_statement" -> newEmptyStatement(rawNode = node)
            "function_item" -> {
                // Nested function inside a block -- delegate to declaration handler
                val decl = frontend.declarationHandler.handle(node)
                val declStmt = newDeclarationStatement(rawNode = node)
                declStmt.addDeclaration(decl)
                declStmt
            }
            else -> {
                // Fallback: delegate to expression handler for trailing expressions in blocks
                // (e.g. the last expression without semicolon is the block's return value)
                frontend.expressionHandler.handle(node)
            }
        }
    }

    /**
     * Translates a Rust `block` (e.g., `{ ... }`) into a [Block] containing its child statements.
     */
    internal fun handleBlock(node: TSNode): Block {
        val block = newBlock(rawNode = node)
        frontend.scopeManager.enterScope(block)

        for (child in node.children) {
            if (child.isNamed && child.type != "{" && child.type != "}" && child.type != "label") {
                block.statements += handle(child)
            }
        }

        frontend.scopeManager.leaveScope(block)
        return block
    }

    /**
     * Checks whether [node] has a `label` child and, if so, wraps [stmt] in a [LabelStatement].
     * Otherwise, returns [stmt] unchanged. This is used for labeled blocks (`'label: { ... }`).
     */
    internal fun wrapWithLabel(node: TSNode, stmt: Statement): Statement {
        var labelNode: TSNode? = null
        for (child in node.children) {
            if (child.type == "label") {
                labelNode = child
                break
            }
        }
        return if (labelNode != null) {
            val labelStmt = newLabelStatement(rawNode = node)
            val code = labelNode.text()
            // Labels look like 'outer: — strip the leading quote and trailing colon
            labelStmt.label = code.removePrefix("'").removeSuffix(":")
            labelStmt.subStatement = stmt
            labelStmt
        } else {
            stmt
        }
    }

    /**
     * Translates a Rust `let_declaration` into a [DeclarationStatement] containing a
     * [VariableDeclaration]. Handles `mut` patterns, type annotations, and tuple destructuring.
     */
    private fun handleLetDeclaration(node: TSNode): Statement {
        val patternNode = node["pattern"]

        // Handle tuple destructuring: let (a, b) = ...
        if (patternNode != null && patternNode.type == "tuple_pattern") {
            return handleTupleLetDeclaration(node, patternNode)
        }

        val declStmt = newDeclarationStatement(rawNode = node)

        // Check for `mut` keyword in the pattern
        var isMutable = false
        var actualName = ""
        if (patternNode != null) {
            if (patternNode.type == "mut_pattern") {
                isMutable = true
                // The actual identifier is a child of mut_pattern
                val innerPattern = patternNode.getNamedChild(0) ?: patternNode["pattern"]
                actualName =
                    if (innerPattern != null && !innerPattern.isNull) {
                        innerPattern.text()
                    } else {
                        // Fallback: use full code minus "mut "
                        patternNode.text().removePrefix("mut ").trim()
                    }
            } else {
                actualName = patternNode.text()
            }
        }

        // Also check for a standalone "mutable_specifier" child of the let_declaration
        for (child in node.children) {
            if (child.type == "mutable_specifier") {
                isMutable = true
                break
            }
        }

        val variable = newVariableDeclaration(actualName, rawNode = node)
        if (isMutable) {
            variable.modifiers += "mut"
        }

        val typeNode = node["type"]
        if (typeNode != null) {
            variable.type = frontend.typeOf(typeNode)
        }

        // Determine the value node and check if it is a labeled block
        var valueNode = node["value"]
        if (valueNode != null && valueNode.isNull) {
            valueNode = null
        }

        if (valueNode != null && !valueNode.isNull) {
            variable.initializer = frontend.expressionHandler.handle(valueNode) as? Expression
        }

        frontend.scopeManager.addDeclaration(variable)
        declStmt.addDeclaration(variable)

        // If the value was a labeled block, wrap the declaration in a LabelStatement
        if (valueNode != null && !valueNode.isNull && valueNode.type == "block") {
            val wrapped = wrapWithLabel(valueNode, declStmt)
            if (wrapped !== declStmt) {
                return wrapped
            }
        }

        return declStmt
    }

    /**
     * Recursively builds a [TupleDeclaration] from a Rust `tuple_pattern`, which can be nested
     * (e.g., `let ((a, b), c) = ...`). The initializer is only set for the top-level tuple; nested
     * tuples will have null initializers since Rust does not allow separate initializers for nested
     * patterns.
     */
    fun buildTuple(pattern: TSNode, initializer: Expression?): TupleDeclaration {
        val elements = mutableListOf<VariableDeclaration>()
        for (child in pattern.children) {
            if (!child.isNamed) continue
            when (child.type) {
                "tuple_pattern" -> {
                    val nested = buildTuple(child, null)
                    elements += nested
                }
                "identifier" -> {
                    val variable = newVariableDeclaration(child.text(), rawNode = child)
                    elements += variable
                }
            }
        }

        val tuple = newTupleDeclaration(elements, initializer, rawNode = pattern)
        // Add tuple and its elements to scope
        frontend.scopeManager.addDeclaration(tuple)
        elements.forEach { frontend.scopeManager.addDeclaration(it) }
        return tuple
    }

    /**
     * Translates a Rust tuple-destructuring `let` (e.g., `let (a, b) = tuple`) into a
     * [DeclarationStatement] containing a [TupleDeclaration].
     */
    private fun handleTupleLetDeclaration(node: TSNode, patternNode: TSNode): DeclarationStatement {
        val declStmt = newDeclarationStatement(rawNode = node)

        val valueNode = node["value"]
        val initializer =
            if (valueNode != null) {
                frontend.expressionHandler.handle(valueNode) as? Expression
            } else {
                null
            }

        val topLevelTuple = buildTuple(patternNode, initializer)

        // Register all tuple declarations (nested + top-level) in this statement
        fun addTupleDecls(tuple: TupleDeclaration) {
            tuple.elements.filterIsInstance<TupleDeclaration>().forEach { addTupleDecls(it) }
            declStmt.addDeclaration(tuple)
        }

        addTupleDecls(topLevelTuple)
        return declStmt
    }

    /**
     * Translates a Rust `return_expression` into a [ReturnStatement] with an optional return value.
     */
    private fun handleReturnExpression(node: TSNode): ReturnStatement {
        val ret = newReturnStatement(rawNode = node)
        // In Rust return_expression, the value is often a child
        for (child in node.children) {
            if (child.isNamed && child.type != "return") {
                ret.returnValue = frontend.expressionHandler.handle(child) as? Expression
                break
            }
        }
        return ret
    }

    /**
     * Translates a Rust `if_expression` into an [IfStatement]. Supports `if let` bindings (which
     * inject variable declarations into the then-branch) and `else if` chains.
     *
     * This method is used for `if` expressions that do **not** have an `else` clause and therefore
     * cannot be used in value position. When an `else` clause is present, the [ExpressionHandler]
     * produces a [de.fraunhofer.aisec.cpg.graph.statements.expressions.ConditionalExpression]
     * instead.
     */
    internal fun handleIfExpression(node: TSNode): IfStatement {
        val ifStmt = newIfStatement(rawNode = node)

        var condition = node["condition"]
        if (condition != null && condition.isNull) condition = null

        if (condition != null) {
            ifStmt.condition = frontend.expressionHandler.handle(condition) as? Expression
        }

        // Check for bindings (if let)
        val bindings =
            if (condition != null && condition.type == "let_condition") {
                extractBindings(condition["pattern"])
            } else {
                emptyList<VariableDeclaration>()
            }

        val consequence = node["consequence"]
        if (consequence != null && !consequence.isNull) {
            ifStmt.thenStatement =
                if (bindings.isNotEmpty()) {
                    handleBlockWithBindings(consequence, bindings)
                } else {
                    handle(consequence)
                }
        }

        val alternative = node["alternative"]
        if (alternative != null && !alternative.isNull) {
            // Alternative can be another if_expression or a block
            // In Rust Tree-sitter, alternative is often an 'else_clause' node
            val elseNode =
                if (alternative.type == "else_clause") {
                    // Search for a named child in else_clause
                    var found: TSNode? = null
                    for (c in alternative.children) {
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

    /**
     * Translates a Rust `while_expression` (including `while let`) into a [WhileStatement]. If a
     * loop label is present, wraps the result in a [LabelStatement].
     */
    private fun handleWhileExpression(node: TSNode): Statement {
        val whileStmt = newWhileStatement(rawNode = node)

        var condition = node["condition"]
        if (condition != null && condition.isNull) condition = null

        if (condition != null) {
            whileStmt.condition = frontend.expressionHandler.handle(condition) as? Expression
        }

        // Check for bindings (while let)
        val bindings =
            if (condition != null && condition.type == "let_condition") {
                extractBindings(condition["pattern"])
            } else {
                emptyList<VariableDeclaration>()
            }

        val body = node["body"]
        if (body != null && !body.isNull) {
            whileStmt.statement =
                if (bindings.isNotEmpty()) {
                    handleBlockWithBindings(body, bindings)
                } else {
                    handle(body)
                }
        }

        val label = findLoopLabel(node)

        return if (label != null) {
            val labelStmt = newLabelStatement(rawNode = node)
            val code = label.text()
            labelStmt.label = code.removePrefix("'")
            labelStmt.subStatement = whileStmt
            labelStmt
        } else {
            whileStmt
        }
    }

    /**
     * Translates a Rust `loop_expression` (infinite loop) into a [WhileStatement] with a `true`
     * condition. If a loop label is present, wraps the result in a [LabelStatement].
     */
    private fun handleLoopExpression(node: TSNode): Statement {
        val loop = newWhileStatement(rawNode = node)
        // Infinite loop: while(true)
        loop.condition = newLiteral(true, primitiveType("bool"), rawNode = node).implicit()

        val body = node["body"]
        if (body != null && !body.isNull) {
            loop.statement = handle(body)
        }

        val label = findLoopLabel(node)

        return if (label != null) {
            val labelStmt = newLabelStatement(rawNode = node)
            val code = label.text()
            labelStmt.label = code.removePrefix("'")
            labelStmt.subStatement = loop
            labelStmt
        } else {
            loop
        }
    }

    /**
     * Translates a Rust `for_expression` (e.g., `for x in items { ... }`) into a
     * [ForEachStatement]. The loop variable is declared in the for-each scope.
     */
    private fun handleForExpression(node: TSNode): Statement {
        val forEach = newForEachStatement(rawNode = node)
        frontend.scopeManager.enterScope(forEach)

        // Pattern is the loop variable (e.g., "x" in "for x in items")
        val pattern = node["pattern"]
        if (pattern != null && !pattern.isNull) {
            val declStmt = newDeclarationStatement(rawNode = pattern)
            val name = pattern.text()
            val variable = newVariableDeclaration(name, rawNode = pattern)
            frontend.scopeManager.addDeclaration(variable)
            declStmt.addDeclaration(variable)
            forEach.variable = declStmt
        }

        // Value is the iterable expression (e.g., "items" in "for x in items")
        val value = node["value"]
        if (value != null && !value.isNull) {
            forEach.iterable = frontend.expressionHandler.handle(value) as? Expression
        }

        // Body is the loop block
        val body = node["body"]
        if (body != null && !body.isNull) {
            forEach.statement = handle(body)
        }

        frontend.scopeManager.leaveScope(forEach)

        // Check for loop label
        val label = findLoopLabel(node)

        return if (label != null) {
            val labelStmt = newLabelStatement(rawNode = node)
            val code = label.text()
            labelStmt.label = code.removePrefix("'")
            labelStmt.subStatement = forEach
            labelStmt
        } else {
            forEach
        }
    }

    /**
     * Finds a `loop_label` or `label` child in the given node. Tree-sitter-rust does not expose
     * labels as a named field, so we search through children directly.
     */
    private fun findLoopLabel(node: TSNode): TSNode? {
        for (child in node.children) {
            if (child.type == "loop_label" || child.type == "label") {
                return child
            }
        }
        return null
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
            for (child in node.children) {
                if (
                    child.isNamed && child.type != "{" && child.type != "}" && child.type != "label"
                ) {
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
                val name = pattern.text()
                vars += newVariableDeclaration(name, rawNode = pattern)
            }
            "tuple_struct_pattern" -> {
                val typeChild = pattern["type"]
                for (child in pattern.children) {
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
                for (child in pattern.children) {
                    if (child.isNamed && child.type != "(" && child.type != ")") {
                        vars += extractBindings(child)
                    }
                }
            }
            "struct_pattern" -> {
                // Point { x, y } or Point { x: a, y: b }
                // Skip the type child, recurse into field_pattern children
                val typeChild = pattern["type"]
                for (child in pattern.children) {
                    val isType =
                        if (typeChild != null && !typeChild.isNull) {
                            child.startByte == typeChild.startByte &&
                                child.endByte == typeChild.endByte
                        } else {
                            false
                        }
                    if (!isType && child.isNamed && child.type != "remaining_field_pattern") {
                        vars += extractBindings(child)
                    }
                }
            }
            "field_pattern" -> {
                // Can be shorthand `x` (binds x) or `x: pattern` (binds from pattern)
                val nameNode = pattern["name"]
                val patternChild = pattern["pattern"]
                if (patternChild != null && !patternChild.isNull) {
                    vars += extractBindings(patternChild)
                } else if (nameNode != null && !nameNode.isNull) {
                    val name = nameNode.text()
                    vars += newVariableDeclaration(name, rawNode = nameNode)
                } else {
                    // Fallback: treat the whole node as a binding
                    val name = pattern.text()
                    if (name.isNotEmpty()) {
                        vars += newVariableDeclaration(name, rawNode = pattern)
                    }
                }
            }
            "slice_pattern" -> {
                // [first, .., last] — recurse into children, skip remaining_field_pattern
                for (child in pattern.children) {
                    if (child.isNamed && child.type != "remaining_field_pattern") {
                        vars += extractBindings(child)
                    }
                }
            }
            "or_pattern" -> {
                // 1 | 2 => bindings should be the same in all alternatives
                // Extract from first alternative only
                val firstChild = pattern.getNamedChild(0)
                if (firstChild != null) {
                    vars += extractBindings(firstChild)
                }
            }
            "ref_pattern" -> {
                // ref x — the binding is the inner pattern
                val inner = pattern.getNamedChild(0)
                if (inner != null) vars += extractBindings(inner)
            }
            "mut_pattern" -> {
                // mut x — the binding is the inner pattern
                val inner = pattern.getNamedChild(0)
                if (inner != null) vars += extractBindings(inner)
            }
            "remaining_field_pattern",
            "_",
            "integer_literal",
            "string_literal",
            "boolean_literal",
            "char_literal",
            "negative_literal",
            "float_literal" -> {
                // These don't bind any variables
            }
            else -> {
                // Generic fallback: recurse into named children
                for (child in pattern.children) {
                    if (child.isNamed) vars += extractBindings(child)
                }
            }
        }
        return vars
    }

    /**
     * Translates an `expression_statement` by delegating statement-like expressions (if, block,
     * return, loops) to the statement handler and all others to the expression handler.
     */
    private fun handleExpressionStatement(node: TSNode): Statement {
        val child = node.getNamedChild(0) ?: return newEmptyStatement(rawNode = node)
        return if (
            child.type == "if_expression" ||
                child.type == "block" ||
                child.type == "return_expression" ||
                child.type == "while_expression" ||
                child.type == "loop_expression" ||
                child.type == "for_expression"
        ) {
            handle(child)
        } else {
            frontend.expressionHandler.handle(child)
        }
    }
}
