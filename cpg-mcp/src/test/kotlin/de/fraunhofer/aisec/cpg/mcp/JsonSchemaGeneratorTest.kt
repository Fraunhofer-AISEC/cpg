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
package de.fraunhofer.aisec.cpg.mcp

import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgApplyConceptsPayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toSchema
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class JsonSchemaGeneratorTest {

    @Test
    fun testComplexSchema() {
        val expected =
            ToolSchema(
                properties =
                    buildJsonObject {
                        putJsonObject("assignments") {
                            put("type", "array")
                            put("description", "List of concept assignments to perform")
                            putJsonObject("items") {
                                put("type", "object")
                                putJsonObject("properties") {
                                    putJsonObject("arguments") {
                                        put("type", "array")
                                        put(
                                            "description",
                                            "Additional constructor arguments (optional)",
                                        )
                                        putJsonObject("items") {
                                            put("type", "object")
                                            putJsonObject("properties") {
                                                putJsonObject("key") {
                                                    put("type", "string")
                                                    put(
                                                        "description",
                                                        "The key of the key-value pair",
                                                    )
                                                }
                                                putJsonObject("value") {
                                                    put("type", "string")
                                                    put(
                                                        "description",
                                                        "The value of the key-value pair",
                                                    )
                                                }
                                            }
                                            putJsonArray("required") {
                                                add("key")
                                                add("value")
                                            }
                                        }
                                    }
                                    putJsonObject("conceptNodeId") {
                                        put("type", "string")
                                        put(
                                            "description",
                                            "NodeId of the concept this operation references (only for operations)",
                                        )
                                    }
                                    putJsonObject("nodeId") {
                                        put("type", "string")
                                        put("description", "ID of the node to apply overlay to")
                                    }
                                    putJsonObject("overlay") {
                                        put("type", "string")
                                        put(
                                            "description",
                                            "Fully qualified name of concept or operation class",
                                        )
                                    }
                                    putJsonObject("overlayType") {
                                        put("type", "string")
                                        put(
                                            "description",
                                            "Type of overlay: 'Concept' or 'Operation'",
                                        )
                                    }
                                    putJsonObject("reasoning") {
                                        put("type", "string")
                                        put(
                                            "description",
                                            "Reasoning for applying this concept/operation (optional)",
                                        )
                                    }
                                    putJsonObject("securityImpact") {
                                        put("type", "string")
                                        put(
                                            "description",
                                            "A description if this concept could have security implications (optional)",
                                        )
                                    }
                                }
                                putJsonArray("required") {
                                    add("nodeId")
                                    add("overlay")
                                }
                            }
                        }
                    },
                required = listOf("assignments"),
            )

        val actual = CpgApplyConceptsPayload::class.toSchema()

        assertEquals(expected, actual)
    }
}
