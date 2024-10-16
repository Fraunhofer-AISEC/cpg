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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewArrayExpression
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.query.value
import org.apache.commons.lang3.NotImplementedException

class Array<T> : Value {
    override fun applyEffect(current: LatticeInterval, node: Node, name: String): LatticeInterval {
        // There are no functions that change the size of a Java array without destroying it
        if (node is VariableDeclaration && node.initializer != null) {
            val initValue = getSize(node.initializer!!)
            return LatticeInterval.Bounded(initValue, initValue)
        }
        return current
    }

    private fun getSize(node: Node): Int {
        return when (node) {
            // TODO: could be more performant if you detect that all initializers are Literals
            is Literal<*> -> {
                if (node.type !is IntegerType) {
                    throw NotImplementedException()
                } else {
                    1
                }
            }
            is InitializerListExpression -> {
                node.initializers.fold(0) { acc, init -> acc + getSize(init) }
            }
            is NewArrayExpression -> {
                if (node.initializer != null) {
                    getSize(node.initializer!!)
                } else {
                    node.dimensions
                        .map { it.value.value as Int }
                        .reduce { acc, dimension -> acc * dimension }
                }
            }
            else -> throw NotImplementedException()
        }
    }
}
