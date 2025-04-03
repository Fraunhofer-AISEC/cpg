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
 * This parts reads a yaml file and creates a [Concept] for each entry in the yaml file.
 *
 * TODO: when should this be executed? @oxisto: "after DFG, but before other concept passes" -> this
 *   is currently an ugly workaround to somewhat achieve this
 */
@DependsOn(DFGPass::class, softDependency = false)
@DependsOn(ControlFlowSensitiveDFGPass::class, softDependency = true)
@ExecuteBefore(DynamicInvokeResolver::class, softDependency = true)
class ConceptSummaries(ctx: TranslationContext) : TranslationResultPass(ctx) {

    class Configuration(var conceptSummaryFiles: List<File> = listOf()) : PassConfiguration()

    val logger: Logger = LoggerFactory.getLogger(ConceptSummaries::class.java)
    lateinit var translationResult: TranslationResult

    /** TODO */
    private fun addEntriesFromFile(file: File) {
        val mapper =
            if (file.extension.lowercase() in listOf("yaml", "yml")) {
                    ObjectMapper(YAMLFactory())
                } else {
                    ObjectMapper(JsonFactory())
                }
                .registerKotlinModule()
        val entries = mapper.readValue<YAMLEntry>(file)

        entries.concepts?.let { concepts ->
            concepts.forEach { concept ->
                if (concept.signature != null) {
                    getNodesBySignature(concept.signature).forEach { underlyingNode ->
                        addConcept(underlyingNode, concept.concept)
                    }
                }
                if (concept.location != null) {
                    getNodesByLocation(concept.location).forEach { underlyingNode ->
                        addConcept(underlyingNode, concept.concept)
                    }
                }
            }
        }
    }

    override fun cleanup() {
        // nothing to do
    }

    override fun accept(p0: TranslationResult) {
        translationResult = p0

        passConfig<Configuration>()?.conceptSummaryFiles?.forEach { file ->
            addEntriesFromFile(file)
        }
    }

    private fun getNodesBySignature(signature: SignatureEntry): List<Node> {
        return translationResult.calls.filter { call ->
            call.reconstructedImportName.toString() == signature.fqn
        }
    }

    private fun getNodesByLocation(location: LocationEntry): List<Node> {
        val regex =
            Regex("(?<startLine>\\d+):(?<startColumn>\\d+)-(?<endLine>\\d+):(?<endColumn>\\d+)")
        val region = regex.matchEntire(location.region) ?: TODO()
        val startLine = region.groups["startLine"]?.value?.toIntOrNull() ?: TODO()
        val startColumn = region.groups["startColumn"]?.value?.toIntOrNull() ?: TODO()
        val endLine = region.groups["endLine"]?.value?.toIntOrNull() ?: TODO()
        val endColumn = region.groups["endColumn"]?.value?.toIntOrNull() ?: TODO()

        val loc =
            PhysicalLocation(
                uri = URI(location.file),
                region = Region(startLine, startColumn, endLine, endColumn),
            )
        // find the matching node
        return translationResult.getNodesByRegion(location = loc, clsName = location.type).also {
            nodes ->
            if (nodes.size != 1) {
                logger.warn(
                    "Found ${nodes.size} nodes for location $loc. Expected 1 node." // TODO
                )
            }
        }
    }

    private fun addConcept(underlyingNode: Node, concept: ConceptEntry) {
        logger.debug("Found node: {}", underlyingNode)
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

    private data class YAMLEntry(val concepts: List<ConceptRootEntry>?)

    private data class ConceptRootEntry(
        val location: LocationEntry?,
        val concept: ConceptEntry,
        val signature: SignatureEntry?,
    )

    private data class ConceptEntry(
        val name: String,
        val constructorArguments: List<ConstructorArgumentEntry>?,
        val dfg: DFGEntry?,
    )

    private data class DFGEntry(
        val fromThisNodeToConcept: Boolean?,
        val fromConceptToThisNode: Boolean?,
    )

    private data class ConstructorArgumentEntry(val name: String, val value: String)

    private data class LocationEntry(val file: String, val region: String, val type: String?)

    private data class SignatureEntry(
        val fqn: String
        // TODO: arguments, type, ...?
    )
}
