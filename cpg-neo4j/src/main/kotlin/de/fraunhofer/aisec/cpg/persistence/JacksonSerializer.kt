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
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.StreamWriteConstraints
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.module.SimpleKeyDeserializers
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.module.SimpleSerializers
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter
import com.fasterxml.jackson.databind.ser.BeanSerializer
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import com.fasterxml.jackson.databind.ser.ResolvableSerializer
import com.fasterxml.jackson.databind.ser.Serializers
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
import de.fraunhofer.aisec.cpg.helpers.neo4j.LocationConverter
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.IOException
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.uuid.Uuid

@JsonSerialize(using = KClassSerializer::class) interface KClassMixin

val DEFAULT_TYPING = ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE
val typeIncludedAs: JsonTypeInfo.As = JsonTypeInfo.As.PROPERTY
val typeValidator: PolymorphicTypeValidator = // LaissezFaireSubTypeValidator.instance
    BasicPolymorphicTypeValidator.builder().allowIfSubType("de.fraunhofer.aisec.cpg.").build()

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
        println("Is this ever executed? $key")
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

/*class NodeDelegatingDeserializer(delegate: JsonDeserializer<*>, val registry: NodeRegistry) :
    StdDeserializer<Node>(Node::class.java) {

    private val delegatee: JsonDeserializer<*> = delegate

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Node {
        @Suppress("UNCHECKED_CAST") val node = delegatee.deserialize(p, ctxt) as Node
        registry.register(node)
        return node
    }

    override fun isCachable(): Boolean = true
}*/
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
    override fun deserializeKey(
        key: String,
        ctxt: com.fasterxml.jackson.databind.DeserializationContext,
    ): Any {

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
    override fun deserializeKey(
        key: String,
        ctxt: com.fasterxml.jackson.databind.DeserializationContext,
    ): Any {
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
        println("createContextual: " + property?.fullName)
        println("createContextual type: " + property?.name + " " + property?.type)
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
        println("Parent: " + p.parsingContext?.parent?.currentValue as? Node)
        println("Type: " + p.parsingContext.typeDesc())
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
        val uuid = p.codec.readTree<JsonNode>(p)
        return Uuid.fromLongs(
            uuid.get("mostSignificantBits").asLong(),
            uuid.get("leastSignificantBits").asLong(),
        ) // or Uuid(text), depending on version
    }
}

class KClassBeanSerializerModifier : BeanSerializerModifier() {
    override fun modifySerializer(
        config: SerializationConfig,
        beanDesc: BeanDescription,
        serializer: JsonSerializer<*>,
    ): JsonSerializer<*> {
        println(beanDesc.beanClass)
        if (KClass::class.java.isAssignableFrom(beanDesc.beanClass)) {
            return KClassSerializer()
        }
        return serializer
    }
}

class KClassSerializers : Serializers.Base() {
    override fun findSerializer(
        config: SerializationConfig,
        javaType: JavaType,
        beanDesc: BeanDescription?,
    ): JsonSerializer<*>? {
        val raw = javaType.rawClass
        return if (KClass::class.java.isAssignableFrom(raw)) {
            KClassSerializer() // returns your serializer
        } else null
    }
}

class LoggingPropertyWriter(base: BeanPropertyWriter) : BeanPropertyWriter(base) {
    override fun serializeAsField(bean: Any?, gen: JsonGenerator, prov: SerializerProvider) {
        println("Serializing field '${name}' of ${bean?.javaClass?.name}")
        super.serializeAsField(bean, gen, prov)
    }
}

class LoggingBeanSerializerModifier : BeanSerializerModifier() {
    override fun changeProperties(
        config: SerializationConfig,
        beanDesc: BeanDescription,
        beanProperties: MutableList<BeanPropertyWriter>,
    ): MutableList<BeanPropertyWriter> {
        return beanProperties.map { LoggingPropertyWriter(it) }.toMutableList()
    }
}

class DebugSerializer : StdSerializer<Any>(Any::class.java) {
    override fun serialize(value: Any, gen: JsonGenerator, provider: SerializerProvider) {
        println("DEBUG serializing: ${value.javaClass.name}")
        provider.defaultSerializeValue(value, gen)
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

fun serializeToJson(translationResult: TranslationResult): String {
    val factory =
        JsonFactory.builder()
            .streamWriteConstraints(
                StreamWriteConstraints.builder()
                    .maxNestingDepth(10000) // Set maximum nesting depth to 10,000
                    .build()
            )
            .build()

    val loggingModule =
        SimpleModule().apply { setSerializerModifier(LoggingBeanSerializerModifier()) }

    val debugModule =
        object : SimpleModule() {
            override fun setupModule(context: SetupContext) {
                super.setupModule(context)
                context.addSerializers(
                    object : Serializers.Base() {
                        override fun findSerializer(
                            config: SerializationConfig,
                            type: JavaType,
                            beanDesc: BeanDescription?,
                        ): JsonSerializer<*>? {
                            val cls = type.rawClass
                            val name = cls.name
                            // Log only if the class is related to Kotlin reflection or unknown
                            // internals
                            if (
                                name.contains("kotlin.reflect") ||
                                    name.startsWith("kotlin.") ||
                                    name.contains("internal")
                            ) {
                                println("Inspecting Kotlin-reflection/UUID type: $name")
                            }
                            if (KClass::class.java.isAssignableFrom(cls)) {
                                println("Match KClass subtype: $name — using KClassSerializer")
                                return KClassSerializer()
                            }
                            return null
                        }
                    }
                )
            }
        }

    val objectMapper =
        ObjectMapper(factory)
            // .deactivateDefaultTyping()
            .findAndRegisterModules()
            // .registerModule(SerializationModule(Node::class.java))
            // .registerModule(loggingModule)
            // .registerModule(debugModule)
            // .addMixIn(KClass::class.java, KClassMixin::class.java)
            .registerModule(
                SimpleModule().apply {
                    setSerializerModifier(
                        object : BeanSerializerModifier() {

                            override fun modifySerializer(
                                config: SerializationConfig,
                                beanDesc: BeanDescription,
                                serializer: JsonSerializer<*>,
                            ): JsonSerializer<*> {
                                println("modifySerializer")
                                return if (Node::class.java.isAssignableFrom(beanDesc.beanClass)) {
                                    println("Wrapping ${beanDesc.beanClass}")
                                    @Suppress("UNCHECKED_CAST")
                                    WrappingBeanSerializer(serializer as BeanSerializer)
                                        as JsonSerializer<Any>
                                } else {
                                    serializer
                                }
                            }
                        }
                    )
                    setSerializers(
                        object : SimpleSerializers() {
                            override fun findSerializer(
                                config: SerializationConfig,
                                type: JavaType,
                                beanDesc: BeanDescription?,
                            ): JsonSerializer<*>? {
                                println("findSerializers")
                                val raw = type.rawClass
                                if (KClass::class.java.isAssignableFrom(raw)) {
                                    // Optionally log match for debugging:
                                    println(">>> Using KClassSerializer for: ${raw.name}")
                                    return KClassSerializer()
                                }
                                println(">>> KClassSerializer not matching for: ${raw.name}")
                                return null // let Jackson handle other types
                            }
                        }
                    )
                }
            )
            .registerKotlinModule()

    // objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
    // objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    val allNodes = translationResult.allChildrenWithOverlays<Node>().toMutableSet()
    val allEdges = mutableSetOf<Edge<*>>()
    var toExplore = allNodes.toSet()

    while (toExplore.isNotEmpty()) {
        val (exploredNodes, exploredEdges) =
            allNodes
                .map { it.explore() }
                .let { pairOfLists ->
                    pairOfLists.flatMap { it.first }.filter { it !in allNodes } to
                        pairOfLists.flatMap { it.second }
                }
        allEdges.addAll(exploredEdges)
        allNodes.addAll(exploredNodes)
        toExplore = exploredNodes.toSet()
    }

    val toSerialize = translationResult

    return objectMapper
        .apply {
            val module =
                SimpleModule().apply {
                    // setSerializerModifier(KClassBeanSerializerModifier())

                    addSerializer(
                        PhysicalLocation::class.java,
                        LocationConverter.LocationSerializer(),
                    )

                    // val kclassImpl = Class.forName("kotlin.reflect.jvm.internal.KClassImpl")
                    // addSerializer(kclassImpl, KClassSerializer())

                    addKeySerializer(Name::class.java, NameKeySerializer())
                    addKeySerializer(Pair::class.java, PairKeySerializer())
                    addKeySerializer(KClass::class.java, KClassKeySerializer())
                }
            //    module.setSerializerModifier(DepthLimitingModifier(maxDepth = 5))
            registerModule(module)
        }
        //    .activateDefaultTyping(typeValidator, DEFAULT_TYPING, typeIncludedAs)
        //    .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(CPG(allNodes, allEdges))
}

fun Node.explore(): Pair<Set<Node>, Set<Edge<*>>> {
    val edges = this.edges<Edge<*>>().toSet()
    val nodes = edges.flatMap { setOf(it.start, it.end) }.toMutableSet()
    this.javaClass.declaredFields.forEach { field -> }

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

    val module =
        SimpleModule().apply {
            setDeserializerModifier(
                object : BeanDeserializerModifier() {
                    override fun modifyDeserializer(
                        config: DeserializationConfig,
                        desc: BeanDescription,
                        deserializer: JsonDeserializer<*>,
                    ): JsonDeserializer<*> {
                        return if (Node::class.java.isAssignableFrom(desc.beanClass)) {
                            NodeDelegatingDeserializer(deserializer, registry)
                        } else {
                            deserializer
                        }
                    }
                }
            )
        }

    val objectMapper =
        ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY).apply {
            registerModule(module)

            // Allow injection of the registry instance
            setInjectableValues(InjectableValues.Std().addValue(NodeRegistry::class.java, registry))

            // Register the deserializer so Jackson knows to use it
            registerModule(
                SimpleModule().apply {
                    /*
                    addKeyDeserializer(Node::class.java, NodeKeyDeserializer(registry))
                    addKeyDeserializer(
                        FunctionDeclaration::class.java,
                        NodeKeyDeserializer(registry),
                    )
                    addKeyDeserializer(ValueDeclaration::class.java, NodeKeyDeserializer(registry))
                    */

                    addDeserializer(
                        PhysicalLocation::class.java,
                        LocationConverter.LocationDeserializer(),
                    )
                    addDeserializer(Uuid::class.java, UuidDeserializer())
                    addDeserializer(KClass::class.java, KClassDeserializer())
                    setKeyDeserializers(NodeKeyDeserializers(registry))
                }
            )
        }

    // objectMapper
    //    .activateDefaultTyping(typeValidator, DEFAULT_TYPING, typeIncludedAs)
    //    .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
    val cpg = objectMapper.readValue(json, CPG::class.java)
    return cpg.nodes.filterIsInstance<TranslationResult>().first()
}
