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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.assumptions.AssumptionStatus

typealias RequirementEvaluation = QueryTree<RequirementState>

sealed class RequirementState

/**
 * Represents a query that has been evaluated and the result is known to be `false` or some
 * [QueryTree.assumptions] have [de.fraunhofer.aisec.cpg.assumptions.Assumption.status]
 * [AssumptionStatus.Rejected].
 */
data object Failed : RequirementState()

/**
 * Represents a query that has been evaluated and the result is known to be `true` and all
 * [QueryTree.assumptions] have [de.fraunhofer.aisec.cpg.assumptions.Assumption.status]
 * [AssumptionStatus.Accepted] or [AssumptionStatus.Ignored].
 */
data object Succeeded : RequirementState()

/**
 * Represents a query that has been evaluated but the result is not yet known because some
 * [QueryTree.assumptions] have [de.fraunhofer.aisec.cpg.assumptions.Assumption.status]
 * [AssumptionStatus.Undecided].
 */
data object Undecided : RequirementState()

/**
 * Represents a query that has not yet been evaluated. Will be most likely used in the context of
 * manual assessments which have to be conducted.
 */
data object NotYetEvaluated : RequirementState()

fun QueryTree<Boolean>.toQueryState(): RequirementEvaluation {
    val (newValue, stringInfo) =
        when {
            !this.value || this.assumptions.any { it.status == AssumptionStatus.Rejected } ->
                Failed to
                    (if (!this.value) "The query was evaluated to false"
                    else
                        "The assumptions ${this.assumptions.filter { it.status == AssumptionStatus.Rejected }.map { it.id.toHexDashString() } } were rejected")
            this.assumptions.any { it.status == AssumptionStatus.Undecided } ->
                Undecided to
                    "The assumptions ${this.assumptions.filter { it.status == AssumptionStatus.Undecided }.map { it.id.toHexDashString() }} are not yet decided"
            this.value ==
                this.assumptions.all {
                    it.status == AssumptionStatus.Ignored || it.status == AssumptionStatus.Accepted
                } ->
                Succeeded to "The query was evaluated to true and all assumptions were accepted."
            else -> NotYetEvaluated to "Something went wrong"
        }

    return QueryTree(
        value = newValue,
        children = mutableListOf(this),
        stringRepresentation = "The requirement ${ newValue::class.simpleName } because $stringInfo",
    )
}
