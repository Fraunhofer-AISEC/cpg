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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgLlmAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toNodeInfo
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun Server.addCpgLlmAnalyzeTool() {
    val toolDescription =
        """
        Generate a prompt asking the LLM to suggest concepts/operations
        
        This tool creates a prompt that asks the LLM to act as a security engineer and analyze 
        the CPG analysis results, suggesting appropriate security concepts and operations based 
        on the Fraunhofer CPG repository documentation. After using this tool,
        the LLM should analyze the prompt and provide JSON suggestions. The user should
        review these suggestions before applying them with cpg_apply_concepts.
        
        Example usage:
        - "Analyze this code for security vulnerabilities"
        - "Focus on authentication vulnerabilities in this code"  
        
        Parameters:
        - description: Additional context for the security analysis (optional)
    """
            .trimIndent()

    val inputSchema =
        Tool.Input(
            properties =
                buildJsonObject {
                    putJsonObject("description") {
                        put("type", "string")
                        put("description", "Additional context for the security analysis")
                    }
                },
            required = listOf(),
        )

    this.addTool(
        name = "cpg_llm_analyze",
        description = toolDescription,
        inputSchema = inputSchema,
    ) { request ->
        try {
            val payload =
                if (request.arguments.isEmpty()) {
                    CpgLlmAnalyzePayload()
                } else {
                    Json.decodeFromString<CpgLlmAnalyzePayload>(
                        Json.encodeToString(request.arguments)
                    )
                }

            val nodes =
                globalAnalysisResult?.allChildren<Node>()?.map { it.toNodeInfo() } ?: emptyList()

            val prompt = buildString {
                appendLine("# Code Analysis")
                appendLine()
                appendLine(
                    "Please take on the role of a security engineer and analyze the provided code."
                )
                appendLine()

                appendLine("## Understanding CPG Concepts and Operations")
                appendLine()
                appendLine(
                    "**Goal:** By marking security-relevant data (concepts) and critical operations, we can analyze how data flows through the code to discover vulnerabilities."
                )
                appendLine()
                appendLine("**Concepts** mark 'what something IS':")
                appendLine("- Applied to data-holding nodes (variables, parameters)")
                appendLine(
                    "- Examples: passwords, encryption keys, user input, authentication tokens"
                )
                appendLine("- Purpose: Track where important data goes")
                appendLine()
                appendLine("**Operations** mark 'what something DOES':")
                appendLine("- Applied to action nodes (function calls)")
                appendLine(
                    "- Examples: HTTP requests, file reads, database queries, authentication checks"
                )
                appendLine("- Purpose: Track what happens to important data")
                appendLine()
                appendLine("**Key principle:** Mark the variables, not assignments.")
                appendLine(
                    "**IMPORTANT**: Research and use only concepts and operations available in the CPG repository. Do not invent new names."
                )
                appendLine()

                appendLine("## Your Task")
                appendLine(
                    "1. Research available concepts/operations in the CPG repository (cpg-concepts module)"
                )
                appendLine("2. Analyze the nodes below from a security perspective")
                appendLine(
                    "3. Suggest the appropriate overlays from the CPG repository by providing their fully qualified class names"
                )
                appendLine("2. Analyze each node for security relevance")
                appendLine("3. Suggest appropriate overlays using fully qualified class names")
                appendLine()
                appendLine(
                    "**IMPORTANT:** Use only existing CPG concepts/operations. Do not invent new names."
                )
                appendLine()

                if (payload.description != null) {
                    appendLine("## Additional Context")
                    appendLine(payload.description)
                    appendLine()
                }

                if (nodes.isNotEmpty()) {
                    appendLine("## Nodes to Analyze")
                    nodes.forEach { node ->
                        val location =
                            if (node.fileName != "unknown" && node.startLine != 0) {
                                " at ${node.fileName}:${node.startLine}-${node.endLine}"
                            } else {
                                ""
                            }
                        appendLine("**Node ${node.nodeId}**: `${node.code}`$location")
                    }
                } else {
                    appendLine(
                        "No CPG analysis available. Please analyze a file first using cpg_analyze."
                    )
                }

                appendLine()
                appendLine("## Response Format")
                appendLine("Please provide your analysis in JSON format:")
                appendLine(
                    """
```json
{
  "overlaySuggestions": [
    {
      "nodeId": "123",
      "nodeName": "node"
      "overlay": "de.fraunhofer.aisec.cpg.graph.concepts.Data",
      "reasoning": "Detailed security reasoning for this classification",
      "securityImpact": "Potential security implications"
    }
  ],
}
```

**IMPORTANT**: After providing your analysis, WAIT for user approval before applying any concepts. Do not automatically execute cpg_apply_concepts.
                """
                        .trimIndent()
                )
            }

            CallToolResult(content = listOf(TextContent(prompt)))
        } catch (e: Exception) {
            CallToolResult(
                content =
                    listOf(
                        TextContent("Error generating prompt: ${e.message ?: e::class.simpleName}")
                    )
            )
        }
    }
}

abstract class Privacy(underlyingNode: Node? = null) : Concept(underlyingNode)

class Data(underlyingNode: Node? = null) : Privacy(underlyingNode)

abstract class PrivacyOperation(underlyingNode: Node? = null, concept: Privacy) :
    Operation(underlyingNode, concept)

class ReadData(underlyingNode: Node? = null, concept: Privacy) :
    PrivacyOperation(underlyingNode, concept)
