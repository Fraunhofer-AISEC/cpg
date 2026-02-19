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
package de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.callees
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.listOverlayClasses
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.passes.Description
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.serialization.toJSON
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import java.util.function.BiFunction
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
 * Registers a [io.modelcontextprotocol.kotlin.sdk.types.Tool] to the MCP [Server]. The tool's input
 * schema is automatically generated from the reified type parameter [T] using reflection. The
 * handler function receives the deserialized input of type [T] and the current [TranslationResult],
 * and must return a [CallToolResult] with the output content. The [description] of the tool is
 * automatically extended with parameter information from the schema, so do NOT add this information
 * to the description yourself
 */
inline fun <reified T> Server.addTool(
    name: String,
    description: String,
    title: String? = null,
    outputSchema: ToolSchema? = null,
    toolAnnotations: ToolAnnotations? = null,
    meta: JsonObject? = null,
    noinline handler: (TranslationResult, T) -> CallToolResult,
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
        try {
            val payload =
                request.arguments?.toObject<T>()
                    ?: return@addTool CallToolResult(
                        content =
                            listOf(
                                TextContent(
                                    "Invalid or missing payload for cpg_list_calls_to tool."
                                )
                            )
                    )
            payload.runOnCpg(handler)
        } catch (e: Exception) {
            CallToolResult(
                content =
                    listOf(
                        TextContent("Error executing query: ${e.message ?: e::class.simpleName}")
                    )
            )
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

fun FunctionDeclaration.toSummary() =
    FunctionSummary(
        id = this.id.toString(),
        name = this.name.localName,
        fileName = this.location?.artifactLocation?.fileName,
        startLine = this.location?.region?.startLine,
        endLine = this.location?.region?.endLine,
        parameters =
            this.parameters.map {
                ParameterInfo(
                    id = it.id.toString(),
                    name = it.name.localName,
                    type = it.type.typeName,
                )
            },
        returnType = this.returnTypes.firstOrNull()?.typeName,
        callees = this.callees.map { it.name.localName },
        code = this.code,
    )

fun RecordDeclaration.toSummary() =
    RecordSummary(
        id = this.id.toString(),
        name = this.name.localName,
        fileName = this.location?.artifactLocation?.fileName,
        startLine = this.location?.region?.startLine,
        endLine = this.location?.region?.endLine,
        kind = this.kind,
        fieldCount = this.fields.size,
        methodNames = this.methods.map { it.name.localName },
    )

fun CallExpression.toSummary() =
    CallSummary(
        id = this.id.toString(),
        name = this.name.localName,
        fileName = this.location?.artifactLocation?.fileName,
        startLine = this.location?.region?.startLine,
        endLine = this.location?.region?.endLine,
        arguments = this.arguments.map { "${it.type.typeName} ${it.name.localName}" },
        code = this.code,
    )

/** Returns all available concrete (non-abstract) concept classes. */
fun getAvailableConcepts(): List<Class<out Concept>> {
    return listOverlayClasses<Concept>()
        .filter { !it.kotlin.isAbstract }
        .filter {
            // TODO: The concept/operation build helper are explicitly checking against underlying
            //  node, which some of our concepts don't have.
            !it.packageName.endsWith(".policy")
        }
}

/** Returns all available concrete (non-abstract) operation classes. */
fun getAvailableOperations(): List<Class<out Operation>> {
    return listOverlayClasses<Operation>()
        .filter { !it.kotlin.isAbstract }
        .filter {
            // TODO: The concept/operation build helper are explicitly checking against underlying
            //  node, which some of our concepts don't have.
            !it.packageName.endsWith(".policy")
        }
}

@PublishedApi internal val lenientJson = Json { ignoreUnknownKeys = true }

inline fun <reified T> JsonObject.toObject() =
    lenientJson.decodeFromString<T>(Json.encodeToString(this))

inline fun <reified T> T.runOnCpg(
    query: BiFunction<TranslationResult, T, CallToolResult>
): CallToolResult {
    return try {
        val result =
            globalAnalysisResult
                ?: return CallToolResult(
                    content =
                        listOf(
                            TextContent(
                                "No analysis result available. Please analyze your code first using cpg_analyze."
                            )
                        )
                )
        query.apply(result, this)
    } catch (e: Exception) {
        CallToolResult(
            content =
                listOf(TextContent("Error executing query: ${e.message ?: e::class.simpleName}"))
        )
    }
}
