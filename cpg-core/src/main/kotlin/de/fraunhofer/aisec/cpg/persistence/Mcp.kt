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

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.Persistable
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeCollection
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.uuid.Uuid

/**
 * The level of detail requested for an MCP (LLM-facing) node view.
 * - [SUMMARY] is the default for bulk-listing tools: it omits properties annotated with
 *   `@McpDetail(FULL)` (e.g. [de.fraunhofer.aisec.cpg.graph.Node.code]), which tend to be large and
 *   are usually not needed until a specific node has been picked.
 * - [FULL] includes everything, e.g. for a "get details of this one node" tool.
 */
enum class McpDetailLevel {
    SUMMARY,
    FULL,
}

/**
 * Marks a [Persistable] property as only relevant starting at [minLevel]. A property without this
 * annotation is always included (i.e. behaves as if annotated with `McpDetail(SUMMARY)`).
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class McpDetail(val minLevel: McpDetailLevel = McpDetailLevel.FULL)

/**
 * Specifies an alternate [AttributeConverter] to use when building the MCP (LLM-facing) view of a
 * property, instead of the [Convert] converter used for graph-database persistence. This exists
 * because a handful of types have a Neo4j-specific shape (e.g. [Name] is split into several
 * properties because Neo4j cannot store nested objects) that is unnecessarily verbose for an
 * LLM-facing JSON view, which has no such restriction.
 *
 * If a property has no [McpConvert], its raw value is used (see [mcpConvert]) - most properties do
 * not need one.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class McpConvert(val value: KClass<out AttributeConverter<*, *>>)

/**
 * Returns this [Persistable]'s scalar properties as a JSON-friendly `Map<String, Any?>`, the same
 * way [properties] does for the Neo4j export, but:
 * - filtered by [detail] via [McpDetail]
 * - converted via [McpConvert] instead of [Convert], where present
 *
 * This only covers scalar properties. Relationships (DFG/CDG/PDG/OVERLAY, or subclass-specific ones
 * such as `Call.invokes`/`Call.arguments`) are handled separately, see [mcpRelationships] and
 * [mcpRelatedNodes].
 */
fun Persistable.mcpProperties(detail: McpDetailLevel = McpDetailLevel.SUMMARY): Map<String, Any?> {
    val properties = mutableMapOf<String, Any?>()
    for (entry in this::class.schemaProperties) {
        val requiredLevel =
            entry.value.findAnnotation<McpDetail>()?.minLevel ?: McpDetailLevel.SUMMARY
        if (detail.ordinal < requiredLevel.ordinal) {
            continue
        }

        val value = entry.value.call(this) ?: continue
        value.mcpConvert(entry, properties)
    }

    return properties
}

/**
 * Runs the [McpConvert] conversion for a single property, if present, mirroring [convert] but
 * looking up [McpConvert] instead of [Convert]. Falls back to the same primitive-friendly handling
 * as [convert] (enums, [Uuid], [Name]) when no [McpConvert] is specified.
 */
private fun Any.mcpConvert(
    entry: Map.Entry<String, KProperty1<out Persistable, *>>,
    properties: MutableMap<String, Any?>,
) {
    val originalKey = entry.key
    val annotation = entry.value.findAnnotation<McpConvert>()

    @Suppress("UNCHECKED_CAST")
    when {
        annotation != null -> {
            val converter = annotation.value.createInstance()
            if (converter is CompositeAttributeConverter<*>) {
                properties += (converter as CompositeAttributeConverter<Any>).toGraphProperty(this)
            } else if (converter is AttributeConverter<*, *>) {
                properties[originalKey] =
                    (converter as AttributeConverter<Any, Any>).toGraphProperty(this)
            }
        }
        this is Name -> properties[originalKey] = this.toString()
        this is Enum<*> -> properties[originalKey] = this.name
        this is Uuid -> properties[originalKey] = this.toString()
        else -> properties[originalKey] = this
    }
}

/**
 * A cache mapping classes of type [Persistable] to their MCP (LLM-facing) relationships, see
 * [mcpRelationships].
 */
private val mcpRelationshipCache:
    MutableMap<KClass<out Persistable>, Map<String, KProperty1<out Persistable, *>>> =
    mutableMapOf()

/**
 * Returns this class's relationships (AST children, DFG/CDG/PDG/OVERLAY, and subclass-specific ones
 * such as `Call.invokes`/`Call.arguments`) as a name -> property map, keyed by a name derived from
 * the underlying Kotlin property (stripping a trailing "Edge"/"Edges", the same convention used as
 * a fallback by [relationshipName]) rather than by [relationshipName] itself.
 *
 * This differs from [schemaRelationships] only in that key: [schemaRelationships] keys by the
 * derived Neo4j relationship label, which is not guaranteed to be unique per class - e.g.
 * `Node.prevDFGEdges`/`nextDFGEdges` both use the label "DFG" (direction is metadata there, not
 * part of the key), so only one of the two would survive in that map. Traversal needs both
 * directions addressable, so this uses the property name instead.
 */
val KClass<out Persistable>.mcpRelationships: Map<String, KProperty1<out Persistable, *>>
    get() =
        mcpRelationshipCache.computeIfAbsent(this) {
            val schema = mutableMapOf<String, KProperty1<out Persistable, *>>()
            for (property in it.memberProperties) {
                if (isRelationship(property)) {
                    schema[property.name.substringBeforeLast("Edge")] = property
                }
            }
            schema
        }

/**
 * Returns the [Node]s connected to this [Persistable] via the given MCP [relationship] (a key from
 * [mcpRelationships]), or `null` if this class has no such relationship.
 */
fun Persistable.mcpRelatedNodes(relationship: String): List<Node>? {
    val property = this::class.mcpRelationships[relationship] ?: return null

    return when (val value = property.call(this)) {
        is EdgeCollection<*, *> -> value.toNodeCollection().filterIsInstance<Node>()
        is Collection<*> -> value.filterIsInstance<Node>()
        is Node -> listOf(value)
        else -> emptyList()
    }
}
