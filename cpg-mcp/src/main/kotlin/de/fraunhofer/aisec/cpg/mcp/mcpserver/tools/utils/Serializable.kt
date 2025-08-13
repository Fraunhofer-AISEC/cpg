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

import kotlinx.serialization.Serializable

@Serializable
data class NodeInfo(
    val nodeId: String,
    val name: String,
    val code: String?,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
    val startColumn: Int?,
    val endColumn: Int?,
)

@Serializable
data class CpgAnalysisResult(
    val fileName: String,
    val totalNodes: Int,
    val functions: Int,
    val variables: Int,
    val callExpressions: Int,
    val nodes: List<NodeInfo>,
)

@Serializable
data class ParameterInfo(val name: String, val type: String, val defaultValue: String? = null)

@Serializable
data class FunctionInfo(
    val nodeId: String,
    val name: String,
    val parameters: List<ParameterInfo>,
    val signature: String,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
    val startColumn: Int?,
    val endColumn: Int?,
)

@Serializable
data class DataflowResult(
    val fromConcept: String,
    val toConcept: String,
    val foundPaths: List<QueryTreeNode>,
)

@Serializable
data class QueryTreeNode(
    val id: String,
    val value: String,
    val node: NodeInfo?,
    val children: List<QueryTreeNode> = emptyList(),
)
