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
package de.fraunhofer.aisec.codyze.console

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgCallArgumentByNameOrIndexPayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgIdPayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgNamePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toJson
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun Server.listFunctions() {
    val toolDescription =
        "This tool lists all functions, more precisely function declarations, which are held in the graph."

    this.addTool(name = "cpg_list_functions", description = toolDescription) { _ ->
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
            CallToolResult(content = result.functions.map { TextContent(it.toJson()) })
        } catch (e: Exception) {
            CallToolResult(
                content =
                    listOf(
                        TextContent("Error listing functions: ${e.message ?: e::class.simpleName}")
                    )
            )
        }
    }
}

fun Server.listRecords() {
    val toolDescription =
        "This tool lists all classes and structs, more precisely their declarations, which are held in the graph."

    this.addTool(name = "cpg_list_records", description = toolDescription) { _ ->
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
            CallToolResult(content = result.records.map { TextContent(it.toJson()) })
        } catch (e: Exception) {
            CallToolResult(
                content =
                    listOf(
                        TextContent("Error listing records: ${e.message ?: e::class.simpleName}")
                    )
            )
        }
    }
}

fun Server.listCalls() {
    val toolDescription =
        "This tool lists all function and method calls, which are held in the graph."

    this.addTool(name = "cpg_list_calls", description = toolDescription) { _ ->
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
            CallToolResult(content = result.calls.map { TextContent(it.toJson()) })
        } catch (e: Exception) {
            CallToolResult(
                content =
                    listOf(TextContent("Error listing calls: ${e.message ?: e::class.simpleName}"))
            )
        }
    }
}

fun Server.listCallsTo() {
    val toolDescription =
        """This tool lists all function and method calls to the method/function with the specified name, which are held in the graph.

        Parameters:
        - name: The local name of the function or method whose calls should be listed.
        """
            .trimIndent()

    val inputSchema =
        Tool.Input(
            properties =
                buildJsonObject {
                    putJsonObject("name") {
                        put("type", "string")
                        put(
                            "description",
                            "The local name of the function or method whose calls should be listed.",
                        )
                    }
                },
            required = listOf("name"),
        )

    this.addTool(
        name = "cpg_list_calls_to",
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
                Json.decodeFromString<CpgNamePayload>(Json.encodeToString(request.arguments))

            CallToolResult(content = result.calls(payload.name).map { TextContent(it.toJson()) })
        } catch (e: Exception) {
            CallToolResult(
                content =
                    listOf(TextContent("Error listing calls: ${e.message ?: e::class.simpleName}"))
            )
        }
    }
}

fun Server.getAllArgs() {
    val toolDescription =
        """This tool lists all arguments passed to the method/function call with the specified ID.

        Parameters:
        - nodeId: ID of the method/function call whose arguments should be listed.
        """
            .trimIndent()

    val inputSchema =
        Tool.Input(
            properties =
                buildJsonObject {
                    putJsonObject("nodeId") {
                        put("type", "string")
                        put(
                            "description",
                            "ID of the method/function call whose arguments should be listed.",
                        )
                    }
                },
            required = listOf("nodeId"),
        )

    this.addTool(
        name = "cpg_list_call_args",
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
                Json.decodeFromString<CpgIdPayload>(Json.encodeToString(request.arguments))

            CallToolResult(
                content =
                    result.calls
                        .single { it.id.toString() == payload.id }
                        .arguments
                        .map { TextContent(it.toJson()) }
            )
        } catch (e: Exception) {
            CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "Error listing call arguments: ${e.message ?: e::class.simpleName}"
                        )
                    )
            )
        }
    }
}

fun Server.getArgByIndexOrName() {
    val toolDescription =
        """This tool lists an argument passed to the method/function call with the specified ID either by name or by index.

        Parameters:
        - nodeId: ID of the method/function call whose arguments should be listed.
        - argName: Name of the argument to retrieve (optional).
        - index: Index of the argument to retrieve (optional). If both are provided, the name takes precedence. At least one of argName or index must be provided.
        """
            .trimIndent()

    val inputSchema =
        Tool.Input(
            properties =
                buildJsonObject {
                    putJsonObject("nodeId") {
                        put("type", "string")
                        put(
                            "description",
                            "ID of the method/function call whose arguments should be listed.",
                        )
                    }
                    putJsonObject("argName") {
                        put("type", "string")
                        put(
                            "description",
                            "The name of the argument (if arguments can be passed by name).",
                        )
                    }
                    putJsonObject("index") {
                        put("type", "integer")
                        put("description", "The index/position of the argument.")
                    }
                },
            required = listOf("nodeId"),
        )

    this.addTool(
        name = "cpg_list_call_arg_by_name_or_index",
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
                Json.decodeFromString<CpgCallArgumentByNameOrIndexPayload>(
                    Json.encodeToString(request.arguments)
                )

            CallToolResult(
                content =
                    listOf(
                        TextContent(
                            result.calls
                                .single { it.id.toString() == payload.id }
                                .argumentByNameOrPosition(
                                    name = payload.argumentName,
                                    position = payload.index,
                                )
                                ?.toJson()
                        )
                    )
            )
        } catch (e: Exception) {
            CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "Error listing call argument: ${e.message ?: e::class.simpleName}"
                        )
                    )
            )
        }
    }
}
