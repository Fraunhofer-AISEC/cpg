package de.fraunhofer.aisec.cpg.frontends.svelte

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope // Import RecordScope
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.Throws

class SvelteLanguageFrontend(
    language: SvelteLanguage,
    config: TranslationConfiguration,
    scopeManager: ScopeManager = ScopeManager(),
) : LanguageFrontend(language, config, scopeManager) {

    private val parserScriptResourcePath = "/parser.js" // Path within resources
    private val nodeExecutable = "node" // Assumes node is in PATH
    // Configure ObjectMapper for polymorphism and to ignore unknown properties
    private val mapper: ObjectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .addMixIn(SvelteNode::class.java, SvelteNodeMixin::class.java)

    private var currentFileContent: String = "" // Store content for getCodeFromRawNode

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        // Store file content for location mapping later
        currentFileContent = file.readText()

        val tempParserScript = extractParserScript()
        // Create the TUD now, passing the file content
        val tud = newTranslationUnitDeclaration(file.name, currentFileContent)
        tud.language = this.language // Set language if not done by newTranslationUnitDeclaration

        scopeManager.resetToGlobal(tud)

        try {
            val processBuilder = ProcessBuilder(
                nodeExecutable,
                tempParserScript.absolutePath,
                file.absolutePath
            )

            log.info("Executing Svelte parser: {} {} {}", nodeExecutable, tempParserScript.absolutePath, file.absolutePath)

            val process = processBuilder.start()
            val astJson = process.inputStream.bufferedReader().readText()
            val errors = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                log.error("Svelte parser script failed with exit code {}. Error output:
{}", exitCode, errors)
                try {
                   val errorMap = mapper.readValue(errors, Map::class.java)
                   val errorMsg = errorMap["message"] as? String ?: "Unknown error"
                   val errorPosMap = errorMap["position"] as? Map<*, *>
                   val line = (errorPosMap?.get("line") as? Number)?.toInt()
                   val col = (errorPosMap?.get("column") as? Number)?.toInt()
                   val region = if(line != null && col != null) Region(line, col + 1, line, col + 1) else Region() // SARIF is 1-based
                   val physLoc = PhysicalLocation(file.toURI(), region)
                   // Create a ProblemDeclaration in the TUD
                   val problem = newProblemDeclaration("Svelte parser failed: $errorMsg", "Parser Error", physLoc)
                   tud.addDeclaration(problem)

                   // Still throw exception to signal failure, but TUD contains the problem
                   throw TranslationException("Svelte parser failed: $errorMsg")
                } catch (e: Exception) {
                   throw TranslationException("Svelte parser failed with exit code $exitCode. Raw error: $errors")
                }
            }

            if (astJson.isBlank()) {
                 throw TranslationException("Svelte parser returned empty AST.")
            }

            log.debug("Received Svelte AST JSON for {}:
{}", file.name, astJson) // Log AST for debugging

            // Deserialize astJson into our Svelte AST data structure
            val svelteAstRoot: Root = try {
                mapper.readValue(astJson, Root::class.java)
            } catch (e: Exception) {
                log.error("Failed to deserialize Svelte AST JSON for {}", file.name, e)
                throw TranslationException("Failed to deserialize Svelte AST JSON", e)
            }

            log.info("Successfully deserialized Svelte AST for {}", file.name)

            // Start CPG conversion from the deserialized AST root
            handleRoot(tud, svelteAstRoot)

            return tud
        } catch (e: Exception) {
            log.error("Error processing Svelte file {}", file.name, e)
            // Ensure exception is wrapped in TranslationException if not already
            if (e is TranslationException) throw e else throw TranslationException(e)
        } finally {
            // Clean up the temporary script file
            Files.deleteIfExists(tempParserScript.toPath())
            currentFileContent = "" // Clear content after processing
        }
    }

    /**
     * Handles the root of the Svelte AST. Creates the component's RecordDeclaration
     * and processes its parts (script, module, fragment, style).
     */
    private fun handleRoot(tud: TranslationUnitDeclaration, ast: Root) {
        // Create a RecordDeclaration representing the Svelte component itself
        val componentName = tud.name.substringBeforeLast('.') // Simple name from filename
        val record = newRecordDeclaration(componentName, "class", ast) // Or "struct", kind might need adjustment
        record.location = tud.location // Component represents the whole file
        scopeManager.enterScope(record) // Enter component scope

        // Handle <script context="module">...</script>
        ast.module?.let { handleScript(it, record, isModuleScript = true) }

        // Handle <script>...</script> (instance script)
        ast.instance?.let { handleScript(it, record, isModuleScript = false) }

        // Handle <style>...</style>
        ast.css?.let { handleStyle(it, record) }

        // Handle the template fragment
        handleFragment(ast.fragment, record)

        // Add the component RecordDeclaration to the TUD
        scopeManager.leaveScope(record)
        tud.addDeclaration(record)
    }

    /**
     * Handles <script> tags (instance or module).
     * For now, creates a placeholder method/function or adds declarations to scope.
     */
    private fun handleScript(ast: Script, parent: RecordDeclaration, isModuleScript: Boolean) {
        val scriptLocation = getLocationFromRawNode(ast)
        val scriptCode = getCodeFromRawNode(ast) // Get the actual script code

        log.info("Processing {} script block at {}", if(isModuleScript) "module" else "instance", scriptLocation)
        log.debug("Script content:
{}", scriptCode)

        if (isModuleScript) {
            // Module script content conceptually belongs to the 'static' part of the component.
            // We could try to parse its declarations and add them directly to the record's scope.
            // Or create a static initializer block/method.
            // TODO: Implement module script parsing/handling (potentially reusing TS/JS frontend)
            val moduleInit = newMethodDeclaration("<module-init>", scriptCode ?: "", true, parent)
            moduleInit.location = scriptLocation
            scopeManager.addDeclaration(moduleInit)
            // For now, add a comment/problem indicating it needs real parsing
             parent.addComment("Module script content needs parsing: ${scriptCode?.take(100)}...")

        } else {
            // Instance script content runs when the component is instantiated.
            // It often contains variable declarations, lifecycle functions, etc.
            // This could be represented as a constructor or an instance initializer method.
            // TODO: Implement instance script parsing/handling (potentially reusing TS/JS frontend)
            val instanceInit = newMethodDeclaration("<instance-init>", scriptCode ?: "", false, parent)
            instanceInit.location = scriptLocation
            scopeManager.addDeclaration(instanceInit)
             // For now, add a comment/problem indicating it needs real parsing
             parent.addComment("Instance script content needs parsing: ${scriptCode?.take(100)}...")
        }
        // Note: A more robust approach would involve invoking the TS/JS frontend here
        // on the `ast.content`, providing the correct scope context. This is complex.
    }

    /**
     * Handles the <style> tag.
     * For now, maybe just create a comment or placeholder node.
     */
    private fun handleStyle(ast: Style, parent: RecordDeclaration) {
        val styleLocation = getLocationFromRawNode(ast)
        val styleCode = ast.content.styles
        log.info("Found style block at {}", styleLocation)
        log.debug("Style content:
{}", styleCode.take(200)) // Log snippet

        // TODO: Implement style block handling (potentially creating a comment or placeholder)
        // Could potentially involve a CSS parser in the future.
        parent.addComment("Style block content: ${styleCode.take(100)}...")
    }

    /**
     * Handles the main template fragment and its children.
     */
    private fun handleFragment(ast: Fragment, parent: Node) {
         log.debug("Processing fragment with {} children.", ast.children.size)
         scopeManager.enterScope(parent) // Assuming fragment children are in parent's scope initially

         for(childNode in ast.children) {
             // TODO: Call specific handlers based on childNode.type
             when(childNode) {
                 is Text -> handleText(childNode, parent)
                 is ExpressionTag -> handleExpressionTag(childNode, parent)
                 is Comment -> handleComment(childNode, parent)
                 is Element -> handleElement(childNode, parent)
                 // Add cases for other valid children (Blocks, etc.)
                 else -> {
                     log.warn("Unsupported Svelte AST node type encountered in fragment: {}", childNode.type)
                     val problem = newProblemDeclaration(
                         "Unsupported node type: ${childNode.type}",
                         "AST Conversion",
                         getLocationFromRawNode(childNode)
                     )
                     scopeManager.addDeclaration(problem) // Add problem to current scope
                 }
             }
         }
         scopeManager.leaveScope(parent)
    }

     /** Handles Text nodes - currently creates a comment */
    private fun handleText(ast: Text, parent: Node) {
        log.debug("Handling Text node: '{}'", ast.raw.trim().take(50))
        // For now, just add a comment to the parent node
        val commentText = "Template text: ${ast.raw.trim().take(100)}"
        if(parent is Declaration) parent.addComment(commentText)
        // Alternatively create a Literal<String> if appropriate CPG representation exists
    }

     /** Handles ExpressionTag nodes - currently creates a comment */
    private fun handleExpressionTag(ast: ExpressionTag, parent: Node) {
        log.debug("Handling ExpressionTag node: type {}", ast.expression.type)
        // TODO: Parse/Handle the inner ast.expression (JS/TS)
        val commentText = "Template expression needs parsing: type=${ast.expression.type}"
         if(parent is Declaration) parent.addComment(commentText)
        // Will need to invoke JS/TS parser on the expression's code snippet
    }

     /** Handles Comment nodes - currently creates a comment */
    private fun handleComment(ast: Comment, parent: Node) {
        log.debug("Handling Comment node: '{}'", ast.data.trim().take(50))
        val commentText = "Template comment: ${ast.data.trim().take(100)}"
        if(parent is Declaration) parent.addComment(commentText)
    }

    /** Handles Element nodes - currently creates a comment and processes children/attributes */
    private fun handleElement(ast: Element, parent: Node) {
        log.debug("Handling Element node: <{}>", ast.name)
        val elementLocation = getLocationFromRawNode(ast)

        // TODO: Create a meaningful CPG representation for HTML elements.
        // This could be a specific node type (e.g., HTMLElementDeclaration)
        // or potentially represented via calls (e.g., document.createElement)
        // or annotations/comments depending on the analysis goal.
        // For now, just add a comment to the parent.
        val commentText = "Template Element: <${ast.name}>"
        if(parent is Declaration) parent.addComment(commentText)

        // Create a placeholder node to act as the scope/parent for children and attributes
        // This is temporary until a proper CPG node is chosen.
        val elementPlaceholderNode = newCompoundStatement(getCodeFromRawNode(ast))
        elementPlaceholderNode.location = elementLocation
        // Add placeholder to parent if possible (e.g., if parent is a CompoundStatement)
        // if (parent is CompoundStatement) parent.addStatement(elementPlaceholderNode)
        // Otherwise, maybe add to the closest method/record scope? Needs thought.
        scopeManager.addStatement(elementPlaceholderNode) // Add to current scope for now

        // Process attributes
        scopeManager.enterScope(elementPlaceholderNode) // Enter scope for attributes/children
        for(attribute in ast.attributes) {
            handleAttribute(attribute, elementPlaceholderNode)
        }

        // Process child nodes recursively (similar to handleFragment)
        for(childNode in ast.children) {
             when(childNode) {
                 is Text -> handleText(childNode, elementPlaceholderNode)
                 is ExpressionTag -> handleExpressionTag(childNode, elementPlaceholderNode)
                 is Comment -> handleComment(childNode, elementPlaceholderNode)
                 is Element -> handleElement(childNode, elementPlaceholderNode) // Handle nested elements
                 // Add cases for other valid children (Blocks, etc.)
                 else -> {
                     log.warn("Unsupported Svelte AST node type encountered in element <{}>: {}", ast.name, childNode.type)
                     val problem = newProblemDeclaration(
                         "Unsupported node type in <${ast.name}>: ${childNode.type}",
                         "AST Conversion",
                         getLocationFromRawNode(childNode)
                     )
                      scopeManager.addDeclaration(problem)
                 }
             }
         }
         scopeManager.leaveScope(elementPlaceholderNode)
    }

    /** Handles Attribute nodes - currently creates a comment */
    private fun handleAttribute(ast: Attribute, parent: Node) {
        log.debug("Handling Attribute node: {}={...}", ast.name)
        val attributeLocation = getLocationFromRawNode(ast)
        val attributeCode = getCodeFromRawNode(ast)

        // TODO: Create CPG representation for attributes/directives.
        // Simple attributes might be literals or key-value pairs.
        // Directives (on:click, bind:value) need special handling (CallExpressions, Refs, etc.)
        val commentText = "Element Attribute: ${attributeCode}"
        if(parent is Declaration) parent.addComment(commentText)
        else if (parent is CompoundStatement) { // Add comment to placeholder scope
             val commentNode = newComment(commentText)
             commentNode.location = attributeLocation
             parent.addStatement(commentNode)
        }
        
        // Process the value of the attribute (which can be complex)
        scopeManager.enterScope(parent) // Attribute values likely in parent scope
        for(valueNode in ast.value) {
             when(valueNode) {
                 is Text -> handleText(valueNode, parent) // Simple string value
                 is ExpressionTag -> handleExpressionTag(valueNode, parent) // Dynamic value
                 // Handle other potential value types if necessary
                 else -> {
                      log.warn("Unsupported Svelte AST node type in attribute '{}' value: {}", ast.name, valueNode.type)
                 }
             }
        }
        scopeManager.leaveScope(parent)
    }

    /**
     * Extracts the parser.js script from resources to a temporary file.
     */
    private fun extractParserScript(): File {
        val resourceStream: InputStream? = 
            SvelteLanguageFrontend::class.java.getResourceAsStream(parserScriptResourcePath)
        
        if (resourceStream == null) {
             throw TranslationException("Could not find parser script in resources: $parserScriptResourcePath")
        }

        val tempFile = Files.createTempFile("svelte-parser-", ".js").toFile()
        tempFile.deleteOnExit() // Ensure cleanup on JVM exit

        resourceStream.use { input ->
            Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        
        log.debug("Extracted parser script to {}", tempFile.absolutePath)
        return tempFile
    }

    /**
     * Gets the source code snippet corresponding to a Svelte AST node.
     */
    override fun <T> getCodeFromRawNode(astNode: T): String? {
        if (astNode is SvelteNode) {
            // Check bounds to prevent IndexOutOfBoundsException
            if (astNode.start >= 0 && astNode.end <= currentFileContent.length && astNode.start <= astNode.end) {
                return currentFileContent.substring(astNode.start, astNode.end)
            } else {
                log.warn("Invalid start/end indices for node type {}: start={}, end={}, contentLength={}",
                    astNode.type, astNode.start, astNode.end, currentFileContent.length)
            }
        } else if (astNode is Map<*, *>) {
            // Fallback if we receive a raw map (less type-safe)
            val start = (astNode["start"] as? Number)?.toInt()
            val end = (astNode["end"] as? Number)?.toInt()
             if (start != null && end != null && start >= 0 && end <= currentFileContent.length && start <= end) {
                return currentFileContent.substring(start, end)
            }
        }
        return null // Or super.getCodeFromRawNode(astNode) if applicable
    }

    /**
     * Gets the PhysicalLocation (file, line, column) for a Svelte AST node.
     */
    override fun <T> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        if (astNode is SvelteNode) {
            return this.locationCache.computeIfAbsent(astNode) { // Use cache
                val region = this.getRegionFromStartEnd(astNode.start, astNode.end)
                // Assuming currentTUD holds the current translation unit context
                PhysicalLocation(this.currentTU.name.toUri(), region)
            }
        } else if (astNode is Map<*, *>) {
             // Fallback for raw map
             val start = (astNode["start"] as? Number)?.toInt()
             val end = (astNode["end"] as? Number)?.toInt()
             if (start != null && end != null) {
                 val region = this.getRegionFromStartEnd(start, end)
                 return PhysicalLocation(this.currentTU.name.toUri(), region)
             }
        }
        return null // Or super.getLocationFromRawNode(astNode)
    }

    override fun <S, T> setComment(s: S, ctx: T) {
        // TODO: Implement if Svelte AST provides comments attached to nodes
        // Svelte AST has Comment nodes, handled in handleComment for now.
        // If comments are attached directly to other nodes, handle here.
        if(s is Node && ctx is SvelteNode) {
             // Check if ctx has associated comments and add them to s
        }
    }
} 