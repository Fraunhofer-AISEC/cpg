/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgNamesPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.addTool
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.toInfo
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.functions
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.Json

fun Server.getFunctionsByName() {
    val toolDescription =
        """
        This tool looks up a batch of functions by their (local) names and returns their full
        details, including code, parameters, and signature.

        Prefer this over cpg_list_functions when the target function names are already known
        (e.g. from an input file), since cpg_list_functions dumps the entire codebase's function
        list, which can be very large and expensive for big libraries.

        Example prompts:
        - "Show me the functions named encrypt, decrypt and hash_password"
        """
            .trimIndent()

    this.addTool<CpgNamesPayload>(
        name = "cpg_get_functions_by_name",
        description = toolDescription,
    ) { result: TranslationResult, payload: CpgNamesPayload ->
        val functionsByName: Map<String, List<Function>> =
            payload.names.associateWith { name ->
                result.functions.filter { it.name.lastPartsMatch(name) }
            }

        val found =
            functionsByName.values.flatten().map { TextContent(Json.encodeToString(it.toInfo())) }
        val notFound = functionsByName.filterValues { it.isEmpty() }.keys

        val notFoundNotice =
            if (notFound.isNotEmpty()) {
                listOf(
                    TextContent("No function found for these names: ${notFound.joinToString(", ")}")
                )
            } else {
                emptyList()
            }

        CallToolResult(content = found + notFoundNotice)
    }
}
