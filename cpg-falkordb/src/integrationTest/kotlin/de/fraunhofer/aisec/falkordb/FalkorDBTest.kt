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
package de.fraunhofer.aisec.falkordb

import com.falkordb.Driver
import com.falkordb.Graph
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.persistence.FalkorDBDatabase
import de.fraunhofer.aisec.cpg.persistence.connectToFalkorDB
import de.fraunhofer.aisec.cpg.persistence.persist
import de.fraunhofer.aisec.cpg.persistence.pushToFalkorDB
import de.fraunhofer.aisec.cpg.test.GraphExamples
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for the FalkorDB persistence. They require a running FalkorDB instance, which
 * can be started with:
 * ```
 * docker run -p 127.0.0.1:6379:6379 -d falkordb/falkordb:latest
 * ```
 *
 * The host, port and graph name can be overridden with the `FALKORDB_HOST`, `FALKORDB_PORT` and
 * `FALKORDB_GRAPH` environment variables.
 */
class FalkorDBTest {

    private val host = System.getenv("FALKORDB_HOST") ?: "localhost"
    private val port = System.getenv("FALKORDB_PORT")?.toIntOrNull() ?: 6379

    private lateinit var driver: Driver
    private lateinit var graph: Graph

    @BeforeTest
    fun setup() {
        val (driver, graph) =
            connectToFalkorDB(
                host = host,
                port = port,
                graphName = System.getenv("FALKORDB_GRAPH") ?: "cpg-integration-test",
            )
        this.driver = driver
        this.graph = graph
    }

    @AfterTest
    fun teardown() {
        graph.close()
        driver.close()
    }

    /** Persists [result] and returns the number of nodes that ended up in the graph. */
    private fun persistAndCountNodes(result: TranslationResult): Long {
        FalkorDBDatabase(graph).use { db -> result.persist(db) }

        val resultSet = graph.readOnlyQuery("MATCH (n:Node) RETURN count(n) AS count")
        val record = resultSet.iterator().next()
        return record.getValue<Long>("count")
    }

    @Test
    fun testPush() {
        val result = GraphExamples.getInitializerListExprDFG()

        val persisted = persistAndCountNodes(result)

        // Every node of the CPG, including the ones that are only reachable through a
        // relationship (types, scopes, languages, ...), has to be persisted
        assertTrue(persisted > 0, "Expected the graph to contain nodes")

        // All functions of the translation result must be found again by their label. This
        // verifies that we assign the *complete* set of labels of the class hierarchy, and not
        // just the most specific one
        val functions =
            graph
                .readOnlyQuery("MATCH (n:Function) RETURN count(n) AS count")
                .iterator()
                .next()
                .getValue<Long>("count")
        assertEquals(result.functions.size.toLong(), functions)
    }

    @Test
    fun testPushPurgesPreviousGraph() {
        val result = GraphExamples.getInitializerListExprDFG()

        val first = persistAndCountNodes(result)
        val second = persistAndCountNodes(result)

        // Since persisting purges the graph beforehand, pushing the same result twice must not
        // duplicate any node
        assertEquals(first, second)
    }

    @Test
    fun testRelationshipsArePersisted() {
        val result = GraphExamples.getInitializerListExprDFG()
        persistAndCountNodes(result)

        // The EOG is one of the relationships that every non-trivial CPG has
        val eogEdges =
            graph
                .readOnlyQuery("MATCH ()-[r:EOG]->() RETURN count(r) AS count")
                .iterator()
                .next()
                .getValue<Long>("count")
        assertTrue(eogEdges > 0, "Expected EOG relationships to be persisted")

        // Calls are connected to their invoked function; this also exercises that relationship
        // properties survive the round trip
        val call = result.calls.firstOrNull()
        assertNotNull(call)

        val persistedCall =
            graph
                .readOnlyQuery("MATCH (n:Call {id: \"${call.id}\"}) RETURN n.name AS name")
                .iterator()
                .next()
                .getValue<String>("name")
        assertEquals(call.name.toString(), persistedCall)
    }

    @Test
    fun testMultipleGraphsAreIndependent() {
        val result = GraphExamples.getInitializerListExprDFG()
        persistAndCountNodes(result)

        // A second graph in the same instance must not be affected by the first one
        val other = driver.graph("cpg-integration-test-other")
        other.use {
            FalkorDBDatabase(it).use { db -> result.persist(db) }

            val count =
                it.readOnlyQuery("MATCH (n:Node) RETURN count(n) AS count")
                    .iterator()
                    .next()
                    .getValue<Long>("count")
            assertTrue(count > 0)
            it.deleteGraph()
        }
    }

    @Test
    fun testPushToFalkorDB() {
        val result = GraphExamples.getInitializerListExprDFG()

        // The public entry point opens (and closes) its own driver and graph
        result.pushToFalkorDB(host = host, port = port, graphName = "cpg-integration-test-push")

        val other = driver.graph("cpg-integration-test-push")
        val count =
            other
                .readOnlyQuery("MATCH (n:Node) RETURN count(n) AS count")
                .iterator()
                .next()
                .getValue<Long>("count")
        assertTrue(count > 0, "Expected pushToFalkorDB to have persisted the graph")
        other.deleteGraph()
    }
}
