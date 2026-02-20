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

import de.fraunhofer.aisec.cpg.analysis.abstracteval.AbstractIntervalEvaluator
import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleStateElement
import de.fraunhofer.aisec.cpg.analysis.abstracteval.intervalOf
import de.fraunhofer.aisec.cpg.analysis.abstracteval.pushToDeclarationState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.pushToGeneralState
import de.fraunhofer.aisec.cpg.evaluation.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Call
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerList
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewArray
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.query.value

/**
 * A [ValueEvaluator] which evaluates the size of arrays. It uses the [ArrayValue] class to track
 * the size of the collection.
 */
class ArraySizeEvaluator : ValueEvaluator() {
    override fun evaluate(node: Any?): Any? {
        if (node !is Node) return cannotEvaluate(null, this)

        return AbstractIntervalEvaluator().evaluate(node, ArrayValue::class)
    }
}

/**
 * This class implements the [Value] interface for Arrays, tracking the size of the collection. We
 * assume that there is no operation that changes an array's size apart from re-declaring it.
 */
class ArrayValue : Value<LatticeInterval> {
    override fun applyEffect(
        lattice: TupleState<Any>,
        state: TupleStateElement<Any>,
        node: Node,
        edge: EvaluationOrder?,
        computeWithoutPush: Boolean,
    ): LatticeInterval {
        var size: LatticeInterval = LatticeInterval.BOTTOM
        var target: Node? = null
        if (node is Variable && node.initializer != null && node.type is PointerType) {
            size = getSize(node.initializer!!)
            target = node
        } else if (node is Assign && node.rhs.size == 1 && node.lhs.size == 1) {
            size = getSize(node.rhs.single())
            target = node.lhs.single()
        }
        if (target != null) {
            lattice.pushToGeneralState(state, target, size)
            lattice.pushToDeclarationState(state, target, size)
            lattice.pushToGeneralState(state, node, state.intervalOf(node))
            return size
        }

        lattice.pushToGeneralState(state, node, state.intervalOf(node))
        return state.intervalOf(node)
    }

    private fun getSize(node: Node): LatticeInterval {
        return when (node) {
            is Literal<*> -> {
                if (node.value is String) {
                    // For strings, we return the length of the string
                    val length = (node.value as String).length.toLong()
                    LatticeInterval.Bounded(length, length)
                } else {
                    // Otherwise, we assume that the size is 1.
                    LatticeInterval.Bounded(1, 1)
                }
            }
            is InitializerList -> {
                // The number of elements in the initializer list
                val length = node.initializers.size.toLong()
                LatticeInterval.Bounded(length, length)
                // node.initializers.fold(0L) { acc, init -> acc + getSize(init) }
            }
            is NewArray -> {
                if (node.initializer != null) {
                    getSize(node.initializer!!)
                } else {
                    val length =
                        node.dimensions
                            .map { (it.value.value as Number).toLong() }
                            .reduce { acc, dimension -> acc * dimension }
                    LatticeInterval.Bounded(length, length)
                }
            }
            is Call -> {
                if (node.name.localName == "malloc") {
                    val length = (node.arguments.singleOrNull()?.value?.value as? Number)?.toLong()
                    length?.let { LatticeInterval.Bounded(length, length) }
                        ?: LatticeInterval.BOTTOM
                } else {
                    LatticeInterval.BOTTOM
                }
            }
            else -> TODO()
        }
    }
}
