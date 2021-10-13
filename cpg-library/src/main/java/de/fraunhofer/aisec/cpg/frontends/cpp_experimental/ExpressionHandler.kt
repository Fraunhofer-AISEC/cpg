/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cpp_experimental

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.cpp_experimental.CXXExperimentalFrontend.Companion.visitChildren
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import org.bytedeco.llvm.clang.CXClientData
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.clang.CXCursorVisitor
import org.bytedeco.llvm.global.clang
import org.bytedeco.llvm.global.clang.*

class ExpressionHandler(lang: CXXExperimentalFrontend) :
    Handler<Expression, CXCursor, CXXExperimentalFrontend>(::Expression, lang) {
    init {
        map.put(CXCursor::class.java, ::handleExpression)
    }

    private fun handleExpression(cursor: CXCursor): Expression {
        val kind = clang.clang_getCursorKind(cursor)

        return when (kind) {
            clang.CXCursor_UnexposedExpr -> handleUnexposedExpr(cursor)
            clang.CXCursor_DeclRefExpr -> handleDeclRefExpr(cursor)
            clang.CXCursor_CallExpr -> handleCallExpr(cursor)
            clang.CXCursor_IntegerLiteral -> handleIntegerLiteral(cursor)
            clang.CXCursor_BinaryOperator -> handleBinaryOperator(cursor)
            else -> {
                log.error("Not handling cursor kind {} yet", kind)
                Expression()
            }
        }
    }

    private fun handleUnexposedExpr(cursor: CXCursor): Expression {
        var expression: Expression? = null

        // just seems to be a simple wrapper
        clang.clang_visitChildren(
            cursor,
            object : CXCursorVisitor() {
                override fun call(
                    child: CXCursor,
                    parent: CXCursor?,
                    client_data: CXClientData?
                ): Int {
                    println(
                        "unexposed: Cursor '" +
                            clang_getCursorSpelling(child).string +
                            "' of kind '" +
                            clang.clang_getCursorKindSpelling(clang.clang_getCursorKind(child))
                                .string +
                            "'"
                    )

                    expression = handleExpression(child)

                    return clang.CXCursor_BreakStmt
                }
            },
            null
        )

        return expression ?: Expression()
    }

    /**
     * Handles a [DeclRefExpr](https://clang.llvm.org/doxygen/classclang_1_1DeclRefExpr.html), which
     * is basically a reference to a declaration.
     */
    private fun handleDeclRefExpr(cursor: CXCursor): DeclaredReferenceExpression {
        val name = clang_getCursorSpelling(cursor)
        val type = lang.typeOf(cursor)

        val ref =
            NodeBuilder.newDeclaredReferenceExpression(
                name.string,
                type,
                lang.getCodeFromRawNode(cursor)
            )

        return ref
    }

    /**
     * Handles a [CallExpr](https://clang.llvm.org/doxygen/classclang_1_1CallExpr.html), which
     * represents a function call and is mapped to a [CallExpression].
     */
    private fun handleCallExpr(cursor: CXCursor): CallExpression {
        val name = clang_getCursorSpelling(cursor).string
        val fqn = name

        val call = newCallExpression(name, fqn, lang.getCodeFromRawNode(cursor), false)

        val length = clang_Cursor_getNumArguments(cursor)

        for (i in 0 until length) {
            val arg = lang.expressionHandler.handle(clang_Cursor_getArgument(cursor, i))
            call.addArgument(arg)
        }

        return call
    }

    private fun handleIntegerLiteral(cursor: CXCursor): Expression {
        val result = clang.clang_Cursor_Evaluate(cursor)
        val value = clang.clang_EvalResult_getAsInt(result)

        val literal =
            NodeBuilder.newLiteral(value, lang.typeOf(cursor), lang.getCodeFromRawNode(cursor))

        return literal
    }

    private fun handleBinaryOperator(cursor: CXCursor): Expression {
        // Houston, we have a problem. Currently, it is impossible to get the binary operator using
        // the C API. See https://reviews.llvm.org/D10833#inline-639821 for a proposed but stale
        // patch and https://bugs.llvm.org/show_bug.cgi?id=28768 for the upstream bug.
        val opCode = ""

        val binOp = NodeBuilder.newBinaryOperator(opCode, lang.getCodeFromRawNode(cursor))

        visitChildren(
            cursor,
            { lang.expressionHandler.handle(it) },
            { it, i ->
                if (i == 0) {
                    binOp.lhs = it
                } else if (i == 1) {
                    binOp.rhs = it
                }
            },
            0,
            2 // max 2 arguments
        )

        return binOp
    }
}
