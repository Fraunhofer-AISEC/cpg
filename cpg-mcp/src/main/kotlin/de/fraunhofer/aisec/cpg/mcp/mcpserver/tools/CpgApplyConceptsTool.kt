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
import de.fraunhofer.aisec.cpg.graph.concepts.conceptBuildHelper
import de.fraunhofer.aisec.cpg.graph.listOverlayClasses
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgApplyConceptsPayload
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

fun Server.addCpgApplyConceptsTool() {
    val availableConcepts = listOverlayClasses<Concept>()
    val availableOperations = listOverlayClasses<Operation>()
    val toolDescription =
        """
            Apply concepts or operations to specific nodes in the CPG.
            
            This tool creates and attaches concepts or operations using their 
            fully qualified class names (FQN) to specific nodes in the graph.
            
            Example usage:
            - "Apply concepts to the nodes you identified"
            
            Parameters:
            - assignments: List of overlay assignments to perform
              Each assignment contains:
              - nodeId: ID of the node to apply overlay to
              - overlay: Fully qualified name of concept or operation class
              
            Available concepts:
            ${availableConcepts.joinToString("\n") { "- ${it.name}" }}
            
            Available operations:
            ${availableOperations.joinToString("\n") { "- ${it.name}" }}
        """
            .trimIndent()

    val inputSchema =
        Tool.Input(
            properties =
                buildJsonObject {
                    putJsonObject("assignments") {
                        put("type", "array")
                        put("description", "List of concept assignments to perform")
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
                            }
                            putJsonArray("required") {
                                add("nodeId")
                                add("overlay")
                            }
                        }
                    }
                },
            required = listOf("assignments"),
        )

    this.addTool(
        name = "cpg_apply_concepts",
        description = toolDescription,
        inputSchema = inputSchema,
    ) { request ->
        try {
            val result =
                globalAnalysisResult
                    ?: return@addTool CallToolResult(
                        content =
                            listOf(
                                TextContent(
                                    "No analysis result available. Please analyze your code first using cpg_analyze."
                                )
                            )
                    )

            val payload =
                Json.decodeFromString<CpgApplyConceptsPayload>(
                    Json.encodeToString(request.arguments)
                )

            val applied = mutableListOf<String>()

            payload.assignments.forEach { assignment ->
                // Find the node by ID
                val node = result.allChildren<Node>().find { it.id.toString() == assignment.nodeId }
                if (node != null) {
                    try {
                        result.conceptBuildHelper(
                            name = assignment.overlay,
                            underlyingNode = node,
                            connectDFGUnderlyingNodeToConcept = true,
                        )

                        applied.add(
                            "Applied ${assignment.overlay} to node ${assignment.nodeId} (${node::class.simpleName})"
                        )
                    } catch (e: Exception) {
                        applied.add(
                            "Failed to create ${assignment.overlay} for node ${assignment.nodeId}: ${e.message}"
                        )
                    }
                } else {
                    applied.add("Node ${assignment.nodeId} not found")
                }
            }

            val summary =
                "Applied ${payload.assignments.size} concept(s):\n" + applied.joinToString("\n")

            CallToolResult(content = listOf(TextContent(summary)))
        } catch (e: Exception) {
            CallToolResult(
                content =
                    listOf(
                        TextContent("Error applying concepts: ${e.message ?: e::class.simpleName}")
                    )
            )
        }
    }
}
