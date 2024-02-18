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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.edge.Dataflow
import de.fraunhofer.aisec.cpg.graph.edge.GranularityType
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/** Utility function to print the DFG using [printGraph]. */
fun Node.printDFG(maxConnections: Int = 25): String {
    return this.printGraph(Dataflow::class, Node::nextDFGEdges, Node::prevDFGEdges, maxConnections)
}
/*
/** Utility function to print the EOG using [printGraph]. */
fun Node.printEOG(maxConnections: Int = 25): String {
    return this.printGraph(PropertyEdge::class, Node::nextEOGEdges, Node::prevEOGEdges, maxConnections)
}
*/
/**
 * This function prints a partial graph, limited to a particular [edgeType], starting with the
 * current [Node] as Markdown, with an embedded [Mermaid](https://mermaid.js.org) graph. The output
 * can either be pasted into a Markdown document (and then rendered) or directly pasted into GitHub
 * issues, discussions or pull requests (see
 * https://github.blog/2022-02-14-include-diagrams-markdown-files-mermaid/).
 *
 * The edge type can be specified with the [nextEdgeGetter] and [prevEdgeGetter] functions, that
 * need to return a list of edges (as a [PropertyEdge]) beginning from this node.
 */
fun <T : PropertyEdge<Node>> Node.printGraph(
    edgeType: KClass<T>,
    nextEdgeGetter: KProperty1<Node, MutableList<T>>,
    prevEdgeGetter: KProperty1<Node, MutableList<T>>,
    maxConnections: Int = 25
): String {
    val builder = StringBuilder()

    builder.append("```mermaid\n")
    builder.append("flowchart TD\n")

    // We use a set with a defined ordering to hold our worklist to have a somewhat consistent
    // ordering of statements in the mermaid file.
    val worklist = LinkedHashSet<PropertyEdge<Node>>()
    val alreadySeen = identitySetOf<PropertyEdge<Node>>()
    var conns = 0

    worklist.addAll(nextEdgeGetter.get(this))

    while (worklist.isNotEmpty() && conns < maxConnections) {
        // Take one edge out of the work-list
        val edge = worklist.first()
        worklist.remove(edge)

        // Add it to the seen-list
        alreadySeen += edge

        val start = edge.start
        val end = edge.end
        builder.append(
            "${start.hashCode()}[\"${start.nodeLabel}\"]-->|${edge.label()}|${end.hashCode()}[\"${end.nodeLabel}\"]\n"
        )
        conns++

        // Add next and prev edges to the work-list (if not already seen). We sort the entries by
        // name to have this somewhat consistent across multiple invocations of this function
        var next = nextEdgeGetter.get(end).filter { it !in alreadySeen }.sortedBy { it.end.name }
        worklist += next

        var prev = prevEdgeGetter.get(end).filter { it !in alreadySeen }.sortedBy { it.start.name }
        worklist += prev

        next = nextEdgeGetter.get(start).filter { it !in alreadySeen }.sortedBy { it.end.name }
        worklist += next

        prev = prevEdgeGetter.get(start).filter { it !in alreadySeen }.sortedBy { it.start.name }
        worklist += prev
    }

    builder.append("```")

    return builder.toString()
}

private fun PropertyEdge<Node>.label(): String {
    val builder = StringBuilder()
    builder.append("\"")
    builder.append(this.label)

    // TODO(oxisto): Once we have proper edge classes, we can directly do this to the edge class
    if (this is Dataflow) {
        if (this.granularity == GranularityType.PARTIAL) {
            builder.append(" (partial, ${this.memberField?.name})")
        } else {
            builder.append(" (full)")
        }
    }

    builder.append("\"")
    return builder.toString()
}

private val Node.nodeLabel: String
    get() {
        return "${this.name}\n(${this::class.simpleName})\n${this.location}"
    }
