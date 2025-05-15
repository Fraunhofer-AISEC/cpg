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

import de.fraunhofer.aisec.cpg.evaluation.MultiValueEvaluator
import de.fraunhofer.aisec.cpg.evaluation.NumberSet
import de.fraunhofer.aisec.cpg.evaluation.SizeEvaluator
import de.fraunhofer.aisec.cpg.evaluation.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.Type

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
    return nodes.map { n ->
        val res = mustSatisfy(n)
        res.stringRepresentation = "Starting at $n: " + res.stringRepresentation
        if (n is Node) {
            res.node = n
        }
        res
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
 * Evaluates the size of a node. The implementation is very, very basic!
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun sizeof(n: Node?, eval: ValueEvaluator = SizeEvaluator()): QueryTree<Int> {
    // The cast could potentially go wrong, but if it's not an int, it's not really a size
    return QueryTree(eval.evaluate(n) as? Int ?: -1, mutableListOf(), "sizeof($n)", n)
}

/**
 * Retrieves the minimal value of the node.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun min(n: Node?, eval: ValueEvaluator = MultiValueEvaluator()): QueryTree<Number> {
    val evalRes = eval.evaluate(n)
    if (evalRes is Number) {
        return QueryTree(evalRes, mutableListOf(QueryTree(n)), "min($n)", n)
    }
    // Extend this when we have other evaluators.
    return QueryTree((evalRes as? NumberSet)?.min() ?: -1, mutableListOf(), "min($n)", n)
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
    return QueryTree(result, mutableListOf(), "min($n)")
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
    return QueryTree(result, mutableListOf(), "max($n)")
}

/**
 * Retrieves the maximal value of the node.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun max(n: Node?, eval: ValueEvaluator = MultiValueEvaluator()): QueryTree<Number> {
    val evalRes = eval.evaluate(n)
    if (evalRes is Number) {
        return QueryTree(evalRes, mutableListOf(QueryTree(n)), node = n)
    }
    // Extend this when we have other evaluators.
    return QueryTree((evalRes as? NumberSet)?.max() ?: -1, mutableListOf(), "max($n)", n)
}

/** Calls [ValueEvaluator.evaluate] for this expression, thus trying to resolve a constant value. */
operator fun Expression?.invoke(): QueryTree<Any?> {
    return QueryTree(this?.evaluate(), mutableListOf(QueryTree(this)), node = this)
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
    return QueryTree(maxVal, mutableListOf(QueryTree(type)), "maxSizeOfType($type)", node = type)
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
    return QueryTree(maxVal, mutableListOf(QueryTree(type)), "minSizeOfType($type)", node = type)
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
        return QueryTree(evaluate(), mutableListOf(), "$this", this)
    }

/**
 * Calls [ValueEvaluator.evaluate] for this expression, thus trying to resolve a constant value. The
 * result is interpreted as an integer.
 */
val Expression.intValue: QueryTree<Int>?
    get() {
        val evalRes = evaluate() as? Int ?: return null
        return QueryTree(evalRes, mutableListOf(), "$this", this)
    }
