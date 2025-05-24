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
    LanguageFrontend<GenericAstNode, GenericAstNode>(ctx, language) {

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

        // Log all declarations for debugging
        LOGGER.info("Declarations in TU after Svelte parse:")
        tud.declarations.forEach { decl ->
            LOGGER.info("  - {} ({})", decl.name, decl.javaClass.simpleName)
        }

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
        for (statementNode in scriptNode.ast.body) {
            val beforeCount = tu.declarations.size
            handleScriptStatement(statementNode, tu)
            val afterCount = tu.declarations.size
            if (afterCount > beforeCount) {
                val newDecls = tu.declarations.subList(beforeCount, afterCount)
            }
        }
    }

    private fun handleModuleScript(scriptNode: SvelteScript, tu: TranslationUnitDeclaration) {
        for (statementNode in scriptNode.ast.body) {
            val beforeCount = tu.declarations.size
            handleScriptStatement(statementNode, tu)
            val afterCount = tu.declarations.size
            if (afterCount > beforeCount) {
                val newDecls = tu.declarations.subList(beforeCount, afterCount)
            }
        }
    }

    private fun handleScriptStatement(stmtNode: EsTreeNode, parent: TranslationUnitDeclaration) {
        when (stmtNode) {
            is EsTreeVariableDeclaration -> {
                val cpgDeclarations = handleVariableDeclaration(stmtNode)
                for (cpgDecl in cpgDeclarations) {
                    parent.addDeclaration(cpgDecl)
                    scopeManager.addDeclaration(cpgDecl)
                }
            }
            is EsTreeFunctionDeclaration -> {
                val funcName = stmtNode.id?.name?.let { parseName(it) } ?: parseName("anonymous")
                val cpgFunction = newFunctionDeclaration(funcName, rawNode = stmtNode)
                // TODO: Handle parameters, body, return type, etc.
                parent.addDeclaration(cpgFunction)
                scopeManager.addDeclaration(cpgFunction)
            }
            is EsTreeExportNamedDeclaration -> {
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
                        }
                    }
                }
                // TODO: Handle stmtNode.specifiers for re-exports like export { name1, name2 }
            }
            else -> {
                val loc =
                    this.locationOf(stmtNode)
            }
        }
    }

    private fun handleVariableDeclaration(
        varDeclNode: EsTreeVariableDeclaration
    ): List<de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration> {
        val cpgVariableDeclarations =
            mutableListOf<de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration>()

        for (declarator in varDeclNode.declarations) {
            val variableName =
                declarator.id.name?.let { parseName(it) } ?: parseName("anonymous_var")
            val variableType = unknownType()
            val cpgVariableDeclaration =
                newVariableDeclaration(
                    variableName,
                    variableType,
                    false, // implicitInitializerAllowed
                    declarator // rawNode
                )
            cpgVariableDeclaration.code = this.codeOf(declarator as GenericAstNode)
            cpgVariableDeclaration.location = this.locationOf(declarator as GenericAstNode)
            this.process(declarator, cpgVariableDeclaration)

            declarator.init?.let {
                val initializerExpression = handleExpression(it)
                if (initializerExpression != null) {
                    cpgVariableDeclaration.initializer = initializerExpression
                }
            }
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
            else -> {
                val loc =
                    this.locationOf(exprNode)
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
        return unknownType()
    }

    // Explicitly override codeOf and locationOf for RawNodeType (GenericAstNode)
    // to ensure these are called by the CPG framework instead of trying to cast
    // GenericAstNode to SvelteProgram (our AstNodeType).

    // This is now the primary override for AstNode (which is GenericAstNode)
    override fun codeOf(astNode: GenericAstNode): String? {
        // Delegate to our existing helper that handles GenericAstNode
        // Specific handling for SvelteProgram if needed
        if (astNode is SvelteProgram) {
            return if (this::code.isInitialized) {
                this.code
            } else {
                return null
            }
        }

        val startOffset = astNode.start ?: return null
        val endOffset = astNode.end ?: return null

        if (this::code.isInitialized) {
            val codeText = this.code
            val codeLength = codeText.length
            if (startOffset >= 0 && endOffset >= startOffset && endOffset <= codeLength) {
                return codeText.substring(startOffset, endOffset)
            } else {
                return null
            }
        } else {
            return null
        }
    }

    // This is now the primary override for AstNode (which is GenericAstNode)
    override fun locationOf(astNode: GenericAstNode): PhysicalLocation? {
        // Delegate to our existing helper that handles GenericAstNode
        // Specific handling for SvelteProgram if needed
        if (astNode is SvelteProgram) {
            if (!this::currentFile.isInitialized || !this::code.isInitialized) {
                return null
            }
            val region =
                getRegionFromStartEnd(this.currentFile, this.code, 0, this.code.length.coerceAtLeast(0))
            return PhysicalLocation(this.currentFile.toURI(), region ?: Region(-1, -1, -1, -1))
        }

        if (!this::currentFile.isInitialized || !this::code.isInitialized) {
            return null
        }

        val startOffset = astNode.start ?: return null
        val endOffset = astNode.end ?: return null
        val currentCode = this.code

        val fileLength = currentCode.length
        if (startOffset < 0 || endOffset > fileLength || startOffset > endOffset) {
            return null
        }
        val region = getRegionFromStartEnd(this.currentFile, currentCode, startOffset, endOffset)
        return PhysicalLocation(this.currentFile.toURI(), region ?: Region(-1, -1, -1, -1))
    }

    // This is the method for AstNodeType (SvelteProgram)
    // Corresponds to abstract override fun codeOf(astNode: AstNode): String?
    // NO LONGER NEEDED AS SEPARATE OVERRIDE - Handled by `override fun codeOf(astNode: GenericAstNode)`
    /*
    override fun codeOf(astNode: SvelteProgram): String? {
        if (astNode !is SvelteProgram) {
            LOGGER.error("codeOf(SvelteProgram) called with wrong type: {}", astNode::class.qualifiedName)
            return null
        }
        return if (this::code.isInitialized) {
            this.code
        } else {
            LOGGER.warn("Attempted to get codeOf SvelteProgram before code was initialized.")
            null
        }
    }
    */

    // This is the method for AstNodeType (SvelteProgram)
    // Corresponds to abstract override fun locationOf(astNode: AstNode): PhysicalLocation?
    // NO LONGER NEEDED AS SEPARATE OVERRIDE - Handled by `override fun locationOf(astNode: GenericAstNode)`
    /*
    override fun locationOf(astNode: SvelteProgram): PhysicalLocation? {
        if (astNode !is SvelteProgram) {
            LOGGER.error("locationOf(SvelteProgram) called with wrong type: {}", astNode::class.qualifiedName)
            return null
        }
        if (!this::currentFile.isInitialized || !this::code.isInitialized) {
            LOGGER.warn("currentFile or code is not initialized for SvelteProgram. Skipping location.")
            return null
        }
        val region =
            getRegionFromStartEnd(this.currentFile, this.code, 0, this.code.length.coerceAtLeast(0))
        return PhysicalLocation(this.currentFile.toURI(), region ?: Region(-1, -1, -1, -1))
    }
    */

    // This is the method for AstNodeType (SvelteProgram)
    // Corresponds to abstract fun setComment(node: Node, astNode: AstNode)
    // Signature changes to match new AstNode type (GenericAstNode)
    override fun setComment(node: Node, astNode: GenericAstNode) {
        // Specific handling for SvelteProgram if astNode is one
        if (astNode is SvelteProgram) {
        } else {
            // Generic handling for other GenericAstNodes if needed
        }
    }

    // Note: A public `fun setComment(node: Node, astNode: GenericAstNode)` (no override) might be
    // needed
    // if the CPG framework attempts to call setComment with RawNodeType. The base LanguageFrontend
    // does not have an abstract or open setComment for TypeNode.
    // For now, we only provide the abstract override for SvelteProgram.
}

