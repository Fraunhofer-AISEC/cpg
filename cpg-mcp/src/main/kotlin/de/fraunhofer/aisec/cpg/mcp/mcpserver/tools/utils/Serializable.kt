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

import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.serialization.NodeJSON
import kotlinx.serialization.Serializable

@Serializable
data class OverlayInfo(
    val nodeId: String,
    val underlyingNodeId: String?,
    val name: String,
    val code: String?,
    val overlayClass: String?,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
    val startColumn: Int?,
    val endColumn: Int?,
) {
    constructor(
        node: OverlayNode
    ) : this(
        nodeId = node.id.toString(),
        underlyingNodeId = node.underlyingNode?.id?.toString(),
        name = node.name.localName,
        code = node.code,
        overlayClass = node::class.simpleName,
        fileName = node.location?.artifactLocation?.fileName,
        startLine = node.location?.region?.startLine,
        endLine = node.location?.region?.endLine,
        startColumn = node.location?.region?.startColumn,
        endColumn = node.location?.region?.endColumn,
    )
}

@Serializable data class ParameterInfo(val id: String, val name: String, val type: String)

@Serializable
data class FunctionSummary(
    val id: String,
    val name: String,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
    val parameters: List<ParameterInfo>,
    val returnType: String?,
    val callees: List<String>,
    val code: String?,
)

@Serializable
data class RecordSummary(
    val id: String,
    val name: String,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
    val kind: String?,
    val fieldCount: Int,
    val methodNames: List<String>,
)

@Serializable
data class CallSummary(
    val id: String,
    val name: String,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
    val arguments: List<String>,
    val code: String?,
)

@Serializable
data class CpgAnalysisResult(
    val totalNodes: Int,
    val functions: Int,
    val variables: Int,
    val callExpressions: Int,
    val functionSummaries: List<FunctionSummary>,
)

@Serializable
data class DataflowResult(
    val fromConcept: String,
    val toConcept: String,
    val foundPaths: List<QueryTreeNode>,
)

@Serializable
data class QueryTreeNode(
    val queryTreeId: String,
    val value: String,
    val node: NodeJSON?,
    val children: List<QueryTreeNode> = emptyList(),
)
