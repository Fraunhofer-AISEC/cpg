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
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.FunctionInfo
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.analysisSessions
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.getSession
import de.fraunhofer.aisec.cpg.ai.mcp.utils.withClient
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir

class CpgAnalysisSessionsTest {

    @BeforeEach
    fun clearSessions() {
        // The sessions are global, so drop the ones previous tests left behind
        analysisSessions.clear()
    }

    @TempDir lateinit var tempDir: Path

    /** Creates a project directory [name] containing a single Python file with [code]. */
    private fun project(name: String, code: String): Path {
        val dir = tempDir.resolve(name).createDirectory()
        dir.resolve("main.py").writeText(code)
        return dir
    }

    /** Analyzes [dir] via `cpg_analyze` and returns the name the project is identified by. */
    private suspend fun Client.analyze(dir: Path): String {
        val analysis = callTool(name = "cpg_analyze", arguments = mapOf("path" to dir.toString()))
        val text = (analysis.content.firstOrNull() as? TextContent)?.text
        assertNotNull(text)
        return Json.decodeFromString<CpgAnalysisResult>(text).projectName
    }

    private fun CallToolResult.functionNames() =
        content.map {
            assertIs<TextContent>(it)
            Json.decodeFromString<FunctionInfo>(it.text).name
        }

    private fun CallToolResult.text(): String {
        val text = (content.firstOrNull() as? TextContent)?.text
        assertNotNull(text)
        return text
    }

    @Test
    fun identifiesAnAnalyzedProjectByItsDirectoryName() =
        withClient(registerTools = { addCpgAnalyzeTool() }) { client ->
            assertEquals("first", client.analyze(project("first", "def foo():\n    print('X')")))
            assertNotNull(getSession("first"))
        }

    @Test
    fun routesToolCallsToTheNamedProject() =
        withClient(
            registerTools = {
                addCpgAnalyzeTool()
                listFunctions()
            }
        ) { client ->
            val first = client.analyze(project("first", "def foo():\n    print('X')"))
            val second = client.analyze(project("second", "def bar():\n    print('X')"))

            val firstNames =
                client
                    .callTool(
                        name = "cpg_list_functions",
                        arguments = mapOf("projectName" to first),
                    )
                    .functionNames()
            assertTrue(firstNames.any { it.endsWith("foo") }, "expected foo in '$first'")
            assertFalse(firstNames.any { it.endsWith("bar") }, "expected no bar in '$first'")

            val secondNames =
                client
                    .callTool(
                        name = "cpg_list_functions",
                        arguments = mapOf("projectName" to second),
                    )
                    .functionNames()
            assertTrue(secondNames.any { it.endsWith("bar") }, "expected bar in '$second'")
            assertFalse(secondNames.any { it.endsWith("foo") }, "expected no foo in '$second'")
        }

    @Test
    fun routesToolCallsWithoutAProjectNameToTheOnlyProject() =
        withClient(
            registerTools = {
                addCpgAnalyzeTool()
                listFunctions()
            }
        ) { client ->
            client.analyze(project("first", "def foo():\n    print('X')"))

            val names =
                client.callTool(name = "cpg_list_functions", arguments = emptyMap()).functionNames()
            assertTrue(names.any { it.endsWith("foo") }, "expected foo, got $names")
        }

    @Test
    fun reportsTheAvailableProjectsForAnUnknownProjectName() =
        withClient(
            registerTools = {
                addCpgAnalyzeTool()
                listFunctions()
            }
        ) { client ->
            client.analyze(project("first", "def foo():\n    print('X')"))

            val text =
                client
                    .callTool(
                        name = "cpg_list_functions",
                        arguments = mapOf("projectName" to "typo"),
                    )
                    .text()
            assertContains(text, "Unknown project 'typo'.")
            assertContains(text, "Available projects: 'first'.")
        }

    @Test
    fun requiresAProjectNameWhenSeveralProjectsAreAnalyzed() =
        withClient(
            registerTools = {
                addCpgAnalyzeTool()
                listFunctions()
            }
        ) { client ->
            client.analyze(project("first", "def foo():\n    print('X')"))
            client.analyze(project("second", "def bar():\n    print('X')"))

            val text = client.callTool(name = "cpg_list_functions", arguments = emptyMap()).text()
            assertContains(text, "Several projects are analyzed, so 'projectName' is required.")
            assertContains(text, "'first'")
            assertContains(text, "'second'")
        }
}
