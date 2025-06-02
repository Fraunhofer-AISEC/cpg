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
import com.fasterxml.jackson.module.kotlin.readValue
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.frontends.typescript.TypeScriptLanguageFrontend.Companion.parserFile
// Core CPG
import de.fraunhofer.aisec.cpg.graph.* // For Node, Type, ScopeManager, ProblemNode, parseName, newFunctionDeclaration, newVariableDeclaration, newParameterDeclaration, newProblemDeclaration, newProblemExpression, newAssignExpression, newUnaryOperator, newReturnStatement etc.

// Specific CPG Declarations
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration

// Specific CPG Statements & Expressions
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator

// Explicit imports for builder functions that might be problematic with wildcard
import de.fraunhofer.aisec.cpg.graph.newBlock
import de.fraunhofer.aisec.cpg.graph.newDeclarationStatement

// CPG Types
import de.fraunhofer.aisec.cpg.graph.types.*

// SARIF
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
        // Handle HTML structure (program.html)
        handleHtmlTemplate(program.html, tu)
        // Handle CSS (program.css)
        program.css?.let { handleCssBlock(it, tu) }
    }

    private fun handleInstanceScript(scriptNode: SvelteScript, tu: TranslationUnitDeclaration) {
        for (statementNode in scriptNode.ast.body) {
            val beforeCount = tu.declarations.size
            val cpgNode = handleScriptStatement(statementNode)
            if (cpgNode is Declaration) {
                tu.addDeclaration(cpgNode)
                scopeManager.addDeclaration(cpgNode)
            }
            val afterCount = tu.declarations.size
            if (afterCount > beforeCount) {
                val newDecls = tu.declarations.subList(beforeCount, afterCount)
            }
        }
    }

    private fun handleModuleScript(scriptNode: SvelteScript, tu: TranslationUnitDeclaration) {
        for (statementNode in scriptNode.ast.body) {
            val beforeCount = tu.declarations.size
            val cpgNode = handleScriptStatement(statementNode)
            if (cpgNode is Declaration) {
                tu.addDeclaration(cpgNode)
                scopeManager.addDeclaration(cpgNode)
            }
            val afterCount = tu.declarations.size
            if (afterCount > beforeCount) {
                val newDecls = tu.declarations.subList(beforeCount, afterCount)
            }
        }
    }

    private fun handleHtmlTemplate(fragment: SvelteFragment, tu: TranslationUnitDeclaration) {
        // Process the HTML template structure
        log.info("Processing HTML template with {} children", fragment.children.size)
        
        for (child in fragment.children) {
            val htmlNode = handleSvelteNode(child, tu)
            // For now, we'll create a placeholder record to represent the HTML structure
            if (htmlNode != null) {
                // HTML elements don't become top-level declarations in typical CPG,
                // but we can create representations for analysis
                log.debug("Created HTML node: {} ({})", htmlNode.javaClass.simpleName, htmlNode)
            }
        }
    }

    private fun handleSvelteNode(node: SvelteNode, tu: TranslationUnitDeclaration): Node? {
        return when (node) {
            is SvelteElement -> {
                log.debug("Processing HTML element: {}", node.name)
                
                // Create a record declaration to represent the HTML element
                val elementName = node.name ?: "unknown_element"
                val htmlElement = newRecordDeclaration(
                    parseName(elementName),
                    "html_element", // kind
                    rawNode = node
                )
                
                // Process attributes (including event handlers)
                node.attributes?.forEach { attr ->
                    when (attr) {
                        is SvelteEventHandler -> {
                            log.debug("Processing event handler: {}", attr.name)
                            // Event handlers connect HTML to script functions
                            // Create a field to represent the event binding
                            val eventField = newFieldDeclaration(
                                parseName("on_${attr.name}"),
                                unknownType(),
                                listOf("event_handler"), // modifiers
                                rawNode = attr
                            )
                            
                            // If there's an expression, it references a function from script
                            attr.expression?.let { expr ->
                                val handlerRef = handleExpression(expr)
                                if (handlerRef != null) {
                                    log.debug("Event handler references: {}", handlerRef)
                                    // In a full implementation, we'd link this to the function
                                }
                            }
                            
                            htmlElement.addField(eventField)
                        }
                        is SvelteAttribute -> {
                            log.debug("Processing attribute: {}", attr.name)
                            // Regular HTML attributes
                            val attrField = newFieldDeclaration(
                                parseName(attr.name),
                                unknownType(),
                                listOf("html_attribute"),
                                rawNode = attr
                            )
                            htmlElement.addField(attrField)
                        }
                    }
                }
                
                // Process child elements recursively
                node.children?.forEach { child ->
                    val childNode = handleSvelteNode(child, tu)
                    // In a more complete implementation, we'd establish parent-child relationships
                }
                
                htmlElement
            }
            is SvelteText -> {
                log.debug("Processing text node: {}", node.data.take(50))
                // Text nodes could contain expressions, but for now we'll create a simple literal
                val textLiteral = newLiteral(node.data, language.getSimpleTypeOf("string") ?: unknownType(), rawNode = node)
                textLiteral
            }
            is SvelteMustacheTag -> {
                log.debug("Processing Svelte expression: {}", node.expression.javaClass.simpleName)
                // This is a Svelte expression like {name} or {count}
                val expression = handleExpression(node.expression)
                if (expression != null) {
                    log.debug("Svelte expression resolved to: {}", expression.javaClass.simpleName)
                    // In a full implementation, we'd track these expressions and their dependencies
                }
                expression
            }
            else -> {
                log.warn("Unhandled Svelte node type: {}", node.javaClass.simpleName)
                null
            }
        }
    }

    private fun handleScriptStatement(stmtNode: EsTreeNode): Node? {
        when (stmtNode) {
            is EsTreeVariableDeclaration -> {
                val cpgDeclarations = handleVariableDeclaration(stmtNode)
                // Callers (handleInstanceScript/handleModuleScript) will add these to TU and scopeManager
                // if the returned node is a Declaration.
                // We return the first one as the representative Node for this statement.
                return cpgDeclarations.firstOrNull()
            }
            is EsTreeFunctionDeclaration -> {
                val funcName = stmtNode.id?.name?.let { parseName(it) } ?: parseName("anonymous")
                val cpgFunction = newFunctionDeclaration(funcName, rawNode = stmtNode)
                // Callers will add cpgFunction to TU and scopeManager.

                scopeManager.enterScope(cpgFunction)

                // Handle parameters
                for (paramNode in stmtNode.params) {
                    if (paramNode is EsTreeIdentifier) { // Assuming params are EsTreeIdentifier for now
                        val paramName = parseName(paramNode.name)
                        // TODO: Infer type from type annotations if available in EsTreeIdentifier or sibling node
                        val paramType = unknownType()
                        val cpgParam = newParameterDeclaration(paramName.localName, paramType, false, rawNode = paramNode)
                        scopeManager.addDeclaration(cpgParam)
                    } else {
                        // Handle other parameter types (e.g., object patterns, array patterns) if necessary
                        val problem = newProblemDeclaration(
                            "Unhandled Svelte/ESTree parameter type: ${paramNode::class.simpleName}",
                            ProblemNode.ProblemType.PARSING, // Changed from PARSER to PARSING
                            rawNode = paramNode
                        )
                    }
                }

                // Handle body
                val functionBodyCompound = newBlock(rawNode = stmtNode.body)
                cpgFunction.body = functionBodyCompound

                for (bodyStmtNode in stmtNode.body.body) {
                    val cpgStatement = handleScriptStatement(bodyStmtNode)
                    if (cpgStatement is Statement) {
                        functionBodyCompound.statements += cpgStatement // Changed from addStatement to statements +=
                    }
                    // If it's a Declaration, it should have been returned by handleScriptStatement
                    // and added to the scope/TU by the top-level callers (handleInstanceScript/ModuleScript).
                    // Here, we are inside a function body, so declarations would typically be local variables.
                    // The current setup for handleScriptStatement might need adjustment if it returns Declarations
                    // that are meant to be local to a function body and not top-level in the TU.
                    // For now, we only add Statements to the CompoundStatement.
                    // Local variable declarations will be handled when handleScriptStatement(EsTreeVariableDeclaration) is called.
                    // Those will be returned as VariableDeclaration (Node), and the caller (handleInstanceScript/ModuleScript)
                    // will add them to the TU. This is not quite right for local variables.

                    // Let's refine: if handleScriptStatement returns a Declaration inside a function body,
                    // it should be added to the function's scope and potentially to the compound statement as a DeclaredStatement.
                    else if (cpgStatement is Declaration) {
                         // Add to scope, but also need to wrap it in a statement for the block
                         scopeManager.addDeclaration(cpgStatement) // Add to current function scope
                         val declStmt = newDeclarationStatement() // Remove rawNode parameter since cpgStatement doesn't have it
                         (declStmt as DeclarationStatement).addDeclaration(cpgStatement) // Rely on DeclarationStatement method
                         functionBodyCompound.statements += declStmt // Changed from addStatement to statements +=
                    }
                }

                scopeManager.leaveScope(cpgFunction)

                return cpgFunction
            }
            is EsTreeExportNamedDeclaration -> {
                stmtNode.declaration?.let { decl ->
                    when (decl) {
                        is EsTreeVariableDeclaration -> {
                            val cpgDeclarations = handleVariableDeclaration(decl)
                            // Callers will add these to TU and scopeManager.
                            // TODO: Mark these as exported? CPG has ExportDeclaration node type
                            return cpgDeclarations.firstOrNull()
                        }
                        is EsTreeFunctionDeclaration -> {
                            val funcName =
                                decl.id?.name?.let { parseName(it) } ?: parseName("anonymous")
                            val cpgFunction = newFunctionDeclaration(funcName, rawNode = decl)
                            // Callers will add cpgFunction to TU and scopeManager.
                            // TODO: Mark as exported?

                            scopeManager.enterScope(cpgFunction)

                            // Handle parameters (mirrors the EsTreeFunctionDeclaration case)
                            for (paramNode in decl.params) {
                                if (paramNode is EsTreeIdentifier) {
                                    val paramName = parseName(paramNode.name)
                                    val paramType = unknownType()
                                    val cpgParam = newParameterDeclaration(paramName.localName, paramType, false, rawNode = paramNode)
                                    scopeManager.addDeclaration(cpgParam)
                                } else {
                                    val problem = newProblemDeclaration(
                                        "Unhandled Svelte/ESTree parameter type: ${paramNode::class.simpleName}",
                                        ProblemNode.ProblemType.PARSING, // Changed from PARSER to PARSING
                                        rawNode = paramNode
                                    )
                                }
                            }

                            // Handle body (mirrors the EsTreeFunctionDeclaration case)
                            val functionBodyCompound = newBlock(rawNode = decl.body)
                            cpgFunction.body = functionBodyCompound

                            for (bodyStmtNode in decl.body.body) {
                                val cpgStatement = handleScriptStatement(bodyStmtNode)
                                if (cpgStatement is Statement) {
                                    functionBodyCompound.statements += cpgStatement // Changed from addStatement to statements +=
                                }
                                else if (cpgStatement is Declaration) {
                                     scopeManager.addDeclaration(cpgStatement)
                                     val declStmt = newDeclarationStatement() // Remove rawNode parameter
                                     (declStmt as DeclarationStatement).addDeclaration(cpgStatement)
                                     functionBodyCompound.statements += declStmt // Changed from addStatement to statements +=
                                }
                            }

                            scopeManager.leaveScope(cpgFunction)

                            return cpgFunction
                        }
                        else -> {
                            return null
                        }
                    }
                }
                // TODO: Handle stmtNode.specifiers for re-exports like export { name1, name2 }
                return null // Or handle specifiers and return something
            }
            is EsTreeExpressionStatement -> { // Added case for EsTreeExpressionStatement
                val expression = handleExpression(stmtNode.expression)
                if (expression != null) {
                    // Just return the expression directly since expressions inherit from statements
                    return expression
                } else {
                    // If expression is null, create a problem
                    val problem = newProblemDeclaration(
                        "Could not handle expression within ExpressionStatement: ${stmtNode.expression::class.simpleName}",
                        ProblemNode.ProblemType.PARSING, // Changed from PARSER to PARSING
                        rawNode = stmtNode.expression ?: stmtNode // Prefer expression raw node if available
                    )
                    return problem
                }
            }
            is EsTreeReturnStatement -> { // Added case for EsTreeReturnStatement
                val returnStmt = newReturnStatement(rawNode = stmtNode)
                stmtNode.argument?.let {
                    returnStmt.returnValue = handleExpression(it)
                }
                return returnStmt
            }
            else -> {
                val loc =
                    this.locationOf(stmtNode)
                // Create a ProblemNode for unhandled statements
                val problem = newProblemDeclaration(
                    "Unhandled Svelte/ESTree AST node type: ${stmtNode::class.simpleName}",
                    ProblemNode.ProblemType.PARSING, // Changed from PARSER to PARSING
                    rawNode = stmtNode
                )
                return problem
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
            is EsTreeAssignmentExpression -> {
                val newAssign = newAssignExpression(rawNode = exprNode)

                val lhs = handleExpression(exprNode.left)
                val rhs = handleExpression(exprNode.right)

                if (lhs != null && rhs != null) {
                    newAssign.lhs = mutableListOf(lhs)
                    newAssign.rhs = mutableListOf(rhs)
                    newAssign.operatorCode = exprNode.operator
                    return newAssign
                } else {
                    val problem = newProblemExpression(
                        "Could not handle LHS or RHS of AssignmentExpression",
                        ProblemNode.ProblemType.PARSING, // Changed from PARSER to PARSING
                        rawNode = exprNode
                    )
                    return problem
                }
            }
            is EsTreeUpdateExpression -> { 
                val unaryOp = newUnaryOperator(
                    exprNode.operator, // operatorCode
                    exprNode.prefix,   // isPrefix
                    !exprNode.prefix,  // isPostfix
                    rawNode = exprNode
                )
                val inputExpr = handleExpression(exprNode.argument)
                if (inputExpr != null) {
                    unaryOp.input = inputExpr
                    return unaryOp
                } else {
                    val problem = newProblemExpression(
                        "Could not handle argument of UpdateExpression",
                        ProblemNode.ProblemType.PARSING, // Changed from PARSER to PARSING
                        rawNode = exprNode
                    )
                    return problem
                }
            }
            else -> {
                val loc =
                    this.locationOf(exprNode)
                val problem = newProblemExpression(
                    "Unknown expression type: ${exprNode::class.simpleName}",
                     ProblemNode.ProblemType.PARSING, // Changed from PARSER to PARSING
                    rawNode = exprNode
                )
                return problem
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

    override fun codeOf(astNode: GenericAstNode): String? {
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

    override fun locationOf(astNode: GenericAstNode): PhysicalLocation? {
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

    override fun setComment(node: Node, astNode: GenericAstNode) {
    }

    private fun handleCssBlock(cssNode: SvelteStyleNode, tu: TranslationUnitDeclaration) {
        log.info("Processing CSS block with {} children", cssNode.children?.size ?: 0)
        
        // Create a record declaration to represent the CSS stylesheet
        val cssStylesheet = newRecordDeclaration(
            parseName("stylesheet"),
            "css_stylesheet",
            rawNode = cssNode
        )
        
        // Process CSS rules
        cssNode.children?.forEach { child ->
            when (child) {
                is SvelteRule -> {
                    log.debug("Processing CSS rule")
                    val cssRule = handleCssRule(child)
                    if (cssRule != null) {
                        cssStylesheet.addField(cssRule)
                    }
                }
                else -> {
                    log.debug("Unhandled CSS child type: {}", child.javaClass.simpleName)
                }
            }
        }
        
        // Add the stylesheet as a declaration to the translation unit
        tu.addDeclaration(cssStylesheet)
        log.debug("Added CSS stylesheet to translation unit")
    }

    private fun handleCssRule(rule: SvelteRule): FieldDeclaration? {
        log.debug("Processing CSS rule with selector list")
        
        // Process the selector (e.g., "h1")
        val selectorName = extractSelectorName(rule.prelude)
        log.debug("CSS rule targets selector: {}", selectorName)
        
        // Create a field to represent the CSS rule with the selector name
        val ruleField = newFieldDeclaration(
            parseName("rule_$selectorName"),
            unknownType(),
            listOf("css_rule"),
            rawNode = rule
        )
        
        // Process CSS declarations (properties and values)
        rule.block.children.forEach { declaration ->
            log.debug("Processing CSS declaration: {} = {}", declaration.property, declaration.value)
            
            // Create a literal for the CSS value
            val cssValue = newLiteral(
                declaration.value,
                language.getSimpleTypeOf("string") ?: unknownType(),
                rawNode = declaration
            )
            
            // For now, we'll combine all properties into a string representation
            // In a more complete implementation, each property could be a separate field
        }
        
        return ruleField
    }

    private fun extractSelectorName(selectorList: SvelteSelectorList): String {
        // Extract the main selector name from the selector list
        for (selector in selectorList.children) {
            for (selectorChild in selector.children) {
                if (selectorChild is SvelteTypeSelector) {
                    return selectorChild.name
                }
            }
        }
        return "unknown_selector"
    }
}

