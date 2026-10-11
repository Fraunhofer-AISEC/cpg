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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.listOverlayClasses
import de.fraunhofer.aisec.cpg.passes.Description
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.serialization.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Registers a [Tool] to the MCP [Server]. The tool's input schema is automatically generated from
 * the reified type parameter [T] using reflection. The handler function receives the [CpgSession]
 * and the deserialized input of type [T] (see [runOnSession]), and must return a [CallToolResult]
 * with the output content. The session is the one of the project named by the payload's
 * [HasProjectNamePayload.projectName]. The [description] of the tool is automatically extended with
 * parameter information from the schema, so do NOT add this information to the description yourself
 */
inline fun <reified T : HasProjectNamePayload> Server.addTool(
    name: String,
    description: String,
    title: String? = null,
    outputSchema: ToolSchema? = null,
    toolAnnotations: ToolAnnotations? = null,
    meta: JsonObject? = null,
    noinline handler: (CpgSession, T) -> CallToolResult,
) {
    val inputSchema = T::class.toSchema()
    val parameters =
        inputSchema.properties
            ?.map { (k, v) ->
                val type = v.jsonObject["type"]?.jsonPrimitive?.content ?: "unknown"
                val description = v.jsonObject["description"]?.jsonPrimitive?.content ?: ""
                "- $k: $description"
            }
            ?.joinToString(separator = "\n", prefix = "$description\n\nParameters:\n") { it }
    this.addTool(
        name,
        description + parameters,
        inputSchema = inputSchema,
        title = title,
        outputSchema = outputSchema,
        toolAnnotations = toolAnnotations,
        meta = meta,
    ) { request ->
        val args = request.arguments ?: buildJsonObject {}
        val payload =
            try {
                args.toObject<T>()
            } catch (e: Exception) {
                return@addTool CallToolResult(
                    content = listOf(TextContent("Invalid or missing payload for $name tool."))
                )
            }
        payload.runOnSession(handler)
    }
}

fun KType.toSchemaType(
    typeProjections: Map<KTypeParameter, KTypeProjection?>? = null
): Pair<String, (JsonObjectBuilder.() -> Unit)?> {
    typeProjections?.get(this.classifier)?.let {
        return it.type?.toSchemaType(typeProjections) ?: ("object" to null)
    }

    return when (val classifier = this.classifier) {
        String::class -> "string" to null
        Int::class,
        Long::class -> "integer" to null
        Float::class,
        Double::class -> "number" to null
        Boolean::class -> "boolean" to null
        Set::class,
        List::class ->
            "array" to
                {
                    this@toSchemaType.arguments.singleOrNull()?.type?.let { itemType ->
                        putJsonObject("items") {
                            val (type, modifier) = itemType.toSchemaType()
                            put("type", type)
                            modifier?.invoke(this)
                        }
                    }
                }
        else ->
            "object" to
                {
                    (classifier as? KClass<*>)?.let { kClass ->
                        this.put("properties", kClass.toSchemaJson(this@toSchemaType.arguments))
                        putJsonArray("required") {
                            kClass.memberProperties.forEach { property ->
                                if (!property.returnType.isMarkedNullable) {
                                    add(property.name)
                                }
                            }
                        }
                    }
                }
    }
}

fun KClass<*>.toSchemaJson(typeProjections: List<KTypeProjection>? = null): JsonObject {
    // Get properties of the KClass, their types and descriptions to build the schema
    return buildJsonObject {
        this@toSchemaJson.memberProperties.forEach { property ->
            val propertyName = property.name
            val paramToProjection =
                this@toSchemaJson.typeParameters
                    .mapIndexed { index, p -> p to typeProjections?.get(index) }
                    .toMap()
            val (propertyType, modifier) = property.returnType.toSchemaType(paramToProjection)
            val description = property.findAnnotations<Description>().firstOrNull()
            putJsonObject(propertyName) {
                put("type", propertyType)
                description?.let { put("description", it.briefDescription) }
                modifier?.invoke(this)
            }
        }
    }
}

fun KClass<*>.toSchema(): ToolSchema {
    val required = mutableListOf<String>()
    // Get properties of the KClass, their types and descriptions to build the schema
    val properties = this.toSchemaJson()

    // Check which properties are nullable
    this@toSchema.memberProperties.forEach { property ->
        if (!property.returnType.isMarkedNullable) {
            required.add(property.name)
        }
    }

    return ToolSchema(properties = properties, required = required)
}

fun <T> QueryTree<T>.toQueryTreeNode(): QueryTreeNode {
    return QueryTreeNode(
        queryTreeId = this.id.toString(),
        value = this.value.toString(),
        node = this.node?.toJSON(noEdges = false),
        children = this.children.map { it.toQueryTreeNode() },
    )
}

/** Converts any [Node] to a JSON string using the [NodeJSON] format. */
fun Node.toJson() = Json.encodeToString(this.toJSON())

fun OverlayNode.toJson() = Json.encodeToString(OverlayInfo(this))

/**
 * Converts to a [FunctionInfo], omitting the (often large - can be an entire function body)
 * [FunctionInfo.code] field when [includeCode] is false. Bulk-listing tools (e.g.
 * `cpg_list_functions`) should pass `false`: they're for finding candidates by name/signature, and
 * embedding every returned function's full body multiplies context size for code the model will
 * mostly never read - `cpg_get_node` fetches the full details (code included) for a specific one
 * once picked.
 */
fun Function.toInfo(includeCode: Boolean = true) = FunctionInfo(this, includeCode)

fun Record.toInfo() = RecordInfo(this)

/** See [Function.toInfo] - the same reasoning applies to [Call]/[CallInfo.code]. */
fun Call.toInfo(includeCode: Boolean = true) = CallInfo(this, includeCode)

/** Returns all available concrete (non-abstract) concept classes. */
fun getAvailableConcepts(): List<Class<out Concept>> {
    return listOverlayClasses<Concept>().filter {
        !it.kotlin.isAbstract &&
            // TODO: The concept/operation build helper are explicitly checking against underlying
            //  node, which some of our concepts don't have.
            !it.packageName.endsWith(".policy")
    }
}

/** Returns all available concrete (non-abstract) operation classes. */
fun getAvailableOperations(): List<Class<out Operation>> {
    return listOverlayClasses<Operation>().filter {
        !it.kotlin.isAbstract &&
            // TODO: The concept/operation build helper are explicitly checking against underlying
            //  node, which some of our concepts don't have.
            !it.packageName.endsWith(".policy")
    }
}

@PublishedApi internal val lenientJson = Json { ignoreUnknownKeys = true }

inline fun <reified T> JsonObject.toObject() =
    lenientJson.decodeFromString<T>(Json.encodeToString(this))

/**
 * The status of a CPG session. The status can be one of the following:
 * - ANALYSIS: The CPG is currently being analyzed.
 * - METADATA_AVAILABLE: The CPG has been analyzed and metadata is available.
 * - LOW_AVAILABLE: The CPG has been analyzed and low-level information is available.
 * - MEDIUM_AVAILABLE: The CPG has been analyzed and medium-level information is available.
 * - HIGH_AVAILABLE: The CPG has been analyzed and high-level information is available.
 *
 * Note: There is no existing logic for the analysis status. Depending on the different use cases or
 * requirements there is an appropriate solution to be implemented.
 */
enum class CpgSessionStatus {
    ANALYSIS,
    METADATA_AVAILABLE,
    LOW_AVAILABLE,
    MEDIUM_AVAILABLE,
    HIGH_AVAILABLE,
}

/**
 * Everything the MCP tools need to operate on one analyzed project: its [translationResult], the
 * [translationContext] to run further passes with, and which passes [nodeToPass] already ran on
 * which nodes. Sessions are kept in [analysisSessions].
 */
open class CpgSession(
    val translationResult: TranslationResult,
    val translationContext: TranslationContext,
    val nodeToPass: IdentityHashMap<Node, MutableSet<KClass<out Pass<*>>>> = IdentityHashMap(),
    var status: CpgSessionStatus = CpgSessionStatus.ANALYSIS,
)

/**
 * Holds one [CpgSession] per analyzed project, keyed by the project name it was analyzed under.
 * Resolve a session through [getSession] rather than indexing into this map, so that a missing
 * `projectName` is handled consistently.
 */
val analysisSessions = ConcurrentHashMap<String, CpgSession>()

/**
 * Implemented by a tool call payload that carries the name of the analyzed project the call should
 * operate on. Every payload registered via [addTool] implements it, and code outside this module
 * can extend it (e.g. with further context of its own) and hand such a payload to [runOnSession] to
 * have the matching [CpgSession] resolved.
 */
interface HasProjectNamePayload {
    val projectName: String?
}

/**
 * Registers [result] as the [CpgSession] of the project [projectName], replacing any session
 * previously registered under that name. This is the entry point for consumers that run their own
 * analysis (e.g. codyze-console) instead of going through `cpg_analyze`.
 */
fun registerSession(projectName: String, result: TranslationResult) {
    analysisSessions[projectName] = CpgSession(result, result.ctx)
}

/**
 * Returns the [CpgSession] analyzed under [projectName]. If no name is given, the only analyzed
 * session is returned, or `null` if there is none or more than one.
 */
fun getSession(projectName: String? = null): CpgSession? =
    if (projectName != null) analysisSessions[projectName]
    else analysisSessions.values.singleOrNull()

/**
 * Runs [query] on the CPG this payload addresses, i.e. on the [CpgSession] resolved for
 * [HasProjectNamePayload.projectName] (see [getSession]). If there is no such session, or [query]
 * throws, an error result is returned instead.
 */
fun <T : HasProjectNamePayload> T.runOnSession(
    query: (CpgSession, T) -> CallToolResult
): CallToolResult {
    return try {
        val session = getSession(projectName)
        if (session == null) {
            val available = "Available projects: ${analysisSessions.keys.joinToString { "'$it'" }}."
            val message =
                when {
                    analysisSessions.isEmpty() ->
                        "No project has been analyzed yet. Please analyze one first using cpg_analyze."
                    projectName != null -> "Unknown project '$projectName'. $available"
                    else ->
                        "Several projects are analyzed, so 'projectName' is required. $available"
                }
            return CallToolResult(content = listOf(TextContent(message)))
        }
        query(session, this)
    } catch (e: Exception) {
        CallToolResult(
            content =
                listOf(TextContent("Error executing query: ${e.message ?: e::class.simpleName}"))
        )
    }
}
