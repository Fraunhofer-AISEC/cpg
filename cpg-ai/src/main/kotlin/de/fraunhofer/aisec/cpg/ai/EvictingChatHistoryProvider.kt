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
 * In-memory [ChatHistoryProvider] that additionally exposes [evict] to drop a single session's
 * stored history. Backs Koog's `ChatMemory` feature inside [ChatService] so that tool-call and
 * tool-result messages (which [ChatService.toChatMessageJsonOrNull] drops) survive across the
 * per-call agent rebuild - the root cause of the repeated `activate_skill` calls.
 *
 * One instance lives for the lifetime of a [ChatService] (one per DUST batch). Sessions are keyed
 * by the `sessionId` string the caller passes via [ChatRequestJSON]; [evict] is called by
 * [ChatService.evictSession] when the batch finishes (success or failure) so memory doesn't leak
 * across batches.
 */
class EvictingChatHistoryProvider : ChatHistoryProvider {
    private val storage = ConcurrentHashMap<String, List<Message>>()

    override suspend fun store(conversationId: String, messages: List<Message>) {
        storage[conversationId] = messages
    }

    override suspend fun load(conversationId: String): List<Message> =
        storage[conversationId] ?: emptyList()

    /** Remove the stored history for [sessionId]; a subsequent [load] returns empty. */
    fun evict(sessionId: String) {
        storage.remove(sessionId)
    }
}
