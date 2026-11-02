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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgDfgPathsPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgSession
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.DEFAULT_LIST_LIMIT
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.Page
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.addTool
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.findNodeById
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.noNodeFoundResult
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.paginate
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
import de.fraunhofer.aisec.cpg.serialization.NodeInfo
import de.fraunhofer.aisec.cpg.serialization.NodePaths
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.Json

/** The longest dataflow path that is followed, in steps. */
private const val MAX_PATH_STEPS = 50

/**
 * The dataflow paths from [start] in one direction, until the dataflow ends, as the page selected
 * by [limit]/[offset]. The paths are sorted by their length, so the first page holds the shortest
 * ones. Each path is returned in the direction of the flow: for [backward] paths, from the source
 * to [start]. [NodePaths.truncated] says whether paths were cut at [MAX_PATH_STEPS]; the summary of
 * the page says how to fetch the paths that are not on it.
 */
internal fun dfgPaths(
    start: Node,
    backward: Boolean,
    interprocedural: Boolean,
    limit: Int? = null,
    offset: Int? = null,
): Page<NodePaths> {
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
    val page = paginate(all, limit, offset)
    val code = start.code?.lineSequence()?.firstOrNull()?.trim() ?: start.name.localName
    val paths =
        NodePaths(
            kind = "dataflow",
            description =
                if (backward) "where the value of `$code` comes from"
                else "where the value of `$code` goes to",
            start = NodeInfo(start),
            paths = page.items.map { path -> path.map { NodeInfo(it) } },
            truncated = result.failed.any { it.first == FailureReason.STEPS_EXCEEDED },
        )
    return Page(listOf(paths), page.summary)
}

/** `cpg_dfg_backward`: the dataflow paths that lead to the node. */
internal fun dfgBackward(session: CpgSession, payload: CpgDfgPathsPayload): CallToolResult =
    dfgPathsResult(session, payload, backward = true)

/** `cpg_dfg_forward`: the dataflow paths that start at the node. */
internal fun dfgForward(session: CpgSession, payload: CpgDfgPathsPayload): CallToolResult =
    dfgPathsResult(session, payload, backward = false)

private fun dfgPathsResult(
    session: CpgSession,
    payload: CpgDfgPathsPayload,
    backward: Boolean,
): CallToolResult {
    val start = session.findNodeById(payload.id) ?: return noNodeFoundResult(payload.id)
    val page = dfgPaths(start, backward, payload.interprocedural, payload.limit, payload.offset)
    // One JSON object with the paths of the page, and the summary of the page if paths are left
    return CallToolResult(
        content =
            listOf(TextContent(Json.encodeToString(page.items.single()))) +
                listOfNotNull(page.summary?.let { TextContent(it) })
    )
}

/** Adds `cpg_dfg_backward`: the dataflow paths that lead to a node. */
fun Server.addDfgBackwardTool() =
    this.addTool<CpgDfgPathsPayload>(
        readOnly = true,
        name = "cpg_dfg_backward",
        description =
            """
            Follow the Data Flow Graph (DFG) backwards from a node to find where its value comes from.

            Returns the dataflow paths that lead to the node, each from its source to the node, in
            the format {kind, description, start, paths, truncated}. Each path is a list of nodes
            with their IDs and locations. The search follows prevDFG edges, by default also through
            called functions, until the dataflow ends; paths longer than $MAX_PATH_STEPS steps are
            cut (truncated).

            The paths are sorted by length, shortest first. Results are capped at $DEFAULT_LIST_LIMIT paths by default; use the limit/offset parameters to paginate through more.

            Example usage:
            - "Where does this value come from?"
            - "Which inputs reach this call?"
            """
                .trimIndent(),
        handler = ::dfgBackward,
    )

/** Adds `cpg_dfg_forward`: the dataflow paths that start at a node. */
fun Server.addDfgForwardTool() =
    this.addTool<CpgDfgPathsPayload>(
        readOnly = true,
        name = "cpg_dfg_forward",
        description =
            """
            Follow the Data Flow Graph (DFG) forwards from a node to find where its value goes to.

            Returns the dataflow paths that start at the node, each from the node to where the
            dataflow ends, in the format {kind, description, start, paths, truncated}. Each path is a
            list of nodes with their IDs and locations. The search follows nextDFG edges, by default
            also through called functions; paths longer than $MAX_PATH_STEPS steps are cut
            (truncated).

            The paths are sorted by length, shortest first. Results are capped at $DEFAULT_LIST_LIMIT paths by default; use the limit/offset parameters to paginate through more.

            Example usage:
            - "Where does this value go to?"
            - "Is this key ever passed to a logging function?"
            """
                .trimIndent(),
        handler = ::dfgForward,
    )
