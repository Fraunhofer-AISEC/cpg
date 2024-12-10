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
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.allEdges
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.helpers.neo4j.NameConverter
import de.fraunhofer.aisec.cpg.helpers.neo4j.SimpleNameConverter
import kotlin.collections.joinToString
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.withNullability
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

val schemaPropertiesCache: MutableMap<KClass<out Node>, Map<String, KProperty1<out Node, *>>> =
    mutableMapOf()

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
    if (this.id == null) {
        this.id = Uuid.random()
    }

    val properties = this.properties()

    this@TransactionContext.run(
            "MERGE (n:${this.labels.joinToString("&")} ${properties.writePlaceHolder()} ) RETURN n.id",
            this.properties()
        )
        .consume()
    println("Created node with id $id")
}

fun Map<String, Any?>.writePlaceHolder(): String {
    return this.map { "${it.key}: \$${it.key}" }.joinToString(", ", prefix = "{ ", postfix = " }")
}

/**
 * Returns the node's properties. This DOES NOT include relationships, but only properties directly
 * attached to the node.
 */
fun Node.properties(): Map<String, Any?> {
    val properties = mutableMapOf<String, Any?>()
    for (entry in schemaProperties) {
        val value = entry.value.call(this)

        if (value == null) {
            continue
        }

        // TODO: generalize conversions
        if (value is Name && entry.key == "name") {
            properties += NameConverter().toGraphProperties(value)
        } else if (value is Name) {
            properties.put(entry.key, SimpleNameConverter().toGraphProperty(value))
        } else if (value is Uuid) {
            properties.put(entry.key, value.toString())
        } else {
            properties.put(entry.key, value)
        }
    }

    return properties
}

context(TransactionContext)
fun Edge<*>.persist() {
    this@TransactionContext.run(
            "MATCH (start { id: \$startId }), (end { id: \$endId } ) MERGE (start)-[r:${label} { }]->(end)",
            mapOf("startId" to this.start.id.toString(), "endId" to this.end.id.toString())
        )
        .consume()
}

val Node.labels: Set<String>
    get() {
        val klazz = this::class

        // Check, if we already computed the labels for this node's class
        return labelCache.computeIfAbsent(klazz) { setOf<String>("Node", klazz.simpleName!!) }
    }

val primitiveTypes =
    setOf(
        String::class.createType(),
        Int::class.createType(),
        Long::class.createType(),
        Boolean::class.createType(),
        Name::class.createType(),
        Uuid::class.createType(),
    )

val Node.schemaProperties: Map<String, KProperty1<out Node, *>>
    get() {
        val klazz = this::class

        // Check, if we already computed the labels for this node's class
        return schemaPropertiesCache.computeIfAbsent(klazz) {
            val schema = mutableMapOf<String, KProperty1<out Node, *>>()
            val properties = it.memberProperties
            for (property in properties) {
                if (property.returnType.withNullability(false) in primitiveTypes) {
                    schema.put(property.name, property)
                }
            }
            schema
        }
    }
