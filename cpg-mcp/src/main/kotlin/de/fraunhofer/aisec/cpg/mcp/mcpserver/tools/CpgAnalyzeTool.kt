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
package de.fraunhofer.aisec.cpg.mcp.mcpserver.tools

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.graph.variables
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toNodeInfo
import de.fraunhofer.aisec.cpg.mcp.setupTranslationConfiguration
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

var globalAnalysisResult: TranslationResult? = null

val toolDescription =
    """
        Analyze source code using CPG (Code Property Graph).
        
        This tool parses source code and creates a comprehensive graph representation 
        containing all nodes, functions, variables, and call expressions.
        
        Example usage:
        - "Analyze this Python file: /path/to/file.py"
        - "Analyze this code: print('hello')"
        - "Parse this uploaded file content"
    """
        .trimIndent()

val inputSchema =
    Tool.Input(
        properties =
            buildJsonObject {
                putJsonObject("filePath") {
                    put("type", "string")
                    put("description", "Path to existing source code file")
                }
                putJsonObject("fileContent") {
                    put("type", "string")
                    put("description", "Uploaded file or source code")
                }
            },
        required = listOf(),
    )

fun Server.addCpgAnalyzeTool() {
    this.addTool(name = "cpg_analyze", description = toolDescription, inputSchema = inputSchema) {
        request ->
        try {
            val payload =
                Json.decodeFromString<CpgAnalyzePayload>(Json.encodeToString(request.arguments))

            val (file, fileName) =
                when {
                    payload.filePath != null -> {
                        val f =
                            File(
                                "/home/shala/repos/cpg/cpg-mcp/src/main/kotlin/de/fraunhofer/aisec/cpg/mcp/test_example.py"
                            )
                        if (!f.exists())
                            throw IllegalArgumentException("File not found: ${f.absolutePath}")
                        f to f.name
                    }

                    else ->
                        throw IllegalArgumentException(
                            "Must provide filePath, fileContent, or sourceCode"
                        )
                }

            val config =
                setupTranslationConfiguration(
                    topLevel = file,
                    files = listOf(file.absolutePath),
                    includePaths = emptyList(),
                )

            val analyzer = TranslationManager.builder().config(config).build()
            val result = analyzer.analyze().get()

            // Store globally for other tools
            globalAnalysisResult = result

            val allNodes = result.nodes
            val functions = result.functions
            val variables = result.variables
            val callExpressions = result.calls

            val nodeInfos = allNodes.map { node: Node -> node.toNodeInfo() }

            val analysisResult =
                CpgAnalysisResult(
                    fileName = fileName,
                    totalNodes = allNodes.size,
                    functions = functions.size,
                    variables = variables.size,
                    callExpressions = callExpressions.size,
                    nodes = nodeInfos,
                )

            val jsonResult = Json.encodeToString(analysisResult)

            CallToolResult(content = listOf(TextContent(jsonResult)))
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent("Error: ${e.message ?: e::class.simpleName}"))
            )
        }
    }
}