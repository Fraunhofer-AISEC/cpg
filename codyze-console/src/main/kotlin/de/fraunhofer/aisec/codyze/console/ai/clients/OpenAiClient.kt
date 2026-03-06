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
        maxAgentSteps: List<List<ToolCallWithResult>>?,
        onText: suspend (String) -> Unit,
        onReasoning: suspend (String) -> Unit,
    ): List<ToolCall> {
        val collectedToolCalls = mutableListOf<ToolCall>()
        val accumulatedToolCalls = mutableListOf<JsonObject>()

        var callIdCounter = 0

        val messages = buildList {
            add(OpenAiMessage(role = "system", content = JsonPrimitive(SYSTEM_PROMPT)))

            conversationHistory.dropLast(1).forEach { msg ->
                if (msg.content.isNotBlank()) {
                    add(OpenAiMessage(role = msg.role, content = JsonPrimitive(msg.content)))
                }
            }

            add(OpenAiMessage(role = "user", content = JsonPrimitive(userMessage)))

            if (maxAgentSteps != null) {
                for (agentStep in maxAgentSteps) {
                    val startId = callIdCounter
                    add(
                        OpenAiMessage(
                            role = "assistant",
                            content = JsonPrimitive(""),
                            toolCalls =
                                agentStep.mapIndexed { index, tr ->
                                    OpenAiToolCall(
                                        id = "call_${startId + index}",
                                        type = "function",
                                        function =
                                            OpenAiFunctionCall(
                                                name = tr.call.name,
                                                arguments = tr.call.arguments,
                                            ),
                                    )
                                },
                        )
                    )
                    agentStep.forEachIndexed { index, tr ->
                        add(
                            OpenAiMessage(
                                role = "tool",
                                toolCallId = "call_${startId + index}",
                                content = JsonPrimitive(tr.result),
                            )
                        )
                    }
                    callIdCounter += agentStep.size
                }
            }
        }

        val openAiTools =
            if (tools.isNotEmpty()) {
                tools.map { tool ->
                    OpenAiTool(
                        type = "function",
                        function =
                            OpenAiFunctionDef(
                                name = tool.name,
                                description = tool.description ?: "",
                                parameters =
                                    buildJsonObject {
                                        put("type", "object")
                                        put(
                                            "properties",
                                            tool.inputSchema.properties ?: buildJsonObject {},
                                        )
                                        tool.inputSchema.required?.let { required ->
                                            put(
                                                "required",
                                                JsonArray(required.map { r -> JsonPrimitive(r) }),
                                            )
                                        }
                                    },
                            ),
                    )
                }
            } else null

        val request =
            OpenAiRequest(model = model, messages = messages, tools = openAiTools, stream = true)

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
                streamMessages(channel, onText, onReasoning, accumulatedToolCalls)
            }

        for (tcObj in accumulatedToolCalls) {
            val function = tcObj["function"]?.jsonObject
            val name = function?.get("name")?.jsonPrimitive?.contentOrNull
            val args = function?.get("arguments")?.jsonPrimitive?.contentOrNull ?: "{}"
            if (name != null) {
                collectedToolCalls.add(ToolCall(name, args))
            }
        }

        return collectedToolCalls
    }

    private suspend fun streamMessages(
        channel: ByteReadChannel,
        onText: suspend (String) -> Unit,
        onReasoning: suspend (String) -> Unit,
        accumulatedToolCalls: MutableList<JsonObject>,
    ) {
        val streamingToolCalls = mutableMapOf<Int, StreamingToolCall>()

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

                delta["tool_calls"]?.jsonArray?.forEach { tool ->
                    val toolJSON = tool.jsonObject
                    val index = toolJSON["index"]?.jsonPrimitive?.intOrNull ?: 0
                    val entry = streamingToolCalls.getOrPut(index) { StreamingToolCall() }

                    toolJSON["id"]?.jsonPrimitive?.contentOrNull?.let { entry.id = it }
                    toolJSON["function"]?.jsonObject?.let { func ->
                        func["name"]?.jsonPrimitive?.contentOrNull?.let { entry.name = it }
                        func["arguments"]?.jsonPrimitive?.contentOrNull?.let {
                            entry.arguments += it
                        }
                    }
                }
            }
        }

        streamingToolCalls.values.forEach { entry ->
            if (entry.name != null) {
                accumulatedToolCalls.add(
                    buildJsonObject {
                        entry.id?.let { put("id", it) }
                        put("type", "function")
                        put(
                            "function",
                            buildJsonObject {
                                put("name", entry.name!!)
                                put("arguments", entry.arguments)
                            },
                        )
                    }
                )
            }
        }
    }
}
