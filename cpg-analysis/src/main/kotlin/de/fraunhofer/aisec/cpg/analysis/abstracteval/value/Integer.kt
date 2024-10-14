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
import de.fraunhofer.aisec.cpg.graph.BranchingNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import org.apache.commons.lang3.NotImplementedException

class Integer : Value {
    override fun applyEffect(
        current: LatticeInterval,
        node: Node,
        name: String
    ): Pair<LatticeInterval, Boolean> {
        // Branching nodes have to be assumed to have an effect
        if (node is BranchingNode) {
            return current to true
        }
        // TODO: recursively evaluate right-hand-side to narrow down results
        if (node is UnaryOperator) {
            if (node.input.code == name) {
                return when (node.operatorCode) {
                    "++" -> {
                        val oneInterval = LatticeInterval.Bounded(1, 1)
                        current + oneInterval to true
                    }
                    "--" -> {
                        val oneInterval = LatticeInterval.Bounded(1, 1)
                        current - oneInterval to true
                    }
                    else -> current to false
                }
            }
        } else if (node is AssignExpression) {
            if (node.lhs.any { it.code == name }) {
                // TODO: we need to evaluate the right hand side!
                return when (node.operatorCode) {
                    "=" -> {
                        TODO()
                    }
                    "+=" -> {
                        val openUpper = LatticeInterval.Bounded(0, LatticeInterval.Bound.INFINITE)
                        current + openUpper to true
                    }
                    "-=" -> {
                        val zeroInterval =
                            LatticeInterval.Bounded(
                                LatticeInterval.Bound.NEGATIVE_INFINITE,
                                LatticeInterval.Bound.NEGATIVE_INFINITE
                            )
                        current.join(zeroInterval) to true
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
                    else -> current to false
                }
            }
        }
        return current to false
    }

    override fun getInitialRange(initializer: Node): LatticeInterval {
        val value =
            when (initializer) {
                is Literal<*> -> initializer.value as? Int ?: throw NotImplementedException()
                else -> throw NotImplementedException()
            }
        return LatticeInterval.Bounded(value, value)
    }
}
