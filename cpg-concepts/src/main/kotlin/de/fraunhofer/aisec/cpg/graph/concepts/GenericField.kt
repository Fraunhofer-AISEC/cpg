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
package de.fraunhofer.aisec.cpg.graph.concepts

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.persistence.CompositeAttributeConverter

/**
 * A single value held by a [GenericProperties] map. The nested implementations cover the value
 * kinds an LLM-defined property can declare via its `type`: plain text ([StringValue]), the usual
 * scalars ([IntegerValue], [FloatValue], [BooleanValue]), and - when the declared type is
 * `"NodeReference"` - an actual reference to another [Node] in the graph ([NodeReferenceValue])
 * instead of a stringified id.
 *
 * This is deliberately an `interface` rather than a `sealed` hierarchy so that projects building on
 * top of the CPG can contribute their own value kinds (e.g. a domain-specific enum or a reference
 * to something outside the graph) without changing this module. The trade-off is that a `when` over
 * a [GenericPropertyValue] is never exhaustive and therefore always needs an `else` branch; use
 * [rawValue] when all that is needed is the underlying value regardless of its kind.
 */
interface GenericPropertyValue {
    /**
     * The underlying value, boxed as [Any]. Lets consumers that do not care about the specific kind
     * (logging, serialization, generic queries) read any [GenericPropertyValue] - including kinds
     * contributed by other projects - without an exhaustive `when`.
     */
    val rawValue: Any?

    data class StringValue(val value: String) : GenericPropertyValue {
        override val rawValue: Any
            get() = value
    }

    /** Holds any integral value. [Long] is used so that no declared integral type can overflow. */
    data class IntegerValue(val value: Long) : GenericPropertyValue {
        override val rawValue: Any
            get() = value
    }

    /**
     * Holds any floating-point value. [Double] is used so that no declared floating-point type
     * loses precision.
     */
    data class FloatValue(val value: Double) : GenericPropertyValue {
        override val rawValue: Any
            get() = value
    }

    data class BooleanValue(val value: Boolean) : GenericPropertyValue {
        override val rawValue: Any
            get() = value
    }

    data class NodeReferenceValue(val node: Node) : GenericPropertyValue {
        override val rawValue: Any
            get() = node
    }

    companion object {
        /** Declared type names that are parsed into an [IntegerValue]. */
        val INTEGER_TYPES = setOf("int", "integer", "long", "short", "byte")

        /** Declared type names that are parsed into a [FloatValue]. */
        val FLOAT_TYPES = setOf("float", "double", "number", "decimal", "real")

        /** Declared type names that are parsed into a [BooleanValue]. */
        val BOOLEAN_TYPES = setOf("bool", "boolean")

        /**
         * Converts the string representation [value] of a property into the [GenericPropertyValue]
         * matching its declared [type].
         *
         * Note that reference-like types (such as `"NodeReference"`) are not handled here, because
         * resolving them requires a context this module has no access to. Callers must handle those
         * before delegating to this function.
         *
         * @return the parsed value, a [StringValue] if [type] is `null`, blank, or not a recognized
         *   scalar type name, or `null` if [type] *is* a recognized scalar type name but [value]
         *   cannot be parsed as it (so the caller can report the mismatch instead of silently
         *   storing text).
         */
        fun of(type: String?, value: String): GenericPropertyValue? {
            return when (type?.trim()?.lowercase().orEmpty()) {
                in INTEGER_TYPES -> value.trim().toLongOrNull()?.let { IntegerValue(it) }
                in FLOAT_TYPES -> value.trim().toDoubleOrNull()?.let { FloatValue(it) }
                in BOOLEAN_TYPES ->
                    when (value.trim().lowercase()) {
                        "true" -> BooleanValue(true)
                        "false" -> BooleanValue(false)
                        else -> null
                    }
                else -> StringValue(value)
            }
        }
    }
}

/**
 * Represents a generic set of properties for a concept or operation. This can be used to store
 * arbitrary "fields". The key represents the name of the property, and the value is a
 * [GenericPropertyValue] carrying the property's value in the kind matching its declared type.
 *
 * When persisting to a graph database, these properties are split across the two mechanisms a graph
 * database offers, because they hold two different kinds of thing:
 * - Scalar values are flattened into properties of the owning node by [GenericPropertiesConverter],
 *   each prefixed with [GenericPropertiesConverter.GRAPH_PROPERTY_PREFIX].
 * - A [GenericPropertyValue.NodeReferenceValue] is not a value at all but an edge, so it is
 *   persisted as an actual relationship (see [nodeReferences] and [GenericPropertyReferences]),
 *   keeping it traversable in the database instead of degrading it to a stringified id.
 */
data class GenericProperties(val properties: Map<String, GenericPropertyValue>) {
    /**
     * The subset of [properties] that reference another [Node] in the graph, keyed by property
     * name. These are persisted as relationships rather than as node properties.
     */
    val nodeReferences: Map<String, Node>
        get() =
            properties
                .mapNotNull { (name, value) ->
                    (value as? GenericPropertyValue.NodeReferenceValue)?.let { name to it.node }
                }
                .toMap()
}

/**
 * Flattens the scalar part of a [GenericProperties] into individual properties of the owning node,
 * so that generic concepts and operations become queryable in a graph database instead of being
 * skipped during persistence.
 *
 * Every key is prefixed with [GRAPH_PROPERTY_PREFIX] because the property names originate from
 * outside the CPG (e.g. from an LLM-defined concept schema) and would otherwise be free to collide
 * with the owning node's own properties such as `name`, `code`, or `location`.
 *
 * Values are mapped to the types a graph database can store natively:
 * [GenericPropertyValue.StringValue] to `String`, [GenericPropertyValue.IntegerValue] to `Long`,
 * [GenericPropertyValue.FloatValue] to `Double`, and [GenericPropertyValue.BooleanValue] to
 * `Boolean`. A [GenericPropertyValue.NodeReferenceValue] is deliberately skipped here, since it is
 * persisted as a relationship instead. Any value kind contributed by another project is stored via
 * its [GenericPropertyValue.rawValue] if that is already a natively supported type, and as its
 * string representation otherwise.
 */
class GenericPropertiesConverter : CompositeAttributeConverter<GenericProperties> {
    override fun toGraphProperty(value: GenericProperties): Map<String, *> {
        return value.properties
            .mapNotNull { (name, propertyValue) ->
                when (propertyValue) {
                    // Persisted as a relationship, not as a property of this node.
                    is GenericPropertyValue.NodeReferenceValue -> null
                    is GenericPropertyValue.StringValue -> propertyValue.value
                    is GenericPropertyValue.IntegerValue -> propertyValue.value
                    is GenericPropertyValue.FloatValue -> propertyValue.value
                    is GenericPropertyValue.BooleanValue -> propertyValue.value
                    // A value kind contributed by another project.
                    else ->
                        when (val raw = propertyValue.rawValue) {
                            null -> null
                            is String,
                            is Long,
                            is Double,
                            is Boolean -> raw
                            is Number -> raw.toDouble()
                            else -> raw.toString()
                        }
                }?.let { "$GRAPH_PROPERTY_PREFIX$name" to it }
            }
            .toMap()
    }

    /**
     * Rebuilds a [GenericProperties] from the flattened graph properties in [value], recognizing
     * the keys written by [toGraphProperty] and ignoring all others.
     *
     * Note that node references cannot be restored here, because they are persisted as
     * relationships and are therefore not part of [value]; they have to be re-attached from the
     * relationships of the owning node.
     */
    override fun toEntityAttribute(value: Map<String, *>?): GenericProperties {
        val properties =
            value
                .orEmpty()
                .mapNotNull { (key, graphValue) ->
                    if (!key.startsWith(GRAPH_PROPERTY_PREFIX)) return@mapNotNull null
                    val propertyValue =
                        when (graphValue) {
                            is String -> GenericPropertyValue.StringValue(graphValue)
                            is Boolean -> GenericPropertyValue.BooleanValue(graphValue)
                            is Double,
                            is Float ->
                                GenericPropertyValue.FloatValue((graphValue as Number).toDouble())
                            is Number -> GenericPropertyValue.IntegerValue(graphValue.toLong())
                            else -> return@mapNotNull null
                        }
                    key.removePrefix(GRAPH_PROPERTY_PREFIX) to propertyValue
                }
                .toMap()
        return GenericProperties(properties)
    }

    companion object {
        /**
         * Prefix for the flattened graph properties, keeping externally defined property names from
         * colliding with the owning node's own properties.
         */
        const val GRAPH_PROPERTY_PREFIX = "genericProperty."
    }
}
