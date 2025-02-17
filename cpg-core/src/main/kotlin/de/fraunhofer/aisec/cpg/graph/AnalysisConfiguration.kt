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

import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edges.flows.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.edges.flows.IndexedDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.Invoke
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression

interface StepSelector {
    /**
     * Returns `true` if the given [edge] will be followed giving the provided [ctx] under the
     * provided configuration.
     */
    fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        analysisDirection: AnalysisDirection,
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
        analysisDirection: AnalysisDirection,
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
        analysisDirection: AnalysisDirection,
    ): Boolean {
        // Follow the edge if we're still in the maxSteps range and (if maxCallDepth is null or the
        // call stack is not deeper yet)
        return (this.maxSteps == null || ctx.steps < maxSteps) &&
            (maxCallDepth == null || ctx.callStack.depth < maxCallDepth)
    }
}

enum class GraphToFollow {
    DFG,
    EOG,
}

/** Determines in which direction we follow the edges. */
sealed class AnalysisDirection {
    abstract fun pickNextStep(
        currentNode: Node,
        scope: AnalysisScope,
        graphToFollow: GraphToFollow,
        ctx: Context,
    ): Collection<Edge<Node>>

    abstract fun unwrapNextStepFromEdge(edge: Edge<Node>): Node

    abstract fun edgeRequiresCallPush(currentNode: Node, edge: Edge<Node>): Boolean

    abstract fun edgeRequiresCallPop(currentNode: Node, edge: Edge<Node>): Boolean
}

/** Follow the order of the EOG */
class Forward() : AnalysisDirection() {
    override fun pickNextStep(
        currentNode: Node,
        scope: AnalysisScope,
        graphToFollow: GraphToFollow,
        ctx: Context,
    ): Collection<Edge<Node>> {
        return if (graphToFollow == GraphToFollow.DFG) {
            currentNode.nextDFGEdges
        } else {
            // TODO: This is a bit more tricky because we need to go to nextEOGEdges when we return
            // to the CallExpression.
            val interprocedural =
                if (currentNode is CallExpression && currentNode.invokes.isNotEmpty()) {
                    currentNode.invokeEdges as Collection<Edge<Node>>
                } else if (currentNode is ReturnStatement || currentNode.nextEOG.isEmpty()) {
                    (currentNode as? FunctionDeclaration
                            ?: currentNode.firstParentOrNull<FunctionDeclaration>())
                        ?.calledByEdges as Collection<Edge<Node>>? ?: setOf()
                } else {
                    currentNode.nextEOGEdges
                }

            if (interprocedural.any { scope.followEdge(currentNode, it, ctx, this) }) {
                interprocedural
            } else {
                currentNode.nextEOGEdges
            }
        }
    }

    override fun unwrapNextStepFromEdge(edge: Edge<Node>): Node {
        return edge.end
    }

    override fun edgeRequiresCallPush(currentNode: Node, edge: Edge<Node>): Boolean {
        return (edge is ContextSensitiveDataflow && edge.callingContext is CallingContextIn) ||
            (edge is Invoke && currentNode is CallExpression)
    }

    override fun edgeRequiresCallPop(currentNode: Node, edge: Edge<Node>): Boolean {
        return (edge is ContextSensitiveDataflow && edge.callingContext is CallingContextOut) ||
            (edge is Invoke && (currentNode is ReturnStatement || currentNode.nextEOG.isEmpty()))
    }
}

/** Against the order of the EOG */
class Backward() : AnalysisDirection() {
    override fun pickNextStep(
        currentNode: Node,
        scope: AnalysisScope,
        graphToFollow: GraphToFollow,
        ctx: Context,
    ): Collection<Edge<Node>> {
        return if (graphToFollow == GraphToFollow.DFG) {
            currentNode.prevDFGEdges
        } else {
            val interprocedural =
                if (currentNode is CallExpression && currentNode.invokes.isNotEmpty()) {
                    currentNode.invokeEdges as Collection<Edge<Node>>
                } else if (currentNode is FunctionDeclaration) {
                    currentNode.calledByEdges as Collection<Edge<Node>>
                } else {
                    currentNode.prevEOGEdges
                }
            if (interprocedural.any { scope.followEdge(currentNode, it, ctx, this) }) {
                interprocedural
            } else {
                currentNode.prevEOGEdges
            }
        }
    }

    override fun unwrapNextStepFromEdge(edge: Edge<Node>): Node {
        return edge.start
    }

    override fun edgeRequiresCallPush(currentNode: Node, edge: Edge<Node>): Boolean {
        return (edge is ContextSensitiveDataflow && edge.callingContext is CallingContextOut) ||
            (edge is Invoke && currentNode is CallExpression)
    }

    override fun edgeRequiresCallPop(currentNode: Node, edge: Edge<Node>): Boolean {
        return (edge is ContextSensitiveDataflow && edge.callingContext is CallingContextIn) ||
            (edge is Invoke && currentNode is FunctionDeclaration)
    }
}

/** In and against the order of the EOG */
class Bidirectional() : AnalysisDirection() {
    override fun pickNextStep(
        currentNode: Node,
        scope: AnalysisScope,
        graphToFollow: GraphToFollow,
        ctx: Context,
    ): Collection<Edge<Node>> {
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

class FilterUnreachableEOG() : AnalysisSensitivity() {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        analysisDirection: AnalysisDirection,
    ): Boolean {
        return edge !is EvaluationOrder || edge.unreachable != true
    }
}

/** Consider the calling context when following paths (e.g. based on a call stack). */
class ContextSensitive() : AnalysisSensitivity() {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        analysisDirection: AnalysisDirection,
    ): Boolean {
        return if (analysisDirection.edgeRequiresCallPush(currentNode, edge)) {
            // Push the call of our calling context to the stack.
            // This is for following DFG edges.
            (edge as? ContextSensitiveDataflow)?.callingContext?.call?.let {
                ctx.callStack.push(it)
            }
                ?:
                // This is for following the EOG
                (currentNode as? CallExpression)?.let { ctx.callStack.push(it) }
            true
        } else if (analysisDirection.edgeRequiresCallPop(currentNode, edge)) {
            // We are only interested in outgoing edges from our current
            // "call-in", i.e., the call expression that is on the stack.
            ctx.callStack.isEmpty() ||
                (edge as? ContextSensitiveDataflow)?.callingContext?.call?.let {
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
class FieldSensitive() : AnalysisSensitivity() {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        analysisDirection: AnalysisDirection,
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
class Implicit() : AnalysisSensitivity() {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        analysisDirection: AnalysisDirection,
    ): Boolean {
        TODO("Not yet implemented. Actually requires following PDG instead of DFG edges...")
    }
}
