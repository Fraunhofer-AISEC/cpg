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
import io.modelcontextprotocol.kotlin.sdk.types.SamplingMessage
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlinx.serialization.json.*

/** OpenAI-compatible API client (Ollama, vLLM, LMStudio) */
class OpenAiClient(
    private val httpClient: HttpClient,
    private val model: String,
    private val baseUrl: String,
) : LlmClient {
    override val modelName: String = model
    private val usesThinkTags = model.contains("glm", ignoreCase = true)

    /** Query the LLM when we have a tool that uses sampling */
    override suspend fun query(
        messages: List<SamplingMessage>,
        systemPrompt: String?,
        maxTokens: Int?,
    ): String {
        val openAiMessages = buildList {
            if (systemPrompt != null) {
                add(OpenAiMessage(role = "system", content = JsonPrimitive(systemPrompt)))
            }
            messages.forEach { msg ->
                add(
                    OpenAiMessage(
                        role = msg.role.toString().lowercase(),
                        content = JsonPrimitive((msg.content as? TextContent)?.text ?: ""),
                    )
                )
            }
        }

        val queryRequest =
            OpenAiRequest(
                model = model,
                messages = openAiMessages,
                maxTokens = maxTokens,
                stream = false,
            )

        val response =
            httpClient.post("$baseUrl/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(queryRequest)
            }

        if (!response.status.isSuccess()) {
            return "LLM request failed: ${response.status.value} ${response.status.description}"
        }

        val result = response.body<JsonObject>()
        return extractContentFromResponse(result) ?: "No response"
    }

    override suspend fun sendPrompt(
        userMessage: String,
        conversationHistory: List<ChatMessageJSON>,
        tools: List<Tool>,
        toolResults: List<ToolCallWithResult>?,
        onText: suspend (String) -> Unit,
        onReasoning: suspend (String) -> Unit,
    ): List<ToolCall> {
        val collectedToolCalls = mutableListOf<ToolCall>()
        val accumulatedToolCalls = mutableListOf<JsonObject>()

        val messages = buildList {
            add(OpenAiMessage(role = "system", content = JsonPrimitive(SYSTEM_PROMPT)))

            conversationHistory.dropLast(1).forEach { msg ->
                if (msg.role == "user" && msg.content.isNotBlank()) {
                    add(OpenAiMessage(role = msg.role, content = JsonPrimitive(msg.content)))
                }
            }

            add(OpenAiMessage(role = "user", content = JsonPrimitive(userMessage)))

            if (toolResults != null) {
                add(
                    OpenAiMessage(
                        role = "assistant",
                        content = JsonPrimitive(""),
                        toolCalls =
                            toolResults.mapIndexed { index, tr ->
                                OpenAiToolCall(
                                    id = "call_$index",
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
                toolResults.forEachIndexed { index, tr ->
                    add(
                        OpenAiMessage(
                            role = "tool",
                            toolCallId = "call_$index",
                            content = JsonPrimitive(tr.result),
                        )
                    )
                }
            }
        }

        val openAiTools =
            if (toolResults == null && tools.isNotEmpty()) {
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

        // Token estimation: messages + tools
        val msgChars = messages.sumOf { (it.content as? JsonPrimitive)?.content?.length ?: 0 }
        val toolChars =
            openAiTools?.sumOf { it.function.description.length + it.function.name.length } ?: 0
        val systemChars = SYSTEM_PROMPT.length
        val totalChars = msgChars + toolChars + systemChars
        println(
            "[OpenAI] Sending request | Messages: ${messages.size} | Tools: ${openAiTools?.size ?: 0} | ~${totalChars / 4} tokens"
        )
        val requestJson = Json { prettyPrint = true }.encodeToString(request)
        println("[OpenAI] Request:\n$requestJson")

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
        val toolCallsMap = mutableMapOf<Int, MutableMap<String, Any?>>()
        val reasoningBuffer = StringBuilder()
        var seenThinkClose = false

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
                        if (usesThinkTags) {
                            seenThinkClose =
                                thinkTagsStreaming(
                                    content,
                                    reasoningBuffer,
                                    seenThinkClose,
                                    onText,
                                    onReasoning,
                                )
                        } else {
                            onText(content)
                        }
                    }
                }

                delta["tool_calls"]?.jsonArray?.forEach { tcElement ->
                    val tc = tcElement.jsonObject
                    val index = tc["index"]?.jsonPrimitive?.intOrNull ?: 0
                    val toolCall =
                        toolCallsMap.getOrPut(index) {
                            mutableMapOf(
                                "id" to null,
                                "type" to "function",
                                "name" to null,
                                "arguments" to "",
                            )
                        }

                    tc["id"]?.jsonPrimitive?.contentOrNull?.let { toolCall["id"] = it }
                    tc["type"]?.jsonPrimitive?.contentOrNull?.let { toolCall["type"] = it }
                    tc["function"]?.jsonObject?.let { func ->
                        func["name"]?.jsonPrimitive?.contentOrNull?.let { toolCall["name"] = it }
                        func["arguments"]?.jsonPrimitive?.contentOrNull?.let { args ->
                            toolCall["arguments"] = (toolCall["arguments"] as String) + args
                        }
                    }
                }
            }
        }

        toolCallsMap.values.forEach { toolCall ->
            if (toolCall["name"] != null) {
                accumulatedToolCalls.add(
                    buildJsonObject {
                        toolCall["id"]?.let { put("id", it as String) }
                        put("type", toolCall["type"] as String)
                        put(
                            "function",
                            buildJsonObject {
                                put("name", toolCall["name"] as String)
                                put("arguments", toolCall["arguments"] as String)
                            },
                        )
                    }
                )
            }
        }
    }

    private fun extractContentFromResponse(result: JsonObject): String? {
        val content =
            result["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("message")
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.content

        return if (content == null) null
        else if (usesThinkTags) stripThinkTags(content) else content
    }
}
