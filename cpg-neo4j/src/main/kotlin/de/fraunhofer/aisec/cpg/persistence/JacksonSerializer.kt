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
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonStreamContext
import com.fasterxml.jackson.core.StreamWriteConstraints
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node

class DepthLimitingSerializer<T>(
    private val delegate: JsonSerializer<T>,
    private val maxDepth: Int,
) : JsonSerializer<T>() {
    override fun serialize(value: T, gen: JsonGenerator, serializers: SerializerProvider) {
        // getCurrentDepth: root object is depth=0, each nested object/array increments.
        fun currentDepth(ctx: JsonStreamContext?): Int {
            if (ctx == null) return -1
            return 1 + currentDepth(ctx.parent)
        }

        val depth = currentDepth(gen.outputContext)
        if (depth > maxDepth) {
            println(depth)
            // short‐circuit: write a placeholder, or just the object’s ID, etc.
            // Here I would like to write the object ID or something to pick up later
            gen.writeString("[truncated at depth $maxDepth]")
        } else {
            // normal serialization
            delegate.serialize(value, gen, serializers)
        }
    }
}

class DepthLimitingModifier(private val maxDepth: Int) : BeanSerializerModifier() {
    override fun modifySerializer(
        config: SerializationConfig,
        beanDesc: BeanDescription,
        serializer: JsonSerializer<*>,
    ): JsonSerializer<*> {

        println(serializer.javaClass.name)
        if (serializer is BeanSerializerBase && !serializer.usesObjectId()) {
            return DepthLimitingSerializer(serializer as JsonSerializer<Any>, maxDepth)
        }
        return serializer
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

class NodeDelegatingDeserializer(delegate: JsonDeserializer<*>, val registry: NodeRegistry) :
    StdDeserializer<Node>(Node::class.java) {

    private val delegatee: JsonDeserializer<*> = delegate

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Node {
        @Suppress("UNCHECKED_CAST") val node = delegatee.deserialize(p, ctxt) as Node
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
        // .apply {
        //    val module = SimpleModule()
        //    module.setSerializerModifier(DepthLimitingModifier(maxDepth = 5))
        //    registerModule(module)
        // }
        // .writerWithDefaultPrettyPrinter()
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
        ObjectMapper().apply {
            registerModule(module)

            // Allow injection of the registry instance
            setInjectableValues(InjectableValues.Std().addValue(NodeRegistry::class.java, registry))

            // Register the deserializer so Jackson knows to use it
            registerModule(
                SimpleModule().apply {
                    addKeyDeserializer(Node::class.java, NodeKeyDeserializer(registry))
                }
            )
        }
    return objectMapper.readValue(json, TranslationResult::class.java)
}
