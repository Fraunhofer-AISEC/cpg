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
package de.fraunhofer.aisec.cpg.serialization

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.component
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.translationUnit
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

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

/** A node as a reference, labelled `Operation` or `Concept` if it is one of them. */
fun Node.toOverlayRefJSON(): NodeRefJSON =
    toRefJSON(
        label =
            when (this) {
                is Operation -> "Operation"
                is Concept -> "Concept"
                else -> null
            }
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

/** How well the analysis knows the target of a call. */
enum class CallStatus {
    /** The target is part of the analysed code. */
    RESOLVED,
    /** The target is not part of the analysed code, e.g. a library function. */
    EXTERNAL,
    /** The target could not be determined at all. */
    UNRESOLVED,
}

val Call.status: CallStatus
    get() =
        when {
            invokes.isEmpty() -> CallStatus.UNRESOLVED
            invokes.all { it.isInferred } -> CallStatus.EXTERNAL
            else -> CallStatus.RESOLVED
        }
