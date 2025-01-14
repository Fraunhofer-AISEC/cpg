package de.fraunhofer.aisec.cpg.codyze.compliance

import com.charleskorn.kaml.Yaml
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.io.path.walk

@Serializable
class SecurityGoal(
    @Serializable(with = NameSerializer::class)
    override var name: Name
) : OverlayNode() {
}

class SecurityObjective : OverlayNode() {

}

class SecurityStatement : OverlayNode() {

}

/**
 * A custom serializer for the Name class.
 */
class NameSerializer : KSerializer<Name> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(Name::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Name) {
        encoder.encodeString(value.localName)
    }

    override fun deserialize(decoder: Decoder): Name {
        return Name(decoder.decodeString())
    }

}

@OptIn(ExperimentalPathApi::class)
fun loadSecurityGoals(directory: String): List<SecurityGoal> {
    // Walk the directory and load all YAML files
    Path(directory).walk().filter { it.extension == "yaml" }.forEach {
        val goals = loadSecurityGoal(it)
    }

    return listOf()
}

/**
 * Load a single security goal from a file.
 */
fun loadSecurityGoal(file: Path): SecurityGoal {
    return Yaml.default.decodeFromString<SecurityGoal>(file.readText())
}
