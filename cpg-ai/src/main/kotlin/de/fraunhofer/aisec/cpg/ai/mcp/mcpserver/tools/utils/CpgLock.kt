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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Guards the shared CPG ([de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.globalAnalysisResult])
 * against concurrent access from parallel MCP calls, e.g. from several chat sessions a host
 * application runs at once.
 *
 * The graph is not thread-safe: its edge collections are plain lists/sets, so a traversal running
 * while another thread attaches an overlay or edge can throw a `ConcurrentModificationException`
 * (reproduced with a handful of hot nodes), and replacing the analysis result while a tool still
 * walks the old graph is worse. Hence:
 * - [read] is shared: any number of tools may query the graph at the same time.
 * - [write] is exclusive: a tool that mutates the graph, replaces it, or does a read-modify-write
 *   on a file shared with other tools runs alone.
 *
 * Tools registered through [addTool] take the right side automatically (see its `mutating`
 * parameter); take it manually only for work outside an MCP tool call. Both sides are reentrant,
 * and a [write] holder may call [read]. Going the other way - [write] while the same thread holds
 * [read]
 * - would deadlock, so it throws instead.
 */
object CpgLock {
    private val lock = ReentrantReadWriteLock()

    /** Runs [block] with shared access to the graph. */
    fun <T> read(block: () -> T): T {
        val readLock = lock.readLock()
        readLock.lock()
        try {
            return block()
        } finally {
            readLock.unlock()
        }
    }

    /** Runs [block] with exclusive access to the graph. */
    fun <T> write(block: () -> T): T {
        check(lock.readHoldCount == 0 || lock.isWriteLockedByCurrentThread) {
            "Cannot take the CPG write lock while holding its read lock (it would deadlock). " +
                "Register the tool with mutating = true instead of nesting write inside read."
        }
        val writeLock = lock.writeLock()
        writeLock.lock()
        try {
            return block()
        } finally {
            writeLock.unlock()
        }
    }

    /** [write] if [mutating], else [read]. */
    fun <T> withAccess(mutating: Boolean, block: () -> T): T =
        if (mutating) write(block) else read(block)
}
