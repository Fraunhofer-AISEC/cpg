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
package de.fraunhofer.aisec.cpg.query.simple

import de.fraunhofer.aisec.cpg.ExperimentalGraph
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.analysis.MultiValueEvaluator
import de.fraunhofer.aisec.cpg.analysis.SizeEvaluator
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for all nodes in the graph. The
 * optional argument [sel] can be used to filter nodes for which the condition has to be fulfilled.
 *
 * This method can be used similar to the logical implication to test "sel => mustSatisfy".
 */
@ExperimentalGraph
inline fun <reified T : Node> TranslationResult.all(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> Boolean
): Pair<Boolean, List<Node>> {
    var nodes = this.graph.nodes.filterIsInstance<T>()

    // filter the nodes according to the selector
    if (sel != null) {
        nodes = nodes.filter(sel)
    }

    val failedNodes = nodes.filterNot(mustSatisfy)
    return Pair(failedNodes.isEmpty(), failedNodes)
}

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for all nodes in the graph. The
 * optional argument [sel] can be used to filter nodes for which the condition has to be fulfilled.
 *
 * This method can be used similar to the logical implication to test "sel => mustSatisfy".
 */
@ExperimentalGraph
inline fun <reified T : Node> Node.all(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> Boolean
): Pair<Boolean, List<Node>> {
    val children = this.astChildren

    var nodes = children.filterIsInstance<T>()

    // filter the nodes according to the selector
    if (sel != null) {
        nodes = nodes.filter(sel)
    }

    val failedNodes = nodes.filterNot(mustSatisfy)
    return Pair(failedNodes.isEmpty(), failedNodes)
}

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for at least one node in the graph.
 * The optional argument [sel] can be used to filter nodes which are considered during the
 * evaluation.
 */
@ExperimentalGraph
inline fun <reified T> TranslationResult.exists(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> Boolean
): Boolean {
    var nodes = this.graph.nodes.filterIsInstance<T>()

    // filter the nodes according to the selector
    if (sel != null) {
        nodes = nodes.filter(sel)
    }

    val queryChildren = nodes.map(mustSatisfy)
    return queryChildren.any { it }
}

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for at least one node in the graph.
 * The optional argument [sel] can be used to filter nodes which are considered during the
 * evaluation.
 */
@ExperimentalGraph
inline fun <reified T> Node.exists(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> Boolean
): Boolean {
    var nodes = this.astChildren.filterIsInstance<T>()

    // filter the nodes according to the selector
    if (sel != null) {
        nodes = nodes.filter(sel)
    }

    val queryChildren = nodes.map(mustSatisfy)
    return queryChildren.any { it }
}

/**
 * Evaluates the size of a node. The implementation is very, very basic!
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun sizeof(n: Node?, eval: ValueEvaluator = SizeEvaluator()): Int =
    de.fraunhofer.aisec.cpg.query.sizeof(n, eval).value

/**
 * Retrieves the minimal value of the node.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun min(n: Node?, eval: ValueEvaluator = MultiValueEvaluator()): Number =
    de.fraunhofer.aisec.cpg.query.min(n, eval).value

/**
 * Retrieves the minimal value of the nodes in the list.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun min(n: List<Node>?, eval: ValueEvaluator = MultiValueEvaluator()): Number =
    de.fraunhofer.aisec.cpg.query.min(n, eval).value

/**
 * Retrieves the maximal value of the nodes in the list.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun max(n: List<Node>?, eval: ValueEvaluator = MultiValueEvaluator()): Number =
    de.fraunhofer.aisec.cpg.query.max(n, eval).value

/**
 * Retrieves the maximal value of the node.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun max(n: Node?, eval: ValueEvaluator = MultiValueEvaluator()): Number =
    de.fraunhofer.aisec.cpg.query.max(n, eval).value

/** Checks if a data flow is possible between the nodes [from] as a source and [to] as sink. */
fun dataFlow(from: Node, to: Node): Boolean = de.fraunhofer.aisec.cpg.query.dataFlow(from, to).value

/** Checks if a path of execution flow is possible between the nodes [from] and [to]. */
fun executionPath(from: Node, to: Node): Boolean =
    de.fraunhofer.aisec.cpg.query.executionPath(from, to).value

/** Checks if a path of execution flow is possible between the nodes [from] and [to]. */
fun executionPath(from: Node, predicate: (Node) -> Boolean): Boolean =
    de.fraunhofer.aisec.cpg.query.executionPath(from, predicate).value

/** Calls [ValueEvaluator.evaluate] for this expression, thus trying to resolve a constant value. */
operator fun Expression?.invoke(): Any? = this?.evaluate()

/** The size of this expression. It uses the default argument for `eval` of [size] */
val Expression.size: Number
    get() {
        return sizeof(this)
    }

/**
 * The minimal integer value of this expression. It uses the default argument for `eval` of [min]
 */
val Expression.min: Number
    get() {
        return min(this)
    }

/**
 * The maximal integer value of this expression. It uses the default argument for `eval` of [max]
 */
val Expression.max: Number
    get() {
        return max(this)
    }

/** Calls [ValueEvaluator.evaluate] for this expression, thus trying to resolve a constant value. */
val Expression.value: Any?
    get() {
        return evaluate()
    }

/**
 * Calls [ValueEvaluator.evaluate] for this expression, thus trying to resolve a constant value. The
 * result is interpreted as an integer.
 */
val Expression.intValue: Number?
    get() {
        return evaluate() as? Int ?: return null
    }
