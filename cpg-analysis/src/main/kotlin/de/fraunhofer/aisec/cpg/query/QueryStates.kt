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
@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.assumptions.Assumption
import de.fraunhofer.aisec.cpg.assumptions.AssumptionStatus

sealed class AcceptanceStatus : Comparable<AcceptanceStatus> {
    override fun compareTo(other: AcceptanceStatus): Int {
        return when {
            this is AcceptedResult && other is AcceptedResult -> 0
            this is RejectedResult && other is RejectedResult -> 0
            this is UndecidedResult && other is UndecidedResult -> 0
            this is AcceptedResult -> 1 // Accepted is the best status
            this is RejectedResult && other is AcceptedResult ->
                -1 // Rejected is "worse" than Accepted
            this is RejectedResult && other is UndecidedResult ->
                1 // Rejected is "better" than Undecided
            else -> -1 // Undecided is worse than both Accepted and Rejected
        }
    }

    companion object {
        fun fromAssumptionsAndStatus(
            statuses: Collection<AcceptanceStatus>,
            assumptions: Collection<Assumption>,
        ): AcceptanceStatus {
            return when {
                statuses.all { it is AcceptedResult } &&
                    assumptions.all {
                        it.status == AssumptionStatus.Accepted ||
                            it.status == AssumptionStatus.Ignored
                    } -> AcceptedResult
                statuses.any { it is RejectedResult } ||
                    assumptions.any { it.status == AssumptionStatus.Rejected } -> RejectedResult
                statuses.any { it is UndecidedResult } ||
                    assumptions.any { it.status == AssumptionStatus.Undecided } -> UndecidedResult
                else -> UndecidedResult // Default case if no assumptions match
            }
        }

        fun fromAssumptionsAndStatus(
            statuses: Collection<AcceptanceStatus>,
            vararg assumptions: Assumption,
        ) = fromAssumptionsAndStatus(statuses, assumptions.asList())

        fun fromAssumptionsAndStatus(status: AcceptanceStatus, vararg assumptions: Assumption) =
            fromAssumptionsAndStatus(listOf(status), assumptions = assumptions.asList())

        fun fromAssumptionsAndStatus(
            status: AcceptanceStatus,
            assumptions: Collection<Assumption>,
        ) = fromAssumptionsAndStatus(listOf(status), assumptions = assumptions)

        fun fromAssumptionsAndStatus(assumptions: Collection<Assumption>) =
            fromAssumptionsAndStatus(emptySet(), assumptions = assumptions)
    }
}

/**
 * Represents that the value kept in [QueryTree.value] holds because all [QueryTree.assumptions]
 * affecting the result have been accepted or ignored.
 */
data object AcceptedResult : AcceptanceStatus()

/**
 * Represents that the value kept in [QueryTree.value] holds because some [QueryTree.assumptions]
 * affecting the result have been rejected.
 */
data object RejectedResult : AcceptanceStatus()

/**
 * Represents that it is not clear if the value kept in [QueryTree.value] holds because some
 * [QueryTree.assumptions] affecting the results have not been decided upon or because rejecting a
 * subtree prevents us from computing the [QueryTree.value] of this [QueryTree].
 */
data object UndecidedResult : AcceptanceStatus()

/**
 * Represents a [QueryTree] that has not yet been evaluated. This is used to indicate that the query
 * has not been processed yet, and the value is not yet known.
 *
 * We formally assign a value of `false` to this query tree, but its [confidence] is set to
 * [UndecidedResult] to indicate that it has not been evaluated yet.
 */
object NotYetEvaluated :
    QueryTree<Boolean>(
        value = false,
        stringRepresentation = "This has to be evaluated.",
        operator = QueryOperators.EVALUATE,
    ) {
    override val confidence: AcceptanceStatus = UndecidedResult
}
