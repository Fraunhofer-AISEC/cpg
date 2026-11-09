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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgDataflowPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgSession
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.DEFAULT_LIST_LIMIT
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.DataflowResult
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.addTool
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.paginate
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.toQueryTreeNode
import de.fraunhofer.aisec.cpg.graph.Forward
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.Intraprocedural
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.allChildrenWithOverlays
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMConcept
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMOperation
import de.fraunhofer.aisec.cpg.query.May
import de.fraunhofer.aisec.cpg.query.dataFlow
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.Json

fun Server.addDataflowTool() {
    val toolDescription =
        """
        Analyze the data flow of the nodes.
        
        This tool performs data flow analysis to find paths between source 
        and target concepts that have been applied to nodes in the graph.
        
        You must first apply concepts to nodes, using cpg_apply_concepts or
        cpg_add_llm_concept_and_operations, before running data flow analysis.

        It returns one path per source node that reaches a target, in pages of at most
        $DEFAULT_LIST_LIMIT paths; use the limit/offset parameters to page through more.
        
        Example usage:
        - Is there a data flow from sensitive data to HTTP Request?
        - Track file reads to network calls     
    """
            .trimIndent()

    this.addTool<CpgDataflowPayload>(
        readOnly = true,
        name = "cpg_dataflow",
        description = toolDescription,
        handler = ::dataflow,
    )
}

/**
 * The paths from the nodes the concept [CpgDataflowPayload.from] is applied to, to those of
 * [CpgDataflowPayload.to], as one [DataflowResult] holding the page of paths selected by its
 * limit/offset (see [paginate]), followed by the note on how to fetch more. Only the paths of the
 * page are serialized.
 */
internal fun dataflow(session: CpgSession, payload: CpgDataflowPayload): CallToolResult {
    val allOverlayNodes = session.translationResult.allChildrenWithOverlays<OverlayNode>()
    val sourceNodes = allOverlayNodes.filter { it.isNamed(payload.from) }
    val targetNodes = allOverlayNodes.filter { it.isNamed(payload.to) }

    if (sourceNodes.isEmpty() || targetNodes.isEmpty()) {
        return CallToolResult(
            content =
                listOf(
                    TextContent(
                        "No concept found. Apply concepts first using cpg_apply_concepts or " +
                            "cpg_add_llm_concept_and_operations."
                    )
                )
        )
    }

    val targets = targetNodes.mapNotNullTo(mutableSetOf()) { it.underlyingNode }
    val paths =
        sourceNodes.mapNotNull { sourceOverlay ->
            val sourceNode = sourceOverlay.underlyingNode ?: return@mapNotNull null
            dataFlow(
                    startNode = sourceNode,
                    direction = Forward(GraphToFollow.DFG),
                    type = May,
                    scope = Intraprocedural(),
                    predicate = { node -> node in targets },
                )
                .takeIf { it.value }
        }

    val page = paginate(paths, payload.limit, payload.offset)
    val dataflowResult =
        DataflowResult(
            fromConcept = payload.from,
            toConcept = payload.to,
            foundPaths = page.items.map { it.toQueryTreeNode() },
        )

    return CallToolResult(
        content =
            listOf(TextContent(Json.encodeToString(dataflowResult))) +
                listOfNotNull(page.summary?.let { TextContent(it) })
    )
}

/**
 * Whether this overlay is the concept or operation called [conceptName]: one of the fixed catalog
 * by its node name (e.g. `ReadData`), or one defined via cpg_add_llm_concept_and_operations by the
 * name given there, since its node name is `GenericLLMConcept[<name>]` or
 * `GenericLLMOperation[<name>]`.
 */
private fun OverlayNode.isNamed(conceptName: String): Boolean =
    name.localName == conceptName ||
        when (this) {
            is GenericLLMConcept -> this.conceptName == conceptName
            is GenericLLMOperation -> operationName == conceptName
            else -> false
        }
