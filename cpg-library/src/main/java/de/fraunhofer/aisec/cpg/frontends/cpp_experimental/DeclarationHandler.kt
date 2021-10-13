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
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParamVariableDeclaration
import org.bytedeco.javacpp.Pointer
import org.bytedeco.llvm.clang.CXClientData
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.clang.CXCursorVisitor
import org.bytedeco.llvm.clang.CXTranslationUnit
import org.bytedeco.llvm.global.clang

class DeclarationHandler(lang: CXXExperimentalFrontend) :
    Handler<Declaration, Pointer, CXXExperimentalFrontend>(::Declaration, lang) {
    init {
        map.put(CXCursor::class.java) { handleDeclaration(it as CXCursor) }
        map.put(CXTranslationUnit::class.java) { handleTranslationUnit(it as CXTranslationUnit) }
    }

    private fun handleDeclaration(cursor: CXCursor): Declaration {
        val kind = clang.clang_getCursorKind(cursor)

        return if (kind == clang.CXCursor_FunctionDecl) {
            handleFunctionDecl(cursor)
        } else {
            Declaration()
        }
    }

    private fun handleTranslationUnit(unit: CXTranslationUnit): Declaration {
        val tu = NodeBuilder.newTranslationUnitDeclaration("", "")

        lang.scopeManager.resetToGlobal(tu)

        val visitor =
            object : CXCursorVisitor() {
                override fun call(
                    child: CXCursor,
                    parent: CXCursor?,
                    client_data: CXClientData?
                ): Int {
                    println(
                        "Cursor '" +
                            clang.clang_getCursorSpelling(child).string +
                            "' of kind '" +
                            clang.clang_getCursorKindSpelling(clang.clang_getCursorKind(child))
                                .string +
                            "'"
                    )

                    var decl = handle(child)

                    if (decl != null) {
                        lang.scopeManager.addDeclaration(decl)
                    }

                    return clang.CXChildVisit_Continue
                }
            }

        var cursor = clang.clang_getTranslationUnitCursor(unit)
        clang.clang_visitChildren(cursor, visitor, null)

        return tu
    }

    private fun handleFunctionDecl(cursor: CXCursor): FunctionDeclaration {
        val name = clang.clang_getCursorSpelling(cursor)

        val decl = NodeBuilder.newFunctionDeclaration(name.string, "")

        lang.scopeManager.enterScope(decl)

        val visitor =
            object : CXCursorVisitor() {
                override fun call(
                    child: CXCursor,
                    parent: CXCursor?,
                    client_data: CXClientData?
                ): Int {
                    println(
                        "F: Cursor '" +
                            clang.clang_getCursorSpelling(child).string +
                            "' of kind '" +
                            clang.clang_getCursorKindSpelling(clang.clang_getCursorKind(child))
                                .string +
                            "'"
                    )

                    val kind = clang.clang_getCursorKind(child)

                    if (kind == clang.CXCursor_ParmDecl) {
                        val param = handleParmDecl(child)

                        lang.scopeManager.addDeclaration(param)
                    } else if (kind == clang.CXCursor_CompoundStmt) {
                        decl.body = lang.statementHandler.handle(child)
                    }

                    return clang.CXChildVisit_Continue
                }
            }

        clang.clang_visitChildren(cursor, visitor, null)

        lang.scopeManager.leaveScope(decl)

        return decl
    }

    private fun handleParmDecl(cursor: CXCursor): ParamVariableDeclaration {
        val name = clang.clang_getCursorDisplayName(cursor)

        val type = lang.typeOf(cursor)

        val param = NodeBuilder.newMethodParameterIn(name.string, type, false, "")

        return param
    }
}
