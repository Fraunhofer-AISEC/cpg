/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.concepts

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeSet

/**
 * An edge from a generic concept or operation to a [Node] that one of its properties refers to
 * (i.e. a [GenericPropertyValue.NodeReferenceValue]).
 *
 * The name of the property the reference originates from is carried in [Edge.name], so that a node
 * with several references stays unambiguous, both in memory and once persisted.
 */
class GenericPropertyReferenceEdge(start: Node, end: Node) : Edge<Node>(start, end) {
    override var labels: Set<String> = LABELS

    companion object {
        /**
         * Shared, immutable label set for all [GenericPropertyReferenceEdge]s (see [Edge.labels]).
         */
        val LABELS = setOf(RELATIONSHIP_NAME)

        /** The relationship name used for these edges in the graph database. */
        const val RELATIONSHIP_NAME = "GENERIC_PROPERTY_REFERENCE"
    }
}

/**
 * The [GenericPropertyReferenceEdge]s of a generic concept or operation, i.e. one edge per property
 * whose value is a [GenericPropertyValue.NodeReferenceValue].
 */
class GenericPropertyReferences(thisRef: Node) :
    EdgeSet<Node, GenericPropertyReferenceEdge>(
        thisRef = thisRef,
        init = ::GenericPropertyReferenceEdge,
    )

/**
 * Something that holds a [GenericProperties] whose [GenericProperties.nodeReferences] are exposed
 * as real graph relationships.
 *
 * Implementations are expected to back [propertyReferenceEdges] with
 * [buildGenericPropertyReferences], which derives the edges from [properties]. The properties
 * remain the single source of truth; the edges exist so that a reference to another node is a
 * traversable relationship in the database rather than a stringified id buried in a property value.
 */
interface HasGenericProperties {
    /** The generic properties of this concept or operation. */
    val properties: GenericProperties

    /** One edge per entry in [GenericProperties.nodeReferences]. */
    val propertyReferenceEdges: GenericPropertyReferences
}

/**
 * Builds the [GenericPropertyReferences] for this node from the [GenericProperties.nodeReferences]
 * of [properties], labelling each edge with the name of the property it originates from.
 */
fun Node.buildGenericPropertyReferences(properties: GenericProperties): GenericPropertyReferences {
    val edges = GenericPropertyReferences(this)
    properties.nodeReferences.forEach { (propertyName, referencedNode) ->
        edges.add(
            GenericPropertyReferenceEdge(this, referencedNode).also { it.name = propertyName }
        )
    }
    return edges
}
