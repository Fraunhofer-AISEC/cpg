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

import de.fraunhofer.aisec.cpg.graph.BranchingNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.statements.LoopStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ComprehensionExpression
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import java.util.Objects

/**
 * A node representing a basic block, i.e. a sequence of nodes without any branching or merge
 * points.
 *
 * Note that there is not a single [underlyingNode] because the basic block can span multiple nodes
 * which are kept in [nodes].
 */
class BasicBlock(val nodes: MutableList<Node> = mutableListOf(), var startNode: Node) :
    OverlayNode() {

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
            return PhysicalLocation(
                uri = startNode.location?.artifactLocation?.uri ?: URI(""),
                region =
                    Region(
                        startLine =
                            nodes.mapNotNull { it.location?.region?.startLine }.minOrNull() ?: -1,
                        startColumn =
                            nodes.mapNotNull { it.location?.region?.startColumn }.minOrNull() ?: -1,
                        endLine =
                            nodes.mapNotNull { it.location?.region?.endLine }.maxOrNull() ?: -1,
                        endColumn =
                            nodes.mapNotNull { it.location?.region?.endColumn }.maxOrNull() ?: -1,
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
        return "$startNode - $endNode"
    }
}
