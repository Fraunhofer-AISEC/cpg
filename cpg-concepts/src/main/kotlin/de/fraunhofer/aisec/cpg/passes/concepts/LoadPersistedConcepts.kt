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
import de.fraunhofer.aisec.cpg.graph.byFQN
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.conceptBuildHelper
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.helpers.getNodesByRegion
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts.ConceptEntry
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts.PersistedConceptEntry
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File

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
     * setting [conceptFiles].
     *
     * @param conceptFiles A list of files containing the persisted concepts to be loaded.
     */
    class Configuration(val conceptFiles: List<File> = listOf()) : PassConfiguration()

    override fun cleanup() {
        // nothing to do
    }

    override fun accept(translationResult: TranslationResult) {
        passConfig<Configuration>()?.conceptFiles?.forEach { file ->
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
                mapper.readValue<PersistedConcepts>(file)
            } catch (ex: Exception) {
                log.error("Error reading persisted concepts from ${file.path}: ${ex.message}", ex)
                return
            }

        entries.concepts?.forEach { concept ->
            if (concept.signature != null && concept.location != null) {
                log.error(
                    "Both signature and location are set. Please use only one of them. The entire entry will be ignored!"
                )
                return@forEach
            } else if (concept.signature != null) {
                translationResult.getNodesBySignature(concept.signature).forEach { underlyingNode ->
                    addConcept(underlyingNode, concept.concept)
                }
            } else if (concept.location != null) {
                translationResult.getNodesByLocation(concept.location).forEach { underlyingNode ->
                    addConcept(underlyingNode, concept.concept)
                }
            } else {
                log.error(
                    "Neither signature nor location are set. The entire entry will be ignored!"
                )
            }
        }
    }

    /**
     * This function retrieves the nodes matching the provided [signature] from the [this].
     * Currently, this function only matches on the node being a [CallExpression] and the
     * [CallExpression.reconstructedImportName] matching the provided FQN.
     *
     * @param signature The [SignatureEntry] containing the signature to match.
     * @return A list of nodes matching the provided [SignatureEntry].
     */
    private fun TranslationResult.getNodesBySignature(signature: SignatureEntry): List<Node> {
        return this.calls.byFQN(signature.fqn)
    }

    /**
     * This function retrieves the nodes matching the provided [location] from the [this].
     *
     * @param location The [LocationEntry] containing the location to match.
     * @return A list of nodes matching the provided [LocationEntry] or an empty list of the
     *   [location] cannot be parsed.
     */
    private fun TranslationResult.getNodesByLocation(location: LocationEntry): List<Node> {
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
                    uri = File(location.file).toURI(),
                    region = Region(startLine, startColumn, endLine, endColumn),
                )
            } catch (ex: Exception) {
                log.error("Failed to parse the location: ${location.region}: ${ex.message}", ex)
                return emptyList()
            }

        // find the matching node
        return this.getNodesByRegion(location = loc, clsName = location.type).also { nodes ->
            if (nodes.size != 1) {
                log.warn("Found ${nodes.size} nodes for location $loc. Expected exactly one node.")
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
        log.debug("Adding concept {} to node {}.", concept, underlyingNode)
        underlyingNode.conceptBuildHelper(
            name = concept.name,
            underlyingNode = underlyingNode,
            constructorArguments =
                concept.constructorArguments.associate { arg -> arg.name to arg.value },
            connectDFGUnderlyingNodeToConcept = concept.dfg.fromThisNodeToConcept,
            connectDFGConceptToUnderlyingNode = concept.dfg.fromConceptToThisNode,
        )
    }

    /** The root node of our YAML/JSON structure. It contains a list of [PersistedConceptEntry]s. */
    data class PersistedConcepts(val concepts: List<PersistedConceptEntry>?)

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
    data class PersistedConceptEntry(
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
    data class ConceptEntry(
        val name: String,
        val constructorArguments: List<ConstructorArgumentEntry> = listOf(),
        val dfg: DFGEntry = DFGEntry(fromThisNodeToConcept = false, fromConceptToThisNode = false),
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
    data class DFGEntry(
        val fromThisNodeToConcept: Boolean = false,
        val fromConceptToThisNode: Boolean = false,
    )

    /**
     * This class represents a single constructor argument entry in the YAML/JSON file.
     *
     * @param name The name of the constructor argument.
     * @param value The value of the constructor argument.
     */
    data class ConstructorArgumentEntry(val name: String, val value: String)

    /**
     * This class represents a single location entry in the YAML/JSON file.
     *
     * @param file The file this entry applies to. E.g. `file:/foo/bar/baz/concepts.yaml`.
     * @param region The region within the [file]. E.g. `1:1-2:2`.
     * @param type Optionally, the type of the node to match against. E.g.
     *   `de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression`.
     */
    data class LocationEntry(val file: String, val region: String, val type: String?)

    /**
     * This class represents a single signature entry in the YAML/JSON file.
     *
     * @param fqn The fully qualified name of the [CallExpression] to match against. E.g.
     *   `foo.bar.baz`.
     */
    data class SignatureEntry(val fqn: String)
}

/**
 * Converts the [Concept] to a [PersistedConceptEntry]. This is used for exporting the concept
 * information.
 */
fun Concept.toPersistedConcept(): PersistedConceptEntry {
    return PersistedConceptEntry(
        concept =
            ConceptEntry(
                name = this.javaClass.name,
                dfg =
                    LoadPersistedConcepts.DFGEntry(
                        fromThisNodeToConcept =
                            this.underlyingNode?.nextDFG?.contains(this) == true,
                        fromConceptToThisNode = this.nextDFG.contains(this.underlyingNode),
                    ),
            ),
        location =
            LoadPersistedConcepts.LocationEntry(
                file = this.location?.artifactLocation?.uri.toString(),
                region = this.location?.region.toString(),
                type = this.underlyingNode?.javaClass?.name,
            ),
        signature = null,
    )
}
