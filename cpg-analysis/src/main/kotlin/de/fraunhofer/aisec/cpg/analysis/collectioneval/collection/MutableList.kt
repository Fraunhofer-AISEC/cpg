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
package de.fraunhofer.aisec.cpg.analysis.collectioneval.collection

import de.fraunhofer.aisec.cpg.analysis.collectioneval.LatticeInterval
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.IntegerType

class MutableList : Collection {
    override fun applyEffect(current: LatticeInterval, node: Node, name: String): LatticeInterval {
        // State can only be changed via MemberCalls (add, clear, ...)
        if (node !is MemberCallExpression) {
            return current
        }
        return when (node.name.toString()) {
            "$name.add" -> {
                val oneInterval = LatticeInterval.Bounded(1, 1)
                current + oneInterval
            }
            // TODO: this should trigger another List size evaluation for the argument!
            //  also check and prevent -1 result
            "$name.addAll" -> {
                val openUpper = LatticeInterval.Bounded(0, LatticeInterval.Bound.TOP)
                current + openUpper
            }
            "$name.clear" -> {
                LatticeInterval.Bounded(0, 0)
            }
            "$name.remove" -> {
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
            "$name.removeAll" -> {
                val zeroInterval = LatticeInterval.Bounded(0, 0)
                current.join(zeroInterval)
            }
            else -> current
        }
    }

    override fun getInitializer(node: Node?): Node? {
        return when (node) {
            null -> null!!
            is Reference -> getInitializer(node.refersTo)
            is VariableDeclaration -> node.initializer!!
            else -> getInitializer(node.prevDFG.firstOrNull())
        }
    }

    override fun getInitialRange(initializer: Node): LatticeInterval {
        val size = (initializer as MemberCallExpression).arguments.size
        return LatticeInterval.Bounded(size, size)
    }
}
