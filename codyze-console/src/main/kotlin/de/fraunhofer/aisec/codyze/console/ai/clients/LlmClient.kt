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

import de.fraunhofer.aisec.codyze.console.ai.ChatMessageJSON
import io.modelcontextprotocol.kotlin.sdk.types.SamplingMessage
import io.modelcontextprotocol.kotlin.sdk.types.Tool

/** Interface abstracting the underlying LLM provider (Gemini, OpenAI, Ollama, etc.). */
interface LlmClient {
    val modelName: String

    /** Query for MCP sampling requests. */
    suspend fun query(
        messages: List<SamplingMessage>,
        systemPrompt: String?,
        maxTokens: Int? = null,
    ): String

    /**
     * Streaming prompt execution for the chat. Calls [onText] for normal content and [onReasoning]
     * for thoughts/reasoning.
     */
    suspend fun sendPrompt(
        userMessage: String,
        conversationHistory: List<ChatMessageJSON> = emptyList(),
        tools: List<Tool> = emptyList(),
        toolResults: List<ToolCallWithResult>? = null,
        onText: suspend (String) -> Unit,
        onReasoning: suspend (String) -> Unit = {},
    ): List<ToolCall>
}
