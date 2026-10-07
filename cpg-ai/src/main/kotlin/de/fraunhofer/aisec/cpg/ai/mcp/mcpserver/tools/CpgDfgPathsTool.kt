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
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgDfgPathsPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.NodeInfo
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.NodePaths
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.addTool
import de.fraunhofer.aisec.cpg.graph.Backward
import de.fraunhofer.aisec.cpg.graph.ContextSensitive
import de.fraunhofer.aisec.cpg.graph.FailureReason
import de.fraunhofer.aisec.cpg.graph.FieldSensitive
import de.fraunhofer.aisec.cpg.graph.Forward
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.Interprocedural
import de.fraunhofer.aisec.cpg.graph.Intraprocedural
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.followDFGEdgesUntilHit
import de.fraunhofer.aisec.cpg.graph.nodes
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.uuid.Uuid
import kotlinx.serialization.json.Json

/** The longest dataflow path that is followed, in steps. */
private const val MAX_PATH_STEPS = 50

/** The most paths a tool returns, whatever is asked for. */
private const val MAX_PATHS = 100

/**
 * The dataflow paths from [start] in one direction, until the dataflow ends. Each path is returned
 * in the direction of the flow: for [backward] paths, from the source to [start].
 */
internal fun dfgPaths(
    start: Node,
    backward: Boolean,
    interprocedural: Boolean,
    maxPaths: Int,
): NodePaths {
    val scope =
        if (interprocedural) Interprocedural(maxSteps = MAX_PATH_STEPS)
        else Intraprocedural(maxSteps = MAX_PATH_STEPS)
    val result =
        start.followDFGEdgesUntilHit(
            collectFailedPaths = true,
            findAllPossiblePaths = true,
            direction = if (backward) Backward(GraphToFollow.DFG) else Forward(GraphToFollow.DFG),
            sensitivities = FieldSensitive + ContextSensitive,
            scope = scope,
        ) {
            // Nothing is searched for: every path is followed until the dataflow ends
            false
        }
    // Without a target, every path "fails" at its end, so the failed paths are all paths
    val all =
        result.failed
            .map { (_, path) -> if (backward) path.nodes.reversed() else path.nodes }
            .filter { it.size >= 2 }
            .distinctBy { path -> path.map { it.id } }
            .sortedBy { it.size }
    val limit = maxPaths.coerceIn(1, MAX_PATHS)
    val code = start.code?.lineSequence()?.firstOrNull()?.trim() ?: start.name.localName
    return NodePaths(
        kind = "dataflow",
        description =
            if (backward) "where the value of `$code` comes from"
            else "where the value of `$code` goes to",
        start = NodeInfo(start),
        paths = all.take(limit).map { path -> path.map { NodeInfo(it) } },
        truncated =
            all.size > limit || result.failed.any { it.first == FailureReason.STEPS_EXCEEDED },
    )
}

private fun Server.addDfgPathsTool(name: String, backward: Boolean, description: String) {
    this.addTool<CpgDfgPathsPayload>(name = name, description = description) {
        result: TranslationResult,
        payload: CpgDfgPathsPayload ->
        val startId = Uuid.parse(payload.id)
        val start =
            result.nodes.find { it.id == startId }
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent("No node found with ID ${payload.id}"))
                )
        val paths = dfgPaths(start, backward, payload.interprocedural, payload.maxPaths)
        CallToolResult(content = listOf(TextContent(Json.encodeToString(paths))))
    }
}

/** Adds `cpg_dfg_backward`: the dataflow paths that lead to a node. */
fun Server.addDfgBackwardTool() =
    addDfgPathsTool(
        name = "cpg_dfg_backward",
        backward = true,
        description =
            """
            Follow the Data Flow Graph (DFG) backwards from a node to find where its value comes from.

            Returns the dataflow paths that lead to the node, each from its source to the node, in
            the format {kind, description, start, paths, truncated}. Each path is a list of nodes
            with their IDs and locations. The search follows prevDFG edges, by default also through
            called functions, until the dataflow ends; long or many paths are cut (truncated).

            Example usage:
            - "Where does this value come from?"
            - "Which inputs reach this call?"
            """
                .trimIndent(),
    )

/** Adds `cpg_dfg_forward`: the dataflow paths that start at a node. */
fun Server.addDfgForwardTool() =
    addDfgPathsTool(
        name = "cpg_dfg_forward",
        backward = false,
        description =
            """
            Follow the Data Flow Graph (DFG) forwards from a node to find where its value goes to.

            Returns the dataflow paths that start at the node, each from the node to where the
            dataflow ends, in the format {kind, description, start, paths, truncated}. Each path is a
            list of nodes with their IDs and locations. The search follows nextDFG edges, by default
            also through called functions; long or many paths are cut (truncated).

            Example usage:
            - "Where does this value go to?"
            - "Is this key ever passed to a logging function?"
            """
                .trimIndent(),
    )
