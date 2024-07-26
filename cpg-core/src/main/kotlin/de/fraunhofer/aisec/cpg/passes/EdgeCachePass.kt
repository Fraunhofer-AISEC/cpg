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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

enum class EdgeType {
    AST
}

class Edge(val source: Node, val target: Node, val type: EdgeType)

/** The edges cache. */
object Edges {
    private val fromMap: MutableMap<Node, MutableList<Edge>> = HashMap()
    private val toMap: MutableMap<Node, MutableList<Edge>> = HashMap()

    fun add(edge: Edge) {
        if (fromMap[edge.source] == null) {
            fromMap[edge.source] = ArrayList()
        }

        if (toMap[edge.target] == null) {
            toMap[edge.target] = ArrayList()
        }

        fromMap[edge.source]?.add(edge)
        toMap[edge.target]?.add(edge)
    }

    fun to(node: Node, type: EdgeType): List<Edge> {
        return toMap.computeIfAbsent(node) { mutableListOf() }.filter { it.type == type }
    }

    fun from(node: Node, type: EdgeType): List<Edge> {
        return fromMap.computeIfAbsent(node) { mutableListOf() }.filter { it.type == type }
    }

    fun size(): Int {
        return fromMap.size
    }

    fun clear() {
        toMap.clear()
        fromMap.clear()
    }
}

/**
 * This pass creates a simple cache of AST edges to quickly traverse them in different directions.
 * Mainly used for the [Node.astChildren] extension.
 *
 * The cache itself is stored in the [Edges] object.
 *
 * Note: Some passes that run after this pass might influence AST parent / child relationship.
 */
@DependsOn(ReplaceCallCastPass::class)
class EdgeCachePass(ctx: TranslationContext) : ComponentPass(ctx) {
    override fun accept(component: Component) {
        Edges.clear()

        for (tu in component.translationUnits) {
            tu.accept(
                Strategy::AST_FORWARD,
                object : IVisitor<Node>() {
                    override fun visit(t: Node) {
                        visitAST(t)

                        super.visit(t)
                    }
                }
            )
        }
    }

    protected fun visitAST(n: Node) {
        for (node in SubgraphWalker.getAstChildren(n)) {
            val edge = Edge(n, node, EdgeType.AST)
            Edges.add(edge)
        }
    }

    override fun cleanup() {
        // nothing to do
    }
}

val Node.astParent: Node?
    get() {
        return Edges.to(this, EdgeType.AST).firstOrNull()?.source
    }
