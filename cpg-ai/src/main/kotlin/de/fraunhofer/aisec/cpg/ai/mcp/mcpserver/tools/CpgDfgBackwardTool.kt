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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgDfgBackwardPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.DEFAULT_LIST_LIMIT
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.NodeInfo
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.addTool
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.findNodeById
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.paginate
import de.fraunhofer.aisec.cpg.graph.collectAllPrevDFGPaths
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.helpers.mapFlatMapped
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.uuid.Uuid
import kotlinx.serialization.json.Json

fun Server.addDfgBackwardTool() {
    val toolDescription =
        """
        Traverse the Data Flow Graph (DFG) backwards from a given node to find where data originates.

        Uses the CPG's built-in dataflow analysis with backward direction to trace data sources.
        The analysis follows prevDFG edges and stops at nodes that have no further incoming data flows.

        Results are capped at $DEFAULT_LIST_LIMIT nodes by default; use the limit/offset parameters to paginate through more.

        Example usage:
        - "Where does this value come from?"
    """
            .trimIndent()

    this.addTool<CpgDfgBackwardPayload>(name = "cpg_dfg_backward", description = toolDescription) {
        result: TranslationResult,
        payload: CpgDfgBackwardPayload ->
        dfgBackward(result, payload)
    }
}

internal fun dfgBackward(
    result: TranslationResult,
    payload: CpgDfgBackwardPayload,
): CallToolResult {
    val startNode =
        result.findNodeById(Uuid.parse(payload.id).toString())
            ?: return CallToolResult(
                content = listOf(TextContent("No node found with ID ${payload.id}"))
            )

    val paths = startNode.collectAllPrevDFGPaths()
    val nodes = paths.mapFlatMapped({ it.nodes }) { NodeInfo(it) }

    // One JSON array per call, sliced to a page, so that a node with a huge backward slice cannot
    // flood the context by itself; the summary says how to fetch the rest.
    val page = paginate(nodes.toList(), payload.limit, payload.offset)
    return CallToolResult(
        content =
            listOf(TextContent(Json.encodeToString(page.items))) +
                listOfNotNull(page.summary?.let { TextContent(it) })
    )
}
