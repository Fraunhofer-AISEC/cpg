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
    override var postAdd: ((EdgeType) -> Unit)? = null,
    override var postRemove: ((EdgeType) -> Unit)? = null
) : ArrayList<EdgeType>(), EdgeCollection<NodeType, EdgeType> {

    override fun add(e: EdgeType): Boolean {
        // Make sure, the index is always set
        if (e.index == null) {
            e.index = this.size
        }

        val ok = super<ArrayList>.add(e)
        if (ok) {
            handlePostAdd(e)
        }
        return ok
    }

    override fun remove(o: EdgeType): Boolean {
        val ok = super<ArrayList>.remove(o)
        if (ok) {
            handlePostRemove(o)
        }
        return ok
    }

    override fun removeAll(c: Collection<EdgeType>): Boolean {
        val ok = super.removeAll(c)
        if (ok) {
            c.forEach { handlePostRemove(it) }
        }
        return ok
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        super.removeRange(fromIndex, toIndex)
    }

    override fun removeAt(index: Int): EdgeType {
        var edge = super.removeAt(index)
        handlePostRemove(edge)
        return edge
    }

    override fun removeIf(predicate: Predicate<in EdgeType>): Boolean {
        var edges = filter { predicate.test(it) }
        val ok = super<ArrayList>.removeIf(predicate)
        if (ok) {
            edges.forEach { handlePostRemove(it) }
        }
        return ok
    }

    override fun clear() {
        var edges = this.toList()
        super.clear()
        edges.forEach { handlePostRemove(it) }
    }

    override fun add(index: Int, element: EdgeType) {
        // Make sure, the index is always set
        element.index = this.size

        super<ArrayList>.add(index, element)

        // TODO: actually we need to re-compute all other index properties

        handlePostAdd(element)
    }

    override fun toNodeCollection(outgoing: Boolean): List<NodeType> {
        return internalToNodeCollection(this, outgoing, ::ArrayList)
    }

    /**
     * Returns an [UnwrappedEdgeList] magic container which holds a structure that provides easy
     * access to the "target" nodes without edge information.
     */
    override fun unwrap(): UnwrappedEdgeList<NodeType, EdgeType> {
        return UnwrappedEdgeList(this)
    }

    override fun equals(o: Any?): Boolean {
        if (o !is EdgeList<*, *>) return false

        // Otherwise, try to compare the contents of the lists with the propertyEquals method
        if (this.size == o.size) {
            for (i in this.indices) {
                if (this[i] != o[i]) {
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
