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

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.Persistable
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeCollection
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeList
import de.fraunhofer.aisec.cpg.helpers.neo4j.NameConverter
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.createType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType
import kotlin.uuid.Uuid
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.Relationship.Direction.INCOMING
import org.neo4j.ogm.annotation.typeconversion.Convert
import org.neo4j.ogm.typeconversion.AttributeConverter
import org.neo4j.ogm.typeconversion.CompositeAttributeConverter

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

/** A cache mapping classes of type [Persistable] to their respective properties. */
val schemaRelationshipCache:
    MutableMap<KClass<out Persistable>, Map<String, KProperty1<out Persistable, *>>> =
    mutableMapOf()

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

        value.convert(entry, properties)
    }

    return properties
}

/**
 * Runs any conversions that are necessary by [CompositeAttributeConverter] and
 * [AttributeConverter]. Since both of these classes are Neo4J OGM classes, we need to find new base
 * types at some point.
 */
fun Any.convert(
    entry: Map.Entry<String, KProperty1<out Persistable, *>>,
    properties: MutableMap<String, Any?>,
) {
    val originalKey = entry.key

    val annotation = entry.value.javaField?.getAnnotation(Convert::class.java)
    @Suppress("UNCHECKED_CAST")
    if (annotation != null) {
        val converter = annotation.value.createInstance()
        if (converter is CompositeAttributeConverter<*>) {
            properties += (converter as CompositeAttributeConverter<Any>).toGraphProperties(this)
        } else if (converter is AttributeConverter<*, *>) {
            properties.put(
                originalKey,
                (converter as AttributeConverter<Any, Any>).toGraphProperty(this),
            )
        }
    } else if (this is Name && originalKey == "name") {
        // needs to be extra because of the way annotations work, this will be re-designed once OGM
        // is completely gone
        properties += NameConverter().toGraphProperties(this)
    } else if (this is Enum<*>) {
        properties.put(originalKey, this.name)
    } else if (this is Uuid) {
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

internal val kClassType = KClass::class.createType(listOf(KTypeProjection.STAR))
internal val persistableType = Persistable::class.createType()
internal val collectionType = Collection::class.createType(listOf(KTypeProjection.STAR))
internal val collectionOfPersistableType =
    Collection::class.createType(
        listOf(KTypeProjection(variance = KVariance.OUT, type = persistableType))
    )
internal val edgeCollectionType =
    EdgeCollection::class.createType(listOf(KTypeProjection.STAR, KTypeProjection.STAR))
internal val mapType = Map::class.createType(listOf(KTypeProjection.STAR, KTypeProjection.STAR))

/**
 * Retrieves a map of schema properties (not relationships!) for the given class implementing
 * [Persistable].
 *
 * This property computes a map that associates property names (as strings) to their corresponding
 * [KProperty1] objects, which represent the properties defined in the class. Only properties for
 * which [isSimpleProperty] returns true, are included.
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
 * Provides a property that computes and returns a map of "relationships" for a given class
 * implementing the [Persistable] interface.
 *
 * The relationships are represented as a `Map` where:
 * - The key is the name of the relationship as a `String`.
 * - The value is a reference to the property representing the relationship, encapsulated as a
 *   [KProperty1].
 *
 * A "relationship" is determined based on a specific set of criteria defined by the
 * [isRelationship] function. These criteria evaluate properties that are associated with other
 * nodes in a graph model, excluding fields explicitly marked to skip persistence.
 *
 * The computed relationships are cached for performance optimization to ensure that repeated
 * lookups do not re-evaluate the relationships for the same class.
 *
 * This property enhances schema introspection, allowing retrieval of relational data connections
 * within classes modeled as entities in a graph database.
 */
val KClass<out Persistable>.schemaRelationships: Map<String, KProperty1<out Persistable, *>>
    get() {
        // Check, if we already computed the relationship for this node's class
        return schemaRelationshipCache.computeIfAbsent(this) {
            val schema = mutableMapOf<String, KProperty1<out Persistable, *>>()
            val properties = it.memberProperties
            for (property in properties) {
                if (isRelationship(property)) {
                    val name = property.relationshipName
                    schema.put(name, property)
                }
            }
            schema
        }
    }

/**
 * Evaluates whether a given property qualifies as a "simple" property based on its characteristics.
 *
 * This evaluates to true, when
 * - The property is not a list (see [collectionType])
 * - The property is not a map (see [mapType])
 * - The property is not a [KClass]
 * - The property is not referring to a [Node]
 * - The property is not an interface
 * - The property does not have the annotation [DoNotPersist] and its return type does not have the
 *   annotation [DoNotPersist]
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
        property.hasAnnotation<DoNotPersist>() -> false
        returnType.hasAnnotation<DoNotPersist>() -> false
        (returnType.javaType as? Class<*>)?.getAnnotation(DoNotPersist::class.java) != null -> false
        returnType.isSubtypeOf(kClassType) -> false
        returnType.isSubtypeOf(collectionType) -> false
        returnType.isSubtypeOf(mapType) -> false
        returnType.isSubtypeOf(persistableType) -> false
        (returnType.javaType as? Class<*>)?.isInterface == true -> false
        else -> true
    }
}

/**
 * Evaluates whether a given property qualifies as a "relationship" based on its characteristics.
 *
 * This evaluates to true, when
 * - The property is not a delegate (Note: this might change in the future, once we re-design node
 *   properties if Neo4J OGM is completely removed)
 * - The property is an [EdgeList]
 * - The property is referring to a [Collection] of [Node] objects
 * - The property is referring to a [Node]
 * - The property does not have the annotation [DoNotPersist]
 * - The property does not have the annotation [org.neo4j.ogm.annotation.Relationship] with an
 *   incoming direction (Note: We will replace this with our own annotation at some point)
 *
 * @param property the property to be evaluated, belonging to a class implementing the Persistable
 *   interface
 * @return true if the property satisfies the conditions of being a "relationship", false otherwise
 */
private fun isRelationship(property: KProperty1<out Persistable, *>): Boolean {
    val returnType = property.returnType.withNullability(false)

    return when {
        // The next 2 lines ensure that MemoryAddress-Nodes end up in Neo4j
        property.name == "memoryAddress" -> true
        property.name == "prevDFGEdges" -> true
        property.hasAnnotation<DoNotPersist>() -> false
        property.javaField?.type?.simpleName?.contains("Delegate") == true -> false
        property.javaField?.getAnnotation(Relationship::class.java)?.direction == INCOMING -> false
        property.visibility == KVisibility.PRIVATE -> false
        returnType.isSubtypeOf(edgeCollectionType) -> true
        returnType.isSubtypeOf(collectionOfPersistableType) -> true
        returnType.isSubtypeOf(persistableType) -> true
        else -> false
    }
}

/**
 * Retrieves the relational name associated with a property in the context of the raph schema.
 *
 * The `relationshipName` is determined based on the following rules:
 * - If the property is annotated with the `@Relationship` annotation, the value of the annotation
 *   is used as the relationship name, provided it is non-null and not an empty string.
 * - If the property name ends with "Edge", this suffix is removed. This adjustment is made to
 *   account for cases where two variables represent an edge, one named with "Edge" and another as
 *   the delegate without the suffix. The desired name is the one without "Edge".
 * - The resulting name is converted to UPPER_SNAKE_CASE for standardization.
 */
val <K, V> KProperty1<K, V>.relationshipName: String
    get() {
        // If we have a (legacy) Neo4J annotation for our relationship, we take this one
        // Note: We will replace this with something else in the future
        val value = this.javaField?.getAnnotation(Relationship::class.java)?.value
        if (value != null && value != "") {
            return value
        }

        // If the name ends with "Edge", we cut that of, since we always have two
        // variables for real edges, one called "exampleEdge" and one just called
        // "example" (which is only a delegate), but the name we want is "example".
        //
        // Replace camel case with UPPER_CASE
        return this.name.substringBeforeLast("Edge").toUpperSnakeCase()
    }

/**
 * Converts the current string to UPPER_SNAKE_CASE format.
 *
 * Each word boundary in camelCase or PascalCase naming convention is replaced with an underscore,
 * and all characters are converted to uppercase. This is commonly used for representing constants
 * or environment-style variable names.
 *
 * @return A string converted to UPPER_SNAKE_CASE.
 */
fun String.toUpperSnakeCase(): String {
    val pattern = "(?<=.)[A-Z]".toRegex()
    return this.replace(pattern, "_$0").uppercase()
}
