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

import de.fraunhofer.aisec.cpg.graph.DataflowNode
import de.fraunhofer.aisec.cpg.graph.EvaluatedNode
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeList
import de.fraunhofer.aisec.cpg.graph.edges.collections.MirroredEdgeCollection
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import kotlin.reflect.KProperty
import org.neo4j.ogm.annotation.RelationshipEntity

/**
 * An edge in a Control Dependence Graph (CDG). Denotes that the [start] node exercises control
 * dependence on the [end] node. See [ControlDependenceGraphPass].
 *
 * Actually it would be sufficient to target an [EvaluatedNode] here, since we only need EOG
 * information, but since we need to combine [ControlDependence] and [Dataflow] nodes into a common
 * [ProgramDependence] graph, we need to target the same node type here. This is sort-of ok because
 * the control dependence is basically an implicit dataflow.
 */
@RelationshipEntity
class ControlDependence(
    start: DataflowNode,
    end: DataflowNode,
    /** A set of [EvaluationOrder.branch] values. */
    var branches: Set<Boolean> = setOf(),
) : ProgramDependence<DataflowNode>(start, end, DependenceType.CONTROL) {

    override var labels = super.labels.plus("CDG")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ControlDependence) return false
        return this.branches == other.branches && super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + branches.hashCode()
        return result
    }
}

/** A container of [ControlDependence] edges. */
class ControlDependences :
    EdgeList<DataflowNode, ControlDependence>,
    MirroredEdgeCollection<DataflowNode, ControlDependence> {

    override var mirrorProperty: KProperty<MutableCollection<ControlDependence>>

    constructor(
        thisRef: EvaluatedNode,
        mirrorProperty: KProperty<MutableCollection<ControlDependence>>,
        outgoing: Boolean,
    ) : super(
        thisRef = thisRef,
        init = { start, end -> ControlDependence(start as DataflowNode, end) },
        outgoing = outgoing,
    ) {
        this.mirrorProperty = mirrorProperty
    }
}
