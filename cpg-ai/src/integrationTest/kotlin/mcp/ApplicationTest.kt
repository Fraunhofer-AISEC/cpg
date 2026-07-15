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
package de.fraunhofer.aisec.cpg.mcp

import de.fraunhofer.aisec.cpg.mcp.mcpserver.configureServer
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class ApplicationTest {

    private fun findFreePort(): Int = ServerSocket(0).use { it.localPort }

    @Test
    fun sseServerStartsAndAcceptsConnectionTest() {
        val port = findFreePort()
        val server = configureServer()
        val engine =
            runSseMcpServerUsingKtorPlugin(port = port, server = server, host = "127.0.0.1")

        runBlocking {
            try {
                withTimeout(10.seconds) {
                    val httpClient = HttpClient(CIO) { install(SSE) }
                    val client = Client(Implementation(name = "test-client", version = "1.0.0"))
                    val transport = SseClientTransport(httpClient, "http://127.0.0.1:$port")
                    client.connect(transport)

                    val tools = client.listTools()
                    assertTrue(tools.tools.isNotEmpty(), "Server should expose MCP tools via SSE")

                    client.close()
                    httpClient.close()
                }
            } finally {
                engine.stop(gracePeriodMillis = 500, timeoutMillis = 1000)
                server.close()
            }
        }
    }

    @Test
    fun httpServerStartsAndAcceptsConnectionTest() {
        val port = findFreePort()
        val server = configureServer()
        val engine =
            runHttpMcpServerUsingKtorPlugin(port = port, server = server, host = "127.0.0.1")

        runBlocking {
            try {
                withTimeout(10.seconds) {
                    val httpClient =
                        HttpClient(CIO) {
                            install(SSE)
                            install(ContentNegotiation) { json() }
                        }
                    val client = Client(Implementation(name = "test-client", version = "1.0.0"))
                    val transport =
                        StreamableHttpClientTransport(httpClient, "http://127.0.0.1:$port/mcp")
                    client.connect(transport)

                    val tools = client.listTools()
                    assertTrue(
                        tools.tools.isNotEmpty(),
                        "Server should expose MCP tools via streamable HTTP",
                    )

                    client.close()
                    httpClient.close()
                }
            } finally {
                engine.stop(gracePeriodMillis = 500, timeoutMillis = 1000)
                server.close()
            }
        }
    }
}
