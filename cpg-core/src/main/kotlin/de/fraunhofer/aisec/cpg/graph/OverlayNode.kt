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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.NoLanguage
import de.fraunhofer.aisec.cpg.graph.edges.overlay.OverlaySingleEdge
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import java.util.Objects
import org.neo4j.ogm.annotation.Relationship

/**
 * Represents an extra node added to the CPG. These nodes can live next to the regular nodes,
 * typically having shared edges to extend the original graph.
 */
abstract class OverlayNode() : Node() {

    init {
        this.language = NoLanguage
    }

    @Relationship(value = "OVERLAY", direction = Relationship.Direction.INCOMING)
    /** All [OverlayNode]s nodes are connected to an original cpg [Node] by this. */
    var underlyingNodeEdge: OverlaySingleEdge =
        OverlaySingleEdge(this, of = null, mirrorProperty = Node::overlayEdges, outgoing = false)

    var underlyingNode by unwrapping(OverlayNode::underlyingNodeEdge)

    override fun equals(other: Any?): Boolean {
        return other is OverlayNode &&
            this.equalWithoutUnderlying(other) &&
            other.underlyingNode == this.underlyingNode
    }

    override fun hashCode() = Objects.hash(super.hashCode(), underlyingNode)

    open fun equalWithoutUnderlying(other: OverlayNode): Boolean {
        return super.equals(other)
    }
}
