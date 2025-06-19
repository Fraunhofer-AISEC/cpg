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
package de.fraunhofer.aisec.cpg.graph.edges.ast

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeList
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeSingletonList
import de.fraunhofer.aisec.cpg.graph.serialize.Serializers
import org.neo4j.ogm.annotation.*

/** This property edge describes a parent/child relationship in the Abstract Syntax Tree (AST). */
@RelationshipEntity
open class AstEdge<T : Node>(
    start: Node,
    @JsonSerialize(using = Serializers.FullObjectSerializer::class) override var end: T,
) : Edge<T>(start, end) {
    init {
        end.astParent = start
    }

    override var labels: Set<String> = setOf("AST")
}

/** Creates an [AstEdges] container starting from this node. */
fun <NodeType : Node> Node.astEdgesOf(
    onAdd: ((AstEdge<NodeType>) -> Unit)? = null,
    onRemove: ((AstEdge<NodeType>) -> Unit)? = null,
): AstEdges<NodeType, AstEdge<NodeType>> {
    return AstEdges(thisRef = this, onAdd = onAdd, onRemove = onRemove)
}

/**
 * Creates a single optional [AstEdge] starting from this node (wrapped in a [EdgeSingletonList]
 * container).
 */
fun <NodeType : Node> Node.astOptionalEdgeOf(
    onChanged: ((old: AstEdge<NodeType>?, new: AstEdge<NodeType>?) -> Unit)? = null
): EdgeSingletonList<NodeType, NodeType?, AstEdge<NodeType>> {
    return EdgeSingletonList(
        thisRef = this,
        init = ::AstEdge,
        outgoing = true,
        onChanged = onChanged,
        of = null,
    )
}

/**
 * Creates a single [AstEdge] starting from this node (wrapped in a [EdgeSingletonList] container).
 */
fun <NodeType : Node> Node.astEdgeOf(
    of: NodeType,
    onChanged: ((old: AstEdge<NodeType>?, new: AstEdge<NodeType>?) -> Unit)? = null,
): EdgeSingletonList<NodeType, NodeType, AstEdge<NodeType>> {
    return EdgeSingletonList(
        thisRef = this,
        init = ::AstEdge,
        outgoing = true,
        onChanged = onChanged,
        of = of,
    )
}

/** This property edge list describes elements that are AST children of a node. */
open class AstEdges<NodeType : Node, PropertyEdgeType : AstEdge<NodeType>>(
    thisRef: Node,
    onAdd: ((PropertyEdgeType) -> Unit)? = null,
    onRemove: ((PropertyEdgeType) -> Unit)? = null,
    @Suppress("UNCHECKED_CAST")
    init: (start: Node, end: NodeType) -> PropertyEdgeType = { start, end ->
        AstEdge(start, end) as PropertyEdgeType
    },
) :
    EdgeList<NodeType, PropertyEdgeType>(
        thisRef = thisRef,
        init = init,
        onAdd = onAdd,
        onRemove = onRemove,
    )
