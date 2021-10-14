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
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang.*

private val CXCursor.prettyAddress: String
    get(): String {
        return "0x${this.address().toString(16)}"
    }

/**
 * This is an experimental frontend for the [TypeManager.Language.CXX] language based on
 * [clang](http://clang.llvm.org). In contrast to the [CXXLanguageFrontend], which is based on
 * Eclipse CDT, this frontend does not (yet) focus on incomplete code.
 *
 * While clang does offer options to parse incomplete code, it seems that this is not working
 * correctly yet. Therefore, this frontend is primarily focused on code that most likely actually
 * compiles. Incomplete code will probably be parsed, but do not expect the quality level of
 * inference of incomplete code as in the current [CXXLanguageFrontend].
 *
 * Beware, that using clang also means that a set of default header locations will be used, mainly
 * the system headers. This might be a good thing, but also a bad thing if you want to analyze
 * something from a different target.
 */
class CXXExperimentalFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, "::") {

    val declarationHandler = DeclarationHandler(this)
    val statementHandler = StatementHandler(this)
    val expressionHandler = ExpressionHandler(this)

    override fun parse(file: File): TranslationUnitDeclaration {
        TypeManager.getInstance().setLanguageFrontend(this)

        val index = clang_createIndex(0, 0)

        val unit =
            clang_parseTranslationUnit(
                index,
                file.path,
                ByteArray(0),
                0,
                null,
                0,
                // somehow these options are not really effective (yet)
                CXTranslationUnit_Incomplete or
                    CXTranslationUnit_KeepGoing or
                    CXTranslationUnit_SingleFileParse
            )

        if (unit == null) {
            log.error("Could not parse")
            throw TranslationException("nope")
        }

        dump(clang_getTranslationUnitCursor(unit))

        val tu = this.declarationHandler.handle(unit) as TranslationUnitDeclaration

        clang_disposeIndex(index)
        clang_disposeTranslationUnit(unit)

        return tu
    }

    fun typeOf(cursor: CXCursor): Type {
        val typeRef = clang_getCursorType(cursor) ?: return UnknownType.getUnknownType()

        return typeFrom(typeRef)
    }

    fun typeFrom(typeRef: CXType): Type {
        val kind = typeRef.kind()

        return when (kind) {
            CXType_Pointer -> {
                val elementType = clang_getPointeeType(typeRef)

                typeFrom(elementType).reference(PointerType.PointerOrigin.POINTER)
            }
            else -> {
                val name = clang_getTypeSpelling(typeRef).string

                return TypeParser.createFrom(name, false)
            }
        }
    }

    override fun <T : Any?> getCodeFromRawNode(astNode: T): String? {
        if (astNode is CXCursor) {
            var code = ""

            val unit = clang_Cursor_getTranslationUnit(astNode)
            var loc = clang_getCursorLocation(astNode)

            val range = clang_getCursorExtent(astNode)

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
        return null
    }

    override fun <S : Any?, T : Any?> setComment(s: S, ctx: T) {}

    companion object {
        internal fun <T : Node> visitChildren(
            cursor: CXCursor,
            function: (CXCursor) -> T,
            apply: (T, Int) -> Unit,
            after: Int = 0,
            before: Int = -1,
        ) {
            println(
                "Invoking clang_visitChildren on ${cursor.prettyAddress}" +
                    clang_getCursorSpelling(cursor).string +
                    "' of kind '" +
                    clang_getCursorKindSpelling(clang_getCursorKind(cursor)).string +
                    "'"
            )

            clang_visitChildren(
                cursor,
                object : CXCursorVisitor() {
                    override fun call(
                        child: CXCursor,
                        parent: CXCursor?,
                        client_data: CXClientData?
                    ): Int {
                        println("${child.prettyAddress}: still here 1")
                        val idx =
                            client_data?.getPointer(IntPointer::class.java, 0) ?: IntPointer(0)

                        if (child == null) {
                            println("problem")
                        }

                        if (clang_Cursor_isNull(child) != 0) {
                            println("problem")
                        }

                        println(
                            "${child.prettyAddress}: Cursor '" +
                                clang_getCursorSpelling(child).string +
                                "' of kind '" +
                                clang_getCursorKindSpelling(clang_getCursorKind(child)).string +
                                "'"
                        )
                        println("${child.prettyAddress}: still here 2")

                        val i = idx.get()

                        if (before != -1 && i >= before) {
                            return CXChildVisit_Break
                        }

                        println("${child.prettyAddress}: still here 3")

                        if (i >= after) {
                            println("${child.prettyAddress}: still here 4")
                            var result = function(child)
                            println("${child.prettyAddress}: still here 5")
                            apply(result, i)
                        }

                        println("${child.prettyAddress}: still here 6")

                        idx.put(0L, i + 1)

                        println("${child.prettyAddress}: still here 7")

                        return CXChildVisit_Continue
                    }
                },
                CXClientData(IntPointer(0))
            )
        }
    }

    fun dump(cursor: CXCursor) {
        val treeLevel = IntPointer(0)

        clang_visitChildren(cursor, visitor, CXClientData(treeLevel))
    }

    object visitor : CXCursorVisitor() {
        override fun call(cursor: CXCursor, parent: CXCursor?, client_data: CXClientData): Int {
            val kind = clang_getCursorKind(cursor)

            val currentLevel = client_data.getPointer(IntPointer::class.java).get()
            val nextLevel = currentLevel + 1

            println(
                "-".repeat(currentLevel) +
                    " " +
                    clang_getCursorKindSpelling(kind).string +
                    " (" +
                    clang_getCursorSpelling(cursor).string +
                    ")"
            )

            clang_visitChildren(cursor, visitor, CXClientData(IntPointer(nextLevel)))

            return CXChildVisit_Continue
        }
    }
}
