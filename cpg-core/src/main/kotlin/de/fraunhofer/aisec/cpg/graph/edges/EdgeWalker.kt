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
import de.fraunhofer.aisec.cpg.graph.ast.AstNode
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeCollection
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.identitySetOf

/**
 * Returns all [Edge]s of [EdgeType] directly attached to this [Node]. Optionally, a [predicate] can
 * be used to filter the edges even further.
 */
inline fun <reified EdgeType : Edge<out Node>> Node.edges(
    noinline predicate: ((EdgeType) -> Boolean) = { true }
): Collection<EdgeType> {
    val edges = mutableSetOf<EdgeType>()
    val fields = SubgraphWalker.getAllEdgeFields(this::class.java)
    for (field in fields) {
        var obj =
            synchronized(field) {
                // Disable access mechanisms
                field.isAccessible = true
                val obj = field[this]

                // Restore old state
                field.isAccessible = false
                obj
            } ?: continue

        // Gather all edges
        if (obj is EdgeCollection<*, *>) {
            for (edge in obj.toList()) {
                if (edge is EdgeType && predicate.invoke(edge)) {
                    edges += edge
                }
            }
        }
    }

    return edges
}

/**
 * This function returns a subgraph containing all [Edge]s starting from this [Node] that are of the
 * specific [EdgeType]. Optionally, a [predicate] can be used to filter the edges even further.
 */
inline fun <reified EdgeType : Edge<out Node>> Node.allEdges(
    noinline predicate: ((EdgeType) -> Boolean) = { true }
): Collection<EdgeType> {
    val alreadySeen = identitySetOf<Node>()
    val worklist = mutableListOf<Node>()
    val edges = mutableSetOf<EdgeType>()

    worklist += this
    alreadySeen += this

    while (worklist.isNotEmpty()) {
        val node = worklist.removeFirst()
        val toAdd = node.edges(predicate)

        val newStart = toAdd.map { it.start }.filter { it !in alreadySeen }
        worklist += newStart
        alreadySeen += newStart

        val newEnd = toAdd.map { it.end }.filter { it !in alreadySeen }
        worklist += newEnd
        alreadySeen += newEnd

        edges += toAdd
    }

    return edges
}

/** A shortcut to return all [AstEdge] edges starting from this node. */
val Node.astEdges: Collection<AstEdge<out AstNode>>
    get() {
        return allEdges()
    }

/** A shortcut to return all [Dataflow] edges starting from this node. */
val Node.dataflows: Collection<Dataflow>
    get() {
        return allEdges()
    }
