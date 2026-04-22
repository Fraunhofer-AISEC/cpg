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
package de.fraunhofer.aisec.codyze.console.ai.clients

import com.typesafe.config.Config
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("LlmProviderCatalog")

/** Catalog of LLM providers configured in `llm.clients.*` */
class LlmProviderCatalog(private val httpClient: HttpClient, val clients: List<ClientConfig>) {

    /**
     * Resolves the [ClientProvider] name with the chosen model to a [LlmClient]. Returns `null` if
     * the provider is unknown, or if a required API key is missing.
     */
    fun clientFor(clientName: String, model: String): LlmClient? {
        val config = clients.firstOrNull { it.name == clientName } ?: return null

        return when (config.provider) {
            ClientProvider.GEMINI -> {
                val apiKey = config.apiKey ?: return null
                GeminiClient(httpClient, model, apiKey, config.baseUrl)
            }

            ClientProvider.OPENAI_COMPATIBLE ->
                OpenAiClient(httpClient, model, config.baseUrl, config.apiKey)
        }
    }

    /**
     * Returns all configured providers that currently expose at least one model. Providers that
     * need an API key but don't have one, and providers that are unreachable, are dropped.
     */
    suspend fun listAvailableProviders(): List<LlmProviderWithModels> {
        val result = mutableListOf<LlmProviderWithModels>()
        for (config in clients) {
            if (config.provider == ClientProvider.GEMINI && config.apiKey.isNullOrBlank()) {
                continue
            }
            val models = fetchModels(config)
            if (models.isNotEmpty()) {
                result += LlmProviderWithModels(name = config.name, models = models)
            }
        }
        return result
    }

    /** Fetches the provider-specific models. */
    private suspend fun fetchModels(cfg: ClientConfig): List<String> {
        return try {
            when (cfg.provider) {
                ClientProvider.GEMINI -> fetchGeminiModels(cfg)
                ClientProvider.OPENAI_COMPATIBLE -> fetchOpenAiModels(cfg)
            }
        } catch (e: Exception) {
            log.debug("Could not fetch models for client {}: {}", cfg.name, e.message)
            emptyList()
        }
    }

    /**
     * Queries an OpenAI-compatible `/v1/models` endpoint. For the official OpenAI provider we
     * filter the GPT-5 chat models. For local servers (vLLM, mlx, etc.) we return all models.
     */
    private suspend fun fetchOpenAiModels(clientConf: ClientConfig): List<String> {
        val response: HttpResponse =
            httpClient.get("${clientConf.baseUrl}/v1/models") {
                timeout { requestTimeoutMillis = 2_000L }
                clientConf.apiKey?.let { headers.append(HttpHeaders.Authorization, "Bearer $it") }
            }
        if (!response.status.isSuccess()) return emptyList()

        val ids = response.body<OpenAiModelsResponse>().data.map { it.id }
        // OpenAI returns all models (also image and audio models), so we filter to get only the
        // GPT-5-series models.
        // Local OpenAI-compatible servers only serve what they loaded, so we show everything they
        // expose.
        return if (clientConf.name == "openai") {
            ids.filter { it.startsWith("gpt-5") }.sorted()
        } else {
            ids.sorted()
        }
    }

    /** Queries the Gemini `/models` endpoint and keeps only the `gemini-*` chat models. */
    private suspend fun fetchGeminiModels(cfg: ClientConfig): List<String> {
        val response: HttpResponse =
            httpClient.get("${cfg.baseUrl}/models?key=${cfg.apiKey}") {
                timeout { requestTimeoutMillis = 2_000L }
            }
        if (!response.status.isSuccess()) return emptyList()

        return response
            .body<GeminiModelsResponse>()
            .models
            // Gemini returns names as "models/gemini-2.5-flash"
            .map { it.name.removePrefix("models/") }
            // Only Gemini chat models
            .filter { it.startsWith("gemini-") }
            .sorted()
    }
}

/** Read `llm.clients.*` from the config */
fun configuredClients(config: Config): List<ClientConfig> {
    val clientsConfig = config.getConfig("llm.clients")
    return clientsConfig.root().keys.map { name ->
        val client = clientsConfig.getConfig(name)
        val apiKey =
            if (client.hasPath("apiKeyEnv")) {
                val apiKeyEnv = client.getString("apiKeyEnv")
                val envValue = System.getenv(apiKeyEnv)

                if (envValue.isNullOrBlank()) {
                    log.debug("No API key found in env var {} for client {}", apiKeyEnv, name)
                }

                envValue
            } else {
                null
            }

        ClientConfig(
            name = name,
            baseUrl = client.getString("baseUrl"),
            apiKey = apiKey,
            provider =
                if (name == "gemini") {
                    ClientProvider.GEMINI
                } else {
                    ClientProvider.OPENAI_COMPATIBLE
                },
        )
    }
}
