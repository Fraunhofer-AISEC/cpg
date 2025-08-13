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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
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
) {
    constructor(
        node: Node
    ) : this(
        nodeId = node.id.toHexString(),
        name = node.name.localName,
        code = node.code,
        fileName = node.location?.artifactLocation?.fileName,
        startLine = node.location?.region?.startLine,
        endLine = node.location?.region?.endLine,
        startColumn = node.location?.region?.startColumn,
        endColumn = node.location?.region?.endColumn,
    )
}

@Serializable
data class TypeInfo(val name: String) {
    constructor(type: Type) : this(type.name.toString())
}

@Serializable
data class ParameterInfo(val name: String, val type: TypeInfo, val defaultValue: String? = null) {
    constructor(
        parameterDeclaration: ParameterDeclaration
    ) : this(
        name = parameterDeclaration.name.toString(),
        type = TypeInfo(parameterDeclaration.type),
        defaultValue = parameterDeclaration.default.toString(),
    )
}

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
) {
    constructor(
        functionDeclaration: FunctionDeclaration
    ) : this(
        nodeId = functionDeclaration.id.toHexString(),
        name = functionDeclaration.name.toString(),
        parameters = functionDeclaration.parameters.map { ParameterInfo(it) },
        signature = functionDeclaration.signature,
        fileName = functionDeclaration.location?.artifactLocation?.fileName,
        startLine = functionDeclaration.location?.region?.startLine,
        endLine = functionDeclaration.location?.region?.endLine,
        startColumn = functionDeclaration.location?.region?.startColumn,
        endColumn = functionDeclaration.location?.region?.endColumn,
    )
}

@Serializable
data class RecordInfo(
    val nodeId: String,
    val name: String,
    val methods: List<FunctionInfo>,
    val fields: List<FieldInfo>,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
    val startColumn: Int?,
    val endColumn: Int?,
) {
    constructor(
        recordDeclaration: RecordDeclaration
    ) : this(
        nodeId = recordDeclaration.id.toHexString(),
        name = recordDeclaration.name.toString(),
        methods = recordDeclaration.methods.map { FunctionInfo(it) },
        fields = recordDeclaration.fields.map { FieldInfo(it) },
        fileName = recordDeclaration.location?.artifactLocation?.fileName,
        startLine = recordDeclaration.location?.region?.startLine,
        endLine = recordDeclaration.location?.region?.endLine,
        startColumn = recordDeclaration.location?.region?.startColumn,
        endColumn = recordDeclaration.location?.region?.endColumn,
    )
}

@Serializable
data class FieldInfo(
    val nodeId: String,
    val name: String,
    val type: TypeInfo,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
    val startColumn: Int?,
    val endColumn: Int?,
) {
    constructor(
        field: FieldDeclaration
    ) : this(
        nodeId = field.id.toHexString(),
        name = field.name.toString(),
        type = TypeInfo(field.type),
        fileName = field.location?.artifactLocation?.fileName,
        startLine = field.location?.region?.startLine,
        endLine = field.location?.region?.endLine,
        startColumn = field.location?.region?.startColumn,
        endColumn = field.location?.region?.endColumn,
    )
}

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
