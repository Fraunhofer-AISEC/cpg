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
 * Handles PHP expression nodes and maps them to CPG expression nodes. Covers variable references,
 * function/method calls, binary/unary operators, literals, new-expressions, assignments, arrays,
 * and cast expressions.
 */
class ExpressionHandler(frontend: PHPLanguageFrontend) :
    PHPHandler<Expression, ParserRuleContext>({ ProblemExpression() }, frontend) {

    /** Dispatches parser nodes that represent PHP expressions or expression-like helpers. */
    override fun handleNode(node: ParserRuleContext): Expression {
        return when (node) {
            is PhpParser.ExpressionContext -> handle(node)
            is PhpParser.ChainContext -> handleChain(node)
            is PhpParser.ConstantContext -> handleConstant(node)
            is PhpParser.ConstantInitializerContext -> handleConstantInitializer(node)
            else -> handleNotSupported(node, node::class.simpleName ?: "unknown")
        }
    }

    /** Models a PHP expression by delegating to the specialized expression routine. */
    fun handle(ctx: PhpParser.ExpressionContext): Expression {
        return when (ctx) {
            is PhpParser.AssignmentExpressionContext -> handleAssignment(ctx)
            is PhpParser.ChainExpressionContext -> handleChain(ctx.chain())
            is PhpParser.NewExpressionContext -> handleNew(ctx.newExpr())
            is PhpParser.ScalarExpressionContext -> handleScalar(ctx)
            is PhpParser.AdditiveExpressionContext ->
                handleBinary(ctx.expression(0), ctx.op.text, ctx.expression(1))
            is PhpParser.MultiplicativeExpressionContext ->
                handleBinary(ctx.expression(0), ctx.op.text, ctx.expression(1))
            is PhpParser.RelationalExpressionContext ->
                handleBinary(ctx.expression(0), ctx.op.text, ctx.expression(1))
            is PhpParser.EqualityExpressionContext ->
                handleBinary(ctx.expression(0), ctx.op.text, ctx.expression(1))
            is PhpParser.LogicalAndExpressionContext ->
                handleBinary(ctx.expression(0), ctx.op.text, ctx.expression(1))
            is PhpParser.LogicalIncOrExpressionContext ->
                handleBinary(ctx.expression(0), ctx.op.text, ctx.expression(1))
            is PhpParser.BitwiseAndExpressionContext ->
                handleBinary(ctx.expression(0), ctx.op.text, ctx.expression(1))
            is PhpParser.BitwiseExcOrExpressionContext ->
                handleBinary(ctx.expression(0), ctx.op.text, ctx.expression(1))
            is PhpParser.BitwiseIncOrExpressionContext ->
                handleBinary(ctx.expression(0), ctx.op.text, ctx.expression(1))
            is PhpParser.ShiftExpressionContext ->
                handleBinary(ctx.expression(0), ctx.op.text, ctx.expression(1))
            is PhpParser.ExponentiationExpressionContext ->
                handleBinary(ctx.expression(0), ctx.op.text, ctx.expression(1))
            is PhpParser.CoalesceExpressionContext ->
                handleBinary(ctx.expression(0), ctx.op.text, ctx.expression(1))
            is PhpParser.SpaceshipExpressionContext ->
                handleBinary(ctx.expression(0), ctx.op.text, ctx.expression(1))
            is PhpParser.UnaryOperatorExpressionContext -> handleUnary(ctx)
            is PhpParser.PrefixIncDecExpressionContext -> handlePrefixIncDec(ctx)
            is PhpParser.PostfixIncDecExpressionContext -> handlePostfixIncDec(ctx)
            is PhpParser.CastExpressionContext -> handleCast(ctx)
            is PhpParser.ArrayCreationExpressionContext -> handleArrayCreation(ctx.arrayCreation())
            is PhpParser.ParenthesisExpressionContext -> handle(ctx.parentheses().expression())
            is PhpParser.InstanceOfExpressionContext -> handleInstanceOf(ctx)
            is PhpParser.ConditionalExpressionContext -> handleConditional(ctx)
            is PhpParser.CloneExpressionContext -> handle(ctx.expression())
            is PhpParser.PrintExpressionContext -> {
                val callee = frontend.newReference("print", rawNode = ctx)
                val call = frontend.newCall(callee, rawNode = ctx)
                call.addArgument(handle(ctx.expression()))
                call
            }
            is PhpParser.LambdaFunctionExpressionContext -> handleLambda(ctx.lambdaFunctionExpr())
            is PhpParser.IncludeExpressionContext ->
                ctx.expression()?.let { handle(it) }
                    ?: ProblemExpression("include without expression")
            is PhpParser.RequireExpressionContext ->
                ctx.expression()?.let { handle(it) }
                    ?: ProblemExpression("require without expression")
            is PhpParser.SpecialWordExpressionContext -> handleSpecialWord(ctx)
            is PhpParser.MatchExpressionContext -> handleMatchExpr(ctx.matchExpr())
            else -> ProblemExpression("unsupported expression: ${ctx::class.simpleName}")
        }
    }

    // ── Assignment ────────────────────────────────────────────────────────────

    /** Models an assignment or compound-assignment expression. */
    private fun handleAssignment(ctx: PhpParser.AssignmentExpressionContext): Expression {
        // Identify the operator: the first child after the assignable that is an operator token
        val opText = ctx.assignmentOperator()?.text ?: ctx.Eq()?.text ?: "="
        val assign = frontend.newAssign(opText, rawNode = ctx)

        val lhsExpr = handleAssignable(ctx.assignable())
        assign.lhs = mutableListOf(lhsExpr)

        val rhs = ctx.expression()?.let { handle(it) } ?: ProblemExpression("missing rhs")
        assign.rhs = mutableListOf(rhs)

        return assign
    }

    /** Models the left-hand side of an assignment expression. */
    private fun handleAssignable(ctx: PhpParser.AssignableContext): Expression {
        return when {
            ctx.chain() != null -> handleChain(ctx.chain())
            ctx.arrayCreation() != null -> handleArrayCreation(ctx.arrayCreation())
            else -> ProblemExpression("unsupported assignable: ${ctx.text}")
        }
    }

    // ── Chains (member access, function calls, variables) ────────────────────

    /** Models a PHP access chain such as `$obj->field()[0]`. */
    fun handleChain(ctx: PhpParser.ChainContext): Expression {
        var base = handleChainOrigin(ctx.chainOrigin())
        for (access in ctx.memberAccess()) {
            base = handleMemberAccess(base, access)
        }
        return base
    }

    /** Models the base origin of a PHP access chain. */
    private fun handleChainOrigin(ctx: PhpParser.ChainOriginContext): Expression {
        return when {
            ctx.chainBase() != null -> handleChainBase(ctx.chainBase())
            ctx.functionCall() != null -> handleFunctionCall(ctx.functionCall())
            ctx.newExpr() != null -> handleNew(ctx.newExpr())
            else -> ProblemExpression("unsupported chain origin: ${ctx.text}")
        }
    }

    /** Models the first base element of a chain. */
    private fun handleChainBase(ctx: PhpParser.ChainBaseContext): Expression {
        return when {
            ctx.qualifiedStaticTypeRef() != null -> {
                // Static call: ClassName::$var
                val typeName = ctx.qualifiedStaticTypeRef().text
                frontend.newReference(typeName, rawNode = ctx)
            }
            ctx.keyedVariable().isNotEmpty() -> {
                val varCtx = ctx.keyedVariable(0)
                handleKeyedVariable(varCtx)
            }
            else -> ProblemExpression("unsupported chain base: ${ctx.text}")
        }
    }

    /** Models a variable access with optional array subscripts. */
    private fun handleKeyedVariable(ctx: PhpParser.KeyedVariableContext): Expression {
        val varName =
            ctx.VarName()?.text?.removePrefix("$") ?: return ProblemExpression("no VarName")
        val ref = frontend.newReference(varName, rawNode = ctx)
        // Handle array access: $var[expr]
        var result: Expression = ref
        for (sqe in ctx.squareCurlyExpression()) {
            val sub = frontend.newSubscription(rawNode = sqe)
            sub.arrayExpression = result
            sqe.expression()?.let { sub.subscriptExpression = handle(it) }
            result = sub
        }
        return result
    }

    /** Models field access or method calls on a chain base. */
    private fun handleMemberAccess(
        base: Expression,
        ctx: PhpParser.MemberAccessContext,
    ): Expression {
        val fieldName =
            ctx.keyedFieldName()?.keyedSimpleFieldName()?.text
                ?: ctx.keyedFieldName()?.keyedVariable()?.VarName()?.text?.removePrefix("$")
                ?: "unknown"

        return if (ctx.actualArguments() != null) {
            // method call
            val callee = frontend.newMemberAccess(fieldName, base, rawNode = ctx)
            val call = frontend.newMemberCall(callee, false, rawNode = ctx)
            ctx.actualArguments()?.arguments()?.forEach { args ->
                args.actualArgument()?.forEach { arg -> call.addArgument(handle(arg.expression())) }
            }
            call
        } else {
            // field access
            frontend.newMemberAccess(fieldName, base, rawNode = ctx)
        }
    }

    /** Models a standalone function call or static method call. */
    private fun handleFunctionCall(ctx: PhpParser.FunctionCallContext): Expression {
        val nameCtx = ctx.functionCallName()
        val callee: Expression =
            when {
                nameCtx.qualifiedNamespaceName() != null -> {
                    val name = nameCtx.qualifiedNamespaceName().text
                    frontend.newReference(name, rawNode = nameCtx)
                }
                nameCtx.classConstant() != null -> {
                    val cc = nameCtx.classConstant()
                    val className =
                        cc.qualifiedStaticTypeRef()?.text ?: cc.text.substringBefore("::")
                    val methodName =
                        cc.identifier()?.text
                            ?: cc.keyedVariable()?.firstOrNull()?.text
                            ?: "unknown"
                    val base = frontend.newReference(className, rawNode = cc)
                    frontend.newMemberAccess(methodName, base, rawNode = cc)
                }
                nameCtx.chainBase() != null -> handleChainBase(nameCtx.chainBase())
                else -> ProblemExpression("unsupported function call name: ${nameCtx.text}")
            }

        val call =
            if (callee is MemberAccess) frontend.newMemberCall(callee, false, rawNode = ctx)
            else frontend.newCall(callee, rawNode = ctx)

        ctx.actualArguments()?.arguments()?.forEach { args ->
            args.actualArgument()?.forEach { arg ->
                val argExpr =
                    arg.expression()?.let { handle(it) } ?: arg.chain()?.let { handleChain(it) }
                if (argExpr != null) call.addArgument(argExpr)
            }
        }
        return call
    }

    // ── New expression ────────────────────────────────────────────────────────

    /** Models object construction via `new`. */
    private fun handleNew(ctx: PhpParser.NewExprContext): Expression {
        val typeName = ctx.typeRef()?.text ?: "unknown"
        val type = frontend.typeOf(typeName)
        val newExpr = frontend.newNew(type, rawNode = ctx)
        val construction = frontend.newConstruction(typeName, rawNode = ctx)
        ctx.arguments()?.actualArgument()?.forEach { arg ->
            val argExpr =
                arg.expression()?.let { handle(it) } ?: arg.chain()?.let { handleChain(it) }
            if (argExpr != null) construction.addArgument(argExpr)
        }
        newExpr.initializer = construction
        return newExpr
    }

    // ── Scalar / literals ─────────────────────────────────────────────────────

    /** Models a scalar expression such as a constant, string, or label reference. */
    private fun handleScalar(ctx: PhpParser.ScalarExpressionContext): Expression {
        return when {
            ctx.constant() != null -> handleConstant(ctx.constant())
            ctx.string() != null -> handleString(ctx.string())
            ctx.Label() != null -> frontend.newReference(ctx.Label().text, rawNode = ctx)
            else -> ProblemExpression("unsupported scalar: ${ctx.text}")
        }
    }

    /** Models PHP constants, magic constants, and class constants. */
    internal fun handleConstant(ctx: PhpParser.ConstantContext): Expression {
        return when {
            ctx.Null() != null ->
                frontend.newLiteral(null, frontend.primitiveType("null"), rawNode = ctx)
            ctx.literalConstant() != null -> handleLiteralConstant(ctx.literalConstant())
            ctx.magicConstant() != null ->
                frontend.newReference(ctx.magicConstant().text, rawNode = ctx)
            ctx.classConstant() != null -> {
                val cc = ctx.classConstant()
                val base = frontend.newReference(cc.text.substringBefore("::"), rawNode = cc)
                val field = cc.text.substringAfter("::")
                frontend.newMemberAccess(field, base, rawNode = cc)
            }
            ctx.qualifiedNamespaceName() != null ->
                frontend.newReference(ctx.qualifiedNamespaceName().text, rawNode = ctx)
            else -> ProblemExpression("unsupported constant: ${ctx.text}")
        }
    }

    /** Models literal constants such as booleans, numbers, and string constants. */
    private fun handleLiteralConstant(ctx: PhpParser.LiteralConstantContext): Expression {
        return when {
            ctx.BooleanConstant() != null -> {
                val value = ctx.BooleanConstant().text.lowercase() == "true"
                frontend.newLiteral(value, frontend.primitiveType("bool"), rawNode = ctx)
            }
            ctx.numericConstant() != null -> handleNumericConstant(ctx.numericConstant())
            ctx.Real() != null -> {
                val value = ctx.Real().text.toDoubleOrNull() ?: 0.0
                frontend.newLiteral(value, frontend.primitiveType("float"), rawNode = ctx)
            }
            ctx.stringConstant() != null ->
                frontend.newReference(ctx.stringConstant().text, rawNode = ctx)
            else -> ProblemExpression("unsupported literal: ${ctx.text}")
        }
    }

    /** Models numeric literals across the supported PHP bases. */
    private fun handleNumericConstant(ctx: PhpParser.NumericConstantContext): Expression {
        val text = ctx.text
        return when {
            ctx.Decimal() != null -> {
                val value = text.replace("_", "").toLongOrNull() ?: 0L
                frontend.newLiteral(value, frontend.primitiveType("int"), rawNode = ctx)
            }
            ctx.Octal() != null -> {
                val stripped = text.removePrefix("0").removePrefix("o")
                val value = stripped.replace("_", "").toLongOrNull(8) ?: 0L
                frontend.newLiteral(value, frontend.primitiveType("int"), rawNode = ctx)
            }
            ctx.Hex() != null -> {
                val stripped = text.removePrefix("0x").replace("_", "")
                val value = stripped.toLongOrNull(16) ?: 0L
                frontend.newLiteral(value, frontend.primitiveType("int"), rawNode = ctx)
            }
            ctx.Binary() != null -> {
                val stripped = text.removePrefix("0b").replace("_", "")
                val value = stripped.toLongOrNull(2) ?: 0L
                frontend.newLiteral(value, frontend.primitiveType("int"), rawNode = ctx)
            }
            else -> ProblemExpression("unsupported numeric constant: $text")
        }
    }

    /** Models string literals and simplified interpolated strings. */
    private fun handleString(ctx: PhpParser.StringContext): Expression {
        val text =
            when {
                ctx.SingleQuoteString() != null ->
                    ctx.SingleQuoteString().text.removeSurrounding("'")
                ctx.DoubleQuote() != null ->
                    // simplified: just collect string parts (no interpolation)
                    ctx.interpolatedStringPart()?.joinToString("") { it.StringPart()?.text ?: "" }
                        ?: ""
                else -> ctx.text
            }
        return frontend.newLiteral(text, frontend.primitiveType("string"), rawNode = ctx)
    }

    // ── Unary / increment / decrement ─────────────────────────────────────────

    /** Models a unary operator expression. */
    private fun handleUnary(ctx: PhpParser.UnaryOperatorExpressionContext): Expression {
        // First child gives the operator text
        val op = ctx.getChild(0).text
        val unary = frontend.newUnaryOperator(op, postfix = false, prefix = true, rawNode = ctx)
        unary.input = handle(ctx.expression())
        return unary
    }

    /** Models a prefix increment or decrement expression. */
    private fun handlePrefixIncDec(ctx: PhpParser.PrefixIncDecExpressionContext): Expression {
        val op = ctx.getChild(0).text // '++' or '--'
        val unary = frontend.newUnaryOperator(op, postfix = false, prefix = true, rawNode = ctx)
        unary.input = handleChain(ctx.chain())
        return unary
    }

    /** Models a postfix increment or decrement expression. */
    private fun handlePostfixIncDec(ctx: PhpParser.PostfixIncDecExpressionContext): Expression {
        val op = ctx.getChild(1).text // '++' or '--'
        val unary = frontend.newUnaryOperator(op, postfix = true, prefix = false, rawNode = ctx)
        unary.input = handleChain(ctx.chain())
        return unary
    }

    // ── Binary operators ──────────────────────────────────────────────────────

    /** Models a binary operator from its left/right subexpressions and operator text. */
    private fun handleBinary(
        lhsCtx: PhpParser.ExpressionContext,
        op: String,
        rhsCtx: PhpParser.ExpressionContext,
    ): Expression {
        val binOp = frontend.newBinaryOperator(op, rawNode = lhsCtx.parent as? ParserRuleContext)
        binOp.lhs = handle(lhsCtx)
        binOp.rhs = handle(rhsCtx)
        return binOp
    }

    // ── Cast ──────────────────────────────────────────────────────────────────

    /** Models an explicit PHP cast expression. */
    private fun handleCast(ctx: PhpParser.CastExpressionContext): Expression {
        val cast = frontend.newCast(rawNode = ctx)
        cast.castType = frontend.typeOf(ctx.castOperation().text)
        cast.expression =
            ctx.expression()?.let { handle(it) } ?: ProblemExpression("cast without expression")
        return cast
    }

    // ── Array ─────────────────────────────────────────────────────────────────

    /** Models an array literal as an initializer list. */
    private fun handleArrayCreation(ctx: PhpParser.ArrayCreationContext): Expression {
        val initList = frontend.newInitializerList(rawNode = ctx)
        ctx.arrayItemList()?.arrayItem()?.forEach { item ->
            val exprs = item.expression()
            val value = exprs.lastOrNull()?.let { handle(it) }
            if (value != null) {
                if (exprs.size == 2) {
                    // key => value
                    val kv =
                        frontend.newKeyValue(key = handle(exprs[0]), value = value, rawNode = item)
                    initList.initializers += kv
                } else {
                    initList.initializers += value
                }
            }
        }
        return initList
    }

    // ── instanceof ────────────────────────────────────────────────────────────

    /** Models an `instanceof` check as a binary operator. */
    private fun handleInstanceOf(ctx: PhpParser.InstanceOfExpressionContext): Expression {
        val binOp = frontend.newBinaryOperator("instanceof", rawNode = ctx)
        binOp.lhs = ctx.expression()?.let { handle(it) } ?: ProblemExpression("instanceof lhs")
        binOp.rhs = frontend.newReference(ctx.typeRef().text, rawNode = ctx.typeRef())
        return binOp
    }

    // ── Conditional / ternary ─────────────────────────────────────────────────

    /** Models ternary and Elvis-style conditional expressions. */
    private fun handleConditional(ctx: PhpParser.ConditionalExpressionContext): Expression {
        val exprs = ctx.expression()
        val condExpr =
            if (exprs.isNotEmpty()) handle(exprs[0]) else ProblemExpression("ternary condition")
        val cond = frontend.newConditional(condExpr, rawNode = ctx)
        if (exprs.size >= 3) {
            // full ternary: cond ? then : else
            cond.thenExpression = handle(exprs[1])
            cond.elseExpression = handle(exprs[2])
        } else if (exprs.size == 2) {
            // Elvis: cond ?: else
            cond.thenExpression = handle(exprs[0])
            cond.elseExpression = handle(exprs[1])
        }
        return cond
    }

    // ── Lambda / closure ─────────────────────────────────────────────────────

    /** Models an anonymous function or arrow function as a CPG lambda. */
    private fun handleLambda(ctx: PhpParser.LambdaFunctionExprContext): Expression {
        val func = frontend.newFunction("", rawNode = ctx)
        frontend.scopeManager.enterScope(func)

        ctx.formalParameterList()?.formalParameter()?.forEach { param ->
            val p = frontend.declarationHandler.handleFormalParameter(param)
            frontend.scopeManager.addDeclaration(p)
            func.parameters += p
        }

        // Arrow function body is a single expression
        ctx.expression()?.let {
            val ret = frontend.newReturn(rawNode = it)
            ret.returnValue = handle(it)
            val block = frontend.newBlock(rawNode = it)
            block.statements += ret
            func.body = block
        }
        ctx.blockStatement()?.let { func.body = frontend.statementHandler.handle(it) }

        frontend.scopeManager.leaveScope(func)

        val lambda = frontend.newLambda(rawNode = ctx)
        lambda.function = func
        return lambda
    }

    // ── Match expression (PHP 8) ──────────────────────────────────────────────

    /** Models a PHP 8 `match` expression as a switch-like construct. */
    private fun handleMatchExpr(ctx: PhpParser.MatchExprContext): Expression {
        // Model as a switch-like construct using a binary operator chain (simplified)
        val sw = frontend.newSwitch(rawNode = ctx)
        sw.selector = ctx.expression()?.let { handle(it) } ?: ProblemExpression("match expression")
        val body = frontend.newBlock(rawNode = ctx)
        ctx.matchItem()?.forEach { item ->
            val exprs = item.expression()
            if (exprs.size >= 2) {
                val caseNode = frontend.newCase(rawNode = item)
                caseNode.caseExpression = handle(exprs.first())
                body.statements += caseNode
                val ret = frontend.newReturn(rawNode = item)
                ret.returnValue = handle(exprs.last())
                body.statements += ret
            }
        }
        sw.statement = body
        return sw
    }

    // ── Special words (isset, empty, eval, exit, list) ────────────────────────

    /** Models special-word expressions such as `isset`, `empty`, or `exit` as calls. */
    private fun handleSpecialWord(ctx: PhpParser.SpecialWordExpressionContext): Expression {
        val callee = frontend.newReference(ctx.getChild(0).text, rawNode = ctx)
        val call = frontend.newCall(callee, rawNode = ctx)
        // expression() returns single ExpressionContext? for SpecialWordExpression
        ctx.expression()?.let { e -> call.addArgument(handle(e)) }
        ctx.chainList()?.chain()?.forEach { c -> call.addArgument(handleChain(c)) }
        ctx.chain()?.let { call.addArgument(handleChain(it)) }
        return call
    }

    // ── Constant initializers (used in field/param defaults) ──────────────────

    /** Models constant initializer expressions used in fields and parameter defaults. */
    internal fun handleConstantInitializer(ctx: PhpParser.ConstantInitializerContext): Expression {
        return when {
            // Unary +/- case: ctx.constantInitializer() returns single
            ctx.constantInitializer() != null -> {
                val inner = handleConstantInitializer(ctx.constantInitializer())
                val unary =
                    frontend.newUnaryOperator(
                        ctx.getChild(0).text,
                        postfix = false,
                        prefix = true,
                        rawNode = ctx,
                    )
                unary.input = inner
                unary
            }
            // Single constant
            ctx.constant().size == 1 && ctx.string().isEmpty() -> handleConstant(ctx.constant()[0])
            // Single string
            ctx.string().size == 1 && ctx.constant().isEmpty() -> handleString(ctx.string()[0])
            // Multiple (concatenation) – iterate children for order
            ctx.constant().isNotEmpty() || ctx.string().isNotEmpty() -> {
                val parts = mutableListOf<Expression>()
                for (child in ctx.children ?: emptyList()) {
                    when (child) {
                        is PhpParser.ConstantContext -> parts += handleConstant(child)
                        is PhpParser.StringContext -> parts += handleString(child)
                    }
                }
                if (parts.isEmpty()) ProblemExpression("empty constant initializer")
                else
                    parts.drop(1).fold(parts[0]) { acc, next ->
                        val bin = frontend.newBinaryOperator(".", rawNode = ctx)
                        bin.lhs = acc
                        bin.rhs = next
                        bin
                    }
            }
            else -> ProblemExpression("unsupported constant initializer: ${ctx.text}")
        }
    }
}
