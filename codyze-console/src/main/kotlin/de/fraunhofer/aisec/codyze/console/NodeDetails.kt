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
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edges.flows.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.PartialDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.sarif.Region
import de.fraunhofer.aisec.cpg.serialization.NodeJSON
import de.fraunhofer.aisec.cpg.serialization.toJSON
import kotlinx.serialization.Serializable

/**
 * Upper bound for each list in [NodeDetailsJSON], so that hub nodes do not blow up the response.
 */
private const val MAX_RELATED_NODES = 200

/** Upper bound for the code of a node that is only referenced, see [toRefJSON]. */
private const val MAX_SNIPPET_LENGTH = 120

/**
 * A node that is related to the node it was listed for, e.g. a neighbour in the dataflow.
 *
 * @property label Describes the relation, if it is not a plain one, e.g. the granularity and
 *   calling context of a dataflow.
 */
@Serializable data class RelatedNodeJSON(val node: NodeJSON, val label: String?)

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
    val node: NodeJSON,
    val enclosingFunction: NodeJSON?,
    val overlays: List<OverlayJSON>,
    val callTargets: List<NodeJSON>,
    val callers: List<NodeJSON>,
    val callees: List<NodeJSON>,
    val dataflowFrom: List<RelatedNodeJSON>,
    val dataflowTo: List<RelatedNodeJSON>,
    val warnings: List<String>,
)

/**
 * This node as [NodeJSON] for a reference to it, e.g. in a list: without its DFG edges and with
 * only the first line of its code, so that hub nodes and large functions do not blow up the
 * response.
 */
fun Node.toRefJSON(): NodeJSON = toJSON(noEdges = true, maxCodeLength = MAX_SNIPPET_LENGTH)

fun Node.toDetailsJSON(): NodeDetailsJSON {
    val warnings = mutableListOf<String>()
    if (isInferred) {
        warnings += "This node was inferred by the analysis; it has no source code of its own."
    }
    if (this is ProblemNode) {
        warnings += "The analysis could not handle this code (${problemType}): $problem"
    }

    val callTargets = (this as? Call)?.invokes.orEmpty()
    if (this is Call && status == CallStatus.UNRESOLVED) {
        warnings += "The call target could not be resolved; dataflows through it are unknown."
    }
    callTargets
        .filter { it.isInferred }
        .forEach {
            warnings +=
                "The target '${it.name}' is not part of the analysed code (inferred); its behaviour is unknown."
        }
    if (this is Function && body == null) {
        warnings +=
            "This function is only declared, its implementation is not part of the analysis."
    }

    return NodeDetailsJSON(
        node = toRefJSON(),
        enclosingFunction = (this as? Function ?: firstParentOrNull<Function>())?.toRefJSON(),
        overlays = overlays.mapNotNull { it.toOverlayJSON() },
        callTargets = callTargets.limited { it.toRefJSON() },
        callers = (this as? Function)?.calledBy.orEmpty().limited { it.toRefJSON() },
        callees = (this as? Function)?.body?.calls.orEmpty().limited { it.toRefJSON() },
        dataflowFrom =
            prevDFGEdges.limited { RelatedNodeJSON(it.start.toRefJSON(), it.describe()) },
        dataflowTo = nextDFGEdges.limited { RelatedNodeJSON(it.end.toRefJSON(), it.describe()) },
        warnings = warnings,
    )
}

/**
 * Returns the innermost node of this translation unit whose region contains the given position, so
 * that clicking anywhere in the code selects the most specific node. Among nodes with the same
 * region, explicit nodes are preferred over implicit ones and deeper nodes over their parents.
 */
fun TranslationUnit.nodeAt(line: Int, column: Int): Node? {
    var best: Node? = null
    var bestSpan = Long.MAX_VALUE
    // The nodes are in pre-order, so among nodes with the same region, a later one is deeper
    for (node in nodes) {
        if (node is TranslationUnit) continue
        val region = node.location?.region ?: continue
        if (!region.contains(line, column)) continue
        val span = region.span()
        if (span < bestSpan || span == bestSpan && (best?.isImplicit == true || !node.isImplicit)) {
            best = node
            bestSpan = span
        }
    }
    return best
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

/** Describes the granularity and calling context of a dataflow, if it is not a plain one. */
private fun Dataflow.describe(): String? {
    val parts = mutableListOf<String>()
    (granularity as? PartialDataflowGranularity<*>)?.partialTarget?.let { target ->
        parts += "partial: ${(target as? Node)?.name ?: target}"
    }
    if (this is ContextSensitiveDataflow) {
        // The contexts in the order of the flow, e.g. out of one call and into the next one
        for (context in callingContexts) {
            when (context) {
                is CallingContextIn -> parts += "into call"
                is CallingContextOut -> parts += "out of call"
            }
        }
    }
    return parts.joinToString(", ").ifEmpty { null }
}

private fun <T, R> Collection<T>.limited(transform: (T) -> R): List<R> =
    asSequence().take(MAX_RELATED_NODES).map(transform).toList()
