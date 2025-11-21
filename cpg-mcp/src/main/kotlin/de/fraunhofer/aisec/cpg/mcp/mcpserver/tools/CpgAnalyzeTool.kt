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
import de.fraunhofer.aisec.cpg.mcp.mcpserver.cpgDescription
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toNodeInfo
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toObject
import de.fraunhofer.aisec.cpg.mcp.setupTranslationConfiguration
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

var globalAnalysisResult: TranslationResult? = null

val toolDescription =
    """
        Analyze source code using CPG (Code Property Graph).
        
        $cpgDescription
        
        This tool parses source code and creates a comprehensive graph representation 
        containing all nodes, functions, variables, and call expressions.
        
        Example usage:
        - "Analyze this code: print('hello')"
        - "Analyze this uploaded file"
    """
        .trimIndent()

val inputSchema =
    ToolSchema(
        properties =
            buildJsonObject {
                putJsonObject("content") {
                    put("type", "string")
                    put("description", "Source code content to analyze")
                }
                putJsonObject("extension") {
                    put("type", "string")
                    put(
                        "description",
                        "File extension for language detection (e.g., 'py', 'java', 'cpp')",
                    )
                }
            },
        required = listOf(),
    )

fun Server.addCpgAnalyzeTool() {
    this.addTool(name = "cpg_analyze", description = toolDescription, inputSchema = inputSchema) {
        request ->
        try {
            val payload = request.arguments?.toObject<CpgAnalyzePayload>()
            val analysisResult = runCpgAnalyze(payload)
            val jsonResult = Json.encodeToString(analysisResult)
            CallToolResult(content = listOf(TextContent(jsonResult)))
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent("Error: ${e.message ?: e::class.simpleName}"))
            )
        }
    }
}

fun runCpgAnalyze(payload: CpgAnalyzePayload?): CpgAnalysisResult {
    val file =
        when {
            payload?.content != null -> {
                val extension =
                    if (payload.extension != null) {
                        if (payload.extension.startsWith(".")) payload.extension
                        else ".${payload.extension}"
                    } else {
                        throw IllegalArgumentException(
                            "Extension is required when providing content"
                        )
                    }

                val tempFile = File.createTempFile("cpg_analysis", extension)
                tempFile.writeText(payload.content)
                tempFile.deleteOnExit()
                tempFile
            }

            else -> throw IllegalArgumentException("Must provide content")
        }

    val config =
        setupTranslationConfiguration(
            topLevel = file,
            files = listOf(file.absolutePath),
            includePaths = emptyList(),
        )

    val analyzer = TranslationManager.builder().config(config).build()
    val result = analyzer.analyze().get()

    // Store the result globally
    globalAnalysisResult = result

    val allNodes = result.nodes
    val functions = result.functions
    val variables = result.variables
    val callExpressions = result.calls

    val nodeInfos = allNodes.map { node: Node -> node.toNodeInfo() }

    return CpgAnalysisResult(
        totalNodes = allNodes.size,
        functions = functions.size,
        variables = variables.size,
        callExpressions = callExpressions.size,
        nodes = nodeInfos,
    )
}
