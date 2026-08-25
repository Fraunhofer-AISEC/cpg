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
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.*
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgCallArgumentByNameOrIndexPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgIdPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgNamePayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.addTool
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.runOnCpg
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.toJson
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.persistence.McpDetailLevel
import de.fraunhofer.aisec.cpg.persistence.mcpRelatedNodes
import de.fraunhofer.aisec.cpg.persistence.mcpRelationships
import de.fraunhofer.aisec.cpg.serialization.toMcpView
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.Json

fun Server.listFunctions() {
    val toolDescription =
        """
        This tool lists all functions, more precisely function declarations, which are held in the graph.
        
        Example prompts:
        - "Show me all functions in the analyzed code"
        - "What functions are defined in this codebase?"
        """
            .trimIndent()

    this.addTool(name = "cpg_list_functions", description = toolDescription) { request ->
        request.runOnCpg { result: TranslationResult, _ ->
            CallToolResult(
                content = result.functions.map { TextContent(Json.encodeToString(it.toInfo())) }
            )
        }
    }
}

fun Server.listRecords() {
    val toolDescription =
        """
        This tool lists all classes and structs, more precisely their declarations as compact summaries.
        Use cpg_get_node with a id to retrieve the full node details.

        Example prompts:
        - "Show me all classes in the code"
        - "What data structures are defined here?"
        """
            .trimIndent()

    this.addTool(name = "cpg_list_records", description = toolDescription) { request ->
        request.runOnCpg { result: TranslationResult, _ ->
            CallToolResult(
                content = result.records.map { TextContent(Json.encodeToString(it.toInfo())) }
            )
        }
    }
}

fun Server.listConceptsAndOperations() {
    val toolDescription =
        "This tool lists all concepts (a special node marking 'what something IS') and operations (a special node marking 'what something DOES') which have been used as overlays to some nodes in the graph."

    this.addTool(name = "cpg_list_concepts_and_operations", description = toolDescription) { request
        ->
        request.runOnCpg { result: TranslationResult, _ ->
            val concepts =
                result.allChildrenWithOverlays<Concept>().map { TextContent(it.toJson()) }
            val operations =
                result.allChildrenWithOverlays<Operation>().map { TextContent(it.toJson()) }
            CallToolResult(content = concepts + operations)
        }
    }
}

fun Server.listCalls() {
    val toolDescription =
        """
        This tool lists all function and method calls as compact summaries.
        Use cpg_get_node with a id to retrieve the full node details.

        Example prompts:
        - "Show me all function calls in the code"
        - "What functions are being called?"
        """
            .trimIndent()

    this.addTool(name = "cpg_list_calls", description = toolDescription) { request ->
        request.runOnCpg { result: TranslationResult, _ ->
            CallToolResult(
                content = result.calls.map { TextContent(Json.encodeToString(it.toInfo())) }
            )
        }
    }
}

fun Server.listCallsTo() {
    val toolDescription =
        """
        This tool lists all function and method calls to the method/function with the specified name, which are held in the graph.

        Example prompts:
        - "Show me all calls to the function 'encrypt'"
        - "Where is the 'authenticate' function called?"
        """
            .trimIndent()

    this.addTool<CpgNamePayload>(name = "cpg_list_calls_to", description = toolDescription) {
        result: TranslationResult,
        payload: CpgNamePayload ->
        CallToolResult(content = result.calls(payload.name).map { TextContent(it.toJson()) })
    }
}

fun Server.getAllArgs() {
    val toolDescription =
        """This tool lists all arguments passed to the method/function call with the specified ID."""
            .trimIndent()

    this.addTool<CpgIdPayload>(name = "cpg_list_call_args", description = toolDescription) {
        result: TranslationResult,
        payload: CpgIdPayload ->
        CallToolResult(
            content =
                result.calls
                    .single { it.id.toString() == payload.id }
                    .arguments
                    .map { TextContent(it.toJson()) }
        )
    }
}

fun Server.getArgByIndexOrName() {
    val toolDescription =
        """This tool lists an argument passed to the method/function call with the specified ID either by name or by index.

        If both arguments, argName and index, are provided, the name takes precedence. At least one of argName or index must be provided.
        """
            .trimIndent()

    this.addTool<CpgCallArgumentByNameOrIndexPayload>(
        name = "cpg_list_call_arg_by_name_or_index",
        description = toolDescription,
    ) { result: TranslationResult, payload: CpgCallArgumentByNameOrIndexPayload ->
        CallToolResult(
            content =
                listOf(
                    TextContent(
                        result.calls
                            .single { it.id.toString() == payload.nodeId }
                            .argumentByNameOrPosition(
                                name = payload.argumentName,
                                position = payload.index,
                            )
                            ?.toJson() ?: "No argument found with the given name or index."
                    )
                )
        )
    }
}

fun Server.getNode() {
    val toolDescription =
        """
        Retrieves the complete information of a single node by its id, including its source code.
        Use this after list commands to inspect the actual code and details of specific nodes.
        """
            .trimIndent()

    this.addTool<CpgIdPayload>(name = "cpg_get_node", description = toolDescription) {
        result: TranslationResult,
        payload: CpgIdPayload ->
        val node = result.nodes.find { it.id.toString() == payload.id }
        if (node != null) {
            CallToolResult(
                content = listOf(TextContent(node.toMcpView(McpDetailLevel.FULL).toString()))
            )
        } else {
            CallToolResult(content = listOf(TextContent("No node found with ${payload.id}")))
        }
    }
}

fun Server.describeRelationships() {
    val toolDescription =
        """
        Lists the relationships available on a node by its id, e.g. AST children ("arguments",
        "parameters", "fields", "statements", ...), dataflow edges ("prevDFG", "nextDFG"), or other
        subclass-specific ones ("invoke" for calls). Use cpg_get_related_nodes with one of the
        returned names to actually fetch the connected nodes.

        Example prompts:
        - "What can I look at from this call?"
        - "What relationships does this node have?"
        """
            .trimIndent()

    this.addTool<CpgIdPayload>(
        name = "cpg_describe_relationships",
        description = toolDescription,
    ) { result: TranslationResult, payload: CpgIdPayload ->
        val node = result.nodes.find { it.id.toString() == payload.id }
        if (node != null) {
            val names = node::class.mcpRelationships.keys.sorted()
            CallToolResult(content = listOf(TextContent(Json.encodeToString(names))))
        } else {
            CallToolResult(content = listOf(TextContent("No node found with ${payload.id}")))
        }
    }
}

fun Server.getRelatedNodes() {
    val toolDescription =
        """
        Returns the nodes connected to a given node via the named relationship (see
        cpg_describe_relationships to discover available names). Results are compact summaries;
        use cpg_get_node with a returned id to inspect the full details of a specific one.

        Example prompts:
        - "Show me the arguments of this call"
        - "What are the parameters of this function?"
        - "Where does this value flow to next?"
        """
            .trimIndent()

    this.addTool<CpgRelatedNodesPayload>(
        name = "cpg_get_related_nodes",
        description = toolDescription,
    ) { result: TranslationResult, payload: CpgRelatedNodesPayload ->
        val node = result.nodes.find { it.id.toString() == payload.nodeId }
        if (node == null) {
            CallToolResult(content = listOf(TextContent("No node found with ${payload.nodeId}")))
        } else {
            val related = node.mcpRelatedNodes(payload.relationship)
            if (related == null) {
                CallToolResult(
                    content =
                        listOf(
                            TextContent(
                                "Unknown relationship '${payload.relationship}' for node type " +
                                    "${node::class.simpleName}. Use cpg_describe_relationships to " +
                                    "list the available relationships for this node."
                            )
                        )
                )
            } else {
                CallToolResult(
                    content =
                        related.map { TextContent(it.toMcpView(McpDetailLevel.SUMMARY).toString()) }
                )
            }
        }
    }
}
