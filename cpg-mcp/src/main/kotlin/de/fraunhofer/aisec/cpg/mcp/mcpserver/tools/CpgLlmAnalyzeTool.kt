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
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toNodeInfo
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

@Serializable data class CpgLlmAnalyzePayload(val description: String? = null)

fun Server.addCpgLlmAnalyzeTool() {
    val toolDescription =
        """
        Generate a security analysis prompt asking the LLM to suggest concepts/operations for code review.
        
        This tool creates a prompt that asks the LLM to act as a security officer and analyze 
        the CPG analysis results, suggesting appropriate security concepts and operations based 
        on the Fraunhofer CPG repository documentation.
        
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
                appendLine("# Security Code Analysis Request")
                appendLine()
                appendLine(
                    "Please take on the role of a security officer and analyze the provided code for security-relevant patterns " +
                        "and potential vulnerabilities."
                )
                appendLine(
                    "For example, when we see code like `open('/etc/passwd', 'r')`, this represents accessing " +
                        "sensitive system files and should be tagged with a 'Data' concept because it handles " +
                        "sensitive information. Similarly, `requests.post()` calls represent HTTP requests that " +
                        "could send data outside the system boundary and should be tagged as 'HttpRequest' operations."
                )
                appendLine()
                appendLine("## Your Task")
                appendLine("As a security officer, please:")
                appendLine(
                    "1. Research currently available concepts and operations in the GitHub Fraunhofer CPG repository, especially in the module cpg-concepts"
                )
                appendLine(
                    "2. Review the CPG documentation to understand the different concept types and their purposes"
                )
                appendLine("3. Analyze the nodes below from a security perspective")
                appendLine(
                    "4. Please suggest appropriate security concepts and operations from the CPG repository " +
                        "that should be applied to each relevant node"
                )
                appendLine()
                appendLine("Focus on identifying nodes that handle:")
                appendLine("- Sensitive data access (files, environment variables, databases)")
                appendLine("- Network communication (HTTP requests, API calls)")
                appendLine("- Authentication and authorization mechanisms")
                appendLine("- Input validation and sanitization")
                appendLine("- Cryptographic operations")
                appendLine("- System resource access")
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
                        appendLine("**Node ${node.name + (node.nodeId)}**: `${node.code}`$location")
                    }
                } else {
                    appendLine(
                        "No CPG analysis available. Please analyze a file first using cpg_analyze."
                    )
                }

                appendLine()
                appendLine("## Response Format")
                appendLine(
                    "Please provide your security analysis and concept suggestions in JSON format:"
                )
                appendLine(
                    """
```json
{
  "analysis_summary": "Brief overview of security findings",
  "concept_suggestions": [
    {
      "nodeId": "123",
      "nodeName": "node"
      "conceptType": "Data|ReadData|Authentication|HttpRequest|etc",
      "reasoning": "Detailed security reasoning for this classification",
      "security_impact": "Potential security implications"
    }
  ],
  "additional_recommendations": "Any additional security recommendations"
}
```

Please research the CPG concept documentation first, then provide your analysis.
                """
                        .trimIndent()
                )
            }

            CallToolResult(content = listOf(TextContent(prompt)))
        } catch (e: Exception) {
            CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "Error generating analysis prompt: ${e.message ?: e::class.simpleName}"
                        )
                    )
            )
        }
    }
}
