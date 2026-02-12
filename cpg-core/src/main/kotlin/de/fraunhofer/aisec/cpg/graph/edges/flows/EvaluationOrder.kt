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
package de.fraunhofer.aisec.cpg.graph.edges.flows

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeList
import de.fraunhofer.aisec.cpg.graph.edges.collections.MirroredEdgeCollection
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import kotlin.reflect.KProperty

/**
 * An edge in our Evaluation Order Graph (EOG). It considers the order in which our AST statements
 * would be "evaluated" (e.g. by a compiler or interpreter). See [EvaluationOrderGraphPass] for more
 * details.
 */
class EvaluationOrder(
    start: Node,
    end: Node,
    /**
     * True, if the edge flows into unreachable code e.g. a branch condition which is always false.
     */
    var unreachable: Boolean = false,

    /**
     * If we have multiple EOG edges the branch property indicates which EOG edge leads to true
     * branch (expression evaluated to true) or the false branch (e.g. with an if/else condition).
     * Otherwise, this property is null.
     */
    var branch: Boolean? = null,
) : Edge<Node>(start, end) {
    /**
     * For nodes with multiple incoming our outcoming edges, we label the node leading to/from a
     * possible strongly connected component (SCC). This is populated by the
     * [de.fraunhofer.aisec.cpg.passes.SccPass]. Remains `null` if the edge is not part of any
     * non-trivial SCC, and otherwise indicates the priority (AKA the nesting level) with which the
     * edge should be taken when iterating the EOG
     */
    var scc: Int? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EvaluationOrder) return false
        return this.unreachable == other.unreachable &&
            this.branch == other.branch &&
            super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + unreachable.hashCode()
        result = 31 * result + branch.hashCode()
        return result
    }

    override var labels = setOf("EOG")
}

/**
 * Holds a container of [EvaluationOrder] edges. The canonical version of this lives in
 * [Node.prevEOGEdges] / [Node.nextEOGEdges] and is populated by the [EvaluationOrderGraphPass].
 *
 * Note: We would not actually need the type parameter [NodeType] here, since all target nodes are
 * of type [Node], but if we skip this parameter, the Neo4J exporter does not recognize this as a
 * "list".
 */
class EvaluationOrders<NodeType : Node>(
    thisRef: Node,
    override var mirrorProperty: KProperty<MutableCollection<EvaluationOrder>>,
    outgoing: Boolean = true,
) :
    EdgeList<Node, EvaluationOrder>(
        thisRef = thisRef,
        init = ::EvaluationOrder,
        outgoing = outgoing,
    ),
    MirroredEdgeCollection<Node, EvaluationOrder>

/**
 * This function inserts the given [newNode] before the current node ([this]) in its existing EOG
 * path.
 *
 * Before:
 * ```
 * <node1> -- EOG -->
 *                    <this>
 * <node2> -- EOG -->
 * ```
 *
 * We want to insert a new [EvaluationOrder] edge between all incoming edges of [this] (node3).
 *
 * Afterward:
 * ```
 * <node1> -- EOG -->
 *                    <new node> -- EOG --> <this>
 * <node2> -- EOG -->
 * ```
 */
fun Node.insertNodeBeforeInEOGPath(
    newNode: Node,
    builder: ((EvaluationOrder) -> Unit) = {},
): Boolean {
    // Construct a new edge from the given node to the current node
    val edge = EvaluationOrder(newNode, this).also(builder)

    // Make a copy of the incoming edges of the current node and set the start of the new edge as
    // the end
    val copy = this.prevEOGEdges.toList()
    copy.forEach { it.end = newNode }

    // Clear the incoming edges of the current node
    this.prevEOGEdges.clear()

    // Add the old edges as the previous edges of the new edge's start. We cannot use "addAll"
    // because otherwise our mirroring will not be triggered.
    copy.forEach { newNode.prevEOGEdges += it }

    // Add the new edge as a previous edge of the current node
    return this.prevEOGEdges.add(edge)
}

/**
 * This function inserts the given [newNode] after the current node ([this]) in its existing EOG
 * path.
 *
 * Before:
 * ```
 *         -- EOG --> <node1>
 * <this>
 *         -- EOG --> <node2>
 * ```
 *
 * We want to insert a new [EvaluationOrder] edge between all outgoing edges of [this].
 *
 * Afterward:
 * ```
 *                              -- EOG --> <node1>
 * <this> -- EOG --> <new node>
 *                              -- EOG --> <node2>
 * ```
 */
fun Node.insertNodeAfterwardInEOGPath(
    newNode: Node,
    builder: ((EvaluationOrder) -> Unit) = {},
): Boolean {
    // Construct a new edge from the current node to the given node
    val edge = EvaluationOrder(this, newNode).also(builder)

    // Make a copy of the outgoing edges of the current node and set the end of the new edge as
    // the start
    val copy = this.nextEOGEdges.toList()
    copy.forEach { it.start = newNode }

    // Clear the outgoing edges of the current node
    this.nextEOGEdges.clear()

    // Add the old edges as the next edges of the new edge's end. We cannot use "addAll" because
    // otherwise our mirroring will not be triggered.
    copy.forEach { newNode.nextEOGEdges += it }

    // Add the new edge as a next edge of the current node
    return this.nextEOGEdges.add(edge)
}
