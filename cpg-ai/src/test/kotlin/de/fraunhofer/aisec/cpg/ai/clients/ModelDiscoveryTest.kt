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

import ai.koog.prompt.llm.LLMProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/** Which providers and models [LlmProviderConfig] reports, and how it resolves a client's model. */
class ModelDiscoveryTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** One local server standing in for an OpenAI, a vLLM, a Gemini and a broken endpoint. */
    private fun withServer(body: (baseUrl: String) -> Unit) {
        val port = ServerSocket(0).use { it.localPort }
        val server =
            embeddedServer(ServerCIO, host = "127.0.0.1", port = port) {
                routing {
                    get("/openai/v1/models") {
                        call.respondText(
                            """{"data":[{"id":"gpt-5"},{"id":"gpt-5-mini"},
                               {"id":"gpt-5-2025-08-07"},{"id":"gpt-4o"},{"id":"dall-e-3"}]}""",
                            ContentType.Application.Json,
                        )
                    }
                    get("/vllm/v1/models") {
                        call.respondText(
                            """{"data":[{"id":"qwen","max_model_len":262144},{"id":"glm"}]}""",
                            ContentType.Application.Json,
                        )
                    }
                    get("/gemini/models") {
                        call.respondText(
                            """{"models":[{"name":"models/gemini-2.5-flash"},
                               {"name":"models/gemini-3-pro-preview"},
                               {"name":"models/gemini-1.5-pro"},
                               {"name":"models/text-embedding-004"}]}""",
                            ContentType.Application.Json,
                        )
                    }
                    get("/broken/v1/models") {
                        call.respondText("down", status = HttpStatusCode.ServiceUnavailable)
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

    private fun providers(vararg clients: ClientConfig) =
        LlmProviderConfig(
            HttpClient(io.ktor.client.engine.cio.CIO) {
                install(ContentNegotiation) { json(json) }
                install(HttpTimeout)
            },
            clients.toList(),
        )

    private fun openAiCompatible(
        name: String,
        baseUrl: String,
        apiKey: String? = null,
        requiresApiKey: Boolean = false,
        contextLength: Long? = null,
    ) =
        ClientConfig(
            name = name,
            baseUrl = baseUrl,
            apiKey = apiKey,
            provider = ClientProvider.OPENAI_COMPATIBLE,
            requiresApiKey = requiresApiKey,
            contextLengthOverride = contextLength,
        )

    private fun gemini(baseUrl: String, apiKey: String?) =
        ClientConfig(
            name = "gemini",
            baseUrl = baseUrl,
            apiKey = apiKey,
            provider = ClientProvider.GEMINI,
            requiresApiKey = true,
        )

    @Test
    fun eachProviderReportsItsChatModels() = withServer { base ->
        val config =
            providers(
                openAiCompatible("openai", "$base/openai", apiKey = "k", requiresApiKey = true),
                openAiCompatible("vLLM", "$base/vllm"),
                gemini("$base/gemini", apiKey = "k"),
            )

        val found =
            runBlocking { config.listAvailableProviders() }.associate { it.name to it.models }

        assertEquals(
            mapOf(
                // The official OpenAI API: GPT-5 aliases only, no dated snapshots.
                "openai" to listOf("gpt-5", "gpt-5-mini"),
                // A local server: everything it has loaded.
                "vLLM" to listOf("glm", "qwen"),
                // Gemini: current chat models only, without the "models/" prefix.
                "gemini" to listOf("gemini-2.5-flash", "gemini-3-pro-preview"),
            ),
            found,
        )
        config.close()
    }

    @Test
    fun providersWithoutKeyOrModelsAreLeftOut() = withServer { base ->
        val config =
            providers(
                openAiCompatible("needs-key", "$base/vllm", apiKey = null, requiresApiKey = true),
                openAiCompatible("broken", "$base/broken"),
                openAiCompatible("unreachable", "http://127.0.0.1:9"),
                openAiCompatible("vLLM", "$base/vllm"),
            )

        val found = runBlocking { config.listAvailableProviders() }.map { it.name }

        assertEquals(listOf("vLLM"), found)
        config.close()
    }

    @Test
    fun aConfiguredContextLengthWinsOverTheServersValue() = withServer { base ->
        val config =
            providers(
                openAiCompatible("configured", "$base/vllm", contextLength = 32_768),
                openAiCompatible("detected", "$base/vllm"),
            )

        runBlocking {
            assertEquals(32_768L, config.clientFor("configured", "qwen")?.model?.contextLength)
            assertEquals(262_144L, config.clientFor("detected", "qwen")?.model?.contextLength)
            // Not reported by the server, no override: the conservative default.
            assertEquals(128_000L, config.clientFor("detected", "glm")?.model?.contextLength)
        }
        config.close()
    }

    @Test
    fun aGeminiClientNeedsItsApiKey() = withServer { base ->
        val withKey = providers(gemini("$base/gemini", apiKey = "k"))
        val withoutKey = providers(gemini("$base/gemini", apiKey = null))

        runBlocking {
            val llm = assertNotNull(withKey.clientFor("gemini", "gemini-2.5-flash"))
            assertEquals(LLMProvider.Google, llm.model.provider)
            assertNull(withoutKey.clientFor("gemini", "gemini-2.5-flash"))
        }
        withKey.close()
        withoutKey.close()
    }
}
