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
package de.fraunhofer.aisec.cpg.mcp.utils

import io.modelcontextprotocol.kotlin.sdk.ExperimentalMcpApi
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.testing.ChannelTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Set up MCP server/client for testing.
 *
 * [ChannelTransport] is the testing-only transport, which creates an in-process connection between
 * server and client.
 */
@OptIn(ExperimentalMcpApi::class)
fun withClient(
    registerTools: Server.() -> Unit = {},
    registerPrompts: Server.() -> Unit = {},
    test: suspend (Client) -> Unit,
) =
    // We need to set multiple threads here, same as ChannelTransport does internally.
    // If not doing so, client and server would share a single thread which leads to deadlock.
    runBlocking(Dispatchers.Default) {
        val server =
            Server(
                Implementation(name = "test-cpg-server", version = "1.0.0"),
                ServerOptions(
                    capabilities =
                        ServerCapabilities(
                            tools = ServerCapabilities.Tools(),
                            resources = ServerCapabilities.Resources(),
                            prompts = ServerCapabilities.Prompts(),
                        )
                ),
            )

        server.registerTools()
        server.registerPrompts()

        val (clientTransport, serverTransport) = ChannelTransport.createLinkedPair()
        val client = Client(Implementation(name = "test-client", version = "1.0.0"))

        listOf(
                launch { client.connect(clientTransport) },
                launch { server.createSession(serverTransport) },
            )
            .joinAll()

        try {
            withTimeout(30.seconds) { test(client) }
        } finally {
            client.close()
            server.close()
        }
    }
