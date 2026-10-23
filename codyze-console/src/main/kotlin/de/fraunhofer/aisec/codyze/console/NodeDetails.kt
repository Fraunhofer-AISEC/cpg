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
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edges.flows.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.PartialDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.sarif.Region
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * Upper bound for each list in [NodeDetailsJSON], so that hub nodes do not blow up the response.
 */
private const val MAX_RELATED_NODES = 200

/** Upper bound for the code snippet of a [NodeRefJSON]. */
private const val MAX_SNIPPET_LENGTH = 120

/**
 * A compact reference to a node, with enough information to show it in a list and to navigate to it
 * (also across files).
 *
 * @property label An optional label describing how the node is related to the node it was listed
 *   for, e.g., the granularity of a dataflow.
 */
@Serializable
data class NodeRefJSON(
    val id: Uuid,
    val type: String,
    val name: String,
    val code: String,
    val fileName: String?,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val translationUnitId: Uuid?,
    val componentName: String?,
    val isInferred: Boolean,
    val label: String? = null,
)

/**
 * Everything the console knows about a single node that is relevant when inspecting it: its calls,
 * its direct dataflows, attached concepts and operations, and where the analysis is uncertain.
 *
 * The dataflows only contain the direct neighbours, so that the frontend can expand them lazily
 * into a tree.
 *
 * @property callTargets For a [Call]: the functions it invokes.
 * @property callers For a [Function]: the calls that invoke it.
 * @property callees For a [Function]: the calls in its body.
 * @property warnings Places where the analysis is incomplete or uncertain for this node.
 */
@Serializable
data class NodeDetailsJSON(
    val node: NodeRefJSON,
    val typeName: String?,
    val value: String?,
    val isImplicit: Boolean,
    val enclosingFunction: NodeRefJSON?,
    val overlays: List<NodeRefJSON>,
    val callTargets: List<NodeRefJSON>,
    val callers: List<NodeRefJSON>,
    val callees: List<NodeRefJSON>,
    val dataflowFrom: List<NodeRefJSON>,
    val dataflowTo: List<NodeRefJSON>,
    val warnings: List<String>,
)

fun Node.toRefJSON(label: String? = null): NodeRefJSON {
    val firstLine = (code ?: "").lineSequence().firstOrNull()?.trim() ?: ""
    return NodeRefJSON(
        id = id,
        type = javaClass.simpleName,
        name = name.toString(),
        code =
            if (firstLine.length > MAX_SNIPPET_LENGTH) firstLine.take(MAX_SNIPPET_LENGTH) + "…"
            else firstLine,
        fileName =
            location
                ?.artifactLocation
                ?.uri
                ?.toString()
                ?.substringAfterLast('/')
                ?.substringAfterLast('\\'),
        startLine = location?.region?.startLine ?: -1,
        startColumn = location?.region?.startColumn ?: -1,
        endLine = location?.region?.endLine ?: -1,
        endColumn = location?.region?.endColumn ?: -1,
        translationUnitId = translationUnit?.id,
        componentName = component?.name?.toString(),
        isInferred = isInferred,
        label = label,
    )
}

fun Node.toDetailsJSON(): NodeDetailsJSON {
    val warnings = mutableListOf<String>()
    if (isInferred) {
        warnings += "This node was inferred by the analysis; it has no source code of its own."
    }
    if (this is ProblemNode) {
        warnings += "The analysis could not handle this code (${problemType}): $problem"
    }

    val callTargets = (this as? Call)?.invokes.orEmpty()
    if (this is Call) {
        if (callTargets.isEmpty()) {
            warnings += "The call target could not be resolved; dataflows through it are unknown."
        }
        callTargets
            .filter { it.isInferred }
            .forEach {
                warnings +=
                    "The target '${it.name}' is not part of the analysed code (inferred); its behaviour is unknown."
            }
    }
    if (this is Function && body == null) {
        warnings +=
            "This function is only declared, its implementation is not part of the analysis."
    }

    return NodeDetailsJSON(
        node = toRefJSON(),
        typeName = (this as? HasType)?.type?.name?.toString(),
        value = constantValue(),
        isImplicit = isImplicit,
        enclosingFunction = (this as? Function ?: firstParentOrNull<Function>())?.toRefJSON(),
        overlays =
            overlays.map {
                it.toRefJSON(
                    label =
                        when (it) {
                            is Operation -> "Operation"
                            is Concept -> "Concept"
                            else -> null
                        }
                )
            },
        callTargets = callTargets.limited { it.toRefJSON() },
        callers = (this as? Function)?.calledBy.orEmpty().limited { it.toRefJSON() },
        callees = (this as? Function)?.body?.calls.orEmpty().limited { it.toRefJSON() },
        dataflowFrom = prevDFGEdges.limited { it.start.toRefJSON(label = it.describe()) },
        dataflowTo = nextDFGEdges.limited { it.end.toRefJSON(label = it.describe()) },
        warnings = warnings,
    )
}

/**
 * Returns the innermost node of this translation unit whose region contains the given position, so
 * that clicking anywhere in the code selects the most specific node. Among nodes with the same
 * region, explicit nodes are preferred over implicit ones and deeper nodes over their parents.
 *
 * If an end position is given, the node has to contain the whole range from the position to the end
 * (whose column is exclusive), e.g. the call that contains a selected text.
 */
fun TranslationUnit.nodeAt(
    line: Int,
    column: Int,
    endLine: Int? = null,
    endColumn: Int? = null,
): Node? {
    return nodes
        .withIndex()
        .filter { (_, node) ->
            val region = node.location?.region
            node !is TranslationUnit &&
                region != null &&
                region.contains(line, column) &&
                (endLine == null ||
                    endColumn == null ||
                    region.contains(endLine, maxOf(endColumn - 1, 1)))
        }
        .minWithOrNull(
            compareBy<IndexedValue<Node>> { (_, node) -> node.location?.region?.span() }
                .thenBy { (_, node) -> node.isImplicit }
                .thenByDescending { (index, _) -> index }
        )
        ?.value
}

private fun Region.contains(line: Int, column: Int): Boolean {
    if (line < startLine || line > endLine) return false
    if (line == startLine && column < startColumn) return false
    if (line == endLine && column >= endColumn) return false
    return true
}

/** The size of a region, where any additional line outweighs any number of columns. */
private fun Region.span(): Long =
    (endLine - startLine).toLong() * Int.MAX_VALUE + (endColumn - startColumn)

/**
 * Evaluates expressions to a constant, if possible. Only simple values are returned, everything
 * else (e.g., the node itself if it cannot be evaluated) is discarded.
 */
private fun Node.constantValue(): String? {
    if (this !is Expression || this is Call) return null
    val value = runCatching { evaluate() }.getOrNull()
    return when (value) {
        is Number,
        is Boolean,
        is Char,
        is String -> value.toString()
        else -> null
    }
}

/** Describes the granularity and calling context of a dataflow, if it is not a plain one. */
private fun Dataflow.describe(): String? {
    val parts = mutableListOf<String>()
    (granularity as? PartialDataflowGranularity<*>)?.partialTarget?.let { target ->
        parts += "partial: ${(target as? Node)?.name ?: target}"
    }
    if (this is ContextSensitiveDataflow) {
        when (callingContext) {
            is CallingContextIn -> parts += "into call"
            is CallingContextOut -> parts += "out of call"
        }
    }
    return parts.joinToString(", ").ifEmpty { null }
}

private fun <T, R> Collection<T>.limited(transform: (T) -> R): List<R> =
    asSequence().take(MAX_RELATED_NODES).map(transform).toList()
