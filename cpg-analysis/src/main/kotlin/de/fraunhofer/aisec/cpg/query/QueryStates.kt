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

import de.fraunhofer.aisec.cpg.TranslationResult
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

typealias Decision = QueryTree<DecisionState>

sealed class DecisionState

/**
 * Represents a query that has been evaluated and the result is known to be `false` or some
 * [QueryTree.assumptions] have [de.fraunhofer.aisec.cpg.assumptions.Assumption.status]
 * [AssumptionStatus.Rejected].
 */
data object Failed : DecisionState()

/**
 * Represents a query that has been evaluated and the result is known to be `true` and all
 * [QueryTree.assumptions] have [de.fraunhofer.aisec.cpg.assumptions.Assumption.status]
 * [AssumptionStatus.Accepted] or [AssumptionStatus.Ignored].
 */
data object Succeeded : DecisionState()

/**
 * Represents a query that has been evaluated, but the result is not yet known because some
 * [QueryTree.assumptions] have [de.fraunhofer.aisec.cpg.assumptions.Assumption.status]
 * [AssumptionStatus.Undecided].
 */
data object Undecided : DecisionState()

/**
 * Represents a query that has not yet been evaluated. Will be most likely used in the context of
 * manual assessments which have to be conducted.
 */
data object NotYetEvaluated : DecisionState()

/**
 * Wraps the given [QueryTree] in a new [Decision] object by checking if the value is `true` or
 * `false` and based on the [de.fraunhofer.aisec.cpg.assumptions.Assumption.status] of all
 * [QueryTree.collectAssumptions] (i.e., it checks if all are [AssumptionStatus.Accepted] or if some
 * are [AssumptionStatus.Rejected] or [AssumptionStatus.Undecided]).
 */
context(TranslationResult)
fun QueryTree<Boolean>.decide(): Decision {
    val statues = this@TranslationResult.assumptionStates
    // The assumptions need to be collected, as they are located at the respective construct they
    // are placed on and only forwarded on evaluation. Accepting or rejecting an assumption has a
    // different impact on query evaluation depending on the sup-query tree the assumption is placed
    // on. Global assumptions are also included below, component wide assumptions are included with
    // collectAssumptions() in the individual nodes.

    // Adding the global assumptions to the assumptions of this QueryTree before update and deciding
    this.assumptions.addAll(this@TranslationResult.collectAssumptions())

    this.collectAssumptions().forEach { it.status = statues.getOrDefault(it.id, it.status) }

    val confidenceValue =
        AcceptanceStatus.fromAssumptionsAndStatus(this.confidence, this.collectAssumptions())
    val (decidedValue, stringRepresentation) =
        when {
            this.value && confidenceValue is AcceptedResult ->
                Succeeded to
                    "The requirement succeeded because the query was evaluated to true and all assumptions were accepted or deemed not influencing the result."
            !this.value || confidenceValue is RejectedResult ->
                Failed to
                    "The requirement failed because " +
                        if (!this.value) "the query was evaluated to false"
                        else "some assumptions were rejected."
            confidenceValue is UndecidedResult ->
                Undecided to
                    "The requirement is undecided because " +
                        if (this.confidence is UndecidedResult) "the subtrees are undecided."
                        else "some assumptions are undecided."
            else ->
                NotYetEvaluated to
                    "The requirement cannot be evaluated because something went wrong"
        }

    // Carry over result and string representation of decision on nested query trees
    return QueryTree(
        value = decidedValue,
        children = mutableListOf(this),
        stringRepresentation = stringRepresentation,
        node = this.node,
        operator = QueryOperators.EVALUATE,
    )
}

/**
 * Performs a logical and (&&) operation between the values and creates and returns [QueryTree]s.
 */
infix fun DecisionState.and(other: DecisionState): Decision {
    return this.toQueryTree() and other.toQueryTree()
}

/**
 * Performs a logical and (&&) operation between the values and creates and returns [QueryTree]s.
 */
infix fun DecisionState.and(other: Decision): Decision {
    return this.toQueryTree() and other
}

/**
 * Performs a logical and (&&) operation between the values and creates and returns [QueryTree]s.
 */
infix fun Decision.and(other: DecisionState): Decision {
    return this and other.toQueryTree()
}

/** Performs a logical and (&&) operation between the values of two [QueryTree]s. */
infix fun Decision.and(other: Decision): Decision {
    return QueryTree(
        value =
            when {
                this.value == Succeeded && other.value == Succeeded -> Succeeded
                this.value == Failed || other.value == Failed -> Failed
                this.value == NotYetEvaluated && other.value == NotYetEvaluated -> NotYetEvaluated
                else -> Undecided
            },
        children = mutableListOf(this, other),
        stringRepresentation = "${this.value} && ${other.value}",
        node = this.node,
        operator = QueryOperators.AND,
    )
}

/** Performs a logical or (||) operation between the values and creates and returns [QueryTree]s. */
infix fun DecisionState.or(other: DecisionState): Decision {
    return this.toQueryTree() or other.toQueryTree()
}

/** Performs a logical or (||) operation between the values and creates and returns [QueryTree]s. */
infix fun DecisionState.or(other: Decision): Decision {
    return this.toQueryTree() or other
}

/** Performs a logical or (||) operation between the values and creates and returns [QueryTree]s. */
infix fun Decision.or(other: DecisionState): Decision {
    return this or other.toQueryTree()
}

/** Performs a logical or (||) operation between the values of two [QueryTree]s. */
infix fun Decision.or(other: Decision): Decision {
    val newValue =
        when {
            this.value == Succeeded && other.value != Succeeded -> Succeeded
            this.value == Succeeded && other.value == Succeeded -> Succeeded
            this.value != Succeeded && other.value == Succeeded -> Succeeded
            this.value == Failed && other.value == Failed -> Failed
            this.value == NotYetEvaluated && other.value == NotYetEvaluated -> NotYetEvaluated
            else -> Undecided
        }
    return QueryTree(
        value = newValue,
        children = mutableListOf(this, other),
        stringRepresentation = "${this.value} || ${other.value}",
        node = this.node,
        operator = QueryOperators.OR,
    )
}

/** Performs a logical xor operation between the values and creates and returns [QueryTree]s. */
infix fun DecisionState.xor(other: DecisionState): Decision {
    return this.toQueryTree() xor other.toQueryTree()
}

/** Performs a logical xor operation between the values and creates and returns [QueryTree]s. */
infix fun DecisionState.xor(other: Decision): Decision {
    return this.toQueryTree() xor other
}

/** Performs a logical xor operation between the values and creates and returns [QueryTree]s. */
infix fun Decision.xor(other: DecisionState): Decision {
    return this xor other.toQueryTree()
}

/** Performs a logical xor operation between the values of two [QueryTree]s. */
infix fun Decision.xor(other: Decision): Decision {
    val newConfidence = minOf(this.confidence, other.confidence)
    val newValue =
        when {
            this.value == Succeeded && other.value == Failed -> Succeeded
            this.value == Failed && other.value == Succeeded -> Succeeded
            this.value == Failed && other.value == Failed -> Failed
            this.value == NotYetEvaluated && other.value == NotYetEvaluated -> Failed
            this.value == Succeeded && other.value == Succeeded -> Failed
            this.value == Undecided && other.value == Undecided -> Failed
            else -> Undecided
        }
    return QueryTree(
        value = newValue,
        children = mutableListOf(this, other),
        stringRepresentation = "${this.value} xor ${other.value}",
        node = this.node,
        operator = QueryOperators.XOR,
    )
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
infix fun DecisionState.implies(other: DecisionState): Decision {
    return this.toQueryTree() implies other.toQueryTree()
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
infix fun DecisionState.implies(other: Decision): Decision {
    return this.toQueryTree() implies other
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
infix fun Decision.implies(other: DecisionState): Decision {
    return this implies other.toQueryTree()
}

/** Evaluates a logical implication (->) operation between the values of two [QueryTree]s. */
infix fun Decision.implies(other: Decision): Decision {
    val newValue =
        when {
            this.value == Succeeded && other.value == Succeeded -> Succeeded
            this.value == Failed -> Succeeded
            this.value == Succeeded && other.value == Failed -> Failed
            this.value == NotYetEvaluated && other.value == NotYetEvaluated -> NotYetEvaluated
            else -> Undecided
        }
    return QueryTree(
        value = newValue,
        children = mutableListOf(this, other),
        stringRepresentation = "${this.value} => ${other.value}",
        node = this.node,
        operator = QueryOperators.IMPLIES,
    )
}

/** Negates the value of [arg] and returns the resulting [QueryTree]. */
fun not(arg: Decision): Decision {
    val result =
        when (arg.value) {
            Succeeded -> Failed
            Failed -> Succeeded
            NotYetEvaluated -> NotYetEvaluated
            else -> Undecided
        }
    return QueryTree(
        value = result,
        children = mutableListOf(arg),
        stringRepresentation = "! ${arg.value}",
        node = arg.node,
        operator = QueryOperators.NOT,
    )
}

/** Negates the value of [arg] and returns the resulting [QueryTree]. */
fun not(arg: DecisionState): Decision {
    return not(arg.toQueryTree())
}
