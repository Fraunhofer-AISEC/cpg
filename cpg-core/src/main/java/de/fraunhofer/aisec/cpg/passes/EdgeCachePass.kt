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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

enum class EdgeType {
    AST,
    DFG,
    EOG
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

fun Node.followNextEOG(predicate: (PropertyEdge<*>) -> Boolean): List<PropertyEdge<*>>? {
    val path = mutableListOf<PropertyEdge<*>>()

    for (edge in this.nextEOGEdges) {
        val target = edge.end

        path.add(edge)

        if (predicate(edge)) {
            return path
        }

        val subPath = target.followNextEOG(predicate)
        if (subPath != null) {
            path.addAll(subPath)

            return path
        }
    }

    return null
}

/**
 * This pass creates a simple cache of commonly used edges, such as DFG or AST to quickly traverse
 * them in different directions.
 *
 * The cache itself is stored in the [Edges] object.
 */
class EdgeCachePass : Pass() {
    override fun accept(result: TranslationResult) {
        Edges.clear()

        for (tu in result.translationUnits) {
            tu.accept(
                Strategy::AST_FORWARD,
                object : IVisitor<Node>() {
                    override fun visit(n: Node) {
                        visitAST(n)
                        visitDFG(n)
                        visitEOG(n)

                        super.visit(n)
                    }
                }
            )
        }
    }

    private fun visitAST(n: Node) {
        for (node in SubgraphWalker.getAstChildren(n)) {
            val edge = Edge(n, node, EdgeType.AST)
            Edges.add(edge)
        }
    }

    private fun visitDFG(n: Node) {
        for (dfg in n.prevDFG) {
            val edge = Edge(dfg, n, EdgeType.DFG)
            Edges.add(edge)
        }

        for (dfg in n.nextDFG) {
            val edge = Edge(n, dfg, EdgeType.DFG)
            Edges.add(edge)
        }
    }

    private fun visitEOG(n: Node) {
        for (eog in n.prevEOG) {
            val edge = Edge(eog, n, EdgeType.EOG)
            Edges.add(edge)
        }

        for (eog in n.nextEOG) {
            val edge = Edge(n, eog, EdgeType.EOG)
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
