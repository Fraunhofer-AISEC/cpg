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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Registers a [Tool] to the MCP [Server]. The tool's input schema is automatically generated from
 * the reified type parameter [T] using reflection. The handler function receives the [CpgSession]
 * and the deserialized input of type [T] (see [runOnSession]), and must return a [CallToolResult]
 * with the output content. The [description] of the tool is automatically extended with parameter
 * information from the schema, so do NOT add this information to the description yourself
 */
inline fun <reified T> Server.addTool(
    name: String,
    description: String,
    title: String? = null,
    outputSchema: ToolSchema? = null,
    toolAnnotations: ToolAnnotations? = null,
    meta: JsonObject? = null,
    noinline handler: (CpgSession, T) -> CallToolResult,
) {
    val baseSchema = T::class.toSchema()
    val properties = buildJsonObject {
        baseSchema.properties?.forEach { (k, v) -> put(k, v) }
        putJsonObject("projectName") {
            put("type", "string")
            put(
                "description",
                "The name identifying which analyzed project this tool should operate on. If omitted, the tool operates on the project analyzed as 'default'.",
            )
        }
    }
    val inputSchema = ToolSchema(properties = properties, required = baseSchema.required)
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
        val projectName = args["projectName"]?.jsonPrimitive?.contentOrNull
        ToolCall(projectName, payload).runOnSession { session, call ->
            handler(session, call.payload)
        }
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
 */
enum class CpgSessionStatus {
    ANALYSIS,
    METADATA_AVAILABLE,
    LOW_AVAILABLE,
    MEDIUM_AVAILABLE,
    HIGH_AVAILABLE,
}

open class CpgSession(
    val translationResult: TranslationResult,
    val translationContext: TranslationContext,
    val nodeToPass: IdentityHashMap<Node, MutableSet<KClass<out Pass<*>>>> = IdentityHashMap(),
    var status: CpgSessionStatus = CpgSessionStatus.ANALYSIS,
)

const val DEFAULT_PROJECT_NAME = "default"

/**
 * Holds one [CpgSession] per analyzed project, keyed by the project name it was analyzed under.
 * This is the single place a session lives in: everything that has a [TranslationResult] to offer
 * turns it into a [CpgSession] here first (under [DEFAULT_PROJECT_NAME] if it has no name for it),
 * so that resolving a tool call never has to look anywhere else.
 *
 * Resolve a session through [getSession] rather than indexing into this map, so that a missing
 * `projectName` consistently means [DEFAULT_PROJECT_NAME].
 */
val analysisSessions = ConcurrentHashMap<String, CpgSession>()

/**
 * Implemented by a tool call payload that carries the name of the analyzed project the call should
 * operate on. This is the extension point for code outside this module: declare a payload class
 * with a `projectName` and hand it to [runOnSession] to have the matching [CpgSession] resolved,
 * without going through [addTool]'s schema injection.
 */
interface HasProjectNamePayload {
    val projectName: String?
}

/**
 * Adapts a payload of arbitrary type [T], one that does not implement [HasProjectNamePayload]
 * itself
 */
class ToolCall<T>(override val projectName: String?, val payload: T) : HasProjectNamePayload

/**
 * Returns the [CpgSession] analyzed under [projectName], or the one under [DEFAULT_PROJECT_NAME] if
 * no name was given.
 */
fun getSession(projectName: String? = null): CpgSession? =
    analysisSessions[projectName ?: DEFAULT_PROJECT_NAME]

/**
 * Runs [query] on the CPG this payload addresses, i.e. on the [CpgSession] analyzed under
 * [HasProjectNamePayload.projectName].
 */
fun <T : HasProjectNamePayload> T.runOnSession(
    query: (CpgSession, T) -> CallToolResult
): CallToolResult {
    return try {
        val session = getSession(projectName)
        if (session == null) {
            val analyzed = analysisSessions.keys
            val available =
                if (analyzed.isEmpty()) ""
                else " Analyzed projects: ${analyzed.joinToString { "'$it'" }}."
            return CallToolResult(
                content =
                    listOf(
                        TextContent(
                            if (projectName != null)
                                "No analysis result available for '$projectName'.$available Please analyze it first using cpg_analyze."
                            else
                                "No analysis result available.$available Please analyze your code first using cpg_analyze, or pass one of the projects above as 'projectName'."
                        )
                    )
            )
        }
        query(session, this)
    } catch (e: Exception) {
        CallToolResult(
            content =
                listOf(TextContent("Error executing query: ${e.message ?: e::class.simpleName}"))
        )
    }
}
