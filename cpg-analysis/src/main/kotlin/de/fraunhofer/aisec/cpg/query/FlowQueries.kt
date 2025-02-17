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
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.IndexedDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference

interface StepSelector {
    /**
     * Returns `true` if the given [edge] will be followed giving the provided [ctx] under the
     * provided configuration.
     */
    fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        analysisDirection: AnalysisDirection? = null,
    ): Boolean
}

/**
 * Determines how far we want to follow edges within the analysis. [maxSteps] defines the total
 * number of steps we will follow in the graph (unlimited depth if `null`).
 */
sealed class AnalysisScope(val maxSteps: Int? = null) : StepSelector {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        analysisDirection: AnalysisDirection?,
    ): Boolean {
        // Follow the edge if maxSteps is null or if maxSteps < ctx.steps
        return this.maxSteps == null || maxSteps < ctx.steps
    }
}

/**
 * Only intraprocedural analysis. [maxSteps] defines the total number of steps we will follow in the
 * graph (unlimited depth if `null`).
 */
class Intraprocedural(maxSteps: Int? = null) : AnalysisScope(maxSteps)

/**
 * Enable interprocedural analysis. [maxCallDepth] defines how many function calls we follow at most
 * (unlimited depth if `null`). [maxSteps] defines the total number of steps we will follow in the
 * graph (unlimited depth if `null`).
 */
class Interprocedural(val maxCallDepth: Int? = null, maxSteps: Int? = null) :
    AnalysisScope(maxSteps) {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        analysisDirection: AnalysisDirection?,
    ): Boolean {
        // Follow the edge if we're still in the maxSteps range and (if maxCallDepth is null or the
        // call stack is not deeper yet)
        return super.followEdge(currentNode, edge, ctx) &&
            (maxCallDepth == null || ctx.callStack.depth < maxCallDepth)
    }
}

/** Determines in which direction we follow the edges. */
sealed class AnalysisDirection : StepSelector {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        analysisDirection: AnalysisDirection?,
    ): Boolean {
        return true
    }

    abstract fun unwrapNextStepFromEdge(edge: Edge<Node>): Node
}

/** Follow the order of the EOG */
class Forward() : AnalysisDirection() {
    override fun unwrapNextStepFromEdge(edge: Edge<Node>): Node {
        return edge.end
    }
}

/** Against the order of the EOG */
class Backward() : AnalysisDirection() {
    override fun unwrapNextStepFromEdge(edge: Edge<Node>): Node {
        return edge.start
    }
}

/** In and against the order of the EOG */
class Bidirectional() : AnalysisDirection() {
    override fun unwrapNextStepFromEdge(edge: Edge<Node>): Node {
        TODO("Not yet implemented")
    }
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
sealed class AnalysisSensitivity : StepSelector {
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

/** Consider the calling context when following paths (e.g. based on a call stack). */
class ContextSensitive() : AnalysisSensitivity() {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        analysisDirection: AnalysisDirection?,
    ): Boolean {
        TODO("Not yet implemented")
    }
}

/**
 * Differentiate between fields, attributes, known keys or known indices of objects. This does not
 * include computing possible indices or keys if they are not given as a literal.
 */
class FieldSensitive() : AnalysisSensitivity() {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        analysisDirection: AnalysisDirection?,
    ): Boolean {
        return if (edge is Dataflow) {
            // Forward analysis
            if (
                currentNode is InitializerListExpression &&
                    analysisDirection?.unwrapNextStepFromEdge(edge) in currentNode.initializers &&
                    edge.granularity is IndexedDataflowGranularity
            ) {
                // currentNode is the ILE, it is the child and the next (e.g. read
                // from).
                // We try to pop from the stack and only select the elements with the
                // matching index.
                ctx.indexStack.popIfOnTop(edge.granularity as IndexedDataflowGranularity)
            } else if (
                analysisDirection?.unwrapNextStepFromEdge(edge) is InitializerListExpression &&
                    edge.granularity is IndexedDataflowGranularity
            ) {
                // CurrentNode is the child and nextDFG goes to ILE => currentNode's written
                // to. Push to stack
                ctx.indexStack.push(edge.granularity as IndexedDataflowGranularity)
                true
            } else {
                true
            }
        } else {
            true
        }
    }
}

/**
 * Also consider implicit flows during the dataflow analysis. E.g. if a condition depends on the
 * value we're interested in, different behaviors in the branches can leak data and thus, the
 * dependencies of this should also be flagged.
 */
class Implicit() : AnalysisSensitivity() {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        analysisDirection: AnalysisDirection?,
    ): Boolean {
        return true
    }
}

/**
 * Follows the [de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow] edges from [startNode] in the
 * given [direction] (default: Forward analysis) until reaching a node fulfilling [predicate].
 *
 * The interpretation of the analysis result can be configured as must analysis (all paths have to
 * fulfill the criterion) or may analysis (at least one path has to fulfill the criterion) by
 * setting the [type] parameter (default: [AnalysisType.MAY]). Note that this only reasons about
 * existing DFG paths, and it might not be sufficient if you actually want a guarantee that some
 * action always happens with the data. In this case, you may need to check the [executionPath].
 *
 * The [sensitivities] can also be configured to a certain extent. The analysis can be run as
 * interprocedural or intraprocedural analysis. If [earlyTermination] is not `null`, this can be
 * used as a criterion to make the query fail if this predicate is fulfilled before [predicate].
 */
fun dataFlow(
    startNode: Node,
    direction: AnalysisDirection = Forward(),
    type: AnalysisType = AnalysisType.MAY,
    vararg sensitivities: AnalysisSensitivity = FieldSensitive() + ContextSensitive(),
    scope: AnalysisScope = Interprocedural(),
    verbose: Boolean = true,
    earlyTermination: ((Node) -> Boolean)? = null,
    predicate: (Node) -> Boolean,
): QueryTree<Boolean> {
    val collectFailedPaths = type == AnalysisType.MUST || verbose
    val findAllPossiblePaths = type == AnalysisType.MUST || verbose
    val useIndexStack = sensitivities.any { it is FieldSensitive }
    val contextSensitive = sensitivities.any { it is ContextSensitive }
    val interproceduralAnalysis = scope is Interprocedural
    val earlyTermination = { n: Node, ctx: Context ->
        earlyTermination?.let { it(n) } == true || scope.maxSteps?.let { ctx.steps >= it } == true
    }
    val evalRes =
        when (direction) {
            is Forward -> {
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
            is Backward -> {
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
            is Bidirectional -> {
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

/**
 * Follows the [de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder] edges from [startNode] in
 * the given [direction] (default: [Forward] analysis) until reaching a node fulfilling [predicate].
 *
 * The interpretation of the analysis result can be configured as must analysis (all paths have to
 * fulfill the criterion) or may analysis (at least one path has to fulfill the criterion) by
 * setting the [type] parameter (default: [AnalysisType.MAY]).
 *
 * The analysis can be run as interprocedural or intraprocedural analysis by setting [scope]. If
 * [earlyTermination] is not `null`, this can be used as a criterion to make the query fail if this
 * predicate is fulfilled before [predicate].
 */
fun executionPath(
    startNode: Node,
    direction: AnalysisDirection = Forward(),
    type: AnalysisType = AnalysisType.MAY,
    scope: AnalysisScope = Interprocedural(),
    verbose: Boolean = true,
    earlyTermination: ((Node) -> Boolean)? = null,
    predicate: (Node) -> Boolean,
): QueryTree<Boolean> {
    val collectFailedPaths = type == AnalysisType.MUST || verbose
    val findAllPossiblePaths = type == AnalysisType.MUST || verbose
    val interproceduralAnalysis = scope is Interprocedural
    val earlyTermination = { n: Node, ctx: Context ->
        earlyTermination?.let { it(n) } == true || scope.maxSteps?.let { ctx.steps >= it } == true
    }
    val evalRes =
        when (direction) {
            is Forward -> {
                startNode.followNextEOGEdgesUntilHit(
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    interproceduralAnalysis = interproceduralAnalysis,
                    earlyTermination = earlyTermination,
                    predicate = predicate,
                )
            }
            is Backward -> {
                startNode.followPrevEOGEdgesUntilHit(
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    interproceduralAnalysis = interproceduralAnalysis,
                    earlyTermination = earlyTermination,
                    predicate = predicate,
                )
            }
            is Bidirectional -> {
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
    vararg sensitivities: AnalysisSensitivity,
    predicate: (Node) -> Boolean,
): QueryTree<Boolean> {
    val nextDFGPaths =
        this.collectAllNextDFGPaths(
                interproceduralAnalysis = scope is Interprocedural,
                contextSensitive = ContextSensitive() in sensitivities,
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
            interproceduralAnalysis = scope is Interprocedural,
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
