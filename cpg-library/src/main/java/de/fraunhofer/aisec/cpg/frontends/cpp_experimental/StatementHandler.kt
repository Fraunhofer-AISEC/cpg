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
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import org.bytedeco.llvm.clang.CXClientData
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.clang.CXCursorVisitor
import org.bytedeco.llvm.global.clang
import org.bytedeco.llvm.global.clang.CXCursor_FirstExpr
import org.bytedeco.llvm.global.clang.CXCursor_LastExpr

class StatementHandler(lang: CXXExperimentalFrontend) :
    Handler<Statement, CXCursor, CXXExperimentalFrontend>(::Statement, lang) {
    init {
        map.put(CXCursor::class.java, ::visitStatement)
    }

    private fun visitStatement(cursor: CXCursor): Statement {
        val kind = clang.clang_getCursorKind(cursor)

        return when (kind) {
            // forward expressions to the expression handler
            in CXCursor_FirstExpr..CXCursor_LastExpr -> lang.expressionHandler.handle(cursor)
            clang.CXCursor_CompoundStmt -> handleCompoundStatement(cursor)
            clang.CXCursor_ReturnStmt -> handleReturnStatement(cursor)
            else -> Statement()
        }
    }

    private fun handleCompoundStatement(cursor: CXCursor): CompoundStatement {
        val compoundStatement = NodeBuilder.newCompoundStatement("")

        clang.clang_visitChildren(
            cursor,
            object : CXCursorVisitor() {
                override fun call(
                    child: CXCursor,
                    parent: CXCursor?,
                    client_data: CXClientData?
                ): Int {
                    println(
                        "C: Cursor '" +
                            clang.clang_getCursorSpelling(child).string +
                            "' of kind '" +
                            clang.clang_getCursorKindSpelling(clang.clang_getCursorKind(child))
                                .string +
                            "'"
                    )

                    val stmt = handle(child)
                    compoundStatement.addStatement(stmt)

                    return clang.CXChildVisit_Continue
                }
            },
            null
        )

        return compoundStatement
    }

    private fun handleReturnStatement(cursor: CXCursor): ReturnStatement {
        val returnStatement = NodeBuilder.newReturnStatement("")

        clang.clang_visitChildren(
            cursor,
            object : CXCursorVisitor() {
                override fun call(
                    child: CXCursor,
                    parent: CXCursor?,
                    client_data: CXClientData?
                ): Int {
                    println(
                        "R: Cursor '" +
                            clang.clang_getCursorSpelling(child).string +
                            "' of kind '" +
                            clang.clang_getCursorKindSpelling(clang.clang_getCursorKind(child))
                                .string +
                            "'"
                    )

                    val expr = lang.expressionHandler.handle(child)
                    returnStatement.returnValue = expr

                    return clang.CXChildVisit_Continue
                }
            },
            null
        )

        return returnStatement
    }
}
