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
import org.apache.commons.lang3.NotImplementedException

/** This class implements the [Value] interface for Integer values. */
class IntegerValue : Value<LatticeInterval> {
    override fun applyEffect(current: LatticeInterval, node: Node, name: String): LatticeInterval {
        // (Re-)Declarations of the Variable
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
        // Unary Operators
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
        }
        // Assignments and combined assign expressions
        // TODO: we should aim to evaluate the right hand side expression for all cases!
        //  currently evaluation only works correctly for literals
        else if (node is AssignExpression) {
            if (node.lhs.any { it.code == name }) {
                return when (node.operatorCode) {
                    "=" -> {
                        // If the rhs is only a literal use this exact value
                        val value = (node.rhs.getOrNull(0) as? Literal<*>)?.value as? Int
                        val newInterval =
                            if (value != null) {
                                LatticeInterval.Bounded(value, value)
                            }
                            // Per default set the bounds to unknown
                            else {
                                LatticeInterval.Bounded(
                                    LatticeInterval.Bound.NEGATIVE_INFINITE,
                                    LatticeInterval.Bound.INFINITE
                                )
                            }
                        newInterval
                    }
                    "+=" -> {
                        // If the rhs is only a literal we subtract this exact value
                        val value = (node.rhs.getOrNull(0) as? Literal<*>)?.value as? Int
                        val newInterval =
                            if (value != null) {
                                val valueInterval = LatticeInterval.Bounded(value, value)
                                current + valueInterval
                            }
                            // Per default lose all information
                            else {
                                val joinInterval: LatticeInterval =
                                    LatticeInterval.Bounded(
                                        LatticeInterval.Bound.NEGATIVE_INFINITE,
                                        LatticeInterval.Bound.INFINITE
                                    )
                                current.join(joinInterval)
                            }
                        newInterval
                    }
                    "-=" -> {
                        // If the rhs is only a literal we subtract this exact value
                        val value = (node.rhs.getOrNull(0) as? Literal<*>)?.value as? Int
                        val newInterval =
                            if (value != null) {
                                val valueInterval = LatticeInterval.Bounded(value, value)
                                current - valueInterval
                            }
                            // Per default lose all information
                            else {
                                val joinInterval: LatticeInterval =
                                    LatticeInterval.Bounded(
                                        LatticeInterval.Bound.NEGATIVE_INFINITE,
                                        LatticeInterval.Bound.INFINITE
                                    )
                                current.join(joinInterval)
                            }
                        newInterval
                    }
                    "*=" -> {
                        // If the rhs is only a literal we subtract this exact value
                        val value = (node.rhs.getOrNull(0) as? Literal<*>)?.value as? Int
                        val newInterval =
                            if (value != null) {
                                val valueInterval = LatticeInterval.Bounded(value, value)
                                current * valueInterval
                            }
                            // Per default lose all information
                            else {
                                LatticeInterval.Bounded(
                                    LatticeInterval.Bound.NEGATIVE_INFINITE,
                                    LatticeInterval.Bound.INFINITE
                                )
                            }
                        newInterval
                    }
                    "/=" -> {
                        // If the rhs is only a literal we subtract this exact value
                        val value = (node.rhs.getOrNull(0) as? Literal<*>)?.value as? Int
                        val newInterval =
                            if (value != null) {
                                val valueInterval = LatticeInterval.Bounded(value, value)
                                current / valueInterval
                            }
                            // Per default lose all information
                            else {
                                LatticeInterval.Bounded(
                                    LatticeInterval.Bound.NEGATIVE_INFINITE,
                                    LatticeInterval.Bound.INFINITE
                                )
                            }
                        newInterval
                    }
                    "%=" -> {
                        // If the rhs is only a literal we subtract this exact value
                        val value = (node.rhs.getOrNull(0) as? Literal<*>)?.value as? Int
                        val newInterval =
                            if (value != null) {
                                val valueInterval = LatticeInterval.Bounded(value, value)
                                current % valueInterval
                            }
                            // Per default lose all information
                            else {
                                LatticeInterval.Bounded(
                                    LatticeInterval.Bound.NEGATIVE_INFINITE,
                                    LatticeInterval.Bound.INFINITE
                                )
                            }
                        newInterval
                    }
                    else -> current
                }
            }
        }
        return current
    }
}
