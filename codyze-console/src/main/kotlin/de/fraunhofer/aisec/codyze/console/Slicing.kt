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
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.flows.ControlDependence
import de.fraunhofer.aisec.cpg.graph.edges.flows.DependenceType
import de.fraunhofer.aisec.cpg.graph.edges.flows.ProgramDependence
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Call
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

/** How many statements around a statement are affected by it or affect it, without itself. */
@Serializable data class SliceCountsJSON(val backward: Int, val forward: Int)

private val Node.sliceFunction: Function?
    get() = this as? Function ?: firstParentOrNull<Function>()

/**
 * Whether the code of a statement is not part of the analysis, e.g. a function that is not
 * declared.
 */
private val Node.isOutsideOfAnalysis: Boolean
    get() = location == null || isInferred || sliceFunction?.isInferred == true

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

/** A statement of the slice with the context of the analysis it was reached in, e.g. its calls. */
private data class SliceState(val statement: Node, val context: Context)

private class SliceCollector(
    root: Node,
    val direction: SliceDirection,
    val hops: Int,
    val graph: DependenceGraph = DependenceGraph.PDG,
    /** Whether the slice follows dependences into other functions or stops at its function */
    val interprocedural: Boolean = true,
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

    private val backward = direction == SliceDirection.BACKWARD

    // The context of calls is kept like in the dataflow analysis of the CPG
    private val analysisDirection =
        if (backward) Backward(GraphToFollow.DFG) else Forward(GraphToFollow.DFG)

    init {
        if (function != null || interprocedural) collect()
    }

    /**
     * Whether a statement becomes a stub: code that is not analysed, or outside of the function.
     */
    private fun isStub(statement: Node) =
        statement.isOutsideOfAnalysis || (!interprocedural && statement.sliceFunction !== function)

    /**
     * The dependences of a statement in the direction of the slice, each with the context after
     * following it. If [interprocedural], dependences are followed like the CPG follows the PDG
     * across functions: dataflows into and out of functions only in their calling context (see
     * [ContextSensitive]), and from a function to the calls of it (backward) or from a call to the
     * functions it invokes (forward).
     */
    private fun dependencesOf(state: SliceState): List<Pair<StatementDependence, Context>> =
        state.statement.statementMembers().flatMap { member ->
            val edges = if (backward) member.prevPDGEdges else member.nextPDGEdges
            val dependences =
                edges.mapNotNull { edge ->
                    val kind =
                        when ((edge as? ProgramDependence)?.dependence) {
                            DependenceType.DATA -> GraphEdgeKind.DATA
                            DependenceType.CONTROL -> GraphEdgeKind.CONTROL
                            null -> return@mapNotNull null
                        }
                    if (kind !in graph.kinds) return@mapNotNull null
                    val context = state.context.clone()
                    if (
                        interprocedural &&
                            !ContextSensitive.followEdge(
                                member,
                                edge,
                                context,
                                emptyList(),
                                mutableSetOf(),
                                analysisDirection,
                                true,
                            )
                    ) {
                        return@mapNotNull null
                    }
                    dependence(edge.start, edge.end, kind, labelOf(edge))?.let { it to context }
                }
            dependences + callsOf(member, state.context)
        }

    /** The edges between functions and their calls the CPG follows in an interprocedural PDG. */
    private fun callsOf(member: Node, context: Context): List<Pair<StatementDependence, Context>> {
        if (!interprocedural || GraphEdgeKind.CONTROL !in graph.kinds) return emptyList()
        return if (backward) {
            (member as? Function)?.usageEdges.orEmpty().mapNotNull { edge ->
                val call = edge.end.astParent as? Call ?: return@mapNotNull null
                val next = context.clone().also { it.callStack.push(call) }
                dependence(edge.end, member, GraphEdgeKind.CONTROL, null)?.let { it to next }
            }
        } else {
            (member as? Call)?.invokeEdges.orEmpty().mapNotNull { edge ->
                dependence(member, edge.end, GraphEdgeKind.CONTROL, null)?.let {
                    it to context.clone()
                }
            }
        }
    }

    /** The dependence of the statement of [end] on the statement of [start], if they differ. */
    private fun dependence(
        start: Node,
        end: Node,
        kind: GraphEdgeKind,
        label: String?,
    ): StatementDependence? {
        val from = start.enclosingStatement()
        val to = end.enclosingStatement()
        if (from === to || from is Block || to is Block) return null
        return StatementDependence(from, to, kind, label)
    }

    private fun labelOf(edge: Edge<Node>): String? =
        when (edge) {
            is ControlDependence -> edge.branches.singleOrNull()?.toString()
            else ->
                listOf(edge.end, edge.start).firstNotNullOfOrNull {
                    (it as? Reference)?.name?.localName
                        ?: (it as? ValueDeclaration)?.name?.localName
                }
        }

    private fun otherEnd(dependence: StatementDependence) =
        if (direction == SliceDirection.BACKWARD) dependence.from else dependence.to

    private fun collect() {
        depths[rootStatement.id] = 0
        statements[rootStatement.id] = rootStatement
        val visited = mutableSetOf(rootStatement.id to emptyList<Uuid>())
        var frontier = listOf(SliceState(rootStatement, Context(steps = 0)))
        for (depth in 0..hops) {
            val next = mutableListOf<SliceState>()
            for (state in frontier) {
                val found = dependencesOf(state)
                if (depth == hops) {
                    // At the border only the dependences inside of the slice are kept, the others
                    // are counted
                    found
                        .filter { (dependence, _) -> otherEnd(dependence).id in depths }
                        .forEach { (dependence, _) ->
                            dependences.putIfAbsent(dependence.key, dependence)
                        }
                    more[state.statement.id] =
                        found
                            .map { (dependence, _) -> otherEnd(dependence) }
                            .filter { it.id !in depths && !isStub(it) }
                            .distinctBy { it.id }
                            .size
                    continue
                }
                for ((dependence, context) in found) {
                    val other = otherEnd(dependence)
                    when {
                        isStub(other) -> stubs[other.id] = other
                        other.id !in depths && depths.size >= MAX_SLICE_NODES -> {
                            truncated = true
                            continue
                        }
                        else -> {
                            // The same statement may be reached through other calls, which can
                            // lead further
                            if (visited.add(other.id to context.callStack.toList().map { it.id })) {
                                next += SliceState(other, context)
                            }
                            if (other.id !in depths) {
                                depths[other.id] = depth + 1
                                statements[other.id] = other
                            }
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
 * The slice of the program dependence graph around the statement of [root]: the statements that
 * affect it ([SliceDirection.BACKWARD]) or are affected by it ([SliceDirection.FORWARD]), up to
 * [hops] dependences away, following the dependences of [graph]. If [interprocedural], the slice
 * follows the dependences across functions like the CPG does (see [SliceCollector.dependencesOf]);
 * otherwise it stays in the function of [root]. Dependences into code that is not analysed (or out
 * of the function) end in stub nodes.
 */
fun slice(
    root: Node,
    direction: SliceDirection,
    hops: Int,
    graph: DependenceGraph = DependenceGraph.PDG,
    interprocedural: Boolean = true,
): GraphSliceJSON {
    val collector = SliceCollector(root, direction, hops, graph, interprocedural)
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

/** The number of statements in the slice of [root] in each direction, without the root. */
fun sliceCounts(root: Node, hops: Int, interprocedural: Boolean = true): SliceCountsJSON =
    SliceCountsJSON(
        backward =
            SliceCollector(root, SliceDirection.BACKWARD, hops, interprocedural = interprocedural)
                .statements
                .size - 1,
        forward =
            SliceCollector(root, SliceDirection.FORWARD, hops, interprocedural = interprocedural)
                .statements
                .size - 1,
    )
