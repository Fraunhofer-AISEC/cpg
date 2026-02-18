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
        "You have ONLY the tools listed in the tool definitions — do NOT invent or guess tool names. " +
        "When the user asks a question about the code, use your tools immediately do not ask for permission or confirmation and also don't ask the user " +
        "to do the analysis themselves. " +
        "Follow a multi-step approach: " +
        "1) First you can use listing tools to get an overview (e.g., cpg_list_functions for function summaries with names, parameters, and callees). " +
        "2) Then use cpg_get_node with specific IDs to inspect the actual source code of functions that look relevant. " +
        "Do NOT stop at summaries alone — always retrieve and inspect the code before drawing conclusions. " +
        "When inspecting a node with cpg_get_node, the result includes prevDFG/nextDFG edges showing data flow connections. " +
        "If you can answer from previous tool results already in the conversation, you can response without calling tools again. " +
        "If a tool call fails, do NOT retry the same call — instead, answer the question using the information you already have from previous tool results and your own knowledge. " +
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
