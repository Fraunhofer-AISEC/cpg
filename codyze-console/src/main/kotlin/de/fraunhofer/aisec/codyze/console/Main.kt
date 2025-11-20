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

import de.fraunhofer.aisec.cpg.mcp.mcpserver.configureServer
import de.fraunhofer.aisec.cpg.mcp.runSseMcpServerUsingKtorPlugin
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * This function starts the embedded server for the web console. It uses the Netty engine and
 * listens on [host] (default: localhost) at [port] (default: 8080). The server is configured using
 * the [configureWebconsole] function.
 */
fun ConsoleService.startConsole(
    host: String = "localhost",
    port: Int = 8080,
    chatService: ChatService = ChatService(),
) {
    // TODO(): MCP server should only run when cpg-mcp module enabled
    runBlocking {
        // Start MCP server in background (wait = false means it won't block)
        println("Starting MCP server on port 8081...")
        runSseMcpServerUsingKtorPlugin(8081, configureServer())

        // Start main server (also with wait = false to avoid blocking)
        println("Starting main server on port 8080...")
        val mainServer =
            embeddedServer(Netty, host = host, port = port) {
                configureWebconsole(this@startConsole, chatService)
            }
        mainServer.start(wait = true)
    }
}

/**
 * This function takes care of configuring the web console based on the [service]. It sets up the
 * CORS policy, content negotiation, and routing.
 *
 * Note: Currently, the CORS policy allows any host. This should be restricted to specific hosts in
 * a production environment and will be made available as an option later.
 */
fun Application.configureWebconsole(
    service: ConsoleService = ConsoleService(),
    chatService: ChatService = ChatService(),
) {
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
            }
        )
    }

    configureRouting(service, chatService)
}

/**
 * This function sets up the routing for the web console. It defines the API routes and static
 * resources (for serving the single-page application frontend).
 */
fun Application.configureRouting(
    service: ConsoleService,
    chatService: ChatService = ChatService(),
) {
    routing {
        // We'll add routes here
        apiRoutes(service, chatService)
        frontendRoutes()
    }
}
