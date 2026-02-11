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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

/** LLMs like GLM use <think> tags instead of the common approach using <reasoning> tags */
suspend fun thinkTagsStreaming(
    content: String,
    reasoningBuffer: StringBuilder,
    seenThinkClose: Boolean,
    onText: suspend (String) -> Unit,
    onReasoning: suspend (String) -> Unit,
): Boolean {
    var closed = seenThinkClose

    if (content.contains("</think>")) {
        val beforeClose = content.substringBefore("</think>")
        reasoningBuffer.append(beforeClose)

        if (reasoningBuffer.isNotEmpty()) {
            onReasoning(reasoningBuffer.toString())
            reasoningBuffer.clear()
        }

        closed = true

        val afterClose = content.substringAfter("</think>")
        if (afterClose.isNotEmpty()) {
            onText(afterClose)
        }
    } else {
        if (!closed) {
            reasoningBuffer.append(content)
        } else {
            onText(content)
        }
    }

    return closed
}

/** Strip <think> tags from non-streaming content Note: Needed for GLM models */
fun stripThinkTags(content: String): String {
    return content.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "").trim()
}

/** Helper to create stream events for the frontend */
object Events {
    fun text(content: String): String =
        Json.encodeToString(
            buildJsonObject {
                put("type", "text")
                put("content", content)
            }
        )

    fun reasoning(content: String): String =
        Json.encodeToString(
            buildJsonObject {
                put("type", "reasoning")
                put("content", content)
            }
        )

    fun keepalive(): String = Json.encodeToString(buildJsonObject { put("type", "keepalive") })

    fun toolResult(toolName: String, content: JsonElement): String =
        Json.encodeToString(
            buildJsonObject {
                put("type", "tool_result")
                put("toolName", toolName)
                put("content", content)
            }
        )
}
