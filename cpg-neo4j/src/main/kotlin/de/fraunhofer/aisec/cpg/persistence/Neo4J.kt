/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.Persistable
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import java.net.ConnectException
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Session
import org.slf4j.LoggerFactory

/**
 * Defines the number of edges to be processed in a single batch operation during persistence.
 *
 * This constant is used for chunking collections of edges into smaller groups to optimize write
 * performance and reduce memory usage when interacting with the Neo4j database. Specifically, it
 * determines the maximum size of each chunk of edges to be persisted in one batch operation.
 */
const val edgeChunkSize = 10000

/**
 * Specifies the maximum number of nodes to be processed in a single chunk during persistence
 * operations.
 *
 * This constant is used to control the size of batches when persisting a list of nodes to the
 * database. Breaking the list into chunks of this size helps improve performance and memory
 * efficiency during database writes. Each chunk is handled individually, ensuring that operations
 * remain manageable even for large data sets.
 */
const val nodeChunkSize = 10000

internal val log = LoggerFactory.getLogger("Persistence")

internal typealias Relationship = Map<String, Any?>

/**
 * This function creates a new Neo4j session, optionally purges the database, and persists the
 * current [TranslationResult] into the database using the [persistNeo4j] function (which requires a
 * session context often not available at the call-site).
 *
 * @param noPurgeDb A boolean flag indicating whether to skip the database purge step. If set to
 *   true, the existing data in the database will not be deleted before persisting the new data.
 * @param protocol The protocol to use for connecting to the Neo4j database
 * @param host The host address of the Neo4j database
 * @param port The port number for the Neo4j database connection
 * @param neo4jUsername The username for authenticating with the Neo4j database
 * @param neo4jPassword The password for authenticating with the Neo4j database
 */
fun TranslationResult.pushToNeo4j(
    noPurgeDb: Boolean = false,
    protocol: String = Neo4jConnectionDefaults.PROTOCOL,
    host: String = Neo4jConnectionDefaults.HOST,
    port: Int = Neo4jConnectionDefaults.PORT,
    neo4jUsername: String = Neo4jConnectionDefaults.USERNAME,
    neo4jPassword: String = Neo4jConnectionDefaults.PASSWORD,
) {
    val session: Session = connect(protocol, host, port, neo4jUsername, neo4jPassword)
    with(session) {
        if (!noPurgeDb) executeWrite { tx -> tx.run("MATCH (n) DETACH DELETE n").consume() }
        this@pushToNeo4j.persistNeo4j()
    }
    session.close()
}

/**
 * Persists the current [TranslationResult] into a graph database.
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
 * - A [Session] context to perform persistence actions.
 */
context(_: Session)
fun TranslationResult.persistNeo4j() {
    val b = Benchmark(Persistable::class.java, "Persisting translation result")

    val astNodes = this@persistNeo4j.nodes
    val connected = astNodes.flatMap { it.connectedNodes }.toSet()
    val nodes = (astNodes + connected).distinct()

    log.info(
        "Persisting {} nodes: AST nodes ({}), other nodes ({})",
        nodes.size,
        astNodes.size,
        connected.size,
    )
    nodes.persistNeo4j()

    val relationships = nodes.collectRelationships()

    log.info("Persisting {} relationships", relationships.size)
    relationships.persistNeo4j()

    b.stop()
}

/**
 * Persists a list of nodes into a database in chunks for efficient processing.
 *
 * This function utilizes the surrounding [Session] context to execute the database write
 * operations. Nodes are processed in chunks of size determined by [nodeChunkSize], and each chunk
 * is persisted using Cypher queries. The process is benchmarked using the [Benchmark] utility.
 *
 * The function generates a list of properties for the nodes, which includes their labels and other
 * properties. These properties are used to construct Cypher queries that create nodes in the
 * database with the given labels and properties.
 *
 * The function uses the APOC library for creating nodes in the database. For each node in the list,
 * it extracts the labels and properties and executes the Cypher query to persist the node.
 */
context(session: Session)
private fun List<Node>.persistNeo4j() {
    this.chunked(nodeChunkSize).map { chunk ->
        val b = Benchmark(Persistable::class.java, "Persisting chunk of ${chunk.size} nodes")
        val params =
            mapOf("props" to chunk.map { mapOf("labels" to it::class.labels) + it.properties() })
        session.executeWrite { tx ->
            tx.run(
                    $$"""
                   UNWIND $props AS map
                   WITH map, apoc.map.removeKeys(map, ['labels']) AS properties
                   CALL apoc.create.node(map.labels, properties) YIELD node
                   RETURN node
                   """,
                    params,
                )
                .consume()
        }
        b.stop()
    }
}

/**
 * Persists a collection of edges into a Neo4j graph database within the context of a [Session].
 *
 * This method ensures that the required index for node IDs is created before proceeding with
 * relationship creation. The edges are subdivided into chunks, and for each chunk, the
 * relationships are created in the database. Neo4j does not support multiple labels on edges, so
 * each edge is duplicated for each assigned label. The created relationships are associated with
 * their respective nodes and additional properties derived from the edges.
 *
 * Constraints:
 * - The session context is required to execute write transactions.
 * - Edges should define their labels and properties for appropriate persistence.
 *
 * Mechanisms:
 * - An index for [Node] IDs is created (if not already existing) to optimize matching operations.
 * - Edges are chunked to avoid overloading transactional operations.
 * - Relationship properties and labels are mapped before using database utilities for creation.
 */
context(session: Session)
private fun Collection<Relationship>.persistNeo4j() {
    // Create an index for the "id" field of node, because we are "MATCH"ing on it in the edge
    // creation. We need to wait for this to be finished
    session.executeWrite { tx ->
        tx.run("CREATE INDEX IF NOT EXISTS FOR (n:Node) ON (n.id)").consume()
    }

    this.chunked(edgeChunkSize).map { chunk -> session.createRelationships(chunk) }
}

/**
 * Creates relationships in a graph database based on provided properties.
 *
 * @param props A list of maps, where each map represents properties of a relationship including
 *   keys such as `startId`, `endId`, and `type`. The `startId` and `endId` identify the nodes to
 *   connect, while `type` defines the type of the relationship. Additional properties for the
 *   relationship can also be included in the map.
 */
private fun Session.createRelationships(props: List<Relationship>) {
    val b = Benchmark(Persistable::class.java, "Persisting chunk of ${props.size} relationships")
    val params = mapOf("props" to props)
    executeWrite { tx ->
        tx.run(
                $$"""
            UNWIND $props AS map
            MATCH (s:Node {id: map.startId})
            MATCH (e:Node {id: map.endId})
            WITH s, e, map, apoc.map.removeKeys(map, ['startId', 'endId', 'type']) AS properties
            CALL apoc.create.relationship(s, map.type, properties, e) YIELD rel
            RETURN rel
            """
                    .trimIndent(),
                params,
            )
            .consume()
    }
    b.stop()
}

/**
 * Connects to the neo4j db.
 *
 * @return the [Session] object for interacting with the database.
 * @throws InterruptedException, if the thread is interrupted while it tries to connect to the neo4j
 *   db.
 * @throws ConnectException, if there is no connection to bolt://localhost:7687 possible
 */
@Throws(InterruptedException::class, ConnectException::class)
fun connect(
    protocol: String = Neo4jConnectionDefaults.PROTOCOL,
    host: String = Neo4jConnectionDefaults.HOST,
    port: Int = Neo4jConnectionDefaults.PORT,
    neo4jUsername: String = Neo4jConnectionDefaults.USERNAME,
    neo4jPassword: String = Neo4jConnectionDefaults.PASSWORD,
): Session {
    val driver =
        GraphDatabase.driver(
            "$protocol$host:$port",
            org.neo4j.driver.AuthTokens.basic(neo4jUsername, neo4jPassword),
        )
    driver.verifyConnectivity()
    return driver.session()
}
