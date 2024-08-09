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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeList
import org.neo4j.ogm.annotation.*

/** This property edge describes a parent/child relationship in the Abstract Syntax Tree (AST). */
@RelationshipEntity
open class AstEdge<T : Node> : Edge<T> {
    constructor(start: Node, end: T) : super(start, end) {
        // In a future PR, we will set the astParent here
    }
}

/** Creates an [AstEdges] container starting from this node. */
fun <NodeType : Node> Node.astEdgesOf(
    postAdd: ((AstEdge<NodeType>) -> Unit)? = null,
    postRemove: ((AstEdge<NodeType>) -> Unit)? = null,
): AstEdges<NodeType, AstEdge<NodeType>> {
    return AstEdges(this, postAdd, postRemove)
}

/** This property edge list describes elements that are AST children of a node. */
open class AstEdges<NodeType : Node, PropertyEdgeType : Edge<NodeType>>(
    thisRef: Node,
    postAdd: ((PropertyEdgeType) -> Unit)? = null,
    postRemove: ((PropertyEdgeType) -> Unit)? = null,
    @Suppress("UNCHECKED_CAST")
    init: (start: Node, end: NodeType) -> PropertyEdgeType = { start, end ->
        AstEdge<NodeType>(start, end) as PropertyEdgeType
    }
) :
    EdgeList<NodeType, PropertyEdgeType>(
        thisRef = thisRef,
        init = init,
        postAdd = postAdd,
        postRemove = postRemove,
    )
