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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.Persistable
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.helpers.Benchmark

/**
 * An abstraction over a graph database that a [TranslationResult] can be persisted to.
 *
 * The CPG can be written to several graph databases, which all understand (a dialect of) Cypher,
 * but differ in their wire protocol and in the exact Cypher features they support. This interface
 * captures the small set of operations that actually differ between them, so that the
 * database-independent part of the persistence — collecting the nodes and relationships of a
 * [TranslationResult] and splitting them into chunks — can be shared by all implementations in
 * [persist].
 *
 * Implementations are expected to hold an open connection and are therefore [AutoCloseable].
 */
interface GraphDatabaseBackend : AutoCloseable {
    /**
     * The number of nodes that are persisted in a single call to [persistNodeChunk]. Chunking keeps
     * the size of an individual database write manageable, even for very large graphs.
     */
    val nodeChunkSize: Int
        get() = DEFAULT_NODE_CHUNK_SIZE

    /**
     * The number of relationships that are persisted in a single call to
     * [persistRelationshipChunk].
     */
    val relationshipChunkSize: Int
        get() = DEFAULT_RELATIONSHIP_CHUNK_SIZE

    /** Removes all nodes and relationships that are currently stored in the database. */
    fun purgeDatabase()

    /**
     * Creates the indexes that are needed to persist (and later query) the CPG efficiently. This is
     * called after the nodes have been written, but before the relationships are written, since
     * relationship creation matches on the node identifier.
     */
    fun createIndexes()

    /** Writes a chunk of at most [nodeChunkSize] [nodes] to the database. */
    fun persistNodeChunk(nodes: List<Node>)

    /**
     * Writes a chunk of at most [relationshipChunkSize] relationships to the database. Each entry
     * is a map as produced by [collectRelationships], i.e. it contains the keys `startId`, `endId`
     * and `type` alongside the properties of the relationship.
     */
    fun persistRelationshipChunk(relationships: List<Map<String, Any?>>)
}

/** The default value for [GraphDatabaseBackend.nodeChunkSize]. */
const val DEFAULT_NODE_CHUNK_SIZE = 10000

/** The default value for [GraphDatabaseBackend.relationshipChunkSize]. */
const val DEFAULT_RELATIONSHIP_CHUNK_SIZE = 10000

/**
 * Persists this [TranslationResult] into the given [db].
 *
 * This method performs the following actions:
 * - Collects the nodes that need to be persisted. These are the AST nodes as well as all other
 *   nodes that are reachable from them through a relationship (e.g. scopes, types and languages).
 * - Optionally purges the database beforehand, see [purgeDb].
 * - Persists the collected nodes, chunked by [GraphDatabaseBackend.nodeChunkSize].
 * - Creates the indexes needed for matching nodes by their identifier.
 * - Collects and persists all relationships between the nodes, chunked by
 *   [GraphDatabaseBackend.relationshipChunkSize].
 *
 * The whole operation as well as each individual chunk is benchmarked using [Benchmark].
 *
 * @param db the database to persist to
 * @param purgeDb whether the database should be emptied before writing the new graph
 */
fun TranslationResult.persist(db: GraphDatabaseBackend, purgeDb: Boolean = true) {
    val b = Benchmark(Persistable::class.java, "Persisting translation result")

    val astNodes = this@persist.nodes
    val connected = astNodes.flatMap { it.connectedNodes }.toSet()
    val nodes = (astNodes + connected).distinct()

    if (purgeDb) {
        log.info("Purging database before persisting")
        db.purgeDatabase()
    }

    log.info(
        "Persisting {} nodes: AST nodes ({}), other nodes ({})",
        nodes.size,
        astNodes.size,
        connected.size,
    )
    nodes.persistChunked(db)

    // The index on the node identifier is needed to match the start and end node of a
    // relationship, so it has to exist before the relationships are written.
    db.createIndexes()

    val relationships = nodes.collectRelationships()

    log.info("Persisting {} relationships", relationships.size)
    relationships.persistChunked(db)

    b.stop()
}

/** Splits this list of nodes into chunks and hands each of them to [db]. */
private fun List<Node>.persistChunked(db: GraphDatabaseBackend) {
    this.chunked(db.nodeChunkSize).forEach { chunk ->
        val b = Benchmark(Persistable::class.java, "Persisting chunk of ${chunk.size} nodes")
        db.persistNodeChunk(chunk)
        b.stop()
    }
}

/** Splits this list of relationships into chunks and hands each of them to [db]. */
@JvmName("persistRelationshipsChunked")
private fun List<Map<String, Any?>>.persistChunked(db: GraphDatabaseBackend) {
    this.chunked(db.relationshipChunkSize).forEach { chunk ->
        val b =
            Benchmark(Persistable::class.java, "Persisting chunk of ${chunk.size} relationships")
        db.persistRelationshipChunk(chunk)
        b.stop()
    }
}
