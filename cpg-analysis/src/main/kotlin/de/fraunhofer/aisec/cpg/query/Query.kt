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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
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
    val queryChildren = evaluateExtended(sel, mustSatisfy)
    return QueryTree(queryChildren.all { it.value }, queryChildren.toMutableList(), "all", this)
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
    val queryChildren = evaluateExtended(sel, mustSatisfy)
    return QueryTree(queryChildren.any { it.value }, queryChildren.toMutableList(), "exists", this)
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

/** Determines in which direction we follow the edges. */
enum class AnalysisScope {
    /** Only intraprocedural analysis */
    INTRAPROCEDURAL,
    /** Enable interprocedural analysis */
    INTERPROCEDURAL,
}

/** Determines in which direction we follow the edges. */
enum class AnalysisDirection {
    /** Follow the order of the EOG */
    FORWARD,
    /** Against the order of the EOG */
    BACKWARD,
}

/** Determines if the predicate must or may hold */
enum class AnalysisType {
    /**
     * The predicate must hold, i.e., all paths fulfill the property/requirement. No path violates
     * the property/requirement.
     */
    MUST,
    /**
     * The predicate may hold, i.e., there is at least one path which fulfills the
     * property/requirement.
     */
    MAY,
}

typealias AnalysisSensitivities = List<AnalysisSensitivity>

/** Configures the sensitivity of the analysis. */
enum class AnalysisSensitivity {
    /** Consider the calling context when following paths (e.g. based on a call stack). */
    CONTEXT_SENSITIVE,

    /**
     * Differentiate between fields, attributes, known keys or known indices of objects. This does
     * not include computing possible indices or keys if they are not given as a literal.
     */
    FIELD_SENSITIVE;

    operator fun plus(other: AnalysisSensitivity): AnalysisSensitivities {
        return listOf(this, other)
    }

    operator fun List<AnalysisSensitivity>.plus(other: AnalysisSensitivity): AnalysisSensitivities {
        return listOf(*this.toTypedArray(), other)
    }

    operator fun plus(other: AnalysisSensitivities): AnalysisSensitivities {
        return listOf(*other.toTypedArray(), this)
    }
}

private fun dataFlowBase(
    startNode: Node,
    predicate: (Node) -> Boolean,
    direction: AnalysisDirection,
    type: AnalysisType,
    sensitivities: AnalysisSensitivities,
    scope: AnalysisScope,
    verbose: Boolean = true,
): QueryTree<Boolean> {
    val collectFailedPaths = type == AnalysisType.MUST || verbose
    val findAllPossiblePaths = type == AnalysisType.MUST || verbose
    val useIndexStack = AnalysisSensitivity.FIELD_SENSITIVE in sensitivities
    val contextSensitive = AnalysisSensitivity.CONTEXT_SENSITIVE in sensitivities
    val interproceduralAnalysis = scope == AnalysisScope.INTERPROCEDURAL
    val evalRes =
        when (direction) {
            AnalysisDirection.FORWARD -> {
                startNode.followNextDFGEdgesUntilHit(
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    useIndexStack = useIndexStack,
                    contextSensitive = contextSensitive,
                    interproceduralAnalysis = interproceduralAnalysis,
                    predicate = predicate,
                )
            }
            AnalysisDirection.BACKWARD -> {
                startNode.followPrevDFGEdgesUntilHit(
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    useIndexStack = useIndexStack,
                    contextSensitive = contextSensitive,
                    interproceduralAnalysis = interproceduralAnalysis,
                    predicate = predicate,
                )
            }
        }
    val allPaths =
        evalRes.fulfilled
            .map {
                QueryTree(
                    true,
                    mutableListOf(QueryTree(it)),
                    "data flow from $startNode to ${it.last()} fulfills the requirement",
                    startNode,
                )
            }
            .toMutableList()
    if (type == AnalysisType.MUST || verbose)
        allPaths +=
            evalRes.failed.map {
                QueryTree(
                    false,
                    mutableListOf(QueryTree(it)),
                    "data flow from $startNode to ${it.last()} does not fulfill the requirement",
                    startNode,
                )
            }

    return when (type) {
        AnalysisType.MUST ->
            QueryTree(
                evalRes.failed.isEmpty(),
                allPaths.toMutableList(),
                "data flow from $startNode to ${evalRes.fulfilled.map { it.last() }}",
                startNode,
            )
        AnalysisType.MAY ->
            QueryTree(
                evalRes.fulfilled.isNotEmpty(),
                allPaths.toMutableList(),
                "data flow from $startNode to ${evalRes.fulfilled.map { it.last() }}",
                startNode,
            )
    }
}

private fun executionPathBase(
    startNode: Node,
    predicate: (Node) -> Boolean,
    direction: AnalysisDirection,
    type: AnalysisType,
    scope: AnalysisScope,
    verbose: Boolean = true,
): QueryTree<Boolean> {
    val collectFailedPaths = type == AnalysisType.MUST || verbose
    val findAllPossiblePaths = type == AnalysisType.MUST || verbose
    val interproceduralAnalysis = scope == AnalysisScope.INTERPROCEDURAL
    val evalRes =
        when (direction) {
            AnalysisDirection.FORWARD -> {
                startNode.followNextEOGEdgesUntilHit(
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    interproceduralAnalysis = interproceduralAnalysis,
                    predicate = predicate,
                )
            }
            AnalysisDirection.BACKWARD -> {
                startNode.followPrevEOGEdgesUntilHit(
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    interproceduralAnalysis = interproceduralAnalysis,
                    predicate = predicate,
                )
            }
        }
    val allPaths =
        evalRes.fulfilled
            .map {
                QueryTree(
                    true,
                    mutableListOf(QueryTree(it)),
                    "execution path from $startNode to ${it.last()} fulfills the requirement",
                    startNode,
                )
            }
            .toMutableList()
    if (type == AnalysisType.MUST || verbose)
        allPaths +=
            evalRes.failed.map {
                QueryTree(
                    false,
                    mutableListOf(QueryTree(it)),
                    "execution path from $startNode to ${it.last()} does not fulfill the requirement",
                    startNode,
                )
            }

    return when (type) {
        AnalysisType.MUST ->
            QueryTree(
                evalRes.failed.isEmpty(),
                allPaths.toMutableList(),
                "execution path from $startNode to ${evalRes.fulfilled.map { it.last() }}",
                startNode,
            )
        AnalysisType.MAY ->
            QueryTree(
                evalRes.fulfilled.isNotEmpty(),
                allPaths.toMutableList(),
                "execution path from $startNode to ${evalRes.fulfilled.map { it.last() }}",
                startNode,
            )
    }
}

/** Checks if a data flow is possible between the nodes [from] as a source and [to] as sink. */
fun dataFlow(
    from: Node,
    to: Node,
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
) =
    dataFlowBase(
        startNode = from,
        { it == to },
        direction = AnalysisDirection.FORWARD,
        type = AnalysisType.MAY,
        sensitivities = AnalysisSensitivity.FIELD_SENSITIVE + AnalysisSensitivity.CONTEXT_SENSITIVE,
        scope = AnalysisScope.INTERPROCEDURAL,
        verbose = collectFailedPaths || findAllPossiblePaths,
    )

/**
 * Checks if a data flow is possible between the nodes [from] as a source and a node fulfilling
 * [predicate].
 */
fun dataFlow(
    from: Node,
    predicate: (Node) -> Boolean,
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
) =
    dataFlowBase(
        startNode = from,
        predicate = predicate,
        direction = AnalysisDirection.FORWARD,
        type = AnalysisType.MAY,
        sensitivities = AnalysisSensitivity.FIELD_SENSITIVE + AnalysisSensitivity.CONTEXT_SENSITIVE,
        scope = AnalysisScope.INTERPROCEDURAL,
        verbose = collectFailedPaths || findAllPossiblePaths,
    )

/** Checks if a path of execution flow is possible between the nodes [from] and [to]. */
fun executionPath(from: Node, to: Node) =
    executionPathBase(
        startNode = from,
        predicate = { it == to },
        direction = AnalysisDirection.FORWARD,
        type = AnalysisType.MAY,
        scope = AnalysisScope.INTERPROCEDURAL,
        verbose = true,
    )

/**
 * Checks if a path of execution flow is possible starting at the node [from] and fulfilling the
 * requirement specified in [predicate].
 */
fun executionPath(from: Node, predicate: (Node) -> Boolean) =
    executionPathBase(
        startNode = from,
        predicate = predicate,
        direction = AnalysisDirection.FORWARD,
        type = AnalysisType.MAY,
        scope = AnalysisScope.INTERPROCEDURAL,
        verbose = true,
    )

/**
 * Checks if a path of execution flow is possible ending at the node [to] and fulfilling the
 * requirement specified in [predicate].
 */
fun executionPathBackwards(to: Node, predicate: (Node) -> Boolean) =
    executionPathBase(
        startNode = to,
        predicate = predicate,
        direction = AnalysisDirection.BACKWARD,
        type = AnalysisType.MAY,
        scope = AnalysisScope.INTERPROCEDURAL,
        verbose = true,
    )

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

fun Node.alwaysFlowsTo(
    predicate: (Node) -> Boolean,
    allowOverwritingValue: Boolean = false,
    scope: AnalysisScope,
    sensitivities: AnalysisSensitivities,
): QueryTree<Boolean> {
    val nextDFGPaths = this.collectAllNextDFGPaths().flatten()
    val executionPaths = this.collectAllNextEOGPaths()
    var result = true
    val children = mutableListOf<QueryTree<Boolean>>()
    for (executionPath in executionPaths) {
        var foundMatch = false
        for ((i, step) in executionPath.withIndex()) {
            // Check if there's a (partial) write to the memory address keeping the data of this
            if (
                !allowOverwritingValue &&
                    this in step.prevDFG &&
                    (step as? Reference)?.access == AccessValues.WRITE
            ) {
                result = false
                children.add(
                    QueryTree(
                        value = false,
                        children = mutableListOf(QueryTree(executionPath.subList(0, i))),
                        stringRepresentation =
                            "Node $step overwrites (some) data of $this before reaching a node matching the predicate",
                        node = this,
                    )
                )
                foundMatch = true
                break
            }
            // Check if the node matches the predicate and there's a data flow path
            if (predicate(step) && step in nextDFGPaths) {
                children.add(
                    QueryTree(
                        value = true,
                        children = mutableListOf(QueryTree(executionPath.subList(0, i))),
                        stringRepresentation =
                            "Node $step fulfills the predicate is reachable from $this",
                        node = this,
                    )
                )
                foundMatch = true
                break
            }
        }
        if (foundMatch == false) {
            result = false
            children.add(
                QueryTree(
                    value = false,
                    children = mutableListOf(QueryTree(executionPath)),
                    stringRepresentation =
                        "Reached the end of the EOG reachable from $this without fulfilling the predicate",
                    node = this,
                )
            )
        }
    }
    return QueryTree(
        result,
        children.toMutableList(),
        stringRepresentation =
            if (result) {
                "All EOG paths fulfilled the predicate"
            } else {
                "Some EOG paths failed to fulfill the predicate"
            },
        node = this,
    )
    return executionPathBase(
        this,
        predicate = { predicate(it) && it in nextDFGPaths },
        direction = AnalysisDirection.FORWARD,
        type = AnalysisType.MUST,
        scope = AnalysisScope.INTERPROCEDURAL,
    )
}

/**
 * Aims to identify if the value which is in [this] reaches a node fulfilling the [predicate] on all
 * execution paths. To do so, it goes some data flow steps backwards in the graph, ideally to find
 * the last assignment but potentially also splits the value up into different parts (e.g. think of
 * a `+` operation where we follow the lhs and the rhs) and then follows this value until the
 * [predicate] is fulfilled on all execution paths. Note: Constant values (literals) are not
 * followed if [followLiterals] is set to `false`.
 */
fun Node.allNonLiteralsFlowTo(
    predicate: (Node) -> Boolean,
    allowOverwritingValue: Boolean = false,
    scope: AnalysisScope,
    sensitivities: AnalysisSensitivities,
    followLiterals: Boolean = false,
): QueryTree<Boolean> {
    val worklist = mutableListOf<Node>(this)
    val finalPathsChecked = mutableListOf<QueryTree<Boolean>>()
    while (worklist.isNotEmpty()) {
        val currentNode = worklist.removeFirst()
        if (currentNode.prevDFG.isEmpty()) {
            finalPathsChecked +=
                currentNode.alwaysFlowsTo(
                    predicate = predicate,
                    allowOverwritingValue = allowOverwritingValue,
                    scope = scope,
                    sensitivities = sensitivities,
                )
        }
        currentNode.prevDFG
            .filter { followLiterals || it !is Literal<*> }
            .forEach {
                val pathResult =
                    it.alwaysFlowsTo(
                        predicate = predicate,
                        allowOverwritingValue = allowOverwritingValue,
                        scope = scope,
                        sensitivities = sensitivities,
                    )
                if (pathResult.value) {
                    // This path always ends pathResult in a node fulfilling the predicate
                    finalPathsChecked.add(pathResult)
                } else {
                    // This path contained some nodes which do not end up in a node fulfilling the
                    // predicate
                    worklist.add(it)
                }
            }
    }

    return QueryTree(
        finalPathsChecked.all { it.value },
        finalPathsChecked.toMutableList(),
        node = this,
    )
}
