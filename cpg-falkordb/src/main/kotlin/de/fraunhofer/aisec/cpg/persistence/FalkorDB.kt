/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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

import com.falkordb.Driver
import com.falkordb.FalkorDB
import com.falkordb.Graph
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import org.slf4j.LoggerFactory
import redis.clients.jedis.exceptions.JedisDataException

internal val log = LoggerFactory.getLogger("FalkorDBPersistence")

/**
 * The keys of a relationship map that describe the relationship itself rather than one of its
 * properties, see [collectRelationships].
 */
private val relationshipStructuralKeys = setOf("startId", "endId", "type")

/**
 * A [GraphDatabaseBackend] that writes the CPG into a [FalkorDB](https://www.falkordb.com) graph.
 *
 * FalkorDB speaks Cypher, but it does not offer a procedure library such as APOC. Labels and
 * relationship types can therefore not be supplied dynamically through a parameter. Instead of
 * creating every node with its own query, we group the nodes of a chunk by their *set of labels*
 * and the relationships by their *type*. Each group is then written with a single `UNWIND` query
 * whose body is constant, which lets FalkorDB reuse the cached execution plan across chunks.
 *
 * @param graph the graph to write to, only closed if [driver] is supplied as well
 * @param driver the driver the [graph] was obtained from, closed together with this backend, or
 *   `null` if the caller manages the driver lifecycle
 */
class FalkorDBDatabase(private val graph: Graph, private val driver: Driver? = null) :
    GraphDatabaseBackend {

    /**
     * FalkorDB inlines query parameters into the query text (see [CypherLiteral]), so a chunk
     * becomes a single (large) string. We therefore use noticeably smaller chunks than the default
     * to keep the individual queries — and the memory needed to build them — reasonable.
     */
    override val nodeChunkSize: Int = 1000

    override val relationshipChunkSize: Int = 5000

    override fun purgeDatabase() {
        try {
            graph.deleteGraph()
        } catch (e: JedisDataException) {
            // Deleting a graph that was never created reports an error on an "empty key". For our
            // purposes there is simply nothing to purge, so we can safely continue.
            if (e.message?.contains("empty key") != true) {
                throw e
            }
            log.debug("Nothing to purge, the graph does not exist yet")
        }
    }

    override fun createIndexes() {
        try {
            graph.query("CREATE INDEX FOR (n:Node) ON (n.id)")
        } catch (e: JedisDataException) {
            // FalkorDB has no "CREATE INDEX IF NOT EXISTS", it reports an error instead. Since we
            // only ever create this one index, an existing index is exactly what we want.
            if (e.message?.contains("already indexed") != true) {
                throw e
            }
        }
    }

    override fun persistNodeChunk(nodes: List<Node>) {
        // Group by the set of labels, so that all nodes in a group can be created by the same
        // query. This replaces what APOC's dynamic node creation does for Neo4j.
        for ((labels, group) in nodes.groupBy { it::class.labels }) {
            val labelSelector = labels.joinToString(":") { CypherLiteral.renderIdentifier(it) }
            val properties = group.map { it.properties() }

            graph.query(
                CypherLiteral.withParameters(
                    mapOf("props" to properties),
                    "UNWIND \$props AS map CREATE (n:$labelSelector) SET n = map",
                )
            )
        }
    }

    override fun persistRelationshipChunk(relationships: List<Map<String, Any?>>) {
        // A relationship type cannot be parameterized either, so we group by it as well.
        for ((type, group) in relationships.groupBy { it["type"] }) {
            if (type !is String) {
                log.error("Skipping {} relationships without a type", group.size)
                continue
            }

            val rows =
                group.map { relationship ->
                    mapOf(
                        "startId" to relationship["startId"],
                        "endId" to relationship["endId"],
                        "properties" to
                            relationship.filterKeys { it !in relationshipStructuralKeys },
                    )
                }

            graph.query(
                CypherLiteral.withParameters(
                    mapOf("props" to rows),
                    "UNWIND \$props AS map " +
                        "MATCH (s:Node {id: map.startId}) " +
                        "MATCH (e:Node {id: map.endId}) " +
                        "CREATE (s)-[r:${CypherLiteral.renderIdentifier(type)}]->(e) " +
                        "SET r = map.properties",
                )
            )
        }
    }

    /**
     * Closes the resources that this backend owns. If no [driver] was handed to us, the caller
     * manages the lifecycle of the [graph] (and of the driver it originates from), so nothing is
     * closed and the [graph] stays usable afterwards.
     */
    override fun close() {
        driver?.let {
            graph.close()
            it.close()
        }
    }
}

/**
 * Connects to a FalkorDB instance and returns the [Driver] as well as the [Graph] identified by
 * [graphName].
 *
 * @param host the host address of the FalkorDB instance
 * @param port the port of the FalkorDB instance
 * @param username the username, or `null` if the instance does not require authentication
 * @param password the password, or `null` if the instance does not require authentication
 * @param graphName the name of the graph key to store the CPG under
 * @throws IllegalArgumentException if only one of [username] and [password] is supplied
 */
fun connectToFalkorDB(
    host: String = FalkorDBConnectionDefaults.HOST,
    port: Int = FalkorDBConnectionDefaults.PORT,
    username: String? = null,
    password: String? = null,
    graphName: String = FalkorDBConnectionDefaults.GRAPH,
): Pair<Driver, Graph> {
    // Silently connecting without authentication when only one half of the credentials is set
    // would turn a typo into a confusing "graph not found" further down the line.
    require((username == null) == (password == null)) {
        "Either both username and password have to be supplied, or neither of them."
    }

    val driver =
        if (username != null && password != null) {
            FalkorDB.driver(host, port, username, password)
        } else {
            FalkorDB.driver(host, port)
        }

    return driver to driver.graph(graphName)
}

/**
 * Translates this [TranslationResult] into a graph and persists it into a FalkorDB instance.
 *
 * @param noPurgeDb if `true`, the graph is not deleted before the new CPG is written
 * @param host the host address of the FalkorDB instance
 * @param port the port of the FalkorDB instance
 * @param username the username, or `null` if the instance does not require authentication
 * @param password the password, or `null` if the instance does not require authentication
 * @param graphName the name of the graph key to store the CPG under
 */
fun TranslationResult.pushToFalkorDB(
    noPurgeDb: Boolean = false,
    host: String = FalkorDBConnectionDefaults.HOST,
    port: Int = FalkorDBConnectionDefaults.PORT,
    username: String? = null,
    password: String? = null,
    graphName: String = FalkorDBConnectionDefaults.GRAPH,
) {
    val (driver, graph) = connectToFalkorDB(host, port, username, password, graphName)
    FalkorDBDatabase(graph, driver).use { db -> this.persist(db, purgeDb = !noPurgeDb) }
}
