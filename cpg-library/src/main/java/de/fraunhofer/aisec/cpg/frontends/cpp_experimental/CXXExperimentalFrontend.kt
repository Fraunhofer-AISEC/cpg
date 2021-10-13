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
import de.fraunhofer.aisec.cpg.frontends.java.ExpressionHandler
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.clang.CXToken
import org.bytedeco.llvm.global.clang.*

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

        val tu = this.declarationHandler.handle(unit) as TranslationUnitDeclaration

        clang_disposeIndex(index)
        clang_disposeTranslationUnit(unit)

        return tu
    }

    fun typeOf(cursor: CXCursor): Type {
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
        return null
    }

    override fun <S : Any?, T : Any?> setComment(s: S, ctx: T) {}
}
