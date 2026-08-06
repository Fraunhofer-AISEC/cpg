/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.core.StreamWriteConstraints
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.module.SimpleKeyDeserializers
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.module.SimpleSerializers
import com.fasterxml.jackson.databind.ser.BeanSerializer
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import com.fasterxml.jackson.databind.ser.ResolvableSerializer
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildrenWithOverlays
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.edges
import de.fraunhofer.aisec.cpg.graph.parseName
import de.fraunhofer.aisec.cpg.persistence.converters.LocationConverter
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.IOException
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.uuid.Uuid

/**
 * CPG graphs can be nested very deeply (think of long call/EOG chains), so we raise Jackson's
 * default read and write nesting limits well beyond what a "normal" JSON document would need.
 */
private const val MAX_NESTING_DEPTH = 10_000

class NameKeySerializer : JsonSerializer<Name>() {
    override fun serialize(value: Name, gen: JsonGenerator, serializers: SerializerProvider) {
        // Convert key object to string — customize your format here
        gen.writeFieldName(value.delimiter + " " + value.toString())
    }
}

class NameKeyDeserializer : KeyDeserializer() {
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any {
        val fqnName = key.substringAfter(" ")
        val delimiter = key.substringBefore(" ")
        // Parse string back into MyKey
        return parseName(fqnName, delimiter)
    }
}

class NodeRegistry {
    private val nodes = mutableMapOf<String, Node>()

    fun register(node: Node) = nodes.put(node.id.toString(), node)

    fun lookup(id: String): Node? = nodes[id]
}

class NodeKeyDeserializer(@JacksonInject val registry: NodeRegistry) : KeyDeserializer() {
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any {
        return registry.lookup(key)
            ?: throw IllegalStateException("Node with id='$key' not registered")
    }
}

class NodeKeyDeserializers(@JacksonInject val registry: NodeRegistry) : SimpleKeyDeserializers() {
    override fun findKeyDeserializer(
        type: JavaType,
        config: DeserializationConfig,
        beanDesc: BeanDescription?,
    ): KeyDeserializer? {
        val raw = type.rawClass
        return if (Node::class.java.isAssignableFrom(raw)) {
            NodeKeyDeserializer(registry)
        } else if (Name::class.java.isAssignableFrom(raw)) {
            NameKeyDeserializer()
        } else if (
            Pair::class.java.isAssignableFrom(raw)
        ) { // || Pair::class.java.isAssignableFrom(type.type)
            PairKeyDeserializer()
        } else if (
            KClass::class.java.isAssignableFrom(raw)
        ) { // || Pair::class.java.isAssignableFrom(type.type)
            KClassKeyDeserializer()
        } else {
            null
        }
    }
}

class KClassSerializer : StdSerializer<Any>(Any::class.java) {
    override fun serialize(value: Any, gen: JsonGenerator, provider: SerializerProvider) {
        if (value is KClass<*>) {
            gen.writeString(value.qualifiedName ?: value.simpleName ?: "unknown")
        } else {
            throw IllegalArgumentException("Unexpected type: ${value.javaClass}")
        }
    }

    override fun serializeWithType(
        value: Any,
        gen: JsonGenerator,
        serializers: SerializerProvider,
        typeSer: TypeSerializer,
    ) {
        serialize(value, gen, serializers)
        // super.serializeWithType(value, gen, serializers, typeSer)
    }
}

class KClassDeserializer : StdDeserializer<KClass<*>>(KClass::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): KClass<*> {
        val className = p.valueAsString
        return Class.forName(className).kotlin
    }
}

class KClassKeySerializer : JsonSerializer<KClass<*>>() {
    @Throws(IOException::class)
    override fun serialize(value: KClass<*>, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeFieldName(value.qualifiedName)
    }
}

class KClassKeyDeserializer : KeyDeserializer() {
    @Throws(IOException::class)
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any {

        val kclass = Class.forName(key).kotlin

        return kclass
    }
}

// KClass<*>
class PairKeySerializer : JsonSerializer<Pair<*, *>>() {
    @Throws(IOException::class)
    override fun serialize(value: Pair<*, *>, gen: JsonGenerator, serializers: SerializerProvider) {
        // Convert the pair to a string key (ensure uniqueness and reversibility)
        val first = (value.first as? KClass<*>)?.qualifiedName ?: value.first.toString()
        val second = (value.second as? KClass<*>)?.qualifiedName ?: value.second.toString()
        val keyStr = "${first}|${second}"
        gen.writeFieldName(keyStr)
    }
}

class PairKeyDeserializer : KeyDeserializer() {
    @Throws(IOException::class)
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any {
        // Expected format: "com.package.ClassA|com.package.ClassB"
        val parts = key.split("|")
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid key format: $key")
        }

        val kclass1 = Class.forName(parts[0]).kotlin
        val kclass2 = Class.forName(parts[1]).kotlin

        return Pair(kclass1, kclass2)
    }
}

class NodeDelegatingDeserializer(
    private var delegate: JsonDeserializer<*>,
    private val registry: NodeRegistry,
) : StdDeserializer<Node>(Node::class.java), ResolvableDeserializer, ContextualDeserializer {

    // Ensure delegate is fully initialized
    override fun resolve(ctxt: DeserializationContext) {
        if (delegate is ResolvableDeserializer) {
            (delegate as ResolvableDeserializer).resolve(ctxt)
        }
    }

    // Handle contextual setup for nested properties
    override fun createContextual(
        ctxt: DeserializationContext,
        property: BeanProperty?,
    ): JsonDeserializer<*> {
        val contextualDelegate =
            if (delegate is ContextualDeserializer) {
                (delegate as ContextualDeserializer).createContextual(ctxt, property)
            } else {
                delegate
            }
        return NodeDelegatingDeserializer(contextualDelegate, registry)
    }

    // Register node after delegating actual deserialization
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Node {
        @Suppress("UNCHECKED_CAST") val node = delegate.deserialize(p, ctxt) as Node
        registry.register(node)
        return node
    }

    override fun deserializeWithType(
        p: JsonParser?,
        ctxt: DeserializationContext?,
        typeDeserializer: TypeDeserializer?,
    ): Any? {
        return delegate.deserializeWithType(p, ctxt, typeDeserializer)
    }

    override fun isCachable(): Boolean = true
}

class UuidDeserializer : StdDeserializer<Uuid>(Uuid::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Uuid {
        val uuid = p.codec.readTree<com.fasterxml.jackson.databind.JsonNode>(p)
        return Uuid.fromLongs(
            uuid.get("mostSignificantBits").asLong(),
            uuid.get("leastSignificantBits").asLong(),
        ) // or Uuid(text), depending on version
    }
}

/**
 * The purpose of this serializer is to wrap the serialization of the node with type information.
 * Jackson omits this information when it is supposed to emit an object id. During deserialization
 * this information is than missing.
 */
class WrappingBeanSerializer(private val defaultSerializer: BeanSerializer) :
    BeanSerializer(defaultSerializer) {

    override fun handledType(): Class<in Any>? {
        return defaultSerializer.handledType()
    }

    override fun usesObjectId(): Boolean {
        return defaultSerializer.usesObjectId()
    }

    override fun withObjectIdWriter(objectIdWriter: ObjectIdWriter): BeanSerializerBase {
        return (defaultSerializer.withObjectIdWriter(objectIdWriter)) as BeanSerializerBase
    }

    override fun serialize(value: Any, gen: JsonGenerator, provider: SerializerProvider) {
        defaultSerializer.serialize(value, gen, provider)
    }

    /**
     * This is the function where we have to apply the workaround. Jackson is supposed to serialize
     * with type when calling this function, but is then running into the subbranch of serializing
     * the object id, neglecting type information.
     */
    override fun serializeWithType(
        value: Any,
        gen: JsonGenerator,
        serializers: SerializerProvider,
        typeSer: TypeSerializer,
    ) {

        // The wrapping serializer and the default serializer share the same ObjectIdWriter. We
        // therefore access it to
        // see if the id was already generated for this value to decide whether we have to apply our
        // workaround
        if (
            this.usesObjectId() &&
                serializers.findObjectId(value, _objectIdWriter?.generator)?.id != null
        ) {
            // If the id is set, jackson will just emit the type information, then failing during
            // deserialization
            // because it needs type information, therefore we emit the information explicitly
            val typeIdInfo = typeSer.typeId(value, value.javaClass, JsonToken.VALUE_STRING)
            typeSer.writeTypePrefix(gen, typeIdInfo)
            defaultSerializer.serializeWithType(value, gen, serializers, typeSer)
            typeSer.writeTypeSuffix(gen, typeIdInfo)
        } else {
            // In case the id was not generated so far, the full object will be serialized and in
            // that case jackson
            // properly prints type information
            defaultSerializer.serializeWithType(value, gen, serializers, typeSer)
        }
    }

    override fun resolve(provider: SerializerProvider) {
        (defaultSerializer as ResolvableSerializer).resolve(provider)
    }

    override fun createContextual(
        provider: SerializerProvider,
        property: BeanProperty?,
    ): JsonSerializer<*> {
        return if (true) {
            // Don't i need a copy of the wrappingBeanSerializer here instead? Such that changes are
            // maintained?
            WrappingBeanSerializer(
                defaultSerializer.createContextual(provider, property) as BeanSerializer
            )
        } else this
    }
}

/**
 * Explicitly deactivating the default behavior to only store references that is annotated in the
 * class header for [Node] and [Edge]. This leads to the nodes and edges in the set being explicitly
 * stored for the first time and a flattening of the graph.
 */
data class CPG(
    @param:JsonIdentityReference(alwaysAsId = false) val nodes: Set<Node> = emptySet(),
    @param:JsonIdentityReference(alwaysAsId = false) val edges: Set<Edge<*>> = emptySet(),
)

/**
 * Builds the [ObjectMapper] used for both serializing and deserializing a [CPG] graph. Keeping a
 * single factory guarantees that the read and write paths stay symmetric: every custom serializer
 * has a matching deserializer and both sides agree on nesting limits, the Kotlin module and
 * property visibility.
 *
 * The [registry] is used on the read path to resolve nodes referenced by id (as map keys or edge
 * endpoints). On the write path it is simply unused.
 */
private fun cpgObjectMapper(registry: NodeRegistry): ObjectMapper {
    val factory =
        JsonFactory.builder()
            .streamWriteConstraints(
                StreamWriteConstraints.builder().maxNestingDepth(MAX_NESTING_DEPTH).build()
            )
            .streamReadConstraints(
                StreamReadConstraints.builder().maxNestingDepth(MAX_NESTING_DEPTH).build()
            )
            .build()

    val cpgModule =
        SimpleModule().apply {
            // Write path: keep the type information on nodes even when Jackson only emits their
            // object id (see [WrappingBeanSerializer]).
            setSerializerModifier(
                object : BeanSerializerModifier() {
                    override fun modifySerializer(
                        config: SerializationConfig,
                        beanDesc: BeanDescription,
                        serializer: JsonSerializer<*>,
                    ): JsonSerializer<*> =
                        if (Node::class.java.isAssignableFrom(beanDesc.beanClass)) {
                            WrappingBeanSerializer(serializer as BeanSerializer)
                        } else {
                            serializer
                        }
                }
            )

            // Read path: register every node in the [registry] as soon as it is deserialized, so
            // that references to it (as map keys or edge endpoints) can be resolved by id.
            setDeserializerModifier(
                object : BeanDeserializerModifier() {
                    override fun modifyDeserializer(
                        config: DeserializationConfig,
                        desc: BeanDescription,
                        deserializer: JsonDeserializer<*>,
                    ): JsonDeserializer<*> =
                        if (Node::class.java.isAssignableFrom(desc.beanClass)) {
                            NodeDelegatingDeserializer(deserializer, registry)
                        } else {
                            deserializer
                        }
                }
            )

            // Complex value types that Jackson cannot (de)serialize out of the box. Each serializer
            // is paired with the deserializer that reverses it.
            setSerializers(
                object : SimpleSerializers() {
                        override fun findSerializer(
                            config: SerializationConfig,
                            type: JavaType,
                            beanDesc: BeanDescription?,
                        ): JsonSerializer<*>? =
                            if (KClass::class.java.isAssignableFrom(type.rawClass)) {
                                KClassSerializer()
                            } else {
                                super.findSerializer(config, type, beanDesc)
                            }
                    }
                    .apply {
                        addSerializer(
                            PhysicalLocation::class.java,
                            LocationConverter.LocationSerializer(),
                        )
                    }
            )
            addDeserializer(KClass::class.java, KClassDeserializer())
            addDeserializer(Uuid::class.java, UuidDeserializer())
            addDeserializer(PhysicalLocation::class.java, LocationConverter.LocationDeserializer())

            // Complex map-key types. The key deserializers are grouped in [NodeKeyDeserializers]
            // because they need access to the [registry].
            addKeySerializer(Name::class.java, NameKeySerializer())
            addKeySerializer(Pair::class.java, PairKeySerializer())
            addKeySerializer(KClass::class.java, KClassKeySerializer())
            setKeyDeserializers(NodeKeyDeserializers(registry))
        }

    return ObjectMapper(factory)
        .findAndRegisterModules()
        .registerKotlinModule()
        .registerModule(cpgModule)
        .setInjectableValues(InjectableValues.Std().addValue(NodeRegistry::class.java, registry))
}

fun serializeToJson(translationResult: TranslationResult): String {
    val objectMapper = cpgObjectMapper(NodeRegistry())

    val allNodes = translationResult.allChildrenWithOverlays<Node>().toMutableSet()
    val allEdges = mutableSetOf<Edge<*>>()
    var toExplore = allNodes.toSet()

    while (toExplore.isNotEmpty()) {
        // Only explore the current frontier, not the whole set of already-known nodes. Exploring
        // `allNodes` on every iteration would re-walk every node again and again, turning the graph
        // collection into O(n^2). Each node is discovered once, so exploring just the frontier
        // visits every node (and therefore every edge) exactly once.
        val (exploredNodes, exploredEdges) =
            toExplore
                .map { it.explore() }
                .let { pairOfLists ->
                    pairOfLists.flatMap { it.first }.filter { it !in allNodes } to
                        pairOfLists.flatMap { it.second }
                }
        allEdges.addAll(exploredEdges)
        allNodes.addAll(exploredNodes)
        toExplore = exploredNodes.toSet()
    }

    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(CPG(allNodes, allEdges))
}

fun Node.explore(): Pair<Set<Node>, Set<Edge<*>>> {
    val edges = this.edges<Edge<*>>().toSet()
    val nodes = edges.flatMap { setOf(it.start, it.end) }.toMutableSet()

    val kClass = this::class as KClass<Node>
    kClass.memberProperties.forEach { prop ->
        prop.isAccessible = true
        val value =
            try {
                prop.get(this)
            } catch (_: Exception) {
                null
            }
        val toUnwrapp = mutableListOf(value)
        while (toUnwrapp.isNotEmpty()) {
            val current = toUnwrapp.removeFirst()
            when (current) {
                is Node -> nodes.add(current)
                is Iterable<*> -> current.forEach { toUnwrapp.add(it) }
                is Array<*> -> current.forEach { toUnwrapp.add(it) }
                is Map<*, *> -> {
                    current.keys.forEach { toUnwrapp.add(it) }
                    current.values.forEach { toUnwrapp.add(it) }
                }
            }
        }
    }

    return Pair(nodes, edges)
}

fun deserializeFromJson(json: String): TranslationResult {
    val registry = NodeRegistry()
    val objectMapper = cpgObjectMapper(registry)

    val cpg = objectMapper.readValue(json, CPG::class.java)
    return cpg.nodes.filterIsInstance<TranslationResult>().first()
}
