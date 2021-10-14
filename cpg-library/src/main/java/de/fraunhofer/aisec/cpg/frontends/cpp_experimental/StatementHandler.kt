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
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import org.bytedeco.llvm.clang.CXClientData
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.clang.CXCursorVisitor
import org.bytedeco.llvm.global.clang
import org.bytedeco.llvm.global.clang.*

class StatementHandler(lang: CXXExperimentalFrontend) :
    Handler<Statement, CXCursor, CXXExperimentalFrontend>(::Statement, lang) {
    init {
        map.put(CXCursor::class.java, ::visitStatement)
    }

    private fun visitStatement(cursor: CXCursor): Statement {
        val kind = clang_getCursorKind(cursor)

        return when (kind) {
            // forward expressions to the expression handler
            in CXCursor_FirstExpr..CXCursor_LastExpr -> lang.expressionHandler.handle(cursor)
            CXCursor_CompoundStmt -> handleCompoundStatement(cursor)
            CXCursor_ReturnStmt -> handleReturnStatement(cursor)
            CXCursor_DeclStmt -> handleDeclStmt(cursor)
            else -> Statement()
        }
    }

    private fun handleCompoundStatement(cursor: CXCursor): CompoundStatement {
        val compoundStatement = NodeBuilder.newCompoundStatement("")

        visitChildren(
            cursor,
            { handle(it) },
            { it, _ -> compoundStatement.addStatement(it) },
        )

        return compoundStatement
    }

    private fun handleReturnStatement(cursor: CXCursor): ReturnStatement {
        val returnStatement = NodeBuilder.newReturnStatement("")

        clang_visitChildren(
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
                            clang.clang_getCursorKindSpelling(clang_getCursorKind(child)).string +
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

    /**
     * Handles a [DeclStmt](https://clang.llvm.org/doxygen/classclang_1_1DeclStmt.html), which
     * represents a statement that declares something.
     */
    private fun handleDeclStmt(cursor: CXCursor): Statement {
        val stmt = NodeBuilder.newDeclarationStatement(lang.getCodeFromRawNode(cursor))

        var size = clang_Cursor_getNumArguments(cursor)

        val type = clang_getTypeSpelling(clang_getCursorType(cursor))

        println(size)
        println(type.string)

        visitChildren(
            cursor,
            {
                println("inside handleDeclStmt pre-handle")
                val expr = lang.declarationHandler.handle(it)
                println("inside handleDeclStmt post-handle")
                expr
            },
            { it, _ ->
                println("inside handleDeclStmt pre-add")
                stmt.addToPropertyEdgeDeclaration(it)
                lang.scopeManager.addDeclaration(it)
                println("inside handleDeclStmt post-add")
            },
        )

        return stmt
    }
}
