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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.nodes

/**
 * Finds a node of this graph by its id, via an index that is built on first use.
 *
 * `result.nodes.find { it.id.toString() == id }` walks the whole AST and allocates a string per
 * node for every lookup; on a 190,000-node graph (FLAC) that took 0.6 to 1.3 seconds each, against
 * about a second to build the index once and microseconds per lookup afterwards. Tools look nodes
 * up by id constantly, some of them while holding [CpgLock.write].
 *
 * Only AST nodes are indexed, like `result.nodes`; overlay nodes are not. The index stays valid
 * while the AST does not change, which is true of everything that only attaches overlays and edges
 * (applying concepts). Whatever can add or remove AST nodes - running a pass, replacing the graph -
 * must call [NodeIndex.invalidate]; the tools in this module do.
 */
fun TranslationResult.findNodeById(id: String): AstNode? = NodeIndex.find(this, id)

object NodeIndex {
    private class Snapshot(val result: TranslationResult, val byId: Map<String, AstNode>)

    @Volatile private var snapshot: Snapshot? = null

    /** How many times the index was built; for tests. */
    @Volatile internal var buildCount = 0

    fun find(result: TranslationResult, id: String): AstNode? {
        val current =
            snapshot?.takeIf { it.result === result }
                ?: synchronized(this) {
                    // Another thread may have built it while this one waited for the monitor.
                    snapshot?.takeIf { it.result === result }
                        ?: build(result).also { snapshot = it }
                }
        return current.byId[id]
    }

    /** Drops the index; the next lookup rebuilds it. Call after the AST changed. */
    fun invalidate() {
        snapshot = null
    }

    private fun build(result: TranslationResult): Snapshot {
        val byId = HashMap<String, AstNode>()
        // The first node wins, like the linear search this replaces.
        result.nodes.forEach { byId.putIfAbsent(it.id.toString(), it) }
        buildCount++
        return Snapshot(result, byId)
    }
}
