/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.mcp.mcpserver.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.codeAndLocationFrom
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMConcept
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMOperation
import de.fraunhofer.aisec.cpg.graph.concepts.GenericProperties
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.*
import de.fraunhofer.aisec.cpg.persistence.pushToNeo4j
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.io.File

private const val fileName = "concepts.yaml"

// TODO move to other file.
/** TODO */
fun Server.persistGraphToNeo4jTool() {
    val toolDescription =
        """
        This tool persists the current CPG graph to a Neo4j database. It can be used to store the graph for further analysis and visualization in Neo4j. Usually, this tool should be used last, after all analyses and concept applications are done, to ensure that the persisted graph contains all the latest annotations and insights.
        
        Example prompts:
        - "Persist the current CPG graph to Neo4j"
        - "Store the enriched graph in Neo4j for later analysis"
        """
            .trimIndent()

    this.addTool(name = "cpg_persist_to_neo4j", description = toolDescription) { request ->
        request.runOnCpg { result: TranslationResult, _ ->
            result.pushToNeo4j()
            CallToolResult(
                content =
                    listOf(TextContent("Persisted the current CPG graph to Neo4j successfully."))
            )
        }
    }
}

/**
 * This function loads persisted concepts and operations from a storage and returns them as a list
 * of [LLMConceptDescription].
 */
internal fun loadPersistedConceptsAndOperations(): List<LLMConceptDescription> {
    // TODO load persisted concepts and operations from a storage (e.g. file, database) and populate
    // the server's internal state with them.
    val file = File(fileName)
    val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
    return mapper.readValue<List<LLMConceptDescription>>(file)
}

/**
 * This is a tool to list all currently known concepts and operations. It should be queried
 * initially to get an overview of the available concepts and operations.
 */
fun Server.getPersistedConceptsOperations() {
    val jsonMapper = ObjectMapper().registerKotlinModule()
    fun LLMConceptDescription.toJson(): String = jsonMapper.writeValueAsString(this)
    val toolDescription =
        """
        This tool lists all currently known concepts and operations. It should be queried initially to get an overview of the available concepts and operations.
        
        Example prompts:
        - "What concepts and operations are available?"
        - "List all known concepts and operations"
        """
            .trimIndent()
    this.addTool(name = "cpg_list_llm_concepts_operations", description = toolDescription) { _ ->
        CallToolResult(
            content = loadPersistedConceptsAndOperations().map { TextContent(it.toJson()) }
        )
    }
}

/**
 * This MCP tool allows to add or update a concept in the server's internal state. It can be used to
 * add new concepts or update existing ones based on the insights gained from analyzing the CPG
 * graph. Matching a concept is done by its name. If a concept with the same name already exists, it
 * will be overwritten with the new information. Otherwise, a new concept will be added.
 */
fun Server.addOrUpdateConcept() {
    val toolDescription =
        """
        This tool adds or updates a concept in the persisted LLM concepts store.
        Matching is done by concept name; if a name already exists, it will be replaced.

        Example prompts:
        - "Add a concept named Authentication"
        - "Update the concept named Encryption with new properties"
        """
            .trimIndent()

    this.addTool( // TODO: cannot use addTool<LLMConceptDescription> here, because it internally
        // uses runOnCpg
        name = "cpg_add_or_update_llm_concept",
        description = toolDescription,
        inputSchema = LLMConceptDescription::class.toSchema(),
    ) { request ->
        val payload =
            request.arguments?.toObject<LLMConceptDescription>()
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent("Invalid input for adding/updating concept."))
                )
        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        val file = File(fileName)
        val existing =
            if (file.exists() && file.length() > 0) {
                mapper.readValue<List<LLMConceptDescription>>(file)
            } else {
                emptyList()
            }

        val updated =
            if (existing.any { it.name == payload.name }) {
                existing.map { if (it.name == payload.name) payload else it }
            } else {
                existing + payload
            }

        mapper.writeValue(file, updated)
        CallToolResult(
            content = listOf(TextContent("Saved concept '${payload.name}' to ${file.path}."))
        )
    }
}

/**
 * This function adds a [GenericLLMConcept] and the corresponding [GenericLLMOperation]s to the CPG.
 */
fun Server.addLLMConceptAndOperations() {
    val toolDescription =
        """
        This tool applies a concept and all its operations to the graph. It creates and attaches a concept node and all operation nodes using their nodeId to specific nodes in the graph.
        Description of the concepts and their corresponding operations can be obtained from the "cpg_list_llm_concepts_operations" tool.
        """
            .trimIndent()
    this.addTool<LLMConcept>(
        name = "cpg_add_llm_concept_and_operations",
        description = toolDescription,
    ) { result: TranslationResult, payload: LLMConcept ->
        val applied = mutableListOf<String>()
        val failed = mutableListOf<String>()

        val cpgConceptNode = result.nodes.find { it.id.toString() == payload.nodeId }
        if (cpgConceptNode == null) {
            return@addTool CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "Node ${payload.nodeId} not found for concept ${payload.name}. Cannot add anything to the graph."
                        )
                    )
            )
        }

        // TODO: check concept/operation and properties actually exist
        val conceptNode =
            GenericLLMConcept(
                    underlyingNode = cpgConceptNode,
                    conceptName = payload.name,
                    properties =
                        GenericProperties(payload.properties.associate { it.name to it.value }),
                )
                .apply {
                    this.codeAndLocationFrom(cpgConceptNode)
                    this.name =
                        Name(
                            "${GenericLLMConcept::class.simpleName}[$conceptName]",
                            cpgConceptNode.name,
                        )
                    NodeBuilder.log(this)
                    applied.add("Concept node \"${payload.name}\" added to the graph.")
                }

        payload.operations.forEach { operation ->
            val cpgOperationNode = result.nodes.find { it.id.toString() == operation.nodeId }
            if (cpgOperationNode == null) {
                failed.add(
                    "Node \"${operation.nodeId}\" not found for operation \"${operation.name}\"."
                )
                return@forEach
            }
            GenericLLMOperation(
                    underlyingNode = cpgOperationNode,
                    operationName = operation.name,
                    genericLLMConcept = conceptNode,
                    properties =
                        GenericProperties(operation.properties.associate { it.name to it.value }),
                )
                .apply {
                    this.codeAndLocationFrom(cpgOperationNode)
                    this.name =
                        Name(
                            "${GenericLLMOperation::class.simpleName}[$operationName]",
                            cpgOperationNode.name,
                        )
                    NodeBuilder.log(this)
                    applied.add("Operation node \"${operation.name}\" added to the graph.")
                }
        }

        val summary =
            "Applied ${applied.size} nodes:\n" +
                applied.joinToString(
                    "\n" + "Failed to apply ${failed.size} nodes:\n" + failed.joinToString("\n")
                )

        CallToolResult(content = listOf(TextContent(summary)))
    }
}
