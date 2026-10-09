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
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgListCallsToPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.addTool
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.findNodeById
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.toJson
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.invoke
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.util.regex.PatternSyntaxException
import kotlinx.serialization.json.Json

fun Server.listFunctions() {
    val toolDescription =
        """
        This tool lists all functions, more precisely function declarations, which are held in the
        graph, as compact summaries (name, parameters, signature, callees, location) without source
        code. Use cpg_get_node with a id to retrieve the full node details (including its code) for a
        specific function once picked.
        Results come in pages of at most $DEFAULT_LIST_LIMIT items. Narrow large listings down with pattern, file or calls
        instead of paging through everything; use the limit/offset parameters to page through the rest.

        Example prompts:
        - "Show me all functions in the analyzed code"
        - "Which functions call memcpy?"
        - "Which functions are defined in main.c?"
        """
            .trimIndent()

    this.addTool<CpgListFunctionsPayload>(
        name = "cpg_list_functions",
        description = toolDescription,
    ) { result: TranslationResult, payload: CpgListFunctionsPayload ->
        val pattern = compilePattern(payload.pattern)
        val matching =
            result.functions.filter { function ->
                function.matches(pattern, payload.file) &&
                    (payload.calls == null ||
                        function.calls.any { it.name.localName == payload.calls })
            }
        matching.toPagedResult(payload.limit, payload.offset) {
            Json.encodeToString(it.toInfo(includeCode = false))
        }
    }
}

fun Server.listRecords() {
    val toolDescription =
        """
        This tool lists all classes and structs, more precisely their declarations as compact summaries.
        Use cpg_get_node with a id to retrieve the full node details.
        Results come in pages of at most $DEFAULT_LIST_LIMIT items. Narrow large listings down with pattern or file instead of
        paging through everything; use the limit/offset parameters to page through the rest.

        Example prompts:
        - "Show me all classes in the code"
        - "What data structures are defined here?"
        """
            .trimIndent()

    this.addTool<CpgListNodesPayload>(name = "cpg_list_records", description = toolDescription) {
        result: TranslationResult,
        payload: CpgListNodesPayload ->
        val pattern = compilePattern(payload.pattern)
        val matching = result.records.filter { it.matches(pattern, payload.file) }
        matching.toPagedResult(payload.limit, payload.offset) { Json.encodeToString(it.toInfo()) }
    }
}

fun Server.listConceptsAndOperations() {
    val toolDescription =
        "This tool lists all concepts (a special node marking 'what something IS') and operations (a special node marking 'what something DOES') which have been used as overlays to some nodes in the graph. " +
            "Results are capped at $DEFAULT_LIST_LIMIT items by default; use the limit/offset parameters to paginate through more."

    this.addTool<CpgListPayload>(
        name = "cpg_list_concepts_and_operations",
        description = toolDescription,
    ) { result: TranslationResult, payload: CpgListPayload ->
        val overlays: List<OverlayNode> =
            result.allChildrenWithOverlays<Concept>() + result.allChildrenWithOverlays<Operation>()
        overlays.toPagedResult(payload.limit, payload.offset) { it.toJson() }
    }
}

fun Server.listCalls() {
    val toolDescription =
        """
        This tool lists all function and method calls as compact summaries.
        Use cpg_get_node with a id to retrieve the full node details.
        Results come in pages of at most $DEFAULT_LIST_LIMIT items. Narrow large listings down with pattern (matched against
        the name of the called function) or file instead of paging through everything; use the
        limit/offset parameters to page through the rest.

        Example prompts:
        - "Show me all function calls in the code"
        - "What functions are being called?"
        """
            .trimIndent()

    this.addTool<CpgListNodesPayload>(name = "cpg_list_calls", description = toolDescription) {
        result: TranslationResult,
        payload: CpgListNodesPayload ->
        val pattern = compilePattern(payload.pattern)
        val matching = result.calls.filter { it.matches(pattern, payload.file) }
        matching.toPagedResult(payload.limit, payload.offset) {
            Json.encodeToString(it.toInfo(includeCode = false))
        }
    }
}

fun Server.listCallsTo() {
    val toolDescription =
        """
        This tool lists all function and method calls to the method/function with the specified name, which are held in the graph.
        Results omit source code to keep this listing compact - use cpg_get_node with a id to retrieve
        the full node details (including its code) for a specific call once picked.
        Results are capped at $DEFAULT_LIST_LIMIT items by default; use the limit/offset parameters to paginate through more.

        Example prompts:
        - "Show me all calls to the function 'encrypt'"
        - "Where is the 'authenticate' function called?"
        """
            .trimIndent()

    this.addTool<CpgListCallsToPayload>(
        name = "cpg_list_calls_to",
        description = toolDescription,
    ) { result: TranslationResult, payload: CpgListCallsToPayload ->
        listCallsTo(result, payload)
    }
}

internal fun listCallsTo(
    result: TranslationResult,
    payload: CpgListCallsToPayload,
): CallToolResult {
    return result.calls(payload.name).toPagedResult(payload.limit, payload.offset) {
        Json.encodeToString(it.toInfo(includeCode = false))
    }
}

/** [pattern] as a case-insensitive regex, compiled once for a whole listing. */
private fun compilePattern(pattern: String?): Regex? =
    pattern?.let {
        try {
            Regex(it, RegexOption.IGNORE_CASE)
        } catch (e: PatternSyntaxException) {
            throw IllegalArgumentException(
                "pattern is not a valid regular expression: ${e.description}"
            )
        }
    }

/** Whether this node is in a file whose path contains [file]. */
private fun Node.isInFile(file: String): Boolean =
    location?.artifactLocation?.uri?.toString()?.contains(file) == true

/** Whether this node's name matches [pattern] and its file [file], where given. */
private fun Node.matches(pattern: Regex?, file: String?): Boolean =
    (pattern == null || pattern.containsMatchIn(name.toString())) &&
        (file == null || isInFile(file))

fun Server.getAllArgs() {
    val toolDescription =
        """This tool lists all arguments passed to the method/function call with the specified ID."""
            .trimIndent()

    this.addTool<CpgIdPayload>(name = "cpg_list_call_args", description = toolDescription) {
        result: TranslationResult,
        payload: CpgIdPayload ->
        listCallArgs(result, payload)
    }
}

internal fun listCallArgs(result: TranslationResult, payload: CpgIdPayload): CallToolResult {
    val call =
        result.findNodeById(payload.id) as? Call
            ?: return CallToolResult(
                content = listOf(TextContent("No call found with id ${payload.id}."))
            )
    return CallToolResult(content = call.arguments.map { TextContent(it.toJson()) })
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
        listCallArgByNameOrIndex(result, payload)
    }
}

internal fun listCallArgByNameOrIndex(
    result: TranslationResult,
    payload: CpgCallArgumentByNameOrIndexPayload,
): CallToolResult {
    val call =
        result.findNodeById(payload.nodeId) as? Call
            ?: return CallToolResult(
                content = listOf(TextContent("No call found with id ${payload.nodeId}."))
            )
    val argument =
        call.argumentByNameOrPosition(name = payload.argumentName, position = payload.index)
    return CallToolResult(
        content =
            listOf(
                TextContent(argument?.toJson() ?: "No argument found with the given name or index.")
            )
    )
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
        val node = result.findNodeById(payload.id)
        if (node != null) {
            CallToolResult(content = listOf(TextContent(node.toJson())))
        } else {
            CallToolResult(content = listOf(TextContent("No node found with ${payload.id}")))
        }
    }
}
