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
import de.fraunhofer.aisec.cpg.graph.Backward
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.Interprocedural
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgIdPayload
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
import kotlin.uuid.Uuid
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun Server.addDfgBackwardTool() {
    val toolDescription =
        """
        Traverse the Data Flow Graph (DFG) backwards from a given node to find where data originates.

        Uses the CPG's built-in dataflow analysis with backward direction to trace data sources.
        The analysis follows prevDFG edges and stops at nodes that have no further incoming data flows.

        Example usage:
        - "Where does this value come from?"

        Parameters:
        - id: The ID of the node to start the backward traversal from.
    """
            .trimIndent()

    val inputSchema =
        ToolSchema(
            properties =
                buildJsonObject {
                    putJsonObject("id") {
                        put("type", "string")
                        put(
                            "description",
                            "The ID of the node to start the backward DFG traversal from.",
                        )
                    }
                },
            required = listOf("id"),
        )

    this.addTool(
        name = "cpg_dfg_backward",
        description = toolDescription,
        inputSchema = inputSchema,
    ) { request ->
        request.runOnCpg { result: TranslationResult, request: CallToolRequest ->
            val payload =
                request.arguments?.toObject<CpgIdPayload>()
                    ?: return@runOnCpg CallToolResult(
                        content =
                            listOf(
                                TextContent("Invalid or missing payload for cpg_dfg_backward tool.")
                            )
                    )

            val startId = Uuid.parse(payload.id)
            val startNode =
                result.nodes.find { it.id == startId }
                    ?: return@runOnCpg CallToolResult(
                        content = listOf(TextContent("No node found with ID ${payload.id}"))
                    )

            val queryTree =
                dataFlow(
                    startNode = startNode,
                    direction = Backward(GraphToFollow.DFG),
                    type = May,
                    scope = Interprocedural(),
                    predicate = { it.prevDFG.isEmpty() },
                )

            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(queryTree.toQueryTreeNode())))
            )
        }
    }
}
