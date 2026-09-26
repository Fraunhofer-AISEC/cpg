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
package de.fraunhofer.aisec.cpg.ai.clients

import de.fraunhofer.aisec.cpg.ai.ChatMessageJSON
import de.fraunhofer.aisec.cpg.ai.ChatService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

const val SYSTEM_PROMPT =
    "You are a code analysis agent with access to CPG (Code Property Graph) tools. " +
        "The code and CPG are already loaded, so you can query them directly. " +
        "Only use the tools listed in the tool definitions and do not invent tool names. " +
        "Choose the tools that fit the task and read their descriptions to understand when each one applies. " +
        "Use tools directly without asking for confirmation, and do not ask the user to run the analysis themselves. " +
        "Start with tools that give an overview, then choose more specific ones to get more details (if needed). " +
        "Do not stop at summaries. Inspect the actual code before drawing conclusions. " +
        "If a previous tool result already answers the question, respond without calling tools again. " +
        "If a tool call fails, do not retry it, instead continue with the information you already have. " +
        "Explain your findings clearly. " +
        "Always prefer calling tools through your normal function-calling mechanism. Only if that " +
        "is not available to you, write the call as a fenced JSON code block instead, in this " +
        "exact shape: ```json\n{\"name\": \"<tool name>\", \"arguments\": {<parameter>: <value>}}\n```."

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

    fun toolResult(toolName: String, args: JsonElement, content: JsonElement): String =
        Json.encodeToString(
            buildJsonObject {
                put("type", "tool_result")
                put("toolName", toolName)
                put("args", args)
                put("content", content)
            }
        )

    /**
     * A structured completion signal (see [de.fraunhofer.aisec.cpg.ai.TaskStatus]), sent once per
     * [ChatService.chat] call if the LLM produced one - callers that don't care can simply ignore
     * this event type, exactly like `reasoning`.
     */
    fun taskStatus(done: Boolean, resolvedItems: List<String>): String =
        Json.encodeToString(
            buildJsonObject {
                put("type", "task_status")
                put("done", done)
                put("resolvedItems", buildJsonArray { resolvedItems.forEach { add(it) } })
            }
        )

    /**
     * The conversation history as it stood at the end of one [ChatService.chat] call - after any
     * in-call history compression, and including tool-calling turns condensed to just their
     * non-blank text content (system messages, and any purely tool-call/tool-result turn with no
     * text part, are omitted - a caller resending this as its next request's `messages` already
     * gets a fresh system prompt, and a blank-content turn carries nothing a plain user/assistant
     * transcript can represent anyway). Emitted so a caller that resends the full conversation on
     * its next call (see [ChatService.chat]'s doc: it is stateless across calls by design) can
     * reuse *this* instead of its own naively-appended raw history - otherwise any compression
     * [ChatService] just did is invisible to that caller and gets undone the moment it resends.
     */
    fun finalHistory(messages: List<ChatMessageJSON>): String =
        Json.encodeToString(
            buildJsonObject {
                put("type", "final_history")
                put("messages", Json.encodeToJsonElement(messages))
            }
        )

    /** LLM token usage summed over an entire [ChatService.chat] call (all its LLM round trips). */
    fun usage(model: String?, inputTokens: Int, outputTokens: Int, totalTokens: Int): String =
        Json.encodeToString(
            buildJsonObject {
                put("type", "usage")
                put("model", model)
                put("inputTokens", inputTokens)
                put("outputTokens", outputTokens)
                put("totalTokens", totalTokens)
            }
        )
}
