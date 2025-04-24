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
 * An intelligent [MutableSet] wrapper around an [EdgeSet] which supports iterating, adding and
 * removing [Node] elements.
 */
class UnwrappedEdgeSet<NodeType : Node, EdgeType : Edge<NodeType>>(
    var set: EdgeSet<NodeType, EdgeType>
) : UnwrappedEdgeCollection<NodeType, EdgeType>(set), MutableSet<NodeType> {

    /**
     * Creates a new [Delegate] for this unwrapped list to be used in
     * [delegated properties](https://kotlinlang.org/docs/delegated-properties.html).
     */
    internal fun <ThisType : Node> delegate():
        UnwrappedEdgeSet<NodeType, EdgeType>.Delegate<ThisType> {
        return Delegate<ThisType>()
    }

    /** See [UnwrappedEdgeList.Delegate], but as a [MutableSet] instead of [MutableList]. */
    @Transient
    inner class Delegate<ThisType : Node>() {
        operator fun getValue(thisRef: ThisType, property: KProperty<*>): MutableSet<NodeType> {
            return this@UnwrappedEdgeSet
        }

        operator fun setValue(thisRef: ThisType, property: KProperty<*>, value: Set<NodeType>) {
            this@UnwrappedEdgeSet.resetTo(value)
        }
    }

    /** See [UnwrappedEdgeList.IncomingDelegate], but as a [MutableSet] instead of [MutableList]. */
    @Transient
    inner class IncomingDelegate<ThisType : Node, IncomingType>() {
        operator fun getValue(thisRef: ThisType, property: KProperty<*>): MutableSet<IncomingType> {
            @Suppress("UNCHECKED_CAST")
            return this@UnwrappedEdgeSet as MutableSet<IncomingType>
        }

        operator fun setValue(thisRef: ThisType, property: KProperty<*>, value: Set<IncomingType>) {
            @Suppress("UNCHECKED_CAST") this@UnwrappedEdgeSet.resetTo(value as Collection<NodeType>)
        }
    }

    operator fun <ThisType : Node> provideDelegate(
        thisRef: ThisType,
        prop: KProperty<*>,
    ): Delegate<ThisType> {
        return Delegate()
    }

    override fun equals(other: Any?): Boolean {
        return other is Set<*> && this.iterator().asSequence().toSet() == other
    }

    override fun hashCode(): Int {
        var hashCode = 1

        val it = iterator()
        while (it.hasNext()) {
            val element = it.next()
            hashCode = 31 * hashCode + element.hashCode()
        }

        return hashCode
    }
}
