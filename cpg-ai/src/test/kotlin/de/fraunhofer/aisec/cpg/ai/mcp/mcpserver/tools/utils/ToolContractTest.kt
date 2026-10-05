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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.configureServer
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class ToolContractTest {

    private fun server() =
        Server(
            Implementation(name = "test", version = "1"),
            ServerOptions(ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = false))),
        )

    private fun ok(text: String = "ok") = CallToolResult(content = listOf(TextContent(text)))

    @Test
    fun everyDefaultToolFollowsTheConventions() {
        assertEquals(emptyList(), configureServer().toolRegistrationProblems())
    }

    @Test
    fun aToolRegisteredThroughTheSdkIsReported() {
        val server = server()
        server.addTool(name = "raw_without_arguments", description = "d") { _ -> ok() }
        server.addToolWithoutCpg<NoArguments>(
            name = "typed_without_arguments",
            description = "d",
        ) { _ ->
            ok()
        }

        assertEquals(
            listOf("raw_without_arguments: registered without addTool/addToolWithoutCpg"),
            server.toolRegistrationProblems(),
        )
    }

    @Test
    fun aToolThatIgnoresWronglyTypedArgumentsIsReported() {
        val server = server()
        val schema =
            ToolSchema(
                properties = buildJsonObject { putJsonObject("id") { put("type", "string") } },
                required = listOf("id"),
            )
        server.addTool(
            name = "swallows_arguments",
            description = "d",
            inputSchema = schema,
            meta = withTypedArgumentsMarker(null), // pretends to be typed, to isolate the 2nd check
        ) { _ ->
            ok("done")
        }

        assertEquals(
            listOf(
                "swallows_arguments: wrongly typed arguments were not reported, the tool answered: done"
            ),
            server.toolRegistrationProblems(),
        )
    }
}
