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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.runOnCpg
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.toJson
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import kotlin.collections.map

val CPG_SCHEME = "cpg://"

fun Server.components() {
    val resourceName = "components"
    val uri = "$CPG_SCHEME$resourceName"
    this.addResource(
        uri,
        resourceName,
        "List of all components (e.g. different projects, services, ...) in the CPG",
    ) { request: ReadResourceRequest ->
        request.runOnCpg(uri) { result: TranslationResult, _ ->
            ReadResourceResult(
                contents =
                    result.components.map {
                        TextResourceContents(it.toJson(), "cpg://components/${it.id}")
                    }
            )
        }
    }
}

fun Server.translationUnits() {
    val resourceName = "translationUnits"
    val uri = "$CPG_SCHEME$resourceName"
    this.addResource(uri, resourceName, "List of all translation units (e.g. files) in the CPG") {
        request: ReadResourceRequest ->
        request.runOnCpg(uri) { result: TranslationResult, _ ->
            ReadResourceResult(
                contents =
                    result.translationUnits.map {
                        TextResourceContents(it.toJson(), "cpg://translationUnit/${it.id}")
                    }
            )
        }
    }
}

fun Server.records() {
    val resourceName = "records"
    val uri = "$CPG_SCHEME$resourceName"
    this.addResource(
        uri,
        resourceName,
        "List of all record declarations (e.g. classes or structs) in the CPG",
    ) { request: ReadResourceRequest ->
        request.runOnCpg(uri) { result: TranslationResult, _ ->
            ReadResourceResult(
                contents =
                    result.records.map {
                        TextResourceContents(it.toJson(), "cpg://record/${it.id}")
                    }
            )
        }
    }
}

fun Server.functions() {
    val resourceName = "functions"
    val uri = "$CPG_SCHEME$resourceName"
    this.addResource(uri, resourceName, "List of all function declarations in the CPG") {
        request: ReadResourceRequest ->
        request.runOnCpg(uri) { result: TranslationResult, _ ->
            ReadResourceResult(
                contents =
                    result.functions.map {
                        TextResourceContents(it.toJson(), "cpg://function/${it.id}")
                    }
            )
        }
    }
}

fun Server.calls() {
    val resourceName = "calls"
    val uri = "$CPG_SCHEME$resourceName"
    this.addResource(uri, resourceName, "List of all function calls in the CPG") {
        request: ReadResourceRequest ->
        request.runOnCpg(uri) { result: TranslationResult, _ ->
            ReadResourceResult(
                contents =
                    result.calls.map { TextResourceContents(it.toJson(), "cpg://call/${it.id}") }
            )
        }
    }
}
