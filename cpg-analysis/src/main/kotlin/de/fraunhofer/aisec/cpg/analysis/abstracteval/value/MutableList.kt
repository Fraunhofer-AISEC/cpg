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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewExpression
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import org.apache.commons.lang3.NotImplementedException

/**
 * This class implements the [Value] interface for Mutable Lists, tracking the size of the
 * collection. We assume that there is no operation that changes an array's size apart from
 * re-declaring it. NOTE: This is an unpolished example implementation. Before actual usage consider
 * the below TODOs and write a test file.
 */
@Suppress("UNUSED")
class MutableList : Value {
    override fun applyEffect(current: LatticeInterval, node: Node, name: String): LatticeInterval {
        if (
            node is VariableDeclaration && node.initializer != null && node.name.localName == name
        ) {
            when (val init = node.initializer) {
                is MemberCallExpression -> {
                    val size = init.arguments.size
                    return LatticeInterval.Bounded(size, size)
                }
                is NewExpression -> {
                    // TODO: consider collection as argument!
                    return LatticeInterval.Bounded(0, 0)
                }
                else -> throw NotImplementedException()
            }
        }
        // State can only be directly changed via MemberCalls (add, clear, ...)
        if (node !is MemberCallExpression) {
            return current
        }
        // If the call does not have the subject as base, check for subject as argument
        if ((node.callee as? MemberExpression)?.base?.code != name) {
            if (node.arguments.any { it.name.localName == name }) {
                // This is a function call that uses the variable as an argument.
                // To find side effects we need to create a local evaluator for this function
                // and return the value of the renamed variable at the last statement.
                // TODO: unfinished:
                //  this currently does not work if the variable is given for multiple
                // parameters!
                val function = node.invokes.first()
                val argPos = node.arguments.indexOfFirst { it.name.localName == name }

                // We cannot take the "first" as that refers to the Block which has no nextEOG
                val functionStart = function.body.statements[1]
                // This variable should be a PhysicalLocation but the CPG often just hands us
                // "null"
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
