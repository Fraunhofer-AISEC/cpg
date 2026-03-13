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
package de.fraunhofer.aisec.codyze.console.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable data class ChatMessageJSON(val role: String, val content: String)

@Serializable data class ChatRequestJSON(val messages: List<ChatMessageJSON>)

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
