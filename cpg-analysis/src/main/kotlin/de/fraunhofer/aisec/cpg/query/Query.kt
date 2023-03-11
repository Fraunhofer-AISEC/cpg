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

import de.fraunhofer.aisec.cpg.analysis.MultiValueEvaluator
import de.fraunhofer.aisec.cpg.analysis.NumberSet
import de.fraunhofer.aisec.cpg.analysis.SizeEvaluator
import de.fraunhofer.aisec.cpg.analysis.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
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
    noinline mustSatisfy: (T) -> QueryTree<Boolean>
): QueryTree<Boolean> {
    val nodes = this.allChildren(sel)

    val queryChildren =
        nodes.map { n ->
            val res = mustSatisfy(n)
            res.stringRepresentation = "Starting at $n: " + res.stringRepresentation
            res
        }
    return QueryTree(queryChildren.all { it.value }, queryChildren.toMutableList(), "all")
}

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for all nodes in the graph. The
 * optional argument [sel] can be used to filter nodes for which the condition has to be fulfilled.
 *
 * This method can be used similar to the logical implication to test "sel => mustSatisfy".
 */
inline fun <reified T> Node.all(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> Boolean
): Pair<Boolean, List<Node>> {
    val nodes = this.allChildren(sel)

    val failedNodes = nodes.filterNot(mustSatisfy) as List<Node>
    return Pair(failedNodes.isEmpty(), failedNodes)
}

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for at least one node in the graph.
 *
 * The optional argument [sel] can be used to filter nodes which are considered during the
 * evaluation. This filter should be rather simple in most cases since its evaluation is not part of
 * the resulting reasoning chain.
 */
inline fun <reified T : Node> Node.existsExtended(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> QueryTree<Boolean>
): QueryTree<Boolean> {
    val nodes = this.allChildren(sel)

    val queryChildren =
        nodes.map { n ->
            val res = mustSatisfy(n)
            res.stringRepresentation = "Starting at $n: " + res.stringRepresentation
            res
        }
    return QueryTree(queryChildren.any { it.value }, queryChildren.toMutableList(), "exists")
}

/**
 * Evaluates if the conditions specified in [mustSatisfy] hold for at least one node in the graph.
 * The optional argument [sel] can be used to filter nodes which are considered during the
 * evaluation.
 */
inline fun <reified T : Node> Node.exists(
    noinline sel: ((T) -> Boolean)? = null,
    noinline mustSatisfy: (T) -> Boolean
): Pair<Boolean, List<Node>> {
    val nodes = this.allChildren(sel)

    val queryChildren = nodes.filter(mustSatisfy) as List<Node>
    return Pair(queryChildren.isNotEmpty(), queryChildren)
}

/**
 * Evaluates the size of a node. The implementation is very, very basic!
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun sizeof(n: Node?, eval: ValueEvaluator = SizeEvaluator()): QueryTree<Int> {
    // The cast could potentially go wrong, but if it's not an int, it's not really a size
    return QueryTree(eval.evaluate(n) as? Int ?: -1, mutableListOf(), "sizeof($n)")
}

/**
 * Retrieves the minimal value of the node.
 *
 * @eval can be used to specify the evaluator but this method has to interpret the result correctly!
 */
fun min(n: Node?, eval: ValueEvaluator = MultiValueEvaluator()): QueryTree<Number> {
    val evalRes = eval.evaluate(n)
    if (evalRes is Number) {
        return QueryTree(evalRes, mutableListOf(QueryTree(n)), "min($n)")
    }
    // Extend this when we have other evaluators.
    return QueryTree((evalRes as? NumberSet)?.min() ?: -1, mutableListOf(), "min($n)")
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
        return QueryTree(evalRes, mutableListOf(QueryTree(n)))
    }
    // Extend this when we have other evaluators.
    return QueryTree((evalRes as? NumberSet)?.max() ?: -1, mutableListOf(), "max($n)")
}

/** Checks if a data flow is possible between the nodes [from] as a source and [to] as sink. */
fun dataFlow(from: Node, to: Node): QueryTree<Boolean> {
    val evalRes = from.followNextDFGEdgesUntilHit { it == to }
    val allPaths = evalRes.fulfilled.map { QueryTree(it) }.toMutableList()
    allPaths.addAll(evalRes.failed.map { QueryTree(it) })
    return QueryTree(
        evalRes.fulfilled.isNotEmpty(),
        allPaths.toMutableList(),
        "data flow from $from to $to"
    )
}

/** Checks if a path of execution flow is possible between the nodes [from] and [to]. */
fun executionPath(from: Node, to: Node): QueryTree<Boolean> {
    val evalRes = from.followNextEOGEdgesUntilHit { it == to }
    val allPaths = evalRes.fulfilled.map { QueryTree(it) }.toMutableList()
    allPaths.addAll(evalRes.failed.map { QueryTree(it) })
    return QueryTree(
        evalRes.fulfilled.isNotEmpty(),
        allPaths.toMutableList(),
        "executionPath($from, $to)"
    )
}

/**
 * Checks if a path of execution flow is possible starting at the node [from] and fulfilling the
 * requirement specified in [predicate].
 */
fun executionPath(from: Node, predicate: (Node) -> Boolean): QueryTree<Boolean> {
    val evalRes = from.followNextEOGEdgesUntilHit(predicate)
    val allPaths = evalRes.fulfilled.map { QueryTree(it) }.toMutableList()
    allPaths.addAll(evalRes.failed.map { QueryTree(it) })
    return QueryTree(
        evalRes.fulfilled.isNotEmpty(),
        allPaths.toMutableList(),
        "executionPath($from, $predicate)"
    )
}

/**
 * Checks if a path of execution flow is possible ending at the node [to] and fulfilling the
 * requirement specified in [predicate].
 */
fun executionPathBackwards(to: Node, predicate: (Node) -> Boolean): QueryTree<Boolean> {
    val evalRes = to.followPrevEOGEdgesUntilHit(predicate)
    val allPaths = evalRes.fulfilled.map { QueryTree(it) }.toMutableList()
    allPaths.addAll(evalRes.failed.map { QueryTree(it) })
    return QueryTree(
        evalRes.fulfilled.isNotEmpty(),
        allPaths.toMutableList(),
        "executionPathBackwards($to, $predicate)"
    )
}

/** Calls [ValueEvaluator.evaluate] for this expression, thus trying to resolve a constant value. */
operator fun Expression?.invoke(): QueryTree<Any?> {
    return QueryTree(this?.evaluate(), mutableListOf(QueryTree(this)))
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
    return QueryTree(maxVal, mutableListOf(QueryTree(type)), "maxSizeOfType($type)")
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
    return QueryTree(maxVal, mutableListOf(QueryTree(type)), "minSizeOfType($type)")
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
        return QueryTree(evaluate(), mutableListOf(), "$this")
    }

/**
 * Calls [ValueEvaluator.evaluate] for this expression, thus trying to resolve a constant value. The
 * result is interpreted as an integer.
 */
val Expression.intValue: QueryTree<Int>?
    get() {
        val evalRes = evaluate() as? Int ?: return null
        return QueryTree(evalRes, mutableListOf(), "$this")
    }

/**
 * Does some magic to identify if the value which is in [from] also reaches [to]. To do so, it goes
 * some data flow steps backwards in the graph (ideally to find the last assignment) and then
 * follows this value to the node [to].
 */
fun allNonLiteralsFromFlowTo(from: Node, to: Node, allPaths: List<List<Node>>): QueryTree<Boolean> {
    return when (from) {
        is CallExpression -> {
            val prevEdges =
                from.prevDFG
                    .fold(mutableListOf<Node>()) { l, e ->
                        if (e !is Literal<*>) {
                            l.add(e)
                        }
                        l
                    }
                    .toMutableList()
            prevEdges.addAll(from.arguments)
            // For a call, we collect the incoming data flows (typically only the arguments)
            val prevQTs = prevEdges.map { allNonLiteralsFromFlowTo(it, to, allPaths) }
            QueryTree(prevQTs.all { it.value }, prevQTs.toMutableList())
        }
        is Literal<*> ->
            QueryTree(true, mutableListOf(QueryTree(from)), "DF Irrelevant for Literal node")
        else -> {
            // We go one step back to see if that one goes into to but also check that no assignment
            // to from happens in the paths between from and to
            val prevQTs = from.prevDFG.map { dataFlow(it, to) }
            val noAssignmentToFrom =
                allPaths.none {
                    it.any { it2 ->
                        if (it2 is AssignmentHolder) {
                            it2.assignments.any { assign ->
                                val prevMemberFrom = (from as? MemberExpression)?.prevDFG
                                val nextMemberTo = (assign.target as? MemberExpression)?.nextDFG
                                assign.target == from ||
                                    prevMemberFrom != null &&
                                        nextMemberTo != null &&
                                        prevMemberFrom.any { it3 -> nextMemberTo.contains(it3) }
                            }
                        } else {
                            false
                        }
                    }
                }
            QueryTree(prevQTs.all { it.value } && noAssignmentToFrom, prevQTs.toMutableList())
        }
    }
}
