/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference

/**
 * Determines how far we want to follow edges within the analysis. [maxSteps] defines the total
 * number of steps we will follow in the graph (unlimited depth if `null`).
 */
sealed class AnalysisScope(val maxSteps: Int? = null)

/**
 * Only intraprocedural analysis. [maxSteps] defines the total number of steps we will follow in the
 * graph (unlimited depth if `null`).
 */
class INTRAPROCEDURAL(maxSteps: Int? = null) : AnalysisScope(maxSteps)

/**
 * Enable interprocedural analysis. [maxCallDepth] defines how many function calls we follow at most
 * (unlimited depth if `null`). [maxSteps] defines the total number of steps we will follow in the
 * graph (unlimited depth if `null`).
 */
class INTERPROCEDURAL(val maxCallDepth: Int? = null, maxSteps: Int? = null) :
    AnalysisScope(maxSteps)

/** Determines in which direction we follow the edges. */
enum class AnalysisDirection {
    /** Follow the order of the EOG */
    FORWARD,
    /** Against the order of the EOG */
    BACKWARD,
    /** In and against the order of the EOG */
    BIDIRECTIONAL,
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

/** Configures the sensitivity of the analysis. */
enum class AnalysisSensitivity {
    /** Consider the calling context when following paths (e.g. based on a call stack). */
    CONTEXT_SENSITIVE,

    /**
     * Differentiate between fields, attributes, known keys or known indices of objects. This does
     * not include computing possible indices or keys if they are not given as a literal.
     */
    FIELD_SENSITIVE,

    /**
     * Also consider implicit flows during the dataflow analysis. E.g. if a condition depends on the
     * value we're interested in, different behaviors in the branches can leak data and thus, the
     * dependencies of this should also be flagged.
     */
    IMPLICIT;

    operator fun plus(other: AnalysisSensitivity) = arrayOf(this, other)

    operator fun List<AnalysisSensitivity>.plus(
        other: AnalysisSensitivity
    ): Array<AnalysisSensitivity> {
        return arrayOf(*this.toTypedArray(), other)
    }

    operator fun plus(other: Array<AnalysisSensitivity>): Array<AnalysisSensitivity> {
        return arrayOf(*other, this)
    }
}

fun dataFlowBase(
    startNode: Node,
    direction: AnalysisDirection,
    type: AnalysisType,
    vararg sensitivities: AnalysisSensitivity,
    scope: AnalysisScope,
    verbose: Boolean = true,
    earlyTermination: ((Node) -> Boolean)? = null,
    predicate: (Node) -> Boolean,
): QueryTree<Boolean> {
    val collectFailedPaths = type == AnalysisType.MUST || verbose
    val findAllPossiblePaths = type == AnalysisType.MUST || verbose
    val useIndexStack = AnalysisSensitivity.FIELD_SENSITIVE in sensitivities
    val contextSensitive = AnalysisSensitivity.CONTEXT_SENSITIVE in sensitivities
    val interproceduralAnalysis = scope is INTERPROCEDURAL
    val earlyTermination = { n: Node, ctx: Context ->
        earlyTermination?.let { it(n) } == true || scope.maxSteps?.let { ctx.steps >= it } == true
    }
    val evalRes =
        when (direction) {
            AnalysisDirection.FORWARD -> {
                startNode.followNextDFGEdgesUntilHit(
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    useIndexStack = useIndexStack,
                    contextSensitive = contextSensitive,
                    interproceduralAnalysis = interproceduralAnalysis,
                    earlyTermination = earlyTermination,
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
                    earlyTermination = earlyTermination,
                    predicate = predicate,
                )
            }
            AnalysisDirection.BIDIRECTIONAL -> {
                startNode.followNextDFGEdgesUntilHit(
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    useIndexStack = useIndexStack,
                    contextSensitive = contextSensitive,
                    interproceduralAnalysis = interproceduralAnalysis,
                    earlyTermination = earlyTermination,
                    predicate = predicate,
                ) +
                    startNode.followPrevDFGEdgesUntilHit(
                        collectFailedPaths = collectFailedPaths,
                        findAllPossiblePaths = findAllPossiblePaths,
                        useIndexStack = useIndexStack,
                        contextSensitive = contextSensitive,
                        interproceduralAnalysis = interproceduralAnalysis,
                        earlyTermination = earlyTermination,
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

fun executionPathBase(
    startNode: Node,
    direction: AnalysisDirection,
    type: AnalysisType,
    scope: AnalysisScope,
    verbose: Boolean = true,
    earlyTermination: ((Node) -> Boolean)? = null,
    predicate: (Node) -> Boolean,
): QueryTree<Boolean> {
    val collectFailedPaths = type == AnalysisType.MUST || verbose
    val findAllPossiblePaths = type == AnalysisType.MUST || verbose
    val interproceduralAnalysis = scope is INTERPROCEDURAL
    val earlyTermination = { n: Node, ctx: Context ->
        earlyTermination?.let { it(n) } == true || scope.maxSteps?.let { ctx.steps >= it } == true
    }
    val evalRes =
        when (direction) {
            AnalysisDirection.FORWARD -> {
                startNode.followNextEOGEdgesUntilHit(
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    interproceduralAnalysis = interproceduralAnalysis,
                    earlyTermination = earlyTermination,
                    predicate = predicate,
                )
            }
            AnalysisDirection.BACKWARD -> {
                startNode.followPrevEOGEdgesUntilHit(
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    interproceduralAnalysis = interproceduralAnalysis,
                    earlyTermination = earlyTermination,
                    predicate = predicate,
                )
            }
            AnalysisDirection.BIDIRECTIONAL -> {
                startNode.followNextEOGEdgesUntilHit(
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    interproceduralAnalysis = interproceduralAnalysis,
                    earlyTermination = earlyTermination,
                    predicate = predicate,
                ) +
                    startNode.followPrevEOGEdgesUntilHit(
                        collectFailedPaths = collectFailedPaths,
                        findAllPossiblePaths = findAllPossiblePaths,
                        interproceduralAnalysis = interproceduralAnalysis,
                        earlyTermination = earlyTermination,
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

fun dataFlowWithValidator(
    source: Node,
    validatorPredicate: (Node) -> Boolean,
    sinkPredicate: (Node) -> Boolean,
    scope: AnalysisScope,
    vararg sensitivities: AnalysisSensitivity,
): QueryTree<Boolean> {
    return source.alwaysFlowsTo(
        allowOverwritingValue = true,
        earlyTermination = sinkPredicate,
        scope = scope,
        sensitivities = sensitivities,
        predicate = validatorPredicate,
    )
}

/** Checks if a data flow is possible between the nodes [source] and [sink]. */
fun dataFlow(
    source: Node,
    sink: Node,
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
) =
    dataFlowBase(
        startNode = source,
        direction = AnalysisDirection.FORWARD,
        type = AnalysisType.MAY,
        sensitivities = AnalysisSensitivity.FIELD_SENSITIVE + AnalysisSensitivity.CONTEXT_SENSITIVE,
        scope = INTERPROCEDURAL(),
        verbose = collectFailedPaths || findAllPossiblePaths,
        predicate = { it == sink },
    )

/** Checks if a data flow is possible between the [source] and a sink fulfilling [predicate]. */
fun dataFlow(
    source: Node,
    predicate: (Node) -> Boolean,
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
) =
    dataFlowBase(
        startNode = source,
        direction = AnalysisDirection.FORWARD,
        type = AnalysisType.MAY,
        sensitivities = AnalysisSensitivity.FIELD_SENSITIVE + AnalysisSensitivity.CONTEXT_SENSITIVE,
        scope = INTERPROCEDURAL(),
        verbose = collectFailedPaths || findAllPossiblePaths,
        predicate = predicate,
    )

/** Checks if a path of execution flow is possible between the nodes [from] and [to]. */
fun executionPath(from: Node, to: Node) =
    executionPathBase(
        startNode = from,
        predicate = { it == to },
        direction = AnalysisDirection.FORWARD,
        type = AnalysisType.MAY,
        scope = INTERPROCEDURAL(),
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
        scope = INTERPROCEDURAL(),
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
        scope = INTERPROCEDURAL(),
        verbose = true,
    )

fun Node.alwaysFlowsTo(
    allowOverwritingValue: Boolean = false,
    earlyTermination: ((Node) -> Boolean)? = null,
    scope: AnalysisScope,
    vararg sensitivities: AnalysisSensitivity,
    predicate: (Node) -> Boolean,
): QueryTree<Boolean> {
    val nextDFGPaths =
        this.collectAllNextDFGPaths(
                interproceduralAnalysis = scope is INTERPROCEDURAL,
                contextSensitive = AnalysisSensitivity.CONTEXT_SENSITIVE in sensitivities,
            )
            .flatten()
    val earlyTerminationPredicate = { n: Node, ctx: Context ->
        earlyTermination?.let { it(n) } == true ||
            scope.maxSteps?.let { ctx.steps >= it } == true ||
            (!allowOverwritingValue &&
                // TODO: This should be replaced with some check if the memory location/whatever
                // where the data is kept is (partially) written to.
                this in n.prevDFG &&
                (n as? Reference)?.access == AccessValues.WRITE)
    }
    val nextEOGEvaluation =
        this.followNextEOGEdgesUntilHit(
            collectFailedPaths = true,
            findAllPossiblePaths = true,
            interproceduralAnalysis = scope is INTERPROCEDURAL,
            earlyTermination = earlyTerminationPredicate,
        ) {
            predicate(it) && it in nextDFGPaths
        }
    val allChildren =
        nextEOGEvaluation.failed.map {
            QueryTree(
                value = false,
                children = mutableListOf(QueryTree(value = it)),
                stringRepresentation =
                    "The EOG path reached the end  " +
                        if (earlyTermination != null)
                            "(or ${it.lastOrNull()} which a predicate marking the end) "
                        else "" + "before passing through a node matching the required predicate.",
                node = this,
            )
        } +
            nextEOGEvaluation.fulfilled.map {
                QueryTree(
                    value = true,
                    children = mutableListOf(QueryTree(value = it)),
                    stringRepresentation =
                        "The EOG path reached the node ${it.lastOrNull()} matching the required predicate" +
                            if (earlyTermination != null)
                                " before reaching a node matching the early termination predicate"
                            else "",
                    node = this,
                )
            }
    return QueryTree(
        value = nextEOGEvaluation.failed.isEmpty(),
        children = allChildren.toMutableList(),
        stringRepresentation =
            if (nextEOGEvaluation.failed.isEmpty()) {
                "All EOG paths fulfilled the predicate"
            } else {
                "Some EOG paths failed to fulfill the predicate"
            },
        node = this,
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
    vararg sensitivities: AnalysisSensitivity,
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
