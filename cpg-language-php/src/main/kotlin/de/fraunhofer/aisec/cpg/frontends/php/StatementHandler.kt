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
package de.fraunhofer.aisec.cpg.frontends.php

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.*
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Handles PHP statement nodes (blocks, if/else, while, for, foreach, try/catch, return, echo,
 * expression statements, etc.) and maps them to CPG expression/statement nodes.
 */
class StatementHandler(frontend: PHPLanguageFrontend) :
    PHPHandler<Expression, ParserRuleContext>({ ProblemExpression() }, frontend) {

    /** Dispatches a parser statement-related node to the corresponding modeling routine. */
    override fun handleNode(node: ParserRuleContext): Expression {
        return when (node) {
            is PhpParser.BlockStatementContext -> handleBlock(node)
            is PhpParser.StatementContext -> handleStatement(node)
            is PhpParser.InnerStatementListContext -> handleInnerStatementList(node)
            else -> handleNotSupported(node, node::class.simpleName ?: "unknown")
        }
    }

    /** Models a regular PHP statement node. */
    fun handle(ctx: PhpParser.StatementContext): Expression = handleStatement(ctx)

    /** Models a PHP block statement. */
    fun handle(ctx: PhpParser.BlockStatementContext): Block = handleBlock(ctx)

    // ── Block ─────────────────────────────────────────────────────────────────

    /** Converts a block statement into a CPG [Block]. */
    private fun handleBlock(ctx: PhpParser.BlockStatementContext): Block {
        val block = frontend.newBlock(rawNode = ctx)
        ctx.innerStatementList()?.innerStatement()?.forEach { inner ->
            val expr = handleInnerStatement(inner)
            block.statements += expr
        }
        return block
    }

    /** Converts an inner statement list into a synthetic block node. */
    private fun handleInnerStatementList(ctx: PhpParser.InnerStatementListContext): Block {
        val block = frontend.newBlock(rawNode = ctx)
        ctx.innerStatement()?.forEach { inner ->
            val expr = handleInnerStatement(inner)
            block.statements += expr
        }
        return block
    }

    /** Models a single inner statement contained in a block-like construct. */
    private fun handleInnerStatement(ctx: PhpParser.InnerStatementContext): Expression {
        return when {
            ctx.statement() != null -> handleStatement(ctx.statement())
            ctx.functionDeclaration() != null -> {
                val decl = frontend.declarationHandler.handleNode(ctx.functionDeclaration())
                val ds = frontend.newDeclarationStatement(rawNode = ctx)
                ds.declarations += decl
                ds
            }
            ctx.classDeclaration() != null -> {
                val decl = frontend.declarationHandler.handleNode(ctx.classDeclaration())
                val ds = frontend.newDeclarationStatement(rawNode = ctx)
                ds.declarations += decl
                ds
            }
            else -> ProblemExpression("unsupported inner statement")
        }
    }

    // ── Statement dispatch ────────────────────────────────────────────────────

    /** Dispatches a PHP statement to its specialized modeling routine. */
    private fun handleStatement(ctx: PhpParser.StatementContext): Expression {
        return when {
            ctx.blockStatement() != null -> handleBlock(ctx.blockStatement())
            ctx.ifStatement() != null -> handleIf(ctx.ifStatement())
            ctx.whileStatement() != null -> handleWhile(ctx.whileStatement())
            ctx.doWhileStatement() != null -> handleDoWhile(ctx.doWhileStatement())
            ctx.forStatement() != null -> handleFor(ctx.forStatement())
            ctx.foreachStatement() != null -> handleForeach(ctx.foreachStatement())
            ctx.returnStatement() != null -> handleReturn(ctx.returnStatement())
            ctx.tryCatchFinally() != null -> handleTryCatch(ctx.tryCatchFinally())
            ctx.throwStatement() != null -> handleThrow(ctx.throwStatement())
            ctx.echoStatement() != null -> handleEcho(ctx.echoStatement())
            ctx.expressionStatement() != null ->
                frontend.expressionHandler.handle(ctx.expressionStatement().expression())
            ctx.switchStatement() != null -> handleSwitch(ctx.switchStatement())
            ctx.breakStatement() != null -> handleBreak(ctx.breakStatement())
            ctx.continueStatement() != null -> handleContinue(ctx.continueStatement())
            ctx.emptyStatement_() != null -> frontend.newEmpty(rawNode = ctx)
            else -> ProblemExpression("unsupported statement: ${ctx.text}")
        }
    }

    // ── If / else ─────────────────────────────────────────────────────────────

    /** Models an if/elseif/else chain as nested [IfElse] nodes. */
    private fun handleIf(ctx: PhpParser.IfStatementContext): IfElse {
        val ifElse = frontend.newIfElse(rawNode = ctx)
        ifElse.condition = frontend.expressionHandler.handle(ctx.parentheses().expression())

        ctx.statement()?.let { ifElse.thenStatement = handle(it) }

        // Build else-if chain from back to front
        var elseChain: Expression? = null
        ctx.elseStatement()?.statement()?.let { elseChain = handle(it) }

        val elseIfs = ctx.elseIfStatement()?.reversed() ?: emptyList()
        for (elif in elseIfs) {
            val nested = frontend.newIfElse(rawNode = elif)
            nested.condition = frontend.expressionHandler.handle(elif.parentheses().expression())
            nested.thenStatement = elif.statement()?.let { handle(it) }
            nested.elseStatement = elseChain
            elseChain = nested
        }
        ifElse.elseStatement = elseChain

        return ifElse
    }

    // ── While ─────────────────────────────────────────────────────────────────

    /** Models a while loop and its body. */
    private fun handleWhile(ctx: PhpParser.WhileStatementContext): While {
        val whileStmt = frontend.newWhile(rawNode = ctx)
        whileStmt.condition = frontend.expressionHandler.handle(ctx.parentheses().expression())
        val body =
            ctx.statement()?.let { handle(it) }
                ?: ctx.innerStatementList()?.let { handleInnerStatementList(it) }
                ?: frontend.newBlock(rawNode = ctx)
        whileStmt.statement = body
        return whileStmt
    }

    /** Models a do/while loop. */
    private fun handleDoWhile(ctx: PhpParser.DoWhileStatementContext): DoWhile {
        val doWhile = frontend.newDoWhile(rawNode = ctx)
        doWhile.condition = frontend.expressionHandler.handle(ctx.parentheses().expression())
        doWhile.statement = handle(ctx.statement())
        return doWhile
    }

    // ── For ───────────────────────────────────────────────────────────────────

    /** Models a classic PHP for-loop including init, condition, update, and body. */
    private fun handleFor(ctx: PhpParser.ForStatementContext): For {
        val forStmt = frontend.newFor(rawNode = ctx)

        // initializerStatement is a single Expression; wrap multiple inits in a block
        val inits =
            ctx.forInit()?.expressionList()?.expression()?.map { e ->
                frontend.expressionHandler.handle(e)
            } ?: emptyList()
        if (inits.size == 1) {
            forStmt.initializerStatement = inits[0]
        } else if (inits.size > 1) {
            val block = frontend.newBlock(rawNode = ctx)
            inits.forEach { block.statements += it }
            forStmt.initializerStatement = block
        }

        // condition (only last expression counts)
        ctx.expressionList()?.expression()?.lastOrNull()?.let {
            forStmt.condition = frontend.expressionHandler.handle(it)
        }

        // iterationStatement is a single Expression; wrap multiple in a block
        val iters =
            ctx.forUpdate()?.expressionList()?.expression()?.map { e ->
                frontend.expressionHandler.handle(e)
            } ?: emptyList()
        if (iters.size == 1) {
            forStmt.iterationStatement = iters[0]
        } else if (iters.size > 1) {
            val block = frontend.newBlock(rawNode = ctx)
            iters.forEach { block.statements += it }
            forStmt.iterationStatement = block
        }

        val body =
            ctx.statement()?.let { handle(it) }
                ?: ctx.innerStatementList()?.let { handleInnerStatementList(it) }
                ?: frontend.newBlock(rawNode = ctx)
        forStmt.statement = body
        return forStmt
    }

    // ── Foreach ───────────────────────────────────────────────────────────────

    /** Models a foreach loop over an iterable expression. */
    private fun handleForeach(ctx: PhpParser.ForeachStatementContext): ForEach {
        val forEach = frontend.newForEach(rawNode = ctx)

        // iterable – the expression being iterated (expression() returns single context)
        ctx.expression()?.let { forEach.iterable = frontend.expressionHandler.handle(it) }
        // fallback: chain-based foreach (chain() returns list)
        if (forEach.iterable == null) {
            ctx.chain()?.firstOrNull()?.let {
                forEach.iterable = frontend.expressionHandler.handleChain(it)
            }
        }

        // the loop variable – represented as a Reference (simplification)
        ctx.assignable()?.let { assignable ->
            val varName = assignable.text.removePrefix("$")
            val variable = frontend.newVariable(varName, rawNode = assignable)
            val ds = frontend.newDeclarationStatement(rawNode = assignable)
            ds.declarations += variable
            forEach.variable = ds
        }

        val body =
            if (ctx.statement() != null) handle(ctx.statement())
            else if (ctx.innerStatementList() != null)
                handleInnerStatementList(ctx.innerStatementList())
            else frontend.newBlock(rawNode = ctx)
        forEach.statement = body
        return forEach
    }

    // ── Return ────────────────────────────────────────────────────────────────

    /** Models a return statement and its optional return value. */
    private fun handleReturn(ctx: PhpParser.ReturnStatementContext): Return {
        val ret = frontend.newReturn(rawNode = ctx)
        ctx.expression()?.let { ret.returnValue = frontend.expressionHandler.handle(it) }
        return ret
    }

    // ── Try / catch / finally ─────────────────────────────────────────────────

    /** Models a try/catch/finally statement with its handlers and cleanup block. */
    private fun handleTryCatch(ctx: PhpParser.TryCatchFinallyContext): Try {
        val tryCatch = frontend.newTry(rawNode = ctx)
        tryCatch.tryBlock = handleBlock(ctx.blockStatement())

        ctx.catchClause()?.forEach { catchCtx ->
            val catch = frontend.newCatchClause(rawNode = catchCtx)

            // Caught type(s) – use first for simplicity
            val typeRef = catchCtx.qualifiedStaticTypeRef()?.firstOrNull()
            val varName = catchCtx.VarName()?.text?.removePrefix("$") ?: "e"
            val catchType =
                if (typeRef != null) frontend.typeOf(typeRef.text) else frontend.autoType()
            // CatchClause.parameter is Variable?, not Parameter
            catch.parameter = frontend.newVariable(varName, catchType, rawNode = catchCtx)

            catch.body = handleBlock(catchCtx.blockStatement())
            tryCatch.catchClauses += catch
        }

        ctx.finallyStatement()?.let { tryCatch.finallyBlock = handleBlock(it.blockStatement()) }

        return tryCatch
    }

    // ── Throw ─────────────────────────────────────────────────────────────────

    /** Models a throw statement. */
    private fun handleThrow(ctx: PhpParser.ThrowStatementContext): Throw {
        val throwExpr = frontend.newThrow(rawNode = ctx)
        throwExpr.exception = frontend.expressionHandler.handle(ctx.expression())
        return throwExpr
    }

    // ── Echo ──────────────────────────────────────────────────────────────────

    /** Models `echo` as a call to the built-in output facility. */
    private fun handleEcho(ctx: PhpParser.EchoStatementContext): Expression {
        // Model echo as a call to the built-in "echo" function
        val callee = frontend.newReference("echo", rawNode = ctx)
        val call = frontend.newCall(callee, rawNode = ctx)
        ctx.expressionList().expression().forEach { e ->
            call.addArgument(frontend.expressionHandler.handle(e))
        }
        return call
    }

    // ── Switch ────────────────────────────────────────────────────────────────

    /** Models a PHP switch statement and its case/default sections. */
    private fun handleSwitch(ctx: PhpParser.SwitchStatementContext): Switch {
        val sw = frontend.newSwitch(rawNode = ctx)
        sw.selector = frontend.expressionHandler.handle(ctx.parentheses().expression())

        val body = frontend.newBlock(rawNode = ctx)
        ctx.switchBlock()?.forEach { block ->
            block.expression()?.forEach { caseExpr ->
                val caseNode = frontend.newCase(rawNode = block)
                caseNode.caseExpression = frontend.expressionHandler.handle(caseExpr)
                body.statements += caseNode
            }
            if (block.Default() != null) {
                body.statements += frontend.newDefault(rawNode = block)
            }
            block.innerStatementList()?.innerStatement()?.forEach { inner ->
                body.statements += handleInnerStatement(inner)
            }
        }
        sw.statement = body
        return sw
    }

    // ── Break / Continue ──────────────────────────────────────────────────────

    /** Models a break statement. */
    private fun handleBreak(ctx: PhpParser.BreakStatementContext): Break {
        return frontend.newBreak(rawNode = ctx)
    }

    /** Models a continue statement. */
    private fun handleContinue(ctx: PhpParser.ContinueStatementContext): Continue {
        return frontend.newContinue(rawNode = ctx)
    }
}
