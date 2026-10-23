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
 * A [MutableSet] optimized for fields that are empty for the overwhelming majority of instances and
 * hold only one or two elements otherwise (e.g. [de.fraunhofer.aisec.cpg.graph.Node.assumptions] or
 * [de.fraunhofer.aisec.cpg.graph.Node.additionalProblems], which measurements show are empty for
 * 98%+ of nodes). Elements are stored directly in two fields, avoiding the overhead of a
 * [HashMap.Node] hash-table entry per element that a plain [HashSet] would pay even for a single
 * element. Only once a third distinct element is added does this fall back to a real [HashSet].
 */
class SmallMutableSet<T : Any> : MutableSet<T> {
    private var elem0: T? = null
    private var elem1: T? = null
    private var overflow: HashSet<T>? = null

    override val size: Int
        get() {
            var count = 0
            if (elem0 != null) count++
            if (elem1 != null) count++
            return count + (overflow?.size ?: 0)
        }

    override fun isEmpty(): Boolean {
        return elem0 == null && elem1 == null && overflow.isNullOrEmpty()
    }

    override fun contains(element: T): Boolean {
        return elem0 == element || elem1 == element || overflow?.contains(element) == true
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return elements.all { contains(it) }
    }

    override fun add(element: T): Boolean {
        if (contains(element)) return false
        return when {
            elem0 == null -> {
                elem0 = element
                true
            }
            elem1 == null -> {
                elem1 = element
                true
            }
            else -> (overflow ?: HashSet<T>().also { overflow = it }).add(element)
        }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        var modified = false
        for (e in elements) {
            if (add(e)) {
                modified = true
            }
        }
        return modified
    }

    override fun remove(element: T): Boolean {
        return when {
            elem0 == element -> {
                elem0 = null
                true
            }
            elem1 == element -> {
                elem1 = null
                true
            }
            else -> {
                val removed = overflow?.remove(element) == true
                if (overflow?.isEmpty() == true) {
                    overflow = null
                }
                removed
            }
        }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var modified = false
        for (e in elements) {
            if (remove(e)) {
                modified = true
            }
        }
        return modified
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        var modified = false
        val it = iterator()
        while (it.hasNext()) {
            if (it.next() !in elements) {
                it.remove()
                modified = true
            }
        }
        return modified
    }

    override fun clear() {
        elem0 = null
        elem1 = null
        overflow = null
    }

    override fun iterator(): MutableIterator<T> = SmallMutableSetIterator()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Set<*>) return false
        return this.size == other.size && this.containsAll(other)
    }

    override fun hashCode(): Int {
        var hash = 0
        for (e in this) {
            hash += e.hashCode()
        }
        return hash
    }

    private inner class SmallMutableSetIterator : MutableIterator<T> {
        /** 0: about to yield [elem0], 1: about to yield [elem1], 2: yielding [overflow]. */
        private var stage = 0
        private var overflowIterator: MutableIterator<T>? = null
        private var lastReturned: T? = null
        private var lastFromOverflow = false

        private fun skipEmptyStages() {
            if (stage == 0 && elem0 == null) stage = 1
            if (stage == 1 && elem1 == null) stage = 2
        }

        private fun ensureOverflowIterator(): MutableIterator<T>? {
            var it = overflowIterator
            if (it == null) {
                it = overflow?.iterator()
                overflowIterator = it
            }
            return it
        }

        override fun hasNext(): Boolean {
            skipEmptyStages()
            return when (stage) {
                0 -> elem0 != null
                1 -> elem1 != null
                else -> ensureOverflowIterator()?.hasNext() == true
            }
        }

        override fun next(): T {
            skipEmptyStages()
            return when (stage) {
                0 -> {
                    val value = elem0 ?: throw NoSuchElementException()
                    lastReturned = value
                    lastFromOverflow = false
                    stage = 1
                    value
                }
                1 -> {
                    val value = elem1 ?: throw NoSuchElementException()
                    lastReturned = value
                    lastFromOverflow = false
                    stage = 2
                    value
                }
                else -> {
                    val it = ensureOverflowIterator() ?: throw NoSuchElementException()
                    val value = it.next()
                    lastReturned = value
                    lastFromOverflow = true
                    value
                }
            }
        }

        override fun remove() {
            val value = lastReturned ?: throw IllegalStateException("next() has not been called")
            if (lastFromOverflow) {
                overflowIterator?.remove()
                if (overflow?.isEmpty() == true) {
                    overflow = null
                }
            } else if (elem0 == value) {
                elem0 = null
            } else if (elem1 == value) {
                elem1 = null
            }
            lastReturned = null
        }
    }
}

/** Returns a new empty [SmallMutableSet]. See its documentation for the intended use case. */
fun <T : Any> smallMutableSetOf(): MutableSet<T> = SmallMutableSet()
