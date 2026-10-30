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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.LLMProperty
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.LLMPropertyDescription
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Plain unit tests for [applyFixedValues], avoiding [CpgGenericConceptsToolTest]'s real CPG/Python
 * analysis setup entirely - that class's `@BeforeEach` requires the Python native library, which
 * isn't available in every environment, whereas [applyFixedValues] itself has no such dependency.
 */
class CpgGenericConceptsToolUnitTest {

    @Test
    fun testBlankLiveDescriptionIsBackfilledFromSchema() {
        val result =
            applyFixedValues(
                properties =
                    listOf(
                        LLMProperty(
                            name = "level",
                            type = "string",
                            description = "",
                            value = "debug",
                        )
                    ),
                descriptions =
                    listOf(
                        LLMPropertyDescription(
                            name = "level",
                            type = "string",
                            description = "Log level",
                        )
                    ),
            )

        assertEquals("Log level", result.single().description)
    }

    @Test
    fun testNonBlankLiveDescriptionIsKept() {
        val result =
            applyFixedValues(
                properties =
                    listOf(
                        LLMProperty(
                            name = "level",
                            type = "string",
                            description = "the log's severity",
                            value = "debug",
                        )
                    ),
                descriptions =
                    listOf(
                        LLMPropertyDescription(
                            name = "level",
                            type = "string",
                            description = "Log level",
                        )
                    ),
            )

        assertEquals("the log's severity", result.single().description)
    }

    @Test
    fun testFixedValueStillOverridesLiveValue() {
        val result =
            applyFixedValues(
                properties =
                    listOf(
                        LLMProperty(
                            name = "algo",
                            type = "string",
                            description = "",
                            value = "wrong",
                        )
                    ),
                descriptions =
                    listOf(
                        LLMPropertyDescription(
                            name = "algo",
                            type = "string",
                            description = "The algorithm identifier",
                            fixedValue = "AES-256",
                        )
                    ),
            )

        assertEquals("AES-256", result.single().value)
        assertEquals("The algorithm identifier", result.single().description)
    }
}
