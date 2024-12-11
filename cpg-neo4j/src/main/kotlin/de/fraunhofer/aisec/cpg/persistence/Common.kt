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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.Persistable
import de.fraunhofer.aisec.cpg.graph.edges.flows.DependenceType
import de.fraunhofer.aisec.cpg.graph.edges.flows.Granularity
import de.fraunhofer.aisec.cpg.helpers.BenchmarkResults
import de.fraunhofer.aisec.cpg.helpers.neo4j.DataflowGranularityConverter
import de.fraunhofer.aisec.cpg.helpers.neo4j.LocationConverter
import de.fraunhofer.aisec.cpg.helpers.neo4j.NameConverter
import de.fraunhofer.aisec.cpg.helpers.neo4j.SimpleNameConverter
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.javaType
import kotlin.uuid.Uuid
import org.neo4j.ogm.typeconversion.CompositeAttributeConverter
import org.slf4j.LoggerFactory

/**
 * A cache used to store and retrieve sets of labels associated with specific Kotlin class types.
 *
 * This mutable map uses a Kotlin class type as the key and a set of strings representing associated
 * labels as the value. The [labelCache] provides efficient lookup and prevents redundant
 * re-computation of labels for the same class type.
 */
val labelCache: MutableMap<KClass<*>, Set<String>> = mutableMapOf()

/**
 * A cache mapping classes of type [Persistable] to their respective properties.
 *
 * This mutable map stores metadata about [Persistable] objects. For each specific class that
 * implements the [Persistable] interface, it caches a mapping between property names and their
 * corresponding [KProperty1] references. This allows efficient reflection-based access to class
 * properties without repeatedly inspecting the class at runtime.
 *
 * The key in the map is the [KClass] of a subclass of [Persistable]. The value is a [Map] where the
 * keys are strings representing the property names, and the values are [KProperty1] references
 * pointing to those properties. This can be used for dynamic property access or serialization
 * processes.
 */
val schemaPropertiesCache:
    MutableMap<KClass<out Persistable>, Map<String, KProperty1<out Persistable, *>>> =
    mutableMapOf()

/**
 * A set containing predefined property types represented as Kotlin type objects that can be used as
 * properties in [schemaProperties].
 */
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

internal val log = LoggerFactory.getLogger("Persistence")

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
    } else if (this is PhysicalLocation) {
        properties += LocationConverter().toGraphProperties(this)
    } else if (this is Granularity) {
        properties += DataflowGranularityConverter().toGraphProperties(this)
    } else if (this is Enum<*>) {
        properties.put(originalKey, this.name)
    } else if (this is Uuid) {
        properties.put(originalKey, this.toString())
    } else if (this is BigInteger) {
        properties.put(originalKey, this.toString())
    } else {
        properties.put(originalKey, this)
    }
}

/**
 * Represents a computed property for obtaining a set of labels associated with a Kotlin class.
 *
 * Recursively collects labels from the class hierarchy, including superclass labels, and adds the
 * simple name of the current class to the set of labels.
 *
 * Interfaces and the Kotlin base class `Any` are excluded from the labels. The results are cached
 * to improve performance.
 */
val KClass<*>.labels: Set<String>
    get() {
        // Ignore interfaces and the Kotlin base class
        if (this.java.isInterface || this == Any::class) {
            return setOf()
        }

        val cacheKey = this

        // Note: we cannot use computeIfAbsent here, because we are calling our function
        // recursively and this would result in a ConcurrentModificationException
        if (labelCache.containsKey(cacheKey)) {
            return labelCache[cacheKey] ?: setOf()
        }

        val labels = mutableSetOf<String>()
        labels.addAll(this.superclasses.flatMap { it.labels })
        this.simpleName?.let { labels.add(it) }

        // update the cache
        labelCache[cacheKey] = labels
        return labels
    }

/** A list of specific types that are intended to be ignored for persistence. */
internal val ignoredTypes =
    listOf(
        TranslationContext::class.createType(),
        TranslationConfiguration::class.createType(),
        BenchmarkResults::class.createType(),
        KClass::class.createType(listOf(KTypeProjection.STAR)),
    )

internal val nodeType = Node::class.createType()
internal val collectionType = Collection::class.createType(listOf(KTypeProjection.STAR))
internal val mapType = Map::class.createType(listOf(KTypeProjection.STAR, KTypeProjection.STAR))

/**
 * Retrieves a map of schema properties (not relationships!) for the given class implementing
 * [Persistable].
 *
 * This property computes a map that associates property names (as strings) to their corresponding
 * [KProperty1] objects, which represent the properties defined in the class. Only properties whose
 * return types are included in a predefined set of supported property types ([propertyTypes]) are
 * included in the map.
 *
 * The computed map is cached to optimize subsequent lookups for properties of the same class.
 */
val KClass<out Persistable>.schemaProperties: Map<String, KProperty1<out Persistable, *>>
    get() {
        // Check, if we already computed the properties for this node's class
        return schemaPropertiesCache.computeIfAbsent(this) {
            val schema = mutableMapOf<String, KProperty1<out Persistable, *>>()
            val properties = it.memberProperties
            for (property in properties) {
                if (isSimpleProperty(property)) {
                    schema.put(property.name, property)
                }
            }
            schema
        }
    }

/**
 * Evaluates whether a given property qualifies as a "simple" property based on its characteristics.
 *
 * This evaluates to false, when
 * - The property is a list (see [collectionType])
 * - The property is a map (see [mapType])
 * - The property is one of our [ignoredTypes]
 * - The property is referring to a [Node]
 * - The property is an interface
 *
 * @param property the property to be evaluated, belonging to a class implementing the Persistable
 *   interface
 * @return true if the property satisfies the conditions of being a "simple" property, false
 *   otherwise
 */
private fun isSimpleProperty(property: KProperty1<out Persistable, *>): Boolean {
    val returnType = property.returnType.withNullability(false)

    return when {
        property.visibility == KVisibility.PRIVATE -> false
        ignoredTypes.any { returnType.isSubtypeOf(it) } -> false
        returnType.isSubtypeOf(collectionType) -> false
        returnType.isSubtypeOf(mapType) -> false
        returnType.withNullability(false).isSubtypeOf(nodeType) -> false
        (returnType.javaType as? Class<*>)?.isInterface == true -> false
        else -> true
    }
}
