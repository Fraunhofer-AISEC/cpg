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

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.ConnectionTimeoutConfig
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.retry.RetryConfig
import ai.koog.prompt.executor.clients.retry.RetryingLLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import com.typesafe.config.Config
import de.fraunhofer.aisec.cpg.helpers.filterMapped
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger as KtorLogger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(LlmProviderConfig::class.java)

/**
 * Retries only well-known transient failures ([RetryConfig.DEFAULT_PATTERNS]: 429/5xx,
 * rate-limit/timeout-ish keywords) - independent of, and unrelated to, the bounded retry of failed
 * *tool* calls described in [SYSTEM_PROMPT], which is about the model's own tool-call behavior, not
 * transport/provider failures. Deliberately much tighter than [RetryConfig.PRODUCTION] (3 attempts,
 * up to 20s max delay each): a host application may bound a call with its own timeout, so retry
 * backoff here must stay a small addition to that budget, not something that can itself balloon
 * into multiples of it. With these settings, the two backoff waits between 3 attempts are at most
 * ~1s and ~2s (before jitter); each attempt is bounded by the client's own request/socket timeout.
 */
private val transientFailureRetryConfig =
    RetryConfig(maxAttempts = 3, initialDelay = 1.seconds, maxDelay = 5.seconds)

/**
 * Context length used when neither [ClientConfig.contextLengthOverride] nor the server
 * ([LlmProviderConfig.contextLengthFor]) provides one. Deliberately conservative: too small only
 * makes history compression start earlier, too large risks a context-overflow error.
 */
internal const val DEFAULT_CONTEXT_LENGTH = 128_000L

/** A [ChatLlm] built by [LlmProviderConfig], together with the HTTP clients it created for it. */
private class OwnedChatLlm(val chatLlm: ChatLlm, val httpClients: List<HttpClient>) :
    AutoCloseable {
    override fun close() {
        chatLlm.executor.close()
        // The executor may not close a base client it was handed; HttpClient.close() is idempotent.
        httpClients.forEach { it.close() }
    }
}

/**
 * Resolves configured LLM providers to Koog executors. Owns every executor it creates: one per
 * client and model, reused by all chat calls, and closed by [close].
 */
class LlmProviderConfig(private val httpClient: HttpClient, val clients: List<ClientConfig>) :
    AutoCloseable {
    /** Executors already built, per client and model (see [clientFor]). */
    private val chatLlms = ConcurrentHashMap<Pair<String, String>, OwnedChatLlm>()

    @Volatile private var closed = false

    /**
     * Resolves [clientName] and [model] to a [ChatLlm], or `null` if the client is unknown or its
     * required API key is missing. Built once per client and model, so the context length is too:
     * [ClientConfig.contextLengthOverride], else the server's value (see [contextLengthFor]), else
     * [DEFAULT_CONTEXT_LENGTH].
     */
    suspend fun clientFor(clientName: String, model: String): ChatLlm? {
        check(!closed) { "LlmProviderConfig is closed" }
        val key = clientName to model
        chatLlms[key]?.let {
            return it.chatLlm
        }
        val config = clients.firstOrNull { it.name == clientName } ?: return null
        // Built outside any lock; if a concurrent call built one for the same key first, ours is
        // closed and theirs is used.
        val created = createChatLlm(config, model) ?: return null
        val winner = chatLlms.putIfAbsent(key, created)
        if (winner != null) {
            created.close()
            return winner.chatLlm
        }
        if (closed) {
            // close() ran while we were building: it may not have seen this entry.
            chatLlms.remove(key)?.close()
            error("LlmProviderConfig is closed")
        }
        return created.chatLlm
    }

    /** Closes every executor built by [clientFor], and the HTTP clients created for them. */
    override fun close() {
        closed = true
        chatLlms.keys.toList().forEach { key -> chatLlms.remove(key)?.close() }
    }

    private suspend fun createChatLlm(config: ClientConfig, model: String): OwnedChatLlm? {
        val contextLength =
            config.contextLengthOverride
                ?: contextLengthFor(config, model)
                ?: DEFAULT_CONTEXT_LENGTH

        return when (config.provider) {
            ClientProvider.GEMINI -> {
                val apiKey = config.apiKey ?: return null
                // Gemini has no real "local/custom endpoint" use case (unlike the OpenAI-compatible
                // providers below), so we intentionally don't try to route config.baseUrl through
                // here; it is still used for model discovery in fetchGeminiModels.
                OwnedChatLlm(
                    ChatLlm(
                        executor =
                            MultiLLMPromptExecutor(
                                LLMProvider.Google to
                                    RetryingLLMClient(
                                        GoogleLLMClient(apiKey),
                                        transientFailureRetryConfig,
                                    )
                            ),
                        model =
                            LLModel(
                                provider = LLMProvider.Google,
                                id = model,
                                capabilities =
                                    listOf(
                                        LLMCapability.Temperature,
                                        LLMCapability.Tools,
                                        // GoogleLLMClient.execute() requires this capability too
                                        // (same
                                        // requireCapability(LLMCapability.Completion) check as
                                        // OpenAILLMClient below); without it every Gemini model
                                        // fails
                                        // with "Model <id> does not support completion".
                                        LLMCapability.Completion,
                                    ),
                                contextLength = contextLength,
                            ),
                    ),
                    httpClients = emptyList(),
                )
            }

            ClientProvider.OPENAI_COMPATIBLE -> {
                // Can log the full request/response bodies exchanged with the OpenAI-compatible
                // endpoint (tool schema, tool_choice, and the raw completion) - invaluable for
                // debugging tool-calling issues against custom/local servers (vLLM, ollama, ...),
                // whose behavior can diverge from the official OpenAI API in ways that are
                // otherwise invisible from inside Koog's client. Gated on this logger's own DEBUG
                // level rather than always-on: with a growing tool-calling history resent on every
                // round trip, unconditional body logging (as opposed to Ktor's terser default
                // LogLevel.INFO, which only logs the request line/status/duration) previously
                // produced multi-tens-of-megabytes log files for a single skill run.
                val loggingKtorClient =
                    HttpClient(CIO) {
                        install(Logging) {
                            logger =
                                object : KtorLogger {
                                    override fun log(message: String) {
                                        log.debug(message)
                                    }
                                }
                            level = if (log.isDebugEnabled) LogLevel.ALL else LogLevel.INFO
                            // At LogLevel.ALL, Ktor logs the full request including headers - an
                            // unredacted Authorization header (a real API key) would otherwise
                            // leak into the host application's debug logs verbatim.
                            sanitizeHeader { header ->
                                header.equals(HttpHeaders.Authorization, ignoreCase = true)
                            }
                        }
                    }
                // Reuses Koog's own KtorKoogHttpClient.Factory (rather than hand-rolling the
                // KoogHttpClient setup) so we get its baseUrl/contentType/ContentNegotiation/SSE
                // wiring exactly as the default apiKey-based OpenAILLMClient constructor would -
                // only swapping in our logging-enabled Ktor client underneath.
                // By default (requestTimeoutMillis == null) this leaves Koog's own
                // ConnectionTimeoutConfig default (900s) in place; a host application that bounds
                // calls with its own timeout should set requestTimeoutMillis to match it.
                val timeoutConfig =
                    config.requestTimeoutMillis?.let {
                        ConnectionTimeoutConfig(requestTimeoutMillis = it, socketTimeoutMillis = it)
                    } ?: ConnectionTimeoutConfig()
                val client =
                    OpenAILLMClient(
                        apiKey = config.apiKey ?: "not-needed",
                        settings =
                            OpenAIClientSettings(
                                baseUrl = config.baseUrl,
                                timeoutConfig = timeoutConfig,
                            ),
                        httpClientFactory =
                            KtorKoogHttpClient.Factory(baseClient = loggingKtorClient),
                    )
                OwnedChatLlm(
                    ChatLlm(
                        executor =
                            MultiLLMPromptExecutor(
                                LLMProvider.OpenAI to
                                    RetryingLLMClient(client, transientFailureRetryConfig)
                            ),
                        model =
                            LLModel(
                                provider = LLMProvider.OpenAI,
                                id = model,
                                capabilities =
                                    listOf(
                                        LLMCapability.Temperature,
                                        LLMCapability.Tools,
                                        // AbstractOpenAILLMClient.execute() requires this base
                                        // capability before it will run a completion at all;
                                        // without
                                        // it every model fails with "Model <id> does not support
                                        // completion".
                                        LLMCapability.Completion,
                                        // OpenAILLMClient.determineParams() separately requires the
                                        // model to declare one of the two OpenAI endpoint
                                        // capabilities
                                        // to know which request shape to send; without it, every
                                        // custom/local model (ollama, vLLM, mlx, ...) fails with
                                        // "Cannot determine proper LLM params". These servers all
                                        // speak
                                        // the legacy /v1/chat/completions shape (matching
                                        // OpenAIClientSettings' default chatCompletionsPath), not
                                        // the
                                        // newer /v1/responses API.
                                        LLMCapability.OpenAIEndpoint.Completions,
                                        // Without any LLMCapability.Schema.JSON.*,
                                        // requestLLMStructured
                                        // can't use native response_format: json_schema and instead
                                        // falls back to emulating structured output via a synthetic
                                        // schema-tool forced through tool_choice - which some
                                        // OpenAI-compatible servers (e.g. vLLM) reject outright
                                        // with a
                                        // "When using tool_choice, tools must be set" 400. Basic
                                        // (rather than Standard, which assumes polymorphism/defs
                                        // support many local/vLLM-served models lack) matches
                                        // Koog's
                                        // own precedent for local/Qwen-class models.
                                        LLMCapability.Schema.JSON.Basic,
                                    ),
                                contextLength = contextLength,
                            ),
                    ),
                    httpClients = listOf(loggingKtorClient),
                )
            }
        }
    }

    /**
     * Returns all configured providers that currently expose at least one model. Providers that
     * need an API key but don't have one, and providers that are unreachable, are dropped.
     */
    suspend fun listAvailableProviders(): List<LlmProviderWithModels> {
        val result = mutableListOf<LlmProviderWithModels>()
        for (config in clients) {
            if (config.requiresApiKey && config.apiKey.isNullOrBlank()) {
                continue
            }
            val models = fetchModels(config)
            if (models.isNotEmpty()) {
                result += LlmProviderWithModels(name = config.name, models = models)
            }
        }
        return result
    }

    /**
     * The context window (in tokens) [config]'s server reports for [model], or `null` if it does
     * not: only OpenAI-compatible servers are asked, and only some report it - `max_model_len`, a
     * vLLM extension field of `/v1/models` (see [OpenAiModel]).
     */
    private suspend fun contextLengthFor(config: ClientConfig, model: String): Long? {
        if (config.provider != ClientProvider.OPENAI_COMPATIBLE) return null
        return try {
            val response =
                httpClient.get("${config.baseUrl}/v1/models") {
                    timeout { requestTimeoutMillis = 2_000L }
                    config.apiKey?.let { headers.append(HttpHeaders.Authorization, "Bearer $it") }
                }
            if (!response.status.isSuccess()) return null
            response.body<OpenAiModelsResponse>().data.firstOrNull { it.id == model }?.maxModelLen
        } catch (e: Exception) {
            log.debug("Could not fetch context length for {}/{}: {}", config.name, model, e.message)
            null
        }
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
        return if (clientConf.name == "openai") {
            // Drop snapshots, e.g. "gpt-5-2025-08-07", and only keep alias "gpt-5".
            val snapshot = Regex("-\\d{4}-\\d{2}-\\d{2}$")
            ids.filter { it.startsWith("gpt-5") && !snapshot.containsMatchIn(it) }.sorted()
        } else {
            // For local OpenAI-compatible servers we show everything they have loaded.
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

        // Only Gemini 2.0/2.5/3/3.1 in "-pro", "-flash", or "-flash-lite", optionally "-preview".
        val allowed = Regex("^gemini-(2\\.0|2\\.5|3|3\\.1)-(pro|flash|flash-lite)(-preview)?$")
        return response
            .body<GeminiModelsResponse>()
            .models
            // Gemini returns names as "models/gemini-2.5-flash"
            .filterMapped({ it.name.removePrefix("models/") }) { allowed.matches(it) }
            .sorted()
    }
}

/** Build a [LlmProviderConfig] by reading `llm.clients.*` from the config. */
fun Config.toLlmProviderConfig(httpClient: HttpClient): LlmProviderConfig {
    val clientsConfig = getConfig("llm.clients")
    val clients =
        clientsConfig.root().keys.map { name ->
            val client = clientsConfig.getConfig(name)
            val requiresApiKey = client.hasPath("apiKeyEnv")
            val apiKey =
                if (requiresApiKey) {
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
                requiresApiKey = requiresApiKey,
                requestTimeoutMillis =
                    if (client.hasPath("requestTimeoutMillis"))
                        client.getLong("requestTimeoutMillis")
                    else null,
                contextLengthOverride =
                    if (client.hasPath("contextLength")) client.getLong("contextLength") else null,
            )
        }
    // Put local providers first, openai and gemini last.
    val sortedClients = clients.sortedBy { it.name == "openai" || it.name == "gemini" }
    return LlmProviderConfig(httpClient, sortedClients)
}
