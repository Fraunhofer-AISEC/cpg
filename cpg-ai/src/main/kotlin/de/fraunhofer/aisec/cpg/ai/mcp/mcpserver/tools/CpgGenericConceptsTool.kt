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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.*
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.codeAndLocationFrom
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMConcept
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMOperation
import de.fraunhofer.aisec.cpg.graph.concepts.GenericProperties
import de.fraunhofer.aisec.cpg.graph.concepts.GenericPropertyValue
import de.fraunhofer.aisec.cpg.graph.nodes
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
        The same applies to any individual property whose `type` is "$NODE_REFERENCE_TYPE": its `value` must be a real node id from prior tool results.
        Properties whose schema declares a `fixedValue` do not need to be supplied; if supplied, they will be overridden with the fixed value anyway.
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
        val persistedSchemas = loadPersistedConceptsAndOperations()

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

            val conceptSchema = persistedSchemas.find { it.name == concept.name }
            val (conceptProperties, conceptPropertyFailures) =
                resolveProperties(
                    applyFixedValues(concept.properties, conceptSchema?.properties.orEmpty()),
                    result,
                )
            if (conceptPropertyFailures.isNotEmpty()) {
                failed.add(
                    FailedConcept(
                        concept = concept,
                        reason = conceptPropertyFailures.joinToString(separator = "; "),
                    )
                )
                return@forEach
            }

            val conceptNode =
                GenericLLMConcept(
                        underlyingNode = cpgConceptNode,
                        conceptName = concept.name,
                        description = concept.description,
                        properties = conceptProperties,
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

                val operationSchema = conceptSchema?.operations?.find { it.name == operation.name }
                val (operationProperties, operationPropertyFailures) =
                    resolveProperties(
                        applyFixedValues(
                            operation.properties,
                            operationSchema?.properties.orEmpty(),
                        ),
                        result,
                    )
                if (operationPropertyFailures.isNotEmpty()) {
                    failedOps.add(
                        FailedOperation(
                            operation = operation,
                            reason = operationPropertyFailures.joinToString(separator = "; "),
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
                            properties = operationProperties,
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
            schemasToPersist.add(mergeFixedValues(conceptSchema, LLMConceptDescription(concept)))
        }

        if (schemasToPersist.isNotEmpty()) {
            persistConceptSchemas(schemasToPersist)
        }

        val response = AddConceptsResult(applied = applied, failed = failed)
        CallToolResult(content = listOf(TextContent(Json.encodeToString(response))))
    }
}

/**
 * Overrides the value of every property in [properties] for which [descriptions] declares a
 * [LLMPropertyDescription.fixedValue], and injects any fixed property from [descriptions] that is
 * missing from [properties] altogether. This ensures values that are intrinsic to a concept's/
 * operation's definition (e.g. a specific ID from a taxonomy) cannot drift between applications and
 * do not need to be supplied by the caller.
 */
private fun applyFixedValues(
    properties: List<LLMProperty>,
    descriptions: List<LLMPropertyDescription>,
): List<LLMProperty> {
    val descriptionsByName = descriptions.associateBy { it.name }
    val overridden =
        properties.map { property ->
            val fixedValue = descriptionsByName[property.name]?.fixedValue
            if (fixedValue != null) property.copy(value = fixedValue) else property
        }
    val missingFixed =
        descriptions
            .filter { it.fixedValue != null && overridden.none { p -> p.name == it.name } }
            .map { description ->
                LLMProperty(
                    name = description.name,
                    type = description.type,
                    description = description.description,
                    value = requireNotNull(description.fixedValue),
                )
            }
    return overridden + missingFixed
}

/**
 * Resolves a list of [LLMProperty] instances into a [GenericProperties] map. Properties whose
 * `type` is [NODE_REFERENCE_TYPE] are resolved against [result]'s nodes and stored as an actual
 * [GenericPropertyValue.NodeReferenceValue] instead of a stringified id; all other properties are
 * stored as [GenericPropertyValue.StringValue]. Returns the resolved properties together with a
 * list of failure reasons for any node reference that could not be resolved.
 */
private fun resolveProperties(
    properties: List<LLMProperty>,
    result: TranslationResult,
): Pair<GenericProperties, List<String>> {
    val failures = mutableListOf<String>()
    val resolved =
        properties.associate { property ->
            val value =
                if (property.type.equals(NODE_REFERENCE_TYPE, ignoreCase = true)) {
                    val referencedNode = result.nodes.find { it.id.toString() == property.value }
                    if (referencedNode == null) {
                        failures.add(
                            "Property \"${property.name}\" declares type $NODE_REFERENCE_TYPE but node ${property.value} was not found or ambiguous."
                        )
                        GenericPropertyValue.StringValue(property.value)
                    } else {
                        GenericPropertyValue.NodeReferenceValue(referencedNode)
                    }
                } else {
                    GenericPropertyValue.StringValue(property.value)
                }
            property.name to value
        }
    return GenericProperties(resolved) to failures
}

/**
 * Re-applying a concept re-derives its [LLMConceptDescription] from the applied instance via
 * [LLMConceptDescription]'s constructor, which does not know about
 * [LLMPropertyDescription.fixedValue] (that field only exists on the schema, not on an applied
 * [LLMProperty]). This merges any `fixedValue` already declared on [old] back into [derived] so
 * that persisting the schema after an apply never erases a previously declared fixed value.
 */
private fun mergeFixedValues(
    old: LLMConceptDescription?,
    derived: LLMConceptDescription,
): LLMConceptDescription {
    if (old == null) return derived
    fun mergeProperties(
        oldProperties: List<LLMPropertyDescription>,
        newProperties: List<LLMPropertyDescription>,
    ) =
        newProperties.map { property ->
            val fixedValue =
                property.fixedValue ?: oldProperties.find { it.name == property.name }?.fixedValue
            if (fixedValue != null) property.copy(fixedValue = fixedValue) else property
        }
    return derived.copy(
        properties = mergeProperties(old.properties, derived.properties),
        operations =
            derived.operations.map { operation ->
                val oldOperation = old.operations.find { it.name == operation.name }
                operation.copy(
                    properties =
                        mergeProperties(oldOperation?.properties.orEmpty(), operation.properties)
                )
            },
    )
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
