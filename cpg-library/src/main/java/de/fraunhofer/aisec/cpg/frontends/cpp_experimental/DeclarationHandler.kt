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
import org.bytedeco.llvm.global.clang.*

class DeclarationHandler(lang: CXXExperimentalFrontend) :
    Handler<Declaration, Pointer, CXXExperimentalFrontend>(::Declaration, lang) {
    init {
        map.put(CXCursor::class.java) { handleDeclaration(it as CXCursor) }
        map.put(CXTranslationUnit::class.java) { handleTranslationUnit(it as CXTranslationUnit) }
    }

    private fun handleDeclaration(cursor: CXCursor): Declaration {
        return when (val kind = clang_getCursorKind(cursor)) {
            CXCursor_FunctionDecl -> handleFunctionDecl(cursor)
            CXCursor_ParmDecl -> handleParmVarDecl(cursor)
            else -> {
                log.error("Not handling cursor kind {} yet", kind)
                Declaration()
            }
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
                            clang_getCursorSpelling(child).string +
                            "' of kind '" +
                            clang_getCursorKindSpelling(clang_getCursorKind(child)).string +
                            "'"
                    )

                    val decl = handle(child)
                    lang.scopeManager.addDeclaration(decl)

                    return CXChildVisit_Continue
                }
            }

        val cursor = clang_getTranslationUnitCursor(unit)
        clang_visitChildren(cursor, visitor, null)

        return tu
    }

    /**
     * Handles a [FunctionDecl](https://clang.llvm.org/doxygen/classclang_1_1FunctionDecl.html),
     * which is either a function declaration or a definition.
     */
    private fun handleFunctionDecl(cursor: CXCursor): FunctionDeclaration {
        val name = clang_getCursorSpelling(cursor)
        val type = lang.typeOf(cursor)

        val decl = NodeBuilder.newFunctionDeclaration(name.string, "")
        decl.type = type

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
                            clang_getCursorSpelling(child).string +
                            "' of kind '" +
                            clang_getCursorKindSpelling(clang_getCursorKind(child)).string +
                            "'"
                    )

                    val kind = clang_getCursorKind(child)

                    if (kind in CXCursor_FirstDecl..CXCursor_LastDecl) {
                        lang.scopeManager.addDeclaration(handle(child))
                    } else if (kind in CXCursor_FirstStmt..CXCursor_LastStmt) {
                        decl.body = lang.statementHandler.handle(child)
                    }

                    return CXChildVisit_Continue
                }
            }

        clang_visitChildren(cursor, visitor, null)

        lang.scopeManager.leaveScope(decl)

        return decl
    }

    /**
     * Handles a [ParmVarDecl](https://clang.llvm.org/doxygen/classclang_1_1ParmVarDecl.html), which
     * is a parameter of a function.
     */
    private fun handleParmVarDecl(cursor: CXCursor): ParamVariableDeclaration {
        val name = clang_getCursorDisplayName(cursor)
        val type = lang.typeOf(cursor)

        val param = NodeBuilder.newMethodParameterIn(name.string, type, false, "")

        return param
    }
}
