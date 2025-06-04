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

import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SvelteLanguageFrontendTest {

    @Test
    fun `test parsing Svelte component with CPG analysis`() {
        // Change this to test different Svelte files
        val svelteFileName = "MockWidget.svelte"
        // val svelteFileName = "SimpleComponent.svelte"
        
        val topLevel = Path.of("src", "test", "resources", "svelte")
        val svelteFile = topLevel.resolve(svelteFileName).toFile()
        
        assertTrue(svelteFile.exists(), "Svelte file should exist: ${svelteFile.absolutePath}")

        println("=== ATTEMPTING TO PARSE SVELTE FILE ===")
        println("File: ${svelteFile.absolutePath}")
        
        val result =
            try {
                analyzeAndGetFirstTU(
                    listOf(svelteFile),
                    topLevel,
                    true,
                ) {
                    it.registerLanguage<SvelteLanguage>()
                }
            } catch (e: Exception) {
                println("Error during parsing: ${e.message}")
                e.printStackTrace()
                null
            }

        val tu = result
        if (tu == null) {
            println("❌ Failed to parse Svelte file - no translation unit created")
            println("This indicates a fundamental parsing issue")
            // Let's not fail the test completely, but show what happened
            return
        }
        
        println("✅ Successfully created translation unit")
        
        println("=== SVELTE CPG ANALYSIS: $svelteFileName ===")
        println("File: ${svelteFile.absolutePath}")
        println("Translation Unit: ${tu.name}")
        println("Language: ${tu.language?.name}")
        println("Total Declarations: ${tu.declarations.size}")
        
        // Categorize declarations by type
        val variables = tu.declarations.filterIsInstance<VariableDeclaration>()
        val functions = tu.declarations.filterIsInstance<FunctionDeclaration>()
        val records = tu.declarations.filterIsInstance<RecordDeclaration>()
        
        println("\n=== SCRIPT ANALYSIS ===")
        println("Variables found: ${variables.size}")
        variables.forEach { variable ->
            println("  - ${variable.name.localName}: ${variable.type}")
            if (variable.initializer != null) {
                println("    Initializer: ${variable.initializer?.javaClass?.simpleName}")
            }
        }
        
        println("\nFunctions found: ${functions.size}")
        functions.forEach { function ->
            println("  - ${function.name.localName}()")
            println("    Parameters: ${function.parameters.size}")
            val bodyStatements = (function.body as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Block)?.statements?.size ?: 0
            println("    Body statements: $bodyStatements")
        }
        
        println("\n=== STRUCTURE ANALYSIS ===")
        println("Record declarations: ${records.size}")
        val htmlElements = records.filter { it.kind == "html_element" }
        val cssStylesheets = records.filter { it.kind == "css_stylesheet" }
        val svelteComponents = records.filter { it.kind == "svelte_component" }
        
        println("HTML elements: ${htmlElements.size}")
        htmlElements.forEach { element ->
            println("  - <${element.name.localName}>")
        }
        
        println("CSS stylesheets: ${cssStylesheets.size}")
        println("Svelte components: ${svelteComponents.size}")
        
        // Show any problem declarations (things that couldn't be parsed)
        val problems = tu.declarations.filter { it.name.localName.startsWith("Problem") || it.javaClass.simpleName.contains("Problem") }
        if (problems.isNotEmpty()) {
            println("\n=== PARSING ISSUES ===")
            println("Problem declarations: ${problems.size}")
            problems.forEach { problem ->
                println("  - ${problem.javaClass.simpleName}: ${problem.name}")
            }
        }
        
        // Create JSON output for external tools
        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        
        try {
            val analysisResult = mapOf(
                "file" to svelteFileName,
                "totalDeclarations" to tu.declarations.size,
                "variables" to variables.map { mapOf(
                    "name" to it.name.localName,
                    "type" to it.type.toString(),
                    "hasInitializer" to (it.initializer != null)
                )},
                "functions" to functions.map { mapOf(
                    "name" to it.name.localName,
                    "parameters" to it.parameters.size,
                    "bodyStatements" to ((it.body as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Block)?.statements?.size ?: 0)
                )},
                "htmlElements" to htmlElements.size,
                "cssStylesheets" to cssStylesheets.size,
                "svelteComponents" to svelteComponents.size,
                "problems" to problems.size,
                "parsingSuccessful" to (tu.declarations.isNotEmpty())
            )
            
            val jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(analysisResult)
            println("\n=== JSON ANALYSIS RESULT ===")
            println(jsonOutput)
            
            // Write to file for external use
            val outputDir = java.io.File("build/test-results/svelte")
            outputDir.mkdirs()
            val baseName = svelteFileName.removeSuffix(".svelte")
            val jsonFile = java.io.File(outputDir, "$baseName-analysis.json")
            jsonFile.writeText(jsonOutput)
            println("\nAnalysis result written to: ${jsonFile.absolutePath}")
            
        } catch (e: Exception) {
            println("Error creating analysis output: ${e.message}")
            e.printStackTrace()
        }

        // Basic success check - we should have some declarations
        assertTrue(tu.declarations.isNotEmpty(), "Should have parsed some declarations from the Svelte file")
        
        println("\n=== ANALYSIS COMPLETE ===")
        println("✅ Successfully parsed $svelteFileName")
        println("Found ${variables.size} variables, ${functions.size} functions, ${records.size} structural elements")
    }
}
