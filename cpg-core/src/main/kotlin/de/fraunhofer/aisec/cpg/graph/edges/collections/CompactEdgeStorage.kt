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
package de.fraunhofer.aisec.cpg.graph.edges.collections

/**
 * Shared helper for compact 0/1/many edge storage.
 *
 * In the common singleton case we avoid allocating a full list/set object and only materialize a
 * collection when we transition to "many" elements.
 */
internal class CompactEdgeStorage<EdgeType, ManyType : MutableCollection<EdgeType>>(
    private val createMany: (capacity: Int) -> ManyType
) {
    var first: EdgeType? = null
    var many: ManyType? = null

    val size: Int
        get() = many?.size ?: if (first == null) 0 else 1

    fun ensureMany(minCapacity: Int = 2): ManyType {
        val existing = many
        if (existing != null) {
            return existing
        }

        val created = createMany(minCapacity)
        first?.let { created.add(it) }
        first = null
        many = created
        return created
    }

    fun clearAndSnapshot(): List<EdgeType> {
        val snapshot = many?.toList() ?: first?.let { listOf(it) } ?: emptyList()
        first = null
        many = null
        return snapshot
    }

    fun compactManyToSingleton(singleExtractor: (ManyType) -> EdgeType?) {
        val current = many ?: return
        when (current.size) {
            0 -> many = null
            1 -> {
                first = singleExtractor(current)
                many = null
            }
        }
    }
}
