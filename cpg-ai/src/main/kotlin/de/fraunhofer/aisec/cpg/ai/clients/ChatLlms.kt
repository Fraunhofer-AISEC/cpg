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
import ai.koog.prompt.executor.clients.retry.RetryingLLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger as KtorLogger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import org.slf4j.LoggerFactory

// The same logger as LlmProviderConfig: its DEBUG level switches on the request/response body
// logging below, and hosts recognize the LLM traffic by its name.
private val log = LoggerFactory.getLogger(LlmProviderConfig::class.java)

/**
 * A [ChatLlm] built by [LlmProviderConfig], together with the HTTP clients it created for it and
 * therefore has to close.
 */
internal sealed class OwnedChatLlm(
    val chatLlm: ChatLlm,
    private val httpClients: List<HttpClient>,
) : AutoCloseable {
    override fun close() {
        chatLlm.executor.close()
        // The executor may not close a base client it was handed; HttpClient.close() is idempotent.
        httpClients.forEach { it.close() }
    }
}

/**
 * A Gemini [model] through Koog's [GoogleLLMClient]. Gemini has no "local/custom endpoint" use case
 * (unlike [OpenAiCompatibleChatLlm]), so [ClientConfig.baseUrl] is not routed through here; it is
 * only used for model discovery.
 */
internal class GeminiChatLlm(apiKey: String, model: String, contextLength: Long) :
    OwnedChatLlm(
        ChatLlm(
            executor =
                MultiLLMPromptExecutor(
                    LLMProvider.Google to
                        RetryingLLMClient(GoogleLLMClient(apiKey), transientFailureRetryConfig)
                ),
            model =
                LLModel(
                    provider = LLMProvider.Google,
                    id = model,
                    capabilities =
                        listOf(
                            LLMCapability.Temperature,
                            LLMCapability.Tools,
                            // GoogleLLMClient.execute() requires it (the same check as in
                            // OpenAILLMClient); without it every Gemini model fails with "Model
                            // <id> does not support completion".
                            LLMCapability.Completion,
                        ),
                    contextLength = contextLength,
                ),
        ),
        httpClients = emptyList(),
    )

/**
 * A [model] on an OpenAI-compatible server (vLLM, ollama, ...) at [ClientConfig.baseUrl], through
 * Koog's [OpenAILLMClient] on top of a Ktor client of our own (see [loggingHttpClient]).
 */
internal class OpenAiCompatibleChatLlm
private constructor(
    httpClient: HttpClient,
    config: ClientConfig,
    model: String,
    contextLength: Long,
) :
    OwnedChatLlm(
        ChatLlm(
            executor =
                MultiLLMPromptExecutor(
                    LLMProvider.OpenAI to
                        RetryingLLMClient(
                            llmClient(httpClient, config),
                            transientFailureRetryConfig,
                        )
                ),
            model = llModel(model, contextLength),
        ),
        httpClients = listOf(httpClient),
    ) {

    constructor(
        config: ClientConfig,
        model: String,
        contextLength: Long,
    ) : this(loggingHttpClient(config), config, model, contextLength)

    private companion object {
        /**
         * The Ktor client underneath Koog's: it can log the full request/response bodies (tool
         * schema, tool_choice, the raw completion) - invaluable for debugging tool calling against
         * custom/local servers, whose behavior can diverge from the official OpenAI API in ways
         * that are otherwise invisible from inside Koog's client. Gated on [log]'s DEBUG level
         * rather than always on: with the tool-calling history resent on every round trip,
         * unconditional body logging (as opposed to Ktor's terser LogLevel.INFO, which only logs
         * the request line/status/duration) produces log files of tens of megabytes per run.
         */
        fun loggingHttpClient(config: ClientConfig): HttpClient {
            if (config.insecureTls) {
                log.warn(
                    "TLS is fully disabled for client {} ({}): any certificate for any host is " +
                        "accepted. Use only on trusted internal networks.",
                    config.name,
                    config.baseUrl,
                )
            }
            return HttpClient(CIO) {
                if (config.insecureTls) engine { https { trustManager = TRUST_ALL } }
                install(Logging) {
                    logger =
                        object : KtorLogger {
                            override fun log(message: String) {
                                log.debug(message)
                            }
                        }
                    level = if (log.isDebugEnabled) LogLevel.ALL else LogLevel.INFO
                    // At LogLevel.ALL, Ktor logs the full request including headers - an
                    // unredacted Authorization header (a real API key) would otherwise leak into
                    // the host application's debug logs verbatim.
                    sanitizeHeader { header ->
                        header.equals(HttpHeaders.Authorization, ignoreCase = true)
                    }
                }
            }
        }

        /**
         * Koog's [OpenAILLMClient] on [httpClient], built through Koog's own
         * [KtorKoogHttpClient.Factory] so that baseUrl, content type, ContentNegotiation and SSE
         * are wired exactly as by its default apiKey-based constructor. Without
         * [ClientConfig.requestTimeoutMillis], Koog's default timeout (900s) stays in place; a host
         * that bounds calls with its own timeout should set it to match.
         */
        fun llmClient(httpClient: HttpClient, config: ClientConfig) =
            OpenAILLMClient(
                apiKey = config.apiKey ?: "not-needed",
                settings =
                    OpenAIClientSettings(
                        baseUrl = config.baseUrl,
                        timeoutConfig =
                            config.requestTimeoutMillis?.let {
                                ConnectionTimeoutConfig(
                                    requestTimeoutMillis = it,
                                    socketTimeoutMillis = it,
                                )
                            } ?: ConnectionTimeoutConfig(),
                    ),
                httpClientFactory = KtorKoogHttpClient.Factory(baseClient = httpClient),
            )

        fun llModel(model: String, contextLength: Long) =
            LLModel(
                provider = LLMProvider.OpenAI,
                id = model,
                capabilities =
                    listOf(
                        LLMCapability.Temperature,
                        LLMCapability.Tools,
                        // AbstractOpenAILLMClient.execute() requires this base capability before it
                        // runs a completion at all; without it every model fails with "Model <id>
                        // does not support completion".
                        LLMCapability.Completion,
                        // OpenAILLMClient.determineParams() needs one of the two OpenAI endpoint
                        // capabilities to know which request shape to send; without it every
                        // custom/local model fails with "Cannot determine proper LLM params".
                        // These servers speak the /v1/chat/completions shape (the default
                        // chatCompletionsPath of OpenAIClientSettings), not /v1/responses.
                        LLMCapability.OpenAIEndpoint.Completions,
                        // Without any LLMCapability.Schema.JSON.*, requestLLMStructured can't use a
                        // native response_format json_schema and emulates structured output with a
                        // schema tool forced through tool_choice - which some OpenAI-compatible
                        // servers (e.g. vLLM) reject with "When using tool_choice, tools must be
                        // set". Basic rather than Standard (which assumes polymorphism/defs that
                        // many local models lack), as Koog does for local/Qwen-class models.
                        LLMCapability.Schema.JSON.Basic,
                    ),
                contextLength = contextLength,
            )
    }
}
