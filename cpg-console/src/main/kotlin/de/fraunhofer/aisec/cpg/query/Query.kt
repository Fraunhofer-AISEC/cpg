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

/** Evaluates the size of a node. The implementation is very very basic! */
fun sizeof(n: Node?): QueryTree<Number> {
    val eval = SizeEvaluator()
    // TODO(oxisto): This cast could potentially go wrong, but if its not an int, its not really a
    // size
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
 * Retrieves the minimal value of the node.
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

operator fun Expression?.invoke(): QueryTree<Any?> {
    return QueryTree(this?.evaluate(), mutableListOf(QueryTree(this)))
}

val Expression.size: QueryTree<Number>
    get() {
        return sizeof(this)
    }

val Expression.min: QueryTree<Number>
    get() {
        return min(this)
    }

val Expression.max: QueryTree<Number>
    get() {
        return max(this)
    }

val Expression.value: QueryTree<Any?>
    get() {
        return QueryTree(evaluate(), mutableListOf(QueryTree(this)))
    }

val Expression.intValue: QueryTree<Number>?
    get() {
        val evalRes = evaluate() as? Int ?: return null
        return QueryTree(evalRes, mutableListOf(QueryTree(this)))
    }
