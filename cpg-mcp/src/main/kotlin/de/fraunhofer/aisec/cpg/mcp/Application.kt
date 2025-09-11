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
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

fun main(args: Array<String>) {
    McpServer().main(args)
}

class McpServer : CliktCommand(name = "mcp-server") {
    private val ssePort by
        option(
                "--sse",
                help =
                    "Port to run SSE (Server Sent Events). If nothing provided, it will use stdio.",
            )
            .int()

    override fun run() {
        val port = ssePort
        if (port != null) {
            println("Starting MCP server in SSE mode on port $port...")
            runSseMcpServerUsingKtorPlugin(port, configureServer())
        } else {
            println("Starting MCP server in stdio mode...")
            runMcpServerUsingStdio()
        }
    }
}

fun runMcpServerUsingStdio() {
    val server = configureServer()
    val transport =
        StdioServerTransport(System.`in`.asSource().buffered(), System.out.asSink().buffered())
    runBlocking {
        val job = Job()
        server.onClose { job.complete() }
        server.connect(transport)
        job.join()
    }
}

/**
 * Starts an SSE (Server Sent Events) MCP server using the Ktor framework and the specified port.
 *
 * The url can be accessed in the MCP inspector at [http://localhost:$port]
 *
 * @param port The port number on which the SSE MCP server will listen for client connections.
 */
fun runSseMcpServerUsingKtorPlugin(port: Int, server: Server) = runBlocking {
    embeddedServer(CIO, host = "0.0.0.0", port = port) { mcp { server } }.start(wait = true)
}
