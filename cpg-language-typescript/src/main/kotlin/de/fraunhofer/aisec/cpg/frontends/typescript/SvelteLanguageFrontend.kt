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

// import de.fraunhofer.aisec.cpg.passes.CallResolver // Currently unused, consider re-adding if
// call resolution for Svelte is implemented
// import de.fraunhofer.aisec.cpg.helpers.executeCommandWithOutputAfterTimeout // Removing this
// problematic import
// import java.nio.file.Files // No longer needed for Files.createTempDirectory with this change
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.io.InputStreamReader // For reading process output
import kotlin.text.isBlank
import org.slf4j.LoggerFactory

/**
 * Language Frontend for analyzing Svelte files.
 *
 * Responsible for converting a Svelte AST (obtained by running the Deno parser script) into CPG
 * nodes.
 */
class SvelteLanguageFrontend(ctx: TranslationContext, language: Language<SvelteLanguageFrontend>) :
    LanguageFrontend<SvelteNode, SvelteNode>(ctx, language) {

    internal val mapper = jacksonObjectMapper()

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SvelteLanguageFrontend::class.java)
    }

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        TypeManager.getInstance().setLanguageFrontend(this)

        val tud = ctx.newTranslationUnitDeclaration(file.absolutePath, file.readText())
        ctx.scopeManager.resetToGlobal(tud)
        this.currentTU = tud
        this.currentFile = file
        this.code = tud.code

        val parserFile = TypeScriptLanguageFrontend.parserFile
        if (parserFile == null || !parserFile.exists()) {
            LOGGER.error("Svelte parser script not found at expected location.")
            throw TranslationException(
                "Svelte parser script not found. TypeScript Deno setup might be incomplete."
            )
        }

        LOGGER.info("Invoking Svelte Deno parser for file: {}", file.absolutePath)
        LOGGER.debug("Parser script path: {}", parserFile.absolutePath)

        val commandArray =
            arrayOf(
                "deno",
                "run",
                "--allow-read",
                "--allow-env",
                parserFile.absolutePath,
                "--file",
                file.absolutePath,
                "--language",
                "svelte",
            )

        try {
            val process = Runtime.getRuntime().exec(commandArray)
            val stdInput = InputStreamReader(process.inputStream)
            val stdError = InputStreamReader(process.errorStream)

            val output = stdInput.readText()
            val errorOutput = stdError.readText()

            stdInput.close()
            stdError.close()

            val returnCode = process.waitFor() // Wait for the process to complete

            if (returnCode != 0) {
                LOGGER.error("Svelte Deno parser failed with exit code $returnCode.")
                LOGGER.error("Parser output: $output")
                LOGGER.error("Parser error: $errorOutput")
                throw TranslationException("Svelte Deno parser execution failed for ${file.name}")
            }

            if (output == null || output.isBlank()) {
                LOGGER.warn("Svelte Deno parser returned empty output for ${file.name}.")
                return tud
            }

            val svelteProgram = mapper.readValue<SvelteProgram>(output)
            handleSvelteProgram(svelteProgram, tud, file)
        } catch (e: Exception) {
            LOGGER.error("Error parsing Svelte file ${file.name}: ${e.message}", e)
            throw TranslationException("Error processing Svelte file ${file.name}: ${e.message}")
        }

        return tud
    }

    private fun handleSvelteProgram(
        program: SvelteProgram,
        tu: TranslationUnitDeclaration,
        currentFile: File,
    ) {
        program.instance?.let { handleInstanceScript(it, tu, currentFile) }
        program.module?.let { handleModuleScript(it, tu, currentFile) }
        // TODO: Handle HTML structure (program.html)
        // TODO: Handle CSS (program.css)
    }

    private fun handleInstanceScript(
        scriptNode: SvelteScript,
        tu: TranslationUnitDeclaration,
        currentFile: File,
    ) {
        LOGGER.debug(
            "Handling instance script. Start: {}, End: {}",
            scriptNode.start,
            scriptNode.end,
        )
        for (statementNode in scriptNode.ast.body) {
            handleScriptStatement(statementNode, tu, currentFile)
        }
    }

    private fun handleModuleScript(
        scriptNode: SvelteScript,
        tu: TranslationUnitDeclaration,
        currentFile: File,
    ) {
        LOGGER.debug("Handling module script. Start: {}, End: {}", scriptNode.start, scriptNode.end)
        for (statementNode in scriptNode.ast.body) {
            handleScriptStatement(statementNode, tu, currentFile)
        }
    }

    private fun handleScriptStatement(
        stmtNode: EsTreeNode,
        parent: TranslationUnitDeclaration,
        currentFile: File,
    ) {
        when (stmtNode) {
            is EsTreeVariableDeclaration -> {
                val cpgDeclarations = handleVariableDeclaration(stmtNode, currentFile)
                for (cpgDecl in cpgDeclarations) {
                    parent.addDeclaration(cpgDecl)
                    ctx.scopeManager.addDeclaration(cpgDecl)
                }
            }
            else ->
                LOGGER.warn(
                    "Unsupported script statement type: {} at {}:{}",
                    stmtNode::class.simpleName,
                    locationOfEsTreeNode(stmtNode)?.region?.startLine,
                    locationOfEsTreeNode(stmtNode)?.region?.startColumn,
                )
        }
    }

    private fun handleVariableDeclaration(
        varDeclNode: EsTreeVariableDeclaration,
        currentFile: File,
    ): List<de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration> {
        val cpgVariableDeclarations =
            mutableListOf<de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration>()

        for (declarator in varDeclNode.declarations) {
            val name = ctx.nameOf(declarator.id.name)
            val type = ctx.unknownType()
            val cpgVarDecl = ctx.newVariableDeclaration(name, type)
            cpgVarDecl.isImplicit = false
            val astNodeForRaw: EsTreeNode = declarator.id.rawNode()
            cpgVarDecl.rawNode = astNodeForRaw

            declarator.init?.let { initExprNode ->
                val initializer = handleExpression(initExprNode, currentFile)
                if (initializer != null) {
                    cpgVarDecl.initializer = initializer
                    cpgVarDecl.type = initializer.type
                }
            }
            cpgVariableDeclarations.add(cpgVarDecl)
        }
        return cpgVariableDeclarations
    }

    private fun handleExpression(
        exprNode: EsTreeNode?,
        currentFile: File,
    ): de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression? {
        return when (exprNode) {
            is EsTreeLiteral -> handleLiteral(exprNode, currentFile)
            is EsTreeIdentifier -> handleIdentifierReference(exprNode, currentFile)
            null -> null
            else -> {
                LOGGER.warn(
                    "Unsupported expression type: {} at {}:{}",
                    exprNode::class.simpleName,
                    locationOfEsTreeNode(exprNode)?.region?.startLine,
                    locationOfEsTreeNode(exprNode)?.region?.startColumn,
                )
                null
            }
        }
    }

    private fun handleLiteral(literalNode: EsTreeLiteral, currentFile: File): Literal<*> {
        val value = literalNode.value
        val typeName =
            when (value) {
                is String -> "string"
                is Boolean -> "boolean"
                is Int,
                is Long,
                is Short,
                is Byte -> value?.javaClass?.simpleName ?: "int"
                is Double,
                is Float -> value?.javaClass?.simpleName ?: "double"
                else -> null
            }
        val cpgType =
            if (typeName != null) TypeParser.createFrom(typeName, language) else ctx.unknownType()

        val literal = ctx.newLiteral(value, cpgType)
        val astNodeForRawLiteral: EsTreeNode = literalNode.rawNode()
        literal.rawNode = astNodeForRawLiteral
        return literal
    }

    private fun handleIdentifierReference(
        identifierNode: EsTreeIdentifier,
        currentFile: File,
    ): Reference {
        val name = ctx.nameOf(identifierNode.name)
        val type = ctx.unknownType()
        val reference = ctx.newReference(name, type)
        val astNodeForRawRef: EsTreeNode = identifierNode.rawNode()
        reference.rawNode = astNodeForRawRef
        return reference
    }

    override fun typeOf(typeNode: SvelteNode): Type = ctx.unknownType()

    override fun codeOf(astNode: SvelteNode): String? {
        if (astNode.start != null && astNode.end != null && this.code != null) {
            val start = astNode.start!!
            val end = astNode.end!!
            val codeLength = this.code!!.length
            if (start >= 0 && end >= start && end <= codeLength) {
                return this.code!!.substring(start, end)
            }
        }
        return null
    }

    override fun locationOf(astNode: SvelteNode): PhysicalLocation? {
        if (this.currentFile == null || this.code == null) {
            LOGGER.warn("currentFile or code is null for SvelteNode. Skipping location.")
            return null
        }

        val startOffset = astNode.start
        val endOffset = astNode.end

        if (startOffset == null || endOffset == null) return null

        val fileLength = this.code!!.length
        if (startOffset < 0 || endOffset > fileLength || startOffset > endOffset) {
            LOGGER.warn(
                "Invalid offsets for SvelteNode: start={}, end={}, fileLength={}. Skipping location.",
                startOffset,
                endOffset,
                fileLength,
            )
            return null
        }

        val region =
            Region(
                getLineOfCode(startOffset),
                getColumnOfCode(startOffset),
                getLineOfCode(endOffset - 1),
                getColumnOfCode(endOffset - 1),
            )
        return PhysicalLocation(this.currentFile!!.toURI(), region)
    }

    private fun locationOfEsTreeNode(astNode: EsTreeNode): PhysicalLocation? {
        if (this.currentFile == null || this.code == null) {
            LOGGER.warn("currentFile or code is null for EsTreeNode. Skipping location.")
            return null
        }

        val startOffset = astNode.start
        val endOffset = astNode.end

        if (startOffset == null || endOffset == null) return null

        val fileLength = this.code!!.length
        if (startOffset < 0 || endOffset > fileLength || startOffset > endOffset) {
            LOGGER.warn(
                "Invalid offsets for EsTreeNode: start={}, end={}, fileLength={}. Skipping location.",
                startOffset,
                endOffset,
                fileLength,
            )
            return null
        }

        val region =
            Region(
                getLineOfCode(startOffset),
                getColumnOfCode(startOffset),
                getLineOfCode(endOffset - 1),
                getColumnOfCode(endOffset - 1),
            )
        return PhysicalLocation(this.currentFile!!.toURI(), region)
    }

    private fun EsTreeNode.rawNode(): EsTreeNode = this

    override fun setComment(node: Node, astNode: SvelteNode) {
        // Not implemented for Svelte yet
    }
}
