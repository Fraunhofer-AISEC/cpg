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
package de.fraunhofer.aisec.cpg.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.Persistable
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import java.io.File

data class JsonNode(val id: String, val labels: Set<String>, val properties: Map<String, Any?>)

data class JsonEdge(
    val id: Long,
    val type: String,
    val startNode: String,
    val endNode: String,
    val properties: Map<String, Any?>,
)

data class JsonGraph(val nodes: List<JsonNode>, val edges: List<JsonEdge>)

/** Serialise the cpg using the OGM as nodes and edge list. */
fun TranslationResult.createJsonGraph(): JsonGraph {
    val astNodes = this@createJsonGraph.nodes
    val connected = astNodes.flatMap { it.connectedNodes }.toSet()
    val nodes = (astNodes + connected).distinct()

    log.info(
        "Persisting {} nodes: AST nodes ({}), other nodes ({})",
        nodes.size,
        astNodes.size,
        connected.size,
    )
    val nodesJs =
        nodes.map {
            JsonNode(
                it.id.toString(),
                it::class.labels,
                it.properties().filter { prop -> prop.key != "id" },
            )
        }

    val relationships = nodes.collectRelationships()
    log.info("Persisting {} relationships", relationships.size)
    val relationshipsJs =
        relationships.mapIndexed { idx, rel ->
            JsonEdge(
                idx.toLong(),
                rel["type"] as String,
                rel["startId"] as String,
                rel["endId"] as String,
                rel.filterKeys { !arrayOf("type", "startId", "endId").contains(it) },
            )
        }

    return JsonGraph(nodesJs, relationshipsJs)
}

/**
 * Persists the current [TranslationResult] into a json file as a list of nodes and edges. The json
 * format is given by [JsonGraph]. Nodes and edges are connected via the [Node.id].
 *
 * This method performs the following actions:
 * - Logs information about the number and categories of nodes (e.g., AST nodes, scopes, types,
 *   languages) and edges that are being persisted.
 * - Collects nodes that include AST nodes, scopes, types, and languages, as well as all associated
 *   edges.
 * - Persists the collected nodes and edges.
 * - Persists additional relationships between nodes, such as those related to types, scopes, and
 *   languages.
 * - Utilizes a benchmarking mechanism to measure and log the time taken to complete the persistence
 *   operation.
 *
 * This method relies on the following context and properties:
 * - The [TranslationResult.finalCtx] property for accessing the scope manager, type manager, and
 *   configuration.
 */
fun TranslationResult.persistJson(path: File) {
    val b = Benchmark(Persistable::class.java, "Persisting translation result to json")

    val graphJs = this@persistJson.createJsonGraph()

    val objectMapper = ObjectMapper()
    objectMapper.writeValue(path, graphJs)
    log.info(
        "Exported ${graphJs.nodes.size} Nodes and ${graphJs.edges.size} Edges to json file ${path.absoluteFile}"
    )

    b.stop()
}
