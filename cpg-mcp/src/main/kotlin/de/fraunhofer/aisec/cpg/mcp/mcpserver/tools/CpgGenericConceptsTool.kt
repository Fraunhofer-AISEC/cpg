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
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.io.File
import kotlinx.serialization.json.Json

private const val fileName = "concepts.yaml"

/**
 * This is a tool to list all currently known concepts and operations. It should be queried
 * initially to get an overview of the available concepts and operations.
 */
fun Server.listLLMConceptsOperations() {
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

    this.addTool(
        name = "cpg_add_or_update_llm_concept",
        description = toolDescription,
        inputSchema = LLMConceptDescription::class.toSchema(),
    ) { request ->
        val payload =
            request.arguments?.toObject<LLMConceptDescription>()
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent("Invalid input for adding/updating concept."))
                )
        persistConceptSchemas(listOf(payload))
        CallToolResult(
            content = listOf(TextContent("Saved concept '${payload.name}' to $fileName."))
        )
    }
}

fun Server.suggestLLMConceptsAndOperations() {
    val toolDescription =
        """
        Suggests concept and operations for a node in the CPG.
        The `nodeId` on the concept refers to the node the concept describes. Each operation's `nodeId` refers to the node where the operation is realized.
        All node IDs must come from prior tool results, so do not pass placeholder or invented IDs.
        """
            .trimIndent()

    this.addTool<LLMConcept>(
        name = "cpg_suggest_llm_concepts_and_operations",
        description = toolDescription,
    ) { result: TranslationResult, payload: LLMConcept ->
        val conceptNode = result.nodes.find { it.id.toString() == payload.nodeId }
        if (conceptNode == null) {
            return@addTool CallToolResult(
                content =
                    listOf(
                        TextContent("Node ${payload.nodeId} not found for concept ${payload.name}.")
                    )
            )
        }

        payload.operations.forEach { operation ->
            val opNode = result.nodes.find { it.id.toString() == operation.nodeId }
            if (opNode == null) {
                return@addTool CallToolResult(
                    content =
                        listOf(
                            TextContent(
                                "Node ${operation.nodeId} not found for operation ${operation.name}."
                            )
                        )
                )
            }
        }

        CallToolResult(content = listOf(TextContent(Json.encodeToString(payload))))
    }
}

/**
 * This function adds a [GenericLLMConcept] and the corresponding [GenericLLMOperation]s to the CPG.
 */
fun Server.addLLMConceptAndOperations() {
    val toolDescription =
        """
        This tool applies a concept and all its operations to the graph.
        It creates and attaches a concept node and all operation nodes using their nodeId to specific nodes in the graph.
        """
            .trimIndent()
    this.addTool<LLMConceptList>(
        name = "cpg_add_llm_concept_and_operations",
        description = toolDescription,
    ) { result: TranslationResult, payload: LLMConceptList ->
        val applied = mutableListOf<AppliedConcept>()
        val failed = mutableListOf<FailedConcept>()
        val schemasToPersist = mutableListOf<LLMConceptDescription>()

        payload.concepts.forEach { concept ->
            val cpgConceptNode = result.nodes.find { it.id.toString() == concept.nodeId }
            if (cpgConceptNode == null) {
                failed.add(
                    FailedConcept(
                        concept = concept,
                        reason =
                            "Underlying CPG node ${concept.nodeId} not found for concept \"${concept.name}\".",
                    )
                )
                return@forEach
            }

            val conceptNode =
                GenericLLMConcept(
                        underlyingNode = cpgConceptNode,
                        conceptName = concept.name,
                        description = concept.description,
                        properties =
                            GenericProperties(concept.properties.associate { it.name to it.value }),
                    )
                    .apply {
                        this.codeAndLocationFrom(cpgConceptNode)
                        this.name =
                            Name(
                                "${GenericLLMConcept::class.simpleName}[$conceptName]",
                                cpgConceptNode.name,
                            )
                        NodeBuilder.log(this)
                    }

            val appliedOps = mutableListOf<AppliedOperation>()
            val failedOps = mutableListOf<FailedOperation>()
            concept.operations.forEach { operation ->
                val cpgOperationNode = result.nodes.find { it.id.toString() == operation.nodeId }
                if (cpgOperationNode == null) {
                    failedOps.add(
                        FailedOperation(
                            operation = operation,
                            reason =
                                "Underlying CPG node ${operation.nodeId} not found for operation \"${operation.name}\".",
                        )
                    )
                    return@forEach
                }
                val opNode =
                    GenericLLMOperation(
                            underlyingNode = cpgOperationNode,
                            operationName = operation.name,
                            description = operation.description,
                            genericLLMConcept = conceptNode,
                            properties =
                                GenericProperties(
                                    operation.properties.associate { it.name to it.value }
                                ),
                        )
                        .apply {
                            this.codeAndLocationFrom(cpgOperationNode)
                            this.name =
                                Name(
                                    "${GenericLLMOperation::class.simpleName}[$operationName]",
                                    cpgOperationNode.name,
                                )
                            NodeBuilder.log(this)
                        }
                appliedOps.add(
                    AppliedOperation(operation = operation, overlayNodeId = opNode.id.toString())
                )
            }

            applied.add(
                AppliedConcept(
                    concept = concept,
                    overlayNodeId = conceptNode.id.toString(),
                    appliedOperations = appliedOps,
                    failedOperations = failedOps,
                )
            )
            schemasToPersist.add(LLMConceptDescription(concept))
        }

        if (schemasToPersist.isNotEmpty()) {
            persistConceptSchemas(schemasToPersist)
        }

        val response = AddConceptsResult(applied = applied, failed = failed)
        CallToolResult(content = listOf(TextContent(Json.encodeToString(response))))
    }
}

/**
 * This function loads persisted concepts and operations from a storage and returns them as a list
 * of [LLMConceptDescription].
 */
internal fun loadPersistedConceptsAndOperations(): List<LLMConceptDescription> {
    val file = File(fileName)
    if (!file.exists() || file.length() == 0L) return emptyList()
    val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
    return mapper.readValue<List<LLMConceptDescription>>(file)
}

/**
 * Merge the given concept schemas into the persisted YAML store. Concepts are matched by name; an
 * existing entry is replaced, otherwise appended.
 */
private fun persistConceptSchemas(schemas: List<LLMConceptDescription>) {
    val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
    val file = File(fileName)
    var updated = loadPersistedConceptsAndOperations()
    schemas.forEach { schema ->
        updated =
            if (updated.any { it.name == schema.name }) {
                updated.map { if (it.name == schema.name) schema else it }
            } else {
                updated + schema
            }
    }
    mapper.writeValue(file, updated)
}
