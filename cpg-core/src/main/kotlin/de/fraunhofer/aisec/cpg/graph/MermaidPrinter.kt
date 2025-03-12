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

import de.fraunhofer.aisec.cpg.graph.PrintDFGDirection.*
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdge
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.edges.flows.FieldDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.PointerDataflowGranularity
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

/**
 * Indicates the direction when building the DFG.
 * - [FORWARD] build the DFG starting from the given node.
 * - [BACKWARD] build the DFG ending at the given node.
 * - [BOTH] build the DFG from and to the given node.
 */
enum class PrintDFGDirection {
    FORWARD,
    BACKWARD,
    BOTH,
}

/** Utility function to print the DFG using [printGraph]. */
fun Node.printDFG2(
    maxConnections: Int = 25,
    selector: (Node) -> Boolean = { true },
    vararg strategies: (Node) -> Iterator<Dataflow> =
        arrayOf<(Node) -> Iterator<Dataflow>>(
            Strategy::DFG_EDGES_FORWARD,
            Strategy::DFG_EDGES_BACKWARD,
            Strategy::MEMORY_VALUES_FORWARD,
            Strategy::MEMORY_VALUES_BACKWARD,
        ),
): String {
    return this.printGraphNew(maxConnections = maxConnections, selector, *strategies)
}

/** Utility function to print the DFG using [printGraph]. */
fun Node.printDFG(
    maxConnections: Int = 25,
    selector: (Node) -> Boolean = { true },
    vararg strategies: (Node) -> Iterator<Dataflow> =
        arrayOf<(Node) -> Iterator<Dataflow>>(
            Strategy::DFG_EDGES_FORWARD,
            Strategy::DFG_EDGES_BACKWARD,
        ),
): String {
    return this.printGraph(maxConnections = maxConnections, selector, *strategies)
}

/** Utility function to print the EOG using [printGraph]. */
fun Node.printEOG(
    maxConnections: Int = 25,
    selector: (Node) -> Boolean = { true },
    vararg strategies: (Node) -> Iterator<EvaluationOrder> =
        arrayOf<(Node) -> Iterator<EvaluationOrder>>(
            Strategy::EOG_EDGES_FORWARD,
            Strategy::EOG_EDGES_BACKWARD,
        ),
): String {
    return this.printGraph(maxConnections, selector, *strategies)
}

/** Utility function to print the AST using [printGraph]. */
fun Node.printAST(
    maxConnections: Int = 25,
    selector: (Node) -> Boolean = { true },
    vararg strategies: (Node) -> Iterator<AstEdge<out Node>> =
        arrayOf<(Node) -> Iterator<AstEdge<out Node>>>(
            Strategy::AST_EDGES_FORWARD,
            Strategy::AST_EDGES_BACKWARD,
        ),
): String {
    return this.printGraph(maxConnections, selector, *strategies)
}

data class Quadtuple(
    val start: Node,
    val end: Node,
    val nextRelevant: Node,
    val relevantEdge: Boolean,
) {
    val component1: Node
        get() = start

    val component2: Node
        get() = end

    val component3: Node
        get() = nextRelevant

    val component4: Boolean
        get() = relevantEdge
}

fun <EdgeType : Edge<out Node>> nextStep(
    edge: EdgeType,
    lastRelevant: Node?,
    selector: (Node) -> Boolean = { true },
    strategy: (Node) -> Iterator<EdgeType>,
): Quadtuple {
    val isForward = strategy(edge.start).asSequence().firstOrNull()?.end == edge.end
    var start: Node
    var end: Node
    var nextRelevant: Node
    var relevantEdge = false
    if (isForward) {
        start = lastRelevant ?: edge.start
        end = edge.end
        nextRelevant =
            if (selector(end)) {
                relevantEdge = true
                end
            } else {
                lastRelevant ?: edge.start
            }
    } else {
        start = edge.start
        end = lastRelevant ?: edge.end
        nextRelevant =
            if (selector(start)) {
                relevantEdge = true
                start
            } else {
                // TODO: Might be useful to propagate/configure edge labels.
                lastRelevant ?: edge.end
            }
    }
    return Quadtuple(start, end, nextRelevant, relevantEdge)
}

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
fun <EdgeType : Edge<out Node>> Node.printGraphNew(
    maxConnections: Int = 25,
    selector: (Node) -> Boolean = { true },
    vararg strategies: (Node) -> Iterator<EdgeType>,
): String {
    val builder = StringBuilder()

    builder.append("```mermaid\n")
    builder.append("flowchart TD\n")

    // We use a set with a defined ordering to hold our work-list to have a somewhat consistent
    // ordering of statements in the mermaid file.
    val worklist = LinkedHashSet<Triple<EdgeType, Node, Node>>()
    val alreadySeen = identitySetOf<EdgeType>()
    var conns = 0

    strategies.forEach { strategy ->
        worklist +=
            strategy(this)
                .asSequence()
                .filter { it !in alreadySeen }
                .sortedBy { it.end.name }
                .map { Triple(it, it.start, it.end) }
    }

    while (worklist.isNotEmpty() && conns < maxConnections) {
        // Take one edge out of the work-list
        val item = worklist.first()
        val (edge, startNode, endNode) = worklist.first()
        worklist.remove(item)

        if (edge in alreadySeen) {
            continue
        }

        // Add it to the seen-list
        alreadySeen += edge

        // val start = edge.start
        // val end = edge.end
        val isForward = endNode == edge.end
        if (
            ((isForward && (selector(endNode) || endNode == this)) ||
                (!isForward && (selector(startNode) || startNode == this))) && startNode != endNode
        ) {
            builder.append(
                "${startNode.hashCode()}[\"${startNode.nodeLabel}\"]-->|${edge.label()}|${endNode.hashCode()}[\"${endNode.nodeLabel}\"]\n"
            )
            conns++
        }

        // Add start and edges to the work-list.
        strategies.forEach { strategy ->
            if (strategy(edge.start).asSequence().firstOrNull()?.end == edge.end) {
                // Is forward strategy
                worklist +=
                    strategy(edge.end)
                        .asSequence()
                        .sortedBy { it.end.name }
                        .map { Triple(it, if (selector(it.start)) it.start else startNode, it.end) }
                worklist +=
                    strategy(edge.start)
                        .asSequence()
                        .sortedBy { it.end.name }
                        .map { Triple(it, if (selector(it.start)) it.start else startNode, it.end) }
            } else {
                // Is backward strategy
                worklist +=
                    strategy(edge.end)
                        .asSequence()
                        .sortedBy { it.end.name }
                        .map { Triple(it, it.start, if (selector(it.end)) it.end else endNode) }
                worklist +=
                    strategy(edge.start)
                        .asSequence()
                        .sortedBy { it.end.name }
                        .map { Triple(it, it.start, if (selector(it.end)) it.end else endNode) }
            }
        }
    }

    builder.append("```")

    return builder.toString()
}

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
fun <EdgeType : Edge<out Node>> Node.printGraph(
    maxConnections: Int = 25,
    selector: (Node) -> Boolean = { true },
    vararg strategies: (Node) -> Iterator<EdgeType>,
): String {
    val builder = StringBuilder()

    builder.append("```mermaid\n")
    builder.append("flowchart TD\n")

    // We use a set with a defined ordering to hold our work-list to have a somewhat consistent
    // ordering of statements in the mermaid file.
    val worklist = LinkedHashSet<Pair<EdgeType, MutableMap<(Node) -> Iterator<EdgeType>, Node>>>()
    val alreadySeen = identitySetOf<EdgeType>()
    var conns = 0

    strategies.forEach { strategy ->
        val nextSteps =
            strategy(this).asSequence().filter { it !in alreadySeen }.sortedBy { it.end.name }

        worklist +=
            nextSteps.map {
                it to
                    mutableMapOf(
                        *strategies
                            .map { str ->
                                val nextStep = nextStep(it, this, selector, strategy)
                                str to nextStep.nextRelevant
                            }
                            .toTypedArray()
                    )
            }
    }

    while (worklist.isNotEmpty() && conns < maxConnections) {
        // Take one edge out of the work-list
        val currentElement = worklist.first()
        val (edge, lastRelevantMap) = currentElement
        worklist.remove(currentElement)

        if (edge in alreadySeen) {
            continue
        }

        // Add it to the seen-list
        alreadySeen += edge

        for (strategy in strategies) {
            val lastRelevant = lastRelevantMap[strategy]
            val (start, end, nextRelevant, relevantEdge) =
                nextStep(edge, lastRelevant, selector, strategy)

            if (relevantEdge) {
                builder.append(
                    "${start.hashCode()}[\"${start.nodeLabel}\"]-->|${edge.label()}|${end.hashCode()}[\"${end.nodeLabel}\"]\n"
                )
                conns++
            }

            // Add start and edges to the work-list.
            worklist +=
                strategy(end)
                    .asSequence()
                    .sortedBy { it.end.name }
                    .map {
                        it to
                            mutableMapOf(
                                *strategies
                                    .map { str ->
                                        val nextStep =
                                            nextStep(
                                                it,
                                                lastRelevantMap[strategy],
                                                selector,
                                                strategy,
                                            )
                                        str to nextStep.nextRelevant
                                    }
                                    .toTypedArray()
                            )
                    } // TODO: Also make the map in both directions here.
            worklist +=
                strategy(start)
                    .asSequence()
                    .sortedBy { it.end.name }
                    .map {
                        it to
                            mutableMapOf(
                                *strategies
                                    .map { str ->
                                        val nextStep =
                                            nextStep(
                                                it,
                                                lastRelevantMap[strategy],
                                                selector,
                                                strategy,
                                            )
                                        str to nextStep.nextRelevant
                                    }
                                    .toTypedArray()
                            )
                    } // TODO: Also make the map in both directions here.
        }
    }

    builder.append("```")

    return builder.toString()
}

private fun Edge<out Node>.label(): String {
    val builder = StringBuilder()
    builder.append("\"")
    builder.append(this.labels.joinToString(","))

    if (this is Dataflow) {
        var granularity = this.granularity
        if (granularity is FieldDataflowGranularity) {
            builder.append(" (partial, ${granularity.partialTarget?.name})")
        } else if (granularity is PointerDataflowGranularity) {
            builder.append(" (pointer, ${granularity.pointerTarget.name})")
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
