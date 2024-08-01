/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.edge

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.Persistable
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible
import org.neo4j.ogm.annotation.*
import org.neo4j.ogm.annotation.typeconversion.Convert
import org.slf4j.LoggerFactory

@RelationshipEntity
class AstChild<T : Node> : PropertyEdge<T> {
    constructor(
        start: Node,
        end: T,
        properties: MutableMap<Properties, Any?>? = null
    ) : super(start, end, properties) {
        end.astParent = start
    }
}

/**
 * This class represents an edge between two [Node] objects in a Neo4J graph. It can be used to
 * store additional information that relate to the relationship between the two nodes that belong to
 * neither of the two nodes directly.
 *
 * An example would be the name (in this case `a`) of an argument between a [CallExpression] (`foo`)
 * and its argument (a [Literal] of `2`) in languages that support keyword arguments, such as
 * Python:
 * ```python
 * foo("bar", a = 2)
 * ```
 */
@RelationshipEntity
open class PropertyEdge<T : Node> : Persistable {
    /** Required field for object graph mapping. It contains the node id. */
    @field:Id @field:GeneratedValue private val id: Long? = null

    // Node where the edge is outgoing
    @field:StartNode var start: Node

    // Node where the edge is ingoing
    @field:EndNode var end: T

    constructor(start: Node, end: T, properties: MutableMap<Properties, Any?>? = null) {
        this.start = start
        this.end = end
        this.properties =
            if (properties != null) {
                properties
            } else {
                EnumMap(Properties::class.java)
            }
    }

    constructor(propertyEdge: PropertyEdge<T>) {
        start = propertyEdge.start
        end = propertyEdge.end
        properties = EnumMap(Properties::class.java)
        properties.putAll(propertyEdge.properties)
    }

    open val label: String = "EDGE"

    /** Map containing all properties of an edge */
    @Convert(PropertyEdgeConverter::class) private var properties: MutableMap<Properties, Any?>

    fun getProperty(property: Properties): Any? {
        return properties.getOrDefault(property, null)
    }

    /**
     * Adds a property to a [PropertyEdge] If the object is not a built-in type you must provide a
     * serializer and deserializer in the [PropertyEdgeConverter]
     *
     * @param property String containing the name of the property
     * @param value Object containing the value of the property
     */
    fun addProperty(property: Properties, value: Any?) {
        properties[property] = value
    }

    fun addProperties(propertyMap: Map<Properties, Any?>?) {
        propertyMap?.let { properties.putAll(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PropertyEdge<*>) return false

        return (properties == other.properties && start == other.start && end == other.end)
    }

    fun propertyEquals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj !is PropertyEdge<*>) return false
        return properties == obj.properties
    }

    /**
     * Checks if the properties of the edge contain the given properties with the specified values.
     */
    fun containsProperties(props: Map<Properties, Any?>): Boolean {
        return properties.entries.containsAll(props.entries)
    }

    override fun hashCode(): Int {
        return Objects.hash(end, properties)
    }

    companion object {
        protected val log = LoggerFactory.getLogger(PropertyEdge::class.java)

        fun <S : PropertyEdge<*>?> findPropertyEdgesByPredicate(
            edges: Collection<S>,
            predicate: (S) -> Boolean
        ): List<S> {
            return edges.filter(predicate)
        }

        /**
         * Add/Update index element of list of PropertyEdges
         *
         * @param propertyEdges propertyEdge list
         * @return new PropertyEdge list with updated index property
         */
        fun <T : Node> applyIndexProperty(
            propertyEdges: List<PropertyEdge<T>>
        ): List<PropertyEdge<T>> {
            for ((counter, propertyEdge) in propertyEdges.withIndex()) {
                propertyEdge.addProperty(Properties.INDEX, counter)
            }
            return propertyEdges
        }

        /**
         * Unwraps this property edge into a list of its target nodes.
         *
         * @param collection the collection of edges
         * @param outgoing whether it is outgoing or not
         * @param <T> the type of the edges
         * @return the list of target nodes </T>
         */
        @JvmStatic
        @JvmOverloads
        fun <T : Node> unwrap(
            collection: List<PropertyEdge<T>>,
            outgoing: Boolean = true
        ): List<T> {
            return collection.map { if (outgoing) it.end else it.start as T }
        }

        /**
         * Checks if an Object is a PropertyEdge or a collection of PropertyEdges
         *
         * @param f Field containing the object
         * @param obj object that is checked if it is a PropertyEdge
         * @return true if obj is/contains a PropertyEdge
         */
        @JvmStatic
        fun checkForPropertyEdge(f: Field, obj: Any?): Boolean {
            if (obj is PropertyEdge<*>) {
                return true
            } else if (obj is Collection<*>) {
                val collectionTypes =
                    listOf(*(f.genericType as ParameterizedType).actualTypeArguments)
                for (t in collectionTypes) {
                    if (t is ParameterizedType) {
                        return t.rawType == PropertyEdge::class.java
                    } else if (PropertyEdge::class.java == t) {
                        return true
                    }
                }
            }
            return false
        }

        @JvmStatic
        fun <E : Node> propertyEqualsList(
            propertyEdges: List<PropertyEdge<E>>?,
            propertyEdges2: List<PropertyEdge<E>>?
        ): Boolean {
            // Check, if the first edge is null
            if (propertyEdges == null) {
                // They can only be equal now, if the second one is also null
                return propertyEdges2 == null
            }

            // Otherwise, try to compare the contents of the lists with the propertyEquals (the
            // second one still might be null)
            if (propertyEdges.size == propertyEdges2?.size) {
                for (i in propertyEdges.indices) {
                    if (!propertyEdges[i].propertyEquals(propertyEdges2[i])) {
                        return false
                    }
                }
                return true
            }
            return false
        }
    }
}

/** Can be used to describe a generic set of property edge. */
class Dataflows() :
    AbstractPropertyEdges<Node, Dataflow>(
        init = { start, end, properties -> Dataflow(start, end) },
        createEdges = ::Dataflows
    )

/** Can be used to describe a generic set of property edge. */
class PropertyEdges<T : Node>() :
    AbstractPropertyEdges<T, PropertyEdge<T>>(init = ::PropertyEdge, createEdges = ::PropertyEdges)

/** This property edge list describes elements that are AST children of a node. */
class AstChildren<T : Node>() :
    AbstractPropertyEdges<T, AstChild<T>>(init = ::AstChild, createEdges = ::AstChildren)

/**
 * This class extends a list of property edges. This allows us to use list of property edges more
 * conveniently.
 */
abstract class AbstractPropertyEdges<T : Node, P : PropertyEdge<T>>(
    var init: (start: Node, end: T, properties: MutableMap<Properties, Any?>) -> P,
    var createEdges: () -> AbstractPropertyEdges<T, P>,
) : ArrayList<P>() {

    override fun add(e: P): Boolean {
        // Make sure, the index is always set
        e.addProperty(Properties.INDEX, this.size)

        return super.add(e)
    }

    override fun add(index: Int, element: P) {
        // Make sure, the index is always set
        element.addProperty(Properties.INDEX, index)

        return super.add(index, element)
    }

    override fun equals(o: Any?): Boolean {
        if (o !is AbstractPropertyEdges<*, *>) return false

        // Otherwise, try to compare the contents of the lists with the propertyEquals method
        if (this.size == o.size) {
            for (i in this.indices) {
                if (!this[i].propertyEquals(o[i])) {
                    return false
                }
            }
            return true
        }

        return false
    }

    fun add(
        start: Node,
        end: T,
        properties: MutableMap<Properties, Any?> = EnumMap(Properties::class.java)
    ) {
        val edge = init(start, end, properties)

        // Add it
        this.add(edge)
    }

    fun resetTo(nodes: Collection<T>, holder: Node, outgoing: Boolean = true) {
        clear()
        for (n in nodes) {
            if (outgoing) {
                add(holder, n)
            } else {
                add(n, holder as T)
            }
        }
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

/**
 * This class can be used to implement
 * [delegated properties](https://kotlinlang.org/docs/delegated-properties.html) in [Node] classes.
 * The most common use case is to have a property that is a list of [PropertyEdge] objects (for
 * persistence) and a second (delegated) property that allows easy access just to the connected
 * nodes of the individual edges for in-memory access.
 *
 * For example:
 * ```kotlin
 *
 * class MyNode {
 *   @Relationship(value = "EXPRESSIONS", direction = "OUTGOING")
 *   @AST
 *   var expressionsEdges = mutableListOf<PropertyEdge<Expression>>()
 *   var expressions by PropertyEdgeDelegate(MyNode::expressionsEdges)
 * }
 * ```
 *
 * This class is intentionally marked with [Transient], so that the delegated properties are not
 * transferred to the Neo4J OGM. Only the property that contains the property edges should be
 * persisted in the graph database.
 */
@Transient
class PropertyEdgeDelegate<PropertyType : Node, NodeType : Node>(
    val edgeProperty:
        KProperty1<NodeType, AbstractPropertyEdges<PropertyType, out PropertyEdge<PropertyType>>>,
    val outgoing: Boolean = true,
) {
    operator fun getValue(thisRef: NodeType, property: KProperty<*>): List<PropertyType> {
        return PropertyEdge.unwrap<PropertyType>(
            edgeProperty.get(thisRef) as List<PropertyEdge<PropertyType>>,
            outgoing
        )
    }

    operator fun setValue(thisRef: NodeType, property: KProperty<*>, value: List<PropertyType>) {
        var list = edgeProperty.get(thisRef)
        if (edgeProperty is KMutableProperty1) {
            val callable = edgeProperty.setter
            callable.isAccessible = true
            list.resetTo(value, thisRef as Node, outgoing)
        }
    }
}

/** Similar to a [PropertyEdgeDelegate], but with a [Set] instead of [List]. */
@Transient
class PropertyEdgeSetDelegate<PropertyType : Node, NodeType : Node>(
    val edgeProperty:
        KProperty1<NodeType, AbstractPropertyEdges<PropertyType, out PropertyEdge<PropertyType>>>,
    val outgoing: Boolean = true,
) {
    operator fun getValue(thisRef: NodeType, property: KProperty<*>): MutableSet<PropertyType> {
        return PropertyEdge.unwrap(edgeProperty.get(thisRef).toList(), outgoing).toMutableSet()
    }

    operator fun setValue(
        thisRef: NodeType,
        property: KProperty<*>,
        value: MutableSet<PropertyType>
    ) {
        var list = edgeProperty.get(thisRef)
        if (edgeProperty is KMutableProperty1) {
            val callable = edgeProperty.setter
            callable.isAccessible = true
            list.resetTo(value, thisRef, outgoing)
            edgeProperty.setter.call(
                thisRef,
            )
        }
    }
}
