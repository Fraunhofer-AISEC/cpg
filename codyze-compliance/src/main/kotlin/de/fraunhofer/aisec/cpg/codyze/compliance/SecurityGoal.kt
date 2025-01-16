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
package de.fraunhofer.aisec.cpg.codyze.compliance

import com.charleskorn.kaml.Yaml
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.io.path.walk
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class SecurityGoal(
    @Serializable(with = NameSerializer::class) override var name: Name,
    val description: String,
    val components: List<@Serializable(with = ComponentSerializer::class) Component?> = listOf(),
    val assumptions: List<String> = listOf(),
    val restrictions: List<String> = listOf(),
    val objectives: List<SecurityObjective>,
) : OverlayNode()

@Serializable
class SecurityObjective(
    @Serializable(with = NameSerializer::class) override var name: Name,
    val description: String,
    val statements: List<String>,
    val components: List<@Serializable(with = ComponentSerializer::class) Component?> = listOf(),
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

/** A custom serializer for the [Component] class. */
class ComponentSerializer : KSerializer<Component?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(Component::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Component?) {
        if (value != null) {
            encoder.encodeString(value.name.toString())
        }
    }

    override fun deserialize(decoder: Decoder): Component? {
        // TODO: find component by name somehow
        return null
    }
}

/** Load all security goals from a directory. */
@OptIn(ExperimentalPathApi::class)
fun loadSecurityGoals(directory: String): List<SecurityGoal> {
    // Walk the directory and load all YAML files
    return Path(directory)
        .walk()
        .filter { it.extension == "yaml" }
        .toList()
        .map { loadSecurityGoal(it) }
}

/** Load a single security goal from a file. */
fun loadSecurityGoal(file: Path): SecurityGoal {
    return Yaml.default.decodeFromString<SecurityGoal>(file.readText())
}
