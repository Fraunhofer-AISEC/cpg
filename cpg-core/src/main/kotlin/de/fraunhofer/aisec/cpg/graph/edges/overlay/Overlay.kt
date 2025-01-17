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
package de.fraunhofer.aisec.cpg.graph.edges.overlay

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeSet
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeSingletonList
import de.fraunhofer.aisec.cpg.graph.edges.collections.MirroredEdgeCollection
import kotlin.reflect.KProperty

/**
 * Represents an edge in a graph specifically used for overlay purposes.
 *
 * @param start The starting node of the edge.
 * @param end The ending node of the edge.
 * @property labels A predefined set of labels associated with the OverlayEdge. By default, it is
 *   initialized with the label "OVERLAY".
 * @constructor Constructs an [OverlayEdge] with a specified [start] and [end] node.
 */
class OverlayEdge(start: Node, end: Node) : Edge<Node>(start, end) {
    override var labels: Set<String> = setOf("OVERLAY")
}

/**
 * Represents a single edge in an overlay graph structure, linking nodes with specific properties.
 *
 * @param thisRef The current node that the edge originates from or is associated with.
 * @param of The optional target node of the edge.
 * @param mirrorProperty The property representing a mutable collection of mirrored overlay edges.
 * @param outgoing A flag indicating whether the edge is outgoing (default is true).
 * @constructor Initializes the [OverlaySingleEdge] instance with the provided parameters.
 */
class OverlaySingleEdge(
    thisRef: Node,
    of: Node?,
    override var mirrorProperty: KProperty<MutableCollection<OverlayEdge>>,
    outgoing: Boolean = true,
) :
    EdgeSingletonList<Node, Node?, OverlayEdge>(
        thisRef = thisRef,
        init = ::OverlayEdge,
        outgoing = outgoing,
        of = of,
    ),
    MirroredEdgeCollection<Node, OverlayEdge>

/**
 * Represents a collection of overlay edges connected to a specific node. This class is used to
 * manage and define relationships between nodes through overlay edges, providing both outgoing and
 * incoming edge handling capabilities.
 *
 * @param thisRef The reference node that the overlays are associated with.
 * @param mirrorProperty A reference to a property that mirrors the collection of overlay edges.
 * @param outgoing A boolean indicating whether the edges managed by this collection are outgoing.
 * @constructor Initializes the [Overlays] object with a reference node, a property for edge
 *   mirroring, and a direction to specify outgoing or incoming edges.
 */
class Overlays(
    thisRef: Node,
    override var mirrorProperty: KProperty<MutableCollection<OverlayEdge>>,
    outgoing: Boolean,
) :
    EdgeSet<Node, OverlayEdge>(thisRef = thisRef, init = ::OverlayEdge, outgoing = outgoing),
    MirroredEdgeCollection<Node, OverlayEdge>
