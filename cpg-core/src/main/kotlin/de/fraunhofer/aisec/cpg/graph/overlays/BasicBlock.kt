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
import de.fraunhofer.aisec.cpg.graph.BranchingNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeList
import de.fraunhofer.aisec.cpg.graph.edges.collections.MirroredEdgeCollection
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.LoopStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ComprehensionExpression
import de.fraunhofer.aisec.cpg.passes.BasicBlockCollectorPass
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import java.util.Objects
import kotlin.reflect.KProperty
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.RelationshipEntity

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

class BasicBlockEdges<NodeType : Node>(
    thisRef: Node,
    override var mirrorProperty: KProperty<MutableCollection<BasicBlockEdge>>,
    outgoing: Boolean = true,
) :
    EdgeList<Node, BasicBlockEdge>(thisRef = thisRef, init = ::BasicBlockEdge, outgoing = outgoing),
    MirroredEdgeCollection<Node, BasicBlockEdge>

/**
 * A node representing a basic block, i.e. a sequence of nodes without any branching or merge
 * points.
 *
 * Note that there is not a single [underlyingNode] because the basic block can span multiple nodes
 * which are kept in [nodes].
 */
class BasicBlock(var startNode: Node) : OverlayNode() {

    @Relationship(value = "BB", direction = Relationship.Direction.INCOMING)
    @PopulatedByPass(BasicBlockCollectorPass::class)
    var nodeEdges: BasicBlockEdges<Node> =
        BasicBlockEdges(this, mirrorProperty = Node::basicBlockEdges, outgoing = false)
        protected set

    var nodes by unwrapping(BasicBlock::nodeEdges)

    val endNode: Node?
        get() = nodes.lastOrNull()

    val branchingNode: Node?
        get() =
            if (
                endNode is BranchingNode ||
                    endNode is LoopStatement ||
                    endNode is ComprehensionExpression
            ) {
                endNode as Node
            } else null

    override var location: PhysicalLocation? = null
        get() {
            val startLine = nodes.mapNotNull { it.location?.region?.startLine }.minOrNull() ?: -1
            val startColumn =
                nodes
                    .filter { it.location?.region?.startLine == startLine }
                    .mapNotNull { it.location?.region?.startColumn }
                    .minOrNull() ?: -1
            val endLine = nodes.mapNotNull { it.location?.region?.endLine }.maxOrNull() ?: -1
            val endColumn =
                nodes
                    .filter { it.location?.region?.endLine == endLine }
                    .mapNotNull { it.location?.region?.endColumn }
                    .maxOrNull() ?: -1
            return PhysicalLocation(
                uri = startNode.location?.artifactLocation?.uri ?: URI(""),
                region =
                    Region(
                        startLine = startLine,
                        startColumn = startColumn,
                        endLine = endLine,
                        endColumn = endColumn,
                    ),
            )
        }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), nodes)
    }

    override fun equals(other: Any?): Boolean {
        return other is BasicBlock &&
            super.equals(other) &&
            this.nodes == other.nodes &&
            this.startNode == other.startNode &&
            this.endNode == other.endNode
    }

    override fun toString(): String {
        return "BasicBlock from ${startNode::class.simpleName} ${startNode.name} to ${(endNode ?: startNode)::class.simpleName} ${endNode?.name} in $location"
    }
}
