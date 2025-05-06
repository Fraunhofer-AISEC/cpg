/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.typescript

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException as CpgTranslationException
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.newTranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.io.InputStreamReader

/**
 * Language Frontend for analyzing Svelte files.
 *
 * Responsible for converting a Svelte AST (obtained by running the Deno parser script) into CPG
 * nodes.
 */
class SvelteLanguageFrontend(ctx: TranslationContext, language: SvelteLanguage = SvelteLanguage()) :
    LanguageFrontend<SvelteNode, SvelteNode>(ctx, language) {

    private val mapper = jacksonObjectMapper()
    private var currentFileContent: String? = null

    // Companion object to manage the parser binary extraction (similar to TS frontend)
    companion object {
        // Use the *same* parser binary as TypeScriptLanguageFrontend
        private val parserFile: File = TypeScriptLanguageFrontend.parserFile
    }

    @Throws(CpgTranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        currentFileContent = file.readText() // Store content for code/location mapping

        if (!parserFile.exists()) {
            // If the TS frontend didn't extract it, something is wrong
            throw CpgTranslationException(
                "Unified parser binary not found at ${parserFile.absolutePath} (expected TS frontend to extract)"
            )
        }

        // This frontend *only* handles Svelte
        val languageFlag = "--language=svelte"

        log.info(
            "Executing unified parser for Svelte file {} with flag: {}",
            file.absolutePath,
            languageFlag,
        )
        val process =
            try {
                Runtime.getRuntime()
                    .exec(arrayOf(parserFile.absolutePath, languageFlag, file.absolutePath))
            } catch (e: Exception) {
                throw CpgTranslationException(
                    "Error executing unified parser for Svelte: ${e.message}",
                    e,
                )
            }

        val stdInput = InputStreamReader(process.inputStream)
        val stdError = InputStreamReader(process.errorStream)
        val jsonResult = stdInput.readText()
        val errors = stdError.readText()
        stdInput.close()
        stdError.close()

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            log.error("Unified parser (Svelte) exited with code {}: {}", exitCode, errors)
            throw CpgTranslationException("Unified parser (Svelte) failed: $errors")
        }
        if (errors.isNotEmpty()) {
            log.warn("Unified parser (Svelte) reported errors/warnings: {}", errors)
        }

        // --- Start AST Deserialization and CPG Creation ---
        log.info("Handling Svelte AST JSON for {}", file.name)

        // TODO: Define SvelteNode data classes in SvelteAST.kt matching svelte.parse output
        // For now, we deserialize into a generic map or a placeholder root node
        // data class SvelteProgramNode(val type: String?, val children: List<Any>?) : SvelteNode //
        // Placeholder - move to SvelteAST.kt
        // val svelteAstRoot: SvelteProgramNode = try { ... } catch ...

        // Placeholder implementation:
        log.debug("Svelte parser JSON output: {}", jsonResult)
        // val svelteAstRoot: Map<String, Any> = mapper.readValue(jsonString) // Example
        // deserialization

        // Create the TranslationUnitDeclaration using the builder function
        val tud = newTranslationUnitDeclaration(file.name, currentFileContent ?: "")

        // TODO: Implement the actual CPG node creation logic here.
        // Iterate through svelteAstRoot (html, css, instance, module sections)
        // and create corresponding CPG nodes (RecordDeclaration, FunctionDeclaration, etc.)

        // TODO: Implement comment handling if needed and possible from Svelte AST

        log.info("Svelte CPG construction finished for {}", file.name)
        return tud
    }

    override fun typeOf(typeNode: SvelteNode): Type {
        // TODO: Implement type mapping based on Svelte AST node types
        return unknownType()
    }

    override fun codeOf(astNode: SvelteNode): String? {
        // TODO: Extract code snippet corresponding to the SvelteNode
        // Requires SvelteNode to have start/end properties from parser
        // Use currentFileContent?.substring(astNode.start, astNode.end)
        return null // Placeholder
    }

    override fun locationOf(astNode: SvelteNode): PhysicalLocation? {
        // TODO: Create PhysicalLocation based on SvelteNode start/end properties
        // Requires SvelteNode to have start/end and file info
        // Use FrontendUtils.parseLocation or similar
        return null // Placeholder
    }

    override fun setComment(node: Node, astNode: SvelteNode) {
        // TODO: Implement comment association if Svelte AST provides comment info
    }
}
