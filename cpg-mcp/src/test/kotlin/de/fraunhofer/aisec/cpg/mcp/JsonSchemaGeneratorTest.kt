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

import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toSchema
import de.fraunhofer.aisec.cpg.passes.Description
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.Serializable
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
                                    putJsonObject("id") {
                                        put("type", "string")
                                        put("description", "Required id")
                                    }
                                    putJsonObject("securityImpact") {
                                        put("type", "string")
                                        put(
                                            "description",
                                            "A description if this concept could have security implications (optional)",
                                        )
                                    }
                                }
                                putJsonArray("required") { add("id") }
                            }
                        }
                    },
                required = listOf("assignments"),
            )

        val actual = TestPayload::class.toSchema()

        assertEquals(expected, actual)
    }
}

@Serializable
private data class TestPair(
    @Description("The key of the key-value pair") val key: String,
    @Description("The value of the key-value pair") val value: String,
)

@Serializable
private data class TestAssignment(
    @Description("Required id") val id: String,
    @Description("A description if this concept could have security implications (optional)")
    val securityImpact: String? = null,
    @Description("Additional constructor arguments (optional)")
    val arguments: List<TestPair>? = null,
)

@Serializable
private data class TestPayload(
    @Description("List of concept assignments to perform") val assignments: List<TestAssignment>
)
