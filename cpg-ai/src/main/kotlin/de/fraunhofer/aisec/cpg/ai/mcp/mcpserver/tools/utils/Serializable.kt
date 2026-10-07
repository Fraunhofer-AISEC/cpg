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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.callees
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.translationUnit
import de.fraunhofer.aisec.cpg.graph.types.Type
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

@Serializable
data class NodeInfo(
    val nodeId: String,
    val name: String,
    val code: String?,
    val type: String?,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
) {
    constructor(
        node: Node
    ) : this(
        nodeId = node.id.toString(),
        name = node.name.localName,
        code = node.code,
        type = node::class.simpleName,
        fileName = node.location?.artifactLocation?.fileName,
        startLine = node.location?.region?.startLine,
        endLine = node.location?.region?.endLine,
    )
}

@Serializable
data class TypeInfo(val name: String) {
    constructor(type: Type) : this(type.name.toString())
}

@Serializable
data class ParameterInfo(val name: String, val type: TypeInfo, val defaultValue: String? = null) {
    constructor(
        parameterDeclaration: Parameter
    ) : this(
        name = parameterDeclaration.name.toString(),
        type = TypeInfo(parameterDeclaration.type),
        defaultValue = parameterDeclaration.default.toString(),
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
) {
    constructor(
        field: Field
    ) : this(
        nodeId = field.id.toString(),
        name = field.name.toString(),
        type = TypeInfo(field.type),
        fileName = field.location?.artifactLocation?.fileName,
        startLine = field.location?.region?.startLine,
        endLine = field.location?.region?.endLine,
    )
}

@Serializable
data class FunctionInfo(
    val nodeId: String,
    val name: String,
    val parameters: List<ParameterInfo>,
    val signature: String,
    val callees: List<String>,
    val code: String?,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
    val translationUnitId: String?,
) {
    constructor(
        functionDeclaration: Function,
        includeCode: Boolean = true,
    ) : this(
        nodeId = functionDeclaration.id.toString(),
        name = functionDeclaration.name.toString(),
        parameters = functionDeclaration.parameters.map { ParameterInfo(it) },
        signature = functionDeclaration.signature,
        callees = functionDeclaration.callees.map { it.name.localName },
        code = if (includeCode) functionDeclaration.code else null,
        fileName = functionDeclaration.location?.artifactLocation?.fileName,
        startLine = functionDeclaration.location?.region?.startLine,
        endLine = functionDeclaration.location?.region?.endLine,
        translationUnitId = functionDeclaration.translationUnit?.id?.toString(),
    )
}

@Serializable
data class RecordInfo(
    val nodeId: String,
    val name: String,
    val kind: String?,
    val methodNames: List<String>,
    val fieldNames: List<String>,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
    val translationUnitId: String?,
) {
    constructor(
        recordDeclaration: Record
    ) : this(
        nodeId = recordDeclaration.id.toString(),
        name = recordDeclaration.name.toString(),
        kind = recordDeclaration.kind,
        methodNames = recordDeclaration.methods.map { it.name.localName },
        fieldNames = recordDeclaration.fields.map { it.name.localName },
        fileName = recordDeclaration.location?.artifactLocation?.fileName,
        startLine = recordDeclaration.location?.region?.startLine,
        endLine = recordDeclaration.location?.region?.endLine,
        translationUnitId = recordDeclaration.translationUnit?.id?.toString(),
    )
}

@Serializable
data class CallInfo(
    val nodeId: String,
    val name: String,
    val argumentNames: List<String>,
    val resolvedTo: List<String>,
    val code: String?,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
    val translationUnitId: String?,
) {
    constructor(
        callExpression: Call,
        includeCode: Boolean = true,
    ) : this(
        nodeId = callExpression.id.toString(),
        name = callExpression.name.toString(),
        argumentNames = callExpression.arguments.map { it.name.localName },
        resolvedTo = callExpression.invokes.map { it.name.toString() },
        code = if (includeCode) callExpression.code else null,
        fileName = callExpression.location?.artifactLocation?.fileName,
        startLine = callExpression.location?.region?.startLine,
        endLine = callExpression.location?.region?.endLine,
        translationUnitId = callExpression.translationUnit?.id?.toString(),
    )
}

@Serializable
data class CpgAnalysisResult(
    val totalNodes: Int,
    val functions: Int,
    val variables: Int,
    val callExpressions: Int,
    /** The names of the components of the analyzed project. */
    val components: List<String> = listOf(),
    /** Notes about what the project auto-detection recognized, e.g., a compilation database. */
    val detectionNotes: List<String> = listOf(),
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

/** A compact, fixed-size overview of a [de.fraunhofer.aisec.cpg.TranslationResult]. */
@Serializable
data class CpgOverview(
    val components: List<ComponentOverview>,
    /** Functions with a body, i.e., functions defined in the analyzed code. */
    val definedFunctions: Int,
    /** Functions without a body, e.g., declarations of library functions. */
    val declaredOnlyFunctions: Int,
    val records: Int,
    val calls: Int,
    /** Files by the number of functions defined in them. */
    val filesWithMostFunctions: List<NamedCount>,
    /** Defined functions by the number of calls to them. */
    val mostCalledFunctions: List<RankedFunction>,
    /** Callees without a body (e.g., library functions) by the number of calls to them. */
    val externalFunctionsCalled: List<NamedCount>,
    /** Defined functions which are never called, by the number of calls they make. */
    val entryPointCandidates: List<RankedFunction>,
    val entryPointCandidatesTotal: Int,
)

@Serializable data class ComponentOverview(val name: String, val translationUnits: Int)

@Serializable data class NamedCount(val name: String, val count: Int)

@Serializable data class RankedFunction(val nodeId: String, val name: String, val count: Int)

/**
 * Paths through the graph, e.g. the dataflows from or to a node. Tools that find paths return this
 * format, so that the LLM gets their structure and the console can show them as a path or a graph.
 *
 * @property kind What the paths follow, e.g. `dataflow`.
 * @property description What the paths show, e.g. "where the value of `key` comes from".
 * @property start The node the paths were searched from.
 * @property paths The paths, each from its first to its last node in the direction of the flow
 *   (from the source to the sink, also when the search went backwards).
 * @property truncated Whether paths are missing because of the limits of the search.
 */
@Serializable
data class NodePaths(
    val kind: String,
    val description: String,
    val start: NodeInfo,
    val paths: List<List<NodeInfo>>,
    val truncated: Boolean,
)
