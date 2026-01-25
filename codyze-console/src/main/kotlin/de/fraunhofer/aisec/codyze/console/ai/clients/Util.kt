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

import io.ktor.utils.io.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

const val SYSTEM_PROMPT =
    "You are a code analysis assistant with access to CPG (Code Property Graph) tools. " +
        "Use multiple tools when needed to answer thoroughly. " +
        "Explain your findings clearly."

suspend fun readSseStream(channel: ByteReadChannel, processLine: suspend (String) -> Unit) {
    while (!channel.isClosedForRead) {
        val line =
            try {
                channel.readUTF8Line()
            } catch (_: Exception) {
                break
            }

        if (line.isNullOrBlank()) continue
        if (!line.startsWith("data: ")) continue

        val jsonStr = line.substringAfter("data: ").trim()
        if (jsonStr == "[DONE]") break

        try {
            processLine(jsonStr)
        } catch (_: Exception) {
            // Continue on parsing errors
        }
    }
}

/** Process <think> tags during streaming - updates seenThinkClose via mutable parameter */
suspend fun processThinkTagsStreaming(
    content: String,
    reasoningBuffer: StringBuilder,
    seenThinkClose: Boolean,
    emit: suspend (String) -> Unit,
): Boolean {
    var closed = seenThinkClose

    if (content.contains("</think>")) {
        val beforeClose = content.substringBefore("</think>")
        reasoningBuffer.append(beforeClose)

        if (reasoningBuffer.isNotEmpty()) {
            emit(Events.reasoning(reasoningBuffer.toString()))
            reasoningBuffer.clear()
        }

        closed = true

        val afterClose = content.substringAfter("</think>")
        if (afterClose.isNotEmpty()) {
            emit(Events.text(afterClose))
        }
    } else {
        if (!closed) {
            reasoningBuffer.append(content)
        } else {
            emit(Events.text(content))
        }
    }

    return closed
}

/** Strip <think> tags from non-streaming content */
fun stripThinkTags(content: String): String {
    return content.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "").trim()
}

/** Stream events sent to frontend */
@Serializable
data class StreamEvent(
    val type: String,
    val content: String? = null,
    val toolName: String? = null,
    val arguments: String? = null,
    val toolContent: JsonElement? = null,
)

/** Helper to create stream events */
object Events {
    fun text(content: String) = Json.encodeToString(StreamEvent(type = "text", content = content))

    fun reasoning(content: String) =
        Json.encodeToString(StreamEvent(type = "reasoning", content = content))

    fun keepalive() = Json.encodeToString(StreamEvent(type = "keepalive"))

    fun toolPending(toolCall: ToolCall) =
        Json.encodeToString(
            StreamEvent(
                type = "tool_pending",
                toolName = toolCall.name,
                arguments = toolCall.arguments,
            )
        )

    fun toolResult(toolName: String, content: JsonElement) =
        Json.encodeToString(
            StreamEvent(type = "tool_result", toolName = toolName, toolContent = content)
        )
}
