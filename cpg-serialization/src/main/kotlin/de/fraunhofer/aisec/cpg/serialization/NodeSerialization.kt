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
package de.fraunhofer.aisec.cpg.serialization

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.component
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.translationUnit
import kotlin.uuid.Uuid
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * Custom serializer for [Uuid] to convert it to and from a string representation. This is used for
 * serialization and deserialization of [Uuid] in the JSON data classes.
 */
object UuidSerializer : KSerializer<Uuid> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Uuid", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uuid) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uuid {
        return Uuid.parse(decoder.decodeString())
    }
}

/** JSON data class for an [Edge]. */
@Serializable
data class EdgeJSON(
    var label: String,
    @Serializable(with = UuidSerializer::class) var start: Uuid,
    @Serializable(with = UuidSerializer::class) var end: Uuid,
)

/** JSON data class for a [Node]. */
@Serializable
data class NodeJSON(
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    val type: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val code: String,
    val name: String,
    //        val astChildren: List<NodeJSON>,
    val prevDFG: List<EdgeJSON> = emptyList(),
    val nextDFG: List<EdgeJSON> = emptyList(),
    @Serializable(with = UuidSerializer::class) val translationUnitId: Uuid? = null,
    val componentName: String? = null,
    val fileName: String? = null,
)

/** Converts a [Node] into its JSON representation. */
fun Node.toJSON(noEdges: Boolean = false): NodeJSON {
    return NodeJSON(
        id = this.id,
        type = this.javaClass.simpleName,
        startLine = location?.region?.startLine ?: -1,
        startColumn = location?.region?.startColumn ?: -1,
        endLine = location?.region?.endLine ?: -1,
        endColumn = location?.region?.endColumn ?: -1,
        code = this.code ?: "",
        name = this.name.toString(),
        fileName =
            this.location?.artifactLocation?.uri?.let { uri ->
                // Extract filename from URI
                val path = uri.toString()
                path.substringAfterLast('/').substringAfterLast('\\')
            },
        //                astChildren =
        //                    if (noEdges) emptyList()
        //                    else (this as? AstNode)?.astChildren?.map { it.toJSON() } ?:
        // emptyList(),
        prevDFG = if (noEdges) emptyList() else this.prevDFGEdges.map { it.toJSON() },
        nextDFG = if (noEdges) emptyList() else this.nextDFGEdges.map { it.toJSON() },
        translationUnitId = this.translationUnit?.id,
        componentName = this.component?.name?.toString(),
    )
}

/** Converts an [Edge] into its JSON representation. */
fun Edge<*>.toJSON(): EdgeJSON {
    return EdgeJSON(
        label = this.labels.firstOrNull() ?: "",
        start = this.start.id,
        end = this.end.id,
    )
}
