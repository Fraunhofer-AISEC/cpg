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
import java.net.ConnectException
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Session

/**
 * Defines the number of edges to be processed in a single batch operation during persistence.
 *
 * This constant is used for chunking collections of edges into smaller groups to optimize write
 * performance and reduce memory usage when interacting with the Neo4j database. Specifically, it
 * determines the maximum size of each chunk of edges to be persisted in one batch operation.
 */
const val edgeChunkSize = DEFAULT_RELATIONSHIP_CHUNK_SIZE

/**
 * Specifies the maximum number of nodes to be processed in a single chunk during persistence
 * operations.
 *
 * This constant is used to control the size of batches when persisting a list of nodes to the
 * database. Breaking the list into chunks of this size helps improve performance and memory
 * efficiency during database writes. Each chunk is handled individually, ensuring that operations
 * remain manageable even for large data sets.
 */
const val nodeChunkSize = DEFAULT_NODE_CHUNK_SIZE

/**
 * A [GraphDatabaseBackend] that writes the CPG into a Neo4j database using the supplied [session].
 *
 * Neo4j does not allow labels and relationship types to be passed as query parameters, so the
 * dynamic creation of nodes and relationships is delegated to the
 * [APOC](https://neo4j.com/labs/apoc/) plugin, which therefore needs to be enabled on the server.
 *
 * Note that closing this backend also closes the underlying [session].
 */
class Neo4jDatabase(private val session: Session) : GraphDatabaseBackend {
    override fun purgeDatabase() {
        session.executeWrite { tx -> tx.run("MATCH (n) DETACH DELETE n").consume() }
    }

    override fun createIndexes() {
        // Create an index for the "id" field of node, because we are "MATCH"ing on it in the edge
        // creation. We need to wait for this to be finished
        session.executeWrite { tx ->
            tx.run("CREATE INDEX IF NOT EXISTS FOR (n:Node) ON (n.id)").consume()
        }
    }

    override fun persistNodeChunk(nodes: List<Node>) {
        val params =
            mapOf("props" to nodes.map { mapOf("labels" to it::class.labels) + it.properties() })
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
    }

    override fun persistRelationshipChunk(relationships: List<Map<String, Any?>>) {
        val params = mapOf("props" to relationships)
        session.executeWrite { tx ->
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
    }

    override fun close() {
        session.close()
    }
}

/**
 * This function creates a new Neo4j session, optionally purges the database, and persists the
 * current [TranslationResult] into the database.
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
    Neo4jDatabase(session).use { db -> this.persist(db, purgeDb = !noPurgeDb) }
}

/**
 * Persists the current [TranslationResult] into a Neo4j database, using the [Session] available in
 * the current context.
 *
 * See [persist] for a description of what exactly is written to the database. Note that, in
 * contrast to [pushToNeo4j], this does not purge the database beforehand.
 */
context(session: Session)
fun TranslationResult.persistNeo4j() {
    this.persist(Neo4jDatabase(session), purgeDb = false)
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
