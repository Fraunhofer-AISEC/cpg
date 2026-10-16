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
package de.fraunhofer.aisec.cpg.ai.mcp

import io.ktor.server.engine.EmbeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class BindAddressTest {

    private fun newServer() =
        Server(Implementation(name = "test", version = "1"), ServerOptions(ServerCapabilities()))

    private fun freePort() = ServerSocket(0).use { it.localPort }

    private fun boundHosts(server: EmbeddedServer<*, *>) = runBlocking {
        server.engine.resolvedConnectors().map { it.host }
    }

    @Test
    fun defaultHostIsLoopback() {
        assertEquals("127.0.0.1", DEFAULT_MCP_HOST)
    }

    @Test
    fun httpServerBindsToLoopbackUnlessToldOtherwise() {
        val server = runHttpMcpServerUsingKtorPlugin(port = freePort(), server = newServer())
        try {
            assertEquals(listOf("127.0.0.1"), boundHosts(server))
        } finally {
            server.stop(0, 0)
        }
    }

    @Test
    fun sseServerBindsToLoopbackUnlessToldOtherwise() {
        val server = runSseMcpServerUsingKtorPlugin(port = freePort(), server = newServer())
        try {
            assertEquals(listOf("127.0.0.1"), boundHosts(server))
        } finally {
            server.stop(0, 0)
        }
    }
}
