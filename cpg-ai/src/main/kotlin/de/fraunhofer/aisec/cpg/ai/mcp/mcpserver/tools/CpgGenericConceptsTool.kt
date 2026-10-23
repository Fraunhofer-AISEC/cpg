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
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.*
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.codeAndLocationFrom
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMConcept
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMOperation
import de.fraunhofer.aisec.cpg.graph.concepts.GenericProperties
import de.fraunhofer.aisec.cpg.graph.concepts.GenericPropertyValue
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json

private const val fileName = "concepts.yaml"

/**
 * This is a tool to list all currently known concepts and operations. It should be queried
 * initially to get an overview of the available concepts and operations.
 */
fun Server.listLLMConceptsOperations(file: File = File(fileName)) {
    val toolDescription =
        """
        This tool lists all currently known concepts and operations. It should be queried initially to get an overview of the available concepts and operations.
        Results are capped at $DEFAULT_LIST_LIMIT concepts by default; use the limit/offset parameters to paginate through more.

        Example prompts:
        - "What concepts and operations are available?"
        - "List all known concepts and operations"
        """
            .trimIndent()
    this.addToolWithoutCpg<CpgListPayload>(
        name = "cpg_list_llm_concepts_operations",
        readOnly = true,
        description = toolDescription,
    ) { payload ->
        listConcepts(file, payload)
    }
}

/** One JSON entry per persisted concept schema in [file], paginated according to [payload]. */
internal fun listConcepts(file: File, payload: CpgListPayload): CallToolResult {
    val jsonMapper = ObjectMapper().registerKotlinModule()
    val schemas = synchronized(conceptsFileLock) { loadPersistedConceptsAndOperations(file) }
    return schemas.toPagedResult(payload.limit, payload.offset) {
        jsonMapper.writeValueAsString(it)
    }
}

/**
 * This MCP tool allows to add or update a concept in the server's internal state. It can be used to
 * add new concepts or update existing ones based on the insights gained from analyzing the CPG
 * graph. Matching a concept is done by its name. If a concept with the same name already exists, it
 * will be overwritten with the new information. Otherwise, a new concept will be added.
 */
fun Server.addOrUpdateConcept(file: File = File(fileName)) {
    val toolDescription =
        """
        This tool adds or updates a concept in the persisted LLM concepts store.
        Matching is done by concept name; if a name already exists, it will be replaced.

        Example prompts:
        - "Add a concept named Authentication"
        - "Update the concept named Encryption with new properties"
        """
            .trimIndent()

    this.addToolWithoutCpg<LLMConceptDescription>(
        name = "cpg_add_or_update_llm_concept",
        readOnly = false,
        description = toolDescription,
    ) { payload ->
        persistConceptSchemas(listOf(payload), file)
        CallToolResult(
            content = listOf(TextContent("Saved concept '${payload.name}' to ${file.path}."))
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

    this.addTool<LLMConceptSuggestion>(
        readOnly = true,
        name = "cpg_suggest_llm_concepts_and_operations",
        description = toolDescription,
    ) { session: CpgSession, payload: LLMConceptSuggestion ->
        val concept = payload.concept
        val conceptNode = session.findNodeById(concept.nodeId)
        if (conceptNode == null) {
            return@addTool CallToolResult(
                content =
                    listOf(
                        TextContent("Node ${concept.nodeId} not found for concept ${concept.name}.")
                    )
            )
        }

        concept.operations.forEach { operation ->
            val opNode = session.findNodeById(operation.nodeId)
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

        // Return the bare concept, which is what the console renders as a suggestion
        CallToolResult(content = listOf(TextContent(Json.encodeToString(concept))))
    }
}

/**
 * This function adds a [GenericLLMConcept] and the corresponding [GenericLLMOperation]s to the CPG.
 */
fun Server.addLLMConceptAndOperations(file: File = File(fileName)) {
    val toolDescription =
        """
        This tool applies a concept and all its operations to the graph.
        It creates and attaches a concept node and all operation nodes using their nodeId to specific nodes in the graph.
        Applying also records operations and properties the stored concept doesn't have yet, keeping
        everything it already defines - no need to redefine a concept via
        cpg_add_or_update_llm_concept just to add an operation.
        """
            .trimIndent()
    this.addTool<LLMConceptList>(
        name = "cpg_add_llm_concept_and_operations",
        description = toolDescription,
        readOnly = false,
    ) { session: CpgSession, payload: LLMConceptList ->
        val response = applyLLMConcepts(session, payload, file)
        CallToolResult(content = listOf(TextContent(Json.encodeToString(response))))
    }
}

/**
 * Applies every concept in [payload] (and its operations) to the graph of [session], resolving
 * property schemas (fixed values, description backfill) from [file] and persisting the applied
 * concepts' schemas back to it. Attaches nodes to the shared graph, so callers must hold
 * [CpgLock.write] - the tool registration does.
 */
internal fun applyLLMConcepts(
    session: CpgSession,
    payload: LLMConceptList,
    file: File,
): AddConceptsResult {
    val applied = mutableListOf<AppliedConcept>()
    val failed = mutableListOf<FailedConcept>()
    val schemasToPersist = mutableListOf<LLMConceptDescription>()

    val persistedSchemas =
        synchronized(conceptsFileLock) { loadPersistedConceptsAndOperations(file) }

    payload.concepts.forEach { concept ->
        val cpgConceptNode = session.findNodeById(concept.nodeId)
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
                session,
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
                    notes = concept.notes,
                )
                .apply {
                    this.codeAndLocationFrom(cpgConceptNode)
                    this.name =
                        Name(
                            "${GenericLLMConcept::class.simpleName}[$conceptName]",
                            cpgConceptNode.name,
                        )
                    // Participate in the dataflow graph, like any other concept/operation
                    // attached via the non-generic (fixed-catalog) concept builders.
                    this.setDFG()
                    NodeBuilder.log(this)
                }

        val appliedOps = mutableListOf<AppliedOperation>()
        val failedOps = mutableListOf<FailedOperation>()
        concept.operations.forEach { operation ->
            val cpgOperationNode = session.findNodeById(operation.nodeId)
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
                    applyFixedValues(operation.properties, operationSchema?.properties.orEmpty()),
                    session,
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
                        notes = operation.notes,
                    )
                    .apply {
                        this.codeAndLocationFrom(cpgOperationNode)
                        this.name =
                            Name(
                                "${GenericLLMOperation::class.simpleName}[$operationName]",
                                cpgOperationNode.name,
                            )
                        this.setDFG()
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
        mergeConceptSchemas(schemasToPersist, file)
    }

    return AddConceptsResult(applied = applied, failed = failed)
}

/**
 * Overrides the value of every property in [properties] for which [descriptions] declares a
 * [LLMPropertyDescription.fixedValue], backfills [LLMProperty.description] from the matching
 * [LLMPropertyDescription.description] whenever the caller left it blank, and injects any fixed
 * property from [descriptions] that is missing from [properties] altogether.
 *
 * The description backfill exists as a defense-in-depth measure: even now that
 * [LLMProperty.description] is a required field in the tool's generated JSON schema, "required"
 * only means the caller must supply the key - a model can still comply with an empty string rather
 * than actually repeating the description it already gave once when the concept/operation's schema
 * was declared. Reusing the schema's own [LLMPropertyDescription.description] in that case doesn't
 * depend on the model bothering to repeat itself correctly.
 */
internal fun applyFixedValues(
    properties: List<LLMProperty>,
    descriptions: List<LLMPropertyDescription>,
): List<LLMProperty> {
    val descriptionsByName = descriptions.associateBy { it.name }
    val overridden =
        properties.map { property ->
            val schema = descriptionsByName[property.name]
            property.copy(
                value = schema?.fixedValue ?: property.value,
                description =
                    property.description.ifBlank { schema?.description ?: property.description },
            )
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
 * Resolves a list of [LLMProperty] instances into a [GenericProperties] map, converting each
 * property's string representation into the [GenericPropertyValue] kind matching its declared
 * `type`. Properties whose `type` is [NODE_REFERENCE_TYPE] are resolved against the nodes of
 * [session] and stored as an actual [GenericPropertyValue.NodeReferenceValue] instead of a
 * stringified id; everything else is delegated to [GenericPropertyValue.of], which yields a typed
 * scalar for a recognized type name and a [GenericPropertyValue.StringValue] otherwise.
 *
 * Returns the resolved properties together with a list of failure reasons for any node reference
 * that could not be resolved and any value that does not parse as its declared type.
 */
private fun resolveProperties(
    properties: List<LLMProperty>,
    session: CpgSession,
): Pair<GenericProperties, List<String>> {
    val failures = mutableListOf<String>()
    val resolved =
        properties.associate { property ->
            val value =
                if (property.type.equals(NODE_REFERENCE_TYPE, ignoreCase = true)) {
                    val referencedNode = session.findNodeById(property.value)
                    if (referencedNode == null) {
                        failures.add(
                            "Property \"${property.name}\" declares type $NODE_REFERENCE_TYPE but node ${property.value} was not found or ambiguous."
                        )
                        GenericPropertyValue.StringValue(property.value, property.description)
                    } else {
                        GenericPropertyValue.NodeReferenceValue(
                            referencedNode,
                            property.description,
                        )
                    }
                } else {
                    GenericPropertyValue.of(property.type, property.value, property.description)
                        ?: run {
                            failures.add(
                                "Property \"${property.name}\" declares type ${property.type} but its value \"${property.value}\" cannot be parsed as that type."
                            )
                            GenericPropertyValue.StringValue(property.value, property.description)
                        }
                }
            property.name to value
        }
    return GenericProperties(resolved) to failures
}

/**
 * Merges [applied] - the schema re-derived from one applied concept via [LLMConceptDescription]'s
 * constructor, so holding only that application's properties and operations - into the [stored]
 * schema. Applying a concept extends its schema rather than redefining it: the stored description,
 * properties and operations stay as they are (including a declared
 * [LLMPropertyDescription.fixedValue], which an applied [LLMProperty] cannot carry), and only names
 * not known yet are added. Every property and operation name appears once, also when an application
 * carries several operations of the same name. Replacing a schema is
 * `cpg_add_or_update_llm_concept`'s job.
 */
internal fun mergeIntoSchema(
    stored: LLMConceptDescription?,
    applied: LLMConceptDescription,
): LLMConceptDescription {
    val base = stored ?: applied.copy(properties = emptyList(), operations = emptyList())
    return base.copy(
        properties = (base.properties + applied.properties).distinctBy { it.name },
        operations =
            (base.operations + applied.operations)
                .groupBy { it.name }
                .values
                .map { versions ->
                    versions
                        .first()
                        .copy(
                            properties = versions.flatMap { it.properties }.distinctBy { it.name }
                        )
                },
    )
}

/** Cached content of one store file as of [lastModified]. */
private data class PersistedSchemasCache(
    val lastModified: Long,
    val schemas: List<LLMConceptDescription>,
)

/**
 * One entry per store file, keyed by its canonical path: a single shared slot would let a second
 * file with an identical last-modified timestamp be served the first file's content.
 */
private val persistedSchemasCaches = ConcurrentHashMap<String, PersistedSchemasCache>()

private fun File.cacheKey(): String = canonicalFile.path

/**
 * This function loads persisted concepts and operations from a storage and returns them as a list
 * of [LLMConceptDescription].
 *
 * The result is cached in memory per [file], validated against its last-modified timestamp, so that
 * repeated calls within the same server run (e.g. one per [addLLMConceptAndOperations] invocation)
 * only re-read and re-parse the YAML file when it has actually changed on disk, rather than on
 * every tool call.
 */
internal fun loadPersistedConceptsAndOperations(
    file: File = File(fileName)
): List<LLMConceptDescription> {
    if (!file.exists() || file.length() == 0L) return emptyList()
    val lastModified = file.lastModified()
    persistedSchemasCaches[file.cacheKey()]?.let {
        if (it.lastModified == lastModified) return it.schemas
    }
    val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
    val schemas = mapper.readValue<List<LLMConceptDescription>>(file)
    persistedSchemasCaches[file.cacheKey()] = PersistedSchemasCache(lastModified, schemas)
    return schemas
}

/**
 * Serializes [updateConceptSchemas]'s read-modify-write of the concepts file. Deliberately not
 * [CpgLock]: `cpg_add_or_update_llm_concept` only touches this file, never the graph, and should
 * not block graph readers (or be blocked by them). Never held while taking [CpgLock], so the two
 * cannot deadlock.
 */
private val conceptsFileLock = Any()

/**
 * Merge the given concept schemas into the persisted YAML store. Concepts are matched by name; an
 * existing entry is replaced, otherwise appended.
 */
internal fun persistConceptSchemas(
    schemas: List<LLMConceptDescription>,
    file: File = File(fileName),
) = updateConceptSchemas(schemas, file) { _, schema -> schema }

/**
 * Merges the schemas of applied concepts into the persisted YAML store via [mergeIntoSchema]. The
 * merge runs against the file's content at the time of writing, under [conceptsFileLock], not
 * against a copy read before - so that concurrent applications (e.g. parallel batches) and several
 * applications of one concept within a single call all add up instead of the last one winning.
 */
internal fun mergeConceptSchemas(applied: List<LLMConceptDescription>, file: File) =
    updateConceptSchemas(applied, file, ::mergeIntoSchema)

/**
 * Writes [schemas] into [file], each combined with the stored schema of the same name (`null` if
 * there is none) by [combine]; a concept not stored yet is appended.
 */
private fun updateConceptSchemas(
    schemas: List<LLMConceptDescription>,
    file: File,
    combine:
        (stored: LLMConceptDescription?, schema: LLMConceptDescription) -> LLMConceptDescription,
) {
    synchronized(conceptsFileLock) {
        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        var updated = loadPersistedConceptsAndOperations(file)
        schemas.forEach { schema ->
            val stored = updated.find { it.name == schema.name }
            val combined = combine(stored, schema)
            updated =
                if (stored != null) {
                    updated.map { if (it.name == schema.name) combined else it }
                } else {
                    updated + combined
                }
        }
        writeAtomically(file) { mapper.writeValue(it, updated) }
        // Update the cache directly instead of relying on the new last-modified timestamp, since
        // filesystem mtime resolution (often 1 second) could otherwise make a write within the
        // same tick invisible to loadPersistedConceptsAndOperations()'s staleness check.
        persistedSchemasCaches[file.cacheKey()] =
            PersistedSchemasCache(file.lastModified(), updated)
    }
}

/**
 * Writes [file] by filling a sibling temp file and renaming it over [file]. Writing in place would
 * truncate [file] first, leaving a window where [loadPersistedConceptsAndOperations], which takes
 * no lock, sees an empty or half-written file - an empty read makes
 * `cpg_list_llm_concepts_operations` report that no concept exists.
 */
private fun writeAtomically(file: File, write: (File) -> Unit) {
    val temp = file.resolveSibling("${file.name}.tmp")
    try {
        write(temp)
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
    } catch (e: Throwable) {
        // Delete the temp file if it still exists, so a failed write does not leave a stale
        // .tmp behind on every retry. ATOMIC_MOVE either fully succeeds or leaves temp in place.
        runCatching { temp.delete() }
        throw e
    }
}
