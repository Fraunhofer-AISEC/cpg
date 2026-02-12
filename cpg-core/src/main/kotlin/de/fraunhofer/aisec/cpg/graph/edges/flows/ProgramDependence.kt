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
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeSet
import de.fraunhofer.aisec.cpg.graph.edges.collections.MirroredEdgeCollection
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass
import kotlin.reflect.KProperty

/** The types of dependences that might be represented in the CPG */
enum class DependenceType {
    CONTROL,
    DATA,
}

/**
 * A container of [Edge] edges that act as a program dependence graph (PDG). The canonical version
 * of this lives in [Node.prevPDGEdges] / [Node.nextPDGEdges] and is populated by the
 * [ProgramDependenceGraphPass].
 *
 * After population, this collection will contain a direct combination of two other edge collections
 * ([Dataflows] and [ControlDependences]). If we would only handle an in-memory graph, we could just
 * store the edges in their original collection (e.g. DFG) as well as in the PDG.
 */
class ProgramDependences<NodeType : Node> :
    EdgeSet<NodeType, Edge<NodeType>>, MirroredEdgeCollection<NodeType, Edge<NodeType>> {
    override var mirrorProperty: KProperty<MutableCollection<Edge<NodeType>>>

    constructor(
        thisRef: Node,
        mirrorProperty: KProperty<MutableCollection<Edge<NodeType>>>,
        outgoing: Boolean,
    ) : super(
        thisRef,
        init = { _, _ ->
            throw UnsupportedOperationException(
                "This container only allows adding existing edges, but not creating new ones."
            )
        },
        outgoing,
    ) {
        this.mirrorProperty = mirrorProperty
    }

    override fun add(element: Edge<NodeType>): Boolean {
        return super<EdgeSet>.add(element)
    }
}

/**
 * This edge class defines that there's some kind of dependency between [start] and [end]. The
 * nature of this dependency is defined by [dependence].
 */
open class ProgramDependence(
    start: Node,
    end: Node,
    /**
     * The type of dependence (e.g. control or data or none). This selection is defined by the class
     * extending this class or in the [ProgramDependenceGraphPass].
     */
    var dependence: DependenceType,
) : Edge<Node>(start, end) {

    override var labels = setOf("PDG")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProgramDependence) return false
        return super.equals(other) && this.dependence == other.dependence
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + dependence.hashCode()
        return result
    }
}
