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
 * This class extends a list of property edges. This allows us to use list of property edges more
 * conveniently.
 */
abstract class EdgeSet<NodeType : Node, EdgeType : Edge<NodeType>>(
    override var thisRef: Node,
    override var init: (start: Node, end: NodeType) -> EdgeType,
    override var outgoing: Boolean = true,
    override var onAdd: ((EdgeType) -> Unit)? = null,
    override var onRemove: ((EdgeType) -> Unit)? = null,
) : HashSet<EdgeType>(), EdgeCollection<NodeType, EdgeType> {
    override fun add(element: EdgeType): Boolean {
        val ok = super<HashSet>.add(element)
        if (ok) {
            handleOnAdd(element)
        }
        return ok
    }

    override fun removeIf(predicate: Predicate<in EdgeType>): Boolean {
        val edges = filter { predicate.test(it) }
        val ok = super<HashSet>.removeIf(predicate)
        if (ok) {
            edges.forEach { handleOnRemove(it) }
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

    override fun remove(element: EdgeType): Boolean {
        val ok = super<HashSet>.remove(element)
        if (ok) {
            handleOnRemove(element)
        }
        return ok
    }

    override fun clear() {
        // Make a copy of our edges so we can pass a copy to our on-remove handler
        val edges = this.toSet()
        super.clear()
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
}
