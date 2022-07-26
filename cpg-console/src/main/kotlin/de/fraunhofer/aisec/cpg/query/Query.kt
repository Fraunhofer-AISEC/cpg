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

import de.fraunhofer.aisec.cpg.ExperimentalGraph
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.analysis.MultiValueEvaluator
import de.fraunhofer.aisec.cpg.analysis.NumberSet
import de.fraunhofer.aisec.cpg.analysis.SizeEvaluator
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for all nodes in the graph.
 *
 * The optional argument [sel] can be used to filter nodes for which the condition has to be
 * fulfilled. This filter should be rather simple in most cases since its evaluation is not part of
 * the resulting reasoning chain.
 *
 * This method can be used similar to the logical implication to test "sel => mustSatisfy".
 */
@ExperimentalGraph
inline fun <reified T> TranslationResult.all(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> QueryTree<Boolean>
): QueryTree<Boolean> {
    var nodes = this.graph.nodes.filterIsInstance<T>()

    // filter the nodes according to the selector
    if (sel != null) {
        nodes = nodes.filter(sel)
    }

    val queryChildren = nodes.map(mustSatisfy)
    return QueryTree(queryChildren.all { it.value }, queryChildren.toMutableList())
}

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for all nodes in the graph.
 *
 * The optional argument [sel] can be used to filter nodes for which the condition has to be
 * fulfilled. This filter should be rather simple in most cases since its evaluation is not part of
 * the resulting reasoning chain.
 *
 * This method can be used similar to the logical implication to test "sel => mustSatisfy".
 */
@ExperimentalGraph
inline fun <reified T> Node.all(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> QueryTree<Boolean>
): QueryTree<Boolean> {
    val children = this.astChildren

    var nodes = children.filterIsInstance<T>()

    // filter the nodes according to the selector
    if (sel != null) {
        nodes = nodes.filter(sel)
    }

    val queryChildren = nodes.map(mustSatisfy)
    return QueryTree(queryChildren.all { it.value }, queryChildren.toMutableList())
}

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for at least one node in the graph.
 *
 * The optional argument [sel] can be used to filter nodes which are considered during the
 * evaluation. This filter should be rather simple in most cases since its evaluation is not part of
 * the resulting reasoning chain.
 */
@ExperimentalGraph
inline fun <reified T> TranslationResult.exists(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> QueryTree<Boolean>
): QueryTree<Boolean> {
    var nodes = this.graph.nodes.filterIsInstance<T>()

    // filter the nodes according to the selector
    if (sel != null) {
        nodes = nodes.filter(sel)
    }

    val queryChildren = nodes.map(mustSatisfy)
    return QueryTree(queryChildren.any { it.value }, queryChildren.toMutableList())
}

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for at least one node in the graph.
 *
 * The optional argument [sel] can be used to filter nodes which are considered during the
 * evaluation. This filter should be rather simple in most cases since its evaluation is not part of
 * the resulting reasoning chain.
 */
@ExperimentalGraph
inline fun <reified T> Node.exists(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> QueryTree<Boolean>
): QueryTree<Boolean> {
    var nodes = this.astChildren.filterIsInstance<T>()

    // filter the nodes according to the selector
    if (sel != null) {
        nodes = nodes.filter(sel)
    }

    val queryChildren = nodes.map(mustSatisfy)
    return QueryTree(queryChildren.any { it.value }, queryChildren.toMutableList())
}

/**
 * Evaluates the size of a node. The implementation is very, very basic!
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun sizeof(n: Node?, eval: ValueEvaluator = SizeEvaluator()): QueryTree<Number> {
    // The cast could potentially go wrong, but if its not an int, its not really a size
    return QueryTree(eval.evaluate(n) as? Int ?: -1, mutableListOf(QueryTree(n)))
}

/**
 * Retrieves the minimal value of the node.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun min(n: Node?, eval: ValueEvaluator = MultiValueEvaluator()): QueryTree<Number> {
    val evalRes = eval.evaluate(n)
    if (evalRes is Number) {
        return QueryTree(evalRes, mutableListOf(QueryTree(n)))
    }
    // Extend this when we have other evaluators.
    return QueryTree((evalRes as? NumberSet)?.min() ?: -1, mutableListOf(QueryTree(n)))
}

/**
 * Retrieves the minimal value of the nodes in the list.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun min(n: List<Node>?, eval: ValueEvaluator = MultiValueEvaluator()): QueryTree<Number> {
    var result = Long.MAX_VALUE
    if (n == null) return QueryTree(result, mutableListOf(QueryTree(null)))

    for (node in n) {
        val evalRes = eval.evaluate(node)
        if (evalRes is Number && evalRes.toLong() < result) {
            result = evalRes.toLong()
        } else if (evalRes is NumberSet && evalRes.min() < result) {
            result = evalRes.min()
        }
        // Extend this when we have other evaluators.
    }
    return QueryTree(result, mutableListOf(QueryTree(n)))
}

/**
 * Retrieves the maximal value of the nodes in the list.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun max(n: List<Node>?, eval: ValueEvaluator = MultiValueEvaluator()): QueryTree<Number> {
    var result = Long.MIN_VALUE
    if (n == null) return QueryTree(result, mutableListOf(QueryTree(null)))

    for (node in n) {
        val evalRes = eval.evaluate(node)
        if (evalRes is Number && evalRes.toLong() > result) {
            result = evalRes.toLong()
        } else if (evalRes is NumberSet && evalRes.max() > result) {
            result = evalRes.max()
        }
        // Extend this when we have other evaluators.
    }
    return QueryTree(result, mutableListOf(QueryTree(n)))
}

/**
 * Retrieves the maximal value of the node.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun max(n: Node?, eval: ValueEvaluator = MultiValueEvaluator()): QueryTree<Number> {
    val evalRes = eval.evaluate(n)
    if (evalRes is Number) {
        return QueryTree(evalRes, mutableListOf(QueryTree(n)))
    }
    // Extend this when we have other evaluators.
    return QueryTree((evalRes as? NumberSet)?.max() ?: -1, mutableListOf(QueryTree(n)))
}

/** Checks if a data flow is possible between the nodes [from] as a source and [to] as sink. */
fun dataFlow(from: Node, to: Node): QueryTree<Boolean> {
    val evalRes = from.followNextDFGEdgesUntilHit { it == to }
    return QueryTree(evalRes.isNotEmpty(), evalRes.map { QueryTree(it) }.toMutableList())
}

/** Checks if a path of execution flow is possible between the nodes [from] and [to]. */
fun executionPath(from: Node, to: Node): QueryTree<Boolean> {
    val evalRes = from.followNextEOGEdgesUntilHit { it == to }
    return QueryTree(evalRes.isNotEmpty(), evalRes.map { QueryTree(it) }.toMutableList())
}

/** Checks if a path of execution flow is possible between the nodes [from] and [to]. */
fun executionPath(from: Node, predicate: (Node) -> Boolean): QueryTree<Boolean> {
    val evalRes = from.followNextEOGEdgesUntilHit(predicate)
    return QueryTree(evalRes.isNotEmpty(), evalRes.map { QueryTree(it) }.toMutableList())
}

/** Calls [ValueEvaluator.evaluate] for this expression, thus trying to resolve a constant value. */
operator fun Expression?.invoke(): QueryTree<Any?> {
    return QueryTree(this?.evaluate(), mutableListOf(QueryTree(this)))
}

/** The size of this expression. It uses the default argument for `eval` of [size] */
val Expression.size: QueryTree<Number>
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
        return QueryTree(evaluate(), mutableListOf(QueryTree(this)))
    }

/**
 * Calls [ValueEvaluator.evaluate] for this expression, thus trying to resolve a constant value. The
 * result is interpreted as an integer.
 */
val Expression.intValue: QueryTree<Number>?
    get() {
        val evalRes = evaluate() as? Int ?: return null
        return QueryTree(evalRes, mutableListOf(QueryTree(this)))
    }
