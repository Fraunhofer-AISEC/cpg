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

import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edges.flows.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.IndexedDataflowGranularity
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
        return this.maxSteps == null || maxSteps <= ctx.steps
    }
}

/**
 * Only intraprocedural analysis. [maxSteps] defines the total number of steps we will follow in the
 * graph (unlimited depth if `null`).
 */
class Intraprocedural(maxSteps: Int? = null) : AnalysisScope(maxSteps) {
    override fun followEdge(
        currentNode: Node,
        edge: Edge<Node>,
        ctx: Context,
        analysisDirection: AnalysisDirection?,
    ): Boolean {
        // Follow the edge if we're still in the maxSteps range and not an edge across function
        // boundaries.
        return (this.maxSteps == null || ctx.steps < maxSteps) && edge !is ContextSensitiveDataflow
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
        analysisDirection: AnalysisDirection?,
    ): Boolean {
        // Follow the edge if we're still in the maxSteps range and (if maxCallDepth is null or the
        // call stack is not deeper yet)
        return (this.maxSteps == null || ctx.steps < maxSteps) &&
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
        return if (edge is ContextSensitiveDataflow && analysisDirection is Forward) {
            // Forward analysis
            if (edge.callingContext is CallingContextIn) {
                // Push the call of our calling context to the stack
                ctx.callStack.push((edge.callingContext as CallingContextIn).call)
                true
            } else if (edge.callingContext is CallingContextOut) {
                // We are only interested in outgoing edges from our current
                // "call-in", i.e., the call expression that is on the stack.
                ctx.callStack.isEmpty() ||
                    ctx.callStack.popIfOnTop((edge.callingContext as CallingContextOut).call)
            } else {
                true
            }
        } else if (edge is ContextSensitiveDataflow && analysisDirection is Backward) {
            // Backward analysis
            if (edge.callingContext is CallingContextOut) {
                // Push the call of our calling context to the stack
                ctx.callStack.push((edge.callingContext as CallingContextOut).call)
                true
            } else if (edge.callingContext is CallingContextIn) {
                // We are only interested in outgoing edges from our current
                // "call-in", i.e., the call expression that is on the stack.
                ctx.callStack.isEmpty() ||
                    ctx.callStack.popIfOnTop((edge.callingContext as CallingContextIn).call)
            } else {
                true
            }
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
        analysisDirection: AnalysisDirection?,
    ): Boolean {
        return if (edge is Dataflow) {
            if (
                currentNode is InitializerListExpression &&
                    analysisDirection?.unwrapNextStepFromEdge(edge) in currentNode.initializers &&
                    edge.granularity is IndexedDataflowGranularity
            ) {
                // currentNode is the ILE, it is the child and the next (e.g. read
                // from).
                // We try to pop from the stack and only select the elements with the
                // matching index.
                ctx.indexStack.isEmpty() ||
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
