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
package de.fraunhofer.aisec.cpg.mcp.mcpserver.utils

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.listOverlayClasses
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.query.QueryTree
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import java.util.function.BiFunction
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

fun Node.toNodeInfo(): NodeInfo {
    return NodeInfo(this)
}

fun <T> QueryTree<T>.toQueryTreeNode(): QueryTreeNode {
    return QueryTreeNode(
        id = this.id.toString(),
        value = this.value.toString(),
        node = this.node?.toNodeInfo(),
        children = this.children.map { it.toQueryTreeNode() },
    )
}

fun Node.toJson() = Json.encodeToString(NodeInfo(this))

fun FunctionDeclaration.toJson() = Json.encodeToString(FunctionInfo(this))

fun FieldDeclaration.toJson() = Json.encodeToString(FieldInfo(this))

fun RecordDeclaration.toJson() = Json.encodeToString(RecordInfo(this))

fun CallExpression.toJson() = Json.encodeToString(CallInfo(this))

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
