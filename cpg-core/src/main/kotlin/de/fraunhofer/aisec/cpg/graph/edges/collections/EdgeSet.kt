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
    override var postAdd: ((EdgeType) -> Unit)? = null,
    override var postRemove: ((EdgeType) -> Unit)? = null
) : HashSet<EdgeType>(), EdgeCollection<NodeType, EdgeType> {
    override fun add(e: EdgeType): Boolean {
        val ok = super<HashSet>.add(e)
        if (ok) {
            handlePostAdd(e)
        }
        return ok
    }

    override fun removeIf(predicate: Predicate<in EdgeType>): Boolean {
        var edges = filter { predicate.test(it) }
        val ok = super<HashSet>.removeIf(predicate)
        if (ok) {
            edges.forEach { handlePostRemove(it) }
        }
        return ok
    }

    override fun removeAll(c: Collection<EdgeType>): Boolean {
        val edges = this.toSet()
        val ok = super.removeAll(c)
        if (ok) {
            edges.forEach { handlePostRemove(it) }
        }
        return ok
    }

    override fun remove(o: EdgeType): Boolean {
        val ok = super<HashSet>.remove(o)
        if (ok) {
            handlePostRemove(o)
        }
        return ok
    }

    override fun clear() {
        var edges = this.toList()
        super.clear()
        edges.forEach { handlePostRemove(it) }
    }

    override fun toNodeCollection(outgoing: Boolean): MutableSet<NodeType> {
        return internalToNodeCollection(this, outgoing, ::HashSet)
    }

    /**
     * Returns an [UnwrappedEdgeSet] magic container which holds a structure that provides easy
     * access to the "target" nodes without edge information.
     */
    override fun unwrap(): UnwrappedEdgeSet<NodeType, EdgeType> {
        return UnwrappedEdgeSet(this)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is EdgeSet<*, *>) return false

        // Otherwise, try to compare the contents of the lists with the propertyEquals method
        return this.containsAll(o)
    }

    override fun hashCode(): Int {
        return internalHashcode(this, outgoing)
    }
}
