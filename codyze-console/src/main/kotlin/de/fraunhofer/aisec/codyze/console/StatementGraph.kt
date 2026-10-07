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
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
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

/*
 * The graph of statements the console shows next to the code, for slices (see Slicing.kt) and for
 * the paths the tools of the agent find: the JSON models and how nodes are aggregated to statements.
 */

enum class GraphNodeKind {
    /** A plain statement. */
    STATEMENT,
    /** A statement that branches, i.e. an if, a loop, a switch or a catch clause. */
    BRANCH,
    /** Code outside of the function of the slice, in another file or not analysed at all. */
    STUB,
}

enum class GraphEdgeKind {
    DATA,
    CONTROL,
}

/**
 * A statement of a graph of statements, e.g. a slice. The nodes of the graph (references, literals,
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
data class GraphNodeJSON(
    val id: String,
    val kind: GraphNodeKind,
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
data class GraphEdgeJSON(
    val from: String,
    val to: String,
    val kind: GraphEdgeKind,
    val label: String?,
)

/**
 * A graph of statements: a slice (the statements that affect a statement or are affected by it in
 * the same function) or the statements of paths a tool found.
 */
@Serializable
data class GraphSliceJSON(
    val root: String,
    val direction: SliceDirection,
    val hops: Int,
    /** The function the slice is limited to */
    val function: NodeRefJSON?,
    val nodes: List<GraphNodeJSON>,
    val edges: List<GraphEdgeJSON>,
    /** Whether statements are missing because the slice reached the upper bound of nodes */
    val truncated: Boolean,
)

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
 * belongs to it, while its body consists of statements of their own. Likewise, the parameters of a
 * function belong to its header.
 */
internal fun Node.enclosingStatement(): Node {
    if (this is Parameter)
        (astParent as? Function)?.let {
            return it
        }
    var current: Node = this
    while (true) {
        val parent = current.astParent ?: return current
        if (parent !is Expression || parent is Block || current.isBodyOf(parent)) return current
        current = parent
    }
}

/** The nodes of a statement, without the statements in its body or in nested blocks. */
internal fun Node.statementMembers(): List<Node> {
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

internal val Node.isBranching: Boolean
    get() = this is IfElse || this is Loop || this is Switch || this is CatchClause

internal fun Node.toGraphNode(kind: GraphNodeKind, depth: Int, more: Int): GraphNodeJSON {
    // Stubs are shown as they are, without everything they contain
    val members = if (kind == GraphNodeKind.STUB) listOf(this) else statementMembers()
    val calls = members.filterIsInstance<Call>()
    val statuses = calls.filter { it !is OperatorCall }.map { it.status }
    val startLine = location?.region?.startLine ?: -1
    // Branches and functions are shown by their head, without their body
    val endLine =
        when {
            isBranching ->
                ((this as? BranchingNode)?.branchedBy?.location?.region?.endLine ?: startLine)
            this is Function -> startLine
            else -> location?.region?.endLine ?: startLine
        }
    return GraphNodeJSON(
        id = id.toString(),
        kind = kind,
        node = toRefJSON(),
        startLine = startLine,
        endLine = maxOf(endLine, startLine),
        // Code that is not analysed, e.g. a function that is not declared, has no code but a name
        code = toRefJSON().code.ifEmpty { name.localName },
        depth = depth,
        more = more,
        concept = members.any { m -> m.overlays.any { it is Concept || it is Operation } },
        unresolved = statuses.any { it == CallStatus.UNRESOLVED },
        external =
            if (kind == GraphNodeKind.STUB) isInferred || location == null
            else statuses.any { it == CallStatus.EXTERNAL },
        warning = members.any { it is ProblemNode },
    )
}

/** The name a value has when it reaches [node], to label the edge to its statement. */
private fun valueName(node: Node): String? =
    when (node) {
        is Reference,
        is ValueDeclaration -> node.name.localName.ifEmpty { null }
        else -> null
    }

/**
 * The statements of [paths] and the steps between them, in the format of a slice so that the
 * console shows them like one. Consecutive nodes of a path that belong to the same statement are
 * one statement; a step between two statements becomes an edge, a data dependence for dataflows
 * ([kind] `dataflow`) and a control dependence otherwise. Nodes outside of the analysed code are
 * stubs. The root is the end of the first path, and the depth of a statement is its distance from
 * the end of its path.
 */
fun pathsGraph(paths: List<List<Node>>, kind: String): GraphSliceJSON {
    val statements = linkedMapOf<Uuid, Node>()
    val depths = mutableMapOf<Uuid, Int>()
    val edges = linkedMapOf<Pair<Uuid, Uuid>, GraphEdgeJSON>()
    val edgeKind = if (kind == "dataflow") GraphEdgeKind.DATA else GraphEdgeKind.CONTROL
    for (path in paths) {
        // The statements of the path, each with the node the path enters it with
        val steps = mutableListOf<Pair<Node, Node>>()
        for (node in path) {
            val statement = if (node.location == null) node else node.enclosingStatement()
            if (steps.lastOrNull()?.first !== statement) steps += statement to node
        }
        steps.forEachIndexed { i, (statement, _) ->
            statements.putIfAbsent(statement.id, statement)
            val depth = steps.size - 1 - i
            depths[statement.id] = minOf(depths[statement.id] ?: depth, depth)
        }
        steps.zipWithNext().forEach { (from, to) ->
            val key = from.first.id to to.first.id
            edges.putIfAbsent(
                key,
                GraphEdgeJSON(
                    from = from.first.id.toString(),
                    to = to.first.id.toString(),
                    kind = edgeKind,
                    label = if (edgeKind == GraphEdgeKind.DATA) valueName(to.second) else null,
                ),
            )
        }
    }
    val root = paths.firstOrNull()?.lastOrNull()
    val rootStatement = root?.let { if (it.location == null) it else it.enclosingStatement() }
    return GraphSliceJSON(
        root = rootStatement?.id?.toString() ?: "",
        direction = SliceDirection.BACKWARD,
        hops = paths.maxOfOrNull { it.size } ?: 0,
        function = null,
        nodes =
            statements.values.map {
                val nodeKind =
                    when {
                        it.location == null || it.isInferred -> GraphNodeKind.STUB
                        it.isBranching -> GraphNodeKind.BRANCH
                        else -> GraphNodeKind.STATEMENT
                    }
                it.toGraphNode(nodeKind, depths.getValue(it.id), 0)
            },
        edges = edges.values.toList(),
        truncated = false,
    )
}
