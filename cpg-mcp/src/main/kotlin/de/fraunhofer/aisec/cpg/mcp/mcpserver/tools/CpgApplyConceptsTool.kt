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

import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.conceptBuildHelper
import de.fraunhofer.aisec.cpg.graph.concepts.operationBuildHelper
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgApplyConceptsPayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.getAvailableConcepts
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.getAvailableOperations
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

/** Provides a tool to list all available concepts which can be used to tag nodes in the CPG. */
fun Server.listAvailableConcepts() {
    val availableConcepts = getAvailableConcepts()
    val toolDescription =
        "This tool provides a list of all concepts that can be applied to nodes in the CPG."
    this.addTool(name = "cpg_list_available_concepts", description = toolDescription) { _ ->
        CallToolResult(content = availableConcepts.map { TextContent(it.name) })
    }
}

/** Provides a tool to list all available operations which can be used to tag nodes in the CPG. */
fun Server.listAvailableOperations() {
    val availableOperations = getAvailableOperations()
    val toolDescription =
        "This tool provides a list of all operations that can be applied to nodes in the CPG."
    this.addTool(name = "cpg_list_available_operations", description = toolDescription) { _ ->
        CallToolResult(content = availableOperations.map { TextContent(it.name) })
    }
}

/**
 * Adds a tool to the server that allows applying concepts or operations to specific nodes in the
 * CPG.
 *
 * This tool can be used to create and attach concepts or operations to nodes identified by their
 * IDs.
 */
fun Server.addCpgApplyConceptsTool() {
    val availableConcepts = getAvailableConcepts()
    val availableOperations = getAvailableOperations()
    val toolDescription =
        """
            Apply concepts or operations to specific nodes in the CPG.
            
            This tool creates and attaches concepts or operations using their 
            fully qualified class names (FQN) to specific nodes in the graph.
            
            Example usage:
            - "Apply concepts to the nodes you identified"
            - "Tag the node with identifier <nodeId> with concept <overlay>"
            
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
                                putJsonObject("overlayType") {
                                    put("type", "string")
                                    put("description", "Type of overlay: 'Concept' or 'Operation'")
                                }
                                putJsonObject("conceptNodeId") {
                                    put("type", "string")
                                    put(
                                        "description",
                                        "NodeId of the concept this operation references (only for operations)",
                                    )
                                }
                                putJsonObject("arguments") {
                                    put("type", "object")
                                    put(
                                        "description",
                                        "Additional constructor arguments (optional)",
                                    )
                                }
                            }
                            putJsonArray("required") {
                                add("nodeId")
                                add("overlay")
                                add("overlayType")
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
                try {
                    val node = result.nodes.find { it.id.toString() == assignment.nodeId }
                    if (node == null) {
                        applied.add("Node ${assignment.nodeId} not found")
                        return@forEach
                    }

                    when (assignment.overlayType?.lowercase()) {
                        "concept" -> {
                            result.conceptBuildHelper(
                                name = assignment.overlay,
                                underlyingNode = node,
                                constructorArguments = /*assignment.arguments ?:*/
                                    emptyMap(), // TODO: handle arguments
                                connectDFGUnderlyingNodeToConcept = true,
                            )
                            applied.add(
                                "Applied concept ${assignment.overlay} to node ${assignment.nodeId} (${node::class.simpleName})"
                            )
                        }
                        "operation" -> {
                            val conceptNodeId = assignment.conceptNodeId
                            if (conceptNodeId == null) {
                                applied.add(
                                    "Cannot apply operation ${assignment.overlay} to node ${assignment.nodeId}: conceptNodeId is required for operations"
                                )
                                return@forEach
                            }

                            val conceptNode =
                                result.nodes.find { it.id.toString() == conceptNodeId }
                            val concept =
                                conceptNode?.overlays?.filterIsInstance<Concept>()?.firstOrNull()
                            if (concept == null) {
                                applied.add(
                                    "Cannot apply operation ${assignment.overlay} to node ${assignment.nodeId}: No concept found on node ${conceptNodeId}"
                                )
                                return@forEach
                            }

                            result.operationBuildHelper(
                                name = assignment.overlay,
                                underlyingNode = node,
                                concept = concept,
                                constructorArguments = /*assignment.arguments ?:*/
                                    emptyMap(), // TODO: handle arguments
                                connectDFGUnderlyingNodeToConcept = true,
                            )
                            applied.add(
                                "Applied operation ${assignment.overlay} to node ${assignment.nodeId} with concept ${concept::class.simpleName}"
                            )
                        }
                        else -> {
                            applied.add(
                                "Unknown overlay type '${assignment.overlayType}' for node ${assignment.nodeId}"
                            )
                        }
                    }
                } catch (e: Exception) {
                    applied.add(
                        "Failed to create ${assignment.overlayType} ${assignment.overlay} for node ${assignment.nodeId}: ${e.message}"
                    )
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
