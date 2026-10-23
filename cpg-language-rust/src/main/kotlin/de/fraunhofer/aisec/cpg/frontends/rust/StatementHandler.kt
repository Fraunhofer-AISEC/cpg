/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.expressions.*
import kotlin.collections.plusAssign
import uniffi.rustast.RsAst
import uniffi.rustast.RsBlockExpr
import uniffi.rustast.RsExpr
import uniffi.rustast.RsExprStmt
import uniffi.rustast.RsItem
import uniffi.rustast.RsLetStmt
import uniffi.rustast.RsPat
import uniffi.rustast.RsStmt

class StatementHandler(frontend: RustLanguageFrontend) :
    RustHandler<Expression, RsAst.RustStmt>(::ProblemExpression, frontend) {

    override fun handleNode(node: RsAst.RustStmt): Expression {
        val unwrapped = node.v1
        return handleNode(unwrapped)
    }

    fun handleNode(node: RsStmt): Expression {
        return when (node) {
            is RsStmt.LetStmt -> handleLetStmt(node.v1)
            is RsStmt.ExprStmt -> handleExprStmt(node.v1)
            is RsStmt.Item -> handleItem(node.v1)
            else ->
                newProblemExpression(
                    problem = "The statement of class ${node.javaClass} is not supported yet",
                    rawNode = RsAst.RustStmt(node),
                )
        }
    }

    /**
     * Handles a `let` statement. A `let-else` (see [handleLetElse]) is delegated separately; a
     * simple identifier pattern becomes a [DeclarationStatement] with a single [Variable]; any
     * other (destructuring) pattern is modeled as an [Assign] whose LHS is the deconstructed
     * pattern (see [PatternHandler]) and whose RHS is the initializer expression.
     *
     * AST: [ast::LetStmt](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.LetStmt.html)
     *
     * Reference:
     * [`let` statements](https://doc.rust-lang.org/reference/statements.html#let-statements)
     */
    fun handleLetStmt(letStmt: RsLetStmt): Expression {
        val raw = RsAst.RustStmt(RsStmt.LetStmt(letStmt))
        // for us, a let expression is an assigment with a deconstruction

        letStmt.letElse?.let {
            return handleLetElse(letStmt, it, raw)
        }

        // If the Pattern is a simple identity pattern we make it a declaration statement
        (letStmt.pat as? RsPat.IdentPat)?.let {
            val declarationStatement = newDeclarationStatement(rawNode = raw)

            val variable =
                newVariable(
                    name = it.v1.name ?: "",
                    type = letStmt.ty?.let { frontend.typeOf(it) } ?: unknownType(),
                    rawNode = raw,
                )

            letStmt.initializer?.let {
                variable.initializer = frontend.expressionHandler.handle(RsAst.RustExpr(it))

                // Here, if we have the classical pattern for initializers we set the base of the
                // contained member access. This part needs to be made more precise.
                val initializingExpressions =
                    when (variable.initializer) {
                        is Construction -> (variable.initializer as Construction).arguments
                        is InitializerList -> (variable.initializer as InitializerList).initializers
                        else -> listOf()
                    }
                initializingExpressions.forEach {
                    (it as? Assign)?.lhs?.forEach {
                        val targetRef = (it as? MemberAccess)?.base ?: it
                        (targetRef as? Reference)?.let {
                            if (it.name.toString() == "null") {
                                it.name = variable.name
                            }
                        }
                    }
                }
            }
            declarationStatement.declarations += variable

            frontend.scopeManager.addDeclaration(variable)
            return declarationStatement
        }

        val assign: Assign =
            newAssign(
                operatorCode = "=",
                lhs =
                    letStmt.pat?.let { listOf(frontend.patternHandler.handle(RsAst.RustPat(it))) }
                        ?: emptyList(),
                rhs =
                    letStmt.initializer?.let {
                        listOf(frontend.expressionHandler.handle(RsAst.RustExpr(it)))
                    } ?: emptyList(),
                rawNode = raw,
            )

        assign.usedAsExpression = true
        return assign
    }

    /**
     * Handles a `let PAT = expr else { diverge }` statement. It is modeled as an [Assign] whose RHS
     * is a synthetic [Switch] with one [Case] matching the pattern (its bindings are collected into
     * an [InitializerList] and returned via a [BreakStatement]) and a `default` case running the
     * diverging else-block; the LHS re-declares the pattern's bindings in the enclosing scope so
     * they stay alive after the statement.
     *
     * AST: [ast::LetStmt](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.LetStmt.html)
     * (`let_else` field)
     *
     * Reference:
     * [`let-else` statements](https://doc.rust-lang.org/reference/statements.html#let-else-statements)
     */
    fun handleLetElse(letStmt: RsLetStmt, blockExpr: RsBlockExpr, raw: RsAst.RustStmt): Expression {

        // The pattern is handled inside the switch/case scope first, so that the bindings it
        // introduces (e.g. `value` in `Some(value)`) are declared fresh, local to the case. Once
        // the switch scope is left, the pattern is handled a second time in the enclosing scope
        // to create the actual variable(s) that stay alive after the let-else statement; at that
        // point the case-local bindings are no longer visible, so this second pass creates new
        // declarations instead of just referencing the case-local ones.
        val switch =
            newSwitch(rawNode = raw).also { switch ->
                switch.selector =
                    letStmt.initializer?.let {
                        frontend.expressionHandler.handle(RsAst.RustExpr(it))
                    }
                        ?: newProblemExpression(
                            "Let statement does not have an initializer",
                            rawNode = raw,
                        )
                frontend.scopeManager.enterScope(switch)

                // Create a block to hold two case statements
                val caseBlock = newBlock(raw)
                caseBlock.usedAsExpression = true

                val case =
                    newCase(raw).also { value ->
                        value.caseExpression =
                            letStmt.pat?.let { frontend.patternHandler.handle(RsAst.RustPat(it)) }
                                ?: newProblemExpression("Pattern cannot be parsed.", rawNode = raw)
                    }
                caseBlock.statements += case

                val declarations = case.nodes.filterIsInstance<DeclarationStatement>()

                val bindingsList = newInitializerList(rawNode = raw)

                declarations
                    .flatMap { it.variables }
                    .forEach { variable ->
                        val reference = newReference(variable.name.toString(), rawNode = raw)
                        reference.refersTo = variable
                        bindingsList.initializers += reference
                    }

                val breakExpr = newBreak(raw)
                breakExpr.expr = bindingsList
                breakExpr.usedAsExpression = true

                caseBlock.statements += breakExpr

                caseBlock.statements += newDefault(raw)
                caseBlock.statements +=
                    frontend.expressionHandler.handleNode(RsExpr.BlockExpr(blockExpr))

                switch.statement = caseBlock

                frontend.scopeManager.leaveScope(switch)

                switch.usedAsExpression = true
            }

        val variableDeconstruction =
            letStmt.pat?.let { frontend.patternHandler.handle(RsAst.RustPat(it)) }
                ?: newProblemExpression("Pattern cannot be parsed.", rawNode = raw)

        letStmt.ty?.let { variableDeconstruction.type = frontend.typeOf(it) }

        return newAssign(
            operatorCode = "=",
            lhs = listOf(variableDeconstruction),
            rhs = listOf(switch),
            rawNode = raw,
        )
    }

    /**
     * Handles an expression statement (an expression followed by `;`, whose value is discarded) by
     * translating the inner expression and marking it as not used as an expression.
     *
     * AST:
     * [ast::ExprStmt](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.ExprStmt.html)
     *
     * Reference:
     * [Expression statements](https://doc.rust-lang.org/reference/statements.html#expression-statements)
     */
    fun handleExprStmt(exprStmt: RsExprStmt): Expression {
        val raw = RsAst.RustStmt(RsStmt.ExprStmt(exprStmt))

        exprStmt.expr.getOrNull(0)?.let {
            return frontend.expressionHandler.handle(RsAst.RustExpr(it)).also {
                it.usedAsExpression = false
            }
        }

        return newProblemExpression(
            "${exprStmt.javaClass.simpleName} does not contain an expression",
            rawNode = raw,
        )
    }

    /**
     * Handles an item (e.g. a nested `fn`, `struct`, `impl`, ...) that appears as a statement
     * inside a block, wrapping the result of [DeclarationHandler] in a [DeclarationStatement].
     *
     * AST: [ast::Item](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/enum.Item.html)
     *
     * Reference: part of [Statements](https://doc.rust-lang.org/reference/statements.html) (item
     * declarations)
     */
    fun handleItem(item: RsItem): Expression {

        val declarationStatement = newDeclarationStatement(rawNode = RsAst.RustItem(item))

        val declaration = frontend.declarationHandler.handle(RsAst.RustItem(item))
        ((declaration as? DeclarationSequence)?.declarations ?: listOf(declaration)).forEach {
            declItem ->
            declarationStatement.declarations += declItem
            frontend.scopeManager.addDeclaration(declItem)
        }

        return declarationStatement
    }
}
