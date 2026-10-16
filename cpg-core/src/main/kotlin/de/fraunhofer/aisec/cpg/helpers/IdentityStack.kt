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
package de.fraunhofer.aisec.cpg.helpers

import java.util.IdentityHashMap

/**
 * A LIFO stack whose [contains] is a hash lookup by reference identity instead of a linear scan
 * (see [IdentitySet] for why identity matters for [de.fraunhofer.aisec.cpg.graph.Node]s).
 *
 * An element may be pushed more than once; [contains] stays true until every copy has been popped.
 * [push], [popTop] and [contains] are amortized expected O(1).
 */
class IdentityStack<T> {
    private val elements = mutableListOf<T>()
    /** How many copies of each element are currently on the stack; absent means zero. */
    private val counts = IdentityHashMap<T, Int>()

    fun push(element: T) {
        elements.add(element)
        counts.merge(element, 1, Int::plus)
    }

    operator fun contains(element: T) = counts.containsKey(element)

    fun top(): T = elements.last()

    fun popTop(): T {
        val top = elements.removeAt(elements.lastIndex)
        // Returning null from the remapping function removes the entry once the count hits zero.
        counts.computeIfPresent(top) { _, count -> (count - 1).takeIf { it > 0 } }
        return top
    }
}
