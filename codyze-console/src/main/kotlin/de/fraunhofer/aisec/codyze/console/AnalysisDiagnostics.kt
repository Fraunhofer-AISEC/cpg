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
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.OperatorCall
import de.fraunhofer.aisec.cpg.serialization.CallStatus
import de.fraunhofer.aisec.cpg.serialization.NodeJSON
import de.fraunhofer.aisec.cpg.serialization.status
import de.fraunhofer.aisec.cpg.serialization.toRefJSON
import kotlinx.serialization.Serializable

/** Upper bound for the diagnostics returned for one set of evidence, the most severe ones first. */
private const val MAX_EVIDENCE_DIAGNOSTICS = 20

/** How much a [Diagnostic] matters. */
enum class DiagnosticSeverity {
    /** The analysis is incomplete or failed here, so results may be wrong or missing. */
    WARNING,
    /** Something to know about the analysis, which is not a defect. */
    INFO,
}

/** What kind of [Diagnostic] the analysis has at a node, ordered by severity. */
enum class DiagnosticKind(val severity: DiagnosticSeverity) {
    /** A call whose target is unknown, so dataflows through it are unknown as well. */
    UNRESOLVED_CALL(DiagnosticSeverity.WARNING),
    /** Code the analysis could not handle. */
    ANALYSIS_PROBLEM(DiagnosticSeverity.WARNING),
    /**
     * Code that is not part of the analysis (e.g. a library function), whose behaviour is unknown.
     */
    EXTERNAL_CODE(DiagnosticSeverity.INFO),
}

/**
 * A note about a place where the analysis is incomplete or uncertain, or where it ends.
 *
 * @property kind What kind of diagnostic this is.
 * @property message A sentence that describes the diagnostic for the user.
 * @property reliance A short phrase for what something relies on if it depends on this diagnostic,
 *   to follow "relies on", e.g. "an unresolved call to get_key".
 */
data class Diagnostic(val kind: DiagnosticKind, val message: String, val reliance: String)

/**
 * Returns the diagnostics of the analysis at this node itself: that it was inferred, that it could
 * not be handled, that it is a call without a known target or with targets that are not part of the
 * analysed code, or a function without a body. It does not look at other nodes, e.g. at the calls
 * in the function of this node, see [reliesOn] for that.
 */
fun Node.diagnostics(): List<Diagnostic> {
    val diagnostics = mutableListOf<Diagnostic>()
    if (isInferred) {
        diagnostics +=
            Diagnostic(
                DiagnosticKind.EXTERNAL_CODE,
                "This node was inferred by the analysis; it has no source code of its own.",
                "'${name.localName}', which is not part of the analysed code",
            )
    }
    if (this is ProblemNode) {
        diagnostics +=
            Diagnostic(
                DiagnosticKind.ANALYSIS_PROBLEM,
                "The analysis could not handle this code ($problemType): $problem",
                "code the analysis could not handle ($problemType)",
            )
    }
    if (this is Call) {
        when (status) {
            CallStatus.UNRESOLVED ->
                diagnostics +=
                    Diagnostic(
                        DiagnosticKind.UNRESOLVED_CALL,
                        "The call target could not be resolved; dataflows through it are unknown.",
                        "an unresolved call to ${name.localName}",
                    )
            // The targets are known, but none of them is part of the analysed code
            CallStatus.EXTERNAL ->
                invokes.forEach {
                    diagnostics +=
                        Diagnostic(
                            DiagnosticKind.EXTERNAL_CODE,
                            "The target '${it.name}' is not part of the analysed code; its behaviour is unknown.",
                            "an external call to ${name.localName}",
                        )
                }
            CallStatus.RESOLVED -> {}
        }
    }
    // A declaration whose definition (e.g. in another file) is analysed is not only declared
    if (this is Function && !(definition ?: this).hasBody()) {
        diagnostics +=
            Diagnostic(
                DiagnosticKind.EXTERNAL_CODE,
                "This function is only declared, its implementation is not part of the analysis.",
                "'${name.localName}', whose implementation is not part of the analysis",
            )
    }
    return diagnostics
}

/**
 * Returns the nodes whose [diagnostics] affect this node: the node itself, the call it is an
 * argument of, the calls it directly flows from or into, and the unresolved calls in its function,
 * which make the dataflows in the function incomplete.
 *
 * @param unresolvedCalls The unresolved calls of the functions seen so far, to compute them only
 *   once for all the nodes in a function.
 */
internal fun Node.reliesOn(
    unresolvedCalls: MutableMap<Function, List<Call>> = mutableMapOf()
): Set<Node> {
    val calls =
        listOfNotNull(this as? Call, (astParent as? Call)?.takeIf { this in it.arguments }) +
            prevDFGEdges.mapNotNull { it.start as? Call } +
            nextDFGEdges.mapNotNull { it.end as? Call }
    val result = linkedSetOf(this)
    // Operators and calls without source code are not what the user would call a call
    calls.filterTo(result) { it !is OperatorCall && it.location != null }

    val function = this as? Function ?: firstParentOrNull<Function>()
    if (function != null) {
        result +=
            unresolvedCalls.getOrPut(function) {
                function.body.calls.filter {
                    it !is OperatorCall && it.location != null && it.status == CallStatus.UNRESOLVED
                }
            }
    }
    return result
}

/**
 * A [Diagnostic] that some evidence of the agent relies on.
 *
 * @property kind What kind of diagnostic this is.
 * @property location The node with the diagnostic, e.g. the unresolved call.
 * @property reason What the evidence relies on, phrased to follow "relies on", e.g. "an unresolved
 *   call to get_key".
 * @property evidence The IDs of the requested nodes that rely on [location].
 */
@Serializable
data class EvidenceDiagnosticJSON(
    val kind: DiagnosticKind,
    val severity: DiagnosticSeverity,
    val location: NodeJSON,
    val reason: String,
    val evidence: List<String>,
)

/**
 * Finds the diagnostics of the analysis that the given evidence relies on, see [reliesOn] and
 * [diagnostics].
 *
 * Gaps at the same place are merged, so that each place is listed once with all the evidence that
 * relies on it. The most severe diagnostics come first, and among those of the same kind the ones
 * that most evidence relies on.
 */
fun evidenceDiagnostics(evidence: Map<String, Node>): List<EvidenceDiagnosticJSON> {
    class Found(val reason: String, val evidence: MutableSet<String> = linkedSetOf())

    val found = mutableMapOf<Pair<DiagnosticKind, Node>, Found>()
    val unresolvedCalls = mutableMapOf<Function, List<Call>>()
    for ((id, node) in evidence) {
        for (place in node.reliesOn(unresolvedCalls)) {
            for (diagnostic in place.diagnostics()) {
                found.getOrPut(diagnostic.kind to place) { Found(diagnostic.reliance) }.evidence +=
                    id
            }
        }
    }

    return found.entries
        .sortedWith(compareBy({ it.key.first }, { -it.value.evidence.size }))
        .take(MAX_EVIDENCE_DIAGNOSTICS)
        .map { (key, value) ->
            EvidenceDiagnosticJSON(
                kind = key.first,
                severity = key.first.severity,
                location = key.second.toRefJSON(),
                reason = value.reason,
                evidence = value.evidence.toList(),
            )
        }
}
