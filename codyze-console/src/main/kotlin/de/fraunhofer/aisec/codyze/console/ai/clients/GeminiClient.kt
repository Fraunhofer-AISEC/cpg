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

class GeminiClient(
    private val httpClient: HttpClient,
    private val model: String,
    private val apiKey: String,
    private val baseUrl: String,
) : LlmClient {
    override val modelName: String = model

    override suspend fun query(
        messages: List<SamplingMessage>,
        systemPrompt: String?,
        maxTokens: Int?,
    ): String {
        val request =
            GeminiRequest(
                systemInstruction =
                    systemPrompt?.let { GeminiContent(parts = listOf(GeminiPart(text = it))) },
                contents =
                    messages.map { msg ->
                        GeminiContent(
                            role =
                                if (msg.role.toString().lowercase() == "user") "user" else "model",
                            parts =
                                listOf(GeminiPart(text = (msg.content as? TextContent)?.text ?: "")),
                        )
                    },
            )

        val response =
            httpClient.post("$baseUrl/models/$model:generateContent?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

        val result = response.body<JsonObject>()
        return extractTextFromResponse(result) ?: "No response"
    }

    override suspend fun sendPrompt(
        userMessage: String,
        conversationHistory: List<ChatMessageJSON>,
        tools: List<Tool>,
        maxAgentSteps: List<List<ToolCallWithResult>>?,
        onText: suspend (String) -> Unit,
        onReasoning: suspend (String) -> Unit,
    ): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()

        val geminiTools =
            if (tools.isNotEmpty()) {
                listOf(
                    GeminiTools(
                        functionDeclarations =
                            tools.map { tool ->
                                GeminiFunctionDef(
                                    name = tool.name,
                                    description = tool.description ?: "",
                                    parameters =
                                        buildJsonObject {
                                            put("type", "object")
                                            put(
                                                "properties",
                                                tool.inputSchema.properties ?: buildJsonObject {},
                                            )
                                        },
                                )
                            }
                    )
                )
            } else null

        val historyContents = buildList {
            conversationHistory.dropLast(1).forEach { msg ->
                if (msg.content.isNotBlank()) {
                    val role = if (msg.role == "assistant") "model" else "user"
                    add(GeminiContent(role = role, parts = listOf(GeminiPart(text = msg.content))))
                }
            }
        }

        val contents =
            if (maxAgentSteps != null) {
                historyContents +
                    listOf(
                        GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessage)))
                    ) +
                    maxAgentSteps.flatMap { roundtrip ->
                        listOf(
                            GeminiContent(
                                role = "model",
                                parts =
                                    roundtrip.map { tr ->
                                        GeminiPart(
                                            functionCall =
                                                GeminiFunctionCall(
                                                    name = tr.call.name,
                                                    args =
                                                        Json.parseToJsonElement(tr.call.arguments)
                                                            .jsonObject,
                                                )
                                        )
                                    },
                            ),
                            GeminiContent(
                                role = "user",
                                parts =
                                    roundtrip.map { tr ->
                                        GeminiPart(
                                            functionResponse =
                                                GeminiFunctionResponse(
                                                    name = tr.call.name,
                                                    response =
                                                        buildJsonObject { put("result", tr.result) },
                                                )
                                        )
                                    },
                            ),
                        )
                    }
            } else {
                historyContents +
                    listOf(
                        GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessage)))
                    )
            }

        val request =
            GeminiRequest(
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = SYSTEM_PROMPT))),
                contents = contents,
                tools = geminiTools,
            )

        httpClient
            .preparePost("$baseUrl/models/$model:streamGenerateContent?alt=sse&key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            .execute { response ->
                if (response.status.value >= 400) {
                    val errorBody = response.body<String>()
                    onText("Gemini API error (${response.status}): $errorBody")
                    return@execute
                }

                val channel = response.body<ByteReadChannel>()
                streamMessages(channel, onText, toolCalls)
            }

        return toolCalls
    }

    private suspend fun streamMessages(
        channel: ByteReadChannel,
        onText: suspend (String) -> Unit,
        toolCalls: MutableList<ToolCall>,
    ) {
        readSseStream(channel) { jsonStr ->
            val chunk = Json.parseToJsonElement(jsonStr).jsonObject
            val parts =
                chunk["candidates"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("content")
                    ?.jsonObject
                    ?.get("parts")
                    ?.jsonArray

            parts?.forEach { part ->
                val partObj = part.jsonObject

                partObj["text"]?.jsonPrimitive?.contentOrNull?.let { text ->
                    if (text.isNotEmpty()) onText(text)
                }

                partObj["functionCall"]?.jsonObject?.let { fc ->
                    val name = fc["name"]?.jsonPrimitive?.contentOrNull
                    val args = fc["args"]?.jsonObject
                    if (name != null) {
                        toolCalls.add(ToolCall(name, args?.toString() ?: "{}"))
                    }
                }
            }
        }
    }

    private fun extractTextFromResponse(result: JsonObject): String? {
        return result["candidates"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("content")
            ?.jsonObject
            ?.get("parts")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("text")
            ?.jsonPrimitive
            ?.content
    }
}
