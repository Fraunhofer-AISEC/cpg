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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgNamesPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgSession
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.DEFAULT_LIST_LIMIT
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.addTool
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.paginate
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
        details, including code (unless includeCode is set to false), parameters, and signature.

        Names are matched exactly, not as a pattern: "read" finds every function whose local name
        is read, and a qualified name like "Stream::read" also finds "ns::Stream::read" (its last
        name parts match). To search by a regular expression instead, use cpg_list_functions'
        pattern.

        Prefer this over cpg_list_functions when the target function names are already known
        (e.g. from an input file), since cpg_list_functions dumps the entire codebase's function
        list, which can be very large and expensive for big libraries.
        Results come in pages of at most $DEFAULT_LIST_LIMIT functions; use the limit/offset
        parameters to page through more.

        Example prompts:
        - "Show me the functions named encrypt, decrypt and hash_password"
        """
            .trimIndent()

    this.addTool<CpgNamesPayload>(
        readOnly = true,
        name = "cpg_get_functions_by_name",
        description = toolDescription,
        handler = ::getFunctionsByName,
    )
}

/**
 * The page of functions matching [CpgNamesPayload.names] selected by its limit/offset (see
 * [paginate]), followed by the names nothing matched. That notice covers all names, not only the
 * page, since it describes the request as a whole.
 */
internal fun getFunctionsByName(session: CpgSession, payload: CpgNamesPayload): CallToolResult {
    val functionsByName: Map<String, List<Function>> =
        payload.names.associateWith { name ->
            session.translationResult.functions.filter { it.name.lastPartsMatch(name) }
        }

    val page = paginate(functionsByName.values.flatten(), payload.limit, payload.offset)
    val found =
        page.items.map {
            TextContent(Json.encodeToString(it.toInfo(includeCode = payload.includeCode)))
        }
    val notFound = functionsByName.filterValues { it.isEmpty() }.keys
    val notFoundNotice =
        if (notFound.isNotEmpty()) {
            TextContent("No function found for these names: ${notFound.joinToString(", ")}")
        } else {
            null
        }

    return CallToolResult(
        content = found + listOfNotNull(page.summary?.let { TextContent(it) }, notFoundNotice)
    )
}
