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

import de.fraunhofer.aisec.codyze.console.ai.ChatService
import de.fraunhofer.aisec.codyze.console.ai.McpServerHelper
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
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("de.fraunhofer.aisec.codyze.console.Main")

/**
 * This function starts the embedded server for the web console. It uses the Netty engine and
 * listens on [host] (default: localhost) at [port] (default: 8080). The server is configured using
 * the [configureWebconsole] function.
 */
fun ConsoleService.startConsole(host: String = "localhost", port: Int = 8080) {
    val chatService: ChatService? =
        if (McpServerHelper.isEnabled) {
            runBlocking { initChatService() }
        } else {
            log.info("MCP module not enabled, AI chat features will be disabled")
            null
        }
    embeddedServer(Netty, host = host, port = port) {
            configureWebconsole(this@startConsole, chatService)
        }
        .start(wait = true)
}

private suspend fun ConsoleService.initChatService(): ChatService? {
    val chatService = ChatService.createIfConfigExist() ?: return null

    McpServerHelper.startMcpServer(8081)

    val translationResult = getTranslationResult()?.analysisResult?.translationResult
    if (translationResult != null) {
        McpServerHelper.setGlobalAnalysisResult(translationResult)
    }
    chatService.connect()
    log.info("MCP client connected")
    return chatService
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
    chatService: ChatService? = null,
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
fun Application.configureRouting(service: ConsoleService, chatService: ChatService? = null) {
    routing {
        apiRoutes(service)
        // If the cpg-mcp module is enabled, chatService won't be null, so the endpoints will be
        // reachable
        if (chatService != null) {
            chatRoutes(chatService)
        }
        frontendRoutes()
    }
}
