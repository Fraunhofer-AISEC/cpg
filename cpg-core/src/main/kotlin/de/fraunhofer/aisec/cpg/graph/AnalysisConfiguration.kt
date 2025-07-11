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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.assumptions.HasAssumptions
import de.fraunhofer.aisec.cpg.assumptions.addAssumptionDependence
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edges.flows.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.IndexedDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.Invoke
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
import kotlin.collections.mapNotNull

/** A generic interface used to determine potential next steps. */
interface StepSelector {
    /**
     * Returns `true` if the given [edge] will be followed giving the provided [ctx] under the
     * provided configuration.
     */
    fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        analysisDirection: AnalysisDirection,
        interproceduralEdgesExist: Boolean = false,
    ): Boolean
}

/**
 * Determines how far we want to follow edges within the analysis. [maxSteps] defines the total
 * number of steps we will follow in the graph (unlimited depth if `null`).
 */
sealed class AnalysisScope(val maxSteps: Int? = null) : StepSelector

/**
 * Only intraprocedural analysis. [maxSteps] defines the total number of steps we will follow in the
 * graph (unlimited depth if `null`).
 */
class Intraprocedural(maxSteps: Int? = null) : AnalysisScope(maxSteps) {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        analysisDirection: AnalysisDirection,
        interproceduralEdgesExist: Boolean,
    ): Boolean {
        // Follow the edge if we're still in the maxSteps range and not an edge across function
        // boundaries.
        return (this.maxSteps == null || ctx.steps < maxSteps) &&
            edge !is ContextSensitiveDataflow &&
            edge !is Invoke
    }
}

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
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        analysisDirection: AnalysisDirection,
        interproceduralEdgesExist: Boolean,
    ): Boolean {
        // is this a short Function Summary Edge?
        val isShortFS = ((edge as? Dataflow)?.functionSummary) == true
        // Are we still in the range of the max steps?
        val maxStepsOk = (this.maxSteps == null || ctx.steps < maxSteps)
        // Are we still in the range of the max depth?
        val maxDepthOk = (maxCallDepth == null || ctx.callStack.depth < maxCallDepth)
        // If we have a shortFS and we exceeded the max depth, we follow it. Otherwise, we ignore it
        val followShortFS = isShortFS && (!maxDepthOk || !interproceduralEdgesExist)
        // If this is no shortFS and we did not yet reach the max depth, we follow it
        val followEverythingButShortFS = !isShortFS && maxDepthOk
        // Is this even an interprocedural edge or an edge we are going to follow anyways (assuming
        // that the maxSteps are still ok)?
        val isInterProcedural = (edge is ContextSensitiveDataflow) || isShortFS
        // Summary: In case we did not yet exceed the maxSteps, we follow the edge either if it's no
        // interprocedural edge or if we follow the shortFS edges or if we follow everything but the
        // short FS edges

        if (
            analysisDirection.edgeRequiresCallPush(currentNode, edge) &&
                currentNode is CallExpression
        ) {
            // Check if the call expression is already in the call stack because this would indicate
            // a loop (recursion).
            if (currentNode in ctx.callStack) {
                loopingPaths.add(
                    NodePath(path.map { it.first } + currentNode)
                        .addAssumptionDependence(path.map { it.second } + ctx)
                )
                return false
            }
        }

        // Follow the edge if we're still in the maxSteps range and (if maxCallDepth is null or the
        // call stack is not deeper yet)
        return maxStepsOk && (!isInterProcedural || followShortFS || followEverythingButShortFS)
    }
}

/**
 * It works similar to the normal [Interprocedural] but with one difference for an [Edge] causing us
 * to leave the current scope: If no [Node] passed in the [allReachableNodes] is in the scope (or a
 * parent scope) of the new element we reach, then we stop following the edge
 */
class InterproceduralWithDfgTermination(
    val maxCallDepth: Int? = null,
    maxSteps: Int? = null,
    val allReachableNodes: Set<Node> = setOf(),
) : AnalysisScope(maxSteps) {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        analysisDirection: AnalysisDirection,
        interproceduralEdgesExist: Boolean,
    ): Boolean {
        val nextNode = analysisDirection.unwrapNextStepFromEdge(edge)
        if (
            currentNode != nextNode &&
                edge is Invoke &&
                currentNode !is CallExpression &&
                ctx.callStack.isEmpty()
        ) {
            // We're leaving the current function and will go to a scope we haven't seen before
            // (i.e., not just pop elements from the call stack).
            // In this case, we check if any of the reachable nodes is in the scope we will reach.
            return (this.maxSteps == null || ctx.steps < maxSteps) &&
                (maxCallDepth == null || ctx.callStack.depth < maxCallDepth) &&
                // We also want to check `allReachableNodes`. One of them has to be in the scope we
                // will reach now.
                allReachableNodes.any { reachable ->
                    reachable.scope != null &&
                        (reachable.scope?.astNode == nextNode ||
                            reachable.scope == nextNode.scope ||
                            nextNode.scope?.firstScopeParentOrNull<Scope> {
                                reachable.scope == it
                            } != null)
                }
        }
        // We're in the same function, or we may pop elements from the call stack and return to a
        // function through which we entered this path. Nothing special here, just continue as
        // always.

        // Follow the edge if we're still in the maxSteps range and (if maxCallDepth is null or the
        // call stack is not deeper yet). This is the behavior of the normal Interprocedural
        return (this.maxSteps == null || ctx.steps < maxSteps) &&
            (maxCallDepth == null || ctx.callStack.depth < maxCallDepth)
    }
}

/**
 * Used to determine which subgraph will be followed. Currently, we support the [DFG] and [EOG].
 * Note that this may be used to follow other edges as well (e.g. the PDG for DFG and [Invoke] edges
 * for EOG).
 */
enum class GraphToFollow {
    DFG,
    EOG,
}

/**
 * Determines in which direction we follow the edges. Must determine which sub-graph we want to
 * follow.
 */
sealed class AnalysisDirection(val graphToFollow: GraphToFollow) {
    /**
     * Determines which nodes are in the next step and also updates the context accordingly. It must
     * be configured with the [currentNode], the [scope] of the analysis, the current [Context][ctx]
     * and the [sensitivities] which should be used by the analysis.
     */
    abstract fun pickNextStep(
        currentNode: Node,
        scope: AnalysisScope,
        ctx: Context,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        vararg sensitivities: AnalysisSensitivity,
    ): Collection<Pair<Node, Context>>

    /**
     * Considering the [edge], it determines which node (start or end of the edge) will be used as
     * next step.
     */
    abstract fun unwrapNextStepFromEdge(edge: Edge<Node>): Node

    /**
     * Considering the [edge], it determines which node (start or end of the edge) will be used as
     * next step. An adds all assumptions on the [edge] to the provided [hasAssumptions] object such
     * that they are not lost.
     */
    fun <T : HasAssumptions> unwrapNextStepFromEdge(
        edge: Edge<Node>,
        hasAssumptions: T,
    ): Pair<Node, T> {
        return Pair(
            unwrapNextStepFromEdge(edge),
            hasAssumptions.addAssumptionDependence(hasAssumptions),
        )
    }

    /**
     * Determines if the [edge] starting at [currentNode] requires to push a [CallExpression] on the
     * stack.
     */
    abstract fun edgeRequiresCallPush(currentNode: Node, edge: Edge<Node>): Boolean

    /**
     * Determines if the [edge] starting at [currentNode] requires to pop a [CallExpression] from
     * the stack.
     */
    abstract fun edgeRequiresCallPop(currentNode: Node, edge: Edge<Node>): Boolean

    /**
     * Filters all provided [edges] and checks if they are in [scope] and fulfill the provided
     * [sensitivities] under the given [Context][ctx].
     */
    internal fun filterEdges(
        currentNode: Node,
        edges: Collection<Edge<Node>>,
        ctx: Context,
        scope: AnalysisScope,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        vararg sensitivities: AnalysisSensitivity,
    ): Collection<Pair<Edge<Node>, Context>> {
        return edges.mapNotNull { edge ->
            val newCtx = ctx.clone()
            if (
                scope.followEdge(
                    currentNode,
                    edge,
                    newCtx,
                    path,
                    loopingPaths,
                    this,
                    edges.any { it is ContextSensitiveDataflow /*&&
                            ((it.start as? FunctionDeclaration)?.isInferred == false ||
                                (it.end as? FunctionDeclaration)?.isInferred == false)*/ },
                ) &&
                    sensitivities.all {
                        it.followEdge(currentNode, edge, newCtx, path, loopingPaths, this)
                    }
            ) {
                Pair(edge, newCtx)
            } else null
        }
    }

    /**
     * In some cases, we have to skip one step to actually continue in the graph. Typical examples
     * are [CallExpression]s where we have a loop through the function's code and return to the same
     * expression in the EOG. We then have to skip the call to proceed with the next step in the
     * EOG. This method applies the filtering (based on [scope] and [sensitivities]) as usual to
     * determine valid next steps but instead of doing it once, it does the same logic twice, first
     * starting at [currentNode] with the outgoing [edges] and the current [Context] [ctx]. Then, it
     * computes the next starting node based on [nodeStart] and the remaining [edges] and, from this
     * new starting node, it calculates the possible next edges by applying [nextStep].
     *
     * Note that the [nodeStart] may not be the same node as [unwrapNextStepFromEdge] would return,
     * e.g. because a [CallExpression] is the start-node of an [Invoke] edge and may be required
     * even when following the graph with [Forward].
     */
    internal fun filterAndJump(
        currentNode: Node,
        edges: Collection<Edge<Node>>,
        ctx: Context,
        scope: AnalysisScope,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        vararg sensitivities: AnalysisSensitivity,
        nextStep: (Node) -> Collection<Edge<Node>>,
        nodeStart: (Edge<Node>) -> Node,
    ): List<Pair<Node, Context>> {
        val filteredToJump =
            filterEdges(
                currentNode = currentNode,
                edges = edges,
                ctx = ctx,
                scope = scope,
                path = path,
                loopingPaths = loopingPaths,
                sensitivities = sensitivities,
            )
        // This is a bit more tricky because we need to go to the next step when we
        // return to the CallExpression. Therefore, we make one more step.

        return filteredToJump.flatMap { (nextEdge, newCtx) ->
            // nextEdge.start is the call expression
            filterEdges(
                    currentNode = nodeStart(nextEdge),
                    edges = nextStep(nodeStart(nextEdge)),
                    ctx = newCtx,
                    scope = scope,
                    path = path,
                    loopingPaths = loopingPaths,
                    sensitivities = sensitivities,
                )
                .map { (edge, moreNewCtx) -> this.unwrapNextStepFromEdge(edge, moreNewCtx) }
        }
    }
}

/** Follow the order of the [graphToFollow] */
class Forward(graphToFollow: GraphToFollow) : AnalysisDirection(graphToFollow) {
    override fun pickNextStep(
        currentNode: Node,
        scope: AnalysisScope,
        ctx: Context,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        vararg sensitivities: AnalysisSensitivity,
    ): Collection<Pair<Node, Context>> {
        return when (graphToFollow) {
            GraphToFollow.DFG -> {

                filterEdges(
                        currentNode = currentNode,
                        edges =
                            if (Implicit in sensitivities) currentNode.nextPDGEdges
                            else currentNode.nextDFGEdges,
                        ctx = ctx,
                        scope = scope,
                        path = path,
                        loopingPaths = loopingPaths,
                        sensitivities = sensitivities,
                    )
                    .map { (edge, newCtx) -> this.unwrapNextStepFromEdge(edge, newCtx) }
            }
            GraphToFollow.EOG -> {
                val interprocedural =
                    if (currentNode is CallExpression && currentNode.invokes.isNotEmpty()) {
                        // Enter the functions/methods which are/can be invoked here
                        val called = currentNode.invokeEdges as Collection<Edge<Node>>

                        filterEdges(
                                currentNode = currentNode,
                                edges = called,
                                ctx = ctx,
                                scope = scope,
                                path = path,
                                loopingPaths = loopingPaths,
                                sensitivities = sensitivities,
                            )
                            .map { (edge, newCtx) -> this.unwrapNextStepFromEdge(edge, newCtx) }
                    } else if (currentNode is ReturnStatement || currentNode.nextEOG.isEmpty()) {
                        // Return from the functions/methods which have been invoked.
                        val returnedTo =
                            (currentNode as? FunctionDeclaration
                                    ?: currentNode.firstParentOrNull<FunctionDeclaration>()
                                    ?: (currentNode as? OverlayNode)?.underlyingNode
                                        as? FunctionDeclaration)
                                ?.calledByEdges as Collection<Edge<Node>>? ?: setOf()

                        filterAndJump(
                            currentNode = currentNode,
                            edges = returnedTo,
                            ctx = ctx,
                            scope = scope,
                            sensitivities = sensitivities,
                            nextStep = { it.nextEOGEdges },
                            path = path,
                            loopingPaths = loopingPaths,
                            nodeStart = { it.start },
                        )
                    } else {
                        filterEdges(
                                currentNode = currentNode,
                                edges = currentNode.nextEOGEdges,
                                ctx = ctx,
                                scope = scope,
                                path = path,
                                loopingPaths = loopingPaths,
                                sensitivities = sensitivities,
                            )
                            .map { (edge, newCtx) -> this.unwrapNextStepFromEdge(edge, newCtx) }
                    }

                interprocedural.ifEmpty {
                    filterEdges(
                            currentNode = currentNode,
                            edges = currentNode.nextEOGEdges,
                            ctx = ctx,
                            scope = scope,
                            path = path,
                            loopingPaths = loopingPaths,
                            sensitivities = sensitivities,
                        )
                        .map { (edge, newCtx) -> this.unwrapNextStepFromEdge(edge, newCtx) }
                }
            }
        }
    }

    override fun unwrapNextStepFromEdge(edge: Edge<Node>): Node {
        return edge.end
    }

    override fun edgeRequiresCallPush(currentNode: Node, edge: Edge<Node>): Boolean {
        return when (graphToFollow) {
            GraphToFollow.DFG -> {
                edge is ContextSensitiveDataflow && edge.callingContext is CallingContextIn
            }
            GraphToFollow.EOG -> {
                edge is Invoke && currentNode is CallExpression
            }
        }
    }

    override fun edgeRequiresCallPop(currentNode: Node, edge: Edge<Node>): Boolean {
        return when (graphToFollow) {
            GraphToFollow.DFG -> {
                edge is ContextSensitiveDataflow && edge.callingContext is CallingContextOut
            }

            GraphToFollow.EOG -> {
                edge is Invoke && (currentNode is ReturnStatement || currentNode.nextEOG.isEmpty())
            }
        }
    }
}

/** Against the order of the [graphToFollow] */
class Backward(graphToFollow: GraphToFollow) : AnalysisDirection(graphToFollow) {
    override fun pickNextStep(
        currentNode: Node,
        scope: AnalysisScope,
        ctx: Context,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        vararg sensitivities: AnalysisSensitivity,
    ): Collection<Pair<Node, Context>> {
        return when (graphToFollow) {
            GraphToFollow.DFG -> {
                filterEdges(
                        currentNode = currentNode,
                        edges =
                            if (Implicit in sensitivities) currentNode.prevPDGEdges
                            else currentNode.prevDFGEdges,
                        ctx = ctx,
                        scope = scope,
                        path = path,
                        loopingPaths = loopingPaths,
                        sensitivities = sensitivities,
                    )
                    .map { (edge, newCtx) -> this.unwrapNextStepFromEdge(edge, newCtx) }
            }

            GraphToFollow.EOG -> {
                val interprocedural =
                    if (currentNode is CallExpression && currentNode.invokes.isNotEmpty()) {
                        val returnedFrom = currentNode.invokeEdges as Collection<Edge<Node>>

                        filterEdges(
                                currentNode = currentNode,
                                edges = returnedFrom,
                                ctx = ctx,
                                scope = scope,
                                path = path,
                                loopingPaths = loopingPaths,
                                sensitivities = sensitivities,
                            )
                            .map { (edge, newCtx) -> this.unwrapNextStepFromEdge(edge, newCtx) }
                    } else if (currentNode is FunctionDeclaration) {
                        val calledBy = currentNode.calledByEdges as Collection<Edge<Node>>

                        filterAndJump(
                            currentNode = currentNode,
                            edges = calledBy,
                            ctx = ctx,
                            scope = scope,
                            sensitivities = sensitivities,
                            nextStep = { it.prevEOGEdges },
                            path = path,
                            loopingPaths = loopingPaths,
                            nodeStart = { it.start },
                        )
                    } else {
                        filterEdges(
                                currentNode = currentNode,
                                edges = currentNode.prevEOGEdges,
                                ctx = ctx,
                                scope = scope,
                                path = path,
                                loopingPaths = loopingPaths,
                                sensitivities = sensitivities,
                            )
                            .map { (edge, newCtx) -> this.unwrapNextStepFromEdge(edge, newCtx) }
                    }

                interprocedural.ifEmpty {
                    filterEdges(
                            currentNode = currentNode,
                            edges = currentNode.prevEOGEdges,
                            ctx = ctx,
                            scope = scope,
                            path = path,
                            loopingPaths = loopingPaths,
                            sensitivities = sensitivities,
                        )
                        .map { (edge, newCtx) -> this.unwrapNextStepFromEdge(edge, newCtx) }
                }
            }
        }
    }

    override fun unwrapNextStepFromEdge(edge: Edge<Node>): Node {
        return edge.start
    }

    override fun edgeRequiresCallPush(currentNode: Node, edge: Edge<Node>): Boolean {
        return when (graphToFollow) {
            GraphToFollow.DFG -> {
                edge is ContextSensitiveDataflow && edge.callingContext is CallingContextOut
            }

            GraphToFollow.EOG -> {
                edge is Invoke && currentNode is CallExpression
            }
        }
    }

    override fun edgeRequiresCallPop(currentNode: Node, edge: Edge<Node>): Boolean {
        return when (graphToFollow) {
            GraphToFollow.DFG -> {
                edge is ContextSensitiveDataflow && edge.callingContext is CallingContextIn
            }

            GraphToFollow.EOG -> {
                edge is Invoke && currentNode is FunctionDeclaration
            }
        }
    }
}

/** In and against the order of the EOG */
class Bidirectional(graphToFollow: GraphToFollow) : AnalysisDirection(graphToFollow) {
    override fun pickNextStep(
        currentNode: Node,
        scope: AnalysisScope,
        ctx: Context,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        vararg sensitivities: AnalysisSensitivity,
    ): Collection<Pair<Node, Context>> {
        TODO("Not yet implemented")
    }

    override fun unwrapNextStepFromEdge(edge: Edge<Node>): Node {
        TODO("Not yet implemented")
    }

    override fun edgeRequiresCallPush(currentNode: Node, edge: Edge<Node>): Boolean {
        TODO("Not yet implemented")
    }

    override fun edgeRequiresCallPop(currentNode: Node, edge: Edge<Node>): Boolean {
        TODO("Not yet implemented")
    }
}

/** Configures the sensitivity of the analysis. */
abstract class AnalysisSensitivity : StepSelector {
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

/** Only follow EOG edges if they are not marked as unreachable. */
object FilterUnreachableEOG : AnalysisSensitivity() {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        analysisDirection: AnalysisDirection,
        interproceduralEdgesExist: Boolean,
    ): Boolean {
        return edge !is EvaluationOrder || edge.unreachable != true
    }
}

/** Only follow full DFG edges. */
object OnlyFullDFG : AnalysisSensitivity() {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        analysisDirection: AnalysisDirection,
        interproceduralEdgesExist: Boolean,
    ): Boolean {
        return edge !is Dataflow || edge.granularity is FullDataflowGranularity
    }
}

/** Consider the calling context when following paths (e.g. based on a call stack). */
object ContextSensitive : AnalysisSensitivity() {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        analysisDirection: AnalysisDirection,
        interproceduralEdgesExist: Boolean,
    ): Boolean {
        return if (analysisDirection.edgeRequiresCallPush(currentNode, edge)) {
            // Push the call of our calling context to the stack.
            // This is for following DFG edges.
            val stack =
                if (analysisDirection is Backward) {
                    (edge as? ContextSensitiveDataflow)?.callingContext?.calls?.reversed()
                } else {
                    (edge as? ContextSensitiveDataflow)?.callingContext?.calls
                }

            stack?.forEach { ctx.callStack.push(it) }
                ?:
                // This is for following the EOG
                (currentNode as? CallExpression)?.let { ctx.callStack.push(it) }
            true
        } else if (analysisDirection.edgeRequiresCallPop(currentNode, edge)) {
            // We are only interested in outgoing edges from our current
            // "call-in", i.e., the call expression that is on the stack.
            ctx.callStack.isEmpty() ||
                (edge as? ContextSensitiveDataflow)?.callingContext?.calls?.all {
                    ctx.callStack.popIfOnTop(it)
                } == true ||
                ((edge as? Invoke)?.start as? CallExpression)?.let {
                    ctx.callStack.popIfOnTop(it)
                } == true
        } else {
            true
        }
    }
}

/**
 * Differentiate between fields, attributes, known keys or known indices of objects. This does not
 * include computing possible indices or keys if they are not given as a literal.
 */
object FieldSensitive : AnalysisSensitivity() {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        analysisDirection: AnalysisDirection,
        interproceduralEdgesExist: Boolean,
    ): Boolean {
        return if (edge is Dataflow) {
            if (
                currentNode is InitializerListExpression &&
                    analysisDirection.unwrapNextStepFromEdge(edge) in currentNode.initializers &&
                    edge.granularity is IndexedDataflowGranularity
            ) {
                // currentNode is the ILE, it is the child and the next (e.g. read
                // from).
                // We try to pop from the stack and only select the elements with the
                // matching index.
                ctx.indexStack.isEmpty() ||
                    ctx.indexStack.popIfOnTop(edge.granularity as IndexedDataflowGranularity)
            } else if (
                analysisDirection.unwrapNextStepFromEdge(edge) is InitializerListExpression &&
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
object Implicit : AnalysisSensitivity() {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        path: List<Pair<Node, Context>>,
        loopingPaths: MutableList<NodePath>,
        analysisDirection: AnalysisDirection,
        interproceduralEdgesExist: Boolean,
    ): Boolean {
        return true
    }
}
