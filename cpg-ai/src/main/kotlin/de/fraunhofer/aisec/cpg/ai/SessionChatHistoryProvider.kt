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

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.prompt.message.Message
import java.util.concurrent.ConcurrentHashMap

/**
 * [ChatHistoryProvider] for Koog's `ChatMemory` in [ChatService], keeping the history per
 * `sessionId` of [ChatRequestJSON] in memory (including tool-call/tool-result messages).
 */
class SessionChatHistoryProvider : ChatHistoryProvider {
    private val storage = ConcurrentHashMap<String, List<Message>>()

    override suspend fun store(conversationId: String, messages: List<Message>) {
        storage[conversationId] = messages
    }

    override suspend fun load(conversationId: String): List<Message> =
        storage[conversationId] ?: emptyList()

    /** Whether any history is stored for [sessionId]. */
    fun contains(sessionId: String): Boolean = storage.containsKey(sessionId)

    /** Remove the stored history for [sessionId]; a subsequent [load] returns empty. */
    fun evict(sessionId: String) {
        storage.remove(sessionId)
    }
}
