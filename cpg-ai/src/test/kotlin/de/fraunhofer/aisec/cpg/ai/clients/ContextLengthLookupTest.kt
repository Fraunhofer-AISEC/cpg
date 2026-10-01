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
package de.fraunhofer.aisec.cpg.ai.clients

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class ContextLengthLookupTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun withModelsServer(
        status: () -> Boolean,
        requests: AtomicInteger,
        body: (baseUrl: String) -> Unit,
    ) {
        val port = ServerSocket(0).use { it.localPort }
        val server =
            embeddedServer(ServerCIO, host = "127.0.0.1", port = port) {
                routing {
                    get("/v1/models") {
                        requests.incrementAndGet()
                        if (status()) {
                            call.respondText(
                                """{"data":[{"id":"m","max_model_len":4096}]}""",
                                ContentType.Application.Json,
                            )
                        } else {
                            call.respondText(
                                "unavailable",
                                status = io.ktor.http.HttpStatusCode.ServiceUnavailable,
                            )
                        }
                    }
                }
            }
        server.start(wait = false)
        try {
            body("http://127.0.0.1:$port")
        } finally {
            server.stop(0, 0)
        }
    }

    private fun config(baseUrl: String) =
        LlmProviderConfig(
            HttpClient(io.ktor.client.engine.cio.CIO) {
                install(ContentNegotiation) { json(json) }
            },
            listOf(
                ClientConfig(
                    name = "vLLM",
                    baseUrl = baseUrl,
                    apiKey = null,
                    provider = ClientProvider.OPENAI_COMPATIBLE,
                    requiresApiKey = false,
                )
            ),
        )

    @Test
    fun aDetectedContextLengthIsFetchedOncePerModel() {
        val requests = AtomicInteger()
        withModelsServer({ true }, requests) { baseUrl ->
            val providers = config(baseUrl)

            runBlocking {
                assertEquals(4096L, providers.contextLengthFor("vLLM", "m"))
                assertEquals(4096L, providers.contextLengthFor("vLLM", "m"))
            }

            assertEquals(1, requests.get())
        }
    }

    @Test
    fun aFailedLookupIsRetriedNextTime() {
        val requests = AtomicInteger()
        var healthy = false
        withModelsServer({ healthy }, requests) { baseUrl ->
            val providers = config(baseUrl)

            runBlocking {
                assertNull(providers.contextLengthFor("vLLM", "m"))
                healthy = true
                assertEquals(4096L, providers.contextLengthFor("vLLM", "m"))
            }

            assertEquals(2, requests.get())
        }
    }
}
