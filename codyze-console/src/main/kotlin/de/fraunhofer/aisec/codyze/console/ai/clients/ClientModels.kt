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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val tools: List<GeminiTools>? = null,
)

@Serializable data class GeminiContent(val role: String? = null, val parts: List<GeminiPart>)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val functionCall: GeminiFunctionCall? = null,
    val functionResponse: GeminiFunctionResponse? = null,
)

@Serializable data class GeminiFunctionCall(val name: String, val args: JsonObject)

@Serializable data class GeminiFunctionResponse(val name: String, val response: JsonObject)

@Serializable data class GeminiTools(val functionDeclarations: List<GeminiFunctionDef>)

@Serializable
data class GeminiFunctionDef(val name: String, val description: String, val parameters: JsonObject)

data class ToolCall(val name: String, val arguments: String)

data class ToolCallWithResult(val call: ToolCall, val result: String)

@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val tools: List<OpenAiTool>? = null,
    val stream: Boolean = false,
    @SerialName("max_tokens") val maxTokens: Int? = null,
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: JsonElement? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenAiToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
)

@Serializable
data class OpenAiToolCall(
    val id: String,
    @kotlinx.serialization.Required val type: String = "function",
    val function: OpenAiFunctionCall,
)

@Serializable data class OpenAiFunctionCall(val name: String, val arguments: String)

@Serializable data class OpenAiTool(val type: String, val function: OpenAiFunctionDef)

@Serializable
data class OpenAiFunctionDef(val name: String, val description: String, val parameters: JsonObject)
