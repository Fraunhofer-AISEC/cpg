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
import de.fraunhofer.aisec.cpg.analysis.ConcreteNumberSet
import de.fraunhofer.aisec.cpg.analysis.MultiValueEvaluator
import de.fraunhofer.aisec.cpg.analysis.SizeEvaluator
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.evaluate
import de.fraunhofer.aisec.cpg.graph.graph
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression

@ExperimentalGraph
inline fun <reified T> TranslationResult.all(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> Boolean
): Pair<Boolean, List<Node>> {
    var nodes = this.graph.nodes.filterIsInstance<T>()

    // filter the nodes according to the selector
    if (sel != null) {
        nodes = nodes.filter(sel)
    }

    val failed = nodes.filterNot(mustSatisfy)

    return Pair(failed.isEmpty(), failed as List<Node>)
}

@ExperimentalGraph
inline fun <reified T> Node.all(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> Boolean
): Pair<Boolean, List<Node>> {
    val children = this.astChildren

    var nodes = children.filterIsInstance<T>()

    // filter the nodes according to the selector
    if (sel != null) {
        nodes = nodes.filter(sel)
    }

    val failed = nodes.filterNot(mustSatisfy)

    return Pair(failed.isEmpty(), failed as List<Node>)
}

/** Evaluates the size of a node. The implementation is very very basic! */
fun sizeof(n: Node?): Int {
    val eval = SizeEvaluator()
    // TODO(oxisto): This cast could potentially go wrong, but if its not an int, its not really a
    // size
    return eval.evaluate(n) as? Int ?: -1
}

/**
 * Retrieves the minimal value of the node.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun min(n: Node?, eval: ValueEvaluator = MultiValueEvaluator()): Long {
    val evalRes = eval.evaluate(n)
    if (evalRes is Number) {
        return evalRes.toLong()
    }
    // TODO: Extend this when we have other evaluators.
    return (evalRes as? ConcreteNumberSet)?.min() ?: -1
}

/**
 * Retrieves the minimal value of the nodes in the list.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun min(n: List<Node>?, eval: ValueEvaluator = MultiValueEvaluator()): Long {
    var result = Long.MAX_VALUE
    if (n == null) return result

    for (node in n) {
        val evalRes = eval.evaluate(node)
        if (evalRes is Number && evalRes.toLong() < result) {
            result = evalRes.toLong()
        } else if (evalRes is ConcreteNumberSet && evalRes.min() < result) {
            result = evalRes.min()
        }
    }
    return result
}

/**
 * Retrieves the maximal value of the nodes in the list.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun max(n: List<Node>?, eval: ValueEvaluator = MultiValueEvaluator()): Long {
    var result = Long.MIN_VALUE
    if (n == null) return result

    for (node in n) {
        val evalRes = eval.evaluate(node)
        if (evalRes is Number && evalRes.toLong() > result) {
            result = evalRes.toLong()
        } else if (evalRes is ConcreteNumberSet && evalRes.max() > result) {
            result = evalRes.max()
        }
    }
    return result
}

/**
 * Retrieves the minimal value of the node.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun max(n: Node?, eval: ValueEvaluator = MultiValueEvaluator()): Long {
    val evalRes = eval.evaluate(n)
    if (evalRes is Number) {
        return evalRes.toLong()
    }
    // TODO: Extend this when we have other evaluators.
    return (evalRes as? ConcreteNumberSet)?.max() ?: -1
}

/**
 * This is a small wrapper to create a [QueryResult] containing a constant value, so that it can be
 * used to in comparison with other [QueryResult] objects.
 */
fun const(n: Int): QueryResult {
    return QueryResult(n)
}

operator fun Expression?.invoke(): QueryResult {
    return QueryResult(this?.evaluate())
}

class QueryResult(val inner: Any? = null) {
    operator fun compareTo(o: QueryResult): Int {
        return if (this.inner is Int && o.inner is Int) {
            // for now assume that its also an int (which is not always the case of course)
            this.inner - o.inner
        } else {
            -1
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is QueryResult) {
            return this.inner?.equals(other.inner) ?: false
        }

        return super.equals(other)
    }

    override fun toString(): String {
        return inner.toString()
    }

    override fun hashCode(): Int {
        return inner?.hashCode() ?: 0
    }
}

val Expression.size: QueryResult
    get() {
        return QueryResult(sizeof(this))
    }

val Expression.min: QueryResult
    get() {
        return QueryResult(min(this))
    }

val Expression.max: QueryResult
    get() {
        return QueryResult(max(this))
    }

val Expression.value: QueryResult
    get() {
        return QueryResult(evaluate())
    }

val Expression.intValue: Int?
    get() {
        return evaluate() as? Int
    }
