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

/**
 * This is a MAJOR workaround since Neo4J OGM does not allow to use our (generic) [Edge] class for
 * our AST edges. See https://github.com/neo4j/neo4j-ogm/issues/1132.
 *
 * Therefore, we need to wrap the edge in a list with a single element.
 */
open class EdgeSingletonList<
    NodeType : Node,
    NullableNodeType : NodeType?,
    EdgeType : Edge<NodeType>,
>(
    override var thisRef: Node,
    override var init: (Node, NodeType) -> EdgeType,
    var onChanged: ((old: EdgeType?, new: EdgeType?) -> Unit)? = null,
    override var outgoing: Boolean,
    of: NullableNodeType,
) : EdgeCollection<NodeType, EdgeType> {

    var element: EdgeType? =
        if (of == null) {
            null
        } else {
            if (outgoing) {
                init(thisRef, of)
            } else {
                @Suppress("UNCHECKED_CAST") init(of, thisRef as NodeType)
            }
        }

    override val size: Int
        get() = if (element == null) 0 else 1

    override fun contains(element: EdgeType): Boolean {
        return this.element == element
    }

    override fun containsAll(elements: Collection<EdgeType>): Boolean {
        return elements.size == 1 && this.element == elements.firstOrNull()
    }

    override fun isEmpty(): Boolean {
        return this.element == null
    }

    override fun add(element: EdgeType): Boolean {
        if (this.element == null) {
            this.element = element
            onChanged?.invoke(null, this.element)
            return true
        } else {
            throw UnsupportedOperationException(
                "We cannot 'add' to a singleton edge list, that is already populated"
            )
        }
    }

    override fun addAll(elements: Collection<EdgeType>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        // TODO: is this correct?
        // Make a copy of our edge so we can pass a copy to our on-remove handler
        val old = this.element
        this.element = null
        old?.let { handleOnRemove(it) }
    }

    override fun iterator(): MutableIterator<EdgeType> {
        return Iterator(element)
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

    override var onAdd: ((EdgeType) -> Unit)?
        get() = null
        set(_) {}

    override var onRemove: ((EdgeType) -> Unit)?
        get() = null
        set(_) {}

    override fun toNodeCollection(predicate: ((EdgeType) -> Boolean)?): Collection<NodeType> {
        val elements = predicate?.let { toList().filter(it) } ?: toList()
        return elements.map {
            if (outgoing) {
                it.end
            } else {
                @Suppress("UNCHECKED_CAST")
                it.start as NodeType
            }
        }
    }

    override fun unwrap(): UnwrappedEdgeCollection<NodeType, EdgeType> {
        TODO("Not yet implemented")
    }

    inner class Iterator(val element: EdgeType?) : MutableIterator<EdgeType> {
        var hasNext = isNotEmpty()

        override fun remove() {
            throw UnsupportedOperationException()
        }

        override fun hasNext(): Boolean {
            return hasNext
        }

        override fun next(): EdgeType {
            if (hasNext && element != null) {
                hasNext = false
                return element
            }
            throw NoSuchElementException()
        }
    }

    fun resetTo(node: NodeType) {
        val old = this.element
        this.element =
            if (outgoing) {
                init(thisRef, node)
            } else {
                @Suppress("UNCHECKED_CAST") init(node, thisRef as NodeType)
            }
        onChanged?.invoke(old, this.element)

        val element = this.element
        if (element != null) {
            handleOnAdd(element)
        } else if (old != null) {
            handleOnRemove(old)
        }
    }

    fun <ThisType : Node> delegate(): UnwrapDelegate<ThisType> {
        return UnwrapDelegate()
    }

    @Transient
    inner class UnwrapDelegate<ThisType : Node>() {
        @Suppress("UNCHECKED_CAST")
        operator fun getValue(thisRef: ThisType, property: KProperty<*>): NullableNodeType {
            return (if (outgoing) {
                this@EdgeSingletonList.element?.end
            } else {
                this@EdgeSingletonList.element?.start as NodeType?
            })
                as NullableNodeType
        }

        operator fun setValue(thisRef: ThisType, property: KProperty<*>, value: NullableNodeType) {
            if (value != null) {
                this@EdgeSingletonList.resetTo(value)
            }
        }
    }
}
