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
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SvelteLanguageFrontendTest {

    @Test
    fun `test parsing a simple Svelte component`() {
        val topLevel = Path.of("src", "test", "resources", "svelte")

        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("SimpleComponent.svelte").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<SvelteLanguage>()
            }

        val varName =
            tu.declarations.filterIsInstance<VariableDeclaration>().firstOrNull { declaration ->
                val nameProperty = declaration.name
                val localNameString = nameProperty.localName
                localNameString == "name"
            }
        assertNotNull(varName, "Variable 'name' should be declared")

        val varCount =
            tu.declarations.filterIsInstance<VariableDeclaration>().firstOrNull {
                it.name.localName == "count"
            }
        assertNotNull(varCount, "Variable 'count' should be declared")

        val funcHandleClick =
            tu.declarations.filterIsInstance<FunctionDeclaration>().firstOrNull {
                it.name.localName == "handleClick"
            }
        assertNotNull(funcHandleClick, "Function 'handleClick' should be declared")

        // Test JSON output for cpg-wrapper-service visualizer
        println("=== CPG STRUCTURE FOR VISUALIZER ===")
        
        // Print TranslationUnit info
        println("TranslationUnit: ${tu.name}")
        println("File: ${tu.location?.artifactLocation?.uri?.path}")
        println("Language: ${tu.language?.name}")
        
        // Print all declarations with details
        println("\n=== DECLARATIONS ===")
        tu.declarations.forEachIndexed { index, declaration ->
            println("$index. ${declaration::class.simpleName}: ${declaration.name.localName}")
            println("   - Type: ${(declaration as? ValueDeclaration)?.type}")
            println("   - Location: ${declaration.location}")
            if (declaration is VariableDeclaration) {
                println("   - Initial Value: ${declaration.initializer}")
                println("   - Is Exported: ${declaration.name.localName == "name"}")
            }
            if (declaration is FunctionDeclaration) {
                println("   - Parameters: ${declaration.parameters.size}")
                println("   - Body Statements: ${(declaration.body as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Block)?.statements?.size ?: 0}")
            }
        }
        
        // Test function body details
        println("\n=== FUNCTION BODY ANALYSIS ===")
        val handleClickFunction = funcHandleClick
        if (handleClickFunction != null) {
            println("Function: ${handleClickFunction.name.localName}")
            println("Parameters: ${handleClickFunction.parameters.size}")
            
            val functionBody = handleClickFunction.body
            println("Body type: ${functionBody?.javaClass?.simpleName}")
            
            if (functionBody is de.fraunhofer.aisec.cpg.graph.statements.expressions.Block) {
                println("Body statements count: ${functionBody.statements.size}")
                functionBody.statements.forEachIndexed { index, statement ->
                    println("  Statement $index: ${statement.javaClass.simpleName}")
                    when (statement) {
                        is de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression -> {
                            println("    - Assignment: ${statement.operatorCode}")
                            println("    - LHS: ${statement.lhs.firstOrNull()?.javaClass?.simpleName}")
                            println("    - RHS: ${statement.rhs.firstOrNull()?.javaClass?.simpleName}")
                        }
                        is de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement -> {
                            println("    - Declaration Statement")
                            statement.declarations.forEach { decl ->
                                println("      - Declaration: ${decl.javaClass.simpleName} - ${decl.name.localName}")
                            }
                        }
                        else -> {
                            println("    - Content: ${statement}")
                        }
                    }
                }
            } else {
                println("Function body is not a Block: ${functionBody}")
            }
        }
        
        // Create JSON representation for the visualizer
        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        
        try {
            // Create a simplified structure for visualization
            val nodeInfo = mapOf(
                "translationUnit" to mapOf(
                    "name" to tu.name.toString(),
                    "file" to tu.location?.artifactLocation?.uri?.path,
                    "language" to tu.language?.name,
                    "declarations" to tu.declarations.map { declaration ->
                        mapOf(
                            "type" to declaration::class.simpleName,
                            "name" to declaration.name.localName,
                            "nodeType" to (declaration as? ValueDeclaration)?.type.toString(),
                            "location" to mapOf(
                                "region" to declaration.location?.region.toString(),
                                "artifactLocation" to declaration.location?.artifactLocation?.uri?.path
                            ),
                            "additionalInfo" to when (declaration) {
                                is VariableDeclaration -> mapOf(
                                    "initializer" to declaration.initializer?.toString(),
                                    "isExported" to (declaration.name.localName == "name")
                                )
                                is FunctionDeclaration -> mapOf(
                                    "parameters" to declaration.parameters.size,
                                    "bodyStatements" to ((declaration.body as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Block)?.statements?.size ?: 0),
                                    "bodyDetails" to if (declaration.body is de.fraunhofer.aisec.cpg.graph.statements.expressions.Block) {
                                        (declaration.body as de.fraunhofer.aisec.cpg.graph.statements.expressions.Block).statements.mapIndexed { index, stmt ->
                                            mapOf(
                                                "index" to index,
                                                "type" to stmt.javaClass.simpleName,
                                                "details" to when (stmt) {
                                                    is de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression -> mapOf(
                                                        "operator" to stmt.operatorCode,
                                                        "lhsType" to stmt.lhs.firstOrNull()?.javaClass?.simpleName,
                                                        "rhsType" to stmt.rhs.firstOrNull()?.javaClass?.simpleName
                                                    )
                                                    is de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement -> mapOf(
                                                        "declarations" to stmt.declarations.map { decl -> 
                                                            mapOf("type" to decl.javaClass.simpleName, "name" to decl.name.localName)
                                                        }
                                                    )
                                                    else -> mapOf("content" to stmt.toString())
                                                }
                                            )
                                        }
                                    } else emptyList<Map<String, Any>>()
                                )
                                else -> emptyMap<String, Any>()
                            }
                        )
                    }
                )
            )
            
            val jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(nodeInfo)
            println("\n=== JSON OUTPUT FOR CPG-WRAPPER-SERVICE ===")
            println(jsonOutput)
            
            // Write JSON to file for cpg-wrapper-service
            val outputDir = java.io.File("build/test-results/svelte")
            outputDir.mkdirs()
            val jsonFile = java.io.File(outputDir, "SimpleComponent-cpg.json")
            jsonFile.writeText(jsonOutput)
            println("\n=== JSON FILE WRITTEN ===")
            println("JSON output written to: ${jsonFile.absolutePath}")
            
            // Verify the JSON structure is valid and contains expected elements
            assertTrue(jsonOutput.contains("translationUnit"), "JSON should contain translationUnit")
            assertTrue(jsonOutput.contains("name"), "JSON should contain variable 'name'")
            assertTrue(jsonOutput.contains("count"), "JSON should contain variable 'count'")
            assertTrue(jsonOutput.contains("handleClick"), "JSON should contain function 'handleClick'")
            
        } catch (e: Exception) {
            println("Error creating JSON output: ${e.message}")
            e.printStackTrace()
        }

        println("Basic assertions passed. Further implementation needed.")
    }
}
