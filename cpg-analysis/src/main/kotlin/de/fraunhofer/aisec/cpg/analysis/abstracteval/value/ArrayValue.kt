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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewArrayExpression
import de.fraunhofer.aisec.cpg.query.value

/**
 * This class implements the [Value] interface for Arrays, tracking the size of the collection. We
 * assume that there is no operation that changes an array's size apart from re-declaring it.
 */
class ArrayValue : Value<LatticeInterval> {
    override fun applyEffect(
        current: LatticeInterval,
        lattice: TupleState<Any>,
        state: TupleStateElement<Any>,
        node: Node,
        name: String,
        computeWithoutPush: Boolean,
    ): LatticeInterval {
        // (Re-)Declaration
        if (
            node is VariableDeclaration && node.initializer != null && node.name.localName == name
        ) {
            val initValue = getSize(node.initializer!!)
            return LatticeInterval.Bounded(initValue, initValue)
        }
        return current
    }

    private fun getSize(node: Node): Long {
        return when (node) {
            // TODO: depending on the desired behavior we could distinguish between included types
            // (e.g. String and Int Literals)
            is Literal<*> -> {
                1
            }
            is InitializerListExpression -> {
                node.initializers.fold(0L) { acc, init -> acc + getSize(init) }
            }
            is NewArrayExpression -> {
                if (node.initializer != null) {
                    getSize(node.initializer!!)
                } else {
                    node.dimensions
                        .map { (it.value.value as Number).toLong() }
                        .reduce { acc, dimension -> acc * dimension }
                }
            }
            else -> TODO()
        }
    }
}
