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
import de.fraunhofer.aisec.cpg.evaluation.compareTo

sealed class RequirementState

/**
 * Represents a query that has been evaluated and the result is known to be `false` or some
 * assumptions are rejected.
 */
data object Failed : RequirementState()

/**
 * Represents a query that has been evaluated and the result is known to be `true` and all
 * assumptions are accepted.
 */
data object Succeeded : RequirementState()

/**
 * Represents a query that has been evaluated but the result is not yet known because some
 * assumptions have to be accepted (or rejected).
 */
data object IsUndecided : RequirementState()

/**
 * Represents a query that has not yet been evaluated. Will be most likely used in the context of
 * manual assessments which have to be conducted.
 */
data object IsNotYetEvaluated : RequirementState()

fun QueryTree<Boolean>.toQueryState(): QueryTree<RequirementState> {
    val (newValue, stringInfo) =
        when {
            !this.value || this.assumptions.any { it.status == AssumptionStatus.Rejected } ->
                Failed to
                    (if (!this.value) "The query was evaluated to false"
                    else
                        "The assumptions ${this.assumptions.filter { it.status == AssumptionStatus.Rejected }.map { it.id.toHexDashString() } } were rejected")
            this.assumptions.any { it.status == AssumptionStatus.Undecided } ->
                IsUndecided to
                    "The assumptions ${this.assumptions.filter { it.status == AssumptionStatus.Undecided }.map { it.id.toHexDashString() }} are not yet decided"
            this.value ==
                this.assumptions.all {
                    it.status == AssumptionStatus.Ignored || it.status == AssumptionStatus.Accepted
                } ->
                Succeeded to "The query was evaluated to true and all assumptions were accepted."
            else -> IsNotYetEvaluated to "Something went wrong"
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
infix fun RequirementState.and(other: RequirementState): QueryTree<RequirementState> {
    return this.toQueryTree() and other.toQueryTree()
}

/**
 * Performs a logical and (&&) operation between the values and creates and returns [QueryTree]s.
 */
infix fun RequirementState.and(other: QueryTree<RequirementState>): QueryTree<RequirementState> {
    return this.toQueryTree() and other
}

/**
 * Performs a logical and (&&) operation between the values and creates and returns [QueryTree]s.
 */
infix fun QueryTree<RequirementState>.and(other: RequirementState): QueryTree<RequirementState> {
    return this and other.toQueryTree()
}

/** Performs a logical and (&&) operation between the values of two [QueryTree]s. */
infix fun QueryTree<RequirementState>.and(
    other: QueryTree<RequirementState>
): QueryTree<RequirementState> {
    return QueryTree(
        if (this.value == Succeeded && other.value == Succeeded) Succeeded
        else if (this.value == Failed || other.value == Failed) Failed
        else if (this.value == IsNotYetEvaluated && other.value == IsNotYetEvaluated)
            IsNotYetEvaluated
        else IsUndecided,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} && ${other.value}",
    )
}

/** Performs a logical or (||) operation between the values and creates and returns [QueryTree]s. */
infix fun RequirementState.or(other: RequirementState): QueryTree<RequirementState> {
    return this.toQueryTree() or other.toQueryTree()
}

/** Performs a logical or (||) operation between the values and creates and returns [QueryTree]s. */
infix fun RequirementState.or(other: QueryTree<RequirementState>): QueryTree<RequirementState> {
    return this.toQueryTree() or other
}

/** Performs a logical or (||) operation between the values and creates and returns [QueryTree]s. */
infix fun QueryTree<RequirementState>.or(other: RequirementState): QueryTree<RequirementState> {
    return this or other.toQueryTree()
}

/** Performs a logical or (||) operation between the values of two [QueryTree]s. */
infix fun QueryTree<RequirementState>.or(
    other: QueryTree<RequirementState>
): QueryTree<RequirementState> {
    return QueryTree(
        if (this.value == Succeeded || other.value == Succeeded) Succeeded
        else if (this.value == Failed && other.value == Failed) Failed
        else if (this.value == IsNotYetEvaluated && other.value == IsNotYetEvaluated)
            IsNotYetEvaluated
        else IsUndecided,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} || ${other.value}",
    )
}

/** Performs a logical xor operation between the values and creates and returns [QueryTree]s. */
infix fun RequirementState.xor(other: RequirementState): QueryTree<RequirementState> {
    return this.toQueryTree() xor other.toQueryTree()
}

/** Performs a logical xor operation between the values and creates and returns [QueryTree]s. */
infix fun RequirementState.xor(other: QueryTree<RequirementState>): QueryTree<RequirementState> {
    return this.toQueryTree() xor other
}

/** Performs a logical xor operation between the values and creates and returns [QueryTree]s. */
infix fun QueryTree<RequirementState>.xor(other: RequirementState): QueryTree<RequirementState> {
    return this xor other.toQueryTree()
}

/** Performs a logical xor operation between the values of two [QueryTree]s. */
infix fun QueryTree<RequirementState>.xor(
    other: QueryTree<RequirementState>
): QueryTree<RequirementState> {
    return QueryTree(
        if (this.value == Succeeded && other.value == Failed) Succeeded
        else if (this.value == Failed && other.value == Succeeded) Succeeded
        else if (this.value == Failed && other.value == Failed) Failed
        else if (this.value == IsNotYetEvaluated && other.value == IsNotYetEvaluated) Failed
        else if (this.value == Succeeded && other.value == Succeeded) Failed
        else if (this.value == IsUndecided && other.value == IsUndecided) Failed else IsUndecided,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} xor ${other.value}",
    )
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
infix fun RequirementState.implies(other: RequirementState): QueryTree<RequirementState> {
    return this.toQueryTree() implies other.toQueryTree()
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
infix fun RequirementState.implies(
    other: QueryTree<RequirementState>
): QueryTree<RequirementState> {
    return this.toQueryTree() implies other
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
infix fun QueryTree<RequirementState>.implies(
    other: RequirementState
): QueryTree<RequirementState> {
    return this implies other.toQueryTree()
}

/** Evaluates a logical implication (->) operation between the values of two [QueryTree]s. */
infix fun QueryTree<RequirementState>.implies(
    other: QueryTree<RequirementState>
): QueryTree<RequirementState> {
    return QueryTree(
        if (this.value == Succeeded && other.value == Succeeded) Succeeded
        else if (this.value == Failed) Succeeded
        else if (this.value == Succeeded && other.value == Failed) Failed
        else if (this.value == IsNotYetEvaluated && other.value == IsNotYetEvaluated)
            IsNotYetEvaluated
        else IsUndecided,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} => ${other.value}",
    )
}

/** Negates the value of [arg] and returns the resulting [QueryTree]. */
fun not(arg: QueryTree<RequirementState>): QueryTree<RequirementState> {
    val result =
        if (arg.value == Succeeded) Failed
        else if (arg.value == Failed) Succeeded
        else if (arg.value == IsNotYetEvaluated) IsNotYetEvaluated else IsUndecided
    return QueryTree(result, mutableListOf(arg), "! ${arg.value}")
}

/** Negates the value of [arg] and returns the resulting [QueryTree]. */
fun not(arg: RequirementState): QueryTree<RequirementState> {
    return not(arg.toQueryTree())
}
