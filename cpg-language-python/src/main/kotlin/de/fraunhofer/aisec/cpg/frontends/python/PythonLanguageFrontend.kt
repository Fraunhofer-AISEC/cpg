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

import de.fraunhofer.aisec.cpg.ExperimentalPython
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.file.Path
import jep.JepException
import jep.SubInterpreter

@ExperimentalPython
class PythonLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, ".") {
    companion object {
        @kotlin.jvm.JvmField var PY_EXTENSIONS: List<String> = listOf(".py")
    }
    private val jep = JepSingleton // configure Jep

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        TypeManager.getInstance().setLanguageFrontend(this)
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
        // check, if the cpg.py is either directly available in the current directory or in the
        // src/main/python folder
        val modulePath = Path.of("cpg.py")

        val possibleLocations =
            listOf(
                Path.of(".").resolve(modulePath),
                Path.of("src/main/python").resolve(modulePath),
                Path.of("cpg-library/src/main/python").resolve(modulePath)
            )

        var found = false

        var entryScript: Path? = null
        possibleLocations.forEach {
            if (it.toFile().exists()) {
                found = true
                entryScript = it.toAbsolutePath()
            }
        }

        val tu: TranslationUnitDeclaration
        var interp: SubInterpreter? = null
        try {
            interp = SubInterpreter(jep.config)

            // load script
            if (found) {
                interp.runScript(entryScript.toString())
            } else {
                // fall back to the cpg.py in the class's resources
                val classLoader = javaClass
                val pyInitFile = classLoader.getResource("/cpg.py")
                interp.exec(pyInitFile?.readText())
            }

            // run python function parse_code()
            tu = interp.invoke("parse_code", code, path, this) as TranslationUnitDeclaration
        } catch (e: JepException) {
            e.printStackTrace()
            throw TranslationException("Python failed with message: $e")
        } catch (e: Exception) {
            throw e
        } finally {
            interp?.close()
        }

        return tu
    }
}
