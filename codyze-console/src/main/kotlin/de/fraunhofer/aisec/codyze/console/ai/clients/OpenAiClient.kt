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
package de.fraunhofer.aisec.codyze.console.ai.clients

import de.fraunhofer.aisec.codyze.console.ai.ChatMessageJSON
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlinx.serialization.json.*

/** OpenAI-compatible API client (Ollama, vLLM, MLX) */
class OpenAiClient(
    private val httpClient: HttpClient,
    private val model: String,
    private val baseUrl: String,
) : LlmClient {
    override val modelName: String = model

    override suspend fun sendPrompt(
        userMessage: String,
        conversationHistory: List<ChatMessageJSON>,
        tools: List<Tool>,
        toolCallHistory: List<List<ToolCallWithResult>>?,
        onText: suspend (String) -> Unit,
        onReasoning: suspend (String) -> Unit,
    ): List<ToolCall> {
        val messages = buildMessages(userMessage, conversationHistory, toolCallHistory)
        val openAiTools = convertToolDefinitions(tools)

        val request =
            OpenAiRequest(model = model, messages = messages, tools = openAiTools, stream = true)

        val pendingToolCalls = mutableMapOf<Int, ToolCall>()

        httpClient
            .preparePost("$baseUrl/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            .execute { response ->
                if (!response.status.isSuccess()) {
                    val errorBody = response.body<String>()
                    onText("LLM request failed: ${response.status.value}\n$errorBody")
                    return@execute
                }
                val channel = response.body<ByteReadChannel>()
                handleStreamingResponse(channel, onText, onReasoning, pendingToolCalls)
            }

        return pendingToolCalls.values.filter { it.name.isNotEmpty() }.toList()
    }

    /**
     * Builds the OpenAI messages.
     *
     * ```json
     * [
     *   { "role": "system", "content": "..." },
     *   { "role": "user", "content": "..." },
     *   { "role": "assistant", "content": "",
     *     "tool_calls": [{
     *       "id": "call_0",
     *       "type": "function",
     *       "function": { "name": "...", "arguments": "..." }
     *     }]
     *   },
     *   { "role": "tool", "tool_call_id": "call_0", "content": "..." }
     * ]
     * ```
     *
     * See
     * [OpenAI Chat Completions](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create)
     */
    private fun buildMessages(
        userMessage: String,
        conversationHistory: List<ChatMessageJSON>,
        toolCallHistory: List<List<ToolCallWithResult>>?,
    ): List<OpenAiMessage> {
        val messages = mutableListOf<OpenAiMessage>()
        messages += OpenAiMessage(role = "system", content = JsonPrimitive(SYSTEM_PROMPT))
        conversationHistory.dropLast(1).forEach { msg ->
            if (msg.content.isNotBlank()) {
                messages += OpenAiMessage(role = msg.role, content = JsonPrimitive(msg.content))
            }
        }

        messages += OpenAiMessage(role = "user", content = JsonPrimitive(userMessage))

        if (toolCallHistory != null) {
            var callIdCounter = 0
            for (agentStep in toolCallHistory) {
                val startId = callIdCounter
                messages +=
                    OpenAiMessage(
                        role = "assistant",
                        content = JsonPrimitive(""),
                        toolCalls =
                            agentStep.mapIndexed { index, toolCallWithResult ->
                                OpenAiToolCall(
                                    id = "call_${startId + index}",
                                    type = "function",
                                    function =
                                        OpenAiFunctionCall(
                                            name = toolCallWithResult.call.name,
                                            arguments = toolCallWithResult.call.arguments,
                                        ),
                                )
                            },
                    )
                agentStep.forEachIndexed { index, toolCallWithResult ->
                    messages +=
                        OpenAiMessage(
                            role = "tool",
                            toolCallId = "call_${startId + index}",
                            content = JsonPrimitive(toolCallWithResult.result),
                        )
                }
                callIdCounter += agentStep.size
            }
        }
        return messages
    }

    /**
     * Converts MCP tools to the OpenAI tools (functions) format.
     *
     * ```json
     * [{
     *   "type": "function",
     *   "function": {
     *     "name": "...",
     *     "description": "...",
     *     "parameters": {
     *       "type": "object",
     *       "properties": { ... },
     *       "required": ["..."]
     *     }
     *   }
     * }]
     * ```
     *
     * See [OpenAI function calling](https://developers.openai.com/api/docs/guides/function-calling)
     */
    private fun convertToolDefinitions(tools: List<Tool>): List<OpenAiTool>? {
        if (tools.isEmpty()) return null

        return tools.map { tool ->
            OpenAiTool(
                type = "function",
                function =
                    OpenAiFunctionDef(
                        name = tool.name,
                        description = tool.description ?: "",
                        parameters =
                            buildJsonObject {
                                put("type", "object")
                                put("properties", tool.inputSchema.properties ?: buildJsonObject {})
                                tool.inputSchema.required?.let { required ->
                                    put("required", JsonArray(required.map { JsonPrimitive(it) }))
                                }
                            },
                    ),
            )
        }
    }

    /**
     * Handles streaming events from OpenAI. Tool call results will be received in multiple chunks
     * and will be then concatenated.
     *
     * ```json
     * {
     *   "choices": [{
     *     "delta": {
     *       "content": "...",
     *       "tool_calls": [{
     *         "index": 0,
     *         "id": "call_0",
     *         "function": { "name": "...", "arguments": "partial..." }
     *       }]
     *     }
     *   }]
     * }
     * ```
     *
     * See
     * [OpenAI streaming events]https://developers.openai.com/api/reference/resources/chat/subresources/completions/streaming-events"></a>
     * See [OpenAI Streaming API](https://developers.openai.com/api/docs/guides/streaming-responses)
     */
    private suspend fun handleStreamingResponse(
        channel: ByteReadChannel,
        onText: suspend (String) -> Unit,
        onReasoning: suspend (String) -> Unit,
        pendingToolCalls: MutableMap<Int, ToolCall>,
    ) {
        readSseStream(channel) { jsonStr ->
            val chunk = Json.parseToJsonElement(jsonStr).jsonObject
            val delta =
                chunk["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("delta")?.jsonObject

            if (delta != null) {
                val reasoningContent =
                    delta["reasoning"]?.jsonPrimitive?.contentOrNull
                        ?: delta["thoughts"]?.jsonPrimitive?.contentOrNull
                        ?: delta["thinking"]?.jsonPrimitive?.contentOrNull

                if (reasoningContent?.isNotEmpty() == true) {
                    onReasoning(reasoningContent)
                }

                delta["content"]?.jsonPrimitive?.contentOrNull?.let { content ->
                    if (content.isNotEmpty()) {
                        onText(content)
                    }
                }

                delta["tool_calls"]?.jsonArray?.forEach { toolElement ->
                    val toolJson = toolElement.jsonObject
                    val index = toolJson["index"]?.jsonPrimitive?.intOrNull ?: 0
                    val entry = pendingToolCalls.getOrPut(index) { ToolCall() }

                    toolJson["id"]?.jsonPrimitive?.contentOrNull?.let { entry.id = it }
                    toolJson["function"]?.jsonObject?.let { function ->
                        function["name"]?.jsonPrimitive?.contentOrNull?.let { entry.name = it }
                        function["arguments"]?.jsonPrimitive?.contentOrNull?.let {
                            entry.arguments += it
                        }
                    }
                }
            }
        }
    }
}
