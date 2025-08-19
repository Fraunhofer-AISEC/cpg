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
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.listOverlayClasses
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgLlmAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toNodeInfo
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
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
        - description: Additional context for the analysis (optional)
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

    val outputSchema =
        Tool.Output(
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
                                            put("description", "ID of the node to apply overlay to")
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
                                                "NodeId of concept this operation references (for operations)",
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

    this.addTool(
        name = "cpg_llm_analyze",
        description = toolDescription,
        inputSchema = inputSchema,
        outputSchema = outputSchema,
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

            val nodes = globalAnalysisResult?.nodes?.map { it.toNodeInfo() } ?: emptyList()
            val availableConcepts = listOverlayClasses<Concept>()
            val availableOperations = listOverlayClasses<Operation>()

            val prompt = buildString {
                appendLine("# Code Analysis")
                appendLine()
                appendLine(
                    "Please take on the role of a security engineer and analyze the provided code."
                )
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
                appendLine("- Purpose: Track where important data is stored")
                appendLine()
                appendLine("**Operations** mark 'what something DOES':")
                appendLine("- Applied to nodes that perform actions (function calls, method calls)")
                appendLine("- Purpose: Track what happens to important data")
                appendLine()

                appendLine("## Critical Rules")
                appendLine(
                    "1. **Same Domain**: Concepts and Operations must be semantically related"
                )
                appendLine(
                    "2. **Dataflow Connection**: Operations should process the Concept's data"
                )
                appendLine(
                    "3. **Concrete Classes**: Use specific implementations, not abstract base classes"
                )
                appendLine(
                    "4. **Operation Linking**: When suggesting an Operation, specify which Concept it processes using conceptNodeId"
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
                    "For additional context, you can check docstrings in the Fraunhofer CPG repository, especially cpg-concepts module."
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

abstract class Privacy(underlyingNode: Node? = null) : Concept(underlyingNode)

class Data(underlyingNode: Node? = null) : Privacy(underlyingNode)

abstract class PrivacyOperation(underlyingNode: Node? = null, concept: Privacy) :
    Operation(underlyingNode, concept)

class ReadData(underlyingNode: Node? = null, concept: Privacy) :
    PrivacyOperation(underlyingNode, concept)
