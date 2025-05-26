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

/**
 * Performs a logical and (&&) operation between the values and creates and returns [QueryTree]s.
 */
infix fun RequirementState.and(other: RequirementState): RequirementEvaluation {
    return this.toQueryTree() and other.toQueryTree()
}

/**
 * Performs a logical and (&&) operation between the values and creates and returns [QueryTree]s.
 */
infix fun RequirementState.and(other: RequirementEvaluation): RequirementEvaluation {
    return this.toQueryTree() and other
}

/**
 * Performs a logical and (&&) operation between the values and creates and returns [QueryTree]s.
 */
infix fun RequirementEvaluation.and(other: RequirementState): RequirementEvaluation {
    return this and other.toQueryTree()
}

/** Performs a logical and (&&) operation between the values of two [QueryTree]s. */
infix fun RequirementEvaluation.and(other: RequirementEvaluation): RequirementEvaluation {
    return QueryTree(
        if (this.value == Succeeded && other.value == Succeeded) Succeeded
        else if (this.value == Failed || other.value == Failed) Failed
        else if (this.value == NotYetEvaluated && other.value == NotYetEvaluated) NotYetEvaluated
        else Undecided,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} && ${other.value}",
    )
}

/** Performs a logical or (||) operation between the values and creates and returns [QueryTree]s. */
infix fun RequirementState.or(other: RequirementState): RequirementEvaluation {
    return this.toQueryTree() or other.toQueryTree()
}

/** Performs a logical or (||) operation between the values and creates and returns [QueryTree]s. */
infix fun RequirementState.or(other: RequirementEvaluation): RequirementEvaluation {
    return this.toQueryTree() or other
}

/** Performs a logical or (||) operation between the values and creates and returns [QueryTree]s. */
infix fun RequirementEvaluation.or(other: RequirementState): RequirementEvaluation {
    return this or other.toQueryTree()
}

/** Performs a logical or (||) operation between the values of two [QueryTree]s. */
infix fun RequirementEvaluation.or(other: RequirementEvaluation): RequirementEvaluation {
    return QueryTree(
        if (this.value == Succeeded || other.value == Succeeded) Succeeded
        else if (this.value == Failed && other.value == Failed) Failed
        else if (this.value == NotYetEvaluated && other.value == NotYetEvaluated) NotYetEvaluated
        else Undecided,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} || ${other.value}",
    )
}

/** Performs a logical xor operation between the values and creates and returns [QueryTree]s. */
infix fun RequirementState.xor(other: RequirementState): RequirementEvaluation {
    return this.toQueryTree() xor other.toQueryTree()
}

/** Performs a logical xor operation between the values and creates and returns [QueryTree]s. */
infix fun RequirementState.xor(other: RequirementEvaluation): RequirementEvaluation {
    return this.toQueryTree() xor other
}

/** Performs a logical xor operation between the values and creates and returns [QueryTree]s. */
infix fun RequirementEvaluation.xor(other: RequirementState): RequirementEvaluation {
    return this xor other.toQueryTree()
}

/** Performs a logical xor operation between the values of two [QueryTree]s. */
infix fun RequirementEvaluation.xor(other: RequirementEvaluation): RequirementEvaluation {
    return QueryTree(
        if (this.value == Succeeded && other.value == Failed) Succeeded
        else if (this.value == Failed && other.value == Succeeded) Succeeded
        else if (this.value == Failed && other.value == Failed) Failed
        else if (this.value == NotYetEvaluated && other.value == NotYetEvaluated) Failed
        else if (this.value == Succeeded && other.value == Succeeded) Failed
        else if (this.value == Undecided && other.value == Undecided) Failed else Undecided,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} xor ${other.value}",
    )
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
infix fun RequirementState.implies(other: RequirementState): RequirementEvaluation {
    return this.toQueryTree() implies other.toQueryTree()
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
infix fun RequirementState.implies(other: RequirementEvaluation): RequirementEvaluation {
    return this.toQueryTree() implies other
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
infix fun RequirementEvaluation.implies(other: RequirementState): RequirementEvaluation {
    return this implies other.toQueryTree()
}

/** Evaluates a logical implication (->) operation between the values of two [QueryTree]s. */
infix fun RequirementEvaluation.implies(other: RequirementEvaluation): RequirementEvaluation {
    return QueryTree(
        if (this.value == Succeeded && other.value == Succeeded) Succeeded
        else if (this.value == Failed) Succeeded
        else if (this.value == Succeeded && other.value == Failed) Failed
        else if (this.value == NotYetEvaluated && other.value == NotYetEvaluated) NotYetEvaluated
        else Undecided,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} => ${other.value}",
    )
}

/** Negates the value of [arg] and returns the resulting [QueryTree]. */
fun not(arg: RequirementEvaluation): RequirementEvaluation {
    val result =
        if (arg.value == Succeeded) Failed
        else if (arg.value == Failed) Succeeded
        else if (arg.value == NotYetEvaluated) NotYetEvaluated else Undecided
    return QueryTree(result, mutableListOf(arg), "! ${arg.value}")
}

/** Negates the value of [arg] and returns the resulting [QueryTree]. */
fun not(arg: RequirementState): RequirementEvaluation {
    return not(arg.toQueryTree())
}
