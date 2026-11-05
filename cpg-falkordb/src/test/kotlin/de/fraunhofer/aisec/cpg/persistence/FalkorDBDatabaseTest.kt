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

import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.test.GraphExamples
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import redis.clients.jedis.exceptions.JedisDataException

/**
 * Unit tests for [FalkorDBDatabase] that assert on the generated Cypher using a [FakeGraph], so
 * that they do not need a running FalkorDB instance. The behaviour against a real server is covered
 * by the integration tests.
 */
class FalkorDBDatabaseTest {

    @Test
    fun testPurgeDatabase() {
        val graph = FakeGraph()
        FalkorDBDatabase(graph).purgeDatabase()

        assertEquals(1, graph.deleteGraphCalls)
    }

    @Test
    fun testPurgeDatabaseIgnoresMissingGraph() {
        // Deleting a graph that was never created is not an error for our purposes
        val graph = FakeGraph(JedisDataException("ERR Invalid graph operation on empty key"))
        FalkorDBDatabase(graph).purgeDatabase()

        assertEquals(1, graph.deleteGraphCalls)
    }

    @Test
    fun testPurgeDatabaseRethrowsOtherErrors() {
        val graph = FakeGraph(JedisDataException("ERR something else went wrong"))

        assertFailsWith<JedisDataException> { FalkorDBDatabase(graph).purgeDatabase() }
    }

    @Test
    fun testCreateIndexes() {
        val graph = FakeGraph()
        FalkorDBDatabase(graph).createIndexes()

        assertEquals(listOf("CREATE INDEX FOR (n:Node) ON (n.id)"), graph.queries)
    }

    @Test
    fun testCreateIndexesIgnoresExistingIndex() {
        // FalkorDB has no "CREATE INDEX IF NOT EXISTS", an already existing index is what we want
        val graph = FakeGraph(JedisDataException("ERR Attribute 'id' is already indexed"))
        FalkorDBDatabase(graph).createIndexes()
    }

    @Test
    fun testCreateIndexesRethrowsOtherErrors() {
        val graph = FakeGraph(JedisDataException("ERR something else went wrong"))

        assertFailsWith<JedisDataException> { FalkorDBDatabase(graph).createIndexes() }
    }

    @Test
    fun testPersistNodeChunkGroupsByLabels() {
        val graph = FakeGraph()
        val result = GraphExamples.getInitializerListExprDFG()
        val nodes = result.nodes

        FalkorDBDatabase(graph).persistNodeChunk(nodes)

        // One query per distinct set of labels, not one per node
        assertEquals(nodes.groupBy { it::class.labels }.size, graph.queries.size)

        // Every query has to use the constant UNWIND body that lets FalkorDB cache the
        // execution plan, with the labels baked into the CREATE clause
        assertTrue(
            graph.queries.all {
                it.startsWith("CYPHER props=[{") &&
                    it.contains(" UNWIND \$props AS map CREATE (n:`Node`") &&
                    it.endsWith(") SET n = map")
            },
            "Unexpected queries: ${graph.queries}",
        )

        // A function declaration carries the labels of its whole class hierarchy
        val function = nodes.first { it is Function }
        val query = graph.queries.single { it.contains(function.id.toString()) }
        assertTrue(query.contains("CREATE (n:`Node`:`AstNode`"), query)
        assertTrue(query.contains(":`Function`)"), query)
    }

    @Test
    fun testPersistRelationshipChunkGroupsByType() {
        val graph = FakeGraph()

        val relationships =
            listOf(
                mapOf("startId" to "a", "endId" to "b", "type" to "EOG", "index" to 0),
                mapOf("startId" to "b", "endId" to "c", "type" to "EOG", "index" to 1),
                mapOf("startId" to "a", "endId" to "c", "type" to "DFG"),
            )

        FalkorDBDatabase(graph).persistRelationshipChunk(relationships)

        assertEquals(2, graph.queries.size)

        val eog = graph.queries.single { it.contains("[r:`EOG`]") }
        // The structural keys must end up in the MATCH, not in the properties of the relationship
        assertTrue(eog.contains("`index`: 0"), eog)
        assertFalse(eog.contains("`type`:"), eog)
        assertTrue(eog.contains("MATCH (s:Node {id: map.startId})"), eog)
        assertTrue(eog.contains("SET r = map.properties"), eog)

        // A relationship without properties still has to produce a (empty) property map
        val dfg = graph.queries.single { it.contains("[r:`DFG`]") }
        assertTrue(dfg.contains("`properties`: {}"), dfg)
    }

    @Test
    fun testPersistRelationshipChunkSkipsUntypedRelationships() {
        val graph = FakeGraph()

        FalkorDBDatabase(graph)
            .persistRelationshipChunk(listOf(mapOf("startId" to "a", "endId" to "b")))

        assertEquals(emptyList(), graph.queries)
    }

    @Test
    fun testPersistUsesTheBackendInTheRightOrder() {
        val graph = FakeGraph()
        val result = GraphExamples.getInitializerListExprDFG()

        FalkorDBDatabase(graph).use { result.persist(it) }

        assertEquals(1, graph.deleteGraphCalls)

        // The index has to exist before the relationships are matched against it
        val indexAt = graph.queries.indexOf("CREATE INDEX FOR (n:Node) ON (n.id)")
        assertTrue(indexAt > 0, "Expected nodes to be persisted before the index is created")
        assertTrue(
            graph.queries.drop(indexAt).any { it.contains("CREATE (s)-[r:") },
            "Expected relationships to be persisted after the index is created",
        )
    }

    @Test
    fun testPersistCanSkipPurging() {
        val graph = FakeGraph()
        val result = GraphExamples.getInitializerListExprDFG()

        FalkorDBDatabase(graph).use { result.persist(it, purgeDb = false) }

        assertEquals(0, graph.deleteGraphCalls)
    }

    @Test
    fun testCloseOnlyClosesWhatWeOwn() {
        // Without a driver, the caller owns the lifecycle of the graph, so it stays usable
        val borrowed = FakeGraph()
        FalkorDBDatabase(borrowed).close()
        assertEquals(0, borrowed.closeCalls)

        val owned = FakeGraph()
        val driver = FakeDriver()
        FalkorDBDatabase(owned, driver).close()
        assertEquals(1, owned.closeCalls)
        assertEquals(1, driver.closeCalls)
    }

    @Test
    fun testChunkSizes() {
        val db = FalkorDBDatabase(FakeGraph())

        // Parameters are inlined into the query text, so we use smaller chunks than the default
        assertTrue(db.nodeChunkSize < DEFAULT_NODE_CHUNK_SIZE)
        assertTrue(db.relationshipChunkSize < DEFAULT_RELATIONSHIP_CHUNK_SIZE)
    }

    @Test
    fun testConnectRejectsIncompleteCredentials() {
        assertFailsWith<IllegalArgumentException> { connectToFalkorDB(username = "user") }
        assertFailsWith<IllegalArgumentException> { connectToFalkorDB(password = "secret") }
    }
}
