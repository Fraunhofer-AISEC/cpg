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
package de.fraunhofer.aisec.cpg.graph.edges.overlay

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeList
import de.fraunhofer.aisec.cpg.graph.edges.collections.MirroredEdgeCollection
import kotlin.reflect.KProperty
import org.neo4j.ogm.annotation.RelationshipEntity

/**
 * An edge representing that a [Node] belongs to a
 * [de.fraunhofer.aisec.cpg.graph.overlays.BasicBlock] or the basic block contains the node.
 */
@RelationshipEntity
class BasicBlockEdge(start: Node, end: Node) : Edge<Node>(start, end) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BasicBlockEdge) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override var labels = setOf("BB")
}

/**
 * A collection of [BasicBlockEdge]s representing the relationship between a
 * [de.fraunhofer.aisec.cpg.graph.overlays.BasicBlock] and its member [Node]s.
 */
class BasicBlockEdgeList<NodeType : Node>(
    thisRef: Node,
    override var mirrorProperty: KProperty<MutableCollection<BasicBlockEdge>>,
    outgoing: Boolean = true,
) :
    EdgeList<Node, BasicBlockEdge>(thisRef = thisRef, init = ::BasicBlockEdge, outgoing = outgoing),
    MirroredEdgeCollection<Node, BasicBlockEdge>
