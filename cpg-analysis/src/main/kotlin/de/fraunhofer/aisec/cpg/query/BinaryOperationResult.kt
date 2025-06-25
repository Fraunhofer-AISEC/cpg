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

import de.fraunhofer.aisec.cpg.graph.Node

/**
 * Represents the result of a binary operation between two [QueryTree]s. It contains the [lhs]
 * (left-hand side) and [rhs] (right-hand side) [QueryTree]s, the [value] of the operation,
 * [stringRepresentation] of the operation, the [node] that was evaluated.
 */
class BinaryOperationResult<LhsValueType : Any?, RhsValueType : Any?>(
    value: Boolean,
    val lhs: QueryTree<LhsValueType>?,
    val rhs: QueryTree<RhsValueType>?,
    stringRepresentation: String = "",
    node: Node? = null,
    operator: BinaryOperators,
) :
    QueryTree<Boolean>(
        value = value,
        children = listOfNotNull(lhs, rhs),
        stringRepresentation = stringRepresentation,
        node = node,
        assumptions = mutableSetOf(),
        operator = operator,
        collectCallerInfo = true,
    ) {

    override fun calculateConfidence(): AcceptanceStatus {
        val assumptionsToUse = this.assumptions
        val operator = this.operator
        if (operator !is BinaryOperators) {
            throw QueryException("The operator must be a BinaryOperator, but was $operator")
        }

        return when (operator) {
            BinaryOperators.AND,
            BinaryOperators.EQ,
            BinaryOperators.NE,
            BinaryOperators.GT,
            BinaryOperators.GE,
            BinaryOperators.LT,
            BinaryOperators.LE,
            BinaryOperators.IS,
            BinaryOperators.XOR -> {
                AcceptanceStatus.fromAssumptionsAndStatus(
                    children.minOf { it.confidence },
                    assumptionsToUse,
                )
            }

            // These operators require only one "true" result to be Accepted. We also want all
            // assumptions related to this requirement to be accepted/ignored.
            BinaryOperators.OR -> {
                val trueChildren = children.filter { it.value == true }
                val trueConfidence = trueChildren.map { it.confidence }
                val falseChildren = children.filter { it.value == false }
                val falseConfidence = falseChildren.map { it.confidence }
                val resultingConfidence =
                    if (trueConfidence.isNotEmpty() && trueConfidence.max() is AcceptedResult) {
                        AcceptedResult
                    } else if (
                        trueChildren.isEmpty() && falseConfidence.all { it is AcceptedResult }
                    ) {
                        AcceptedResult
                    } else if (
                        trueConfidence.isEmpty() &&
                            falseChildren.isNotEmpty() &&
                            falseConfidence.max() is RejectedResult
                    ) {
                        RejectedResult
                    } else {
                        UndecidedResult
                    }

                AcceptanceStatus.fromAssumptionsAndStatus(resultingConfidence, assumptionsToUse)
            }
            BinaryOperators.IMPLIES -> {
                return when {
                    lhs?.value == false && lhs.confidence is AcceptedResult -> {
                        // If the lhs is false and accepted, we can say that the implication is
                        // accepted.
                        AcceptedResult
                    }
                    rhs?.value == true && rhs.confidence is AcceptedResult -> {
                        // If the rhs is true and accepted, we can say that the implication is
                        // accepted.
                        AcceptedResult
                    }
                    else -> {
                        // We do not know if the implication is accepted or not
                        UndecidedResult
                    }
                }
            }
            BinaryOperators.IN -> {
                if (value) {
                    // TODO: The lhs must be accepted and the element matching lhs in the rhs
                    //  must be accepted too. We do not care about the rest.
                    //  Qick-fix until we have this: Everything must be accepted.
                    AcceptanceStatus.fromAssumptionsAndStatus(
                        setOfNotNull(lhs?.confidence, rhs?.confidence),
                        assumptionsToUse,
                    )
                } else {
                    if (lhs == null || rhs == null) {
                        // If either lhs or rhs is null, we cannot determine the confidence.
                        UndecidedResult
                    } else {
                        // If the value is false and not everything is of confidence accepted, we
                        // say it's undecided.
                        if (minOf(lhs.confidence, rhs.confidence) != AcceptedResult) UndecidedResult
                        else
                            AcceptanceStatus.fromAssumptionsAndStatus(
                                minOf(lhs.confidence, rhs.confidence),
                                assumptionsToUse,
                            )
                    }
                }
            }
        }
    }
}
