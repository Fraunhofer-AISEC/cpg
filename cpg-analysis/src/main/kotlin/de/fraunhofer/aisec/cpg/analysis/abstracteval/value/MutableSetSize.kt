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
import de.fraunhofer.aisec.cpg.analysis.abstracteval.pushToDeclarationState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.pushToGeneralState
import de.fraunhofer.aisec.cpg.evaluation.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ast.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.NewExpression
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.types.ListType
import de.fraunhofer.aisec.cpg.graph.types.SetType

/**
 * A [ValueEvaluator] which evaluates the size of mutable sets. It uses the [MutableSetSize] class
 * to track the size of the collection.
 */
class SetSizeEvaluator : ValueEvaluator() {
    override fun evaluate(node: Any?): Any? {
        if (node !is Node) return cannotEvaluate(null, this)

        return AbstractIntervalEvaluator().evaluate(node, MutableSetSize::class)
    }
}

/**
 * This class implements the [Value] interface for mutable Lists, tracking the size of the
 * collection. We assume that there is no operation that changes an array's size apart from
 * re-declaring it. NOTE: This is an unpolished example implementation. Before actual usage consider
 * the below TODOs and write a test file.
 */
class MutableSetSize() : MutableCollectionSize() {
    override fun applyEffect(
        lattice: TupleState<Any>,
        state: TupleStateElement<Any>,
        node: Node,
        edge: EvaluationOrder?,
        computeWithoutPush: Boolean,
    ): LatticeInterval {
        var target: Node? = null
        var variableSize: LatticeInterval = LatticeInterval.BOTTOM

        if (node is VariableDeclaration && node.initializer != null) {
            target = node
            variableSize =
                when (val init = node.initializer) {
                    is MemberCallExpression -> {
                        if (init.base?.type is SetType) { // TODO: check the function name
                            // This is a set copying the base, we can use the size of the base
                            createWithElementsFromElement(init.base!!, state)
                        } else if (init.base?.type is ListType) { // TODO: check the function name
                            // This is a set copying the base but as set. This may remove some
                            // elements!
                            val tmp = createWithElementsFromElement(init.base!!, state)
                            if (
                                tmp is LatticeInterval.Bounded &&
                                    tmp.upper > LatticeInterval.Bound.Value(0)
                            ) {
                                LatticeInterval.Bounded(LatticeInterval.Bound.Value(1), tmp.upper)
                            } else {
                                LatticeInterval.Bounded(0, 0)
                            }
                        } else {
                            val size = init.arguments.size.toLong()
                            if (size > 0) {
                                LatticeInterval.Bounded(1, size)
                            } else {
                                LatticeInterval.Bounded(0, 0)
                            }
                        }
                    }
                    is NewExpression -> {
                        if (
                            init.initializer !is CallExpression ||
                                (init.initializer as CallExpression).arguments.isEmpty()
                        ) {
                            // Empty List, we can use the empty collection size
                            createEmptyCollection()
                        } else if (
                            (init.initializer as CallExpression).arguments.size == 1 &&
                                (init.initializer as CallExpression).arguments.single().type is
                                    ListType
                        ) {
                            // This is a List with a single element, we can use the size of that
                            // element
                            val element = (init.initializer as CallExpression).arguments.single()
                            createWithElementsFromElement(element, state)
                        } else {
                            // This is a List with multiple elements, we can use the size of those
                            // elements
                            val elements = (init.initializer as CallExpression).arguments
                            createWithElements(elements, state)
                        }
                    }
                    else -> LatticeInterval.BOTTOM
                }
        } else if (node is MemberCallExpression && node.base != null) {
            target = node.base!!

            variableSize =
                when (node.name.localName) {
                    "add" -> {
                        addSingleElementWithElementCheck(target, state)
                    }

                    "addAll" -> {
                        if (node.arguments.singleOrNull()?.type is ListType) {
                            addMultipleElementsWithElementCheck(
                                target,
                                node.arguments.single(),
                                state,
                            )
                        } else {
                            addMultipleElementsWithElementCheck(target, node.arguments, state)
                        }
                    }

                    "clear" -> {
                        clearAllElements()
                    }

                    "remove" -> {
                        removeSingleElementWithElementCheck(target, state)
                    }

                    "removeAll" -> {
                        removeMultipleElementsWithoutElementCheck(target, state)
                    }

                    else -> LatticeInterval.BOTTOM
                }
        }

        if (target != null) {
            lattice.pushToGeneralState(state, target, variableSize)
            lattice.pushToDeclarationState(state, target, variableSize)
        }
        lattice.pushToDeclarationState(state, node, LatticeInterval.BOTTOM)

        // TODO: Push stuff, modify state.
        return variableSize
    }
}
