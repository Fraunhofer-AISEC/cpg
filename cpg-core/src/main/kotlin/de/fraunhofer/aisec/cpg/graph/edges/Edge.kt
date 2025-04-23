/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonIgnore
import de.fraunhofer.aisec.cpg.assumptions.Assumption
import de.fraunhofer.aisec.cpg.assumptions.HasAssumptions
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.Node.Companion.TO_STRING_STYLE
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.Persistable
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import java.util.*
import kotlin.reflect.KProperty
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.*

/**
 * This class represents an edge between two [Node] objects in a Neo4J graph. It can be used to
 * store additional information that relate to the relationship between the two nodes that belong to
 * neither of the two nodes directly.
 *
 * An example would be the name (in this case `a`) of an argument between a [CallExpression] (`foo`)
 * and its argument (a [Literal] of `2`) in languages that support keyword arguments, such as
 * Python:
 * ```python
 * foo("bar", a = 2)
 * ```
 */
@RelationshipEntity
abstract class Edge<NodeType : Node> : Persistable, Cloneable, HasAssumptions {
    /** Required field for object graph mapping. It contains the node id. */
    @field:Id @field:GeneratedValue private val id: Long? = null

    // Node where the edge is outgoing
    @JsonIgnore @field:StartNode var start: Node

    // Node where the edge is ingoing
    @JsonBackReference @field:EndNode var end: NodeType

    @DoNotPersist override val assumptions: MutableList<Assumption> = mutableListOf()

    constructor(start: Node, end: NodeType) {
        this.start = start
        this.end = end
    }

    constructor(edge: Edge<NodeType>) {
        start = edge.start
        end = edge.end
    }

    abstract var labels: Set<String>

    /** `true` if one of the two nodes connected by the [Edge] is an overlay node. */
    val overlaying: Boolean
        get() = end is OverlayNode || start is OverlayNode

    /**
     * The index of this node, if it is stored in an
     * [de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeList].
     */
    var index: Int? = null

    /** An optional name. */
    var name: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Edge<*>) return false

        return start == other.start &&
            end == other.end &&
            index == other.index &&
            name == other.name
    }

    fun propertyEquals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj !is Edge<*>) return false
        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(start, end, index, name)
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("end", end)
            .toString()
    }

    public override fun clone(): Edge<NodeType> {
        // needs to be implemented by subclasses
        return super.clone() as Edge<NodeType>
    }

    companion object {
        @JvmStatic
        fun <E : Node> propertyEqualsList(edges: List<Edge<E>>?, edges2: List<Edge<E>>?): Boolean {
            // Check, if the first edge is null
            if (edges == null) {
                // They can only be equal now, if the second one is also null
                return edges2 == null
            }

            // Otherwise, try to compare the contents of the lists with the propertyEquals (the
            // second one still might be null)
            if (edges.size == edges2?.size) {
                for (i in edges.indices) {
                    if (!edges[i].propertyEquals(edges2[i])) {
                        return false
                    }
                }
                return true
            }
            return false
        }
    }

    fun <ThisType : Node> delegate(): Delegate<ThisType> {
        return Delegate()
    }

    @Transient
    inner class Delegate<ThisType : Node>() {
        operator fun getValue(thisRef: ThisType, property: KProperty<*>): NodeType {
            var edge = this@Edge
            // We only support outgoing edges this way
            return edge.end
        }

        operator fun setValue(thisRef: ThisType, property: KProperty<*>, value: NodeType) {
            this@Edge.end = value
            // TODO: trigger some update hook
        }
    }
}
