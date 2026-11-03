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
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edges.flows.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.PartialDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.OperatorCall
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts.*
import de.fraunhofer.aisec.cpg.query.*
import de.fraunhofer.aisec.cpg.serialization.*
import io.github.detekt.sarif4k.ArtifactLocation
import io.github.detekt.sarif4k.Result
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.toPath
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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

/**
 * JSON data class for a [TranslationUnit].
 *
 * The [code] is only included when a single translation unit is requested (see
 * [ConsoleService.getTranslationUnit]), since it can be large and listings of translation units
 * (e.g., in [AnalysisResultJSON]) only need the metadata.
 */
@Serializable
data class TranslationUnitJSON(
    val name: String,
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    val path: String,
    val code: String? = null,
    @Transient val cpgTU: TranslationUnit? = null,
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
    val childrenWithAssumptionIds: Map<String, List<String>> =
        emptyMap(), // Map from assumption ID to list of child QueryTree IDs with that assumption
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
                            ConstructorArgumentEntry(
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

/** Request body for the `/api/execute-query` endpoint. */
@Serializable data class ExecuteQueryRequestJSON(val scriptCode: String)

/**
 * Upper bound for each list in [NodeDetailsJSON], so that hub nodes do not blow up the response.
 */
private const val MAX_RELATED_NODES = 200

/**
 * A node that is related to the node it was listed for, e.g. a neighbour in the dataflow.
 *
 * @property label Describes the relation, if it is not a plain one, e.g. the granularity and
 *   calling context of a dataflow.
 */
@Serializable data class RelatedNodeJSON(val node: NodeJSON, val label: String?)

/**
 * Everything the console knows about a single node that is relevant when inspecting it: its calls,
 * its direct dataflows, attached concepts and operations, and where the analysis is uncertain.
 *
 * The dataflows only contain the direct neighbours, so that the frontend can expand them lazily
 * into a tree.
 *
 * @property callTargets For a [Call]: the functions it invokes.
 * @property callers For a [Function]: the calls that invoke it.
 * @property callees For a [Function]: the calls in its body.
 * @property warnings Places where the analysis is incomplete or uncertain for this node.
 */
@Serializable
data class NodeDetailsJSON(
    val node: NodeJSON,
    val enclosingFunction: NodeJSON?,
    val overlays: List<OverlayJSON>,
    val callTargets: List<NodeJSON>,
    val callers: List<NodeJSON>,
    val callees: List<NodeJSON>,
    val dataflowFrom: List<RelatedNodeJSON>,
    val dataflowTo: List<RelatedNodeJSON>,
    val warnings: List<String>,
)

/** A function together with its key figures. */
@Serializable
data class FunctionCallStatsJSON(
    val function: NodeJSON,
    val callers: Int,
    val callees: Int,
    val externalCalls: Int,
    val unresolvedCalls: Int,
)

/**
 * A call and how well the analysis knows its target.
 *
 * @property callee The expression naming the target (e.g. `fopen` in `fopen(path, "r")`), if it has
 *   a location.
 */
@Serializable
data class CallSiteJSON(val call: NodeJSON, val callee: NodeJSON?, val status: CallStatus)

/**
 * A concept or operation and the node it is attached to.
 *
 * @property category The area of the overlay, derived from its package, e.g. `crypto` or `file`.
 * @property concept For an operation: the concept it belongs to. `null` for a concept.
 */
@Serializable
data class OverlayJSON(
    val overlay: NodeJSON,
    val category: String,
    val underlyingNode: NodeJSON?,
    val concept: NodeJSON?,
)

/** Converts a [AnalysisResult] into its JSON representation. */
fun AnalysisResult.toJSON(): AnalysisResultJSON =
    with(translationResult) {
        AnalysisResultJSON(
            components = components.map { it.toJSON() },
            totalNodes = nodes.size,
            analysisResult = this@toJSON,
            sourceDir = config.sourceLocations.firstOrNull()?.absolutePath ?: "",
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

/** Converts a [TranslationUnit] into its JSON representation. */
context(_: ContextProvider)
fun TranslationUnit.toJSON(): TranslationUnitJSON {
    val localName =
        component?.topLevel()?.let {
            this.location?.artifactLocation?.uri?.toPath()?.toFile()?.relativeToOrNull(it)
        }

    return TranslationUnitJSON(
        id = this.id,
        name = localName?.toString() ?: this.name.toString(),
        path = this.location?.artifactLocation?.uri.toString(),
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

/**
 * Groups all children with assumptions by their assumption IDs. Returns a map from assumption ID to
 * list of child QueryTree IDs that have that assumption.
 */
fun <T> QueryTree<T>.getChildrenGroupedByAssumptions(): Map<String, List<String>> {
    val result = mutableMapOf<String, MutableSet<String>>()

    // Get all children with assumptions
    val childrenWithAssumptions =
        this.mapAllChildren(filter = { it.relevantAssumptions().isNotEmpty() }) { it }

    // Group by assumption ID
    for (child in childrenWithAssumptions) {
        val childId = child.id.toString()
        for (assumption in child.relevantAssumptions()) {
            val assumptionId = assumption.id.toString()
            result.getOrPut(assumptionId) { mutableSetOf() }.add(childId)
        }
    }

    // Convert sets to lists for the return type
    return result.mapValues { it.value.toList() }
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
        childrenWithAssumptionIds = this.getChildrenGroupedByAssumptions(),
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
        assumptions = this.relevantAssumptions().mapTo(mutableSetOf()) { it.toJSON() },
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

fun Node.toDetailsJSON(): NodeDetailsJSON {
    val warnings = mutableListOf<String>()
    if (isInferred) {
        warnings += "This node was inferred by the analysis; it has no source code of its own."
    }
    if (this is ProblemNode) {
        warnings += "The analysis could not handle this code (${problemType}): $problem"
    }

    val callTargets = (this as? Call)?.invokes.orEmpty()
    if (this is Call && status == CallStatus.UNRESOLVED) {
        warnings += "The call target could not be resolved; dataflows through it are unknown."
    }
    callTargets
        .filter { it.isInferred }
        .forEach {
            warnings +=
                "The target '${it.name}' is not part of the analysed code (inferred); its behaviour is unknown."
        }
    if (this is Function && body == null) {
        warnings +=
            "This function is only declared, its implementation is not part of the analysis."
    }

    return NodeDetailsJSON(
        node = toRefJSON(),
        enclosingFunction = (this as? Function ?: firstParentOrNull<Function>())?.toRefJSON(),
        overlays = overlays.mapNotNull { it.toOverlayJSON() },
        callTargets = callTargets.limited { it.toRefJSON() },
        callers = (this as? Function)?.calledBy.orEmpty().limited { it.toRefJSON() },
        callees = (this as? Function)?.body?.calls.orEmpty().limited { it.toRefJSON() },
        dataflowFrom =
            prevDFGEdges.limited { RelatedNodeJSON(it.start.toRefJSON(), it.describe()) },
        dataflowTo = nextDFGEdges.limited { RelatedNodeJSON(it.end.toRefJSON(), it.describe()) },
        warnings = warnings,
    )
}

/** Describes the granularity and calling context of a dataflow, if it is not a plain one. */
private fun Dataflow.describe(): String? {
    val parts = mutableListOf<String>()
    (granularity as? PartialDataflowGranularity<*>)?.partialTarget?.let { target ->
        parts += "partial: ${(target as? Node)?.name ?: target}"
    }
    if (this is ContextSensitiveDataflow) {
        // The contexts in the order of the flow, e.g. out of one call and into the next one
        for (context in callingContexts) {
            when (context) {
                is CallingContextIn -> parts += "into call"
                is CallingContextOut -> parts += "out of call"
            }
        }
    }
    return parts.joinToString(", ").ifEmpty { null }
}

private fun <T, R> Collection<T>.limited(transform: (T) -> R): List<R> =
    asSequence().take(MAX_RELATED_NODES).map(transform).toList()

/** The functions of this translation unit that have a body, with their key figures. */
fun TranslationUnit.toFunctionCallStatsJSON(): List<FunctionCallStatsJSON> =
    functions
        .filter { !it.isImplicit && it.body != null && it.isInFileOf(this) }
        .map { function ->
            val statuses = function.body.calls.filter { it !is OperatorCall }.map { it.status }
            FunctionCallStatsJSON(
                function = function.toRefJSON(),
                callers = function.calledBy.size,
                callees = statuses.size,
                externalCalls = statuses.count { it == CallStatus.EXTERNAL },
                unresolvedCalls = statuses.count { it == CallStatus.UNRESOLVED },
            )
        }

/** The calls written in this translation unit, with the status of their targets. */
fun TranslationUnit.toCallSitesJSON(): List<CallSiteJSON> =
    nodes
        .filter { it.isInFileOf(this) }
        .explicitCalls()
        .map { call ->
            CallSiteJSON(
                call = call.toRefJSON(),
                callee = call.callee.takeIf { it.location != null }?.toRefJSON(),
                status = call.status,
            )
        }

/** The concepts and operations attached to the nodes of this translation unit. */
fun TranslationUnit.toOverlaysJSON(): List<OverlayJSON> =
    nodes
        .filter { it.isInFileOf(this) }
        .flatMap { node -> node.overlays.mapNotNull { it.toOverlayJSON() } }

/** This node as [OverlayJSON], or `null` if it is neither a [Concept] nor an [Operation]. */
fun Node.toOverlayJSON(): OverlayJSON? {
    if (this !is Concept && this !is Operation) return null
    return OverlayJSON(
        overlay = toRefJSON(),
        category =
            javaClass.packageName.substringAfter("concepts.", "").substringBefore('.').ifEmpty {
                "other"
            },
        underlyingNode = (this as OverlayNode).underlyingNode?.toRefJSON(),
        concept = (this as? Operation)?.concept?.toRefJSON(),
    )
}
