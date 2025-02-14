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

/** Determines how far we want to follow edges within the analysis. */
sealed class AnalysisScope

/** Only intraprocedural analysis */
class INTRAPROCEDURAL : AnalysisScope()

/**
 * Enable interprocedural analysis. [maxDepth] defines how many function calls we follow at most.
 */
class INTERPROCEDURAL(val maxDepth: Int = -1) : AnalysisScope()

/** Enable interprocedural analysis */
class MAX_STEPS(val steps: Int) : AnalysisScope()

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

typealias AnalysisSensitivities = List<AnalysisSensitivity>

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
    val interproceduralAnalysis = scope is INTERPROCEDURAL
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
            AnalysisDirection.BIDIRECTIONAL -> {
                startNode.followNextDFGEdgesUntilHit(
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    useIndexStack = useIndexStack,
                    contextSensitive = contextSensitive,
                    interproceduralAnalysis = interproceduralAnalysis,
                    predicate = predicate,
                ) +
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
    val interproceduralAnalysis = scope is INTERPROCEDURAL
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
            AnalysisDirection.BIDIRECTIONAL -> {
                startNode.followNextEOGEdgesUntilHit(
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    interproceduralAnalysis = interproceduralAnalysis,
                    predicate = predicate,
                ) +
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

fun dataFlowWithValidator(
    source: Node,
    validatorPredicate: (Node) -> Boolean,
    sinkPredicate: (Node) -> Boolean,
    scope: AnalysisScope,
    sensitivities: AnalysisSensitivities,
): QueryTree<Boolean> {
    val flowsToValidator =
        source.alwaysFlowsTo(validatorPredicate, true, scope = scope, sensitivities = sensitivities)
    val resultChildren = mutableListOf<QueryTree<Boolean>>()
    for (child in (flowsToValidator.children as List<QueryTree<Boolean>>)) {
        // child is a QueryTree<Boolean> whose children consist of a list with a single item of type
        // QueryTree<List<Node>>.
        val nodesOnPath = (child.children.singleOrNull() as? QueryTree<List<Node>>)?.value
        // We check if any node in the List<Node> fulfills sinkPredicate.
        val sinksOnPath =
            nodesOnPath?.filter { node ->
                sinkPredicate(node) &&
                    dataFlowBase(
                            startNode = source,
                            predicate = { it == node },
                            direction = AnalysisDirection.FORWARD,
                            type = AnalysisType.MAY,
                            sensitivities = sensitivities,
                            scope = scope,
                            verbose = false,
                        )
                        .value
            } ?: listOf()
        if (sinksOnPath.isNotEmpty()) {
            // There's at least one node on the path between source and validatorPredicate which
            // qualifies as sink. This means, this subtree does not fulfill our requirement. We
            // change the value of this subtree and update the string.
            resultChildren.add(
                QueryTree(
                    value = false,
                    children =
                        mutableListOf(
                            QueryTree(
                                value = nodesOnPath,
                                stringRepresentation = "Path between the source and validator",
                            ),
                            QueryTree(
                                value = sinksOnPath,
                                stringRepresentation = "The sinks on the path",
                            ),
                        ),
                    stringRepresentation =
                        "The path between $source and the sink(s) $sinksOnPath is shorter than the path to the validator ${nodesOnPath?.lastOrNull()}",
                    node = source,
                )
            )
        } else {
            // There was no sink on the path. We're happy and keep the child as-is for the result.
            resultChildren.add(child)
        }
    }
    val value = resultChildren.all { it.value }
    return QueryTree(
        value = value,
        children = resultChildren.toMutableList(),
        stringRepresentation =
            if (value)
                "All paths from $source first run through a validator before reaching a sink or never reach a sink."
            else "Some paths from $source reach a sink before visiting a validator",
        node = source,
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
        { it == sink },
        direction = AnalysisDirection.FORWARD,
        type = AnalysisType.MAY,
        sensitivities = AnalysisSensitivity.FIELD_SENSITIVE + AnalysisSensitivity.CONTEXT_SENSITIVE,
        scope = INTERPROCEDURAL(),
        verbose = collectFailedPaths || findAllPossiblePaths,
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
        predicate = predicate,
        direction = AnalysisDirection.FORWARD,
        type = AnalysisType.MAY,
        sensitivities = AnalysisSensitivity.FIELD_SENSITIVE + AnalysisSensitivity.CONTEXT_SENSITIVE,
        scope = INTERPROCEDURAL(),
        verbose = collectFailedPaths || findAllPossiblePaths,
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
                    // TODO: This should be replaced with some check if the memory location/whatever
                    // where the data is kept is (partially) written to.
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
        scope = INTERPROCEDURAL(),
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
