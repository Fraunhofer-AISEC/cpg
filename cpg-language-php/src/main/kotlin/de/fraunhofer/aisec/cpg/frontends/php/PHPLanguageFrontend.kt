/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.php

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.SupportsNewParse
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * A relatively simple PHP frontend that extracts top-level function declarations and parameters
 * from PHP source code.
 */
class PHPLanguageFrontend(ctx: TranslationContext, language: PHPLanguage) :
    LanguageFrontend<String, String>(ctx, language), SupportsNewParse {

    override fun parse(file: File): TranslationUnit {
        return parse(file.readText(StandardCharsets.UTF_8), file.toPath())
    }

    override fun parse(content: String, path: Path?): TranslationUnit {
        val tu = newTranslationUnit(path?.toString() ?: "unknown.php", rawNode = content)
        scopeManager.resetToGlobal(tu)

        FUNCTION_REGEX.findAll(content).forEach { match ->
            val function = newFunction(match.groupValues[1], rawNode = match.value)
            scopeManager.enterScope(function)
            parseParameters(match.groupValues[2], match.value).forEach { parameter ->
                scopeManager.addDeclaration(parameter)
                function.parameters += parameter
            }
            function.body = newBlock(rawNode = match.value)
            scopeManager.leaveScope(function)

            scopeManager.addDeclaration(function)
            tu.declarations += function
        }

        return tu
    }

    private fun parseParameters(
        parameterList: String,
        rawNode: String,
    ): List<de.fraunhofer.aisec.cpg.graph.declarations.Parameter> {
        if (parameterList.isBlank()) {
            return emptyList()
        }

        return parameterList.split(",").mapNotNull { parameterDeclaration ->
            PARAMETER_REGEX.find(parameterDeclaration.trim())?.let { parameterMatch ->
                val isVariadic = parameterMatch.groupValues[1].isNotEmpty()
                val name = parameterMatch.groupValues[2]
                newParameter(name, variadic = isVariadic, rawNode = rawNode)
            }
        }
    }

    override fun codeOf(astNode: String): String? {
        return astNode
    }

    override fun locationOf(astNode: String): PhysicalLocation? {
        return null
    }

    override fun typeOf(type: String): Type {
        return autoType()
    }

    override fun setComment(node: Node, astNode: String) {
        // not implemented
    }

    companion object {
        private val FUNCTION_REGEX =
            Regex(
                """\bfunction\s+&?([A-Za-z_][A-Za-z0-9_]*)\s*\(([^)]*)\)\s*(?::\s*[A-Za-z_\\|?][A-Za-z0-9_\\|?]*)?"""
            )
        private val PARAMETER_REGEX = Regex("""(\.\.\.)?\$([A-Za-z_][A-Za-z0-9_]*)""")
    }
}
