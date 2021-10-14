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
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import org.bytedeco.javacpp.Pointer
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang.*

class DeclarationHandler(lang: CXXExperimentalFrontend) :
    Handler<Declaration, Pointer, CXXExperimentalFrontend>(::Declaration, lang) {
    init {
        map.put(CXCursor::class.java) { handleDeclaration(it as CXCursor) }
        map.put(CXTranslationUnit::class.java) {
            handleTranslationUnitDecl(it as CXTranslationUnit)
        }
    }

    private fun handleDeclaration(cursor: CXCursor): Declaration {
        return when (val kind = clang_getCursorKind(cursor)) {
            CXCursor_FunctionDecl -> handleFunctionDecl(cursor)
            CXCursor_VarDecl -> handleVarDecl(cursor)
            CXCursor_ParmDecl -> handleParmVarDecl(cursor)
            else -> {
                log.error("Not handling cursor kind {} yet", kind)
                Declaration()
            }
        }
    }

    /**
     * Handles a
     * [TranslationUnitDecl](https://clang.llvm.org/doxygen/classclang_1_1TranslationUnitDecl.html)
     * which represents a translation unit containing all other nodes.
     */
    private fun handleTranslationUnitDecl(unit: CXTranslationUnit): Declaration {
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
        val name = clang_getCursorSpelling(cursor).string
        val type = lang.typeOf(cursor)

        val decl = NodeBuilder.newFunctionDeclaration(name, "")
        decl.type = type

        lang.scopeManager.enterScope(decl)

        clang_visitChildren(
            cursor,
            object : CXCursorVisitor() {
                override fun call(
                    child: CXCursor,
                    parent: CXCursor?,
                    client_data: CXClientData?
                ): Int {
                    val kind = clang_getCursorKind(child)

                    if (kind in CXCursor_FirstDecl..CXCursor_LastDecl) {
                        lang.scopeManager.addDeclaration(handle(child))
                    } else if (kind in CXCursor_FirstStmt..CXCursor_LastStmt) {
                        decl.body = lang.statementHandler.handle(child)

                        return CXChildVisit_Break
                    }

                    return CXChildVisit_Continue
                }
            },
            null
        )

        lang.scopeManager.leaveScope(decl)

        return decl
    }

    /**
     * Handles a [ParmVarDecl](https://clang.llvm.org/doxygen/classclang_1_1ParmVarDecl.html), which
     * is a parameter of a function.
     */
    private fun handleParmVarDecl(cursor: CXCursor): ParamVariableDeclaration {
        val name = clang_getCursorSpelling(cursor).string
        val type = lang.typeOf(cursor)

        val param = NodeBuilder.newMethodParameterIn(name, type, false, "")

        return param
    }

    /** Handles a [VarDecl](https://clang.llvm.org/doxygen/classclang_1_1VarDecl.html). */
    private fun handleVarDecl(cursor: CXCursor): VariableDeclaration {
        val name = clang_getCursorSpelling(cursor).string
        var type = lang.typeOf(cursor)

        // be aware, that if clang considers the declaration to be "invalid", e.g. if the type is
        // not known, it will NOT parse the initializer
        var initCursor = clang_Cursor_getVarDeclInitializer(cursor)

        // we need to use clang_Cursor_isNull to check for NULL
        if (clang_Cursor_isNull(initCursor) != 0) {
            initCursor = null
        }

        val code = lang.getCodeFromRawNode(cursor)

        // clang defaults back to those types if they are not known, so we try to guess them
        if (type.typeName == "int*" || type.typeName == "int") {
            type = guessType(code, initCursor, name) ?: type
        }

        val decl = NodeBuilder.newVariableDeclaration(name, type, code, false)

        initCursor?.let { decl.initializer = lang.expressionHandler.handle(it) }

        return decl
    }

    private fun guessType(code: String?, initCursor: CXCursor?, name: String): Type? {
        val initializerCode = lang.getCodeFromRawNode(initCursor)

        // clang's type system will not see the original type if it is an unknown type, so we need
        // to some dirty tricks here
        val type =
            code?.substring(0, code.length - name.length - (initializerCode?.length?.plus(1) ?: 0))
                ?.let { TypeParser.createFrom(it, false) }

        if (type is UnknownType) {
            return null
        }

        return type
    }
}
