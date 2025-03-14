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
@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package de.fraunhofer.aisec.codyze.console

import de.fraunhofer.aisec.codyze.AnalysisResult
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.component
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import io.github.detekt.sarif4k.Result
import java.net.URL
import kotlin.io.path.Path
import kotlin.io.path.toPath
import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class EdgeJSON(
    var label: String,
    @Serializable(with = UuidSerializer::class) var start: Uuid,
    @Serializable(with = UuidSerializer::class) var end: Uuid,
)

@Serializable
data class FindingsJSON(
    var kind: String,
    var component: String,
    var path: String,
    var rule: String?,
    val startLine: Long,
    val startColumn: Long,
    val endLine: Long,
    val endColumn: Long,
)

@Serializable
data class AnalysisResultJSON(
    val components: List<ComponentJSON>,
    val totalNodes: Int,
    var sourceDir: String,
    @Transient val cpgResult: TranslationResult? = null,
    @Transient val analysisResult: AnalysisResult? = null,
    val findings: List<FindingsJSON>,
)

@Serializable
data class ComponentJSON(
    val name: String,
    val translationUnits: List<TranslationUnitJSON>,
    val topLevel: String?,
)

@Serializable
data class TranslationUnitJSON(
    val name: String,
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    val path: String,
    val code: String,
    @Transient val cpgTU: TranslationUnitDeclaration? = null,
)

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
    val astChildren: List<NodeJSON>,
    val prevDFG: List<EdgeJSON> = emptyList(),
    val nextDFG: List<EdgeJSON> = emptyList(),
)

context(ContextProvider)
fun TranslationUnitDeclaration.toJSON(): TranslationUnitJSON {
    val localName =
        component?.topLevel()?.let {
            this.location?.artifactLocation?.uri?.toPath()?.toFile()?.relativeToOrNull(it)
        }

    return TranslationUnitJSON(
        id = this.id,
        name = localName?.toString() ?: this.name.toString(),
        path = this.location?.artifactLocation?.uri.toString(),
        code = this.code ?: "",
        cpgTU = this,
    )
}

fun Node.toJSON(): NodeJSON {
    return NodeJSON(
        id = this.id,
        type = this.javaClass.simpleName,
        startLine = location?.region?.startLine ?: -1,
        startColumn = location?.region?.startColumn ?: -1,
        endLine = location?.region?.endLine ?: -1,
        endColumn = location?.region?.endColumn ?: -1,
        code = this.code ?: "",
        name = this.name.toString(),
        astChildren = this.astChildren.map { it.toJSON() },
        prevDFG = this.prevDFGEdges.map { it.toJSON() },
        nextDFG = this.nextDFGEdges.map { it.toJSON() },
    )
}

fun Edge<*>.toJSON(): EdgeJSON {
    return EdgeJSON(
        label = this.labels.firstOrNull() ?: "",
        start = this.start.id,
        end = this.end.id,
    )
}

context(ContextProvider)
fun Component.toJSON(): ComponentJSON {
    return ComponentJSON(
        name = this.name.toString(),
        translationUnits = this.translationUnits.map { tu -> tu.toJSON() },
        topLevel = this.topLevel()?.absolutePath,
    )
}

context(TranslationResult)
fun Result.toJSON(): FindingsJSON {
    var path =
        Path(URL(this.locations?.firstOrNull()?.physicalLocation?.artifactLocation?.uri).path)

    var component =
        this@TranslationResult.components.firstOrNull {
            it.translationUnits.any { tu -> tu.location?.artifactLocation?.uri?.toPath() == path }
        }

    return FindingsJSON(
        kind = this.kind?.name ?: "",
        path = path.toString(),
        component = component?.name.toString(),
        rule = this.ruleID,
        startLine = this.locations?.firstOrNull()?.physicalLocation?.region?.startLine ?: -1,
        startColumn = this.locations?.firstOrNull()?.physicalLocation?.region?.startColumn ?: -1,
        endLine = this.locations?.firstOrNull()?.physicalLocation?.region?.endLine ?: -1,
        endColumn = this.locations?.firstOrNull()?.physicalLocation?.region?.endColumn ?: -1,
    )
}

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
