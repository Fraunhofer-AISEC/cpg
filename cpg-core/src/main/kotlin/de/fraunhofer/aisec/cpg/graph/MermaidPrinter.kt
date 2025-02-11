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

import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.PartialDataflowGranularity
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

/** Utility function to print the DFG using [printGraph]. */
fun Node.printDFG(
    maxConnections: Int = 25,
    vararg strategies: (Node) -> Iterator<Edge<Node>> =
        arrayOf<(Node) -> Iterator<Edge<Node>>>(
            Strategy::DFG_FORWARD_EDGES,
            Strategy::DFG_BACKWARD_EDGES,
        ),
): String {
    return this.printGraph(maxConnections = maxConnections, *strategies)
}

/*
/** Utility function to print the EOG using [printGraph]. */
fun Node.printEOG(maxConnections: Int = 25): String {
    return this.printGraph(PropertyEdge::class, Node::nextEOGEdges, Node::prevEOGEdges, maxConnections)
}
*/

/**
 * This function prints a partial graph, limited to a particular set of edges, starting with the
 * current [Node] as Markdown, with an embedded [Mermaid](https://mermaid.js.org) graph. The output
 * can either be pasted into a Markdown document (and then rendered) or directly pasted into GitHub
 * issues, discussions or pull requests (see
 * https://github.blog/2022-02-14-include-diagrams-markdown-files-mermaid/).
 *
 * @param strategies The strategies to use when iterating the graph. See [Strategy] for
 *   implementations.
 * @return The Mermaid graph as a string encapsulated in triple-backticks.
 */
fun Node.printGraph(
    maxConnections: Int = 25,
    vararg strategies: (Node) -> Iterator<Edge<Node>>,
): String {
    val builder = StringBuilder()

    builder.append("```mermaid\n")
    builder.append("flowchart TD\n")

    // We use a set with a defined ordering to hold our work-list to have a somewhat consistent
    // ordering of statements in the mermaid file.
    val worklist = LinkedHashSet<Edge<Node>>()
    val alreadySeen = identitySetOf<Edge<Node>>()
    var conns = 0

    strategies.forEach { strategy ->
        worklist += strategy(this).asSequence().sortedBy { it.end.name }
    }

    while (worklist.isNotEmpty() && conns < maxConnections) {
        // Take one edge out of the work-list
        val edge = worklist.first()
        worklist.remove(edge)

        if (edge in alreadySeen) {
            continue
        }

        // Add it to the seen-list
        alreadySeen += edge

        val start = edge.start
        val end = edge.end
        builder.append(
            "${start.hashCode()}[\"${start.nodeLabel}\"]-->|${edge.label()}|${end.hashCode()}[\"${end.nodeLabel}\"]\n"
        )
        conns++

        // Add next and prev edges to the work-list. We sort the entries by name to have this
        // somewhat consistent across multiple invocations of this function
        strategies.forEach { strategy ->
            when (strategy) {
                Strategy::DFG_FORWARD_EDGES -> {
                    worklist += strategy(end).asSequence().sortedBy { it.end.name }
                    worklist += strategy(start).asSequence().sortedBy { it.end.name }
                }

                Strategy::DFG_BACKWARD_EDGES -> {
                    worklist += strategy(end).asSequence().sortedBy { it.start.name }
                    worklist += strategy(start).asSequence().sortedBy { it.start.name }
                }

                else -> TODO("Unknown strategy received.")
            }
        }
    }

    builder.append("```")

    return builder.toString()
}

private fun Edge<Node>.label(): String {
    val builder = StringBuilder()
    builder.append("\"")
    builder.append(this.labels.joinToString(","))

    if (this is Dataflow) {
        var granularity = this.granularity
        if (granularity is PartialDataflowGranularity) {
            builder.append(" (partial, ${granularity.partialTarget?.name})")
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
