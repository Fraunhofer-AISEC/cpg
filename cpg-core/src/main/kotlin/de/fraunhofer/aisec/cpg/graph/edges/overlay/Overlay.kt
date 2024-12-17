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

class OverlayEdge(start: Node, end: Node) : Edge<Node>(start, end) {
    override var labels: Set<String> = setOf("OVERLAY")
}

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

class Overlays(
    thisRef: Node,
    override var mirrorProperty: KProperty<MutableCollection<OverlayEdge>>,
    outgoing: Boolean,
) :
    EdgeSet<Node, OverlayEdge>(thisRef = thisRef, init = ::OverlayEdge, outgoing = outgoing),
    MirroredEdgeCollection<Node, OverlayEdge>
