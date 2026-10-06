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
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.OperatorCall
import kotlinx.serialization.Serializable

/** How well the analysis knows the target of a call. */
enum class CallStatus {
    /** The target is part of the analysed code. */
    RESOLVED,
    /** The target is not part of the analysed code, e.g. a library function. */
    EXTERNAL,
    /** The target could not be determined at all. */
    UNRESOLVED,
}

/** Key figures of a function, shown next to it in the code viewer. */
@Serializable
data class FunctionAnnotationJSON(
    val function: NodeRefJSON,
    val callers: Int,
    val callees: Int,
    val externalCalls: Int,
    val unresolvedCalls: Int,
)

/** A call in the code, located at its callee (e.g. only `fopen` in `fopen(path, "r")`). */
@Serializable
data class CallAnnotationJSON(
    val id: String,
    val name: String,
    val status: CallStatus,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
)

/**
 * A concept or operation in the code.
 *
 * @property category The area of the concept, derived from its package, e.g. `crypto` or `file`.
 */
@Serializable
data class ConceptAnnotationJSON(
    val id: String,
    val type: String,
    val category: String,
    val isOperation: Boolean,
    val line: Int,
)

/** Everything the code viewer shows about a file without any interaction. */
@Serializable
data class FileAnnotationsJSON(
    val functions: List<FunctionAnnotationJSON>,
    val calls: List<CallAnnotationJSON>,
    val concepts: List<ConceptAnnotationJSON>,
)

fun TranslationUnit.toAnnotationsJSON(): FileAnnotationsJSON {
    val ownNodes = nodes.filter { it.location != null && it.translationUnit == this }
    val calls = ownNodes.filterIsInstance<Call>().filter { it !is OperatorCall && !it.isImplicit }

    val functions =
        functions
            .filter { !it.isImplicit && it.body != null && it.location != null }
            .map { function ->
                val bodyCalls = function.body.calls.filter { it !is OperatorCall }
                FunctionAnnotationJSON(
                    function = function.toRefJSON(),
                    callers = function.calledBy.size,
                    callees = bodyCalls.size,
                    externalCalls = bodyCalls.count { it.status == CallStatus.EXTERNAL },
                    unresolvedCalls = bodyCalls.count { it.status == CallStatus.UNRESOLVED },
                )
            }

    val callAnnotations =
        calls.mapNotNull { call ->
            val region = (call.callee.takeIf { it.location != null } ?: call).location?.region
            region?.let {
                CallAnnotationJSON(
                    id = call.id.toString(),
                    name = call.name.localName,
                    status = call.status,
                    startLine = it.startLine,
                    startColumn = it.startColumn,
                    endLine = it.endLine,
                    endColumn = it.endColumn,
                )
            }
        }

    val concepts =
        ownNodes.flatMap { node ->
            node.overlays.mapNotNull { overlay ->
                val line = node.location?.region?.startLine ?: return@mapNotNull null
                if (overlay !is Concept && overlay !is Operation) return@mapNotNull null
                ConceptAnnotationJSON(
                    id = overlay.id.toString(),
                    type = overlay.javaClass.simpleName,
                    category =
                        overlay.javaClass.packageName
                            .substringAfter("concepts.", "")
                            .substringBefore('.')
                            .ifEmpty { "other" },
                    isOperation = overlay is Operation,
                    line = line,
                )
            }
        }

    return FileAnnotationsJSON(functions = functions, calls = callAnnotations, concepts = concepts)
}

val Call.status: CallStatus
    get() =
        when {
            invokes.isEmpty() -> CallStatus.UNRESOLVED
            invokes.all { it.isInferred } -> CallStatus.EXTERNAL
            else -> CallStatus.RESOLVED
        }
