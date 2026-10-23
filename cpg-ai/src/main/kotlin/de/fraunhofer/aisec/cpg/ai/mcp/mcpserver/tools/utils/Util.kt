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
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.serializer

/**
 * Key in a tool's `_meta` that marks it as registered through [addTool] or [addToolWithoutCpg], so
 * its arguments are decoded and reported the standard way. `toolRegistrationProblems` (in cpg-ai's
 * test fixtures) reports every tool without it.
 */
const val TYPED_ARGUMENTS_META_KEY = "de.fraunhofer.aisec.cpg.ai/typedArguments"

/** Arguments of a tool that takes none; use it as the type parameter of [addTool]. */
@Serializable object NoArguments

/** A tool call's arguments, decoded into [T] or turned into the answer for the model. */
sealed interface DecodedArguments<out T> {
    data class Valid<T>(val payload: T) : DecodedArguments<T>

    data class Invalid(val result: CallToolResult) : DecodedArguments<Nothing>
}

/**
 * Key under which the chat side passes on a tool call whose argument string is not a JSON object
 * (e.g. cut off mid-stream by the backend), so the tool reports it to the model instead of the
 * whole agent run failing (see `assembleStreamedAnswer`).
 */
const val UNPARSABLE_ARGUMENTS_KEY = "cpg_ai_unparsable_arguments"

/**
 * Decodes [request]'s arguments into [T]; on failure, the result lists every problem with them (see
 * [describeInvalidArguments]). The only place tool arguments are decoded.
 */
@PublishedApi
internal inline fun <reified T> decodeArguments(
    name: String,
    request: CallToolRequest,
): DecodedArguments<T> {
    val unparsable = request.arguments?.get(UNPARSABLE_ARGUMENTS_KEY)
    if (unparsable != null) {
        val message = describeUnparsableArguments(name, unparsable.jsonPrimitive.contentOrNull)
        return DecodedArguments.Invalid(CallToolResult(content = listOf(TextContent(message))))
    }
    return try {
        DecodedArguments.Valid(request.arguments.toPayload<T>())
    } catch (e: SerializationException) {
        val message =
            describeInvalidArguments(name, serializer<T>().descriptor, request.arguments, e)
        DecodedArguments.Invalid(CallToolResult(content = listOf(TextContent(message))))
    }
}

/**
 * The answer to a call whose argument string was not a JSON object ([UNPARSABLE_ARGUMENTS_KEY]).
 */
@PublishedApi
internal fun describeUnparsableArguments(tool: String, received: String?): String =
    "Invalid arguments for $tool: the arguments did not arrive as complete JSON (received: " +
        "${received?.take(200)?.let { "`$it`" } ?: "nothing"}; probably cut off). " +
        "Send the complete call again."

/** [description] followed by one line per parameter in [schema], if it has any. */
@PublishedApi
internal fun describeWithParameters(description: String, schema: ToolSchema): String {
    val parameters =
        schema.properties.orEmpty().map { (k, v) ->
            "- $k: ${v.jsonObject["description"]?.jsonPrimitive?.content ?: ""}"
        }
    return if (parameters.isEmpty()) description
    else parameters.joinToString("\n", prefix = "$description\n\nParameters:\n")
}

@PublishedApi
internal fun withTypedArgumentsMarker(meta: JsonObject?): JsonObject =
    JsonObject(meta.orEmpty() + (TYPED_ARGUMENTS_META_KEY to JsonPrimitive(true)))

/**
 * Registers a [io.modelcontextprotocol.kotlin.sdk.types.Tool] to the MCP [Server]. The tool's input
 * schema is automatically generated from the reified type parameter [T] using reflection (use
 * [NoArguments] for a tool without any). The handler function receives the [CpgSession] and the
 * deserialized input of type [T] (see [runOnSession]), and must return a [CallToolResult] with the
 * output content. The session is the one of the project named by the payload's
 * [HasProjectNamePayload.projectName]. The [description] of the tool is automatically extended with
 * parameter information from the schema, so do NOT add this information to the description
 * yourself. Arguments that do not decode are answered with the list of problems, without calling
 * [handler].
 *
 * The handler runs under the session's [CpgLock]. [readOnly] is required, so every tool has to
 * decide: `true` keeps it concurrent (shared lock), `false` is for tools that change the graph (or
 * do a read-modify-write on a file shared with other tools); they run exclusively. The tool is
 * announced to clients with the MCP annotation `readOnlyHint = readOnly`; the chat side runs only
 * read-only tools concurrently and everything else sequentially, in the order the model called
 * them.
 *
 * Register every tool through this or [addToolWithoutCpg], never through the SDK's own `addTool`:
 * only these decode arguments the standard way, and `toolRegistrationProblems` (in cpg-ai's test
 * fixtures) flags the rest.
 */
inline fun <reified T : HasProjectNamePayload> Server.addTool(
    name: String,
    description: String,
    readOnly: Boolean,
    title: String? = null,
    outputSchema: ToolSchema? = null,
    toolAnnotations: ToolAnnotations? = null,
    meta: JsonObject? = null,
    noinline handler: (CpgSession, T) -> CallToolResult,
) {
    val inputSchema = T::class.toSchema()
    checkSchemaMatchesSerialNames<T>(name, inputSchema)
    this.addTool(
        name,
        describeWithParameters(description, inputSchema),
        inputSchema = inputSchema,
        title = title,
        outputSchema = outputSchema,
        toolAnnotations = toolAnnotations ?: ToolAnnotations(readOnlyHint = readOnly),
        meta = withTypedArgumentsMarker(meta),
    ) { request ->
        try {
            when (val decoded = decodeArguments<T>(name, request)) {
                is DecodedArguments.Invalid -> decoded.result
                is DecodedArguments.Valid -> decoded.payload.runOnSession(!readOnly, handler)
            }
        } catch (e: Exception) {
            CallToolResult(
                content =
                    listOf(
                        TextContent("Error executing $name: ${e.message ?: e::class.simpleName}")
                    )
            )
        }
    }
}

/**
 * Like [addTool], for tools that do not work on the shared CPG - they only touch files, or take
 * [CpgLock] themselves (e.g. `cpg_analyze`, which replaces the graph): [handler] gets just the
 * decoded arguments and runs without the lock. Unlike in [addTool], [readOnly] is required: say
 * `false` if the tool changes the graph or a file (see [addTool]).
 */
inline fun <reified T> Server.addToolWithoutCpg(
    name: String,
    description: String,
    readOnly: Boolean,
    title: String? = null,
    outputSchema: ToolSchema? = null,
    toolAnnotations: ToolAnnotations? = null,
    meta: JsonObject? = null,
    noinline handler: (T) -> CallToolResult,
) {
    val inputSchema = T::class.toSchema()
    checkSchemaMatchesSerialNames<T>(name, inputSchema)
    this.addTool(
        name,
        describeWithParameters(description, inputSchema),
        inputSchema = inputSchema,
        title = title,
        outputSchema = outputSchema,
        toolAnnotations = toolAnnotations ?: ToolAnnotations(readOnlyHint = readOnly),
        meta = withTypedArgumentsMarker(meta),
    ) { request ->
        try {
            when (val decoded = decodeArguments<T>(name, request)) {
                is DecodedArguments.Invalid -> decoded.result
                is DecodedArguments.Valid -> handler(decoded.payload)
            }
        } catch (e: Exception) {
            CallToolResult(
                content =
                    listOf(
                        TextContent("Error executing $name: ${e.message ?: e::class.simpleName}")
                    )
            )
        }
    }
}

/**
 * Asserts that the property names in [schema] (built from Kotlin reflection, see [toSchema]) match
 * the serial names the decoder actually accepts (from [serializer]'s descriptor). A `@SerialName`
 * on a payload property would make these diverge: the model sends the schema name, the decoder
 * reports it as missing. Fails at server startup rather than silently desyncing the model.
 */
@PublishedApi
internal inline fun <reified T> checkSchemaMatchesSerialNames(
    toolName: String,
    schema: ToolSchema,
) {
    val descriptor = serializer<T>().descriptor
    val schemaNames = schema.properties.orEmpty().keys
    val serialNames =
        (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }.toSet()
    check(schemaNames == serialNames) {
        "Schema property names for tool '$toolName' ($schemaNames) do not match the serial names " +
            "of ${T::class.qualifiedName} ($serialNames). A @SerialName on a payload property " +
            "would make the model send a name the decoder does not accept."
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
 * [FunctionInfo.code] field when [includeCode] is false, as bulk listings do: `cpg_get_node`
 * fetches the full details (code included) for a specific function once picked.
 */
fun Function.toInfo(includeCode: Boolean = true) = FunctionInfo(this, includeCode)

fun Record.toInfo() = RecordInfo(this)

/** See [Function.toInfo] - the same reasoning applies to [Call]/[CallInfo.code]. */
fun Call.toInfo(includeCode: Boolean = true) = CallInfo(this, includeCode)

/**
 * The compact entry of a function listing, e.g. `parse(char*)` at `parser.c:40`. The location tells
 * apart functions with the same signature, such as a declaration and its definition.
 */
fun Function.toSignatureInfo() = SignatureInfo(id.toString(), signature, shortLocation())

/**
 * The compact entry of a call listing, e.g. `memcpy(buf, src, len)` at `net.c:142`. Arguments
 * without a name (literals, nested expressions) are shown as `_`.
 */
fun Call.toSignatureInfo() =
    SignatureInfo(
        id.toString(),
        "$name(${arguments.joinToString { it.name.localName.ifBlank { "_" } }})",
        shortLocation(),
    )

/** The compact entry of a record listing, e.g. `struct Connection` at `net.h:12`. */
fun Record.toSignatureInfo() =
    SignatureInfo(
        id.toString(),
        listOfNotNull(kind, name.toString()).joinToString(" "),
        shortLocation(),
    )

/** `file:line`, or `null` if the node has no usable location. */
private fun Node.shortLocation(): String? {
    val location = location ?: return null
    val line = location.region.startLine
    return if (location.artifactLocation.uri == null || line < 0) null
    else "${location.artifactLocation.fileName}:$line"
}

/** The default maximum number of items returned by paginated list tools. */
const val DEFAULT_LIST_LIMIT = 20

/** One page of [items], and - if it does not reach the end - a note on how to fetch the next. */
class Page<T>(val items: List<T>, val summary: String?)

/**
 * Takes the page of [items] selected by [limit]/[offset] (defaulting to [DEFAULT_LIST_LIMIT] items
 * from the start, clamped to at least one item and a non-negative offset). If the page does not
 * reach the end, [Page.summary] says how many items were shown and which offset to use next, so a
 * caller that only sees the tool's textual result still knows there is more to fetch.
 */
fun <T> paginate(items: List<T>, limit: Int?, offset: Int?): Page<T> {
    val start = (offset ?: 0).coerceAtLeast(0)
    val size = (limit ?: DEFAULT_LIST_LIMIT).coerceAtLeast(1)

    val page = items.drop(start).take(size)
    val end = start + page.size
    val summary =
        if (end < items.size) {
            "Showing ${page.size} of ${items.size} items (offset=$start, limit=$size). " +
                "To see more, call this tool again with offset=$end."
        } else {
            null
        }
    return Page(page, summary)
}

/**
 * The page of this list selected by [limit]/[offset] (see [paginate]) as the result of a tool: the
 * [toText] of each item of that page as a [TextContent], followed by the summary entry if there is
 * more. Only the items of the page are converted, so a large listing is not serialized to show a
 * few of its entries.
 */
fun <T> List<T>.toPagedResult(limit: Int?, offset: Int?, toText: (T) -> String): CallToolResult {
    val page = paginate(this, limit, offset)
    return CallToolResult(
        content =
            page.items.map { TextContent(toText(it)) } +
                listOfNotNull(page.summary?.let { TextContent(it) })
    )
}

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
 * Decodes the arguments of a tool call into [T]. A call without any `arguments` is treated like
 * `{}`: a payload whose fields are all optional falls back to its defaults, and one with required
 * fields fails with a [SerializationException] that names the missing field.
 */
inline fun <reified T> JsonObject?.toPayload(): T = (this ?: JsonObject(emptyMap())).toObject<T>()

/**
 * Everything the MCP tools need to operate on one analyzed project: its [translationResult], the
 * [translationContext] to run further passes with, and which passes [nodeToPass] already ran on
 * which nodes. Sessions are kept in [analysisSessions].
 *
 * The [lock] guards the graph of this session, and [nodeIndex] looks nodes up by id (see
 * [findNodeById]). Whatever replaces or changes the AST of the session must call
 * [NodeIndex.invalidate]; [nodeToPass] is keyed by the session's nodes and so belongs to its graph.
 */
open class CpgSession(
    var translationResult: TranslationResult?,
    var translationContext: TranslationContext?,
    val nodeToPass: IdentityHashMap<Node, MutableSet<KClass<out Pass<*>>>> = IdentityHashMap(),
    var status: CpgSessionStatus = CpgSessionStatus.ANALYSIS,
) {
    val lock = CpgLock()
    val nodeIndex = NodeIndex()
}

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
 * [HasProjectNamePayload.projectName] (see [getSession]), under the session's [CpgLock] - shared,
 * or exclusive if [mutating]. If there is no such session, or [query] throws, an error result is
 * returned instead.
 */
fun <T : HasProjectNamePayload> T.runOnSession(
    mutating: Boolean = false,
    query: (CpgSession, T) -> CallToolResult,
): CallToolResult {
    return try {
        var session = getSession(projectName)
        var result: CallToolResult? = null
        // A re-analysis of the project replaces its session while holding the old one's write lock,
        // so a call that waited for that lock must not query the stale session but the new one.
        while (session != null && result == null) {
            val current = session
            result =
                current.lock.withAccess(mutating) {
                    if (getSession(projectName) === current) query(current, this) else null
                }
            if (result == null) session = getSession(projectName)
        }
        if (result != null) return result

        val available = "Available projects: ${analysisSessions.keys.joinToString { "'$it'" }}."
        val message =
            when {
                analysisSessions.isEmpty() ->
                    "No project has been analyzed yet. Please analyze one first using cpg_analyze."
                projectName != null -> "Unknown project '$projectName'. $available"
                else -> "Several projects are analyzed, so 'projectName' is required. $available"
            }
        CallToolResult(content = listOf(TextContent(message)))
    } catch (e: Exception) {
        CallToolResult(
            content =
                listOf(TextContent("Error executing query: ${e.message ?: e::class.simpleName}"))
        )
    }
}
