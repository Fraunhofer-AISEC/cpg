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
import de.fraunhofer.aisec.cpg.graph.edges.overlay.BasicBlockEdgeList
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.helpers.neo4j.LocationConverter
import de.fraunhofer.aisec.cpg.passes.BasicBlockCollectorPass
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import java.util.*
import java.util.Objects
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.typeconversion.Convert

/**
 * A node representing a basic block, i.e. a sequence of nodes without any branching or merge
 * points.
 *
 * Note that there is not a single [underlyingNode] because the basic block can span multiple nodes
 * which are kept in [nodes].
 */
class BasicBlock() : OverlayNode() {
    /**
     * The starting node of this basic block. I.e., the first node in the BB's internal EOG. It's
     * either a merge point or the first node in a branch.
     */
    val startNode: Node?
        get() = nodes.firstOrNull()

    /** The edges connecting this basic block to its member nodes. */
    @Relationship(value = "BB", direction = Relationship.Direction.INCOMING)
    @PopulatedByPass(BasicBlockCollectorPass::class)
    var nodeEdges: BasicBlockEdgeList<Node> =
        BasicBlockEdgeList(this, mirrorProperty = Node::basicBlockEdges, outgoing = false)
        protected set

    /** The nodes contained in this basic block. */
    var nodes by unwrapping(BasicBlock::nodeEdges)

    /**
     * The ending node of this basic block. I.e., the last node in the BB's internal EOG. The next
     * node in the EOG is a merge point or a branch.
     */
    val endNode: Node?
        get() = nodes.lastOrNull()

    /**
     * If this basic block ends in a branching point (i.e., has multiple outgoing EOG edges), this
     * returns the [endNode] of this basic blocks EOG. Otherwise, null.
     */
    val branchingNode: Node?
        get() = if (this.nextEOG.size > 1) endNode else null

    @Convert(LocationConverter::class)
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
                uri = startNode?.location?.artifactLocation?.uri ?: URI(""),
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
        if (startNode == null) {
            return "Empty BasicBlock in $location"
        }
        return "BasicBlock from ${startNode!!::class.simpleName} ${startNode?.name} to ${(endNode ?: startNode!!)::class.simpleName} ${endNode?.name} in $location"
    }
}
