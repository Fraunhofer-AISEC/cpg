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

import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

fun FunctionDeclaration.toJson(): String {
    val functionInfo =
        FunctionInfo(
            nodeId = this.id.toHexString(),
            name = this.name.toString(),
            parameters =
                this.parameters.map {
                    ParameterInfo(
                        it.name.toString(),
                        it.type.name.toString(),
                        it.default.toString(),
                    )
                },
            signature = this.signature,
            fileName = this.location?.artifactLocation?.fileName,
            startLine = this.location?.region?.startLine,
            endLine = this.location?.region?.endLine,
            startColumn = this.location?.region?.startColumn,
            endColumn = this.location?.region?.endColumn,
        )
    return Json.encodeToString(functionInfo)
}

@Serializable
data class ParameterInfo(val name: String, val type: String, val defaultValue: String? = null)

@Serializable
data class FunctionInfo(
    val nodeId: String,
    val name: String,
    val parameters: List<ParameterInfo>,
    val signature: String,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
    val startColumn: Int?,
    val endColumn: Int?,
)
