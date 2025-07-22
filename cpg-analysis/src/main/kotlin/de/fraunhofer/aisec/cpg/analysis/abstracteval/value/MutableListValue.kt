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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewExpression
import de.fraunhofer.aisec.cpg.graph.types.IntegerType

/**
 * This class implements the [Value] interface for mutable Lists, tracking the size of the
 * collection. We assume that there is no operation that changes an array's size apart from
 * re-declaring it. NOTE: This is an unpolished example implementation. Before actual usage consider
 * the below TODOs and write a test file.
 */
@Suppress("UNUSED")
class MutableListValue : Value<LatticeInterval> {
    override fun applyEffect(
        current: LatticeInterval?,
        lattice: TupleState<Any>,
        state: TupleStateElement<Any>,
        node: Node,
        edge: EvaluationOrder?,
        name: String?,
        computeWithoutPush: Boolean,
    ): LatticeInterval {
        current ?: TODO()

        if (
            node is VariableDeclaration && node.initializer != null && node.name.localName == name
        ) {
            when (val init = node.initializer) {
                is MemberCallExpression -> {
                    val size = init.arguments.size.toLong()
                    return LatticeInterval.Bounded(size, size)
                }
                is NewExpression -> {
                    // TODO: consider collection as argument!
                    return LatticeInterval.Bounded(0, 0)
                }
                else -> TODO()
            }
        }
        // State can only be directly changed via MemberCalls (add, clear, ...)
        if (node !is MemberCallExpression) {
            return current
        }
        // If the call does not have the subject as base, check for subject as argument
        if ((node.callee as? MemberExpression)?.base?.code != name) {
            if (node.arguments.any { it.name.localName == name }) {
                TODO()
                // This is a function call that uses the variable as an argument.
                // To find side effects we need to create a local evaluator for this function
                // and return the value of the renamed variable at the last statement.
            } else {
                return current
            }
        }
        return when (node.name.localName) {
            "add" -> {
                val oneInterval = LatticeInterval.Bounded(1, 1)
                current + oneInterval
            }
            // TODO: this should trigger another List size evaluation for the argument!
            //  also check and prevent -1 result
            "addAll" -> {
                val openUpper = LatticeInterval.Bounded(0, LatticeInterval.Bound.INFINITE)
                current + openUpper
            }
            "clear" -> {
                LatticeInterval.Bounded(0, 0)
            }
            "remove" -> {
                // We have to differentiate between remove with index or object argument
                // Latter may do nothing if the element is not in the list
                val modificationInterval =
                    if (node.arguments.first().type is IntegerType) {
                        LatticeInterval.Bounded(1, 1)
                    } else {
                        LatticeInterval.Bounded(1, 0)
                    }
                // This meet makes sure we do not drop below zero
                (current - modificationInterval).meet(
                    LatticeInterval.Bounded(0, LatticeInterval.Bound.INFINITE)
                )
            }
            // The size of the argument list is (almost) irrelevant as it has no influence on the
            // possible outcomes.
            // We could check if it is empty, but that causes significant overhead
            "removeAll" -> {
                val zeroInterval = LatticeInterval.Bounded(0, 0)
                current.join(zeroInterval)
            }
            else -> current
        }
    }
}
