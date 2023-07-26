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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.newUnknownType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.file.Paths
import jep.JepException
import kotlin.io.path.absolutePathString

class PythonLanguageFrontend(language: Language<PythonLanguageFrontend>, ctx: TranslationContext) :
    LanguageFrontend<Any, Any>(language, ctx) {
    private val jep = JepSingleton // configure Jep

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        return parseInternal(file.readText(Charsets.UTF_8), file.path)
    }

    override fun typeOf(type: Any): Type {
        // will be invoked by native function
        return newUnknownType()
    }

    override fun codeOf(astNode: Any): String? {
        // will be invoked by native function
        return null
    }

    override fun locationOf(astNode: Any): PhysicalLocation? {
        // will be invoked by native function
        return null
    }

    override fun setComment(node: Node, astNode: Any) {
        // will be invoked by native function
    }

    private fun parseInternal(code: String, path: String): TranslationUnitDeclaration {
        val pythonInterpreter = jep.getInterp()
        val tu: TranslationUnitDeclaration
        val absolutePath = Paths.get(path).absolutePathString()
        try {
            // run python function parse_code()
            tu =
                pythonInterpreter.invoke("parse_code", this, code, absolutePath)
                    as TranslationUnitDeclaration

            if (config.matchCommentsToNodes) {
                // Parse comments and attach to nodes
                pythonInterpreter.invoke("parse_comments", this, code, absolutePath, tu)
            }
        } catch (e: JepException) {
            e.printStackTrace()
            throw TranslationException("Python failed with message: $e")
        } catch (e: Exception) {
            throw e
        } finally {
            pythonInterpreter.close()
        }

        return tu
    }
}
