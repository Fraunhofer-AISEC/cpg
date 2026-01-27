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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import uniffi.cpgrust.RsAst
import uniffi.cpgrust.RsBinExpr
import uniffi.cpgrust.RsBlockExpr
import uniffi.cpgrust.RsCallExpr
import uniffi.cpgrust.RsExpr
import uniffi.cpgrust.RsLiteral
import uniffi.cpgrust.RsLiteralType
import uniffi.cpgrust.RsMacroExpr
import uniffi.cpgrust.RsMethodCallExpr
import uniffi.cpgrust.RsPathExpr

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

    fun handleCallExpr(callExpr: RsCallExpr): CallExpression {

        val callee: Expression? = callExpr.expr.getOrNull(0)?.let { handleNode(it) }

        val call =
            newCallExpression(callee = callee, rawNode = RsAst.RustExpr(RsExpr.CallExpr(callExpr)))

        for (arg in callExpr.arguments) {
            call.arguments += handleNode(arg)
        }

        return call
    }

    fun handleMethodCallExpr(methodCallExpr: RsMethodCallExpr): MemberCallExpression {

        val callee: Expression? =
            methodCallExpr.receiver.firstOrNull()?.let {
                val base = handleNode(it)

                methodCallExpr.nameRef?.let { call ->
                    newMemberExpression(
                        call.text,
                        base,
                        rawNode = RsAst.RustExpr(RsExpr.NameRef(call)),
                    )
                }
            }

        val method =
            newMemberCallExpression(
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
            val call = newCallExpression(callee = base, rawNode = raw)
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

    fun handleBinExpr(binExpr: RsBinExpr): Expression {
        val raw = RsAst.RustExpr(RsExpr.BinExpr(binExpr))
        if (binExpr.expressions.size == 2) {
            return newBinaryOperator(binExpr.operator, raw).also {
                it.lhs =
                    frontend.expressionHandler.handle(RsAst.RustExpr(binExpr.expressions.first()))
                it.rhs =
                    frontend.expressionHandler.handle(RsAst.RustExpr(binExpr.expressions.last()))
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
}
