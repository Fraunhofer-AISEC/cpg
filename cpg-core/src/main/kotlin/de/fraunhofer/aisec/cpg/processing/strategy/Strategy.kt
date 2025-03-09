/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.processing.strategy

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.DataflowNode
import de.fraunhofer.aisec.cpg.graph.EvaluatedNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.ast.*
import de.fraunhofer.aisec.cpg.graph.edges.astEdges
import de.fraunhofer.aisec.cpg.graph.edges.flows.*
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Strategies (iterators) for traversing graphs to be used by visitors. */
object Strategy {

    val log: Logger = LoggerFactory.getLogger(Strategy::class.java)

    /**
     * Do not traverse any nodes.
     *
     * @param x
     * @return
     */
    fun NO_STRATEGY(x: Node): Iterator<Node> {
        return Collections.emptyIterator()
    }

    /**
     * Traverse Evaluation Order Graph in forward direction.
     *
     * @param x Current node in EOG.
     * @return Iterator over successors.
     */
    fun EOG_FORWARD(x: EvaluatedNode): Iterator<AstNode> {
        return x.nextEOG.filterIsInstance<AstNode>().iterator()
    }

    /** A strategy to traverse the EOG in forward direction, but only if the edge is reachable. */
    fun REACHABLE_EOG_FORWARD(x: EvaluatedNode): Iterator<AstNode> {
        return x.nextEOGEdges
            .filter { !it.unreachable }
            .map { it.end }
            .filterIsInstance<AstNode>()
            .iterator()
    }

    fun COMPONENTS_LEAST_IMPORTS(x: TranslationResult): Iterator<Component> {
        return x.componentDependencies?.sorted?.iterator()
            ?: x.components.iterator().also {
                log.warn(
                    "Strategy for components with least import dependencies was requested, but no import dependency information is available."
                )
                log.warn("Please make sure that the ImportResolver pass was run successfully.")
            }
    }

    fun TRANSLATION_UNITS_LEAST_IMPORTS(x: Component): Iterator<TranslationUnitDeclaration> {
        return x.translationUnitDependencies?.sorted?.iterator()
            ?: x.translationUnits.iterator().also {
                log.warn(
                    "Strategy for translation units with least import dependencies was requested, but no import dependency information is available."
                )
                log.warn("Please make sure that the ImportResolver pass was run successfully.")
            }
    }

    /**
     * Traverse Evaluation Order Graph in backward direction.
     *
     * @param x Current node in EOG.
     * @return Iterator over successors.
     */
    fun EOG_BACKWARD(x: EvaluatedNode): Iterator<EvaluatedNode> {
        return x.prevEOG.iterator()
    }

    /**
     * Traverse Data Flow Graph in forward direction.
     *
     * @param x Current node in DFG.
     * @return Iterator over successors.
     */
    fun DFG_FORWARD(x: Node): Iterator<DataflowNode> {
        if (x !is DataflowNode) {
            return emptyList<DataflowNode>().iterator()
        }

        return x.nextDFG.iterator()
    }

    /**
     * Traverse Data Flow Graph in backward direction.
     *
     * @param x Current node in DFG.
     * @return Iterator over predecessor.
     */
    fun DFG_BACKWARD(x: Node): Iterator<Node> {
        if (x !is DataflowNode) {
            return emptyList<DataflowNode>().iterator()
        }

        return x.prevDFG.iterator()
    }

    /**
     * Traverse AST in forward direction.
     *
     * @param x
     * @return
     */
    fun AST_FORWARD(x: AstNode): Iterator<AstNode> {
        return x.astChildren.iterator()
    }

    /**
     * Traverse Data Flow Graph in forward direction.
     *
     * @param x Current node in DFG.
     * @return Iterator over successor edges.
     */
    fun DFG_EDGES_FORWARD(x: Node): Iterator<Dataflow> {
        if (x !is DataflowNode) {
            return emptyList<Dataflow>().iterator()
        }

        return x.nextDFGEdges.iterator()
    }

    /**
     * Traverse Data Flow Graph in backward direction.
     *
     * @param x Current node in DFG.
     * @return Iterator over predecessor edges.
     */
    fun DFG_EDGES_BACKWARD(x: Node): Iterator<Dataflow> {
        if (x !is DataflowNode) {
            return emptyList<Dataflow>().iterator()
        }

        return x.prevDFGEdges.iterator()
    }

    /**
     * Traverse [EvaluationOrder] edges in forward direction.
     *
     * @param x Current node in EOG.
     * @return Iterator over successor edges.
     */
    fun EOG_EDGES_FORWARD(x: Node): Iterator<EvaluationOrder> {
        if (x !is EvaluatedNode) {
            return emptyList<EvaluationOrder>().iterator()
        }

        return x.nextEOGEdges.iterator()
    }

    /**
     * Traverse [EvaluationOrder] edges in backward direction.
     *
     * @param x Current node in EOG.
     * @return Iterator over predecessor edges.
     */
    fun EOG_EDGES_BACKWARD(x: Node): Iterator<EvaluationOrder> {
        if (x !is EvaluatedNode) {
            return emptyList<EvaluationOrder>().iterator()
        }

        return x.prevEOGEdges.iterator()
    }

    /**
     * Traverse [AstEdge] edges in forward direction.
     *
     * @param x Current node in EOG.
     * @return Iterator over successor edges.
     */
    fun AST_EDGES_FORWARD(x: Node): Iterator<AstEdge<out AstNode>> {
        return if (x is AstNode) x.astEdges.iterator()
        else emptyList<AstEdge<out AstNode>>().iterator()
    }

    /**
     * Traverse [AstEdge] edges in forward direction.
     *
     * @param x Current node in EOG.
     * @return Iterator over successor edges.
     */
    fun AST_EDGES_BACKWARD(x: Node): Iterator<AstEdge<out AstNode>> {
        if (x !is AstNode) {
            return emptyList<AstEdge<out AstNode>>().iterator()
        }

        return (x.astParent?.astEdges?.filter { it.end == x } ?: emptyList()).iterator()
    }
}
