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

import de.fraunhofer.aisec.cpg.assumptions.Assumption
import de.fraunhofer.aisec.cpg.assumptions.AssumptionType
import de.fraunhofer.aisec.cpg.assumptions.HasAssumptions
import de.fraunhofer.aisec.cpg.assumptions.addAssumptionDependence
import de.fraunhofer.aisec.cpg.assumptions.assume
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.AnalysisSensitivity
import de.fraunhofer.aisec.cpg.graph.FilterUnreachableEOG
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CollectionComprehension
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.HasType
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
            value = true,
            children =
                mutableListOf(
                    QueryTree(value = it.nodes, operator = GenericQueryOperators.EVALUATE)
                ),
            stringRepresentation =
                "$queryType from ${startNode.compactToString()} to ${it.nodes.last().compactToString()} fulfills the requirement",
            node = startNode,
            terminationReason = Success(it.nodes.last()),
            operator = GenericQueryOperators.EVALUATE,
        )
    } +
        this.failed.map { (reason, nodePath) ->
            SinglePathResult(
                value = false,
                children =
                    mutableListOf(
                        QueryTree(value = nodePath.nodes, operator = GenericQueryOperators.EVALUATE)
                            .addAssumptionDependence(nodePath)
                    ),
                stringRepresentation =
                    "$queryType from $startNode to ${nodePath.nodes.last()} does not fulfill the requirement",
                node = startNode,
                terminationReason =
                    if (reason == FailureReason.PATH_ENDED) {
                        PathEnded(nodePath.nodes.last())
                    } else if (reason == FailureReason.HIT_EARLY_TERMINATION) {
                        HitEarlyTermination(nodePath.nodes.last())
                    } else {
                        // TODO: We cannot set this (yet) but it might be useful to differentiate
                        // between "path is really at the end" or "we just stopped". Requires
                        // adaptions
                        // in followXUntilHit and all of its callers
                        StepsExceeded(nodePath.nodes.last())
                    },
                operator = GenericQueryOperators.EVALUATE,
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
            value = allPaths.all { it.value },
            children = allPaths.toMutableList(),
            stringRepresentation =
                "$queryType from ${startNode.compactToString()} to ${(evalRes.fulfilled.map { it.nodes.last().compactToString() })}",
            node = startNode,
            operator = GenericQueryOperators.ALL,
            collectCallerInfo = true,
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
            value = allPaths.any { it.value },
            children = allPaths.toMutableList(),
            stringRepresentation =
                "$queryType from ${startNode.compactToString()} to ${evalRes.fulfilled.map { it.nodes.last().compactToString() }}",
            node = startNode,
            operator = GenericQueryOperators.ANY,
            collectCallerInfo = true,
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
    ctx: Context = Context(steps = 0),
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
                        ctx = ctx,
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
 * can be configured with [scope] and [sensitivities]. If no matching sink is found, the path is
 * considered as ok even if there's no validator on that path.
 */
fun dataFlowWithValidator(
    source: Node,
    validatorPredicate: (Node) -> Boolean,
    sinkPredicate: (Node) -> Boolean,
    scope: AnalysisScope,
    vararg sensitivities: AnalysisSensitivity,
): QueryTree<Boolean> {
    return source.alwaysFlowsToInternal(
        allowOverwritingValue = true,
        noSinkIsGood = true,
        earlyTermination = sinkPredicate,
        identifyCopies = false,
        stopIfImpossible = true,
        scope = scope,
        sensitivities = sensitivities,
        predicate = validatorPredicate,
    )
}

/**
 * This data class serves as a wrapper for a [Node] that is returned as a result. The wrapper is
 * needed to add additional information on the result, such as [assumptions] that were taken when
 * computing this node as the result of a function.
 */
data class NodeWithAssumption(val node: Node) : HasAssumptions {
    override val assumptions: MutableSet<Assumption> = mutableSetOf()

    /**
     * Adds the [assumptions] of the current [NodeCollectionWithAssumption] and the assumptions of
     * the node that is the result.
     */
    override fun relevantAssumptions(): Set<Assumption> {
        return super.relevantAssumptions() + node.relevantAssumptions()
    }
}

/**
 * This data class serves as a wrapper for a collection of [Node] that is returned as a result. The
 * wrapper is needed to add additional information on the result, such as [assumptions] that were
 * taken when computing this collection of nodes as the result of a function.
 */
data class NodeCollectionWithAssumption(val nodes: Collection<Node>) : HasAssumptions {
    override val assumptions: MutableSet<Assumption> = mutableSetOf()

    /**
     * Adds the [assumptions] of the current [NodeCollectionWithAssumption] and the assumptions of
     * all nodes contained in the object.
     */
    override fun relevantAssumptions(): Set<Assumption> {
        return super.relevantAssumptions() + nodes.flatMap { it.relevantAssumptions() }
    }
}

/**
 * This function tries to identify if the data held in [this] node are copied to another memory
 * location. This could happen by:
 * - Constructing a new object where our data flow to: We should track this object and the target of
 *   the operation separately.
 * - Operating on immutable objects (e.g. via [BinaryOperator]s): We should track this object and
 *   the target of the operation separately.
 * - Modifying an object via an operator (e.g. `+=`) and we are the rhs of the assignment: We should
 *   track both, this object and the target of the assignment.
 * - Assigning an immutable object to a new variable: We should track this object and the target of
 *   the assignment separately.
 */
fun Node.generatesNewData(): NodeCollectionWithAssumption {
    val tempAssumptions = mutableSetOf<HasAssumptions>()
    val splitNodes =
        when {
            this is BinaryOperator && !this.type.isMutable -> {
                // If the type of the node is a primitive (more precisely immutable) type, we will
                // create new "object" that we have to track. But we should find the first
                // declaration
                // this flows to and track that one. If it flows to a reference first, this will
                // typically be identified by handling the assignments below which is why we
                // explicitly exclude such a flow here.
                val tmp =
                    this.followDFGEdgesUntilHit(
                        earlyTermination = { node, _ -> node is Reference }
                    ) {
                        it is Declaration
                    }
                tempAssumptions.addAll(tmp.fulfilled)

                tmp.fulfilled.map { it.nodes.last() }.toSet()
            }
            /* A new object is constructed and our data flow into this object -> track the new object. */
            this is ConstructExpression ||
                /* A collection comprehension (e.g. list, set, dict comprehension) generates a new object similar to calling the constructor. */
                this is CollectionComprehension -> {
                setOf(this)
            }

            this.astParent is AssignExpression &&
                this in (this.astParent as AssignExpression).rhs &&
                (this.astParent as AssignExpression).operatorCode in
                    this.language.compoundAssignmentOperators -> {
                // If we're the rhs of an assignment with an operator like +=, we should track the
                // lhs value separately.
                (this as? HasType)?.let { (this.astParent as AssignExpression).findTargets(it) }
                    ?: setOf()
            }
            this is Expression &&
                this.astParent is AssignExpression &&
                this in (this.astParent as AssignExpression).rhs &&
                !this.type.isMutable -> {
                (this.astParent as AssignExpression).findTargets(this)
            }
            else -> emptySet()
        }
    return NodeCollectionWithAssumption(splitNodes)
        .addAssumptionDependence(tempAssumptions + splitNodes + this)
        .assume(
            AssumptionType.DataFlowAssumption,
            "We assume that the node $this generates the following new \"objects\" which require separate handling as they represent copies/clones of the original \"object\": $splitNodes.\n\n" +
                "We assume that this list is complete and does not contain additional elements.\n" +
                "To verify this assumption, we need to check if all of the following (global) sub-assumptions hold:\n" +
                "1. The list of mutable and immutable types is complete and sound for each programming language used in the analyzed project.\n" +
                "2. Copies of data happen exclusively by one of the following patterns:\n" +
                "2.a) Constructing a new object where our data flow to by a constructor invocation or by a collection comprehension: We should track this object and the target of the operation separately." +
                "2.b) Operating on immutable objects (via BinaryOperators): We should track this object and the target of the operation separately." +
                "2.c) Modifying an object via an operator (e.g. `+=`) and the \"object\" is the rhs of the assignment: We should track both, this object and the target of the assignment." +
                "2.d) Assigning an immutable object to a new variable: We should track this object and the target of the assignment separately.",
        )
}

/**
 * Identifies the information to track for the given node based on the data flow graph (DFG). This
 * function collects all reachable DFG nodes and determines if any of these nodes generate new data.
 *
 * @param scope The analysis scope, which can be interprocedural or intraprocedural.
 * @param sensitivities The analysis sensitivities to be considered, such as context sensitivity,
 *   field sensitivity, and filtering unreachable EOG.
 * @return A set of nodes that includes the original node and any nodes that generate new data.
 */
fun Node.identifyInfoToTrack(
    scope: AnalysisScope,
    direction: AnalysisDirection = Forward(GraphToFollow.DFG),
    vararg sensitivities: AnalysisSensitivity =
        ContextSensitive + FieldSensitive + FilterUnreachableEOG,
): Set<NodeWithAssumption> {
    // Get all next DFG nodes. These must include all relevant operations to make this work but that
    // should be the case with our current implementation.
    val reachableDFGNodes =
        if (direction is Forward) {
                this.collectAllNextDFGPaths(
                    interproceduralAnalysis = scope is Interprocedural,
                    contextSensitive = ContextSensitive in sensitivities,
                )
            } else {
                this.collectAllPrevDFGPaths(
                    interproceduralAnalysis = scope is Interprocedural,
                    contextSensitive = ContextSensitive in sensitivities,
                )
            }
            .map { it.nodes }
            .flatten()
            .toSet()
    val result = mutableSetOf(NodeWithAssumption(this))
    for (node in reachableDFGNodes) {
        // Is this node a node copying the data? If so, add its targets to the list.
        val targets = node.generatesNewData()
        result.addAll(targets.nodes.map { NodeWithAssumption(it).addAssumptionDependence(targets) })
    }
    return result
}

/**
 * This function tracks if the data in [this] always originates from a node which fulfills
 * [predicate]. An early termination can be specified by the predicate [earlyTermination].
 * [allowOverwritingValue] can be used to configure if overwriting the value (or part of it) results
 * in a failure of the requirement (if `false`) or if it does not affect the evaluation. The
 * analysis can be configured with [scope] and [sensitivities]. [stopIfImpossible] enables the
 * option to stop iterating through the EOG if we already left the function where we started and
 * none of the nodes reaching by the DFG is in the new scope or one of its parents (i.e., the
 * condition cannot be fulfilled anymore).
 */
fun Node.alwaysComesFrom(
    allowOverwritingValue: Boolean = false,
    earlyTermination: ((Node) -> Boolean)? = null,
    identifyCopies: Boolean = true,
    stopIfImpossible: Boolean = true,
    scope: AnalysisScope,
    vararg sensitivities: AnalysisSensitivity =
        ContextSensitive + FieldSensitive + FilterUnreachableEOG,
    predicate: (Node) -> Boolean,
): QueryTree<Boolean> {
    return this.alwaysComesFromInternal(
        allowOverwritingValue = allowOverwritingValue,
        noSinkIsGood = false,
        earlyTermination = earlyTermination,
        identifyCopies = identifyCopies,
        stopIfImpossible = stopIfImpossible,
        scope = scope,
        sensitivities = sensitivities,
        predicate = predicate,
    )
}

/**
 * This function tracks if the data in [this] always come from a node which fulfills [predicate]. An
 * early termination can be specified by the predicate [earlyTermination]. [allowOverwritingValue]
 * can be used to configure if overwriting the value (or part of it) results in a failure of the
 * requirement (if `false`) or if it does not affect the evaluation. The analysis can be configured
 * with [scope] and [sensitivities]. [stopIfImpossible] enables the option to stop iterating through
 * the EOG if we already left the function where we started and none of the nodes reaching by the
 * DFG is in the new scope or one of its parents (i.e., the condition cannot be fulfilled anymore).
 *
 * If [noSinkIsGood] is set to `true`, all results with "ended path" are considered as fulfilled.
 */
internal fun Node.alwaysComesFromInternal(
    allowOverwritingValue: Boolean = false,
    noSinkIsGood: Boolean = false,
    earlyTermination: ((Node) -> Boolean)? = null,
    identifyCopies: Boolean = true,
    stopIfImpossible: Boolean = true,
    scope: AnalysisScope,
    vararg sensitivities: AnalysisSensitivity =
        ContextSensitive + FieldSensitive + FilterUnreachableEOG,
    predicate: (Node) -> Boolean,
): QueryTree<Boolean> {
    val nodesToTrack =
        if (identifyCopies) {
            this.identifyInfoToTrack(
                scope = scope,
                direction = Backward(GraphToFollow.DFG),
                sensitivities = sensitivities,
            )
        } else {
            setOf(NodeWithAssumption(this))
        }
    val allChildren = mutableListOf<QueryTree<Boolean>>()
    for (nodeToTrack in nodesToTrack) {
        val prevDFGPaths =
            nodeToTrack.node
                .collectAllPrevDFGPaths(
                    interproceduralAnalysis = scope is Interprocedural,
                    contextSensitive = ContextSensitive in sensitivities,
                )
                .map { it.nodes }
                .flatten()
                .toSet()
        val earlyTerminationPredicate = { n: Node, ctx: Context ->
            earlyTermination?.let { it(n) } == true ||
                // If we are not allowed to overwrite the value, we need to check if the node may
                // overwrite the value. In this case, we terminate early.
                (!allowOverwritingValue &&
                    // TODO: This should be replaced with some check if the memory location/whatever
                    // where the data is kept is (partially) written to.
                    nodeToTrack.node in n.nextDFG &&
                    (n as? Reference)?.access == AccessValues.WRITE)
        }
        val eogScope =
            if (stopIfImpossible && scope is Interprocedural) {
                InterproceduralWithDfgTermination(
                    maxCallDepth = scope.maxCallDepth,
                    maxSteps = scope.maxSteps,
                    allReachableNodes =
                        prevDFGPaths
                            .filter { it.scope != null && it !is FunctionDeclaration }
                            .toSet(),
                )
            } else scope
        val prevEOGEvaluation =
            nodeToTrack.node.followEOGEdgesUntilHit(
                direction = Backward(GraphToFollow.EOG),
                collectFailedPaths = true,
                findAllPossiblePaths = true,
                scope = eogScope,
                sensitivities = sensitivities,
                earlyTermination = earlyTerminationPredicate,
            ) {
                predicate(it) && it in prevDFGPaths
            }
        allChildren +=
            prevEOGEvaluation.failed.map { (failureReason, path) ->
                SinglePathResult(
                    // If we configure this function with "noSinkIsGood == true", then we only
                    // consider paths which hit the early termination or which exceeded the steps
                    // (though the latter is debatable).
                    // If "noSinkIsGood == false", we consider all paths which are not fulfilled as
                    // failed.
                    value = noSinkIsGood && failureReason == FailureReason.PATH_ENDED,
                    children =
                        mutableListOf(
                            QueryTree(value = path.nodes, operator = GenericQueryOperators.EVALUATE)
                                .addAssumptionDependence(path)
                        ),
                    stringRepresentation =
                        "The EOG path reached the end  " +
                            if (earlyTermination != null)
                                "(or ${path.nodes.lastOrNull()} which a predicate marking the end) "
                            else
                                "" +
                                    "before passing through a node matching the required predicate.",
                    node = nodeToTrack.node,
                    terminationReason =
                        if (failureReason == FailureReason.PATH_ENDED) {
                            PathEnded(path.nodes.last())
                        } else if (failureReason == FailureReason.HIT_EARLY_TERMINATION) {
                            HitEarlyTermination(path.nodes.last())
                        } else {
                            StepsExceeded(path.nodes.last())
                        },
                    operator = GenericQueryOperators.EVALUATE,
                )
            } +
                prevEOGEvaluation.fulfilled.map {
                    SinglePathResult(
                        value = true,
                        children =
                            mutableListOf(
                                QueryTree(
                                        value = it.nodes,
                                        operator = GenericQueryOperators.EVALUATE,
                                    )
                                    .addAssumptionDependence(it)
                            ),
                        stringRepresentation =
                            "The EOG path reached the node ${it.nodes.lastOrNull()} matching the required predicate" +
                                if (earlyTermination != null)
                                    " before reaching a node matching the early termination predicate"
                                else "",
                        node = this,
                        terminationReason = Success(it.nodes.last()),
                        operator = GenericQueryOperators.EVALUATE,
                    )
                }
    }

    val nothingFailed = allChildren.all { it.value }
    return QueryTree(
        value = nothingFailed,
        children = allChildren.toMutableList(),
        stringRepresentation =
            if (nothingFailed) {
                "All EOG paths fulfilled the predicate"
            } else {
                "Some EOG paths failed to fulfill the predicate"
            },
        node = this,
        assumptions = nodesToTrack.flatMap { it.assumptions }.toMutableSet(),
        operator = GenericQueryOperators.ALL,
    )
}

/**
 * This function tracks if the data in [this] always flow through a node which fulfills [predicate].
 * An early termination can be specified by the predicate [earlyTermination].
 * [allowOverwritingValue] can be used to configure if overwriting the value (or part of it) results
 * in a failure of the requirement (if `false`) or if it does not affect the evaluation. The
 * analysis can be configured with [scope] and [sensitivities]. [stopIfImpossible] enables the
 * option to stop iterating through the EOG if we already left the function where we started and
 * none of the nodes reaching by the DFG is in the new scope or one of its parents (i.e., the
 * condition cannot be fulfilled anymore).
 */
fun Node.alwaysFlowsTo(
    allowOverwritingValue: Boolean = false,
    earlyTermination: ((Node) -> Boolean)? = null,
    identifyCopies: Boolean = true,
    stopIfImpossible: Boolean = true,
    scope: AnalysisScope,
    vararg sensitivities: AnalysisSensitivity =
        ContextSensitive + FieldSensitive + FilterUnreachableEOG,
    predicate: (Node) -> Boolean,
): QueryTree<Boolean> {
    return this.alwaysFlowsToInternal(
        allowOverwritingValue = allowOverwritingValue,
        noSinkIsGood = false,
        earlyTermination = earlyTermination,
        identifyCopies = identifyCopies,
        stopIfImpossible = stopIfImpossible,
        scope = scope,
        sensitivities = sensitivities,
        predicate = predicate,
    )
}

/**
 * This function tracks if the data in [this] always flow through a node which fulfills [predicate].
 * An early termination can be specified by the predicate [earlyTermination].
 * [allowOverwritingValue] can be used to configure if overwriting the value (or part of it) results
 * in a failure of the requirement (if `false`) or if it does not affect the evaluation. The
 * analysis can be configured with [scope] and [sensitivities]. [stopIfImpossible] enables the
 * option to stop iterating through the EOG if we already left the function where we started and
 * none of the nodes reaching by the DFG is in the new scope or one of its parents (i.e., the
 * condition cannot be fulfilled anymore).
 *
 * If [noSinkIsGood] is set to `true`, all results with "ended path" are considered as fulfilled.
 */
internal fun Node.alwaysFlowsToInternal(
    allowOverwritingValue: Boolean = false,
    noSinkIsGood: Boolean = false,
    earlyTermination: ((Node) -> Boolean)? = null,
    identifyCopies: Boolean = true,
    stopIfImpossible: Boolean = true,
    scope: AnalysisScope,
    vararg sensitivities: AnalysisSensitivity =
        ContextSensitive + FieldSensitive + FilterUnreachableEOG,
    predicate: (Node) -> Boolean,
): QueryTree<Boolean> {
    val nodesToTrack =
        if (identifyCopies) {
            this.identifyInfoToTrack(scope = scope, sensitivities = sensitivities)
        } else {
            setOf(NodeWithAssumption(this))
        }
    val allChildren = mutableListOf<QueryTree<Boolean>>()
    for (nodeToTrack in nodesToTrack) {
        val nextDFGPaths =
            nodeToTrack.node
                .collectAllNextDFGPaths(
                    interproceduralAnalysis = scope is Interprocedural,
                    contextSensitive = ContextSensitive in sensitivities,
                )
                .map { it.nodes }
                .flatten()
                .toSet()
        val earlyTerminationPredicate = { n: Node, ctx: Context ->
            earlyTermination?.let { it(n) } == true ||
                // If we are not allowed to overwrite the value, we need to check if the node may
                // overwrite the value. In this case, we terminate early.
                (!allowOverwritingValue &&
                    // TODO: This should be replaced with some check if the memory location/whatever
                    // where the data is kept is (partially) written to.
                    nodeToTrack.node in n.prevDFG &&
                    (n as? Reference)?.access == AccessValues.WRITE)
        }
        val eogScope =
            if (stopIfImpossible && scope is Interprocedural) {
                InterproceduralWithDfgTermination(
                    maxCallDepth = scope.maxCallDepth,
                    maxSteps = scope.maxSteps,
                    allReachableNodes =
                        nextDFGPaths
                            .filter { it.scope != null && it !is FunctionDeclaration }
                            .toSet(),
                )
            } else scope
        val nextEOGEvaluation =
            nodeToTrack.node.followEOGEdgesUntilHit(
                collectFailedPaths = true,
                findAllPossiblePaths = true,
                scope = eogScope,
                sensitivities = sensitivities,
                earlyTermination = earlyTerminationPredicate,
            ) {
                predicate(it) && it in nextDFGPaths
            }
        allChildren +=
            nextEOGEvaluation.failed.map { (failureReason, path) ->
                SinglePathResult(
                    // If we configure this function with "noSinkIsGood == true", then we only
                    // consider paths which hit the early termination or which exceeded the steps
                    // (though the latter is debatable).
                    // If "noSinkIsGood == false", we consider all paths which are not fulfilled as
                    // failed.
                    value = noSinkIsGood && failureReason == FailureReason.PATH_ENDED,
                    children =
                        mutableListOf(
                            QueryTree(value = path.nodes, operator = GenericQueryOperators.EVALUATE)
                                .addAssumptionDependence(path)
                        ),
                    stringRepresentation =
                        "The EOG path reached the end  " +
                            if (earlyTermination != null)
                                "(or ${path.nodes.lastOrNull()} which a predicate marking the end) "
                            else
                                "" +
                                    "before passing through a node matching the required predicate.",
                    node = nodeToTrack.node,
                    terminationReason =
                        if (failureReason == FailureReason.PATH_ENDED) {
                            PathEnded(path.nodes.last())
                        } else if (failureReason == FailureReason.HIT_EARLY_TERMINATION) {
                            HitEarlyTermination(path.nodes.last())
                        } else {
                            StepsExceeded(path.nodes.last())
                        },
                    operator = GenericQueryOperators.EVALUATE,
                )
            } +
                nextEOGEvaluation.fulfilled.map {
                    SinglePathResult(
                        value = true,
                        children =
                            mutableListOf(
                                QueryTree(
                                        value = it.nodes,
                                        operator = GenericQueryOperators.EVALUATE,
                                    )
                                    .addAssumptionDependence(it)
                            ),
                        stringRepresentation =
                            "The EOG path reached the node ${it.nodes.lastOrNull()} matching the required predicate" +
                                if (earlyTermination != null)
                                    " before reaching a node matching the early termination predicate"
                                else "",
                        node = this,
                        terminationReason = Success(it.nodes.last()),
                        operator = GenericQueryOperators.EVALUATE,
                    )
                }
    }

    val nothingFailed = allChildren.all { it.value }
    return QueryTree(
        value = nothingFailed,
        children = allChildren.toMutableList(),
        stringRepresentation =
            if (nothingFailed) {
                "All EOG paths fulfilled the predicate"
            } else {
                "Some EOG paths failed to fulfill the predicate"
            },
        node = this,
        assumptions = nodesToTrack.flatMap { it.assumptions }.toMutableSet(),
        operator = GenericQueryOperators.ALL,
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
        operator = GenericQueryOperators.ALL,
    )
}
