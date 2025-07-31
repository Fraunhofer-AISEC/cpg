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
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@Serializable data class CpgApplyConceptsPayload(val applications: List<ConceptApplication>)

@Serializable data class ConceptApplication(val nodeId: String, val conceptType: String)

fun Server.addCpgApplyConceptsTool() {
    val toolDescription =
        """
            Apply concept overlays to specific nodes in the CPG.
            
            This tool attaches security concepts (like 'Data', 'ReadData', 'Authentication') 
            to specific nodes in the graph.
            
            Available concepts: Data, ReadData, Authentication, HttpRequest, etc.
            
            Parameters:
            - applications: List of concept applications to perform
              Each application contains:
              - nodeId: ID of the node to apply concept to
              - conceptType: Type of concept to apply (e.g., 'Data', 'ReadData')
        """
            .trimIndent()

    val inputSchema =
        Tool.Input(
            properties =
                buildJsonObject {
                    putJsonObject("applications") {
                        put("type", "array")
                        put("description", "List of concept applications to perform")
                        putJsonObject("items") {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("nodeId") {
                                    put("type", "string")
                                    put("description", "ID of the node to apply concept to")
                                }
                                putJsonObject("conceptType") {
                                    put("type", "string")
                                    put(
                                        "description",
                                        "Type of concept to apply (e.g., 'Data', 'ReadData')",
                                    )
                                }
                            }
                            putJsonArray("required") {
                                add("nodeId")
                                add("conceptType")
                            }
                        }
                    }
                },
            required = listOf("applications"),
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

            payload.applications.forEach { app ->
                // Find the node by ID
                val node = result.allChildren<Node>().find { it.id.toString() == app.nodeId }
                if (node != null) {
                    val concept = createConcepts(app.conceptType, node)
                    if (concept != null) {
                        concept.underlyingNode = node
                        applied.add(
                            "Applied ${app.conceptType} to node ${app.nodeId} (${node::class.simpleName})"
                        )
                    } else {
                        applied.add(
                            "Failed to create concept ${app.conceptType} - concept type not found or invalid"
                        )
                    }
                } else {
                    applied.add("Node ${app.nodeId} not found")
                }
            }

            val summary =
                "Applied ${payload.applications.size} concept(s):\n" + applied.joinToString("\n")

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

abstract class Privacy(underlyingNode: Node? = null) : Concept(underlyingNode)

class Data(underlyingNode: Node? = null) : Privacy(underlyingNode)

abstract class PrivacyOperation(underlyingNode: Node? = null, concept: Privacy) :
    Operation(underlyingNode, concept)

class ReadData(underlyingNode: Node? = null, concept: Privacy) :
    PrivacyOperation(underlyingNode, concept)

fun createConcepts(conceptType: String, node: Node): OverlayNode? {
    // Try to find the concept class in common packages
    val conceptClass = findConceptClass(conceptType)
    return conceptClass?.let { clazz ->
        val constructor =
            clazz.constructors.find { constructor ->
                constructor.parameters.size == 1 &&
                    constructor.parameters[0].type.classifier == Node::class
            }
        constructor?.call(node) as? OverlayNode
    }
}

fun findConceptClass(conceptType: String): KClass<*>? {
    val packagePrefixes =
        listOf(
            "de.fraunhofer.aisec.cpg.graph.concepts.",
            "de.fraunhofer.aisec.cpg.graph.concepts.auth.",
            "de.fraunhofer.aisec.cpg.graph.concepts.config.",
            "de.fraunhofer.aisec.cpg.graph.concepts.file.",
            "de.fraunhofer.aisec.cpg.graph.concepts.http.",
            "de.fraunhofer.aisec.cpg.graph.concepts.logging.",
            "de.fraunhofer.aisec.cpg.graph.concepts.memory.",
            "de.fraunhofer.aisec.cpg.graph.concepts.policy.",
            "de.fraunhofer.aisec.cpg.mcp.",
        )

    for (prefix in packagePrefixes) {
        val className = prefix + conceptType
        val clazz = Class.forName(className).kotlin
        if (OverlayNode::class.java.isAssignableFrom(clazz.java)) {
            return clazz
        }
    }
    return null
}
