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
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.ControlDependence
import de.fraunhofer.aisec.cpg.graph.edges.flows.DependenceType
import de.fraunhofer.aisec.cpg.graph.edges.flows.ProgramDependence
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.CatchClause
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.IfElse
import de.fraunhofer.aisec.cpg.graph.expressions.Loop
import de.fraunhofer.aisec.cpg.graph.expressions.OperatorCall
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Switch
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/** Upper bound for the nodes of a slice, so that hub statements do not blow up the response. */
private const val MAX_SLICE_NODES = 80

/** In which direction the program dependence graph is followed from a node. */
enum class PdgDirection {
    /** To what the node depends on, i.e. what affects it. */
    BACKWARD,
    /** To what depends on the node, i.e. what it affects. */
    FORWARD,
}

enum class PdgNodeKind {
    /** A plain statement. */
    STATEMENT,
    /** A statement that branches, i.e. an if, a loop, a switch or a catch clause. */
    BRANCH,
    /** Code outside of the function of the slice, in another file or not analysed at all. */
    STUB,
}

enum class PdgEdgeKind {
    DATA,
    CONTROL,
}

/**
 * A statement of a PDG slice. The nodes of the program dependence graph (references, literals,
 * calls, ...) are aggregated to the statement that contains them.
 *
 * @property id The ID of the statement node, which is also used by the edges.
 * @property depth The number of hops from the root of the slice.
 * @property more The number of statements further away that are not part of the slice because of
 *   the hop limit, so that the UI can offer to expand it.
 * @property concept Whether a concept or operation is attached to the statement.
 * @property unresolved Whether the statement has a call whose target is unknown.
 * @property external Whether the statement has a call to code that is not analysed. For a stub: the
 *   code is not part of the analysis at all.
 * @property warning Whether the analysis could not handle the code of the statement.
 */
@Serializable
data class PdgNodeJSON(
    val id: String,
    val kind: PdgNodeKind,
    val node: NodeRefJSON,
    val startLine: Int,
    val endLine: Int,
    val code: String,
    val depth: Int,
    val more: Int,
    val concept: Boolean,
    val unresolved: Boolean,
    val external: Boolean,
    val warning: Boolean,
)

/**
 * A dependence between two statements, from the statement that is depended on to the one that
 * depends on it.
 *
 * @property label For data dependences the name of the variable, for control dependences `true` or
 *   `false` for the branch of the condition.
 */
@Serializable
data class PdgEdgeJSON(val from: String, val to: String, val kind: PdgEdgeKind, val label: String?)

/** The statements that affect a statement (or are affected by it) in the same function. */
@Serializable
data class PdgSliceJSON(
    val root: String,
    val direction: PdgDirection,
    val hops: Int,
    /** The function the slice is limited to */
    val function: NodeRefJSON?,
    val nodes: List<PdgNodeJSON>,
    val edges: List<PdgEdgeJSON>,
    /** Whether statements are missing because the slice reached the upper bound of nodes */
    val truncated: Boolean,
)

/** The number of statements in the function that are affected by or affect a statement. */
@Serializable data class PdgCountsJSON(val backward: Int, val forward: Int)

/** Whether this node is the body of the control flow statement [parent], e.g. its then branch. */
private fun Node.isBodyOf(parent: Node): Boolean =
    when (parent) {
        is IfElse -> this === parent.thenStatement || this === parent.elseStatement
        is Loop -> this === parent.statement || this === parent.elseStatement
        is Switch -> this === parent.statement
        else -> false
    }

/**
 * The statement a node belongs to: the outermost expression that is not part of another statement.
 * The header of an if or a loop (its condition, the initializer and iteration of a for loop)
 * belongs to it, while its body consists of statements of their own.
 */
internal fun Node.pdgStatement(): Node {
    var current: Node = this
    while (true) {
        val parent = current.astParent ?: return current
        if (parent !is Expression || parent is Block || current.isBodyOf(parent)) return current
        current = parent
    }
}

/** The nodes of a statement, without the statements in its body or in nested blocks. */
private fun Node.pdgMembers(): List<Node> {
    val members = mutableListOf<Node>()
    fun visit(node: Node) {
        members += node
        (node as? AstNode)?.astChildren?.forEach { child ->
            if (child !is Block && !child.isBodyOf(node)) visit(child)
        }
    }
    visit(this)
    return members
}

private val Node.pdgFunction: Function?
    get() = this as? Function ?: firstParentOrNull<Function>()

private val Node.isBranching: Boolean
    get() = this is IfElse || this is Loop || this is Switch || this is CatchClause

/**
 * A dependence between two statements, before it is turned into JSON. Nodes are compared by their
 * ID, not structurally, see [key].
 */
private class StatementDependence(
    val from: Node,
    val to: Node,
    val kind: PdgEdgeKind,
    val label: String?,
) {
    val key = listOf(from.id, to.id, kind, label)
}

private class PdgCollector(root: Node, val direction: PdgDirection, val hops: Int) {
    val rootStatement = root.pdgStatement()
    val function = rootStatement.pdgFunction
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

    private fun isInFunction(statement: Node) = statement.pdgFunction === function

    /** The dependences of a statement in the direction of the slice. */
    private fun dependencesOf(statement: Node): List<StatementDependence> =
        statement.pdgMembers().flatMap { member ->
            val edges =
                if (direction == PdgDirection.BACKWARD) member.prevPDGEdges else member.nextPDGEdges
            edges.mapNotNull { edge ->
                val kind =
                    when ((edge as? ProgramDependence)?.dependence) {
                        DependenceType.DATA -> PdgEdgeKind.DATA
                        DependenceType.CONTROL -> PdgEdgeKind.CONTROL
                        null -> return@mapNotNull null
                    }
                val from = edge.start.pdgStatement()
                val to = edge.end.pdgStatement()
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
        if (direction == PdgDirection.BACKWARD) dependence.from else dependence.to

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

private fun Node.toPdgNode(kind: PdgNodeKind, depth: Int, more: Int): PdgNodeJSON {
    // Stubs are shown as they are, without everything they contain
    val members = if (kind == PdgNodeKind.STUB) listOf(this) else pdgMembers()
    val calls = members.filterIsInstance<Call>()
    val statuses = calls.filter { it !is OperatorCall }.map { it.status }
    val startLine = location?.region?.startLine ?: -1
    val endLine =
        if (isBranching) {
            ((this as? BranchingNode)?.branchedBy?.location?.region?.endLine ?: startLine)
        } else {
            location?.region?.endLine ?: startLine
        }
    return PdgNodeJSON(
        id = id.toString(),
        kind = kind,
        node = toRefJSON(),
        startLine = startLine,
        endLine = maxOf(endLine, startLine),
        code = toRefJSON().code,
        depth = depth,
        more = more,
        concept = members.any { m -> m.overlays.any { it is Concept || it is Operation } },
        unresolved = statuses.any { it == CallStatus.UNRESOLVED },
        external =
            if (kind == PdgNodeKind.STUB) isInferred || location == null
            else statuses.any { it == CallStatus.EXTERNAL },
        warning = members.any { it is ProblemNode },
    )
}

/**
 * The slice of the program dependence graph around the statement of [root]: the statements of its
 * function that affect it ([PdgDirection.BACKWARD]) or are affected by it ([PdgDirection.FORWARD]),
 * up to [hops] dependences away. Dependences that leave the function end in stub nodes.
 */
fun pdgSlice(root: Node, direction: PdgDirection, hops: Int): PdgSliceJSON {
    val collector = PdgCollector(root, direction, hops)
    val nodes =
        collector.statements.values.map {
            it.toPdgNode(
                if (it.isBranching) PdgNodeKind.BRANCH else PdgNodeKind.STATEMENT,
                collector.depths.getValue(it.id),
                collector.more[it.id] ?: 0,
            )
        } + collector.stubs.values.map { it.toPdgNode(PdgNodeKind.STUB, hops + 1, 0) }
    return PdgSliceJSON(
        root = collector.rootStatement.id.toString(),
        direction = direction,
        hops = hops,
        function = collector.function?.toRefJSON(),
        nodes = nodes,
        edges =
            collector.dependences.values.map {
                PdgEdgeJSON(it.from.id.toString(), it.to.id.toString(), it.kind, it.label)
            },
        truncated = collector.truncated,
    )
}

/** The number of statements in the function of [root] in each direction, without the root. */
fun pdgCounts(root: Node, hops: Int): PdgCountsJSON =
    PdgCountsJSON(
        backward = PdgCollector(root, PdgDirection.BACKWARD, hops).statements.size - 1,
        forward = PdgCollector(root, PdgDirection.FORWARD, hops).statements.size - 1,
    )
