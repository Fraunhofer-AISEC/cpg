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
import kotlin.collections.AbstractMutableSet

/**
 * This class extends a list of property edges. This allows us to use list of property edges more
 * conveniently.
 */
abstract class EdgeSet<NodeType : Node, EdgeType : Edge<NodeType>>(
    override var thisRef: Node,
    override var init: (start: Node, end: NodeType) -> EdgeType,
    override var outgoing: Boolean = true,
    override var onAdd: ((EdgeType) -> Unit)? = null,
    override var onRemove: ((EdgeType) -> Unit)? = null,
    // We explicitly set the capacity to 1, as we expect that most nodes will only have one edge of
    // a given type. This is a common case for many edge types in the CPG, and setting the initial
    // capacity to 1 can save memory in these cases.
) : AbstractMutableSet<EdgeType>(), EdgeCollection<NodeType, EdgeType> {

    // Draft: compact 0/1/many storage to avoid eagerly allocating a HashSet for singleton cases.
    private var first: EdgeType? = null
    private var many: MutableSet<EdgeType>? = null

    override val size: Int
        get() = many?.size ?: if (first == null) 0 else 1

    override fun add(element: EdgeType): Boolean {
        return when {
            many != null -> {
                val ok = many!!.add(element)
                if (ok) {
                    handleOnAdd(element)
                }
                ok
            }
            first == null -> {
                first = element
                handleOnAdd(element)
                true
            }
            first == element -> false
            else -> {
                many =
                    HashSet<EdgeType>(2).also {
                        it.add(first!!)
                        it.add(element)
                    }
                first = null
                handleOnAdd(element)
                true
            }
        }
    }

    override fun iterator(): MutableIterator<EdgeType> {
        val singleton = first
        return if (many != null) {
            ManyIterator(many!!.iterator())
        } else {
            SingletonIterator(singleton)
        }
    }

    override fun contains(element: EdgeType): Boolean {
        return many?.contains(element) ?: (first == element)
    }

    override fun remove(element: EdgeType): Boolean {
        return when {
            many != null -> {
                val ok = many!!.remove(element)
                if (ok) {
                    handleOnRemove(element)
                    compactManyIfPossible()
                }
                ok
            }
            first == element -> {
                first = null
                handleOnRemove(element)
                true
            }
            else -> false
        }
    }

    override fun removeIf(predicate: Predicate<in EdgeType>): Boolean {
        val toRemove = this.filter { predicate.test(it) }
        return removeAll(toRemove)
    }

    override fun removeAll(elements: Collection<EdgeType>): Boolean {
        if (elements.isEmpty() || isEmpty()) {
            return false
        }

        var changed = false
        val toRemove = elements.toSet()
        val it = iterator()

        while (it.hasNext()) {
            val edge = it.next()
            if (edge in toRemove) {
                it.remove()
                changed = true
            }
        }

        return changed
    }

    override fun clear() {
        if (isEmpty()) {
            return
        }

        // Make a copy of our edges so we can pass a copy to our on-remove handler
        val edges = this.toSet()
        first = null
        many = null
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

    private fun compactManyIfPossible() {
        val current = many ?: return
        when (current.size) {
            0 -> many = null
            1 -> {
                first = current.first()
                many = null
            }
        }
    }

    private inner class ManyIterator(private val delegate: MutableIterator<EdgeType>) :
        MutableIterator<EdgeType> {
        private var last: EdgeType? = null

        override fun hasNext(): Boolean {
            return delegate.hasNext()
        }

        override fun next(): EdgeType {
            return delegate.next().also { last = it }
        }

        override fun remove() {
            val removed =
                last ?: throw IllegalStateException("next() must be called before remove()")
            delegate.remove()
            handleOnRemove(removed)
            compactManyIfPossible()
            last = null
        }
    }

    private inner class SingletonIterator(private val edge: EdgeType?) : MutableIterator<EdgeType> {
        private var consumed = false
        private var canRemove = false

        override fun hasNext(): Boolean {
            return !consumed && edge != null
        }

        override fun next(): EdgeType {
            if (!hasNext()) {
                throw NoSuchElementException()
            }

            consumed = true
            canRemove = true
            return edge!!
        }

        override fun remove() {
            if (!canRemove || edge == null) {
                throw IllegalStateException("next() must be called before remove()")
            }

            first = null
            handleOnRemove(edge)
            canRemove = false
        }
    }
}
