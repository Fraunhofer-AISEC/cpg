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
import kotlinx.serialization.Serializable

/** Upper bound for the issues returned for one set of evidence, the most severe ones first. */
private const val MAX_TRUST_ISSUES = 20

/** Why evidence of the agent may not be reliable, ordered by severity. */
enum class TrustIssueKind {
    /** A call whose target is unknown, so dataflows through it are unknown as well. */
    UNRESOLVED_CALL,
    /** Code the analysis could not handle, or a node without source code of its own. */
    ANALYSIS_WARNING,
    /** Code that is not part of the analysis (e.g. a library call), whose behaviour is unknown. */
    EXTERNAL_CODE,
}

/**
 * A place where the analysis is uncertain and that some evidence of the agent relies on.
 *
 * @property location The uncertain node, e.g. the unresolved call.
 * @property reason What the evidence relies on, phrased to follow "relies on", e.g. "an unresolved
 *   call to get_key".
 * @property evidence The IDs of the requested nodes that rely on [location].
 */
@Serializable
data class TrustIssueJSON(
    val kind: TrustIssueKind,
    val location: NodeRefJSON,
    val reason: String,
    val evidence: List<String>,
)

/**
 * Finds the places where the analysis is uncertain that the given evidence relies on:
 * - unresolved calls in the function that contains an evidence node (its dataflows may be
 *   incomplete),
 * - external calls the evidence passes through (the node is such a call, one of its arguments or a
 *   direct dataflow neighbour of one),
 * - nodes the analysis could not handle or only inferred.
 *
 * Issues at the same place are merged, so that each place is listed once with all the evidence that
 * relies on it.
 */
fun trustIssues(evidence: Map<String, Node>): List<TrustIssueJSON> {
    val issues = mutableMapOf<Pair<TrustIssueKind, Node>, Pair<String, MutableSet<String>>>()
    fun add(kind: TrustIssueKind, location: Node, reason: String, id: String) {
        issues.getOrPut(kind to location) { reason to linkedSetOf() }.second += id
    }

    // The unresolved calls of each function, computed once for all the evidence in it
    val unresolvedCalls = mutableMapOf<Function, List<Call>>()

    for ((id, node) in evidence) {
        if (node is ProblemNode) {
            add(
                TrustIssueKind.ANALYSIS_WARNING,
                node,
                "code the analysis could not handle (${node.problemType})",
                id,
            )
        } else if (node.isInferred && node !is Call) {
            add(
                TrustIssueKind.EXTERNAL_CODE,
                node,
                "'${node.name.localName}', which is not part of the analysed code",
                id,
            )
        }

        // The calls the node passes through: the node itself, the call it is an argument of, and
        // the calls it directly flows from or into
        val passedCalls =
            listOfNotNull(
                node as? Call,
                (node.astParent as? Call)?.takeIf { node in it.arguments },
            ) +
                node.prevDFGEdges.mapNotNull { it.start as? Call } +
                node.nextDFGEdges.mapNotNull { it.end as? Call }
        passedCalls
            .filter { it !is OperatorCall && it.location != null }
            .distinct()
            .forEach { call ->
                when (call.status) {
                    CallStatus.EXTERNAL ->
                        add(
                            TrustIssueKind.EXTERNAL_CODE,
                            call,
                            "an external call to ${call.name.localName}",
                            id,
                        )
                    CallStatus.UNRESOLVED ->
                        add(
                            TrustIssueKind.UNRESOLVED_CALL,
                            call,
                            "an unresolved call to ${call.name.localName}",
                            id,
                        )
                    CallStatus.RESOLVED -> {}
                }
            }

        // The unresolved calls in the enclosing function, which make its dataflows incomplete
        val function = node as? Function ?: node.firstParentOrNull<Function>() ?: continue
        unresolvedCalls
            .getOrPut(function) {
                function.body.calls.filter {
                    it !is OperatorCall && it.location != null && it.status == CallStatus.UNRESOLVED
                }
            }
            .forEach { call ->
                add(
                    TrustIssueKind.UNRESOLVED_CALL,
                    call,
                    "an unresolved call to ${call.name.localName} in ${function.name.localName}",
                    id,
                )
            }
    }

    return issues.entries
        .sortedWith(
            compareBy<Map.Entry<Pair<TrustIssueKind, Node>, Pair<String, MutableSet<String>>>> {
                    it.key.first
                }
                .thenByDescending { it.value.second.size }
        )
        .take(MAX_TRUST_ISSUES)
        .map { (key, value) ->
            TrustIssueJSON(
                kind = key.first,
                location = key.second.toRefJSON(),
                reason = value.first,
                evidence = value.second.toList(),
            )
        }
}
