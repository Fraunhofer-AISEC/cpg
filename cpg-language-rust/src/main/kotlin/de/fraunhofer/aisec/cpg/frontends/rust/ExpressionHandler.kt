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
import de.fraunhofer.aisec.cpg.graph.expressions.*
import uniffi.cpgrust.RsAst
import uniffi.cpgrust.RsBinExpr
import uniffi.cpgrust.RsBlockExpr
import uniffi.cpgrust.RsBreakExpr
import uniffi.cpgrust.RsCallExpr
import uniffi.cpgrust.RsCastExpr
import uniffi.cpgrust.RsContinueExpr
import uniffi.cpgrust.RsExpr
import uniffi.cpgrust.RsFieldExpr
import uniffi.cpgrust.RsForExpr
import uniffi.cpgrust.RsIfExpr
import uniffi.cpgrust.RsIndexExpr
import uniffi.cpgrust.RsLetExpr
import uniffi.cpgrust.RsLiteral
import uniffi.cpgrust.RsLiteralType
import uniffi.cpgrust.RsLoopExpr
import uniffi.cpgrust.RsMacroExpr
import uniffi.cpgrust.RsMethodCallExpr
import uniffi.cpgrust.RsPat
import uniffi.cpgrust.RsPathExpr
import uniffi.cpgrust.RsPrefixExpr
import uniffi.cpgrust.RsRangeExpr
import uniffi.cpgrust.RsRecordExpr
import uniffi.cpgrust.RsRefExpr
import uniffi.cpgrust.RsWhileExpr

class ExpressionHandler(frontend: RustLanguageFrontend) :
    RustHandler<Expression, RsAst.RustExpr>(::ProblemExpression, frontend) {

    override fun handleNode(node: RsAst.RustExpr): Expression {
        val unwrapped = node.v1
        return handleNode(unwrapped)
    }

    fun handleNode(node: RsExpr): Expression {
        return when (node) {
            is RsExpr.BlockExpr -> handleBlockExpr(node.v1)
            is RsExpr.Literal -> handleLiteral(node.v1)
            is RsExpr.CallExpr -> handleCallExpr(node.v1)
            is RsExpr.MethodCallExpr -> handleMethodCallExpr(node.v1)
            is RsExpr.MacroExpr -> handleMacroExpr(node.v1)
            is RsExpr.PathExpr -> handlePathExpr(node.v1)
            is RsExpr.BinExpr -> handleBinExpr(node.v1)
            is RsExpr.PrefixExpr -> handlePrefixExpr(node.v1)
            is RsExpr.ParenExpr -> handleNode(node.v1.expr.first())
            is RsExpr.RecordExpr -> handleRecordExpr(node.v1)
            is RsExpr.IfExpr -> handleIfExpr(node.v1)
            is RsExpr.LetExpr -> handleLetExpr(node.v1)
            is RsExpr.WhileExpr -> handleWhileExpr(node.v1)
            is RsExpr.ForExpr -> handleForExpr(node.v1)
            is RsExpr.LoopExpr -> handleLoopExpr(node.v1)
            is RsExpr.RangeExpr -> handleRangeExpr(node.v1)
            is RsExpr.FieldExpr -> handleFieldExpr(node.v1)
            is RsExpr.BreakExpr -> handleBreakExpr(node.v1)
            is RsExpr.ContinueExpr -> handleContinueExpr(node.v1)
            is RsExpr.CastExpr -> handleCastExpr(node.v1)
            is RsExpr.IndexExpr -> handleIndexExpr(node.v1)
            is RsExpr.RefExpr -> handleRefExpr(node.v1)
            else -> handleNotSupported(RsAst.RustExpr(node), node::class.simpleName ?: "")
        }
    }

    fun handleBlockExpr(blockExpr: RsBlockExpr): Expression {

        val block = newBlock(RsAst.RustExpr(RsExpr.BlockExpr(blockExpr)))

        frontend.scopeManager.enterScope(block)

        for (stmt in blockExpr.stmts) {
            block.statements += frontend.statementHandler.handle(RsAst.RustStmt(stmt))
        }

        blockExpr.tailExpr.getOrNull(0)?.let {
            block.statements += frontend.expressionHandler.handle(RsAst.RustExpr(it))
        }

        frontend.scopeManager.leaveScope(block)
        return block
    }

    fun handleLiteral(literal: RsLiteral): Expression {
        val stringValue = literal.astNode.text
        val raw = RsAst.RustExpr(RsExpr.Literal(literal))

        return when (literal.literalType) {
            RsLiteralType.CHAR_L ->
                newLiteral(stringValue[0], language.builtInTypes["char"] ?: unknownType(), raw)
            RsLiteralType.STRING_L ->
                newLiteral(stringValue, language.builtInTypes["str"] ?: unknownType(), raw)
            RsLiteralType.BYTE_L ->
                newLiteral(
                    stringValue.removePrefix("b'").removeSuffix("'").let {
                        if (it.startsWith("\\x")) it.removePrefix("\\x").toInt(16)
                        else it.toInt(256)
                    },
                    language.builtInTypes["u8"] ?: unknownType(),
                    raw,
                )
            RsLiteralType.C_STRING_L ->
                newLiteral(
                    stringValue.removePrefix("c").removeSuffix("'"),
                    objectType("CString"),
                    raw,
                )
            RsLiteralType.INT_NUMBER_L ->
                newLiteral(stringValue.toInt(), language.builtInTypes["str"] ?: unknownType(), raw)
            RsLiteralType.BYTE_STRING_L ->
                newLiteral(
                    stringValue.removePrefix("b").removeSuffix("'"),
                    language.builtInTypes["u8"] ?: unknownType().array(),
                    raw,
                )
            RsLiteralType.FLOAT_NUMBER_L ->
                newLiteral(
                    stringValue.substringBefore("f").toFloat(),
                    (if (stringValue.endsWith("f32")) language.builtInTypes["f32"]
                    else language.builtInTypes["f32"]) ?: unknownType(),
                    raw,
                )
            RsLiteralType.UNKNOWN_L ->
                newLiteral(stringValue, language.builtInTypes["str"] ?: unknownType(), raw)
        }
    }

    fun handleCallExpr(callExpr: RsCallExpr): Call {

        val callee: Expression? = callExpr.expr.getOrNull(0)?.let { handleNode(it) }

        val call = newCall(callee = callee, rawNode = RsAst.RustExpr(RsExpr.CallExpr(callExpr)))

        for (arg in callExpr.arguments) {
            call.arguments += handleNode(arg)
        }

        return call
    }

    fun handleMethodCallExpr(methodCallExpr: RsMethodCallExpr): MemberCall {

        val callee: Expression? =
            methodCallExpr.receiver.firstOrNull()?.let {
                val base = handleNode(it)

                methodCallExpr.nameRef?.let { call ->
                    newMemberAccess(call.text, base, rawNode = RsAst.RustExpr(RsExpr.NameRef(call)))
                }
            }

        val method =
            newMemberCall(
                callee = callee,
                rawNode = RsAst.RustExpr(RsExpr.MethodCallExpr(methodCallExpr)),
            )

        for (arg in methodCallExpr.arguments) {
            method.arguments += handleNode(arg)
        }

        return method
    }

    fun handleMacroExpr(macroExpr: RsMacroExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.MacroExpr(macroExpr))
        macroExpr.macroCall?.let {
            val base =
                it.path?.segment?.nameRef?.let {
                    newReference(it.text, rawNode = RsAst.RustExpr(RsExpr.NameRef(it)))
                }
            val call = newCall(callee = base, rawNode = raw)
            call.arguments += newLiteral(it.macroString)
            return call
        }

        return newProblemExpression(
            problem = "MacroExpression does not contain Macro Call",
            rawNode = raw,
        )
    }

    fun handlePathExpr(pathExpr: RsPathExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.PathExpr(pathExpr))

        pathExpr.segment?.nameRef?.let {
            return newReference(it.text, rawNode = raw)
        }

        return newProblemExpression(
            problem = "PathExpression does not contain reference to a name",
            rawNode = raw,
        )
    }

    fun handleRefExpr(refExpr: RsRefExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.RefExpr(refExpr))

        refExpr.expr.firstOrNull()?.let {
            val subExpr = handleNode(it)

            // We for now do not handle const and mut modifiers as they have no direct consequence
            // in control or data flow.
            // They are relevant to whether code is compilable, and therefore we may need to include
            // it as the type.
            return if (refExpr.isRef)
                newUnaryOperator(operatorCode = "&", postfix = false, prefix = true, rawNode = raw)
                    .also { unaryOp -> unaryOp.input = subExpr }
            else subExpr
        }

        return newProblemExpression(
            problem = "Reference expressions are not supported yet",
            rawNode = raw,
        )
    }

    fun handlePrefixExpr(prefixExpr: RsPrefixExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.PrefixExpr(prefixExpr))
        return newUnaryOperator(prefixExpr.operator, postfix = false, prefix = true, rawNode = raw)
            .also {
                it.input =
                    frontend.expressionHandler.handle(RsAst.RustExpr(prefixExpr.expr.first()))
            }
    }

    fun handleBinExpr(binExpr: RsBinExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.BinExpr(binExpr))
        if (binExpr.expressions.size == 2) {
            val lhs = frontend.expressionHandler.handle(RsAst.RustExpr(binExpr.expressions.first()))
            val rhs = frontend.expressionHandler.handle(RsAst.RustExpr(binExpr.expressions.last()))
            if (
                binExpr.operator in language.compoundAssignmentOperators ||
                    binExpr.operator in language.simpleAssignmentOperators
            ) {
                return newAssign(binExpr.operator, listOf(lhs), listOf(rhs), raw)
            }

            return newBinaryOperator(binExpr.operator, raw).also {
                it.lhs = lhs
                it.rhs = rhs
            }
        } else if (binExpr.expressions.size == 1) {
            return newUnaryOperator(
                    binExpr.operator,
                    postfix = false,
                    prefix = false,
                    rawNode = raw,
                )
                .also {
                    it.input =
                        frontend.expressionHandler.handle(
                            RsAst.RustExpr(binExpr.expressions.first())
                        )
                }
        }

        return newProblemExpression(
            problem =
                "Operator based expression has an incorrect amount of ${binExpr.expressions} operators",
            rawNode = raw,
        )
    }

    fun handleIfExpr(ifExpr: RsIfExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.IfExpr(ifExpr))
        val ifElse = newIfElse(raw)
        frontend.scopeManager.enterScope(ifElse)

        // Depending on whether the first expression is a let expression we want fo fill condition
        // or condition declaration
        ifExpr.expressions.first().let {
            val condExpr = frontend.expressionHandler.handle(RsAst.RustExpr(it))
            if (condExpr is DeclarationStatement) {
                // There should only be one declaration inside a let of an if
                ifElse.conditionDeclaration = condExpr.declarations.first()
            } else {
                ifElse.condition = condExpr
            }
        }

        ifExpr.expressions.getOrNull(1)?.let {
            ifElse.thenStatement = frontend.expressionHandler.handle(RsAst.RustExpr(it))
        }

        ifExpr.expressions.getOrNull(2)?.let {
            ifElse.elseStatement = frontend.expressionHandler.handle(RsAst.RustExpr(it))
        }

        frontend.scopeManager.leaveScope(ifElse)
        return ifElse
    }

    fun handleLetExpr(letExpr: RsLetExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.LetExpr(letExpr))

        val declarationStatement = newDeclarationStatement(rawNode = raw)

        val variable =
            newVariable(
                name = (letExpr.pat as? RsPat.IdentPat)?.v1?.name ?: "",
                type = unknownType(),
                rawNode = raw,
            )

        letExpr.expr.let {
            variable.initializer = frontend.expressionHandler.handle(RsAst.RustExpr(it.first()))
        }

        declarationStatement.declarations += variable

        declarationStatement.usedAsExpression = true

        return declarationStatement
    }

    fun handleWhileExpr(whileExpr: RsWhileExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.WhileExpr(whileExpr))

        val whileExpression = newWhile(raw)

        frontend.scopeManager.enterScope(whileExpression)

        whileExpr.expressions.first().let {
            val condExpr = frontend.expressionHandler.handle(RsAst.RustExpr(it))
            if (condExpr is DeclarationStatement) {
                // There should only be one declaration inside a let of an if
                whileExpression.conditionDeclaration = condExpr.declarations.first()
            } else {
                whileExpression.condition = condExpr
            }
        }

        whileExpr.expressions.getOrNull(1)?.let {
            whileExpression.statement = frontend.expressionHandler.handle(RsAst.RustExpr(it))
        }

        frontend.scopeManager.leaveScope(whileExpression)

        whileExpression.usedAsExpression = true

        return whileExpression
    }

    fun handleLoopExpr(loopExpr: RsLoopExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.LoopExpr(loopExpr))
        val whileExpression = newWhile(raw)

        frontend.scopeManager.enterScope(whileExpression)

        whileExpression.condition =
            newLiteral(true, language.builtInTypes["bool"] ?: unknownType(), raw).also {
                it.isImplicit = true
            }

        loopExpr.body.firstOrNull()?.let {
            whileExpression.statement = frontend.expressionHandler.handleBlockExpr(it)
        }

        frontend.scopeManager.leaveScope(whileExpression)

        whileExpression.usedAsExpression = true

        return whileExpression
    }

    fun handleForExpr(forExpr: RsForExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.ForExpr(forExpr))
        val forEach = newForEach(rawNode = raw)
        frontend.scopeManager.enterScope(forEach)

        val variable =
            newVariable(
                name = (forExpr.pat as? RsPat.IdentPat)?.v1?.name ?: "",
                type = unknownType(),
                rawNode = raw,
            )
        val declarationStatement = newDeclarationStatement()
        declarationStatement.singleDeclaration = variable

        forExpr.expressions.first().let {
            val iterable = frontend.expressionHandler.handle(RsAst.RustExpr(it))
            forEach.iterable = iterable
        }

        forExpr.expressions.getOrNull(1)?.let {
            forEach.statement = frontend.expressionHandler.handle(RsAst.RustExpr(it))
        }

        forEach.variable = declarationStatement

        frontend.scopeManager.leaveScope(forEach)

        forEach.usedAsExpression = true

        return forEach
    }

    fun handleBreakExpr(breakExpr: RsBreakExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.BreakExpr(breakExpr))

        val breakExpression = newBreak(raw)

        breakExpr.lifetime?.let { breakExpression.label = it.name }

        breakExpr.expr.firstOrNull()?.let {
            breakExpression.expr = frontend.expressionHandler.handle(RsAst.RustExpr(it))
            breakExpression.usedAsExpression = true
        }

        return breakExpression
    }

    fun handleContinueExpr(continueExpr: RsContinueExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.ContinueExpr(continueExpr))

        val continueExpression = newContinue(raw)

        continueExpr.lifetime?.let { continueExpression.label = it.name }

        return continueExpression
    }

    fun handleRangeExpr(rangeExpr: RsRangeExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.RangeExpr(rangeExpr))
        val range = newRange(rawNode = raw)

        rangeExpr.expressions.getOrNull(0)?.let {
            range.floor = frontend.expressionHandler.handle(RsAst.RustExpr(it))
        }

        rangeExpr.expressions.getOrNull(1)?.let {
            range.ceiling = frontend.expressionHandler.handle(RsAst.RustExpr(it))
        }

        range.operatorCode = rangeExpr.operator
        return range
    }

    fun handleFieldExpr(fieldExpr: RsFieldExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.FieldExpr(fieldExpr))

        fieldExpr.expr.first().let {
            val base = frontend.expressionHandler.handle(RsAst.RustExpr(it))
            fieldExpr.nameRef?.let { nameRef ->
                return newMemberAccess(name = nameRef.text, base = base, rawNode = raw)
            }
        }

        return newProblemExpression(
            problem = "FieldExpression does not contain a base expression or a name reference",
            rawNode = raw,
        )
    }

    fun handleCastExpr(castExpr: RsCastExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.CastExpr(castExpr))

        val input = frontend.expressionHandler.handle(RsAst.RustExpr(castExpr.expr.first()))

        val type = castExpr.ty.firstOrNull()?.let { frontend.typeOf(it) } ?: unknownType()

        return newCast(raw).also {
            it.expression = input
            it.castType = type
        }
    }

    fun handleIndexExpr(indexExpr: RsIndexExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.IndexExpr(indexExpr))

        if (indexExpr.expressions.size >= 2) {
            return newSubscription(rawNode = raw).also { subscription ->
                indexExpr.expressions.getOrNull(0)?.let {
                    subscription.arrayExpression =
                        frontend.expressionHandler.handle(RsAst.RustExpr(it))
                }

                indexExpr.expressions.getOrNull(1)?.let {
                    subscription.subscriptExpression =
                        frontend.expressionHandler.handle(RsAst.RustExpr(it))
                }
            }
        }

        return newProblemExpression(
            problem = "Index expressions was not parsed with two ore more expressions.",
            rawNode = raw,
        )
    }

    fun handleRecordExpr(recordExpr: RsRecordExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.RecordExpr(recordExpr))

        return newProblemExpression(
            problem =
                "RecordExpression needs more complex initialization, which is not supported yet",
            rawNode = raw,
        )
    }
}
