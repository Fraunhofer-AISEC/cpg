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
) {
    suspend fun query(messages: List<SamplingMessage>, systemPrompt: String?): String {
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

    /** Send a prompt to Gemini and stream the response. */
    suspend fun sendPrompt(
        userMessage: String,
        conversationHistory: List<Any> = emptyList(),
        tools: List<Tool> = emptyList(),
        toolResults: List<ToolCallWithResult>? = null,
        emit: suspend (String) -> Unit,
    ): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()

        // Only include tools if we're not already processing tool results
        val geminiTools =
            if (toolResults == null && tools.isNotEmpty()) {
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

        // Build contents based on whether we have tool results
        val contents =
            if (toolResults != null) {
                listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessage))),
                    GeminiContent(
                        role = "model",
                        parts =
                            toolResults.map { tr ->
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
                            toolResults.map { tr ->
                                GeminiPart(
                                    functionResponse =
                                        GeminiFunctionResponse(
                                            name = tr.call.name,
                                            response = buildJsonObject { put("result", tr.result) },
                                        )
                                )
                            },
                    ),
                )
            } else {
                listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessage))))
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
                    emit(Events.text("Gemini API error (${response.status}): $errorBody"))
                    return@execute
                }

                val channel = response.body<ByteReadChannel>()
                processStream(channel, emit, toolCalls)
            }

        return toolCalls
    }

    private suspend fun processStream(
        channel: ByteReadChannel,
        emit: suspend (String) -> Unit,
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
                    if (text.isNotEmpty()) emit(Events.text(text))
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
