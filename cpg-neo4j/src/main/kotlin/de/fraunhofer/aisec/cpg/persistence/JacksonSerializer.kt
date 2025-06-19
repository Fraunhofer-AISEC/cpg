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

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonStreamContext
import com.fasterxml.jackson.core.StreamWriteConstraints
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.fraunhofer.aisec.cpg.TranslationResult

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
    val objectMapper = ObjectMapper()
    return objectMapper.readValue(json, TranslationResult::class.java)
}
