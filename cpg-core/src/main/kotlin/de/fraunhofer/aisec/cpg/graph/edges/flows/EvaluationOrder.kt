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
import org.neo4j.ogm.annotation.RelationshipEntity

/**
 * An edge in our Evaluation Order Graph (EOG). It considers the order in which our AST statements
 * would be "evaluated" (e.g. by a compiler or interpreter). See [EvaluationOrderGraphPass] for more
 * details.
 */
@RelationshipEntity
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
    var branch: Boolean? = null
) : Edge<Node>(start, end) {
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
        outgoing = outgoing
    ),
    MirroredEdgeCollection<Node, EvaluationOrder>
