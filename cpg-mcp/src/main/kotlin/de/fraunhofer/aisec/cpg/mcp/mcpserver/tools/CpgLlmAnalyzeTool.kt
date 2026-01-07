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

import de.fraunhofer.aisec.cpg.mcp.mcpserver.cpgDescription
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgLlmAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.getAvailableConcepts
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.getAvailableOperations
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toObject
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

fun Server.addCpgLlmAnalyzeTool() {
    val toolDescription =
        """
        Generate a prompt asking the LLM to suggest concepts/operations.
        
        This tool creates a detailed prompt that asks the LLM to act as a software engineer with expertise in software security
        and suggest appropriate concepts and operations for the analyzed code. 
        After using this tool, review the suggestions before applying them with cpg_apply_concepts.
        """
            .trimIndent()

    val inputSchema =
        ToolSchema(
            properties =
                buildJsonObject {
                    putJsonObject("description") {
                        put("type", "string")
                        put("description", "Additional context for the analysis")
                    }
                },
            required = listOf(),
        )

    this.addTool(
        name = "cpg_llm_analyze",
        description = toolDescription,
        inputSchema = inputSchema,
        //        outputSchema = outputSchema - not supported by all LLMs yet
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
                    appendLine(
                        "Use the nodes from the previous cpg_analyze tool response to make your suggestions."
                    )
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
                  "overlaySuggestions": [
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

// Note: The output schema is not supported by all LLMs yet.
@Suppress("unused")
val outputSchema =
    ToolSchema(
        properties =
            buildJsonObject {
                putJsonObject("prompt") {
                    put("type", "string")
                    put("description", "Generated prompt for LLM analysis")
                }
                putJsonObject("expectedResponseFormat") {
                    put("type", "object")
                    put("description", "Expected JSON structure for LLM response")
                    putJsonObject("properties") {
                        putJsonObject("overlaySuggestions") {
                            put("type", "array")
                            put("description", "List of concept/operation suggestions")
                            putJsonObject("items") {
                                put("type", "object")
                                putJsonObject("properties") {
                                    putJsonObject("nodeId") {
                                        put("type", "string")
                                        put("description", "NodeId of the CPG node")
                                    }
                                    putJsonObject("overlay") {
                                        put("type", "string")
                                        put(
                                            "description",
                                            "Fully qualified name of concept or operation class",
                                        )
                                    }
                                    putJsonObject("overlayType") {
                                        put("type", "string")
                                        put(
                                            "description",
                                            "Type of overlay: 'Concept' or 'Operation'",
                                        )
                                    }
                                    putJsonObject("conceptNodeId") {
                                        put("type", "string")
                                        put(
                                            "description",
                                            "NodeId of concept this operation references (REQUIRED for ALL operations)",
                                        )
                                    }
                                    putJsonObject("arguments") {
                                        put("type", "object")
                                        put("description", "Additional constructor arguments")
                                    }
                                    putJsonObject("reasoning") {
                                        put("type", "string")
                                        put(
                                            "description",
                                            "Security reasoning for this classification",
                                        )
                                    }
                                    putJsonObject("securityImpact") {
                                        put("type", "string")
                                        put("description", "Potential security implications")
                                    }
                                }
                                putJsonArray("required") {
                                    add(JsonPrimitive("nodeId"))
                                    add(JsonPrimitive("overlay"))
                                    add(JsonPrimitive("overlayType"))
                                }
                            }
                        }
                    }
                }
            }
    )
