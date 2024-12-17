package de.fraunhofer.aisec.cpg.graph.edges.overlay

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge
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
