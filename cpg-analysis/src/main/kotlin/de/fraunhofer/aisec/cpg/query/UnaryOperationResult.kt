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
 * Represents the result of a unary operation on a [QueryTree]. It contains the [input] [QueryTree],
 * the [value] of the operation, the [stringRepresentation] of the operation, the [node] that was
 * evaluated, and the [operator] that was used for the operation.
 */
class UnaryOperationResult<T>(
    value: Boolean,
    val input: QueryTree<T>,
    stringRepresentation: String = "",
    node: Node? = null,
    operator: UnaryOperators,
) :
    QueryTree<Boolean>(
        value = value,
        children = listOf(input),
        stringRepresentation = stringRepresentation,
        node = node,
        assumptions = mutableSetOf(),
        operator = operator,
    ) {

    override fun calculateConfidence(): AcceptanceStatus {
        val assumptionsToUse = this.assumptions
        val operator = this.operator
        if (operator !is UnaryOperators) {
            throw QueryException("The operator must be a UnaryOperator, but was $operator")
        }

        return when (operator) {
            UnaryOperators.NOT -> {
                AcceptanceStatus.fromAssumptionsAndStatus(input.confidence, assumptionsToUse)
            }
        }
    }
}
