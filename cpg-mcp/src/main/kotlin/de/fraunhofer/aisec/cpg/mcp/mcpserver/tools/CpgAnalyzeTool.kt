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
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toNodeInfo
import de.fraunhofer.aisec.cpg.mcp.setupTranslationConfiguration
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

@Serializable data class CpgAnalyzePayload(val file: String)

var globalAnalysisResult: TranslationResult? = null

fun Server.addCpgAnalyzeTool() {
    val toolDescription =
        """
        Analyze source code files using CPG (Code Property Graph).
        
        This tool parses source code and creates a comprehensive graph representation 
        containing all nodes, functions, variables, and call expressions.
        
        Example usage:
        - "Analyze this Python script for security issues"
        - "Parse this code and show me the structure"
        - "Load this C++ file and analyze its functions"
        
        Parameters:
        - file: Path to the source code file to analyze (required)
    """
            .trimIndent()

    val inputSchema =
        Tool.Input(
            properties =
                buildJsonObject {
                    putJsonObject("file") {
                        put("type", "string")
                        put("description", "Path to source code file to analyze")
                    }
                },
            required = listOf("file"),
        )

    this.addTool(name = "cpg_analyze", description = toolDescription, inputSchema = inputSchema) {
        request ->
        try {
            val payload =
                Json.decodeFromString<CpgAnalyzePayload>(Json.encodeToString(request.arguments))

            val file = File(payload.file)

            if (!file.exists()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent("Error: File not found: ${file.absolutePath}"))
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
            val functions = result.allChildren<FunctionDeclaration>()
            val variables = result.allChildren<VariableDeclaration>()
            val callExpressions = result.allChildren<CallExpression>()

            val nodeInfos = allNodes.map { node: Node -> node.toNodeInfo() }

            val analysisResult =
                CpgAnalysisResult(
                    fileName = file.name,
                    totalNodes = allNodes.size,
                    functions = functions.size,
                    variables = variables.size,
                    callExpressions = callExpressions.size,
                    nodes = nodeInfos,
                )

            val jsonResult = Json.encodeToString(analysisResult)

            CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "Analysis completed for ${file.name}:\n" +
                                "${allNodes.size} nodes, ${functions.size} functions, " +
                                "${variables.size} variables, ${callExpressions.size} calls\n\n" +
                                jsonResult
                        )
                    )
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent("Error: ${e.message ?: e::class.simpleName}"))
            )
        }
    }
}

@Serializable
data class NodeInfo(
    val nodeId: String,
    val name: String,
    val code: String?,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
    val startColumn: Int?,
    val endColumn: Int?,
)

@Serializable
data class CpgAnalysisResult(
    val fileName: String,
    val totalNodes: Int,
    val functions: Int,
    val variables: Int,
    val callExpressions: Int,
    val nodes: List<NodeInfo>,
)
