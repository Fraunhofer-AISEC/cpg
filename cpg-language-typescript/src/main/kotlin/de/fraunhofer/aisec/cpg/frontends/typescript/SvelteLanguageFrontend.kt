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
import de.fraunhofer.aisec.cpg.frontends.typescript.TypeScriptLanguageFrontend.Companion.parserFile
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.io.InputStreamReader // For reading process output
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
    lateinit var currentFile: File
    lateinit var code: String

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SvelteLanguageFrontend::class.java)
    }

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        this.currentFile = file
        this.code = file.readText()

        val tud = newTranslationUnitDeclaration(file.absolutePath)
        scopeManager.resetToGlobal(tud)
        this.currentTU = tud
        this.currentFile = file

        // Always use the svelte language flag for this frontend
        val languageFlag = "--language=svelte"

        log.info("Executing Svelte parser for {} with flag: {}", file.absolutePath, languageFlag)
        val process =
            try {
                Runtime.getRuntime()
                    .exec(arrayOf(parserFile.absolutePath, languageFlag, file.absolutePath))
            } catch (e: Exception) {
                throw TranslationException("Error executing TypeScript parser: ${e.message}")
            }

        val stdInput = InputStreamReader(process.inputStream)
        val stdError = InputStreamReader(process.errorStream)
        val jsonResult = stdInput.readText()
        val errors = stdError.readText()
        stdInput.close()
        stdError.close()

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            log.error("TypeScript parser exited with code {}: {}", exitCode, errors)
            throw TranslationException("TypeScript parser failed: $errors")
        }
        if (errors.isNotEmpty()) {
            log.warn("TypeScript parser reported errors/warnings: {}", errors)
        }

        val svelteProgram = mapper.readValue<SvelteProgram>(jsonResult)
        handleSvelteProgram(svelteProgram, tud, file)

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
                    scopeManager.addDeclaration(cpgDecl)
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
            val name = parseName(declarator.id.name)
            val type = unknownType()
            val cpgVarDecl = newVariableDeclaration(name, type, rawNode = declarator)
            cpgVarDecl.isImplicit = false

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
        val cpgType = if (typeName != null) objectType(typeName) else unknownType()

        val literal = newLiteral(value, cpgType, rawNode = literalNode)
        return literal
    }

    private fun handleIdentifierReference(
        identifierNode: EsTreeIdentifier,
        currentFile: File,
    ): Reference {
        val name = parseName(identifierNode.name)
        val type = unknownType()
        val reference = newReference(name, type, rawNode = identifierNode)

        return reference
    }

    override fun typeOf(typeNode: SvelteNode): Type = unknownType()

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
            getRegionFromStartEnd(currentFile, code, startOffset, endOffset)
                ?: Region(-1, -1, -1, -1)
        return PhysicalLocation(this.currentFile.toURI(), region)
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
            getRegionFromStartEnd(currentFile, code, startOffset, endOffset)
                ?: Region(-1, -1, -1, -1)
        return PhysicalLocation(this.currentFile.toURI(), region)
    }

    private fun EsTreeNode.rawNode(): EsTreeNode = this

    override fun setComment(node: Node, astNode: SvelteNode) {
        // Not implemented for Svelte yet
    }
}
