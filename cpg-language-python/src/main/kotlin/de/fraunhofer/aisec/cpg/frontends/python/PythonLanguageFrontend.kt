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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.SupportsParallelParsing
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import jep.JepException

@SupportsParallelParsing(false)
class PythonLanguageFrontend(
    language: Language<PythonLanguageFrontend>,
    config: TranslationConfiguration,
    scopeManager: ScopeManager
) : LanguageFrontend(language, config, scopeManager) {
    private val jep = JepSingleton // configure Jep

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        return parseInternal(file.readText(Charsets.UTF_8), file.path)
    }

    override fun <T> getCodeFromRawNode(astNode: T): String? {
        // will be invoked by native function
        return null
    }

    override fun <T> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        // will be invoked by native function
        return null
    }

    override fun <S, T> setComment(s: S, ctx: T) {
        // will be invoked by native function
    }

    private fun parseInternal(code: String, path: String): TranslationUnitDeclaration {
        val tu: TranslationUnitDeclaration
        try {
            // run python function parse_code()
            tu = jep.interp.invoke("parse_code", this, code, path) as TranslationUnitDeclaration

            if (config.matchCommentsToNodes) {
                // Parse comments and attach to nodes
                jep.interp.invoke("parse_comments", this, code, path, tu)
            }
        } catch (e: JepException) {
            e.printStackTrace()
            throw TranslationException("Python failed with message: $e")
        } catch (e: Exception) {
            throw e
        }

        return tu
    }
}
