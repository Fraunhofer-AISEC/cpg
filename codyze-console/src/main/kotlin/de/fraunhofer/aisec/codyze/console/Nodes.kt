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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts.ConceptEntry
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts.DFGEntry
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts.LocationEntry
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts.PersistedConceptEntry
import io.github.detekt.sarif4k.ArtifactLocation
import io.github.detekt.sarif4k.Result
import java.net.URI
import java.nio.file.Path
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

/**
 * JSON data class for an analysis request. It contains the source directory, an optional include
 * directory, and an optional top-level directory.
 */
@Serializable
data class AnalyzeRequestJSON(
    val sourceDir: String,
    val includeDir: String? = null,
    val topLevel: String? = null,
    val conceptsFile: String? = null,
)

/** JSON data class for an [Edge]. */
@Serializable
data class EdgeJSON(
    var label: String,
    @Serializable(with = UuidSerializer::class) var start: Uuid,
    @Serializable(with = UuidSerializer::class) var end: Uuid,
)

/** JSON data class for a SARIF [Result]. */
@Serializable
data class FindingsJSON(
    var kind: String,
    var component: String?,
    @Serializable(with = UuidSerializer::class) var translationUnit: Uuid?,
    var path: String,
    var rule: String?,
    val startLine: Long,
    val startColumn: Long,
    val endLine: Long,
    val endColumn: Long,
)

/**
 * JSON data class for the [AnalysisResult]. It contains a list of components, the total number of
 * nodes, the source directory, and a list of findings.
 */
@Serializable
data class AnalysisResultJSON(
    val components: List<ComponentJSON>,
    val totalNodes: Int,
    var sourceDir: String,
    @Transient val analysisResult: AnalysisResult? = null,
    val findings: List<FindingsJSON>,
)

/**
 * JSON data class for a [Component]. It contains the name of the component, a list of translation
 * units, and an optional top-level directory.
 */
@Serializable
data class ComponentJSON(
    val name: String,
    val translationUnits: List<TranslationUnitJSON>,
    val topLevel: String?,
)

/** JSON data class for a [TranslationUnitDeclaration]. */
@Serializable
data class TranslationUnitJSON(
    val name: String,
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    val path: String,
    val code: String,
    @Transient val cpgTU: TranslationUnitDeclaration? = null,
)

/** JSON data class holding all relevant information required to instantiate a [Concept]. */
@Serializable
data class ConceptInfo(val conceptName: String, val constructorInfo: List<ConstructorInfo>)

@Serializable
data class ConstructorInfo(
    val argumentName: String,
    val argumentType: String,
    val isOptional: Boolean,
)

@Serializable
data class ConstructorArguments(
    val argumentName: String,
    val argumentValue: String,
    val argumentType: String? = null, // currently not used
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
    val astChildren: List<NodeJSON>,
    val prevDFG: List<EdgeJSON> = emptyList(),
    val nextDFG: List<EdgeJSON> = emptyList(),
)

/**
 * JSON data class for an "add new concept" request (see [ConsoleService.addConcept]).
 *
 * @param nodeId The UUID of the underlying node.
 * @param conceptName The (Java class) name of the concept.
 * @param addDFGToConcept Whether to add DFG edges from the underlying node to the new concept node.
 * @param addDFGFromConcept Whether to add DFG edges from the new concept node to the underlying
 *   node.
 */
@Serializable
data class ConceptRequestJSON(
    val nodeId: Uuid,
    val conceptName: String,
    val addDFGToConcept: Boolean,
    val addDFGFromConcept: Boolean,
    val constructorArgs: List<ConstructorArguments>? = null,
) {
    /**
     * Converts this JSON structure into a [PersistedConceptEntry] based on the instantiated
     * [concept].
     *
     * The information in the JSON structure is primarily used to create the
     * [PersistedConceptEntry.concept] (e.g., including the constructor arguments) and the
     * instantiation of the [concept] is primarily used to build the
     * [PersistedConceptEntry.location] entry.
     */
    fun buildPersistedConcept(concept: Concept): PersistedConceptEntry {
        return PersistedConceptEntry(
            concept =
                ConceptEntry(
                    name = this.conceptName,
                    dfg =
                        DFGEntry(
                            fromThisNodeToConcept = this.addDFGToConcept,
                            fromConceptToThisNode = this.addDFGFromConcept,
                        ),
                    constructorArguments =
                        this.constructorArgs?.map {
                            LoadPersistedConcepts.ConstructorArgumentEntry(
                                name = it.argumentName,
                                value = it.argumentValue,
                            )
                        } ?: listOf(),
                ),
            location =
                LocationEntry(
                    file = concept.location?.artifactLocation?.uri.toString(),
                    region = concept.location?.region.toString(),
                    type = concept.underlyingNode?.javaClass?.name,
                ),
            signature = null,
        )
    }
}

/** Converts a [AnalysisResult] into its JSON representation. */
fun AnalysisResult.toJSON(): AnalysisResultJSON =
    with(translationResult) {
        AnalysisResultJSON(
            components = components.map { it.toJSON() },
            totalNodes = nodes.size,
            analysisResult = this@toJSON,
            sourceDir = config.sourceLocations.first().absolutePath,
            findings = sarif.runs.flatMap { it.results?.map { it.toJSON() } ?: emptyList() },
        )
    }

/** Converts a [TranslationUnitDeclaration] into its JSON representation. */
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

/** Converts a [Component] into its JSON representation. */
context(ContextProvider)
fun Component.toJSON(): ComponentJSON {
    return ComponentJSON(
        name = this.name.toString(),
        translationUnits = this.translationUnits.map { tu -> tu.toJSON() },
        topLevel = this.topLevel()?.absolutePath,
    )
}

/** Converts a [Node] into its JSON representation. */
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

/** Converts an [Edge] into its JSON representation. */
fun Edge<*>.toJSON(): EdgeJSON {
    return EdgeJSON(
        label = this.labels.firstOrNull() ?: "",
        start = this.start.id,
        end = this.end.id,
    )
}

/**
 * Converts a SARIF [Result] into its JSON representation. It needs a [TranslationResult] as a
 * context receiver to find the component and translation unit of the finding.
 */
context(TranslationResult)
fun Result.toJSON(): FindingsJSON {
    var path = this.locations?.firstOrNull()?.physicalLocation?.artifactLocation?.absolutePath

    var translationUnit =
        this@TranslationResult.components
            .flatMap { it.translationUnits }
            .firstOrNull { tu -> tu.location?.artifactLocation?.uri?.toPath() == path }

    return FindingsJSON(
        kind = this.kind?.name ?: "",
        path = path.toString(),
        component = translationUnit?.component?.name?.toString(),
        rule = this.ruleID,
        startLine = this.locations?.firstOrNull()?.physicalLocation?.region?.startLine ?: -1,
        startColumn = this.locations?.firstOrNull()?.physicalLocation?.region?.startColumn ?: -1,
        endLine = this.locations?.firstOrNull()?.physicalLocation?.region?.endLine ?: -1,
        endColumn = this.locations?.firstOrNull()?.physicalLocation?.region?.endColumn ?: -1,
        translationUnit = translationUnit?.id,
    )
}

/**
 * Tries to convert the [ArtifactLocation.uri] (which can either be absolute or relative to a
 * [Component.topLevel]) into an absolute [Path].
 */
context(TranslationResult)
val ArtifactLocation.absolutePath: Path?
    get() {
        val uri = this.uri
        val uriBaseID = this.uriBaseID

        return when {
            // If the URI is null, return null
            uri == null -> {
                null
            }
            // If the URI is already absolute, return it as is
            uriBaseID == null -> {
                URI.create(uri).path?.let { Path(it) }
            }
            // Otherwise, try to find the URI base (which is the name of a component) and try to
            // build an absolute path again
            else -> {
                val componentPath =
                    this@TranslationResult.components[uriBaseID]?.topLevel()?.absoluteFile?.toPath()
                return componentPath?.resolve(uri)
            }
        }
    }

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
