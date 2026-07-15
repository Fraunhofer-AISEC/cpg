/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import java.util.function.Predicate

/**
 * This class extends a set of property edges. This allows us to use sets of property edges more
 * conveniently.
 *
 * Measurements across representative test corpora show that the overwhelming majority of edge sets
 * (evaluation order, dataflow, control- and program-dependence, overlays, ...) hold either 0, 1 or
 * 2 elements per node, with a long tail of much larger sets for a small minority of nodes (e.g. a
 * branch condition that control-depends hundreds of statements). A plain [HashSet] pays for a full
 * hash table entry ([java.util.HashMap.Node], i.e. hash + key + value + next pointer) per element
 * even when there is only one. To avoid that overhead for the common case, this class stores up to
 * two elements directly in fields and only falls back to a real [HashSet] once a third distinct
 * element is added.
 */
abstract class EdgeSet<NodeType : Node, EdgeType : Edge<NodeType>>(
    override var thisRef: Node,
    override var init: (start: Node, end: NodeType) -> EdgeType,
    override var outgoing: Boolean = true,
    override var onAdd: ((EdgeType) -> Unit)? = null,
    override var onRemove: ((EdgeType) -> Unit)? = null,
) : AbstractMutableSet<EdgeType>(), EdgeCollection<NodeType, EdgeType> {

    private var elem0: EdgeType? = null
    private var elem1: EdgeType? = null
    private var overflow: HashSet<EdgeType>? = null

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

    override fun contains(element: EdgeType): Boolean {
        return elem0 == element || elem1 == element || overflow?.contains(element) == true
    }

    override fun iterator(): MutableIterator<EdgeType> = EdgeSetIterator()

    /** Mutates the backing storage only, without triggering [onAdd]/[onRemove] notifications. */
    private fun addInternal(element: EdgeType): Boolean {
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
            else -> {
                val of = overflow ?: HashSet<EdgeType>().also { overflow = it }
                of.add(element)
            }
        }
    }

    private fun removeInternal(element: EdgeType): Boolean {
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

    override fun add(element: EdgeType): Boolean {
        val ok = addInternal(element)
        if (ok) {
            handleOnAdd(element)
        }
        return ok
    }

    override fun remove(element: EdgeType): Boolean {
        val ok = removeInternal(element)
        if (ok) {
            handleOnRemove(element)
        }
        return ok
    }

    override fun removeIf(predicate: Predicate<in EdgeType>): Boolean {
        val edges = filter { predicate.test(it) }
        var ok = false
        for (edge in edges) {
            if (removeInternal(edge)) {
                ok = true
            }
        }
        if (ok) {
            edges.forEach { handleOnRemove(it) }
        }
        return ok
    }

    override fun removeAll(elements: Collection<EdgeType>): Boolean {
        var ok = false
        for (edge in elements.toSet()) {
            if (removeInternal(edge)) {
                ok = true
            }
        }
        if (ok) {
            elements.forEach { handleOnRemove(it) }
        }
        return ok
    }

    override fun clear() {
        // Make a copy of our edges so we can pass a copy to our on-remove handler
        val edges = this.toSet()
        elem0 = null
        elem1 = null
        overflow = null
        edges.forEach { handleOnRemove(it) }
    }

    override fun toNodeCollection(predicate: ((EdgeType) -> Boolean)?): MutableSet<NodeType> {
        return internalToNodeCollection(this, outgoing, predicate, ::HashSet)
    }

    /**
     * Returns an [UnwrappedEdgeSet] magic container which holds a structure that provides easy
     * access to the "target" nodes without edge information.
     */
    override fun unwrap(): UnwrappedEdgeSet<NodeType, EdgeType> {
        return UnwrappedEdgeSet(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EdgeSet<*, *>) return false

        // Otherwise, try to compare the contents of the lists with the propertyEquals method
        return this.containsAll(other)
    }

    override fun hashCode(): Int {
        return internalHashcode(this, outgoing)
    }

    private inner class EdgeSetIterator : MutableIterator<EdgeType> {
        /** 0: about to yield [elem0], 1: about to yield [elem1], 2: yielding [overflow]. */
        private var stage = 0
        private var overflowIterator: MutableIterator<EdgeType>? = null
        private var lastReturned: EdgeType? = null
        private var lastFromOverflow = false

        private fun skipEmptyStages() {
            if (stage == 0 && elem0 == null) stage = 1
            if (stage == 1 && elem1 == null) stage = 2
        }

        private fun ensureOverflowIterator(): MutableIterator<EdgeType>? {
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

        override fun next(): EdgeType {
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
            // Note: like the previous HashSet-based implementation, removal via the iterator does
            // not trigger the onRemove notification (unlike remove()/removeAll()/clear() below,
            // which do) - this matches the behavior of java.util.HashSet's iterator, which
            // EdgeSet used to inherit from directly.
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
