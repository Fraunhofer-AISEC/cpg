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

import de.fraunhofer.aisec.cpg.analysis.abstracteval.AbstractEvaluator
import de.fraunhofer.aisec.cpg.analysis.abstracteval.IntervalLattice
import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.query.value
import org.apache.commons.lang3.NotImplementedException

class Integer : Value {
    override fun applyEffect(current: LatticeInterval, node: Node, name: String): LatticeInterval {
        if (
            node is VariableDeclaration && node.initializer != null && node.name.localName == name
        ) {
            val initValue =
                when (val init = node.initializer) {
                    is Literal<*> -> init.value as? Int ?: throw NotImplementedException()
                    else -> throw NotImplementedException()
                }
            return LatticeInterval.Bounded(initValue, initValue)
        }
        if (node is UnaryOperator) {
            if (node.input.code == name) {
                return when (node.operatorCode) {
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
            }
        } else if (node is AssignExpression) {
            if (node.lhs.any { it.code == name }) {
                // TODO: we need to evaluate the right hand side for all cases!
                return when (node.operatorCode) {
                    "=" -> {
                        var newInterval: LatticeInterval = current
                        // If the rhs is only a literal use this exact value
                        if (node.rhs.size == 1 && node.rhs[0] is Literal<*>) {
                            val value = node.rhs[0].value.value as? Int
                            if (value != null) {
                                newInterval = LatticeInterval.Bounded(value, value)
                            }
                        } else {
                            TODO()
                        }
                        newInterval
                    }
                    "+=" -> {
                        var newInterval: LatticeInterval = current
                        // If the rhs is only a literal we subtract this exact value
                        if (node.rhs.size == 1 && node.rhs[0] is Literal<*>) {
                            val value = node.rhs[0].value.value as? Int
                            if (value != null) {
                                val valueInterval = LatticeInterval.Bounded(value, value)
                                newInterval = current.plus(valueInterval)
                            }
                        }
                        // Per default set upper bound to infinite
                        else {
                            val joinInterval: LatticeInterval =
                                LatticeInterval.Bounded(
                                    LatticeInterval.Bound.INFINITE,
                                    LatticeInterval.Bound.INFINITE
                                )
                            newInterval = current.join(joinInterval)
                        }
                        newInterval
                    }
                    "-=" -> {
                        var newInterval: LatticeInterval = current
                        // If the rhs is only a literal we subtract this exact value
                        if (node.rhs.size == 1 && node.rhs[0] is Literal<*>) {
                            val value = node.rhs[0].value.value as? Int
                            if (value != null) {
                                val valueInterval = LatticeInterval.Bounded(value, value)
                                newInterval = current.minus(valueInterval)
                            }
                        }
                        // Per default set lower bound to negative infinite
                        else {
                            val joinInterval: LatticeInterval =
                                LatticeInterval.Bounded(
                                    LatticeInterval.Bound.NEGATIVE_INFINITE,
                                    LatticeInterval.Bound.NEGATIVE_INFINITE
                                )
                            newInterval = current.join(joinInterval)
                        }
                        newInterval
                    }
                    "*=" -> {
                        TODO()
                    }
                    "/=" -> {
                        TODO()
                    }
                    "%=" -> {
                        TODO()
                    }
                    else -> current
                }
            }
        } else if (
            node is MemberCallExpression && node.arguments.any { it.name.localName == name }
        ) {
            // This is a function call that uses the variable as an argument.
            // To find side effects we need to create a local evaluator for this function and
            // return the value of the renamed variable at the last statement
            // TODO: this currently does not work if the variable is given for multiple parameters
            // TODO: error handling
            val function = node.invokes.first()
            val argPos = node.arguments.indexOfFirst { it.name.localName == name }

            // We cannot take the "first" as that refers to the Block which has no nextEOG
            // Also debugging is ugly because the getter of Node.statements is overwritten
            val functionStart = function.body.statements[1]
            // This could be a Location but the CPG often just hands us "null"
            val functionEnd = function.body.statements.last()
            val newTargetName = function.parameters[argPos].name.localName

            val localEvaluator = AbstractEvaluator()
            return localEvaluator.evaluate(
                newTargetName,
                functionStart,
                functionEnd,
                this::class,
                IntervalLattice(current)
            )
        }
        return current
    }
}
