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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

/**
 * Regression tests for the two `MissingFieldException`s observed in `log_dedupe_persist_entries`/
 * `log_consolidate_generic_concepts_tool`: a `cpg_add_or_update_llm_concept` payload omitting
 * [LLMOperationDescription.properties] or [LLMPropertyDescription.type] used to abort the whole
 * schema-registration call instead of falling back to a sensible default.
 */
class LenientSchemaDefaultsTest {

    @Test
    fun operationMissingPropertiesDefaultsToEmptyList() {
        // Reproduces the exact shape of the failing payload from the log: an operation object
        // with no "properties" key at all.
        val json =
            """
            {
              "name": "StateInspection",
              "description": "d",
              "properties": [],
              "operations": [
                {"name": "describe_state", "description": "d2"}
              ]
            }
            """
                .trimIndent()

        val decoded = Json.decodeFromString<LLMConceptDescription>(json)

        assertEquals(emptyList(), decoded.operations.single().properties)
    }

    @Test
    fun propertyMissingTypeDefaultsToString() {
        val json =
            """
            {
              "name": "StateInspection",
              "description": "d",
              "properties": [
                {"name": "subject", "description": "d2"}
              ],
              "operations": []
            }
            """
                .trimIndent()

        val decoded = Json.decodeFromString<LLMConceptDescription>(json)

        assertEquals("String", decoded.properties.single().type)
    }
}
