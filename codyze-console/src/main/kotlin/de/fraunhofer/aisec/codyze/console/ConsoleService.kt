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
package de.fraunhofer.aisec.codyze.console

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import de.fraunhofer.aisec.codyze.AnalysisProject
import de.fraunhofer.aisec.codyze.AnalysisResult
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.conceptBuildHelper
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts.PersistedConceptEntry
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts.PersistedConcepts
import de.fraunhofer.aisec.cpg.passes.concepts.config.python.PythonStdLibConfigurationPass
import de.fraunhofer.aisec.cpg.query.QueryTree
import java.io.File
import java.nio.file.Path
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val AD_HOC_PROJECT_NAME = "ad-hoc"

/**
 * Service class for the console. This class is responsible for managing the translation process and
 * storing the results.
 *
 * @property analysisResult The result of the last translation process
 * @property lastProject The [AnalysisProject] of the last run.
 */
class ConsoleService {
    private var analysisResult: AnalysisResultJSON? = null
    var lastProject: AnalysisProject? = null

    private var newConceptNodes: Set<Concept> = emptySet()
    private var newPersistedConcepts = mutableListOf<PersistedConceptEntry>()

    // Cache for QueryTrees to support lazy loading
    private var queryTreeCache: Map<String, QueryTree<*>> = emptyMap()

    // Cache for parent relationships to support tree expansion
    private var queryTreeParentMap: Map<String, String> = emptyMap()

    /**
     * Analyzes the given source directory and returns the analysis result as [AnalysisResultJSON].
     */
    suspend fun analyze(request: AnalyzeRequestJSON): AnalysisResultJSON =
        withContext(Dispatchers.IO) {
            val path = Path.of(request.sourceDir)
            val builder =
                TranslationConfiguration.builder()
                    .sourceLocations(path.toFile())
                    .defaultPasses()
                    .loadIncludes(true)
                    .registerPass<PythonStdLibConfigurationPass>()
                    .registerPass<LoadPersistedConcepts>()
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage")
                    .optionalLanguage(
                        "de.fraunhofer.aisec.cpg.frontends.typescript.TypeScriptLanguage"
                    )
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.ruby.RubyLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.jvm.JVMLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.ini.IniFileLanguage")
                    .codeInNodes(true)

            if (request.includeDir != null) {
                builder.includePath(request.includeDir)
            }

            if (request.topLevel != null) {
                builder.topLevel(File(request.topLevel))
            }

            if (request.conceptsFile != null) {
                builder.configurePass<LoadPersistedConcepts>(
                    LoadPersistedConcepts.Configuration(
                        conceptFiles = listOf(File(request.conceptsFile))
                    )
                )
            }

            val config = builder.build()

            // Build an ad-hoc project
            val project =
                AnalysisProject(name = AD_HOC_PROJECT_NAME, projectDir = null, config = config)
            analyzeProject(project)
        }

    /** Analyzes the given project and returns the analysis result as [AnalysisResultJSON]. */
    fun analyzeProject(project: AnalysisProject): AnalysisResultJSON {
        lastProject = project

        val result = project.analyze()

        // Populate QueryTree cache for lazy loading
        populateQueryTreeCache(result.requirementsResults)

        val json = result.toJSON()
        this@ConsoleService.analysisResult = json
        return json
    }

    /**
     * Populates the QueryTree cache with all QueryTrees from the analysis results. This enables
     * lazy loading of QueryTree children and builds parent relationships for tree expansion.
     */
    private fun populateQueryTreeCache(requirementsResults: Map<String, QueryTree<Boolean>>?) {
        if (requirementsResults == null) {
            queryTreeCache = emptyMap()
            queryTreeParentMap = emptyMap()
            return
        }

        val cache = mutableMapOf<String, QueryTree<*>>()
        val parentMap = mutableMapOf<String, String>()

        // Recursively collect all QueryTrees and build parent relationships
        fun collectQueryTrees(queryTree: QueryTree<*>) {
            cache[queryTree.id.toString()] = queryTree
            queryTree.children.forEach { child ->
                parentMap[child.id.toString()] = queryTree.id.toString()
                collectQueryTrees(child)
            }
        }

        requirementsResults.values.forEach { collectQueryTrees(it) }
        queryTreeCache = cache
        queryTreeParentMap = parentMap
    }

    /** Returns the translation result of the last analysis as [AnalysisResultJSON]. */
    fun getTranslationResult(): AnalysisResultJSON? {
        return analysisResult
    }

    /** Returns the component with the given name as [ComponentJSON]. */
    fun getComponent(componentName: String): ComponentJSON? {
        return analysisResult?.components?.find { it.name == componentName }
    }

    /**
     * Returns the translation unit with the given ID for the specified component as
     * [TranslationUnitJSON].
     */
    fun getTranslationUnit(componentName: String, id: String): TranslationUnitJSON? {
        return getComponent(componentName)?.translationUnits?.find { it.id == Uuid.parse(id) }
    }

    /**
     * Returns the nodes for the given translation unit ID for the specified component as a list of
     * [NodeJSON].
     */
    fun getNodesForTranslationUnit(
        componentName: String,
        id: String,
        overlayNodes: Boolean,
    ): List<NodeJSON> {
        return getComponent(componentName)
            ?.translationUnits
            ?.find { it.id == Uuid.parse(id) }
            ?.cpgTU
            ?.let { extractNodes(it, overlayNodes) } ?: emptyList()
    }

    /** Returns the requirement with the given ID as [RequirementJSON]. */
    fun getRequirement(requirementId: String): RequirementJSON? {
        return analysisResult
            ?.requirementCategories
            ?.flatMap { it.requirements }
            ?.find { it.id == requirementId }
    }

    /** Returns the QueryTree with the given ID as [QueryTreeJSON] for lazy loading. */
    fun getQueryTree(queryTreeId: String): QueryTreeJSON? {
        return queryTreeCache[queryTreeId]?.toJSON()
    }

    /** Returns multiple QueryTrees by their IDs as a list of [QueryTreeJSON] for lazy loading. */
    fun getQueryTrees(queryTreeIds: List<String>): List<QueryTreeJSON> {
        val results =
            queryTreeIds.mapNotNull { id ->
                val result = queryTreeCache[id]?.toJSON()
                result
            }

        return results
    }

    /** Returns a QueryTree with all its parent IDs for tree expansion. */
    fun getQueryTreeWithParents(queryTreeId: String): QueryTreeWithParentsJSON? {
        val queryTree = queryTreeCache[queryTreeId]?.toJSON() ?: return null

        // Build the list of parent IDs by following the parent chain
        val parentIds = mutableListOf<String>()
        var currentId: String? = queryTreeId

        while (currentId != null) {
            val parentId = queryTreeParentMap[currentId]
            if (parentId != null) {
                parentIds.add(parentId)
                currentId = parentId
            } else {
                currentId = null
            }
        }

        return QueryTreeWithParentsJSON(queryTree = queryTree, parentIds = parentIds)
    }

    /**
     * Adds a new [Concept] node as an [de.fraunhofer.aisec.cpg.graph.OverlayNode] to an existing
     * node in the analysis result. The DFG edges can be configured to connect the new concept node
     * to the existing node.
     *
     * @param request The request containing node ID, concept name and configuration parameters
     *   (connect DFG)
     * @throws IllegalStateException if no analysis result exists
     * @throws IllegalArgumentException if the target node is not found or the concept name is
     *   invalid
     */
    fun addConcept(request: ConceptRequestJSON) {
        val analysisResult =
            this.analysisResult ?: throw IllegalStateException("No analysis result found.")

        val node =
            analysisResult.components
                .flatMap { it.translationUnits }
                .flatMap { it.cpgTU.nodes }
                .singleOrNull { it.id == request.nodeId }
                ?: throw IllegalArgumentException("Unique target node not found.")

        val concept =
            node
                .conceptBuildHelper(
                    name = request.conceptName,
                    underlyingNode = node,
                    constructorArguments =
                        request.constructorArgs?.associate { it.argumentName to it.argumentValue }
                            ?: emptyMap(),
                    connectDFGUnderlyingNodeToConcept = request.addDFGToConcept,
                    connectDFGConceptToUnderlyingNode = request.addDFGFromConcept,
                )
                .also { newConceptNodes += it }

        // Build the new persisted concept entry and store it, so we can export it later
        newPersistedConcepts += request.buildPersistedConcept(concept)
    }

    /**
     * Exports all new [Concept] nodes (added via [addConcept] and thus stored in [newConceptNodes])
     * as a YAML string.
     */
    fun exportPersistedConcepts(): String {
        val concepts = PersistedConcepts(concepts = newPersistedConcepts)
        return ObjectMapper(YAMLFactory())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_NULL_MAP_VALUES)
            .writeValueAsString(concepts)
    }

    /**
     * Extracts the nodes from the given translation unit. If [overlayNodes] is true, it extracts
     * the overlay nodes, otherwise it extracts the AST nodes.
     */
    private fun extractNodes(
        tu: TranslationUnitDeclaration,
        overlayNodes: Boolean,
    ): List<NodeJSON> {
        return if (overlayNodes) {
            tu.nodes.flatMap { it.overlays }.map { it.toJSON() }
        } else {
            tu.declarations.map { it.toJSON() } + tu.statements.map { it.toJSON() }
        }
    }

    companion object {
        /** Creates a new [ConsoleService] instance from the given [AnalysisResult]. */
        fun fromAnalysisResult(result: AnalysisResult): ConsoleService {
            val service = ConsoleService()
            service.analysisResult = result.toJSON()
            service.lastProject = result.project
            // Populate QueryTree cache for lazy loading
            service.populateQueryTreeCache(result.requirementsResults)

            return service
        }
    }
}
