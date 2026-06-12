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
import kotlin.collections.AbstractMutableList

/** This class extends a list of edges. This allows us to use lists of edges more conveniently. */
abstract class EdgeList<NodeType : Node, EdgeType : Edge<NodeType>>(
    override var thisRef: Node,
    override var init: (start: Node, end: NodeType) -> EdgeType,
    override var outgoing: Boolean = true,
    override var onAdd: ((EdgeType) -> Unit)? = null,
    override var onRemove: ((EdgeType) -> Unit)? = null,
    /**
     * We allow to explicitly set the capacity. In most cases, 1 is fine as we expect that most
     * nodes will only have one edge of a given type. This is a common case for many edge types in
     * the CPG, and setting the initial capacity to 1 can save memory in these cases. In cases where
     * we expect more edges, we can increase the capacity to avoid unnecessary and expensive copy
     * operations to a larger list.
     */
    initialCapacity: Int = 1,
) : AbstractMutableList<EdgeType>(), EdgeCollection<NodeType, EdgeType> {

    // Draft: compact 0/1/many storage to avoid allocating an ArrayList per node in the common case.
    private var first: EdgeType? = null
    private var many: MutableList<EdgeType>? = null
    private var cachedCapacity: Int = initialCapacity

    override val size: Int
        get() = many?.size ?: if (first == null) 0 else 1

    override fun get(index: Int): EdgeType {
        return when {
            many != null -> many!![index]
            index == 0 && first != null -> first!!
            else -> throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
    }

    override fun add(element: EdgeType): Boolean {
        if (element.index == null) {
            element.index = this.size
        }

        when {
            many != null -> many!!.add(element)
            first == null -> first = element
            else -> {
                many =
                    ArrayList<EdgeType>(maxOf(cachedCapacity, 2)).also {
                        it.add(first!!)
                        it.add(element)
                    }
                first = null
            }
        }

        handleOnAdd(element)
        return true
    }

    override fun add(index: Int, element: EdgeType) {
        if (index < 0 || index > size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }

        when {
            many != null -> many!!.add(index, element)
            first == null -> {
                if (index != 0) throw IndexOutOfBoundsException("Index: $index, Size: $size")
                first = element
            }
            else -> {
                val prev = first!!
                many =
                    ArrayList<EdgeType>(maxOf(cachedCapacity, 2)).also {
                        if (index == 0) {
                            it.add(element)
                            it.add(prev)
                        } else {
                            it.add(prev)
                            it.add(element)
                        }
                    }
                first = null
            }
        }

        handleOnAdd(element)
        updateIndicesFrom(index)
    }

    override fun removeAt(index: Int): EdgeType {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }

        val removed =
            when {
                many != null -> {
                    val r = many!!.removeAt(index)
                    if (many!!.size == 1) {
                        first = many!![0]
                        many = null
                    }
                    r
                }
                else -> {
                    val r = first!!
                    first = null
                    r
                }
            }

        handleOnRemove(removed)
        updateIndicesFrom(index)
        return removed
    }

    override fun set(index: Int, element: EdgeType): EdgeType {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }

        return when {
            many != null -> {
                val prev = many!![index]
                many!![index] = element
                prev
            }
            else -> {
                val prev = first!!
                first = element
                prev
            }
        }
    }

    override fun remove(element: EdgeType): Boolean {
        val idx = indexOf(element)
        if (idx == -1) {
            return false
        }

        removeAt(idx)
        return true
    }

    override fun removeAll(elements: Collection<EdgeType>): Boolean {
        if (elements.isEmpty() || isEmpty()) {
            return false
        }

        val toRemove = elements.toSet()
        var changed = false

        for (i in size - 1 downTo 0) {
            if (this[i] in toRemove) {
                removeAt(i)
                changed = true
            }
        }

        return changed
    }

    override fun clear() {
        if (isEmpty()) {
            return
        }

        val edges = this.toList()
        first = null
        many = null
        edges.forEach { handleOnRemove(it) }
    }

    override fun removeIf(predicate: Predicate<in EdgeType>): Boolean {
        val toRemove = this.filter { predicate.test(it) }
        return removeAll(toRemove)
    }

    /** Replaces the first occurrence of an edge with [old] with a new edge to [new]. */
    fun replace(old: NodeType, new: NodeType): Boolean {
        val idx = this.indexOfFirst { it.end == old }
        if (idx != -1) {
            this[idx] = init(thisRef, new)
            return true
        }

        return false
    }

    /**
     * This function creates a new edge (of [EdgeType]) to/from the specified node [target]
     * (depending on [outgoing]) and adds it to the specified index in the list.
     */
    fun add(index: Int, target: NodeType) {
        val edge = createEdge(target, init, this.outgoing)

        return add(index, edge)
    }

    override fun toNodeCollection(predicate: ((EdgeType) -> Boolean)?): List<NodeType> {
        return internalToNodeCollection(this, outgoing, predicate, ::ArrayList)
    }

    /**
     * Returns an [UnwrappedEdgeList] magic container which holds a structure that provides easy
     * access to the "target" nodes without edge information.
     */
    override fun unwrap(): UnwrappedEdgeList<NodeType, EdgeType> {
        return UnwrappedEdgeList(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EdgeList<*, *>) return false

        // Otherwise, try to compare the contents of the lists with the propertyEquals method
        if (this.size == other.size) {
            for (i in this.indices) {
                if (this[i] != other[i]) {
                    return false
                }
            }
            return true
        }

        return false
    }

    override fun hashCode(): Int {
        return internalHashcode(this, outgoing)
    }

    private fun updateIndicesFrom(startIndex: Int) {
        if (startIndex < 0) {
            return
        }

        for (i in startIndex until size) {
            this[i].index = i
        }
    }
}
