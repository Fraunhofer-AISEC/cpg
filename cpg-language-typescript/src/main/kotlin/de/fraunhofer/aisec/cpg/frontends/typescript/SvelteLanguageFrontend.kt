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
import kotlin.jvm.Throws
import org.slf4j.LoggerFactory

/**
 * Language Frontend for analyzing Svelte files.
 *
 * Responsible for converting a Svelte AST (obtained by running the Deno parser script) into CPG
 * nodes.
 */
class SvelteLanguageFrontend(ctx: TranslationContext, language: Language<SvelteLanguageFrontend>) :
    LanguageFrontend<SvelteProgram, GenericAstNode>(ctx, language) {

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
        program.instance?.let { handleInstanceScript(it, tu) }
        program.module?.let { handleModuleScript(it, tu) }
        // TODO: Handle HTML structure (program.html)
        // TODO: Handle CSS (program.css)
    }

    private fun handleInstanceScript(scriptNode: SvelteScript, tu: TranslationUnitDeclaration) {
        LOGGER.debug(
            "Handling instance script. Start: {}, End: {}",
            scriptNode.start,
            scriptNode.end,
        )
        for (statementNode in scriptNode.ast.body) {
            val beforeCount = tu.declarations.size
            handleScriptStatement(statementNode, tu)
            val afterCount = tu.declarations.size
            if (afterCount > beforeCount) {
                val newDecls = tu.declarations.subList(beforeCount, afterCount)
                newDecls.forEach { decl ->
                    LOGGER.info(
                        "Added declaration to TU: {} (type: {})",
                        decl.name,
                        decl.javaClass.simpleName,
                    )
                }
            }
        }
    }

    private fun handleModuleScript(scriptNode: SvelteScript, tu: TranslationUnitDeclaration) {
        LOGGER.debug("Handling module script. Start: {}, End: {}", scriptNode.start, scriptNode.end)
        for (statementNode in scriptNode.ast.body) {
            val beforeCount = tu.declarations.size
            handleScriptStatement(statementNode, tu)
            val afterCount = tu.declarations.size
            if (afterCount > beforeCount) {
                val newDecls = tu.declarations.subList(beforeCount, afterCount)
                newDecls.forEach { decl ->
                    LOGGER.info(
                        "Added declaration to TU: {} (type: {})",
                        decl.name,
                        decl.javaClass.simpleName,
                    )
                }
            }
        }
    }

    private fun handleScriptStatement(stmtNode: EsTreeNode, parent: TranslationUnitDeclaration) {
        LOGGER.debug("Processing script statement node: {}", mapper.writeValueAsString(stmtNode))
        LOGGER.debug("Processing script statement of type: {}", stmtNode::class.simpleName)
        when (stmtNode) {
            is EsTreeVariableDeclaration -> {
                val cpgDeclarations = handleVariableDeclaration(stmtNode)
                for (cpgDecl in cpgDeclarations) {
                    parent.addDeclaration(cpgDecl)
                    scopeManager.addDeclaration(cpgDecl)
                }
            }
            is EsTreeFunctionDeclaration -> {
                LOGGER.debug("Handling EsTreeFunctionDeclaration: {}", stmtNode.id?.name)
                val funcName = stmtNode.id?.name?.let { parseName(it) } ?: parseName("anonymous")
                val cpgFunction = newFunctionDeclaration(funcName, rawNode = stmtNode)
                // TODO: Handle parameters, body, return type, etc.
                parent.addDeclaration(cpgFunction)
                scopeManager.addDeclaration(cpgFunction)
            }
            is EsTreeExportNamedDeclaration -> {
                LOGGER.debug("Handling EsTreeExportNamedDeclaration")
                stmtNode.declaration?.let { decl ->
                    when (decl) {
                        is EsTreeVariableDeclaration -> {
                            val cpgDeclarations = handleVariableDeclaration(decl)
                            for (cpgDecl in cpgDeclarations) {
                                parent.addDeclaration(cpgDecl)
                                scopeManager.addDeclaration(cpgDecl)
                                // TODO: Mark these as exported? CPG has ExportDeclaration node type
                            }
                        }
                        is EsTreeFunctionDeclaration -> {
                            val funcName =
                                decl.id?.name?.let { parseName(it) } ?: parseName("anonymous")
                            val cpgFunction = newFunctionDeclaration(funcName, rawNode = decl)
                            // TODO: Handle parameters, body, return type, etc.
                            parent.addDeclaration(cpgFunction)
                            scopeManager.addDeclaration(cpgFunction)
                            // TODO: Mark as exported?
                        }
                        else -> {
                            LOGGER.warn(
                                "Unsupported declaration type within ExportNamedDeclaration: {}",
                                decl::class.simpleName,
                            )
                        }
                    }
                }
                // TODO: Handle stmtNode.specifiers for re-exports like export { name1, name2 }
            }
            else -> {
                val loc =
                    this.locationOf(
                        stmtNode as GenericAstNode
                    ) // Explicit cast if needed by compiler, or direct call
                LOGGER.warn(
                    "Unsupported script statement type: {} at {}:{}. For Svelte, this frontend primarily focuses on script content. HTML and CSS are parsed but full CPG representation for them is a work in progress.",
                    stmtNode::class.simpleName,
                    loc?.region?.startLine,
                    loc?.region?.startColumn,
                )
            }
        }
    }

    private fun handleVariableDeclaration(
        varDeclNode: EsTreeVariableDeclaration
    ): List<de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration> {
        val cpgVariableDeclarations =
            mutableListOf<de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration>()
        LOGGER.debug(
            "Handling VariableDeclaration node: {}",
            mapper.writeValueAsString(varDeclNode),
        )

        for (declarator in varDeclNode.declarations) {
            LOGGER.debug("Handling VariableDeclarator: {}", mapper.writeValueAsString(declarator))
            val variableName =
                declarator.id.name?.let { parseName(it) } ?: parseName("anonymous_var")
            // TODO: Determine actual type based on initializer or type hints if available
            val variableType = unknownType()
            // Create the CPG node
            val cpgVariableDeclaration =
                newVariableDeclaration(
                    variableName,
                    variableType,
                    this.codeOf(declarator as GenericAstNode),
                    false
                )
            // Associate the raw AST node (declarator) with the CPG node.
            // This will set cpgVariableDeclaration.rawNode, .code, .location, and handle comments.
            this.process(cpgVariableDeclaration, declarator as GenericAstNode)

            LOGGER.warn("Processing declarator with ID: {}", declarator.id.name) // WARN level

            // Temporarily commenting out to diagnose Spotless issue // Now uncommenting
            declarator.init?.let {
                LOGGER.warn(
                    "Declarator has initializer (raw): {}",
                    mapper.writeValueAsString(it)
                )
                val initializerExpression = handleExpression(it)
                LOGGER.warn(
                    "Result of handleExpression for initializer: {} (Type: {})",
                    initializerExpression,
                    initializerExpression?.javaClass?.simpleName ?: "null"
                )

                if (initializerExpression != null) {
                    cpgVariableDeclaration.initializer = initializerExpression
                } else {
                    // Use ERROR level if initializer creation fails
                    /* LOGGER.error(
                        "Failed to create initializer expression for variable {}",
                        variableName
                    ) */
                }
            } else {
                LOGGER.warn("Declarator has no initializer.")
            }

            // WARN level log for the final CPG node before adding
            LOGGER.warn(
                "Created CPG VariableDeclaration: Name={}, Type={}, Initializer={}",
                cpgVariableDeclaration.name,
                cpgVariableDeclaration.type,
                cpgVariableDeclaration.initializer?.javaClass?.simpleName ?: "null",
            )

            cpgVariableDeclarations.add(cpgVariableDeclaration)
        }

        return cpgVariableDeclarations
    }

    private fun handleExpression(
        exprNode: EsTreeNode?
    ): de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression? {
        // Ensure exprNode is not null before processing
        if (exprNode == null) return null

        return when (exprNode) {
            is EsTreeLiteral -> handleLiteral(exprNode)
            is EsTreeIdentifier -> handleIdentifierReference(exprNode)
            // null case already handled above
            else -> {
                val loc =
                    this.locationOf(exprNode as GenericAstNode) // Explicit cast or direct call
                LOGGER.warn(
                    "Unsupported expression type: {} at {}:{}. Note: Full CPG representation for all Svelte HTML/CSS parts is a work in progress.",
                    exprNode::class.simpleName,
                    loc?.region?.startLine,
                    loc?.region?.startColumn,
                )
                null
            }
        }
    }

    private fun handleLiteral(literalNode: EsTreeLiteral): Literal<*> {
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

    private fun handleIdentifierReference(identifierNode: EsTreeIdentifier): Reference {
        val name = parseName(identifierNode.name)
        val type = unknownType()
        val reference = newReference(name, type, rawNode = identifierNode)

        return reference
    }

    // Corresponds to abstract fun typeOf(type: TypeNode): Type in LanguageFrontend
    override fun typeOf(type: GenericAstNode): Type {
        // TODO: Implement type resolution for GenericAstNode.
        // Could dispatch based on actual type: if (type is SvelteNode) vs if (type is EsTreeNode)
        LOGGER.debug("typeOf called for GenericAstNode: {}", type::class.simpleName)
        return unknownType()
    }

    // Not overriding from LanguageFrontend directly, but needed by CPG framework for RawNodeType.
    // LanguageFrontend.getCode(rawNode: RawNodeType) calls this if RawNodeType is not AstNodeType.
    fun codeOf(astNode: GenericAstNode): String? { // No 'override'
        val startOffset = astNode.start ?: return null
        val endOffset = astNode.end ?: return null

        if (this::code.isInitialized) {
            val codeText = this.code
            val codeLength = codeText.length
            if (startOffset >= 0 && endOffset >= startOffset && endOffset <= codeLength) {
                return codeText.substring(startOffset, endOffset)
            } else {
                LOGGER.warn(
                    "Invalid offsets for GenericAstNode in codeOf: start={}, end={}, codeLength={}",
                    startOffset,
                    endOffset,
                    codeLength,
                )
                return null
            }
        } else {
            LOGGER.warn("Attempted to get codeOf GenericAstNode before code was initialized.")
            return null
        }
    }

    // Not overriding from LanguageFrontend directly, but needed for RawNodeType.
    // LanguageFrontend.getLocation(rawNode: RawNodeType) calls this if RawNodeType is not
    // AstNodeType.
    fun locationOf(astNode: GenericAstNode): PhysicalLocation? { // No 'override'
        if (!this::currentFile.isInitialized || !this::code.isInitialized) {
            LOGGER.warn(
                "currentFile or code is not initialized for GenericAstNode. Skipping location."
            )
            return null
        }

        val startOffset = astNode.start ?: return null
        val endOffset = astNode.end ?: return null
        val currentCode = this.code

        val fileLength = currentCode.length
        if (startOffset < 0 || endOffset > fileLength || startOffset > endOffset) {
            LOGGER.warn(
                "Invalid offsets for GenericAstNode in locationOf: start={}, end={}, fileLength={}. Skipping location.",
                startOffset,
                endOffset,
                fileLength,
            )
            return null
        }
        val region = getRegionFromStartEnd(this.currentFile, currentCode, startOffset, endOffset)
        return PhysicalLocation(this.currentFile.toURI(), region ?: Region(-1, -1, -1, -1))
    }

    // This is the method for AstNodeType (SvelteProgram)
    // Corresponds to abstract override fun codeOf(astNode: AstNode): String?
    override fun codeOf(astNode: SvelteProgram): String? {
        return if (this::code.isInitialized) {
            this.code
        } else {
            LOGGER.warn("Attempted to get codeOf SvelteProgram before code was initialized.")
            null
        }
    }

    // This is the method for AstNodeType (SvelteProgram)
    // Corresponds to abstract override fun locationOf(astNode: AstNode): PhysicalLocation?
    override fun locationOf(astNode: SvelteProgram): PhysicalLocation? {
        if (!this::currentFile.isInitialized || !this::code.isInitialized) {
            LOGGER.warn(
                "currentFile or code is not initialized for SvelteProgram. Skipping location."
            )
            return null
        }
        val region =
            getRegionFromStartEnd(this.currentFile, this.code, 0, this.code.length.coerceAtLeast(0))
        return PhysicalLocation(this.currentFile.toURI(), region ?: Region(-1, -1, -1, -1))
    }

    // This is the method for AstNodeType (SvelteProgram)
    // Corresponds to abstract fun setComment(node: Node, astNode: AstNode)
    override fun setComment(node: Node, astNode: SvelteProgram) {
        LOGGER.debug(
            "setComment called for SvelteProgram (Node type: {})",
            node.javaClass.simpleName,
        )
        // Typically, no specific comments for the entire program TU itself.
    }

    // Note: A public `fun setComment(node: Node, astNode: GenericAstNode)` (no override) might be
    // needed
    // if the CPG framework attempts to call setComment with RawNodeType. The base LanguageFrontend
    // does not have an abstract or open setComment for TypeNode.
    // For now, we only provide the abstract override for SvelteProgram.
}
