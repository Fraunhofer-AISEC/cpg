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

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable data class ChatMessageJSON(val role: String, val content: String)

@Serializable data class ChatRequestJSON(val messages: List<ChatMessageJSON>)

/**
 * ChatService wraps the McpClient to provide a clean API for chat interactions. Uses Ollama as the
 * LLM backend with MCP tools.
 */
class ChatService(private val mcpClient: McpClient? = null) {

    fun chat(request: ChatRequestJSON): Flow<String> {
        return if (mcpClient != null) {
            mcpClient.chat(request)
        } else {
            throw IllegalStateException("McpClient not initialized")
        }
    }
}
