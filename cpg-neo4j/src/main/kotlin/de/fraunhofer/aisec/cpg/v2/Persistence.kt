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
import de.fraunhofer.aisec.cpg.graph.Persistable
import de.fraunhofer.aisec.cpg.graph.PersistedAsNode
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.allEdges
import de.fraunhofer.aisec.cpg.graph.edges.flows.DependenceType
import de.fraunhofer.aisec.cpg.graph.edges.flows.Granularity
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.helpers.neo4j.DataflowGranularityConverter
import de.fraunhofer.aisec.cpg.helpers.neo4j.NameConverter
import de.fraunhofer.aisec.cpg.helpers.neo4j.SimpleNameConverter
import kotlin.collections.plusAssign
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.withNullability
import kotlin.uuid.Uuid
import org.neo4j.driver.GraphDatabase
import org.neo4j.ogm.typeconversion.CompositeAttributeConverter
import org.slf4j.LoggerFactory

/**
 * docker run \ --name neo4j-apoc \ -p 7474:7474 -p 7687:7687 \ -d \ -e NEO4J_AUTH=neo4j/password \
 * -e NEO4JLABS_PLUGINS='["apoc"]' \ neo4j:5
 */
val dbUri = "neo4j://localhost"
val dbUser = "neo4j"
val dbPassword = "password"

val neo4jSession by lazy {
    val driver = GraphDatabase.driver(dbUri, org.neo4j.driver.AuthTokens.basic(dbUser, dbPassword))
    driver.session()
}

val labelCache: MutableMap<KClass<out PersistedAsNode>, Set<String>> = mutableMapOf()

val schemaPropertiesCache:
    MutableMap<KClass<out Persistable>, Map<String, KProperty1<out Persistable, *>>> =
    mutableMapOf()

val log = LoggerFactory.getLogger("Persistence")

val edgeChunkSize = 10000
val nodeChunkSize = 10000

fun TranslationResult.persist() {
    val nodes = this@persist.nodes
    val edges = this@persist.allEdges<Edge<*>>()
    val scopes = this.finalCtx.scopeManager.filterScopes { true }
    val languages = this.finalCtx.config.languages

    val b = Benchmark(Persistable::class.java, "Persisting translation result")

    log.info("Persisting {} AST nodes", nodes.size)
    nodes.persist()

    log.info("Persisting {} scopes", nodes.size)
    scopes.persist()

    log.info("Persisting {} languages", nodes.size)
    languages.persist()

    log.info("Persisting {} edges", edges.size)
    edges.persist()

    log.info("Persisting {} extra relationships (language, scopes)", edges.size)
    nodes.persistExtraRelationships()

    b.stop()
}

private fun List<PersistedAsNode>.persist() {
    this.chunked(nodeChunkSize).map { chunk ->
        val b = Benchmark(Persistable::class.java, "Persisting chunk of ${chunk.size} nodes")
        val params =
            mapOf("props" to chunk.map { mapOf("labels" to it::class.labels) + it.properties() })
        neo4jSession.executeWrite { tx ->
            tx.run(
                    """
                   UNWIND ${"$"}props AS map
                   WITH map, apoc.map.removeKeys(map, ['labels']) AS properties
                   CALL apoc.create.node(map.labels, properties) YIELD node
                   RETURN node
                   """,
                    params
                )
                .consume()
        }
        b.stop()
    }
}

private fun Collection<Edge<*>>.persist() {
    // Create an index for the "id" field of node, because we are "MATCH"ing on it in the edge
    // creation. We need to wait for this to be finished
    neo4jSession.executeWrite { tx ->
        tx.run("CREATE INDEX IF NOT EXISTS FOR (n:Node) ON (n.id)").consume()
    }

    this.chunked(edgeChunkSize).map { chunk ->
        createRelationships(
            chunk.map {
                mapOf(
                    "startId" to it.start.id.toString(),
                    "endId" to it.end.id.toString(),
                    "type" to it.label
                ) + it.properties()
            }
        )
    }
}

private fun List<Node>.persistExtraRelationships() {
    this.flatMap {
            listOf(
                mapOf(
                    "startId" to it.id.toString(),
                    "endId" to it.language?.id.toString(),
                    "type" to "LANGUAGE"
                ),
                mapOf(
                    "startId" to it.id.toString(),
                    "endId" to it.scope?.id.toString(),
                    "type" to "SCOPE"
                ),
            )
        }
        .chunked(10000)
        .map { chunk -> createRelationships(chunk) }
}

private fun createRelationships(
    props: List<Map<String, Any?>>,
) {
    val b = Benchmark(Persistable::class.java, "Persisting chunk of ${props.size} relationships")
    val params = mapOf("props" to props)
    neo4jSession.executeWrite { tx ->
        tx.run(
                """
            UNWIND ${'$'}props AS map
            MATCH (s:Node {id: map.startId})
            MATCH (e:Node {id: map.endId})
            WITH s, e, map, apoc.map.removeKeys(map, ['startId', 'endId', 'type']) AS properties
            CALL apoc.create.relationship(s, map.type, properties, e) YIELD rel
            RETURN rel
            """
                    .trimIndent(),
                params
            )
            .consume()
    }
    b.stop()
}

/**
 * Returns the [Persistable]'s properties. This DOES NOT include relationships, but only properties
 * directly attached to the node/edge.
 */
fun Persistable.properties(): Map<String, Any?> {
    val properties = mutableMapOf<String, Any?>()
    for (entry in this::class.schemaProperties) {
        val value = entry.value.call(this)

        if (value == null) {
            continue
        }

        value.convert(entry.key, properties)
    }

    return properties
}

/**
 * Runs any conversions that are necessary by [CompositeAttributeConverter] and
 * [org.neo4j.ogm.typeconversion.AttributeConverter]. Since both of these classes are Neo4J OGM
 * classes, we need to find new base types at some point.
 */
fun Any.convert(originalKey: String, properties: MutableMap<String, Any?>) {
    // TODO: generalize conversions
    if (this is Name && originalKey == "name") {
        properties += NameConverter().toGraphProperties(this)
    } else if (this is Name) {
        properties.put(originalKey, SimpleNameConverter().toGraphProperty(this))
    } else if (this is Granularity) {
        properties += DataflowGranularityConverter().toGraphProperties(this)
    } else if (this is Enum<*>) {
        properties.put(originalKey, this.name)
    } else if (this is Uuid) {
        properties.put(originalKey, this.toString())
    } else {
        properties.put(originalKey, this)
    }
}

val KClass<out PersistedAsNode>.labels: Set<String>
    get() {
        // Check, if we already computed the labels for this node's class
        return labelCache.computeIfAbsent(this) { setOf<String>("Node", this.simpleName!!) }
    }

val propertyTypes =
    setOf(
        String::class.createType(),
        Int::class.createType(),
        Long::class.createType(),
        Boolean::class.createType(),
        Name::class.createType(),
        Uuid::class.createType(),
        Granularity::class.createType(),
        DependenceType::class.createType(),
    )

val KClass<out Persistable>.schemaProperties: Map<String, KProperty1<out Persistable, *>>
    get() {
        // Check, if we already computed the labels for this node's class
        return schemaPropertiesCache.computeIfAbsent(this) {
            val schema = mutableMapOf<String, KProperty1<out Persistable, *>>()
            val properties = it.memberProperties
            for (property in properties) {
                if (property.returnType.withNullability(false) in propertyTypes) {
                    schema.put(property.name, property)
                }
            }
            schema
        }
    }
