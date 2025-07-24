/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis.abstracteval.value

import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleStateElement
import de.fraunhofer.aisec.cpg.analysis.abstracteval.changeDeclarationState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.intervalOf
import de.fraunhofer.aisec.cpg.analysis.abstracteval.pushToDeclarationState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.pushToGeneralState
import de.fraunhofer.aisec.cpg.graph.BranchingNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator

/** This class implements the [Value] interface for Integer values. */
class IntegerValue : Value<LatticeInterval> {
    private fun simpleComparison(
        lhs: Node,
        rhs: Node,
        operator: String?,
        lattice: TupleState<Any>,
        state: TupleStateElement<Any>,
    ): TupleStateElement<Any> {
        if (operator == "||" && lhs is BinaryOperator && rhs is BinaryOperator) {
            val lhsState = state.duplicate()
            simpleComparison(lhs.lhs, rhs.lhs, lhs.operatorCode, lattice, lhsState)
            val rhsState = state.duplicate()
            simpleComparison(rhs.lhs, rhs.lhs, rhs.operatorCode, lattice, rhsState)
            val newState = lattice.lub(lhsState, rhsState, false)
            // TODO: Handle the new state
            return newState
        } else if (operator == "&&" && lhs is BinaryOperator && rhs is BinaryOperator) {
            val lhsState = state.duplicate()
            simpleComparison(lhs.lhs, rhs.lhs, lhs.operatorCode, lattice, lhsState)
            val rhsState = state.duplicate()
            simpleComparison(rhs.lhs, rhs.lhs, rhs.operatorCode, lattice, rhsState)
            val newState = lattice.glb(lhsState, rhsState)
            // TODO: Handle the new state
            return newState
        }

        val lhsValue = state.intervalOf(lhs)
        val rhsValue = state.intervalOf(rhs)

        if (
            lhs is Reference &&
                lhsValue is LatticeInterval.Bounded &&
                rhsValue is LatticeInterval.Bounded
        ) {
            val newLhsInterval =
                when (operator) {
                    "<" -> {
                        val newUpper =
                            minOf(lhsValue.upper, rhsValue.upper - LatticeInterval.Bound.Value(1))
                        if (newUpper < lhsValue.lower) {
                            LatticeInterval.BOTTOM
                        } else {
                            LatticeInterval.Bounded(lhsValue.lower, newUpper)
                        }
                    }
                    "<=" -> {
                        val newUpper = minOf(lhsValue.upper, rhsValue.upper)
                        if (newUpper < lhsValue.lower) {
                            LatticeInterval.BOTTOM
                        } else {
                            LatticeInterval.Bounded(lhsValue.lower, newUpper)
                        }
                    }
                    ">" -> {
                        val newLower =
                            maxOf(lhsValue.lower, rhsValue.lower + LatticeInterval.Bound.Value(1))
                        if (newLower > lhsValue.upper) {
                            LatticeInterval.BOTTOM
                        } else {
                            LatticeInterval.Bounded(newLower, lhsValue.upper)
                        }
                    }
                    ">=" -> {
                        val newLower = maxOf(lhsValue.lower, rhsValue.lower)
                        if (newLower > lhsValue.upper) {
                            LatticeInterval.BOTTOM
                        } else {
                            LatticeInterval.Bounded(newLower, lhsValue.upper)
                        }
                    }
                    "==" -> {
                        val newLower = maxOf(lhsValue.lower, rhsValue.lower)
                        val newUpper = minOf(lhsValue.upper, rhsValue.upper)
                        if (newLower > newUpper) {
                            LatticeInterval.BOTTOM
                        } else {
                            LatticeInterval.Bounded(newLower, newUpper)
                        }
                    }
                    "!=" -> {
                        if (rhsValue.lower == rhsValue.upper) {
                            if (lhsValue.lower == rhsValue.upper) {
                                // Move lower bound up by one
                                LatticeInterval.Bounded(
                                    lhsValue.lower + LatticeInterval.Bound.Value(1),
                                    lhsValue.upper,
                                )
                            } else if (lhsValue.upper == rhsValue.lower) {
                                // Move upper bound up by one
                                LatticeInterval.Bounded(
                                    lhsValue.lower,
                                    lhsValue.upper - LatticeInterval.Bound.Value(1),
                                )
                            } else {
                                lhsValue
                            }
                        } else {
                            lhsValue
                        }
                    }
                    else -> {
                        lhsValue
                    }
                }
            lattice.changeDeclarationState(state, lhs, newLhsInterval)
            lattice.pushToGeneralState(state, lhs, newLhsInterval)
        }
        return state
    }

    override fun applyEffect(
        current: LatticeInterval?,
        lattice: TupleState<Any>,
        state: TupleStateElement<Any>,
        node: Node,
        edge: EvaluationOrder?,
        name: String?,
        computeWithoutPush: Boolean,
    ): LatticeInterval {
        val prevNode = edge?.start
        // For a node after a branching node: Calculate how variables are affected by the condition
        // in the different branches.
        val state =
            if (prevNode is BranchingNode) {
                val condition = prevNode.branchedBy
                if (edge.branch == true && condition is BinaryOperator) {
                    // The "then" branch is taken, so the condition is true, and we can use the
                    // condition as is.
                    simpleComparison(
                        condition.lhs,
                        condition.rhs,
                        condition.operatorCode,
                        lattice,
                        state,
                    )
                } else if (edge.branch == false && condition is BinaryOperator) {
                    // The "else" branch is taken, so the condition is false, and we have to
                    // calculate the negation of the condition.
                    val invertedOperator =
                        when (condition.operatorCode) {
                            "==" -> "!="
                            "!=" -> "=="
                            "<" -> ">="
                            "<=" -> ">"
                            ">" -> "<="
                            ">=" -> "<"
                            else -> condition.operatorCode
                        }
                    simpleComparison(condition.lhs, condition.rhs, invertedOperator, lattice, state)
                } else {
                    // Unknown branch. Continue with the current state.
                    state
                }
            } else {
                state
            }

        if (node is Literal<*>) {
            val value = node.value as? Number ?: return current ?: state.intervalOf(node)

            val interval = LatticeInterval.Bounded(value.toLong(), value.toLong())
            if (!computeWithoutPush) {
                lattice.pushToDeclarationState(state, node, interval)
                lattice.pushToGeneralState(state, node, interval)
            }
            return interval
        } // (Re-)Declarations of the Variable
        else if (node is VariableDeclaration) {
            val initializerValue =
                node.initializer?.let {
                    this.applyEffect(
                        current,
                        lattice,
                        state,
                        it,
                        null,
                        name,
                        computeWithoutPush = true,
                    )
                } ?: LatticeInterval.TOP
            lattice.pushToDeclarationState(state, node, initializerValue)
            lattice.pushToGeneralState(state, node, initializerValue)
            return initializerValue
        }
        // Unary Operators
        else if (node is UnaryOperator) {
            val current = state.intervalOf(node.input)
            val newValue =
                when (node.operatorCode) {
                    "++" -> {
                        val oneInterval = LatticeInterval.Bounded(1, 1)
                        current + oneInterval
                    }
                    "--" -> {
                        val oneInterval = LatticeInterval.Bounded(1, 1)
                        current - oneInterval
                    }
                    else -> current
                }
            lattice.changeDeclarationState(state, node.input, newValue)
            lattice.pushToGeneralState(state, node, newValue)
            return newValue
        } // Binary Operators
        else if (node is BinaryOperator) {
            val lhsValue = state.intervalOf(node.lhs)
            val rhsValue = state.intervalOf(node.rhs)
            val newValue =
                when (node.operatorCode) {
                    "+" -> {
                        lhsValue + rhsValue
                    }
                    "-" -> {
                        lhsValue - rhsValue
                    }
                    "*" -> {
                        lhsValue * rhsValue
                    }
                    "/" -> {
                        lhsValue / rhsValue
                    }
                    "%" -> {
                        lhsValue % rhsValue
                    }

                    "<" -> {
                        if (
                            lhsValue is LatticeInterval.Bounded &&
                                rhsValue is LatticeInterval.Bounded
                        ) {
                            if (lhsValue.lower > rhsValue.upper) {
                                LatticeInterval.Bounded(0, 0)
                            } else if (lhsValue.upper < rhsValue.lower) {
                                LatticeInterval.Bounded(1, 1)
                            } else {
                                LatticeInterval.Bounded(0, 1)
                            }
                        } else {
                            LatticeInterval.TOP // Cannot determine bounds
                        }
                    }
                    "<=" -> {
                        if (
                            lhsValue is LatticeInterval.Bounded &&
                                rhsValue is LatticeInterval.Bounded
                        ) {
                            if (lhsValue.lower >= rhsValue.upper) {
                                LatticeInterval.Bounded(0, 0)
                            } else if (lhsValue.upper <= rhsValue.lower) {
                                LatticeInterval.Bounded(1, 1)
                            } else {
                                LatticeInterval.Bounded(0, 1)
                            }
                        } else {
                            LatticeInterval.TOP // Cannot determine bounds
                        }
                    }
                    ">" -> {
                        if (
                            lhsValue is LatticeInterval.Bounded &&
                                rhsValue is LatticeInterval.Bounded
                        ) {
                            if (lhsValue.lower < rhsValue.upper) {
                                LatticeInterval.Bounded(0, 0)
                            } else if (lhsValue.upper > rhsValue.lower) {
                                LatticeInterval.Bounded(1, 1)
                            } else {
                                LatticeInterval.Bounded(0, 1)
                            }
                        } else {
                            LatticeInterval.TOP // Cannot determine bounds
                        }
                    }
                    ">=" -> {
                        if (
                            lhsValue is LatticeInterval.Bounded &&
                                rhsValue is LatticeInterval.Bounded
                        ) {
                            if (lhsValue.lower <= rhsValue.upper) {
                                LatticeInterval.Bounded(0, 0)
                            } else if (lhsValue.upper >= rhsValue.lower) {
                                LatticeInterval.Bounded(1, 1)
                            } else {
                                LatticeInterval.Bounded(0, 1)
                            }
                        } else {
                            LatticeInterval.TOP // Cannot determine bounds
                        }
                    }
                    "==" -> {
                        if (
                            lhsValue is LatticeInterval.Bounded &&
                                rhsValue is LatticeInterval.Bounded
                        ) {
                            if (
                                lhsValue.upper == lhsValue.lower &&
                                    rhsValue.lower == rhsValue.upper &&
                                    lhsValue.lower == rhsValue.lower
                            ) {
                                LatticeInterval.Bounded(1, 1)
                            } else if (
                                lhsValue.upper < rhsValue.lower || rhsValue.upper < lhsValue.lower
                            ) {
                                LatticeInterval.Bounded(0, 0)
                            } else {
                                LatticeInterval.Bounded(0, 1)
                            }
                        } else {
                            LatticeInterval.TOP // Cannot determine bounds
                        }
                    }
                    "!=" -> {
                        if (
                            lhsValue is LatticeInterval.Bounded &&
                                rhsValue is LatticeInterval.Bounded
                        ) {
                            if (
                                lhsValue.upper == lhsValue.lower &&
                                    rhsValue.lower == rhsValue.upper &&
                                    lhsValue.lower == rhsValue.lower
                            ) {
                                LatticeInterval.Bounded(0, 0)
                            } else if (
                                lhsValue.upper < rhsValue.lower || rhsValue.upper < lhsValue.lower
                            ) {
                                LatticeInterval.Bounded(1, 1)
                            } else {
                                LatticeInterval.Bounded(0, 1)
                            }
                        } else {
                            LatticeInterval.TOP // Cannot determine bounds
                        }
                    }
                    else -> TODO("Unsupported operator: ${node.operatorCode}")
                }
            lattice.pushToGeneralState(state, node, newValue)
            lattice.pushToDeclarationState(state, node, newValue)
            return newValue
        }
        // Assignments and combined assign expressions
        else if (node is AssignExpression) {
            if (node.lhs.size == 1 && node.rhs.size == 1) {
                // The lhs and rhs must already have been evaluated before reaching the operator.
                // This should be guaranteed by the evaluation order graph.
                val rhsValue = state.intervalOf(node.rhs[0])
                val lhsValue = state.intervalOf(node.lhs[0])
                val newValue =
                    when (node.operatorCode) {
                        "=" -> rhsValue
                        "+=" -> lhsValue + rhsValue
                        "-=" -> lhsValue - rhsValue
                        "*=" -> lhsValue * rhsValue
                        "/=" -> lhsValue / rhsValue
                        "%=" -> lhsValue % rhsValue
                        else -> TODO("Unsupported operator: ${node.operatorCode}")
                    }
                // Push the new value to the declaration state of the variable
                lattice.changeDeclarationState(state, node.lhs.first(), newValue)
                // lattice.pushToGeneralState(state, node, newValue)
                return newValue
            } else {
                // We do not support multiple lhs or rhs in the current implementation.
            }
        }

        lattice.pushToGeneralState(state, node, state.intervalOf(node))

        return state.intervalOf(node)
    }
}
