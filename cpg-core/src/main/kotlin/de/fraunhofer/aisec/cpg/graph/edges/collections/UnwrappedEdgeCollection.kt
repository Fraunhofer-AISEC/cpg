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

/**
 * An intelligent [MutableCollection] wrapper around an [EdgeCollection] which supports iterating,
 * adding and removing [Node] elements. Basis for [UnwrappedEdgeList] and [UnwrappedEdgeSet].
 */
@Suppress("EqualsOrHashCode")
sealed class UnwrappedEdgeCollection<NodeType : Node, EdgeType : Edge<NodeType>>(
    var collection: EdgeCollection<NodeType, EdgeType>
) : MutableCollection<NodeType> {

    fun add(element: NodeType, builder: (EdgeType.() -> Unit)? = null): Boolean {
        return collection.add(element, builder = builder)
    }

    override fun add(element: NodeType): Boolean {
        return collection.add(element)
    }

    override fun addAll(elements: Collection<NodeType>): Boolean {
        return collection.addAll(elements)
    }

    override fun clear() {
        return collection.clear()
    }

    override fun iterator(): MutableIterator<NodeType> {
        return Iterator(collection.iterator())
    }

    override fun remove(element: NodeType): Boolean {
        return collection.remove(element)
    }

    override fun removeAll(elements: Collection<NodeType>): Boolean {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<NodeType>): Boolean {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = collection.size

    override fun contains(element: NodeType): Boolean {
        return collection.contains(element)
    }

    override fun containsAll(elements: Collection<NodeType>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        return collection.isEmpty()
    }

    fun resetTo(c: Collection<NodeType>) {
        return collection.resetTo(c)
    }

    override fun toString() = this.iterator().asSequence().toList().toString()

    inner class Iterator(var edgeIterator: MutableIterator<Edge<NodeType>>) :
        MutableIterator<NodeType> {
        override fun remove() {
            return edgeIterator.remove()
        }

        override fun hasNext(): Boolean {
            return edgeIterator.hasNext()
        }

        @Suppress("UNCHECKED_CAST")
        override fun next(): NodeType {
            val next = edgeIterator.next()
            return if (collection.outgoing) {
                next.end
            } else {
                next.start as NodeType
            }
        }
    }

    override fun hashCode(): Int {
        // Calculating a real hash code is very performance intensive, so we just return the
        // collection size. This will lead to some hash collisions but its not a problem since equal
        // will be used to differentiate between two collections then.

        return collection.size
    }

    abstract override fun equals(other: Any?): Boolean
}
