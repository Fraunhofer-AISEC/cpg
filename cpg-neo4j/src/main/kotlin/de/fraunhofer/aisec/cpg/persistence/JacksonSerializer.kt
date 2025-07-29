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
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleKeyDeserializers
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.parseName
import java.io.IOException
import kotlin.reflect.KClass

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
        println(property?.fullName)
        println(property?.name + " " + property?.type)
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
        @Suppress("UNCHECKED_CAST") val node = delegate.deserialize(p, ctxt) as Node
        registry.register(node)
        return node
    }

    override fun isCachable(): Boolean = true
}

fun serializeToJson(translationResult: TranslationResult): String {
    val factory =
        JsonFactory.builder()
            .streamWriteConstraints(
                StreamWriteConstraints.builder()
                    .maxNestingDepth(10000) // Set maximum nesting depth to 10,000
                    .build()
            )
            .build()

    val objectMapper = ObjectMapper(factory).registerKotlinModule()

    // objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
    // objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

    return objectMapper
        .apply {
            val module =
                SimpleModule().apply {
                    addKeySerializer(Name::class.java, NameKeySerializer())
                    addKeySerializer(Pair::class.java, PairKeySerializer())
                    addKeySerializer(KClass::class.java, KClassKeySerializer())
                }
            //    module.setSerializerModifier(DepthLimitingModifier(maxDepth = 5))
            registerModule(module)
        }
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(translationResult)
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
                    setKeyDeserializers(NodeKeyDeserializers(registry))
                }
            )
        }
    return objectMapper.readValue(json, TranslationResult::class.java)
}
