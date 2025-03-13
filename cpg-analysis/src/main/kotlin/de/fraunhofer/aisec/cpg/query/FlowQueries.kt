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
import de.fraunhofer.aisec.cpg.graph.AnalysisSensitivity
import de.fraunhofer.aisec.cpg.graph.FilterUnreachableEOG
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import kotlin.collections.all

/**
 * Converts the [FulfilledAndFailedPaths] to a list of [QueryTree]s containing the failed and
 * fulfilled paths.
 */
fun FulfilledAndFailedPaths.toQueryTree(
    startNode: Node,
    queryType: String,
): List<QueryTree<Boolean>> {
    return this.fulfilled.map {
        SinglePathResult(
            true,
            mutableListOf(QueryTree(it)),
            "$queryType from $startNode to ${it.last()} fulfills the requirement",
            startNode,
            Success(it.last()),
        )
    } +
        this.failed.map { (reason, nodes) ->
            SinglePathResult(
                true,
                mutableListOf(QueryTree(nodes)),
                "$queryType from $startNode to ${nodes.last()} fulfills the requirement",
                startNode,
                if (reason == FailureReason.PATH_ENDED) {
                    PathEnded(nodes.last())
                } else if (reason == FailureReason.HIT_EARLY_TERMINATION) {
                    HitEarlyTermination(nodes.last())
                } else {
                    // TODO: We cannot set this (yet) but it might be useful to differentiate
                    // between "path is really at the end" or "we just stopped". Requires adaptions
                    // in followXUntilHit and all of its callers
                    StepsExceeded(nodes.last())
                },
            )
        }
}

/** Determines if the predicate [Must] or [May] hold */
sealed class AnalysisType {
    abstract fun createQueryTree(
        evalRes: FulfilledAndFailedPaths,
        startNode: Node,
        queryType: String,
    ): QueryTree<Boolean>
}

/**
 * The predicate must hold, i.e., all paths fulfill the property/requirement. No path violates the
 * property/requirement.
 */
object Must : AnalysisType() {
    override fun createQueryTree(
        evalRes: FulfilledAndFailedPaths,
        startNode: Node,
        queryType: String,
    ): QueryTree<Boolean> {
        val allPaths = evalRes.toQueryTree(startNode, queryType)

        return QueryTree(
            evalRes.failed.isEmpty(),
            allPaths.toMutableList(),
            "$queryType from $startNode to ${evalRes.fulfilled.map { it.last() }}",
            startNode,
        )
    }
}

/**
 * The predicate may hold, i.e., there is at least one path which fulfills the property/requirement.
 */
object May : AnalysisType() {
    override fun createQueryTree(
        evalRes: FulfilledAndFailedPaths,
        startNode: Node,
        queryType: String,
    ): QueryTree<Boolean> {
        val allPaths = evalRes.toQueryTree(startNode, queryType)

        return QueryTree(
            evalRes.fulfilled.isNotEmpty(),
            allPaths.toMutableList(),
            "$queryType from $startNode to ${evalRes.fulfilled.map { it.last() }}",
            startNode,
        )
    }
}

/**
 * Follows the [Dataflow] edges from [startNode] in the given [direction] (default: Forward
 * analysis) until reaching a node fulfilling [predicate].
 *
 * The interpretation of the analysis result can be configured as must analysis (all paths have to
 * fulfill the criterion) or may analysis (at least one path has to fulfill the criterion) by
 * setting the [type] parameter (default: [May]). Note that this only reasons about existing DFG
 * paths, and it might not be sufficient if you actually want a guarantee that some action always
 * happens with the data. In this case, you may need to check the [executionPath].
 *
 * The [sensitivities] can also be configured to a certain extent. The analysis can be run as
 * inter-procedural or intra-procedural analysis. If [earlyTermination] is not `null`, this can be
 * used as a criterion to make the query fail if this predicate is fulfilled before [predicate].
 */
fun dataFlow(
    startNode: Node,
    direction: AnalysisDirection = Forward(GraphToFollow.DFG),
    type: AnalysisType = May,
    vararg sensitivities: AnalysisSensitivity = FieldSensitive + ContextSensitive,
    scope: AnalysisScope = Interprocedural(),
    earlyTermination: ((Node) -> Boolean)? = null,
    predicate: (Node) -> Boolean,
): QueryTree<Boolean> {
    val collectFailedPaths = type == Must
    val findAllPossiblePaths = type == Must
    val earlyTermination = { n: Node, ctx: Context -> earlyTermination?.let { it(n) } == true }

    val evalRes =
        if (direction is Bidirectional) {
                arrayOf(Forward(GraphToFollow.DFG), Backward(GraphToFollow.DFG))
            } else {
                arrayOf(direction)
            }
            .fold(FulfilledAndFailedPaths(listOf(), listOf())) { result, direction ->
                result +
                    startNode.followDFGEdgesUntilHit(
                        collectFailedPaths = collectFailedPaths,
                        findAllPossiblePaths = findAllPossiblePaths,
                        direction = direction,
                        sensitivities = sensitivities,
                        scope = scope,
                        earlyTermination = earlyTermination,
                        predicate = predicate,
                    )
            }

    return type.createQueryTree(evalRes = evalRes, startNode = startNode, queryType = "data flow")
}

/**
 * Follows the [de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder] edges from [startNode] in
 * the given [direction] (default: [Forward] analysis) until reaching a node fulfilling [predicate].
 *
 * The interpretation of the analysis result can be configured as must analysis (all paths have to
 * fulfill the criterion) or may analysis (at least one path has to fulfill the criterion) by
 * setting the [type] parameter (default: [May]).
 *
 * The analysis can be run as interprocedural or intraprocedural analysis by setting [scope]. If
 * [earlyTermination] is not `null`, this can be used as a criterion to make the query fail if this
 * predicate is fulfilled before [predicate].
 */
fun executionPath(
    startNode: Node,
    direction: AnalysisDirection = Forward(GraphToFollow.EOG),
    type: AnalysisType = May,
    scope: AnalysisScope = Interprocedural(),
    earlyTermination: ((Node) -> Boolean)? = null,
    predicate: (Node) -> Boolean,
): QueryTree<Boolean> {
    val collectFailedPaths = type == Must
    val findAllPossiblePaths = type == Must
    val earlyTermination = { n: Node, ctx: Context -> earlyTermination?.let { it(n) } == true }

    val evalRes =
        if (direction is Bidirectional) {
                arrayOf(Forward(GraphToFollow.EOG), Backward(GraphToFollow.EOG))
            } else {
                arrayOf(direction)
            }
            .fold(FulfilledAndFailedPaths(listOf(), listOf())) { result, direction ->
                result +
                    startNode.followEOGEdgesUntilHit(
                        collectFailedPaths = collectFailedPaths,
                        findAllPossiblePaths = findAllPossiblePaths,
                        direction = direction,
                        sensitivities = FilterUnreachableEOG + ContextSensitive,
                        scope = scope,
                        earlyTermination = earlyTermination,
                        predicate = predicate,
                    )
            }

    return type.createQueryTree(
        evalRes = evalRes,
        startNode = startNode,
        queryType = "execution path",
    )
}

/**
 * This function tracks if the data in [source] always flow through a node which fulfills
 * [validatorPredicate] before reaching a sink which is specified by [sinkPredicate]. The analysis
 * can be configured with [scope] and [sensitivities].
 */
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

/**
 * This function tracks if the data in [this] always flow through a node which fulfills [predicate].
 * An early termination can be specified by the predicate [earlyTermination].
 * [allowOverwritingValue] can be used to configure if overwriting the value (or part of it) results
 * in a failure of the requirement (if `false`) or if it does not affect the evaluation. The
 * analysis can be configured with [scope] and [sensitivities].
 */
fun Node.alwaysFlowsTo(
    allowOverwritingValue: Boolean = false,
    earlyTermination: ((Node) -> Boolean)? = null,
    scope: AnalysisScope,
    vararg sensitivities: AnalysisSensitivity =
        ContextSensitive + FieldSensitive + FilterUnreachableEOG,
    predicate: (Node) -> Boolean,
): QueryTree<Boolean> {
    val nextDFGPaths =
        this.collectAllNextDFGPaths(
                interproceduralAnalysis = scope is Interprocedural,
                contextSensitive = ContextSensitive in sensitivities,
            )
            .flatten()
    val earlyTerminationPredicate = { n: Node, ctx: Context ->
        earlyTermination?.let { it(n) } == true ||
            (!allowOverwritingValue &&
                // TODO: This should be replaced with some check if the memory location/whatever
                // where the data is kept is (partially) written to.
                this in n.prevDFG &&
                (n as? Reference)?.access == AccessValues.WRITE)
    }
    val nextEOGEvaluation =
        this.followEOGEdgesUntilHit(
            collectFailedPaths = true,
            findAllPossiblePaths = true,
            scope = scope,
            sensitivities = sensitivities,
            earlyTermination = earlyTerminationPredicate,
        ) {
            predicate(it) && it in nextDFGPaths
        }
    val allChildren =
        nextEOGEvaluation.failed.map {
            // TODO: We can update this too
            QueryTree(
                value = false,
                children = mutableListOf(QueryTree(value = it)),
                stringRepresentation =
                    "The EOG path reached the end  " +
                        if (earlyTermination != null)
                            "(or ${it.second.lastOrNull()} which a predicate marking the end) "
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
