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
package de.fraunhofer.aisec.cpg.mcp.tools

import de.fraunhofer.aisec.cpg.graph.literals
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addLLMConceptAndOperations
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addOrUpdateConcept
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listLLMConceptsOperations
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.LLMConceptDescription
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.LLMOperation
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.LLMProperty
import de.fraunhofer.aisec.cpg.mcp.utils.withClient
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class CpgGenericConceptsToolTest {

    private val conceptsFile = File("concepts.yaml")

    @BeforeEach
    fun setUp() {
        if (conceptsFile.exists()) conceptsFile.delete()

        val payload =
            CpgAnalyzePayload(
                content =
                    "class Foo:\n    secretKey = '0000'\ndef hello():\n    print('Hello World')",
                extension = "py",
            )
        runCpgAnalyze(payload, runPasses = true, cleanup = true)
        assertNotNull(globalAnalysisResult, "Result should be set after analyze")
    }

    @AfterEach
    fun tearDown() {
        if (conceptsFile.exists()) conceptsFile.delete()
    }

    @Test
    fun listLLMConceptsOperationsWithNoFileTest() =
        withClient(registerTools = { listLLMConceptsOperations() }) { client ->
            val result =
                client.callTool(name = "cpg_list_llm_concepts_operations", arguments = emptyMap())
            assertNotNull(result)
            assertTrue(
                result.content.isEmpty(),
                "With no concepts.yaml present, the list should be empty",
            )
        }

    @Test
    fun addOrUpdateConceptTest() =
        withClient(
            registerTools = {
                addOrUpdateConcept()
                listLLMConceptsOperations()
            }
        ) { client ->
            val addResult =
                client.callTool(
                    name = "cpg_add_or_update_llm_concept",
                    arguments =
                        mapOf(
                            "name" to "Authentication",
                            "description" to "Authenticates a user",
                            "properties" to emptyList<LLMProperty>(),
                            "operations" to emptyList<LLMOperation>(),
                        ),
                )
            assertNotNull(addResult)
            val addText = (addResult.content.single() as TextContent).text
            assertTrue(
                "Saved concept 'Authentication'" in addText,
                "Expected confirmation message, got: $addText",
            )
            assertTrue(conceptsFile.exists(), "concepts.yaml should be created")

            val listResult =
                client.callTool(name = "cpg_list_llm_concepts_operations", arguments = emptyMap())
            assertEquals(1, listResult.content.size, "Exactly one concept should be listed")
            val description =
                Json.decodeFromString<LLMConceptDescription>(
                    (listResult.content.single() as TextContent).text
                )
            assertEquals("Authentication", description.name)
            assertEquals("Authenticates a user", description.description)
        }

    @Test
    fun addOrUpdateConceptReplacesExistingTest() =
        withClient(
            registerTools = {
                addOrUpdateConcept()
                listLLMConceptsOperations()
            }
        ) { client ->
            client.callTool(
                name = "cpg_add_or_update_llm_concept",
                arguments =
                    mapOf(
                        "name" to "Encryption",
                        "description" to "initial",
                        "properties" to emptyList<LLMProperty>(),
                        "operations" to emptyList<LLMOperation>(),
                    ),
            )
            client.callTool(
                name = "cpg_add_or_update_llm_concept",
                arguments =
                    mapOf(
                        "name" to "Encryption",
                        "description" to "updated",
                        "properties" to emptyList<LLMProperty>(),
                        "operations" to emptyList<LLMOperation>(),
                    ),
            )

            val listResult =
                client.callTool(name = "cpg_list_llm_concepts_operations", arguments = emptyMap())
            assertEquals(
                1,
                listResult.content.size,
                "Updating by name must not create a duplicate entry",
            )
            val description =
                Json.decodeFromString<LLMConceptDescription>(
                    (listResult.content.single() as TextContent).text
                )
            assertEquals("updated", description.description)
        }

    @Test
    fun listLLMConceptsOperationsTest() =
        withClient(
            registerTools = {
                addOrUpdateConcept()
                listLLMConceptsOperations()
            }
        ) { client ->
            client.callTool(
                name = "cpg_add_or_update_llm_concept",
                arguments =
                    mapOf(
                        "name" to "Logging",
                        "description" to "Writes log entries",
                        "properties" to
                            listOf(
                                mapOf(
                                    "name" to "level",
                                    "type" to "string",
                                    "description" to "Log level",
                                )
                            ),
                        "operations" to
                            listOf(
                                mapOf(
                                    "name" to "log",
                                    "description" to "Logs a message",
                                    "properties" to emptyList<LLMProperty>(),
                                )
                            ),
                    ),
            )

            val result =
                client.callTool(name = "cpg_list_llm_concepts_operations", arguments = emptyMap())
            assertEquals(1, result.content.size)
            val description =
                Json.decodeFromString<LLMConceptDescription>(
                    (result.content.single() as TextContent).text
                )
            assertEquals("Logging", description.name)
            assertEquals(1, description.operations.size)
            assertEquals("log", description.operations.single().name)
        }

    @Test
    fun addLLMConceptAndOperationsAppliesTest() =
        withClient(
            registerTools = {
                addLLMConceptAndOperations()
                listLLMConceptsOperations()
            }
        ) { client ->
            val secretInitializer =
                globalAnalysisResult?.literals?.singleOrNull { it.value == "0000" }
            assertNotNull(secretInitializer, "Expected the '0000' literal in the analyzed code")
            val nodeId = secretInitializer.id.toString()

            val applyResult =
                client.callTool(
                    name = "cpg_add_llm_concept_and_operations",
                    arguments =
                        mapOf(
                            "concepts" to
                                listOf(
                                    mapOf(
                                        "name" to "Secret",
                                        "description" to "A hardcoded secret",
                                        "nodeId" to nodeId,
                                        "properties" to emptyList<LLMProperty>(),
                                        "operations" to emptyList<LLMOperation>(),
                                    )
                                )
                        ),
                )
            assertNotNull(applyResult)
            val text = (applyResult.content.single() as TextContent).text
            assertTrue("\"applied\"" in text, "Response should contain applied section")

            val listResult =
                client.callTool(name = "cpg_list_llm_concepts_operations", arguments = emptyMap())
            assertEquals(
                1,
                listResult.content.size,
                "The applied concept's schema should be persisted",
            )
            val description =
                Json.decodeFromString<LLMConceptDescription>(
                    (listResult.content.single() as TextContent).text
                )
            assertEquals("Secret", description.name)
        }

    @Test
    fun addLLMConceptAndOperationsNoValidIdTest() =
        withClient(registerTools = { addLLMConceptAndOperations() }) { client ->
            val applyResult =
                client.callTool(
                    name = "cpg_add_llm_concept_and_operations",
                    arguments =
                        mapOf(
                            "concepts" to
                                listOf(
                                    mapOf(
                                        "name" to "HttpRequest",
                                        "description" to "Http request",
                                        "nodeId" to "00000000-0000-0000-0000-000000000000",
                                        "properties" to emptyList<LLMProperty>(),
                                        "operations" to emptyList<LLMOperation>(),
                                    )
                                )
                        ),
                )
            assertNotNull(applyResult)
            val text = (applyResult.content.single() as TextContent).text
            assertTrue("not found" in text)
            assertTrue(!conceptsFile.exists() || conceptsFile.readText().isBlank())
        }
}
