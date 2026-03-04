/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import de.fraunhofer.aisec.cpg.mcp.mcpserver.configureServer
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

class Application : CliktCommand(name = "cpg-mcp") {
    private val ssePort by
        option("--sse", help = "Provide the port to run SSE (Server Sent Events).").int()

    private val httpPort by
        option("--http", help = "Provide the port to run streamable HTTP.").int()

    override fun run() {
        val http = httpPort
        val sse = ssePort
        if (http != null) {
            println("Starting MCP server in streamable HTTP mode on port $http...")
            runHttpMcpServerUsingKtorPlugin(port = http, server = configureServer(), wait = true)
        } else if (sse != null) {
            println("Starting MCP server in SSE mode on port $sse...")
            runSseMcpServerUsingKtorPlugin(sse, configureServer(), wait = true)
        } else {
            println("Starting MCP server in stdio mode...")
            runMcpServerUsingStdio()
        }
    }
}

fun main(args: Array<String>) {
    Application().main(args)
}

fun runMcpServerUsingStdio() {
    val server = configureServer()
    val transport =
        StdioServerTransport(System.`in`.asSource().buffered(), System.out.asSink().buffered())
    runBlocking {
        val job = Job()
        server.onClose { job.complete() }
        server.createSession(transport)
        job.join()
    }
}

/**
 * Starts an SSE (Server Sent Events) MCP server using the Ktor framework and the specified port.
 *
 * The url can be accessed in the MCP inspector at [http://localhost:$port]
 *
 * @param port The port number on which the SSE MCP server will listen for client connections.
 * @param wait If true the thread is blocked until the server stops. This flag is needed when the
 *   server runs in the background alongside another server (e.g. in codyze-console).
 * @param host The host/IP address on which the server will bind.
 * @param server The MCP server instance that will handle incoming requests and provide responses to
 *   clients.
 */
fun runSseMcpServerUsingKtorPlugin(
    port: Int,
    server: Server,
    wait: Boolean = false,
    host: String = "0.0.0.0",
) {
    embeddedServer(CIO, host = host, port = port) { mcp { server } }.start(wait = wait)
}

/**
 * Starts a streamable HTTP MCP server using the Ktor framework and the specified port.
 *
 * @param port The port number on which the HTTP MCP server will listen for client connections.
 * @param host The host/IP address on which the server will bind.
 * @param server The MCP server instance that will handle incoming requests and provide responses to
 *   clients.
 */
fun runHttpMcpServerUsingKtorPlugin(
    port: Int,
    host: String = "0.0.0.0",
    server: Server,
    wait: Boolean = false,
) {
    embeddedServer(factory = CIO, host = host, port = port) { mcpStreamableHttp { server } }
        .start(wait = wait)
}
