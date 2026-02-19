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

import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.mcp.mcpserver.cpgDescription
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.*
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgLlmAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.getAvailableConcepts
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.getAvailableOperations
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toObject
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toSchema
import de.fraunhofer.aisec.cpg.serialization.toJSON
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageRequest
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.ModelPreferences
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.SamplingMessage
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable data class OverlaySuggestionsResponse(val items: List<OverlaySuggestion>)

fun Server.addCpgLlmAnalyzeTool() {
    val toolDescription =
        """
        Generate a prompt asking the LLM to suggest concepts/operations.
        
        This tool creates a detailed prompt that asks the LLM to act as a software engineer with expertise in software security
        and suggest appropriate concepts and operations for the analyzed code. 
        After using this tool, review the suggestions before applying them with cpg_apply_concepts.
        """
            .trimIndent()

    this.addTool(
        name = "cpg_llm_analyze",
        description = toolDescription,
        inputSchema = CpgLlmAnalyzePayload::class.toSchema(),
    ) { request ->
        try {
            val payload =
                if (request.arguments.isNullOrEmpty()) {
                    CpgLlmAnalyzePayload()
                } else {
                    request.arguments?.toObject<CpgLlmAnalyzePayload>() ?: CpgLlmAnalyzePayload()
                }

            val hasAnalysisResult = globalAnalysisResult != null
            val availableConcepts = getAvailableConcepts()
            val availableOperations = getAvailableOperations()

            val prompt = buildString {
                appendLine("# Code Analysis")
                appendLine()
                appendLine(
                    "You are a Software engineer with expertise in software security for more than 10 years.\n"
                )
                appendLine()

                appendLine("## About CPG")
                appendLine(cpgDescription)
                appendLine()

                appendLine("## Goal")
                appendLine(
                    "Mark interesting data and operations so we can analyze how data flows through code to discover patterns."
                )
                appendLine()

                appendLine("## Understanding Concepts and Operations")
                appendLine()
                appendLine("**Concepts** mark 'what something IS':")
                appendLine("- Applied to data-holding nodes (variables, fields, return values)")
                appendLine("- Examples: user_email → Data, api_token → Secret")
                appendLine("- Purpose: Track where important data is stored")
                appendLine()
                appendLine("**Operations** mark 'what something DOES':")
                appendLine("- Applied to nodes that perform actions (function calls, method calls)")
                appendLine(
                    "- Examples: requests.post() → HttpRequest, file.write() → FileWrite, encrypt() → Encryption"
                )
                appendLine("- Purpose: Track what happens to important data")
                appendLine()

                appendLine("## Rules")
                appendLine(
                    "1. **Domain consistency**: When using Operations, they must match their Concept's domain (e.g., GetSecret needs Secret, HttpRequest needs HttpClient)"
                )
                appendLine(
                    "2. **Operations always need Concepts**: Every Operation MUST have a conceptNodeId pointing to an existing Concept node. However, Concepts can stand alone without Operations."
                )
                appendLine()

                appendLine("## Your Task")
                appendLine("1. Analyze each node for relevance")
                appendLine("2. Suggest appropriate overlays using fully qualified class names")
                appendLine("3. Ensure concept-operation pairs belong to the same domain")
                appendLine()
                appendLine(
                    "**IMPORTANT:** Use only existing CPG concepts/operations from the list below."
                )
                appendLine(
                    "For additional context, you can check docstrings in the Fraunhofer CPG repository on GitHub, especially the cpg-concepts module."
                )
                appendLine()

                appendLine("Available concepts:")
                availableConcepts.forEach { appendLine("- ${it.name}") }
                appendLine()
                appendLine("Available operations:")
                availableOperations.forEach { appendLine("- ${it.name}") }
                appendLine()

                if (payload.description != null) {
                    appendLine("## Additional Context")
                    appendLine(payload.description)
                    appendLine()
                }

                if (hasAnalysisResult) {
                    appendLine("## Nodes to Analyze")
                    appendLine("Here are all the nodes from the CPG analysis:")
                    appendLine("```json")
                    val nodes = globalAnalysisResult.nodes
                    nodes.forEach { node ->
                        appendLine(Json.encodeToString(node.toJSON(noEdges = true)))
                    }
                    appendLine("```")
                } else {
                    appendLine("## No Analysis Available")
                    appendLine("No CPG analysis available. Analyze a file first using cpg_analyze.")
                }
                appendLine()
                appendLine("## Expected Response Format")
                appendLine("Respond with a JSON object in this exact format:")
                appendLine(
                    """
                {
                  "items": [
                    {
                      "nodeId": "1234",
                      "overlay": "fully.qualified.class.Name",
                      "overlayType": "Concept" | "Operation",
                      "conceptNodeId": "string (REQUIRED for operations)",
                      "reasoning": "Security reasoning for this classification",
                      "securityImpact": "Potential security implications"
                    }
                  ]
                }
                """
                        .trimIndent()
                )
                appendLine()
                appendLine(
                    "**IMPORTANT**: After providing your analysis, WAIT for user approval before applying any concepts. Do not automatically execute cpg_apply_concepts."
                )
            }

            // Get the session ID
            val sessionId = this.sessions.values.firstOrNull()?.sessionId

            if (sessionId == null) {
                CallToolResult(
                    content = listOf(TextContent("Error: No active client session found")),
                    isError = true,
                )
            } else {
                // Use sampling to send the prompt to the LLM via the client
                val samplingRequest =
                    CreateMessageRequest(
                        params =
                            CreateMessageRequestParams(
                                messages =
                                    listOf(
                                        SamplingMessage(
                                            role = Role.User,
                                            content = TextContent(text = prompt),
                                        )
                                    ),
                                systemPrompt =
                                    "You are a software security expert analyzing code for security vulnerabilities.",
                                maxTokens = 4000,
                                modelPreferences =
                                    ModelPreferences(
                                        intelligencePriority = 0.9,
                                        speedPriority = 0.5,
                                    ),
                            )
                    )

                // Send sampling request to client (which will forward to LLM)
                val result = this.createMessage(sessionId, samplingRequest)

                // Extract the LLM response
                val llmResponse = (result.content as? TextContent)?.text ?: "No response from LLM"

                // Extract and validate JSON
                val jsonStr = extractJson(llmResponse)

                // Parse LLM response and enrich with further node properties
                try {
                    val parsed = Json.decodeFromString<OverlaySuggestionsResponse>(jsonStr)
                    val nodes = globalAnalysisResult?.nodes ?: emptyList()

                    val enrichedItems =
                        parsed.items.map { suggestion ->
                            val node = nodes.find { it.id.toString() == suggestion.nodeId }

                            suggestion.copy(
                                name = node?.name?.toString() ?: suggestion.name,
                                type = node?.javaClass?.simpleName ?: suggestion.type,
                                code = node?.code ?: suggestion.code,
                                fileName =
                                    node
                                        ?.location
                                        ?.artifactLocation
                                        ?.uri
                                        ?.toString()
                                        ?.substringAfterLast('/')
                                        ?.substringAfterLast('\\') ?: suggestion.fileName,
                                startLine =
                                    node?.location?.region?.startLine ?: suggestion.startLine,
                                endLine = node?.location?.region?.endLine ?: suggestion.endLine,
                            )
                        }

                    val response = OverlaySuggestionsResponse(items = enrichedItems)
                    CallToolResult(content = listOf(TextContent(Json.encodeToString(response))))
                } catch (_: Exception) {
                    // If parsing fails, return the extracted JSON anyway
                    CallToolResult(content = listOf(TextContent(jsonStr)))
                }
            }
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

/**
 * Extract JSON from LLM response that might be wrapped in markdown code blocks or contain extra
 * text
 */
private fun extractJson(response: String): String {
    val trimmed = response.trim()

    val fencedMatch = Regex("(?s)```(?:json)?\\s*(\\{.*?\\})\\s*```").find(trimmed)
    if (fencedMatch != null) {
        return fencedMatch.groupValues[1]
    }

    // Try to extract JSON between first { and last }
    val firstBrace = trimmed.indexOf('{')
    val lastBrace = trimmed.lastIndexOf('}')
    if (firstBrace >= 0 && lastBrace > firstBrace) {
        return trimmed.substring(firstBrace, lastBrace + 1)
    }

    // Return as-is if no extraction worked
    return trimmed
}
