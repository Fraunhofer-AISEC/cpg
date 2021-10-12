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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParamVariableDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.llvm.clang.CXClientData
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.clang.CXCursorVisitor
import org.bytedeco.llvm.clang.CXToken
import org.bytedeco.llvm.global.clang.*

class CXXExperimentalFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, "::") {
    override fun parse(file: File): TranslationUnitDeclaration {
        var tu = NodeBuilder.newTranslationUnitDeclaration("", "")

        TypeManager.getInstance().setLanguageFrontend(this)

        var index = clang_createIndex(0, 0)

        var unit =
            clang_parseTranslationUnit(
                index,
                file.path,
                null as? ByteArray,
                0,
                null,
                0,
                CXTranslationUnit_Incomplete
            )

        if (unit == null) {
            log.error("Could not parse")
            throw TranslationException("nope")
        }

        scopeManager.resetToGlobal(tu)

        var visitor =
            object : CXCursorVisitor() {
                override fun call(
                    child: CXCursor,
                    parent: CXCursor?,
                    client_data: CXClientData?
                ): Int {
                    println(
                        "Cursor '" +
                            clang_getCursorSpelling(child).string +
                            "' of kind '" +
                            clang_getCursorKindSpelling(clang_getCursorKind(child)).string +
                            "'"
                    )

                    var kind = clang_getCursorKind(child)

                    var decl: Declaration? = null
                    if (kind == CXCursor_FunctionDecl) {
                        decl = handleFunctionDecl(child)
                    }

                    if (decl != null) {
                        scopeManager.addDeclaration(decl)
                    }

                    return CXChildVisit_Continue
                }
            }

        var cursor = clang_getTranslationUnitCursor(unit)
        clang_visitChildren(cursor, visitor, null)

        clang_disposeIndex(index)
        clang_disposeTranslationUnit(unit)

        return tu
    }

    private fun handleFunctionDecl(cursor: CXCursor): FunctionDeclaration {
        val name = clang_getCursorSpelling(cursor)

        val decl = NodeBuilder.newFunctionDeclaration(name.string, "")

        scopeManager.enterScope(decl)

        val visitor =
            object : CXCursorVisitor() {
                override fun call(
                    child: CXCursor,
                    parent: CXCursor?,
                    client_data: CXClientData?
                ): Int {
                    println(
                        "F: Cursor '" +
                            clang_getCursorSpelling(child).string +
                            "' of kind '" +
                            clang_getCursorKindSpelling(clang_getCursorKind(child)).string +
                            "'"
                    )

                    val kind = clang_getCursorKind(child)

                    if (kind == CXCursor_ParmDecl) {
                        val param = handleParmDecl(child)

                        scopeManager.addDeclaration(param)
                    } else if (kind == CXCursor_CompoundStmt) {
                        val stmt = handleCompoundStatement(child)

                        decl.body = stmt
                    }

                    return CXChildVisit_Continue
                }
            }

        clang_visitChildren(cursor, visitor, null)

        scopeManager.leaveScope(decl)

        return decl
    }

    private fun handleCompoundStatement(cursor: CXCursor): CompoundStatement {
        val compoundStatement = NodeBuilder.newCompoundStatement("")

        clang_visitChildren(
            cursor,
            object : CXCursorVisitor() {
                override fun call(
                    child: CXCursor,
                    parent: CXCursor?,
                    client_data: CXClientData?
                ): Int {
                    println(
                        "C: Cursor '" +
                            clang_getCursorSpelling(child).string +
                            "' of kind '" +
                            clang_getCursorKindSpelling(clang_getCursorKind(child)).string +
                            "'"
                    )

                    val stmt = visitStatement(child)
                    compoundStatement.addStatement(stmt)

                    return CXChildVisit_Continue
                }
            },
            null
        )

        return compoundStatement
    }

    private fun visitStatement(cursor: CXCursor): Statement {
        val kind = clang_getCursorKind(cursor)

        return when (kind) {
            CXCursor_ReturnStmt -> handleReturnStatement(cursor)
            CXCursor_CompoundStmt -> handleCompoundStatement(cursor)
            else -> Statement()
        }
    }

    private fun handleReturnStatement(cursor: CXCursor): ReturnStatement {
        var returnStatement = NodeBuilder.newReturnStatement("")

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
                            clang_getCursorSpelling(child).string +
                            "' of kind '" +
                            clang_getCursorKindSpelling(clang_getCursorKind(child)).string +
                            "'"
                    )

                    val expr = handleExpression(child)
                    returnStatement.returnValue = expr

                    return CXChildVisit_Continue
                }
            },
            null
        )

        return returnStatement
    }

    private fun handleExpression(cursor: CXCursor): Expression {
        val kind = clang_getCursorKind(cursor)

        return when (kind) {
            CXCursor_BinaryOperator -> handleBinaryOperator(cursor)
            CXCursor_CallExpr -> handleCallExpr(cursor)
            CXCursor_UnexposedExpr -> handleUnexposedExpr(cursor)
            CXCursor_DeclRefExpr -> handleDeclRefExpr(cursor)
            CXCursor_IntegerLiteral -> handleIntegerLiteral(cursor)
            else -> Expression()
        }
    }

    private fun handleIntegerLiteral(cursor: CXCursor): Expression {
        val result = clang_Cursor_Evaluate(cursor)
        val value = clang_EvalResult_getAsInt(result)

        val literal = NodeBuilder.newLiteral(value, typeOf(cursor), getCodeFromRawNode(cursor))

        return literal
    }

    private fun handleDeclRefExpr(cursor: CXCursor): Expression {
        val name = clang_getCursorSpelling(cursor)
        val type = typeOf(cursor)

        val ref =
            NodeBuilder.newDeclaredReferenceExpression(
                name.string,
                type,
                getCodeFromRawNode(cursor)
            )

        return ref
    }

    private fun handleUnexposedExpr(cursor: CXCursor): Expression {
        var expression: Expression? = null

        // just seems to be a simple wrapper
        clang_visitChildren(
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
                            clang_getCursorKindSpelling(clang_getCursorKind(child)).string +
                            "'"
                    )

                    expression = handleExpression(child)

                    return CXCursor_BreakStmt
                }
            },
            null
        )

        return expression ?: Expression()
    }

    private fun handleCallExpr(cursor: CXCursor): Expression {
        return Expression()
    }

    private fun handleBinaryOperator(cursor: CXCursor): Expression {
        // Houston, we have a problem. Currently it is impossible to get the binary operator using
        // the C API. See https://reviews.llvm.org/D10833#inline-639821 for a proposed but stale
        // patch and https://bugs.llvm.org/show_bug.cgi?id=28768 for the upstream bug.
        val opCode = ""

        val binOp = NodeBuilder.newBinaryOperator(opCode, getCodeFromRawNode(cursor))

        clang_visitChildren(
            cursor,
            object : CXCursorVisitor() {
                override fun call(
                    child: CXCursor,
                    parent: CXCursor?,
                    client_data: CXClientData
                ): Int {
                    val idx = client_data.getPointer(IntPointer::class.java, 0)

                    println(
                        "binOp: Cursor '" +
                            clang_getCursorSpelling(child).string +
                            "' of kind '" +
                            clang_getCursorKindSpelling(clang_getCursorKind(child)).string +
                            "'"
                    )

                    if (idx.get() == 0) {
                        binOp.lhs = handleExpression(child)
                        idx.put(0L, 1)
                        return CXChildVisit_Continue
                    }

                    binOp.rhs = handleExpression(child)
                    return CXChildVisit_Break
                }
            },
            CXClientData(IntPointer(0))
        )

        return binOp
    }

    private fun handleParmDecl(cursor: CXCursor): ParamVariableDeclaration {
        var name = clang_getCursorDisplayName(cursor)

        var type = typeOf(cursor)

        var param = NodeBuilder.newMethodParameterIn(name.string, type, false, "")

        return param
    }

    private fun typeOf(cursor: CXCursor): Type {
        val type = clang_getCursorType(cursor) ?: return UnknownType.getUnknownType()

        return TypeParser.createFrom(clang_getTypeSpelling(type).string, false)
    }

    override fun <T : Any?> getCodeFromRawNode(astNode: T): String? {
        if (astNode is CXCursor) {
            var code = ""

            var unit = clang_Cursor_getTranslationUnit(astNode)
            var loc = clang_getCursorLocation(astNode)

            var range = clang_getCursorExtent(astNode)

            val tokens = CXToken()
            val numTokens = IntArray(1)
            clang_tokenize(unit, range, tokens, numTokens)

            for (i in 0L until numTokens[0]) {
                val token = tokens.position(i)
                if (token != null) {
                    code += clang_getTokenSpelling(unit, token).string
                }
            }

            return code
        }

        return null
    }

    override fun <T : Any?> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        TODO("Not yet implemented")
    }

    override fun <S : Any?, T : Any?> setComment(s: S, ctx: T) {
        TODO("Not yet implemented")
    }
}
