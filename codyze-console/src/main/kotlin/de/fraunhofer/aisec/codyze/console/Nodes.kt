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

import de.fraunhofer.aisec.codyze.AnalysisProject
import de.fraunhofer.aisec.codyze.AnalysisResult
import de.fraunhofer.aisec.codyze.dsl.RequirementBuilder
import de.fraunhofer.aisec.codyze.dsl.RequirementCategoryBuilder
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.assumptions.Assumption
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts.*
import de.fraunhofer.aisec.cpg.query.*
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
    val requirementCategories: List<RequirementsCategoryJSON> = emptyList(),
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
    @Serializable(with = UuidSerializer::class) val translationUnitId: Uuid? = null,
    val componentName: String? = null,
    val fileName: String? = null,
)

/** JSON data class for a requirement category. */
@Serializable
data class RequirementsCategoryJSON(
    val id: String,
    val name: String,
    val description: String,
    val requirements: List<RequirementJSON>,
)

/**
 * JSON data class for project information separate from analysis results. This provides metadata
 * about the analysis project.
 */
@Serializable
data class AnalysisProjectJSON(
    val name: String,
    val sourceDir: String,
    val includeDir: String? = null,
    val topLevel: String? = null,
    val projectCreatedAt: String,
    val lastAnalyzedAt: String? = null,
    val requirementCategories: List<RequirementsCategoryJSON> = emptyList(),
    @Transient val project: AnalysisProject? = null,
)

/** JSON data class for a single requirement. */
@Serializable
data class RequirementJSON(
    val id: String,
    val name: String,
    val description: String,
    val status: String,
    val categoryId: String,
    val queryTree: QueryTreeJSON? = null,
)

/** JSON data class for caller information from QueryTree. */
@Serializable
data class CallerInfoJSON(
    val className: String,
    val methodName: String,
    val fileName: String,
    val lineNumber: Int,
)

/** JSON data class for an assumption. */
@Serializable
data class AssumptionJSON(
    val id: String,
    val assumptionType: String, // AssumptionType as string
    val message: String,
    val status: String, // AssumptionStatus as string
    val nodeId: String? = null, // UUID of associated node, if any
    val node: NodeJSON? = null, // Full node information when available
    val edgeLabel: String? = null, // Label of associated edge, if any
    val assumptionScopeId: String? = null, // UUID of assumption scope node, if any
)

/** JSON data class for a QueryTree result with lazy loading support. */
@Serializable
data class QueryTreeJSON(
    val id: String, // Unique identifier for this QueryTree
    val value: String? = null, // Serialized as string to handle simple types
    val nodeValues: List<NodeJSON>? = null, // List of nodes when value is List<Node>
    val confidence: String, // AcceptanceStatus as string
    val stringRepresentation: String,
    val operator: String,
    val queryTreeType:
        String, // Type of QueryTree (QueryTree, BinaryOperationResult, UnaryOperationResult)
    val childrenIds: List<String> = emptyList(), // IDs of child QueryTrees for lazy loading
    val childrenWithAssumptionIds: List<String> =
        emptyList(), // IDs of child QueryTrees with assumptions
    val hasChildren: Boolean = false, // Quick check for UI expansion
    val nodeId: String? = null, // UUID of associated node, if any
    val node: NodeJSON? = null, // Full node information, if any
    val callerInfo: CallerInfoJSON? = null, // Information about where the query was called from
    val assumptions: Set<AssumptionJSON> = emptySet(), // List of assumptions for this QueryTree
)

/** JSON data class for a QueryTree with its parent IDs for tree expansion. */
@Serializable
data class QueryTreeWithParentsJSON(
    val queryTree: QueryTreeJSON,
    val parentIds: List<String> = emptyList(), // IDs of all parent QueryTrees
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
            requirementCategories =
                project.requirementCategoriesToJSON(this@toJSON.requirementsResults),
        )
    }

/** Converts a [AnalysisProject] into its JSON representation. */
fun AnalysisProject.toJSON(): AnalysisProjectJSON {
    return AnalysisProjectJSON(
        name = this.name,
        sourceDir = this.projectDir?.toString() ?: "",
        includeDir = this.config.includePaths.firstOrNull()?.toString(),
        topLevel = this.projectDir?.toString() ?: "",
        projectCreatedAt = java.time.Instant.now().toString(),
        lastAnalyzedAt = null,
        requirementCategories = this.requirementCategoriesToJSON(),
        project = this,
    )
}

/** Converts a [TranslationUnitDeclaration] into its JSON representation. */
context(_: ContextProvider)
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
context(_: ContextProvider)
fun Component.toJSON(): ComponentJSON {
    return ComponentJSON(
        name = this.name.toString(),
        translationUnits = this.translationUnits.map { tu -> tu.toJSON() },
        topLevel = this.topLevel()?.absolutePath,
    )
}

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
        astChildren = if (noEdges) emptyList() else this.astChildren.map { it.toJSON() },
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

/** Converts an [Assumption] into its JSON representation. */
fun Assumption.toJSON(): AssumptionJSON {
    return AssumptionJSON(
        id = this.id.toString(),
        assumptionType = this.assumptionType.name,
        message = this.message,
        status = this.status.name,
        nodeId = this.underlyingNode?.id?.toString(),
        node = this.underlyingNode?.toJSON(),
        edgeLabel = this.edge?.labels?.firstOrNull(),
        assumptionScopeId = this.assumptionScope?.id?.toString(),
    )
}

/**
 * Converts a SARIF [Result] into its JSON representation. It needs a [TranslationResult] as a
 * context receiver to find the component and translation unit of the finding.
 */
context(result: TranslationResult)
fun Result.toJSON(): FindingsJSON {
    val path = this.locations?.firstOrNull()?.physicalLocation?.artifactLocation?.absolutePath

    val translationUnit =
        result.components
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
context(result: TranslationResult)
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
                val componentPath = result.components[uriBaseID]?.topLevel()?.absoluteFile?.toPath()
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

/** Converts the requirement categories of an [AnalysisProject] into their JSON representation. */
fun AnalysisProject.requirementCategoriesToJSON(
    requirementsResults: Map<String, QueryTree<Boolean>>? = null
): List<RequirementsCategoryJSON> {
    return this.requirementCategories.map { (_, categoryBuilder) ->
        categoryBuilder.toJSON(requirementsResults)
    }
}

/** Converts a [RequirementCategoryBuilder] into its JSON representation. */
fun RequirementCategoryBuilder.toJSON(
    requirementsResults: Map<String, QueryTree<Boolean>>? = null
): RequirementsCategoryJSON {
    return RequirementsCategoryJSON(
        id = this.id,
        name = this.name ?: this.id,
        description = this.description ?: "",
        requirements =
            this.requirements.map { (_, reqBuilder) ->
                reqBuilder.toJSON(this.id, requirementsResults)
            },
    )
}

/** Converts a [QueryTree] into its JSON representation with lazy loading support. */
fun <T> QueryTree<T>.toJSON(): QueryTreeJSON {
    // Determine the QueryTree type based on the class
    val queryTreeType =
        when (this) {
            is BinaryOperationResult<*, *> -> "BinaryOperationResult"
            is UnaryOperationResult<*> -> "UnaryOperationResult"
            is SinglePathResult -> "SinglePathResult"
            else -> "QueryTree"
        }

    // Handle different value types
    val (stringValue, nodeValues) =
        when (val value = this.value) {
            is List<*> -> {
                // Check if it's a list of nodes
                if (value.isNotEmpty() && value.first() is Node) {
                    @Suppress("UNCHECKED_CAST") val nodes = value as List<Node>
                    null to nodes.map { it.toJSON(noEdges = true) }
                } else {
                    value.toString() to null
                }
            }
            else -> value.toString() to null
        }

    return QueryTreeJSON(
        id = this.id.toString(),
        value = stringValue,
        nodeValues = nodeValues,
        confidence = this.confidence.toString(),
        stringRepresentation = this.stringRepresentation,
        operator = this.operator.toString(),
        queryTreeType = queryTreeType,
        childrenIds = this.children.map { it.id.toString() },
        childrenWithAssumptionIds =
            this.mapAllChildren(filter = { it.relevantAssumptions().isNotEmpty() }) {
                it.id.toString()
            },
        hasChildren = this.children.isNotEmpty(),
        nodeId = this.node?.id?.toString(),
        node = this.node?.toJSON(noEdges = true),
        callerInfo =
            this.callerInfo?.let {
                CallerInfoJSON(
                    className = it.className,
                    methodName = it.methodName,
                    fileName = it.fileName,
                    lineNumber = it.lineNumber,
                )
            },
        assumptions = this.relevantAssumptions().map { it.toJSON() }.toSet(),
    )
}

/**
 * Converts a [RequirementBuilder] into its JSON representation.
 *
 * @param categoryId The ID of the parent category, required for the JSON structure.
 * @param requirementsResults Map containing the evaluation results for requirements, keyed by
 *   requirement ID
 */
fun RequirementBuilder.toJSON(
    categoryId: String,
    requirementsResults: Map<String, QueryTree<Boolean>>? = null,
): RequirementJSON {
    val queryTree = requirementsResults?.get(this.id)
    val status =
        when {
            requirementsResults == null -> "NOT_YET_EVALUATED"
            else -> {
                when {
                    queryTree == null || queryTree is NotYetEvaluated -> "NOT_YET_EVALUATED"
                    queryTree.confidence is RejectedResult -> "REJECTED"
                    queryTree.confidence is UndecidedResult -> "UNDECIDED"
                    queryTree.value && queryTree.confidence is AcceptedResult -> "FULFILLED"
                    !queryTree.value && queryTree.confidence is AcceptedResult -> "NOT_FULFILLED"
                    else -> "UNDECIDED"
                }
            }
        }

    return RequirementJSON(
        id = this.id,
        name = this.name ?: this.id,
        description = this.description ?: "",
        status = status,
        categoryId = categoryId,
        queryTree = queryTree?.toJSON(),
    )
}
