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
package de.fraunhofer.aisec.codyze.compliance

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

@Serializable
data class SecurityGoal(
    @Serializable(with = NameSerializer::class) override var name: Name,
    val description: String,
    val components: List<@Contextual Component?> = listOf(),
    val assumptions: List<String> = listOf(),
    val restrictions: List<String> = listOf(),
    val objectives: List<SecurityObjective>,
) : OverlayNode()

@Serializable
class SecurityObjective(
    @Serializable(with = NameSerializer::class) override var name: Name,
    val description: String,
    val statements: List<String>,
    val components: List<@Contextual Component?> = listOf(),
    val assumptions: List<String> = listOf(),
    val restrictions: List<String> = listOf(),
) : OverlayNode()

/** A custom serializer for the [Name] class. */
class NameSerializer : KSerializer<Name> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(Name::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Name) {
        encoder.encodeString(value.localName)
    }

    override fun deserialize(decoder: Decoder): Name {
        return Name(decoder.decodeString())
    }
}

/**
 * A custom serializer for the [Component] class. If the [result] is non-null, it is used to resolve
 * the component name in an actual [Component] of the [result]. Otherwise, a new [Component] with
 * the given name is returned.
 */
class ComponentSerializer(val result: TranslationResult?) : KSerializer<Component> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(Component::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Component) {
        encoder.encodeString(value.name.toString())
    }

    override fun deserialize(decoder: Decoder): Component {
        // Use the context to find the component by name
        val componentName = decoder.decodeString()

        return result?.components?.firstOrNull { it.name.localName == componentName }
            ?: Component(result?.finalCtx ?: TranslationContext.EmptyTranslationContext).also {
                it.name = Name(componentName)
            }
    }
}

/**
 * Load all security goals from a directory. If a [result] is given, it will be used to resolve
 * component names.
 */
fun loadSecurityGoals(directory: Path, result: TranslationResult? = null): List<SecurityGoal> {
    // Walk the directory and load all YAML files
    return directory
        .toFile()
        .walk()
        .filter { it.extension == "yaml" }
        .toList()
        .map { loadSecurityGoal(it, result) }
}

/**
 * Load a single security goal from a file. If a [result] is given, it will be used to resolve
 * component names.
 */
fun loadSecurityGoal(file: File, result: TranslationResult? = null): SecurityGoal {
    return yaml(result).decodeFromString<SecurityGoal>(file.readText())
}

/**
 * Load a single security goal from an input stream. If a [result] is given, it will be used to
 * resolve component names.
 */
fun loadSecurityGoal(stream: InputStream, result: TranslationResult? = null): SecurityGoal {
    return yaml(result).decodeFromStream<SecurityGoal>(stream)
}

/**
 * This function returns a [com.charleskorn.kaml.Yaml] instance that is configured to use the given
 * [result] to resolve components.
 */
private fun yaml(result: TranslationResult?): Yaml {
    val module = SerializersModule { contextual(Component::class, ComponentSerializer(result)) }
    return Yaml(serializersModule = module)
}
