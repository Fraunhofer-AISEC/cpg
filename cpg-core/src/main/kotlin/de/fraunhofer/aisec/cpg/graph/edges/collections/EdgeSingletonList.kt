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
import kotlin.reflect.KProperty
import org.neo4j.ogm.annotation.Transient

class EdgeSingletonList<NodeType : Node, EdgeType : Edge<NodeType>>(
    override var thisRef: Node,
    override var init: (Node, NodeType) -> EdgeType,
    override var outgoing: Boolean,
    of: NodeType,
    override var postAdd: ((EdgeType) -> Unit)? = null,
    override var postRemove: ((EdgeType) -> Unit)? = null
) : EdgeCollection<NodeType, EdgeType> {

    var element: EdgeType =
        if (outgoing) {
            init(thisRef, of)
        } else {
            @Suppress("UNCHECKED_CAST") init(of, thisRef as NodeType)
        }

    override val size: Int
        get() = 1

    override fun contains(element: EdgeType): Boolean {
        return this.element == element
    }

    override fun containsAll(elements: Collection<EdgeType>): Boolean {
        return this.element == elements.firstOrNull()
    }

    override fun isEmpty(): Boolean {
        return false
    }

    override fun add(element: EdgeType): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(elements: Collection<EdgeType>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        throw UnsupportedOperationException()
    }

    override fun iterator(): MutableIterator<EdgeType> {
        return Iterator(element)
    }

    inner class Iterator(val element: EdgeType) : MutableIterator<EdgeType> {
        var hasNext = true

        override fun remove() {
            throw UnsupportedOperationException()
        }

        override fun hasNext(): Boolean {
            return hasNext
        }

        override fun next(): EdgeType {
            if (hasNext) {
                hasNext = false
                return element
            }
            throw NoSuchElementException()
        }
    }

    override fun remove(element: EdgeType): Boolean {
        throw UnsupportedOperationException()
    }

    override fun removeAll(elements: Collection<EdgeType>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun retainAll(elements: Collection<EdgeType>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun toNodeCollection(outgoing: Boolean): Collection<NodeType> {
        var node =
            if (outgoing) {
                this.element.end
            } else {
                this.element.start as NodeType
            }
        return listOf(node)
    }

    override fun unwrap(): UnwrappedEdgeCollection<NodeType, EdgeType> {
        throw UnsupportedOperationException()
    }

    fun resetTo(node: NodeType) {
        this.element =
            if (outgoing) {
                init(thisRef, node)
            } else {
                @Suppress("UNCHECKED_CAST") init(node, thisRef as NodeType)
            }
    }

    fun <ThisType : Node> delegate(): Delegate<ThisType> {
        return Delegate()
    }

    @Transient
    inner class Delegate<
        ThisType : Node,
    >() {
        @Suppress("UNCHECKED_CAST")
        operator fun getValue(thisRef: ThisType, property: KProperty<*>): NodeType {
            return if (outgoing) {
                this@EdgeSingletonList.element.end
            } else {
                this@EdgeSingletonList.element.start as NodeType
            }
        }

        operator fun setValue(thisRef: ThisType, property: KProperty<*>, value: NodeType) {
            this@EdgeSingletonList.resetTo(value)
        }
    }
}
