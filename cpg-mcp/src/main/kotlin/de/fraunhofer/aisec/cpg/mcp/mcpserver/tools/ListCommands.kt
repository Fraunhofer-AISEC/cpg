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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.CpgCallArgumentByNameOrIndexPayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.CpgIdPayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.CpgNamePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.runOnCpg
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.toJson
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.toObject
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

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
            CallToolResult(content = result.functions.map { TextContent(it.toJson()) })
        }
    }
}

fun Server.functions() {
    val uri = "cpg://functions"
    this.addResource(uri, "functions", "List of all function declarations in the CPG") {
        request: ReadResourceRequest ->
        request.runOnCpg(uri) { result: TranslationResult, _ ->
            ReadResourceResult(
                contents =
                    result.functions.map {
                        TextResourceContents(it.toJson(), "cpg://function/${it.id}")
                    }
            )
        }
    }
}

/*fun Server.functionById() {
    val uri = "cpg://function/{id}"
    ResourceTemplate(
        uriTemplate = uri,
        name = "functionById",
        description = "Get function declaration by ID and show the function body",
        mimeType = "application/json",
    )
}*/

fun Server.listRecords() {
    val toolDescription =
        """
        This tool lists all classes and structs, more precisely their declarations, which are held in the graph.
        
        Example prompts:
        - "Show me all classes in the code"
        - "What data structures are defined here?"
        """
            .trimIndent()

    this.addTool(name = "cpg_list_records", description = toolDescription) { request ->
        request.runOnCpg { result: TranslationResult, _ ->
            CallToolResult(content = result.records.map { TextContent(it.toJson()) })
        }
    }
}

fun Server.listConceptsAndOperations() {
    val toolDescription =
        "This tool lists all concepts (a special node marking 'what something IS') and operations (a special node marking 'what something DOES') which have been used as overlays to some nodes in the graph."

    this.addTool(name = "cpg_list_concepts_and_operations", description = toolDescription) { request
        ->
        request.runOnCpg { result: TranslationResult, _: CallToolRequest ->
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
        This tool lists all function and method calls, which are held in the graph.
        
        Example prompts:
        - "Show me all function calls in the code"
        - "What functions are being called?"
        """
            .trimIndent()

    this.addTool(name = "cpg_list_calls", description = toolDescription) { request ->
        request.runOnCpg { result: TranslationResult, _ ->
            CallToolResult(content = result.calls.map { TextContent(it.toJson()) })
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

        Parameters:
        - name: The local name of the function or method whose calls should be listed.
        """
            .trimIndent()

    val inputSchema =
        ToolSchema(
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
        request.runOnCpg { result: TranslationResult, request: CallToolRequest ->
            val payload =
                request.arguments?.toObject<CpgNamePayload>()
                    ?: return@runOnCpg CallToolResult(
                        content =
                            listOf(
                                TextContent(
                                    "Invalid or missing payload for cpg_list_calls_to tool."
                                )
                            )
                    )

            CallToolResult(content = result.calls(payload.name).map { TextContent(it.toJson()) })
        }
    }
}

fun Server.getAllArgs() {
    val toolDescription =
        """This tool lists all arguments passed to the method/function call with the specified ID.

        Parameters:
        - id: ID of the method/function call whose arguments should be listed.
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
                            "ID of the method/function call whose arguments should be listed.",
                        )
                    }
                },
            required = listOf("id"),
        )

    this.addTool(
        name = "cpg_list_call_args",
        description = toolDescription,
        inputSchema = inputSchema,
    ) { request ->
        request.runOnCpg { result: TranslationResult, request: CallToolRequest ->
            val payload =
                request.arguments?.toObject<CpgIdPayload>()
                    ?: return@runOnCpg CallToolResult(
                        content =
                            listOf(
                                TextContent(
                                    "Invalid or missing payload for cpg_list_call_args tool."
                                )
                            )
                    )

            CallToolResult(
                content =
                    result.calls
                        .single { it.id.toString() == payload.id }
                        .arguments
                        .map { TextContent(it.toJson()) }
            )
        }
    }
}

fun Server.getArgByIndexOrName() {
    val toolDescription =
        """This tool lists an argument passed to the method/function call with the specified ID either by name or by index.

        Parameters:
        - id: ID of the method/function call whose arguments should be listed.
        - argName: Name of the argument to retrieve (optional).
        - index: Index of the argument to retrieve (optional). The first argument is at index 0. We do not support the base/receiver of a method call here.
        
        If both argName and index are provided, the name takes precedence. At least one of argName or index must be provided.
        """
            .trimIndent()

    val inputSchema =
        ToolSchema(
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
                        put(
                            "description",
                            "The index/position of the argument. The first argument is at index 0. We do not support the base/receiver of a method call here.",
                        )
                    }
                },
            required = listOf("nodeId"),
        )

    this.addTool(
        name = "cpg_list_call_arg_by_name_or_index",
        description = toolDescription,
        inputSchema = inputSchema,
    ) { request ->
        request.runOnCpg { result: TranslationResult, request: CallToolRequest ->
            val payload =
                request.arguments?.toObject<CpgCallArgumentByNameOrIndexPayload>()
                    ?: return@runOnCpg CallToolResult(
                        content =
                            listOf(
                                TextContent(
                                    "Invalid or missing payload for cpg_list_call_arg_by_name_or_index tool."
                                )
                            )
                    )

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
}
