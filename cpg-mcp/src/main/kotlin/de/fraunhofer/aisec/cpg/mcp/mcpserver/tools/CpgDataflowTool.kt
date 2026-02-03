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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Forward
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.Intraprocedural
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.allChildrenWithOverlays
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgDataflowPayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.DataflowResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.QueryTreeNode
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.runOnCpg
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toObject
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toQueryTreeNode
import de.fraunhofer.aisec.cpg.query.May
import de.fraunhofer.aisec.cpg.query.dataFlow
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun Server.addCpgDataflowTool() {
    val toolDescription =
        """
        Analyze the data flow of the nodes.
        
        This tool performs data flow analysis to find paths between source 
        and target concepts that have been applied to nodes in the graph.
        
        You must first apply concepts to nodes using cpg_apply_concepts 
        before running data flow analysis.
        
        Example usage:
        - Is there a data flow from sensitive data to HTTP Request?
        - Track file reads to network calls
        
        Parameters:
        - from: Source concept type (e.g., 'ReadData', 'Data', 'Authentication')
        - to: Target concept type (e.g., 'HttpRequest', 'CallExpression')
    """
            .trimIndent()

    val inputSchema =
        ToolSchema(
            properties =
                buildJsonObject {
                    putJsonObject("from") {
                        put("type", "string")
                        put(
                            "description",
                            "Source concept type (e.g., 'ReadData', 'Data', 'Authentication')",
                        )
                    }
                    putJsonObject("to") {
                        put("type", "string")
                        put(
                            "description",
                            "Target concept type (e.g., 'HttpRequest', 'CallExpression')",
                        )
                    }
                },
            required = listOf("from", "to"),
        )

    this.addTool(name = "cpg_dataflow", description = toolDescription, inputSchema = inputSchema) {
        request ->
        request.runOnCpg { result: TranslationResult, request: CallToolRequest ->
            val payload =
                request.arguments?.toObject<CpgDataflowPayload>()
                    ?: return@runOnCpg CallToolResult(
                        content =
                            listOf(TextContent("Invalid or missing payload for cpg_dataflow tool."))
                    )

            val allOverlayNodes = result.allChildrenWithOverlays<OverlayNode>()
            val sourceNodes = allOverlayNodes.filter { it.name.localName == payload.from }
            val targetNodes = allOverlayNodes.filter { it.name.localName == payload.to }

            if (sourceNodes.isEmpty() || targetNodes.isEmpty()) {
                return@runOnCpg CallToolResult(
                    content =
                        listOf(
                            TextContent(
                                "No concept found. Apply concepts first using cpg_apply_concepts."
                            )
                        )
                )
            }

            val qTrees = mutableListOf<QueryTreeNode>()

            sourceNodes.forEach { sourceOverlay ->
                val sourceNode = sourceOverlay.underlyingNode
                if (sourceNode != null) {
                    val queryTree =
                        dataFlow(
                            startNode = sourceNode,
                            direction = Forward(GraphToFollow.DFG),
                            type = May,
                            scope = Intraprocedural(),
                            predicate = { node ->
                                targetNodes.any { targetOverlay ->
                                    targetOverlay.underlyingNode == node
                                }
                            },
                        )

                    if (queryTree.value) {
                        qTrees.add(queryTree.toQueryTreeNode())
                    }
                }
            }

            val dataflowResult =
                DataflowResult(
                    fromConcept = payload.from,
                    toConcept = payload.to,
                    foundPaths = qTrees,
                )

            CallToolResult(content = listOf(TextContent(Json.encodeToString(dataflowResult))))
        }
    }
}
