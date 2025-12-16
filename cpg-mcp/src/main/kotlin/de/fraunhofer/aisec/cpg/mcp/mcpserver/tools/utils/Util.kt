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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.listOverlayClasses
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.serialization.NodeJSON
import de.fraunhofer.aisec.cpg.serialization.toJSON
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import java.util.function.BiFunction
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun <T> QueryTree<T>.toQueryTreeNode(): QueryTreeNode {
    return QueryTreeNode(
        id = this.id.toString(),
        value = this.value.toString(),
        node = this.node?.toJSON(noEdges = true),
        children = this.children.map { it.toQueryTreeNode() },
    )
}

/** Converts any [Node] to a JSON string using the [NodeJSON] format. */
fun Node.toJson() = Json.encodeToString(this.toJSON())

fun OverlayNode.toJson() = Json.encodeToString(OverlayInfo(this))

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

inline fun <reified T> JsonObject.toObject() = Json.decodeFromString<T>(Json.encodeToString(this))

fun CallToolRequest.runOnCpg(
    query: BiFunction<TranslationResult, CallToolRequest, CallToolResult>
): CallToolResult {
    return try {
        var result = globalAnalysisResult

        if (result == null) {
            val content =
                this.arguments?.get("content")?.let {
                    if (it is JsonPrimitive) it.content else null
                }
            val extension =
                this.arguments?.get("extension")?.let {
                    if (it is JsonPrimitive) it.content else null
                }

            if (content != null && extension != null) {
                val payload = CpgAnalyzePayload(content = content, extension = extension)
                runCpgAnalyze(payload)
                result = globalAnalysisResult
            }
        }

        result?.let { query.apply(it, this) }
            ?: CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "No analysis result available. Either run cpg_analyze first or provide 'content' and 'extension' parameters."
                        )
                    )
            )
    } catch (e: Exception) {
        CallToolResult(
            content =
                listOf(TextContent("Error executing query: ${e.message ?: e::class.simpleName}"))
        )
    }
}
