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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CallStatus
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.status
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.OperatorCall
import de.fraunhofer.aisec.cpg.serialization.NodeJSON
import kotlinx.serialization.Serializable

/** A function together with its key figures. */
@Serializable
data class FunctionCallStatsJSON(
    val function: NodeJSON,
    val callers: Int,
    val callees: Int,
    val externalCalls: Int,
    val unresolvedCalls: Int,
)

/**
 * A call and how well the analysis knows its target.
 *
 * @property callee The expression naming the target (e.g. `fopen` in `fopen(path, "r")`), if it has
 *   a location.
 */
@Serializable
data class CallSiteJSON(val call: NodeJSON, val callee: NodeJSON?, val status: CallStatus)

/**
 * A concept or operation and the node it is attached to.
 *
 * @property category The area of the overlay, derived from its package, e.g. `crypto` or `file`.
 * @property concept For an operation: the concept it belongs to. `null` for a concept.
 */
@Serializable
data class OverlayJSON(
    val overlay: NodeJSON,
    val category: String,
    val underlyingNode: NodeJSON?,
    val concept: NodeJSON?,
)

/**
 * Whether this node is located in the file of the given translation unit. The AST of a translation
 * unit can also contain nodes of other files, e.g. the declarations of included headers in C (see
 * `loadIncludes`), whose positions refer to these files. If the unit has no file, any node with a
 * location is considered to be in it.
 */
fun Node.isInFileOf(translationUnit: TranslationUnit): Boolean {
    val location = location ?: return false
    val file = translationUnit.location?.artifactLocation?.uri ?: return true
    return location.artifactLocation.uri == file
}

/** The nodes of this translation unit that are located in its file, see [isInFileOf]. */
private val TranslationUnit.ownNodes: List<Node>
    get() = nodes.filter { it.translationUnit == this && it.isInFileOf(this) }

/** The calls written in the code, i.e. without operator calls and implicit calls. */
private fun Iterable<Node>.explicitCalls(): List<Call> =
    filterIsInstance<Call>().filter { it !is OperatorCall && !it.isImplicit }

/** The functions of this translation unit that have a body, with their key figures. */
fun TranslationUnit.toFunctionCallStatsJSON(): List<FunctionCallStatsJSON> =
    functions
        .filter { !it.isImplicit && it.body != null && it.isInFileOf(this) }
        .map { function ->
            val statuses = function.body.calls.filter { it !is OperatorCall }.map { it.status }
            FunctionCallStatsJSON(
                function = function.toRefJSON(),
                callers = function.calledBy.size,
                callees = statuses.size,
                externalCalls = statuses.count { it == CallStatus.EXTERNAL },
                unresolvedCalls = statuses.count { it == CallStatus.UNRESOLVED },
            )
        }

/** The calls written in this translation unit, with the status of their targets. */
fun TranslationUnit.toCallSitesJSON(): List<CallSiteJSON> =
    ownNodes.explicitCalls().map { call ->
        CallSiteJSON(
            call = call.toRefJSON(),
            callee = call.callee.takeIf { it.location != null }?.toRefJSON(),
            status = call.status,
        )
    }

/** The concepts and operations attached to the nodes of this translation unit. */
fun TranslationUnit.toOverlaysJSON(): List<OverlayJSON> =
    ownNodes.flatMap { node -> node.overlays.mapNotNull { it.toOverlayJSON() } }

/** This node as [OverlayJSON], or `null` if it is neither a [Concept] nor an [Operation]. */
fun Node.toOverlayJSON(): OverlayJSON? {
    if (this !is Concept && this !is Operation) return null
    return OverlayJSON(
        overlay = toRefJSON(),
        category =
            javaClass.packageName.substringAfter("concepts.", "").substringBefore('.').ifEmpty {
                "other"
            },
        underlyingNode = (this as OverlayNode).underlyingNode?.toRefJSON(),
        concept = (this as? Operation)?.concept?.toRefJSON(),
    )
}
