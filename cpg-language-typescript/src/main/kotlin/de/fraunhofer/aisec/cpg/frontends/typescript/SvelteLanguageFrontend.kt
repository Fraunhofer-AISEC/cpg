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
                    "Error executing unified parser for Svelte: ${e.message}"
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

        // Deserialize the JSON result into our SvelteProgram data class
        val svelteAstRoot: SvelteProgram = try {
            mapper.readValue(jsonResult) // Use extension function for SvelteProgram
        } catch (e: Exception) {
            log.error("Error deserializing Svelte AST JSON: ${e.message}", e)
            throw CpgTranslationException("Failed to deserialize Svelte AST JSON from parser")
        }

        log.debug("Deserialized Svelte AST root successfully.")

        // Create the TranslationUnitDeclaration using the builder function
        val tud = newTranslationUnitDeclaration(file.name, currentFileContent ?: "")

        // TODO: Implement the actual CPG node creation logic here.
        // Start processing the deserialized svelteAstRoot
        handleSvelteProgram(svelteAstRoot, tud)

        // TODO: Implement comment handling if needed and possible from Svelte AST

        log.info("Svelte CPG construction finished for {}", file.name)
        return tud
    }

    // New function to handle the root SvelteProgram node
    private fun handleSvelteProgram(program: SvelteProgram, tud: TranslationUnitDeclaration) {
        log.debug("Processing SvelteProgram node.")
        // TODO: Create a top-level RecordDeclaration or NamespaceDeclaration for the component?
        // This depends on how Svelte components should be represented in the CPG.

        // Process the instance script (<script>) if it exists
        program.instance?.let { handleScript(it, tud) }

        // Process the module script (<script context="module">) if it exists
        program.module?.let { handleScript(it, tud, isModuleScript = true) }
        
        // Process the HTML template fragment if it exists
        program.html?.let { handleFragment(it, tud) }

        // Process the CSS style block if it exists
        program.css?.let { handleStyle(it, tud) }
    }

    // Placeholder handler functions - to be implemented
    private fun handleScript(script: SvelteScript, parent: Node, isModuleScript: Boolean = false) {
        log.warn("handleScript not fully implemented yet for script starting at {}", script.start)
        // TODO: Parse script.content (e.g., using TypeScriptLanguageFrontend internally or another JS parser?)
        // Create FunctionDeclarations, VariableDeclarations etc. and add them to the parent (TUD or Component Record)
    }

    private fun handleFragment(fragment: SvelteFragment, parent: Node) {
        log.warn("handleFragment not fully implemented yet for fragment starting at {}", fragment.start)
        // TODO: Iterate through fragment.children
        // Call handlers for SvelteElement, SvelteText, SvelteMustacheTag etc.
        // Create corresponding CPG nodes (CallExpression for components, EOG edges, etc.)
        fragment.children?.forEach { handleNode(it, parent) }
    }

    private fun handleStyle(style: SvelteStyle, parent: Node) {
        log.warn("handleStyle not fully implemented yet for style starting at {}", style.start)
        // TODO: Optionally parse style.content.styles if CSS analysis is needed
        // Potentially create Comment nodes or specific CSS nodes.
    }

    // Generic handler dispatcher (can be expanded)
    private fun handleNode(node: SvelteNode, parent: Node) {
        when (node) {
            is SvelteElement -> handleElement(node, parent)
            is SvelteText -> handleText(node, parent)
            is SvelteMustacheTag -> handleMustacheTag(node, parent)
            // Add cases for other SvelteNode types (IfBlock, EachBlock, etc.)
            else -> log.warn("No handler implemented for SvelteNode type: {}", node.type)
        }
    }

    private fun handleElement(element: SvelteElement, parent: Node) {
        log.warn("handleElement not fully implemented yet for <{}> at {}", element.name, element.start)
        // TODO: Create CPG nodes for element (e.g., CallExpression if it's a component? Literal<String> for HTML tag?)
        // TODO: Process attributes (handleAttributeLike)
        // TODO: Recursively process children (handleNode)
        element.attributes.forEach { handleAttributeLike(it, /* CPG node for element */ parent) }
        element.children?.forEach { handleNode(it, /* CPG node for element */ parent) }
    }

    private fun handleText(text: SvelteText, parent: Node) {
        log.warn("handleText not fully implemented yet for text '{}' at {}", text.text.take(20), text.start)
        // TODO: Create Literal<String> CPG node?
    }

    private fun handleMustacheTag(tag: SvelteMustacheTag, parent: Node) {
        log.warn("handleMustacheTag not fully implemented yet at {}", tag.start)
        // TODO: Process tag.expression (handleExpression)
        // TODO: Create CPG nodes representing the expression output/binding
    }

    private fun handleAttributeLike(attr: SvelteAttributeLike, parent: Node) {
        when (attr) {
            is SvelteAttribute -> handleAttribute(attr, parent)
            is SvelteEventHandler -> handleEventHandler(attr, parent)
            // Add cases for other SvelteAttributeLike types (Binding, ClassList, etc.)
            else -> log.warn("No handler implemented for SvelteAttributeLike type: {}", attr.type)
        }
    }

    private fun handleAttribute(attribute: SvelteAttribute, parent: Node) {
        log.warn("handleAttribute not fully implemented yet for '{}' at {}", attribute.name, attribute.start)
        // TODO: Create CPG nodes for attribute/value pair
    }

    private fun handleEventHandler(handler: SvelteEventHandler, parent: Node) {
        log.warn("handleEventHandler not fully implemented yet for '{}' at {}", handler.name, handler.start)
        // TODO: Process handler.expression (handleExpression)
        // TODO: Create CPG nodes (e.g., connect to handler function)
    }

    // TODO: Add handleExpression methods for SvelteExpression subtypes

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
