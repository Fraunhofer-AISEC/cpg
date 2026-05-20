/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.analysis.abstracteval.value.ArraySizeEvaluator
import de.fraunhofer.aisec.cpg.analysis.abstracteval.value.IntegerIntervalEvaluator
import de.fraunhofer.aisec.cpg.assumptions.addAssumptionDependence
import de.fraunhofer.aisec.cpg.evaluation.NumberSet
import de.fraunhofer.aisec.cpg.evaluation.SizeEvaluator
import de.fraunhofer.aisec.cpg.evaluation.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.UnknownMemoryValue
import de.fraunhofer.aisec.cpg.graph.types.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for all nodes in the graph.
 *
 * The optional argument [sel] can be used to filter nodes for which the condition has to be
 * fulfilled. This filter should be rather simple in most cases since its evaluation is not part of
 * the resulting reasoning chain.
 *
 * This method can be used similar to the logical implication to test "sel => mustSatisfy".
 */
inline fun <reified T> Node.allExtended(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> QueryTree<Boolean>,
): QueryTree<Boolean> {
    return evaluateExtended(sel, mustSatisfy).mergeWithAll(node = this)
}

/**
 * Evaluates the conditions specified in [mustSatisfy] hold for all nodes in the graph and returns
 * the individual results.
 *
 * The optional argument [sel] can be used to filter nodes for which the condition has to be
 * fulfilled. This filter should be rather simple in most cases since its evaluation is not part of
 * the resulting reasoning chain.
 *
 * This method can be used similar to the logical implication to test "sel => mustSatisfy".
 */
inline fun <reified T> Node.evaluateExtended(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> QueryTree<Boolean>,
): List<QueryTree<Boolean>> {
    val nodes = this.allChildrenWithOverlays(sel)
    return runBlocking {
        // Split the task into chunks of $CPU_SIZE and run them in coroutines. Collect the results
        // for each chunk, and when all coroutines are finished, flatMap them together to the final
        // result
        nodes
            .splitInto(minPartSize = 1)
            .map { chunk ->
                async(Dispatchers.Default) {
                    val local = mutableListOf<QueryTree<Boolean>>()
                    for (n in chunk) {
                        val res = mustSatisfy(n)
                        res.stringRepresentation =
                            "Starting at ${if (n is Node) n.compactToString() else n.toString()}: " +
                                res.stringRepresentation
                        if (n is Node) {
                            res.node = n
                        }
                        res.checkForSuppression()
                        res.addAssumptionDependence(this@evaluateExtended)
                        local.add(res)
                    }
                    local
                }
            }
            .awaitAll()
            .flatMap { it }
    }
}

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for all nodes in the graph. The
 * optional argument [sel] can be used to filter nodes for which the condition has to be fulfilled.
 *
 * This method can be used similar to the logical implication to test "sel => mustSatisfy".
 */
inline fun <reified T> Node.all(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> Boolean,
): Pair<Boolean, List<T>> {
    val nodes = this.allChildrenWithOverlays(sel)

    val failedNodes = nodes.filterNot(mustSatisfy)
    return Pair(failedNodes.isEmpty(), failedNodes)
}

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for at least one node in the graph.
 *
 * The optional argument [sel] can be used to filter nodes which are considered during the
 * evaluation. This filter should be rather simple in most cases since its evaluation is not part of
 * the resulting reasoning chain.
 */
inline fun <reified T> Node.existsExtended(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> QueryTree<Boolean>,
): QueryTree<Boolean> {
    return evaluateExtended(sel, mustSatisfy).mergeWithAny(node = this)
}

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for at least one node in the graph.
 * The optional argument [sel] can be used to filter nodes which are considered during the
 * evaluation.
 */
inline fun <reified T> Node.exists(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> Boolean,
): Pair<Boolean, List<T>> {
    val nodes = this.allChildrenWithOverlays(sel)

    val queryChildren = nodes.filter(mustSatisfy)
    return Pair(queryChildren.isNotEmpty(), queryChildren)
}

/**
 * Evaluates the size of a node, e.g. the byte-capacity of a buffer, the length of a string literal,
 * the element count of an initializer list.
 *
 * Defaults to [ArraySizeEvaluator], which is the canonical size evaluator going forward — it
 * handles literals, `InitializerList`, `ArrayConstruction`, and `malloc(constant)` directly, plus
 * tracks size through variable initializers via the abstract-interval analysis (so
 * `sizeof(reference_to_p)` where `char *p = malloc(64)` returns 64).
 *
 * Returns -1 when the size cannot be determined statically (parameters, opaque pointers, unbounded
 * reads, …).
 *
 * @param eval the evaluator to use; pass [SizeEvaluator] for the older, simpler implementation that
 *   walks variable initializers but doesn't track `malloc` sizes.
 */
fun sizeof(n: Node?, eval: ValueEvaluator = ArraySizeEvaluator()): QueryTree<Int> {
    val raw = eval.evaluate(n)
    val value: Int =
        when (raw) {
            is LatticeInterval.Bounded ->
                ((raw.upper as? LatticeInterval.Bound.Value)?.value)?.toInt() ?: -1
            is Number -> raw.toInt()
            else -> -1
        }
    return QueryTree(
        value,
        mutableListOf(),
        "sizeof($n)",
        n,
        operator = GenericQueryOperators.EVALUATE,
    )
}

/**
 * Like [sizeof], but preserves the full [LatticeInterval] in the [QueryTree] so callers (and the
 * REPL renderer) can see the lower/upper bounds the abstract evaluator computed, not just the
 * upper. Use this when you care about *ranges* — e.g. "this variable's size lies in [1, 64]" —
 * rather than a single conservative number.
 */
fun sizeBounds(n: Node?, eval: ValueEvaluator = ArraySizeEvaluator()): QueryTree<LatticeInterval> {
    val raw = eval.evaluate(n)
    val interval: LatticeInterval =
        when (raw) {
            is LatticeInterval -> raw
            is Number -> LatticeInterval.Bounded(raw.toLong(), raw.toLong())
            else -> LatticeInterval.BOTTOM
        }
    return QueryTree(
        interval,
        mutableListOf(),
        "sizeBounds($n)",
        n,
        operator = GenericQueryOperators.EVALUATE,
    )
}

/**
 * Retrieves the minimal value of the node.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun min(n: Node?, eval: ValueEvaluator = IntegerIntervalEvaluator()): QueryTree<Number> {
    val evalRes = eval.evaluate(n)
    if (evalRes is LatticeInterval) {
        val result =
            ((evalRes as? LatticeInterval.Bounded)?.upper as? LatticeInterval.Bound.Value)?.value
                ?: Long.MIN_VALUE
        return QueryTree(
            result,
            mutableListOf(QueryTree(n, operator = GenericQueryOperators.EVALUATE)),
            node = n,
            operator = GenericQueryOperators.EVALUATE,
        )
    } else if (evalRes is Number) {
        return QueryTree(
            evalRes,
            mutableListOf(QueryTree(n, operator = GenericQueryOperators.EVALUATE)),
            "min($n)",
            n,
            operator = GenericQueryOperators.EVALUATE,
        )
    }
    // Extend this when we have other evaluators.
    return QueryTree(
        (evalRes as? NumberSet)?.min() ?: -1,
        mutableListOf(),
        "min($n)",
        n,
        operator = GenericQueryOperators.EVALUATE,
    )
}

/**
 * Retrieves the minimal value of the nodes in the list.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun min(n: List<Node>?, eval: ValueEvaluator = IntegerIntervalEvaluator()): QueryTree<Number> {
    var result = Long.MAX_VALUE
    if (n == null)
        return QueryTree(
            result,
            mutableListOf(QueryTree(null, operator = GenericQueryOperators.EVALUATE)),
            operator = GenericQueryOperators.EVALUATE,
        )

    for (node in n) {
        when (val evalRes = eval.evaluate(node)) {
            is LatticeInterval -> {
                val minValue =
                    ((evalRes as? LatticeInterval.Bounded)?.upper as? LatticeInterval.Bound.Value)
                        ?.value
                        ?: ((evalRes as? LatticeInterval.Bounded)?.upper
                                as? LatticeInterval.Bound.INFINITE)
                            ?.let { Long.MIN_VALUE }
                        ?: Long.MAX_VALUE
                if (minValue < result) {
                    result = minValue
                }
            }

            is Number if evalRes.toLong() < result -> {
                result = evalRes.toLong()
            }

            is NumberSet if evalRes.min() < result -> {
                result = evalRes.min()
            }
        }
        // Extend this when we have other evaluators.
    }
    return QueryTree(result, mutableListOf(), "min($n)", operator = GenericQueryOperators.EVALUATE)
}

/**
 * Retrieves the maximal value of the nodes in the list.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun max(n: List<Node>?, eval: ValueEvaluator = IntegerIntervalEvaluator()): QueryTree<Number> {
    var result = Long.MIN_VALUE
    if (n == null)
        return QueryTree(
            result,
            mutableListOf(QueryTree(null, operator = GenericQueryOperators.EVALUATE)),
            operator = GenericQueryOperators.EVALUATE,
        )

    for (node in n) {
        when (val evalRes = eval.evaluate(node)) {
            is LatticeInterval -> {
                val maxValue =
                    ((evalRes as? LatticeInterval.Bounded)?.upper as? LatticeInterval.Bound.Value)
                        ?.value
                        ?: ((evalRes as? LatticeInterval.Bounded)?.upper
                                as? LatticeInterval.Bound.INFINITE)
                            ?.let { Long.MAX_VALUE }
                        ?: Long.MIN_VALUE
                if (maxValue > result) {
                    result = maxValue
                }
            }

            is Number if evalRes.toLong() > result -> {
                result = evalRes.toLong()
            }

            is NumberSet if evalRes.max() > result -> {
                result = evalRes.max()
            }
        }
        // Extend this when we have other evaluators.
    }
    return QueryTree(result, mutableListOf(), "max($n)", operator = GenericQueryOperators.EVALUATE)
}

/**
 * Retrieves the maximal value of the node.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun max(n: Node?, eval: ValueEvaluator = IntegerIntervalEvaluator()): QueryTree<Number> {
    val evalRes = eval.evaluate(n)

    if (evalRes is LatticeInterval) {
        val result =
            ((evalRes as? LatticeInterval.Bounded)?.upper as? LatticeInterval.Bound.Value)?.value
                ?: Long.MAX_VALUE
        return QueryTree(
            result,
            mutableListOf(QueryTree(n, operator = GenericQueryOperators.EVALUATE)),
            node = n,
            operator = GenericQueryOperators.EVALUATE,
        )
    } else if (evalRes is Number) {
        return QueryTree(
            evalRes,
            mutableListOf(QueryTree(n, operator = GenericQueryOperators.EVALUATE)),
            node = n,
            operator = GenericQueryOperators.EVALUATE,
        )
    }
    // Extend this when we have other evaluators.
    return QueryTree(
        (evalRes as? NumberSet)?.max() ?: -1,
        mutableListOf(),
        "max($n)",
        n,
        operator = GenericQueryOperators.EVALUATE,
    )
}

/** Calls [ValueEvaluator.evaluate] for this expression, thus trying to resolve a constant value. */
operator fun Expression?.invoke(): QueryTree<Any?> {
    return QueryTree(
        this?.evaluate(),
        mutableListOf(QueryTree(this, operator = GenericQueryOperators.EVALUATE)),
        node = this,
        operator = GenericQueryOperators.EVALUATE,
    )
}

/**
 * Determines the maximal value. Only works for a couple of types! TODO: This method needs
 * improvement! It only works for Java types!
 */
fun maxSizeOfType(type: Type): QueryTree<Number> {
    val maxVal =
        when (type.typeName) {
            "byte" -> Byte.MAX_VALUE
            "short" -> Short.MAX_VALUE
            "int" -> Int.MAX_VALUE
            "long" -> Long.MAX_VALUE
            "float" -> Float.MAX_VALUE
            "double" -> Double.MAX_VALUE
            else -> Long.MAX_VALUE
        }
    return QueryTree(
        maxVal,
        mutableListOf(QueryTree(type, operator = GenericQueryOperators.EVALUATE)),
        "maxSizeOfType($type)",
        node = type,
        operator = GenericQueryOperators.EVALUATE,
    )
}

/**
 * Determines the minimal value. Only works for a couple of types! TODO: This method needs
 * improvement! It only works for Java types!
 */
fun minSizeOfType(type: Type): QueryTree<Number> {
    val maxVal =
        when (type.typeName) {
            "byte" -> Byte.MIN_VALUE
            "short" -> Short.MIN_VALUE
            "int" -> Int.MIN_VALUE
            "long" -> Long.MIN_VALUE
            "float" -> Float.MIN_VALUE
            "double" -> Double.MIN_VALUE
            else -> Long.MIN_VALUE
        }
    return QueryTree(
        maxVal,
        mutableListOf(QueryTree(type, operator = GenericQueryOperators.EVALUATE)),
        "minSizeOfType($type)",
        node = type,
        operator = GenericQueryOperators.EVALUATE,
    )
}

/** The size of this expression. It uses the default argument for `eval` of [size] */
val Expression.size: QueryTree<Int>
    get() {
        return sizeof(this)
    }

/**
 * The minimal integer value of this expression. It uses the default argument for `eval` of [min]
 */
val Expression.min: QueryTree<Number>
    get() {
        return min(this)
    }

/**
 * The maximal integer value of this expression. It uses the default argument for `eval` of [max]
 */
val Expression.max: QueryTree<Number>
    get() {
        return max(this)
    }

/** Calls [ValueEvaluator.evaluate] for this expression, thus trying to resolve a constant value. */
val Expression.value: QueryTree<Any?>
    get() {
        return QueryTree(
            evaluate(ValueEvaluator(), useCache = true),
            mutableListOf(),
            "$this",
            this,
            operator = GenericQueryOperators.EVALUATE,
        )
    }

/**
 * Calls [ValueEvaluator.evaluate] for this expression, thus trying to resolve a constant value. The
 * result is interpreted as an integer.
 */
val Expression.intValue: QueryTree<Int>?
    get() {
        val evalRes = evaluate() as? Int ?: return null
        return QueryTree(
            evalRes,
            mutableListOf(),
            "$this",
            this,
            operator = GenericQueryOperators.EVALUATE,
        )
    }

/**
 * Checks if this node is an [UnknownMemoryValue] with a taint name ending in "taint.[name]". Common
 * taint names: "freed", "deallocated", "uninitialized"
 */
fun Node.isTaint(taintName: String): Boolean {
    return this is UnknownMemoryValue && this.name.localName.endsWith("taint.$taintName")
}
