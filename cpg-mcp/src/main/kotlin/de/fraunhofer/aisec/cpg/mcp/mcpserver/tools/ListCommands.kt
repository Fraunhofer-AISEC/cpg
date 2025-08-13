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

import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.records
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toJson
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server

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
                        TextContent("Error listing functions: ${e.message ?: e::class.simpleName}")
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
                    listOf(
                        TextContent("Error listing functions: ${e.message ?: e::class.simpleName}")
                    )
            )
        }
    }
}
