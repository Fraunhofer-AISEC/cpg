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
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgSlicePayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.addTool
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.serialization.DependenceGraph
import de.fraunhofer.aisec.cpg.serialization.SliceDirection
import de.fraunhofer.aisec.cpg.serialization.nodeSlice
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.uuid.Uuid
import kotlinx.serialization.json.Json

/** The furthest a slice reaches, in dependences, whatever is asked for. */
private const val MAX_SLICE_HOPS = 5

/** Adds `cpg_slice`: the statements that affect a node or are affected by it. */
fun Server.addSliceTool() {
    val toolDescription =
        """
        Compute a slice of the Program Dependence Graph (PDG) around a node: the statements that
        affect it (backward) or that it affects (forward), through data dependences (a value flows
        from one statement to the other) and control dependences (a condition decides whether a
        statement runs).

        Returns {kind, description, start, graph, direction, hops, interprocedural, statements,
        dependences, truncated}. Each statement has its ID, code, location and its distance from the
        node (depth); each dependence connects two statements by their IDs, with the variable (data)
        or the branch of the condition, true or false (control). Statements of kind STUB are code
        that is not part of the analysis.

        Use graph=CDG with direction=backward to find the conditions under which a statement runs,
        and follow the data of a condition backward to find what decides it.

        Example usage:
        - "Under which conditions does this call run?"
        - "What has to happen to reach this statement?"
        - "What does this statement affect?"
        """
            .trimIndent()

    this.addTool<CpgSlicePayload>(name = "cpg_slice", description = toolDescription) {
        result: TranslationResult,
        payload: CpgSlicePayload ->
        val startId = Uuid.parse(payload.id)
        val start =
            result.nodes.find { it.id == startId }
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent("No node found with ID ${payload.id}"))
                )
        val graph =
            DependenceGraph.entries.find { it.name.equals(payload.graph, ignoreCase = true) }
                ?: return@addTool CallToolResult(
                    content =
                        listOf(TextContent("Unknown graph ${payload.graph}: use PDG, DFG or CDG"))
                )
        val direction =
            SliceDirection.entries.find { it.name.equals(payload.direction, ignoreCase = true) }
                ?: return@addTool CallToolResult(
                    content =
                        listOf(
                            TextContent(
                                "Unknown direction ${payload.direction}: use backward or forward"
                            )
                        )
                )
        val slice =
            nodeSlice(
                start,
                direction,
                payload.hops.coerceIn(1, MAX_SLICE_HOPS),
                graph,
                payload.interprocedural,
            )
        CallToolResult(content = listOf(TextContent(Json.encodeToString(slice))))
    }
}
