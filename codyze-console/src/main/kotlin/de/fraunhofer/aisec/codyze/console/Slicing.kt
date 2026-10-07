/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.codyze.console

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.ControlDependence
import de.fraunhofer.aisec.cpg.graph.edges.flows.DependenceType
import de.fraunhofer.aisec.cpg.graph.edges.flows.ProgramDependence
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/** Upper bound for the nodes of a slice, so that hub statements do not blow up the response. */
private const val MAX_SLICE_NODES = 80

/** In which direction the program dependence graph is followed from a node. */
enum class SliceDirection {
    /** To what the node depends on, i.e. what affects it. */
    BACKWARD,
    /** To what depends on the node, i.e. what it affects. */
    FORWARD,
}

/**
 * Which dependences a slice follows: both kinds (the program dependence graph), only the data
 * dependences (the dataflow) or only the control dependences.
 */
enum class DependenceGraph(val kinds: Set<GraphEdgeKind>) {
    PDG(setOf(GraphEdgeKind.DATA, GraphEdgeKind.CONTROL)),
    DFG(setOf(GraphEdgeKind.DATA)),
    CDG(setOf(GraphEdgeKind.CONTROL)),
}

/** The number of statements in the function that are affected by or affect a statement. */
@Serializable data class SliceCountsJSON(val backward: Int, val forward: Int)

private val Node.sliceFunction: Function?
    get() = this as? Function ?: firstParentOrNull<Function>()

/**
 * A dependence between two statements, before it is turned into JSON. Nodes are compared by their
 * ID, not structurally, see [key].
 */
private class StatementDependence(
    val from: Node,
    val to: Node,
    val kind: GraphEdgeKind,
    val label: String?,
) {
    val key = listOf(from.id, to.id, kind, label)
}

private class SliceCollector(
    root: Node,
    val direction: SliceDirection,
    val hops: Int,
    val graph: DependenceGraph = DependenceGraph.PDG,
) {
    val rootStatement = root.enclosingStatement()
    val function = rootStatement.sliceFunction
    /** The statements of the slice, by their ID, and their distance from the root */
    val depths = LinkedHashMap<Uuid, Int>()
    val statements = LinkedHashMap<Uuid, Node>()
    val stubs = LinkedHashMap<Uuid, Node>()
    val more = HashMap<Uuid, Int>()
    val dependences = LinkedHashMap<List<Any?>, StatementDependence>()
    var truncated = false

    init {
        if (function != null) collect()
    }

    private fun isInFunction(statement: Node) = statement.sliceFunction === function

    /** The dependences of a statement in the direction of the slice. */
    private fun dependencesOf(statement: Node): List<StatementDependence> =
        statement.statementMembers().flatMap { member ->
            val edges =
                if (direction == SliceDirection.BACKWARD) member.prevPDGEdges
                else member.nextPDGEdges
            edges.mapNotNull { edge ->
                val kind =
                    when ((edge as? ProgramDependence)?.dependence) {
                        DependenceType.DATA -> GraphEdgeKind.DATA
                        DependenceType.CONTROL -> GraphEdgeKind.CONTROL
                        null -> return@mapNotNull null
                    }
                if (kind !in graph.kinds) return@mapNotNull null
                val from = edge.start.enclosingStatement()
                val to = edge.end.enclosingStatement()
                if (from === to || from is Block || to is Block) return@mapNotNull null
                val label =
                    when (edge) {
                        is ControlDependence -> edge.branches.singleOrNull()?.toString()
                        else ->
                            listOf(edge.end, edge.start).firstNotNullOfOrNull {
                                (it as? Reference)?.name?.localName
                                    ?: (it as? ValueDeclaration)?.name?.localName
                            }
                    }
                StatementDependence(from, to, kind, label)
            }
        }

    private fun otherEnd(dependence: StatementDependence) =
        if (direction == SliceDirection.BACKWARD) dependence.from else dependence.to

    private fun collect() {
        depths[rootStatement.id] = 0
        statements[rootStatement.id] = rootStatement
        var frontier = listOf(rootStatement)
        for (depth in 0..hops) {
            val next = mutableListOf<Node>()
            for (statement in frontier) {
                val found = dependencesOf(statement)
                if (depth == hops) {
                    // At the border only the dependences inside of the slice are kept, the others
                    // are
                    // counted
                    found
                        .filter { otherEnd(it).id in depths }
                        .forEach { dependences.putIfAbsent(it.key, it) }
                    more[statement.id] =
                        found
                            .map { otherEnd(it) }
                            .filter { it.id !in depths && isInFunction(it) }
                            .distinctBy { it.id }
                            .size
                    continue
                }
                for (dependence in found) {
                    val other = otherEnd(dependence)
                    when {
                        other.id in depths -> {}
                        !isInFunction(other) -> stubs[other.id] = other
                        depths.size >= MAX_SLICE_NODES -> {
                            truncated = true
                            continue
                        }
                        else -> {
                            depths[other.id] = depth + 1
                            statements[other.id] = other
                            next += other
                        }
                    }
                    dependences.putIfAbsent(dependence.key, dependence)
                }
            }
            frontier = next
        }
    }
}

/**
 * The slice of the program dependence graph around the statement of [root]: the statements of its
 * function that affect it ([SliceDirection.BACKWARD]) or are affected by it
 * ([SliceDirection.FORWARD]), up to [hops] dependences away, following the dependences of [graph].
 * Dependences that leave the function end in stub nodes.
 */
fun slice(
    root: Node,
    direction: SliceDirection,
    hops: Int,
    graph: DependenceGraph = DependenceGraph.PDG,
): GraphSliceJSON {
    val collector = SliceCollector(root, direction, hops, graph)
    val nodes =
        collector.statements.values.map {
            it.toGraphNode(
                if (it.isBranching) GraphNodeKind.BRANCH else GraphNodeKind.STATEMENT,
                collector.depths.getValue(it.id),
                collector.more[it.id] ?: 0,
            )
        } + collector.stubs.values.map { it.toGraphNode(GraphNodeKind.STUB, hops + 1, 0) }
    return GraphSliceJSON(
        root = collector.rootStatement.id.toString(),
        direction = direction,
        hops = hops,
        function = collector.function?.toRefJSON(),
        nodes = nodes,
        edges =
            collector.dependences.values.map {
                GraphEdgeJSON(it.from.id.toString(), it.to.id.toString(), it.kind, it.label)
            },
        truncated = collector.truncated,
    )
}

/** The number of statements in the function of [root] in each direction, without the root. */
fun sliceCounts(root: Node, hops: Int): SliceCountsJSON =
    SliceCountsJSON(
        backward = SliceCollector(root, SliceDirection.BACKWARD, hops).statements.size - 1,
        forward = SliceCollector(root, SliceDirection.FORWARD, hops).statements.size - 1,
    )
