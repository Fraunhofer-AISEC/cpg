package de.fraunhofer.aisec.cpg.graph.edge

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration

/**
 * The granularity of the data-flow, e.g., whether the flow contains the whole object, or just a
 * part of it, for example a record (class/struct) member. In the latter case, the part can be
 * specified using the [Dataflow.memberField], which contains the field declaration
 * node.
 */
enum class GranularityType {
    FULL,
    PARTIAL
}

class Dataflow(start: Node, end: Node, val granularity: GranularityType = GranularityType.FULL, val memberField: FieldDeclaration? = null) : PropertyEdge<Node>(start, end) {
}
