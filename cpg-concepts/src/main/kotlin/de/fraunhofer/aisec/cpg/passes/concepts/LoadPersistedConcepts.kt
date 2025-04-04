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
package de.fraunhofer.aisec.cpg.passes.concepts

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.conceptBuildHelper
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.helpers.getNodesByRegion
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.net.URI
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This pass reads a yaml or JSON file and creates a [Concept] for each entry in the file. The pass
 * must be executed after the [DFGPass] and [ControlFlowSensitiveDFGPass] passes as it adds
 * [de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow] edges to the graph. Other [ConceptPass]
 * passes should be executed later to build upon the concepts created by this pass.
 */
@DependsOn(DFGPass::class, softDependency = false)
@DependsOn(ControlFlowSensitiveDFGPass::class, softDependency = true)
@ExecuteBefore(ConceptPass::class, softDependency = true)
class LoadPersistedConcepts(ctx: TranslationContext) : TranslationResultPass(ctx) {

    /**
     * The [PassConfiguration] enabling the user to configure the persisted concepts to be loaded by
     * setting [conceptSummaryFiles].
     *
     * @param conceptSummaryFiles A list of files containing the persisted concepts to be loaded.
     */
    class Configuration(var conceptSummaryFiles: List<File> = listOf()) : PassConfiguration()

    val logger: Logger = LoggerFactory.getLogger(LoadPersistedConcepts::class.java)

    override fun cleanup() {
        // nothing to do
    }

    override fun accept(translationResult: TranslationResult) {
        passConfig<Configuration>()?.conceptSummaryFiles?.forEach { file ->
            addEntriesFromFile(file, translationResult)
        }
    }

    /**
     * This function parses the provided [file] (containing the persisted [Concept]s) and adds the
     * entries to the [translationResult].
     *
     * @param file The file containing the persisted concepts.
     * @param translationResult The [TranslationResult] to which the [Concept]s will be added.
     */
    private fun addEntriesFromFile(file: File, translationResult: TranslationResult) {
        val entries =
            try {
                val mapper =
                    if (file.extension.lowercase() in listOf("yaml", "yml")) {
                            ObjectMapper(YAMLFactory())
                        } else {
                            ObjectMapper(JsonFactory())
                        }
                        .registerKotlinModule()
                mapper.readValue<PersistedConceptsEntry>(file)
            } catch (ex: Exception) {
                logger.error(
                    "Error reading persisted concepts from ${file.path}: ${ex.message}",
                    ex,
                )
                return
            }

        entries.concepts?.let { concepts ->
            concepts.forEach { concept ->
                if (concept.signature != null && concept.location != null) {
                    log.error(
                        "Both signature and location are set. Please use only one of them. The entire entry will be ignored!"
                    )
                    return@forEach
                } else if (concept.signature != null) {
                    getNodesBySignature(translationResult, concept.signature).forEach {
                        underlyingNode ->
                        addConcept(underlyingNode, concept.concept)
                    }
                } else if (concept.location != null) {
                    getNodesByLocation(translationResult, concept.location).forEach { underlyingNode
                        ->
                        addConcept(underlyingNode, concept.concept)
                    }
                } else {
                    log.error(
                        "Neither signature nor location are set. The entire entry will be ignored!"
                    )
                }
            }
        }
    }

    /**
     * This function retrieves the nodes matching the provided [signature] from the
     * [translationResult]. Currently, this function only matches on the node being a
     * [CallExpression] and the [CallExpression.reconstructedImportName] matching the provided FQN.
     *
     * @param translationResult The [TranslationResult] to search for nodes.
     * @param signature The [SignatureEntry] containing the signature to match.
     * @return A list of nodes matching the provided [SignatureEntry].
     */
    private fun getNodesBySignature(
        translationResult: TranslationResult,
        signature: SignatureEntry,
    ): List<Node> {
        return translationResult.getCallsByFQN(signature.fqn)
    }

    /**
     * This function retrieves the nodes matching the provided [location] from the
     * [translationResult].
     *
     * @param translationResult The [TranslationResult] to search for nodes.
     * @param location The [LocationEntry] containing the location to match.
     * @return A list of nodes matching the provided [LocationEntry] or an empty list of the
     *   [location] cannot be parsed.
     */
    private fun getNodesByLocation(
        translationResult: TranslationResult,
        location: LocationEntry,
    ): List<Node> {
        val loc =
            try {
                val regex =
                    Regex(
                        "(?<startLine>\\d+):(?<startColumn>\\d+)-(?<endLine>\\d+):(?<endColumn>\\d+)"
                    )
                val region =
                    regex.matchEntire(location.region)
                        ?: throw IllegalArgumentException(
                            "Invalid region format: \${location.region}"
                        )
                val startLine =
                    region.groups["startLine"]?.value?.toIntOrNull()
                        ?: throw IllegalArgumentException("Invalid or missing startLine in region.")
                val startColumn =
                    region.groups["startColumn"]?.value?.toIntOrNull()
                        ?: throw IllegalArgumentException(
                            "Invalid or missing startColumn in region."
                        )
                val endLine =
                    region.groups["endLine"]?.value?.toIntOrNull()
                        ?: throw IllegalArgumentException("Invalid or missing endLine in region.")
                val endColumn =
                    region.groups["endColumn"]?.value?.toIntOrNull()
                        ?: throw IllegalArgumentException("Invalid or missing endColumn in region.")

                PhysicalLocation(
                    uri = URI(location.file),
                    region = Region(startLine, startColumn, endLine, endColumn),
                )
            } catch (ex: Exception) {
                log.error("Failed to parse the location: ${location.region}: ${ex.message}", ex)
                return emptyList()
            }

        // find the matching node
        return translationResult.getNodesByRegion(location = loc, clsName = location.type).also {
            nodes ->
            if (nodes.size != 1) {
                logger.warn(
                    "Found ${nodes.size} nodes for location $loc. Expected exactly one node."
                )
            }
        }
    }

    /**
     * This is a helper function to add a [Concept] node to the [underlyingNode] as described in the
     * provided [ConceptEntry].
     *
     * @param underlyingNode The node to which the concept will be added.
     * @param concept The [ConceptEntry] containing the concept information.
     */
    private fun addConcept(underlyingNode: Node, concept: ConceptEntry) {
        logger.debug("Adding concept {} to node {}.", concept, underlyingNode)
        underlyingNode.conceptBuildHelper(
            name = concept.name,
            underlyingNode = underlyingNode,
            constructorArguments =
                concept.constructorArguments?.associate { arg -> arg.name to arg.value }
                    ?: emptyMap(),
            connectDFGUnderlyingNodeToConcept =
                concept.dfg?.fromThisNodeToConcept ?: false, // TODO: this `?: false` is not nice
            connectDFGConceptToUnderlyingNode =
                concept.dfg?.fromConceptToThisNode ?: false, // TODO: this `?: false` is not nice
        )
    }

    /** The root node of our YAML/JSON structure. It contains a list of [ConceptRootEntry]s. */
    private data class PersistedConceptsEntry(val concepts: List<ConceptRootEntry>?)

    /**
     * This class represents a single entry in the YAML/JSON file. It contains the concept and
     * optionally a location and/or a signature entry. The pass itself enforces that exactly one of
     * location or signature is set.
     *
     * @param concept The [ConceptEntry] containing the concept information.
     * @param location The [LocationEntry] containing the [PhysicalLocation] information to match
     *   against.
     * @param signature The [SignatureEntry] containing the signature of nodes to match.
     */
    private data class ConceptRootEntry(
        val concept: ConceptEntry,
        val location: LocationEntry?,
        val signature: SignatureEntry?,
    )

    /**
     * This class represents a single concept entry in the YAML/JSON file. It contains the name of
     * the concept, optional constructor arguments, and optional DFG connections.
     *
     * @param name The FQN of the concept to be added.
     * @param constructorArguments The constructor arguments to be passed to the concepts'
     *   constructor.
     * @param dfg The DFG connections to be created between the concept and the underlying node.
     */
    private data class ConceptEntry(
        val name: String,
        val constructorArguments: List<ConstructorArgumentEntry>?,
        val dfg: DFGEntry?,
    )

    /**
     * This class represents the DFG connections to be created between the concept and the
     * underlying node.
     *
     * @param fromThisNodeToConcept Whether to add a DFG connection from the underlying node to the
     *   concept node.
     * @param fromConceptToThisNode Whether to add a DFG connection from the concept node to the
     *   underlying node.
     */
    private data class DFGEntry(
        val fromThisNodeToConcept: Boolean?,
        val fromConceptToThisNode: Boolean?,
    )

    /**
     * This class represents a single constructor argument entry in the YAML/JSON file.
     *
     * @param name The name of the constructor argument.
     * @param value The value of the constructor argument.
     */
    private data class ConstructorArgumentEntry(val name: String, val value: String)

    /**
     * This class represents a single location entry in the YAML/JSON file.
     *
     * @param file The file this entry applies to. E.g. `file:/foo/bar/baz/concepts.yaml`.
     * @param region The region within the [file]. E.g. `1:1-2:2`.
     * @param type Optionally, the type of the node to match against. E.g.
     *   `de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression`.
     */
    private data class LocationEntry(val file: String, val region: String, val type: String?)

    /**
     * This class represents a single signature entry in the YAML/JSON file.
     *
     * @param fqn The fully qualified name of the [CallExpression] to match against. E.g.
     *   `foo.bar.baz`.
     */
    private data class SignatureEntry(
        val fqn: String
        // Extend this class with arguments / types as needed in the future
    )

    /**
     * This function retrieves the [Node.calls] from the [TranslationResult] by their fully
     * qualified name (FQN). The match is performed on the [CallExpression.reconstructedImportName].
     *
     * @param fqn The fully qualified name of the calls to retrieve.
     * @return A list of [CallExpression] nodes matching the provided FQN.
     */
    private fun TranslationResult.getCallsByFQN(fqn: String): List<Node> {
        return this.calls.filter { call -> call.reconstructedImportName.toString() == fqn }
    }
}
