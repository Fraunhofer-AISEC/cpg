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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewExpression
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import org.apache.commons.lang3.NotImplementedException

class MutableList : Value {
    override fun applyEffect(current: LatticeInterval, node: Node, name: String): LatticeInterval {
        if (node is VariableDeclaration && node.initializer != null) {
            when (val init = node.initializer) {
                is MemberCallExpression -> {
                    val size = init.arguments.size
                    return LatticeInterval.Bounded(size, size)
                }
                is NewExpression -> {
                    // TODO: could have a collection as argument!
                    return LatticeInterval.Bounded(0, 0)
                }
                else -> throw NotImplementedException()
            }
        }
        // TODO: state can also be estimated by conditions! (if (l.size < 3) ...)
        // TODO: assignment -> new size
        // State can only be directly changed via MemberCalls (add, clear, ...)
        if (node !is MemberCallExpression) {
            return current
        }
        // Only consider calls that have the subject as base
        if ((node.callee as? MemberExpression)?.base?.code != name) {
            return current
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
                if (node.arguments.first().type is IntegerType) {
                    val oneInterval = LatticeInterval.Bounded(1, 1)
                    current - oneInterval
                } else {
                    // TODO: If we know the list is empty, we know the operation has no effect
                    val oneZeroInterval = LatticeInterval.Bounded(1, 0)
                    current - oneZeroInterval
                }
            }
            // TODO: as optimization we could check whether the argument list is empty.
            // The size of the argument list is (almost) irrelevant as it has no influence on the
            // possible outcomes
            "removeAll" -> {
                val zeroInterval = LatticeInterval.Bounded(0, 0)
                current.join(zeroInterval)
            }
            else -> current
        }
    }
}
