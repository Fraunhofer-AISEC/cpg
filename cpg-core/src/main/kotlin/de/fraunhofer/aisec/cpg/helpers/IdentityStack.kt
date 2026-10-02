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

/**
 * A LIFO stack with O(1) [contains] by reference identity (see [IdentitySet] for why that matters
 * for [de.fraunhofer.aisec.cpg.graph.Node]s): an ordered list for push/pop/[top] plus an
 * [IdentitySet] for membership, kept in sync behind one interface.
 *
 * Each element may be on the stack at most once (as in Tarjan's SCC algorithm, its main user);
 * pushing an element that is already on the stack is not supported. Only a suffix is ever popped
 * (just [top], or everything down to a given element via [popThrough]).
 */
class IdentityStack<T> {
    private val elements = mutableListOf<T>()
    private val present = IdentitySet<T>()

    fun push(element: T) {
        elements.add(element)
        present.add(element)
    }

    operator fun contains(element: T) = element in present

    fun top(): T = elements.last()

    fun popTop(): T {
        val top = elements.removeAt(elements.lastIndex)
        present.remove(top)
        return top
    }

    /**
     * Pops every element from the top down to and including [element] (which must currently be on
     * the stack), returning them in pop order ([top] first, [element] last).
     */
    fun popThrough(element: T): List<T> {
        val index = elements.indexOfLast { it === element }
        require(index >= 0) { "element is not on the stack" }
        val suffix = elements.subList(index, elements.size)
        val popped = suffix.asReversed().toList()
        popped.forEach { present.remove(it) }
        suffix.clear()
        return popped
    }
}
