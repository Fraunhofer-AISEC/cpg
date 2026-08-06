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
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import kotlin.reflect.KProperty

/**
 * An intelligent [MutableList] wrapper around an [EdgeList] which supports iterating, adding and
 * removing [Node] elements.
 */
@Suppress("EqualsOrHashCode")
class UnwrappedEdgeList<NodeType : Node, EdgeType : Edge<NodeType>>(
    var list: EdgeList<NodeType, EdgeType>
) : UnwrappedEdgeCollection<NodeType, EdgeType>(list), MutableList<NodeType> {

    override fun add(index: Int, element: NodeType) {
        return list.add(index, element)
    }

    override fun addAll(index: Int, elements: Collection<NodeType>): Boolean {
        TODO("Not yet implemented")
    }

    override fun listIterator(): MutableListIterator<NodeType> {
        return ListIterator(list.listIterator())
    }

    override fun listIterator(index: Int): MutableListIterator<NodeType> {
        return ListIterator(list.listIterator(index))
    }

    override fun removeAt(index: Int): NodeType {
        val edge = list.removeAt(index)
        return if (list.outgoing) {
            edge.end
        } else {
            @Suppress("UNCHECKED_CAST")
            edge.start as NodeType
        }
    }

    override fun set(index: Int, element: NodeType): NodeType {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<NodeType> {
        return if (list.outgoing) {
            list.subList(fromIndex, toIndex).map { it.end }.toMutableList()
        } else {
            @Suppress("UNCHECKED_CAST")
            list.subList(fromIndex, toIndex).map { it.start as NodeType }.toMutableList()
        }
    }

    override fun get(index: Int): NodeType {
        var edge = list[index]
        return if (list.outgoing) {
            edge.end
        } else {
            @Suppress("UNCHECKED_CAST")
            edge.start as NodeType
        }
    }

    override fun indexOf(element: NodeType): Int {
        for (i in 0 until this.size) {
            if (element == this[i]) {
                return i
            }
        }

        return -1
    }

    override fun lastIndexOf(element: NodeType): Int {
        for (i in this.size - 1 downTo 0) {
            if (element == this[i]) {
                return i
            }
        }

        return -1
    }

    inner class ListIterator(var edgeIterator: MutableListIterator<EdgeType>) :
        MutableListIterator<NodeType> {
        override fun add(element: NodeType) {
            edgeIterator.add(list.createEdge(element, list.init, list.outgoing))
        }

        override fun hasNext(): Boolean {
            return edgeIterator.hasNext()
        }

        override fun hasPrevious(): Boolean {
            return edgeIterator.hasPrevious()
        }

        override fun next(): NodeType {
            var next = edgeIterator.next()
            return if (list.outgoing) {
                next.end
            } else {
                @Suppress("UNCHECKED_CAST")
                next.start as NodeType
            }
        }

        override fun remove() {
            return edgeIterator.remove()
        }

        override fun set(element: NodeType) {
            TODO("Not yet implemented")
        }

        override fun nextIndex(): Int {
            return edgeIterator.nextIndex()
        }

        override fun previous(): NodeType {
            var next = edgeIterator.previous()
            return if (list.outgoing) {
                next.end
            } else {
                @Suppress("UNCHECKED_CAST")
                next.start as NodeType
            }
        }

        override fun previousIndex(): Int {
            return edgeIterator.previousIndex()
        }
    }

    /**
     * Creates a new [Delegate] for this unwrapped list to be used in
     * [delegated properties](https://kotlinlang.org/docs/delegated-properties.html).
     */
    internal fun <ThisType : Node> delegate():
        UnwrappedEdgeList<NodeType, EdgeType>.Delegate<ThisType> {
        return Delegate<ThisType>()
    }

    /**
     * This class can be used to implement
     * [delegated properties](https://kotlinlang.org/docs/delegated-properties.html) in [Node]
     * classes. The most common use case is to have a property that is a list of [Edge] objects (for
     * persistence) and a second (delegated) property that allows easy access just to the connected
     * nodes of the individual edges for in-memory access. It should not be used directly, but
     * rather by using [unwrapping].
     *
     * For example:
     * ```kotlin
     * class MyNode {
     *   @Relationship(value = "EXPRESSIONS", direction = "OUTGOING")
     *   var expressionsEdges = astChildrenOf<Expression>()
     *   var expressions by unwrapping(MyNode::expressionsEdges)
     * }
     * ```
     *
     * This class is intentionally marked with [Transient], so that the delegated properties are not
     * transferred to the persistence layer. Only the property that contains the property edges
     * should be persisted in the graph database.
     */
    @DoNotPersist
    inner class Delegate<ThisType : Node>() {
        operator fun getValue(thisRef: ThisType, property: KProperty<*>): MutableList<NodeType> {
            return this@UnwrappedEdgeList
        }

        operator fun setValue(thisRef: ThisType, property: KProperty<*>, value: List<NodeType>) {
            this@UnwrappedEdgeList.resetTo(value)
        }
    }

    /**
     * Similar to [Delegate] but this employs dark voodoo magic to make an incoming list available
     * as delegate. This is a little bit dangerous because we need to cast the unwrapped listed to
     * the incoming type. The originating reason for this is that an [Edge] only has a generic type
     * parameter for the [Edge.end] property, but not for the [Edge.start] property. This is why we
     * need to cast the underlying [Edge.start] property to the incoming type.
     */
    @DoNotPersist
    inner class IncomingDelegate<ThisType : Node, IncomingType>() {
        operator fun getValue(
            thisRef: ThisType,
            property: KProperty<*>,
        ): MutableList<IncomingType> {
            @Suppress("UNCHECKED_CAST")
            return this@UnwrappedEdgeList as MutableList<IncomingType>
        }

        operator fun setValue(
            thisRef: ThisType,
            property: KProperty<*>,
            value: List<IncomingType>,
        ) {
            @Suppress("UNCHECKED_CAST")
            this@UnwrappedEdgeList.resetTo(value as Collection<NodeType>)
        }
    }

    operator fun <ThisType : Node> provideDelegate(
        thisRef: ThisType,
        prop: KProperty<*>,
    ): Delegate<ThisType> {
        return Delegate()
    }

    override fun equals(other: Any?): Boolean {
        return other is List<*> && this.iterator().asSequence().toList() == other
    }
}
