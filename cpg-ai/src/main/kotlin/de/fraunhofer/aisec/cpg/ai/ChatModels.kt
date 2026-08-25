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
package de.fraunhofer.aisec.cpg.ai

import ai.koog.agents.core.tools.annotations.LLMDescription
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject

@Serializable data class ChatMessageJSON(val role: String, val content: String)

@Serializable
data class ChatRequestJSON(
    val messages: List<ChatMessageJSON>,
    val client: String,
    val model: String,
)

@Serializable data class ToolSchemaJSON(val properties: JsonObject?, val required: List<String>?)

@Serializable
data class McpToolJSON(
    val name: String,
    val description: String?,
    val inputSchema: ToolSchemaJSON?,
)

@Serializable
data class PromptArgumentJSON(val name: String, val description: String?, val required: Boolean?)

@Serializable
data class McpPromptJSON(
    val name: String,
    val description: String?,
    val arguments: List<PromptArgumentJSON>?,
)

@Serializable
data class McpResourceJSON(
    val uri: String,
    val name: String?,
    val description: String?,
    val mimeType: String?,
)

@Serializable
data class McpCapabilitiesJSON(
    val serverName: String,
    val serverVersion: String,
    val tools: List<McpToolJSON>,
    val prompts: List<McpPromptJSON>,
    val resources: List<McpResourceJSON>,
)

@Serializable
data class Skill(
    val name: String,
    val description: String,
    val body: String,
    @Transient val location: Path? = null,
)

/**
 * Structured completion signal requested once from the LLM whenever [ChatService.chatStrategy]
 * believes a turn is finished (see [ChatService.requestTaskStatus]), as a more reliable alternative
 * to a caller inferring completion by re-parsing whatever the skill happened to persist to disk.
 * Deliberately skill-agnostic - the same "resolved identifiers" shape covers both tag-library's
 * tagged/skipped function names and match-library's matched/unmatched ones - matching the existing
 * [ChatService]-level design principle (see `historyCompressionConcepts`).
 */
@Serializable
@LLMDescription(
    "The current status of the requested task, reported once you believe you have finished (or " +
        "given up on) everything asked of you."
)
data class TaskStatus(
    @property:LLMDescription(
        "True only if every requested item has been addressed - either completed or explicitly " +
            "given up on as unresolvable. False if meaningful work remains."
    )
    val done: Boolean,
    @property:LLMDescription(
        "Identifiers (e.g. function names) that were completed or explicitly given up on/skipped " +
            "this run, written exactly as used elsewhere in this conversation."
    )
    val resolvedItems: List<String> = emptyList(),
)
