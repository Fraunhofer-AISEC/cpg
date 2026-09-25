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
package de.fraunhofer.aisec.cpg.ai.mcp.tools

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.addCpgAnalyzeTool
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.listFunctions
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgAnalysisResult
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.DEFAULT_PROJECT_NAME
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.FunctionInfo
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.analysisSessions
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.getSession
import de.fraunhofer.aisec.cpg.ai.mcp.utils.withClient
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach

class CpgAnalysisSessionsTest {

    @BeforeEach
    fun resetSessions() {
        analysisSessions.clear()
    }

    @Test
    fun routesToolCallsToTheNamedProject() =
        withClient(
            registerTools = {
                addCpgAnalyzeTool()
                listFunctions()
            }
        ) { client ->
            client.callTool(
                name = "cpg_analyze",
                arguments =
                    mapOf(
                        "projectName" to "first",
                        "content" to "def foo():\n    print('X')",
                        "extension" to "py",
                    ),
            )
            val analysis =
                client.callTool(
                    name = "cpg_analyze",
                    arguments =
                        mapOf(
                            "projectName" to "second",
                            "content" to "def bar():\n    print('X')",
                            "extension" to "py",
                        ),
                )

            val analysisText = (analysis.content.firstOrNull() as? TextContent)?.text
            assertNotNull(analysisText)
            assertEquals(
                setOf("first", "second"),
                Json.decodeFromString<CpgAnalysisResult>(analysisText).projectNames.toSet(),
            )

            val firstResult =
                client.callTool(
                    name = "cpg_list_functions",
                    arguments = mapOf("projectName" to "first"),
                )
            val firstNames =
                firstResult.content.map {
                    assertIs<TextContent>(it)
                    Json.decodeFromString<FunctionInfo>(it.text).name
                }
            assertTrue(
                firstNames.any { it.endsWith("foo") },
                "expected foo in 'first', got $firstNames",
            )
            assertFalse(firstNames.any { it.endsWith("bar") }, "expected no bar in 'first'")

            val secondResult =
                client.callTool(
                    name = "cpg_list_functions",
                    arguments = mapOf("projectName" to "second"),
                )
            val secondNames =
                secondResult.content.map {
                    assertIs<TextContent>(it)
                    Json.decodeFromString<FunctionInfo>(it.text).name
                }
            assertTrue(
                secondNames.any { it.endsWith("bar") },
                "expected bar in 'second', got $secondNames",
            )
            assertFalse(secondNames.any { it.endsWith("foo") }, "expected no foo in 'second'")
        }

    @Test
    fun routesToolCallsWithoutAProjectNameToTheDefaultProject() =
        withClient(
            registerTools = {
                addCpgAnalyzeTool()
                listFunctions()
            }
        ) { client ->
            val analysis =
                client.callTool(
                    name = "cpg_analyze",
                    arguments =
                        mapOf("content" to "def foo():\n    print('X')", "extension" to "py"),
                )

            val analysisText = (analysis.content.firstOrNull() as? TextContent)?.text
            assertNotNull(analysisText)
            assertEquals(
                listOf(DEFAULT_PROJECT_NAME),
                Json.decodeFromString<CpgAnalysisResult>(analysisText).projectNames,
            )
            assertNotNull(getSession())

            val result = client.callTool(name = "cpg_list_functions", arguments = emptyMap())
            val functionNames =
                result.content.map {
                    assertIs<TextContent>(it)
                    Json.decodeFromString<FunctionInfo>(it.text).name
                }
            assertTrue(
                functionNames.any { it.endsWith("foo") },
                "expected foo in the unnamed result, got $functionNames",
            )
        }

    @Test
    fun reportsTheAnalyzedProjectsForAnUnknownProjectName() =
        withClient(
            registerTools = {
                addCpgAnalyzeTool()
                listFunctions()
            }
        ) { client ->
            client.callTool(
                name = "cpg_analyze",
                arguments =
                    mapOf(
                        "projectName" to "first",
                        "content" to "def foo():\n    print('X')",
                        "extension" to "py",
                    ),
            )

            val result =
                client.callTool(
                    name = "cpg_list_functions",
                    arguments = mapOf("projectName" to "typo"),
                )
            val text = (result.content.firstOrNull() as? TextContent)?.text
            assertNotNull(text)
            assertContains(text, "No analysis result available for 'typo'.")
            assertContains(text, "Analyzed projects: 'first'.")
        }

    @Test
    fun doesNotFallBackToANamedProjectWhenTheProjectNameIsOmitted() =
        withClient(
            registerTools = {
                addCpgAnalyzeTool()
                listFunctions()
            }
        ) { client ->
            client.callTool(
                name = "cpg_analyze",
                arguments =
                    mapOf(
                        "projectName" to "first",
                        "content" to "def foo():\n    print('X')",
                        "extension" to "py",
                    ),
            )

            val result = client.callTool(name = "cpg_list_functions", arguments = emptyMap())
            val text = (result.content.firstOrNull() as? TextContent)?.text
            assertNotNull(text)
            assertContains(text, "No analysis result available.")
            assertContains(text, "Analyzed projects: 'first'.")
        }
}
