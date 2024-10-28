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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
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
        }
        return current
    }
}
