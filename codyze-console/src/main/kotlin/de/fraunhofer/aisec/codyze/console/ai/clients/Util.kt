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

import de.fraunhofer.aisec.codyze.console.ai.ChatService
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

const val SYSTEM_PROMPT =
    "You are a code analysis agent with access to CPG (Code Property Graph) tools. " +
        "The code and CPG is already loaded you can start querying the graph immediately. " +
        "You have ONLY the tools listed in the tool definitions — do NOT invent or guess tool names. " +
        "When the user asks a question, use the tools that best fit the task. " +
        "Read each tool's description carefully to understand what it does and when to use it. " +
        "Use tools immediately do not ask for permission or confirmation and do not ask the user to do the analysis themselves. " +
        "Follow a multi-step approach: start with tools that give you an overview, then use more specific tools to get more information. " +
        "Do NOT stop at summaries alone always retrieve and inspect the code before drawing conclusions. " +
        "If you can answer from previous tool results already in the conversation, you can respond without calling tools again. " +
        "If a tool call fails, do NOT retry the same call instead, answer the question using the information you already have from previous tool results and your own knowledge. " +
        "When the user asks you to suggest, propose, or identify concepts and operations use the `cpg_suggest_llm_concepts_and_operations` tool rather than responding with text. Follow this order strictly: (1) call `cpg_list_llm_concepts_operations` once to load previously accepted concept definitions - if empty, propose new concepts, otherwise reuse existing concept and operation names (and their property schemas) wherever they semantically fit instead of inventing duplicates; " +
        "(2) discover the code comprehensively concepts and operations attach to any kind of CPG node (functions, calls, records, fields, variables, arguments, etc), so a single listing is never sufficient. In particular, operations typically live on call sites, not on function declarations, so always inspect every node kind that could surface candidates; (3) only then call `cpg_suggest_llm_concepts_and_operations` with REAL node IDs returned by the previous tools. Never pass placeholder, invented, or guessed IDs. " +
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

/** SSE event payloads streamed from [ChatService] to the frontend. */
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
