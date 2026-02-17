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
) : ArrayList<EdgeType>(initialCapacity), EdgeCollection<NodeType, EdgeType> {

    override fun add(element: EdgeType): Boolean {
        // Make sure, the index is always set
        if (element.index == null) {
            element.index = this.size
        }

        val ok = super<ArrayList>.add(element)
        if (ok) {
            handleOnAdd(element)
        }
        return ok
    }

    override fun remove(element: EdgeType): Boolean {
        val ok = super<ArrayList>.remove(element)
        if (ok) {
            handleOnRemove(element)
        }
        return ok
    }

    override fun removeAll(elements: Collection<EdgeType>): Boolean {
        val ok = super.removeAll(elements.toSet())
        if (ok) {
            elements.forEach { handleOnRemove(it) }
        }
        return ok
    }

    override fun removeAt(index: Int): EdgeType {
        val edge = super.removeAt(index)
        handleOnRemove(edge)
        return edge
    }

    override fun removeIf(predicate: Predicate<in EdgeType>): Boolean {
        val edges = filter { predicate.test(it) }
        val ok = super<ArrayList>.removeIf(predicate)
        if (ok) {
            edges.forEach { handleOnRemove(it) }
        }
        return ok
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

    override fun clear() {
        // Make a copy of our edges so we can pass a copy to our on-remove handler
        val edges = this.toList()
        super.clear()
        edges.forEach { handleOnRemove(it) }
    }

    /**
     * This function creates a new edge (of [EdgeType]) to/from the specified node [target]
     * (depending on [outgoing]) and adds it to the specified index in the list.
     */
    fun add(index: Int, target: NodeType) {
        val edge = createEdge(target, init, this.outgoing)

        return add(index, edge)
    }

    override fun add(index: Int, element: EdgeType) {
        // Make sure, the index is always set
        element.index = this.size

        super<ArrayList>.add(index, element)

        handleOnAdd(element)

        // We need to re-compute all edges with an index > inserted index
        for (i in index until this.size) {
            this[i].index = i
        }
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
}
