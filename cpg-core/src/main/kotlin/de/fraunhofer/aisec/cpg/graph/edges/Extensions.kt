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
package de.fraunhofer.aisec.cpg.graph.edges

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeList
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeSet
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeSingletonList
import de.fraunhofer.aisec.cpg.graph.edges.collections.UnwrappedEdgeList
import de.fraunhofer.aisec.cpg.graph.edges.collections.UnwrappedEdgeSet
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible

fun <EdgeType : Edge<Node>> MutableList<Node>.add(
    target: Node,
    builder: EdgeType.() -> Unit
): Boolean {
    if (this is UnwrappedEdgeList<*, *>) {
        @Suppress("UNCHECKED_CAST")
        return (this as UnwrappedEdgeList<Node, EdgeType>).add(target, builder)
    }

    throw UnsupportedOperationException()
}

/** See [UnwrappedEdgeList.Delegate]. */
fun <PropertyType : Node, NodeType : Node, EdgeType : Edge<PropertyType>> NodeType.unwrapping(
    edgeProperty: KProperty1<NodeType, EdgeList<PropertyType, EdgeType>>,
): UnwrappedEdgeList<PropertyType, EdgeType> {
    // Create an unwrapped container out of the edge property...
    edgeProperty.isAccessible = true
    val edge = edgeProperty.call(this)
    return edge.unwrap()
}

/** See [UnwrappedEdgeSet.Delegate]. */
fun <PropertyType : Node, NodeType : Node, EdgeType : Edge<PropertyType>> NodeType.unwrapping(
    edgeProperty: KProperty1<NodeType, EdgeSet<PropertyType, EdgeType>>,
): UnwrappedEdgeSet<PropertyType, EdgeType> {
    // Create an unwrapped container out of the edge property...
    edgeProperty.isAccessible = true
    val edge = edgeProperty.call(this)
    return edge.unwrap()
}

/** See [EdgeSingletonList.Delegate]. */
fun <PropertyType : Node, NodeType : Node, EdgeType : Edge<PropertyType>> NodeType
    .unwrappingOptional(
    edgeProperty: KProperty1<NodeType, EdgeSingletonList<PropertyType, PropertyType?, EdgeType>>,
): EdgeSingletonList<PropertyType, PropertyType?, EdgeType>.Delegate<NodeType> {
    edgeProperty.isAccessible = true
    val edge = edgeProperty.call(this)
    return edge.delegate<NodeType>()
}

/** See [EdgeSingletonList.Delegate]. */
fun <PropertyType : Node, NodeType : Node, EdgeType : Edge<PropertyType>> NodeType.unwrapping(
    edgeProperty: KProperty1<NodeType, EdgeSingletonList<PropertyType, PropertyType, EdgeType>>,
): EdgeSingletonList<PropertyType, PropertyType, EdgeType>.Delegate<NodeType> {
    edgeProperty.isAccessible = true
    val edge = edgeProperty.call(this)
    return edge.delegate<NodeType>()
}
