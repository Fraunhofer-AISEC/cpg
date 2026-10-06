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

import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.io.File
import java.lang.reflect.Proxy
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.io.TempDir

/**
 * Bad arguments must come back as the model-readable problem list, from every registration path.
 */
class InvalidArgumentsToolTest {

    /** The handlers never touch the connection; any call on it is a test bug. */
    private val connection: ClientConnection =
        Proxy.newProxyInstance(javaClass.classLoader, arrayOf(ClientConnection::class.java)) {
            _,
            method,
            _ ->
            error("unexpected ClientConnection.${method.name}")
        } as? ClientConnection ?: error("proxy does not implement ClientConnection")

    private fun server() =
        Server(
            Implementation(name = "test", version = "1"),
            ServerOptions(ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = false))),
        )

    private fun call(server: Server, tool: String, arguments: String): CallToolResult {
        val registered = server.tools[tool] ?: error("$tool is not registered")
        val request =
            CallToolRequest(
                CallToolRequestParams(
                    name = tool,
                    arguments = Json.parseToJsonElement(arguments).jsonObject,
                )
            )
        return runBlocking { registered.handler(connection, request) }
    }

    private fun CallToolResult.text() =
        content.filterIsInstance<TextContent>().joinToString("\n") { it.text.orEmpty() }

    @Test
    fun addOrUpdateConceptReturnsTheProblemListInsteadOfFailing(@TempDir dir: Path) {
        val server = server()
        val file = File(dir.toFile(), "concepts.yaml")
        server.addOrUpdateConcept(file)

        val result =
            call(
                server,
                "cpg_add_or_update_llm_concept",
                """{"name": "C", "properties": "[]", "operations": []}""",
            )

        val text = result.text()
        assertNotEquals(true, result.isError, text)
        assertTrue(
            text.startsWith("Invalid arguments for cpg_add_or_update_llm_concept: 2 problem(s)"),
            text,
        )
        assertTrue("- (arguments): missing required field \"description\" (string)" in text, text)
        assertTrue("- properties: expected array of objects, got a string: \"[]\"" in text, text)
        assertFalse(file.exists(), "nothing may be persisted for invalid arguments")
    }

    @Test
    fun typedToolsReturnTheProblemListToo(@TempDir dir: Path) {
        val server = server()
        server.addLLMConceptAndOperations(File(dir.toFile(), "concepts.yaml"))

        val text =
            call(
                    server,
                    "cpg_add_llm_concept_and_operations",
                    """{"concepts": [{"name": "C", "nodeId": "n", "properties": [{"name": "p"}], "operations": []}]}""",
                )
                .text()

        assertTrue(
            text.startsWith(
                "Invalid arguments for cpg_add_llm_concept_and_operations: 4 problem(s)"
            ),
            text,
        )
        assertTrue(
            "- concepts[0].properties[0]: missing required field \"value\" (string)" in text,
            text,
        )
        assertTrue("Expected shape:" in text, text)
    }
}
