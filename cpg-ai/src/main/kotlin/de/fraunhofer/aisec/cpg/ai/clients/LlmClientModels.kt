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

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A resolved LLM target for a chat request: a Koog [PromptExecutor] (already bound to a provider
 * and its credentials/base URL) together with the specific [LLModel] to use with it. Returned by
 * [LlmProviderConfig.clientFor].
 */
data class ChatLlm(val executor: PromptExecutor, val model: LLModel)

enum class ClientProvider {
    GEMINI,
    OPENAI_COMPATIBLE,
}

data class ClientConfig(
    val name: String,
    val baseUrl: String,
    val apiKey: String?,
    val provider: ClientProvider,
    val requiresApiKey: Boolean,
    /**
     * Request/socket timeout for the LLM HTTP client, in milliseconds. `null` keeps Koog's own
     * default (900s) - see [LlmProviderConfig.clientFor].
     */
    val requestTimeoutMillis: Long? = null,
    /**
     * Explicit override for the model's context window, in tokens. `null` (the default) lets
     * [LlmProviderConfig.clientFor] auto-detect it from the server instead, falling back to a
     * generic default if that isn't possible. When set, this always wins over the auto-detected
     * value - see [LlmProviderConfig.resolveContextLength].
     */
    val contextLengthOverride: Long? = null,
)

@Serializable data class LlmProviderWithModels(val name: String, val models: List<String>)

@Serializable data class OpenAiModelsResponse(val data: List<OpenAiModel> = emptyList())

@Serializable
data class OpenAiModel(
    val id: String,
    /**
     * The model's real context window, in tokens - not part of the official OpenAI `/v1/models`
     * schema, but reported by some self-hosted OpenAI-compatible servers (e.g. vLLM) as an
     * extension field. Absent (`null`) for servers that don't report it.
     */
    @SerialName("max_model_len") val maxModelLen: Long? = null,
)

@Serializable data class GeminiModelsResponse(val models: List<GeminiModel> = emptyList())

@Serializable data class GeminiModel(val name: String)
