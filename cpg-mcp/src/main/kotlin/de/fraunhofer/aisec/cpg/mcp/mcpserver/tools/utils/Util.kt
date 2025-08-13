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
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.query.QueryTree
import kotlinx.serialization.json.Json

fun Node.toNodeInfo(): NodeInfo {
    return NodeInfo(
        nodeId = this.id.toString(),
        name = this.name.localName,
        code = this.code,
        fileName =
            this.location?.artifactLocation?.uri?.let { uri ->
                val path = uri.toString()
                path.substringAfterLast('/').substringAfterLast('\\')
            } ?: "unknown",
        startLine = this.location?.region?.startLine ?: 0,
        endLine = this.location?.region?.endLine ?: 0,
        startColumn = this.location?.region?.startColumn ?: 0,
        endColumn = this.location?.region?.endColumn ?: 0,
    )
}

fun <T> QueryTree<T>.toQueryTreeNode(): QueryTreeNode {
    return QueryTreeNode(
        id = this.id.toString(),
        value = this.value.toString(),
        node = this.node?.toNodeInfo(),
        children = this.children.map { it.toQueryTreeNode() },
    )
}

fun FunctionDeclaration.toJson(): String {
    val functionInfo =
        FunctionInfo(
            nodeId = this.id.toHexString(),
            name = this.name.toString(),
            parameters =
                this.parameters.map {
                    ParameterInfo(
                        it.name.toString(),
                        it.type.name.toString(),
                        it.default.toString(),
                    )
                },
            signature = this.signature,
            fileName = this.location?.artifactLocation?.fileName,
            startLine = this.location?.region?.startLine,
            endLine = this.location?.region?.endLine,
            startColumn = this.location?.region?.startColumn,
            endColumn = this.location?.region?.endColumn,
        )
    return Json.encodeToString(functionInfo)
}
