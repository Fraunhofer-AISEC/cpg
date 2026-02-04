/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.mcp.mcpserver.resources

import de.fraunhofer.aisec.cpg.mcp.mcpserver.cpgDescription
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.getAvailableConcepts
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.getAvailableOperations
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.listPasses
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import kotlinx.serialization.json.Json

fun Server.provideCpgDescription() {
    val resourceName = "documentation/cpg"
    val uri = "$CPG_SCHEME$resourceName"
    this.addResource(uri, resourceName, "Provides a description of the CPG") {
        ReadResourceResult(listOf(TextResourceContents(text = cpgDescription, uri = uri)))
    }
}

fun Server.providePasses() {
    val resourceName = "documentation/passes"
    val uri = "$CPG_SCHEME$resourceName"
    this.addResource(
        uri,
        resourceName,
        "Provides a description of the passes available in the CPG framework",
        mimeType = "application/json",
    ) {
        try {
            val passesList = listPasses()
            ReadResourceResult(
                contents =
                    passesList.map { passInfo ->
                        TextResourceContents(
                            Json.encodeToString(passInfo),
                            uri = uri,
                            mimeType = "application/json",
                        )
                    }
            )
        } catch (e: Exception) {
            ReadResourceResult(
                contents =
                    listOf(
                        TextResourceContents(
                            "Error: ${e.message ?: e::class.simpleName}",
                            uri = uri,
                        )
                    )
            )
        }
    }
}

fun Server.documentOperations() {
    val resourceName = "documentation/operations"
    val uri = "$CPG_SCHEME$resourceName"
    this.addResource(
        uri,
        resourceName,
        "Provides a description of available Operations for annotating the CPG with semantic information",
    ) {
        val availableOperations = getAvailableOperations()

        ReadResourceResult(
            contents =
                availableOperations.map { operation ->
                    TextResourceContents(operation.name, uri = uri)
                }
        )
    }
}

fun Server.documentConcepts() {
    val resourceName = "documentation/concepts"
    val uri = "$CPG_SCHEME$resourceName"
    this.addResource(
        uri,
        resourceName,
        "Provides a description of available Concepts for annotating the CPG with semantic information",
    ) {
        val availableConcepts = getAvailableConcepts()

        ReadResourceResult(
            contents =
                availableConcepts.map { operation ->
                    TextResourceContents(operation.name, uri = uri)
                }
        )
    }
}
