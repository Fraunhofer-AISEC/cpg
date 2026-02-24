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
import de.fraunhofer.aisec.cpg.graph.types.HasType.TypeObserver

/**
 * This interfaces is an extension of [MutableCollection] that holds specific functions for the
 * collection of [Edge] edges.
 */
interface EdgeCollection<NodeType : Node, EdgeType : Edge<NodeType>> : MutableCollection<EdgeType> {
    var thisRef: Node
    var init: (start: Node, end: NodeType) -> EdgeType
    var outgoing: Boolean
    var onAdd: ((EdgeType) -> Unit)?
    var onRemove: ((EdgeType) -> Unit)?

    /**
     * Removes all edges with the target node. The target is considered to be either the [Edge.end]
     * or [Edge.start] depending on [outgoing].
     */
    fun remove(target: NodeType): Boolean {
        val toRemove =
            this.filter {
                if (outgoing) {
                    it.end == target
                } else {
                    it.start == target
                }
            }
        return this.removeAll(toRemove.toSet())
    }

    fun addAll(targets: Collection<NodeType>, builder: (EdgeType.() -> Unit)? = null): Boolean {
        val edges =
            targets.map {
                val edge =
                    if (outgoing) {
                        init(thisRef, it)
                    } else {
                        @Suppress("UNCHECKED_CAST") init(it, thisRef as NodeType)
                    }
                // Apply builder
                if (builder != null) {
                    builder(edge)
                }
                edge
            }

        return addAll(edges)
    }

    /**
     * Creates a new edge with the target node and an optional [builder] to include edge properties.
     * If [outgoing] is true, the edge is created from [thisRef] -> [target], otherwise from
     * [target] to [thisRef].
     */
    fun add(
        target: NodeType,
        init: ((Node, NodeType) -> EdgeType) = this.init,
        builder: (EdgeType.() -> Unit)? = null,
    ): Boolean {
        val edge = createEdge(target, init, this.outgoing, builder)

        // Add it
        return this.add(edge)
    }

    fun <NodeType : Node, PropertyEdgeType : Edge<NodeType>> createEdge(
        target: NodeType,
        init: ((Node, NodeType) -> PropertyEdgeType),
        outgoing: Boolean = true,
        builder: (PropertyEdgeType.() -> Unit)? = null,
    ): PropertyEdgeType {
        val edge =
            if (outgoing) {
                init(thisRef, target)
            } else {
                @Suppress("UNCHECKED_CAST") init(target, thisRef as NodeType)
            }

        // Apply builder
        if (builder != null) {
            builder(edge)
        }

        return edge
    }

    fun contains(target: NodeType): Boolean {
        return any { if (outgoing) it.end == target else it.start == target }
    }

    operator fun plusAssign(end: NodeType) {
        add(end)
    }

    /** Clears the collection and adds the [nodes]. */
    fun resetTo(nodes: Collection<NodeType>) {
        clear()
        for (n in nodes) {
            this += n
        }
    }

    /**
     * Converts this collection of edges into a collection of nodes for easier access to the
     * "target" nodes.
     *
     * Note, that is an immutable list and only a snapshot. If you want a magic container that is in
     * sync with this [EdgeCollection], please use [unwrap].
     */
    fun toNodeCollection(predicate: ((EdgeType) -> Boolean)? = null): Collection<NodeType>

    /**
     * Returns an [UnwrappedEdgeCollection] magic container which holds a structure that provides
     * easy access to the "target" nodes without edge information, but is mutable and in-sync with
     * this collection.
     */
    fun unwrap(): UnwrappedEdgeCollection<NodeType, EdgeType>

    /**
     * This function will be executed after the edge was added to the container. This can be used to
     * propagate the edge to other properties or register additional handlers, e.g. a
     * [TypeObserver].
     */
    fun handleOnAdd(edge: EdgeType) {
        onAdd?.invoke(edge)
    }

    /**
     * This function will be executed after an edge was removed from the container. This can be used
     * to unregister additional handlers, e.g. a [TypeObserver].
     */
    fun handleOnRemove(edge: EdgeType) {
        onRemove?.invoke(edge)
    }
}

/** A helper function for [EdgeCollection.toNodeCollection]. */
internal fun <
    NodeType : Node,
    EdgeType : Edge<NodeType>,
    CollectionType : MutableCollection<NodeType>,
> internalToNodeCollection(
    edges: EdgeCollection<NodeType, EdgeType>,
    outgoing: Boolean = true,
    predicate: ((EdgeType) -> Boolean)? = null,
    createCollection: () -> CollectionType,
): CollectionType {
    val unwrapped = createCollection()
    for (edge in edges) {
        if (predicate != null && !predicate(edge)) {
            continue
        }

        @Suppress("UNCHECKED_CAST")
        unwrapped += if (outgoing) edge.end else edge.start as NodeType
    }

    return unwrapped
}

/**
 * This is a special hashcode implementation that takes into account whether this edge container is
 * containing incoming or outgoing edges. It builds the hash-code based on the direction of the
 * edges. The reason for this is to avoid loops in the hash-code implementation.
 */
internal fun <NodeType : Node> internalHashcode(
    edges: EdgeCollection<NodeType, out Edge<NodeType>>,
    outgoing: Boolean = true,
): Int {
    var hashCode = 0

    for (edge in edges) {
        hashCode =
            31 * hashCode +
                if (outgoing) {
                    edge.end.hashCode()
                } else {
                    edge.start.hashCode()
                }
    }

    return hashCode
}
