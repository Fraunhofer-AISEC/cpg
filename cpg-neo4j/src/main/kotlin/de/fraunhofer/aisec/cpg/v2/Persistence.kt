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
package de.fraunhofer.aisec.cpg.v2

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.allEdges
import de.fraunhofer.aisec.cpg.graph.edges.edges
import de.fraunhofer.aisec.cpg.graph.nodes
import kotlin.collections.joinToString
import kotlin.reflect.KClass
import kotlin.uuid.Uuid
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.TransactionContext

val dbUri = "neo4j://localhost"
val dbUser = "neo4j"
val dbPassword = "password"

val neo4jSession by lazy {
    GraphDatabase.driver(dbUri, org.neo4j.driver.AuthTokens.basic(dbUser, dbPassword)).session()
}

val labelCache: MutableMap<KClass<out Node>, Set<String>> = mutableMapOf()

fun TranslationResult.persist() {
    neo4jSession.executeWrite { tx ->
        with(tx) {
            val nodes = this@persist.nodes
            for (node in nodes) {
                node.persist()
            }

            val edges = this@persist.allEdges<Edge<*>>()
            for (edge in edges) {
                edge.persist()
            }
        }
    }
}

context(TransactionContext)
fun Node.persist() {
    if (this.graphId == null) {
        this.graphId = Uuid.random()
    }

    val result =
        this@TransactionContext.run(
            "MERGE (n:${this.labels.joinToString("&")} { name: \$name, id: \$id } ) RETURN n.id",
            mapOf(
                "name" to this.name.localName,
                "id" to this.graphId.toString(),
            )
        )
    println("Created node with id $graphId")
}

context(TransactionContext)
fun Edge<*>.persist() {
    val result =
        this@TransactionContext.run(
            "MATCH (start { id: \$startId }), (end { id: \$endId } ) MERGE (start)-[r:${label}]->(end)",
            mapOf(
                "startId" to this.start.graphId.toString(),
                "endId" to this.end.graphId.toString()
            )
        )
}

val Node.labels: Set<String>
    get() {
        val klazz = this::class

        // Check, if we already computed the labels for this node's class
        return labelCache.computeIfAbsent(klazz) { setOf<String>("Node", klazz.simpleName!!) }
    }
