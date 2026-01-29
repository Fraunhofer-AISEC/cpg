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
package de.fraunhofer.aisec.codyze.console.ai

import com.typesafe.config.ConfigFactory
import de.fraunhofer.aisec.codyze.console.ai.clients.GeminiClient
import de.fraunhofer.aisec.codyze.console.ai.clients.LlmClient
import de.fraunhofer.aisec.codyze.console.ai.clients.OpenAiClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class ChatMessageJSON(val role: String, val content: String)

@Serializable data class ChatRequestJSON(val messages: List<ChatMessageJSON>)

/** ChatService manages LLM client configuration and provides an API for chat interactions. */
class ChatService {
    private val config = ConfigFactory.load()
    private val llmProvider: String = config.getString("llm.client")

    private val llmModel: String = config.getString("llm.$llmProvider.model")

    private val llmBaseUrl: String = config.getString("llm.$llmProvider.baseUrl")

    private val httpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                )
            }
            install(SSE)
            install(HttpTimeout) {
                requestTimeoutMillis = 600_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 600_000
            }
        }

    private val llmClient: LlmClient =
        when (llmProvider) {
            "gemini" -> {
                val apiKey =
                    System.getenv("GOOGLE_API_KEY")
                        ?: throw IllegalStateException("GOOGLE_API_KEY not set")
                GeminiClient(httpClient, llmModel, apiKey, llmBaseUrl)
            }
            else -> OpenAiClient(httpClient, llmModel, llmBaseUrl)
        }

    private val chatClient: ChatClient =
        ChatClient(
            httpClient = httpClient,
            llm = llmClient,
            mcpServerUrl = config.getString("mcp.serverUrl"),
        )

    suspend fun connect() {
        chatClient.connect()
    }

    fun chat(request: ChatRequestJSON): Flow<String> {
        return chatClient.chat(request)
    }

    fun close() {
        httpClient.close()
    }
}
