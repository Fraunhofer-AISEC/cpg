package de.fraunhofer.aisec.cpg.graph.edges.flows

/**
 * This interface declares that the implementing edge can point to or come from an overlay nodes and that
 * this has to be explicitly marked.
 */
interface OverlayingEdge {
    /**
     * Can be used in extending classes to set whether the specific edge instance is connected to an overlay node
     * or it can be computed from the connected nodes themselves.
     */
    var overlaying: Boolean

}