/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.edges

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeSet
import de.fraunhofer.aisec.cpg.graph.edges.collections.MirroredEdgeCollection
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemoryAddress
import kotlin.reflect.KProperty
import org.neo4j.ogm.annotation.RelationshipEntity

/** This edge class defines that [end] is a (possible) memory address of [start]. */
@RelationshipEntity
open class MemoryAddressEdge(start: Node, end: MemoryAddress, var outgoing: Boolean) :
    Edge<MemoryAddress>(start, end) {
    override var labels = setOf("MemoryAddress")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemoryAddressEdge) return false
        return this.outgoing == other.outgoing && super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode() * 31 + outgoing.hashCode()
    }
}

/** This class represents a container of [MemoryAddressEdge] property edges in a [thisRef]. */
class MemoryAddressEdges(
    thisRef: Node,
    override var mirrorProperty: KProperty<MutableCollection<MemoryAddressEdge>>,
    outgoing: Boolean,
    onAdd: ((MemoryAddressEdge) -> Unit)? = null,
) :
    EdgeSet<MemoryAddress, MemoryAddressEdge>(
        thisRef = thisRef,
        init = { start: Node, end: MemoryAddress -> MemoryAddressEdge(start, end, outgoing) },
        outgoing = outgoing,
        onAdd = onAdd,
    ),
    MirroredEdgeCollection<MemoryAddress, MemoryAddressEdge>

/** Creates an [Node] container starting from this node. */
fun Node.memoryAddressEdgesOf(
    mirrorProperty: KProperty<MutableCollection<MemoryAddressEdge>>,
    outgoing: Boolean,
    onAdd: ((MemoryAddressEdge) -> Unit)? = null,
): MemoryAddressEdges {
    return MemoryAddressEdges(
        thisRef = this,
        mirrorProperty = mirrorProperty,
        outgoing = outgoing,
        onAdd = onAdd,
    )
}
