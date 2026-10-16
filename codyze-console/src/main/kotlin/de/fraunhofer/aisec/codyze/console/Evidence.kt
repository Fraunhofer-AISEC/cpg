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
package de.fraunhofer.aisec.codyze.console

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.serialization.GraphSliceJSON
import de.fraunhofer.aisec.cpg.serialization.NodePaths
import de.fraunhofer.aisec.cpg.serialization.NodeSlice
import de.fraunhofer.aisec.cpg.serialization.enclosingStatement
import de.fraunhofer.aisec.cpg.serialization.mergeGraphs
import de.fraunhofer.aisec.cpg.serialization.pathsGraph
import de.fraunhofer.aisec.cpg.serialization.slice
import kotlinx.serialization.Serializable

/**
 * A step of the agent whose result is part of the evidence graph: the paths or the slice a tool
 * returned, as it returned them.
 */
@Serializable
data class EvidenceStepJSON(
    val step: Int,
    val paths: NodePaths? = null,
    val slice: NodeSlice? = null,
)

/** The steps of a question to the agent and the nodes cited in its answer. */
@Serializable
data class EvidenceRequestJSON(val steps: List<EvidenceStepJSON>, val cited: List<String>)

/**
 * The evidence of a question to the agent as one graph of statements: the union of the paths and
 * slices of its steps.
 *
 * @property steps The steps that found each statement, by the ID of the statement.
 * @property cited The statements of the nodes the answer cites, also those outside of the graph.
 * @property statementOf The statement of each node the answer cites, by the ID of the node.
 */
@Serializable
data class EvidenceGraphJSON(
    val graph: GraphSliceJSON,
    val steps: Map<String, List<Int>>,
    val cited: List<String>,
    val statementOf: Map<String, String>,
)

/**
 * The evidence graph of [request]. The slices are computed again from their parameters, so that
 * they have the format of the slices in the console; [findNode] finds the nodes by their ID.
 */
fun evidenceGraph(request: EvidenceRequestJSON, findNode: (String) -> Node?): EvidenceGraphJSON {
    val graphs =
        request.steps.mapNotNull { step ->
            val graph =
                step.slice?.let { s ->
                    findNode(s.start.nodeId)?.let {
                        slice(it, s.direction, s.hops, s.graph, s.interprocedural)
                    }
                }
                    ?: step.paths?.let { p ->
                        val nodes = p.paths.map { path -> path.mapNotNull { findNode(it.nodeId) } }
                        pathsGraph(nodes.filter { it.isNotEmpty() }, p.kind)
                    }
            graph?.let { step.step to it }
        }
    val steps = mutableMapOf<String, MutableList<Int>>()
    for ((step, graph) in graphs) {
        graph.nodes.forEach { steps.getOrPut(it.id) { mutableListOf() }.add(step) }
    }
    val statementOf =
        request.cited
            .mapNotNull { id ->
                findNode(id)?.enclosingStatement()?.id?.toString()?.let { id to it }
            }
            .toMap()
    return EvidenceGraphJSON(
        graph = mergeGraphs(graphs.map { it.second }),
        steps = steps.mapValues { (_, s) -> s.distinct().sorted() },
        cited = statementOf.values.distinct(),
        statementOf = statementOf,
    )
}
