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
package de.fraunhofer.aisec.cpg.graph.overlays

import de.fraunhofer.aisec.cpg.PopulatedByPass
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeList
import de.fraunhofer.aisec.cpg.graph.edges.collections.MirroredEdgeCollection
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.passes.SccPass
import kotlin.reflect.KProperty
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.RelationshipEntity

@RelationshipEntity
class StronglyConnectedComponentEdge(start: Node, end: Node) : Edge<Node>(start, end) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BasicBlockEdge) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override var labels = setOf("SCC")
}

class StronglyConnectedComponentEdges<NodeType : Node>(
    thisRef: Node,
    override var mirrorProperty: KProperty<MutableCollection<StronglyConnectedComponentEdge>>,
    outgoing: Boolean = true,
) :
    EdgeList<Node, StronglyConnectedComponentEdge>(
        thisRef = thisRef,
        init = ::StronglyConnectedComponentEdge,
        outgoing = outgoing,
    ),
    MirroredEdgeCollection<Node, StronglyConnectedComponentEdge>

/**
 * A node representing a basic block, i.e. a sequence of nodes without any branching or merge
 * points.
 *
 * Note that there is not a single [underlyingNode] because the basic block can span multiple nodes
 * which are kept in [nodes].
 */
class StronglyConnectedComponent() : OverlayNode() {

    @Relationship(value = "SCC", direction = Relationship.Direction.INCOMING)
    @PopulatedByPass(SccPass::class)
    var nodeEdges: StronglyConnectedComponentEdges<Node> =
        StronglyConnectedComponentEdges(
            this,
            mirrorProperty = Node::stronglyConnectedComponentEdges,
            outgoing = false,
        )
        protected set

    var nodes by unwrapping(StronglyConnectedComponent::nodeEdges)
}
