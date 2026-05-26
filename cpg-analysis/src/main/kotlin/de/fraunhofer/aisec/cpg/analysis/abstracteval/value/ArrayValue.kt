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
import de.fraunhofer.aisec.cpg.graph.expressions.ArrayConstruction
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.InitializerList
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.query.value
import java.util.concurrent.ConcurrentHashMap

/**
 * A [ValueEvaluator] which evaluates the size of arrays. It uses the [ArrayValue] class to track
 * the size of the collection.
 */
class ArraySizeEvaluator : ValueEvaluator() {
    /** Cache calculated values so that we don't have to calculate them each time */
    companion object {
        private val valuesCache = ConcurrentHashMap<Int, Any>()
    }

    override fun evaluate(node: Any?, useCache: Boolean): Any? {
        if (node !is Node) return cannotEvaluate(null, this)

        // Shape-based shortcut: literals, initializer lists, ArrayConstruction, and malloc with
        // a constant argument all carry their size in their AST shape, so we can answer without
        // walking the EOG. For nodes whose size lives in interval-analysis state (Variable,
        // Reference, branch-merged allocations) directSize returns BOTTOM and we fall through
        // to the full analysis.
        val direct = ArrayValue.directSize(node)
        if (direct !is LatticeInterval.BOTTOM) return direct

        return if (useCache)
            valuesCache.getOrPut(node.hashCode()) {
                AbstractIntervalEvaluator().evaluate(node, ArrayValue::class)
            }
        else AbstractIntervalEvaluator().evaluate(node, ArrayValue::class)
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
            size = directSize(node.initializer!!)
            target = node
        } else if (node is Assign && node.rhs.size == 1 && node.lhs.size == 1) {
            size = directSize(node.rhs.single())
            // Anchor on the underlying Variable rather than the LHS Reference so that branches
            // writing to the same variable (e.g. `if (c) buf = malloc(16) else buf = malloc(64)`)
            // share a single key in general state — without this, each branch pushes against its
            // own Reference instance and the join doesn't merge them.
            target = (node.lhs.single() as? Reference)?.refersTo as? Variable ?: node.lhs.single()
        }
        if (target != null) {
            lattice.pushToGeneralState(state, target, size)
            lattice.pushToDeclarationState(state, target, size)
            return size
        }

        // Don't pollute general state with a TOP entry for nodes we have no info about — a later
        // `pushToGeneralState` for the same node lubs against TOP and would clamp any refined
        // value (e.g. an Assign's [16, 16]) right back to TOP.
        val current = state.intervalOf(node)
        if (current !is LatticeInterval.TOP) {
            lattice.pushToGeneralState(state, node, current)
        }
        return current
    }

    companion object {
        /**
         * Size of [node] determinable directly from its AST shape: string literals, fixed
         * initializer lists, statically-dimensioned [ArrayConstruction], and `malloc` with a
         * constant argument. Returns [LatticeInterval.BOTTOM] for nodes whose size requires
         * interval-analysis context (Variable reads, References, branch-merged allocations, unknown
         * calls) — callers should run [AbstractIntervalEvaluator] as a fallback in that case.
         */
        fun directSize(node: Node): LatticeInterval =
            when (node) {
                is Literal<*> -> {
                    val v = node.value
                    if (v is String) LatticeInterval.Bounded(v.length.toLong(), v.length.toLong())
                    else LatticeInterval.Bounded(1, 1)
                }
                is InitializerList -> {
                    val length = node.initializers.size.toLong()
                    LatticeInterval.Bounded(length, length)
                }
                is ArrayConstruction -> {
                    if (node.initializer != null) {
                        directSize(node.initializer!!)
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
                        (node.arguments.singleOrNull()?.value?.value as? Number)?.toLong()?.let {
                            LatticeInterval.Bounded(it, it)
                        } ?: LatticeInterval.BOTTOM
                    } else {
                        LatticeInterval.BOTTOM
                    }
                }
                else -> LatticeInterval.BOTTOM
            }
    }
}
