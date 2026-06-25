/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.Granularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.PartialDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.PointerDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.graph.reachingWrites
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgIdPayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.NodeInfo
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.addTool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LastWriteInfo(val node: NodeInfo, val granularity: String, val functionSummary: Boolean)

private fun Granularity.toTag(): String =
    when (this) {
        is FullDataflowGranularity -> "full"
        is PartialDataflowGranularity<*> -> "partial:${this.partialTarget}"
        is PointerDataflowGranularity -> "pointer:${this.pointerTarget}"
    }

fun getLastWrite(result: TranslationResult, payload: CpgIdPayload): CallToolResult {
    val startId = Uuid.parse(payload.id)
    val startNode =
        result.nodes.find { it.id == startId }
            ?: return CallToolResult(
                content = listOf(TextContent("No node found with ID ${payload.id}"))
            )

    val writes =
        startNode.reachingWrites().map {
            LastWriteInfo(
                node = NodeInfo(it.source),
                granularity = it.granularity.toTag(),
                functionSummary = it.functionSummary,
            )
        }

    return CallToolResult(content = listOf(TextContent(Json.encodeToString(writes))))
}

fun Server.addGetLastWriteTool() {
    val toolDescription =
        """
      Find all writes that may have produced the current value of a given node, a one-hop,
      exhaustive reaching-definitions query.

      Reads the node's materialized prevDFG edges directly (already computed by PointsToPass's
      kill/union analysis across all control-flow branches), so it returns every write that could
      reach this point, not just one example path. Each result includes the writing node, its
      dataflow granularity (full/partial/pointer), and whether it came from a cross-function
      summary rather than a direct write in this scope.

      Example usage:
      - "What are all the writes that could have produced this value?"
      - "What was the last write to this variable before this point?"
  """
            .trimIndent()

    this.addTool<CpgIdPayload>(name = "cpg_get_last_write", description = toolDescription) {
        result: TranslationResult,
        payload: CpgIdPayload ->
        getLastWrite(result, payload)
    }
}
