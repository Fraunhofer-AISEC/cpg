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
package de.fraunhofer.aisec.cpg.serialization

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.persistence.McpDetailLevel
import de.fraunhofer.aisec.cpg.persistence.mcpProperties
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Builds a generic, LLM-facing view of any [Node] via reflection over its
 * [de.fraunhofer.aisec.cpg.graph.Persistable] schema properties (the same mechanism used for the
 * Neo4j export, see [de.fraunhofer.aisec.cpg.persistence.mcpProperties]), instead of a hand-written
 * `@Serializable` DTO per node subclass. This means new [Node] subclasses automatically get a
 * reasonable view without any changes in `cpg-ai`.
 *
 * [detail] controls whether properties annotated with a higher
 * [de.fraunhofer.aisec.cpg.persistence.McpDetail] level (e.g. [Node.code]) are
 * included - [McpDetailLevel.SUMMARY] (the default) omits them, [McpDetailLevel.FULL] includes
 * everything.
 *
 * This intentionally does NOT include relationships yet (DFG/CDG/PDG/OVERLAY, or subclass-specific
 * ones such as `Call.invokes`/`Call.arguments`) - that is a deliberate follow-up once this
 * scalar-only view has been reviewed.
 */
fun Node.toMcpView(detail: McpDetailLevel = McpDetailLevel.SUMMARY): JsonObject {
    val properties = this.mcpProperties(detail)
    return buildJsonObject {
        put("nodeId", this@toMcpView.id.toString())
        put("type", this@toMcpView.javaClass.simpleName)
        properties.forEach { (key, value) ->
            // "id" is already covered by "nodeId" above.
            if (key != "id") {
                put(key, value.toMcpJsonElement())
            }
        }
    }
}

/**
 * Converts an arbitrary property value coming out of
 * [de.fraunhofer.aisec.cpg.persistence.mcpProperties] into a [JsonElement]. Unlike
 * kotlinx.serialization's default `encodeToString`, this does not require a statically-known type
 * for every value - it is exactly what lets [toMcpView] serialize any [Node] subclass without a
 * dedicated `@Serializable` DTO. Any value type not explicitly handled here falls back to its
 * [toString] representation rather than failing.
 */
private fun Any?.toMcpJsonElement(): JsonElement =
    when (this) {
        null -> JsonNull
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Map<*, *> -> {
            val map = this
            buildJsonObject { map.forEach { (k, v) -> put(k.toString(), v.toMcpJsonElement()) } }
        }
        is Iterable<*> -> JsonArray(this.map { it.toMcpJsonElement() })
        else -> JsonPrimitive(this.toString())
    }
