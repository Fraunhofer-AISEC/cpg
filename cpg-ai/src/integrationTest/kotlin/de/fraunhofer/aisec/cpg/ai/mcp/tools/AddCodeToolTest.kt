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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.addAddCodeTool
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.addAnalyzeTool
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.analysisSessions
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.getSession
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.registerSession
import de.fraunhofer.aisec.cpg.ai.mcp.utils.withClient
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.project.project
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach

class AddCodeToolTest {
    @BeforeEach
    fun clearSessions() {
        // The sessions are global, so drop the ones previous tests left behind
        analysisSessions.clear()
    }

    @Test
    fun cpgAnalyzeToolIntegrationTest() =
        withClient(
            registerTools = {
                addAnalyzeTool()
                addAddCodeTool()
            }
        ) { client ->
            val result =
                client.callTool(
                    name = "cpg_analyze",
                    arguments =
                        mapOf(
                            "path" to "src/integrationTest/resources/addCodeTool/simple_config.py",
                            // cpg_add_code requires the analyzed project's frontends/scope
                            // manager to still be alive.
                            "keepLive" to true,
                        ),
                )

            val session = getSession()
            assertNotNull(session, "Result should be set after tool execution")

            val resultContent = result.content.firstOrNull()
            assertIs<TextContent>(resultContent)
            assertNotNull(resultContent.text, "Result content should not be null")

            var inferredCall =
                session.translationResult?.calls?.singleOrNull {
                    it.name.localName == "create_config_list"
                }
            assertNotNull(inferredCall, "Inferred call should not be null")
            var invokes = inferredCall.invokes.singleOrNull()
            assertNotNull(invokes, "Invokes should be null")
            assertTrue(invokes.isInferred, "The invoked function should be inferred")

            val functionCode =
                "def create_config_list(api_key):\n" +
                    "    \"\"\"Processing: Put key into a list\"\"\"\n" +
                    "    config_items = [\"app_name\", api_key, \"version_1.0\"]\n" +
                    "    return config_items"
            val addResult =
                client.callTool(
                    name = "cpg_add_code",
                    arguments = mapOf("code" to functionCode, "languageFileEnding" to "py"),
                )
            val content = addResult.content.firstOrNull()
            assertIs<TextContent>(content, "Result content should not be null")
            assertEquals("The source code has been added to the CPG.", content.text)

            inferredCall =
                session.translationResult?.calls?.singleOrNull {
                    it.name.localName == "create_config_list"
                }
            assertNotNull(inferredCall, "Inferred call should not be null")
            invokes = inferredCall.invokes.singleOrNull()
            assertNotNull(invokes, "Invokes should be null")
            assertFalse(invokes.isInferred, "The invoked function should not be inferred")
            assertEquals(functionCode, invokes.code)
            val arg = inferredCall.arguments.singleOrNull()
            assertNotNull(arg)
            assertTrue(
                invokes.parameters.single() in arg.nextDFG,
                "The parameter and argument should be wired with a DFG edge",
            )
        }

    @Test
    fun cpgAddCodeFailsGracefullyWhenProjectWasNotKeptLive() =
        withClient(
            registerTools = {
                addAnalyzeTool()
                addAddCodeTool()
            }
        ) { client ->
            // keepLive is intentionally omitted here (it defaults to false), so the project's
            // frontends/scope manager are torn down right after cpg_analyze returns, and
            // cpg_add_code cannot possibly keep working with them.
            client.callTool(
                name = "cpg_analyze",
                arguments =
                    mapOf(
                        "path" to "src/integrationTest/resources/addCodeTool/simple_config.py"
                    ),
            )

            val addResult =
                client.callTool(
                    name = "cpg_add_code",
                    arguments =
                        mapOf(
                            "code" to "def extra_helper():\n    pass",
                            "languageFileEnding" to "py",
                        ),
                )

            val content = addResult.content.firstOrNull()
            assertIs<TextContent>(content, "Result content should not be null")
            assertTrue(
                content.text?.contains("disableCleanup") == true,
                "Expected an error explaining that addSource requires disableCleanup to be " +
                    "enabled, got: ${content.text}",
            )
        }

    @Test
    fun cpgAddCodeRequiresComponentWhenMultipleComponentsExist() {
        val topLevel =
            Files.createTempDirectory("cpg-add-code-multi-component-test").toFile().apply {
                deleteOnExit()
            }
        val compA = File(topLevel, "compA").apply { mkdirs() }
        val compB = File(topLevel, "compB").apply { mkdirs() }
        File(compA, "a.py").apply {
            writeText("def existing_in_a():\n    pass\n")
            deleteOnExit()
        }
        File(compB, "b.py").apply {
            writeText("def existing_in_b():\n    pass\n")
            deleteOnExit()
        }

        val project =
            project(topLevel.toPath()) {
                languages { use<PythonLanguage>() }
                component("compA", root = compA.toPath())
                component("compB", root = compB.toPath())
            }
        project.config.disableCleanup = true
        val result = project.analyze()
        registerSession("multi-component-project", result)

        withClient(registerTools = { addAddCodeTool() }) { client ->
            val newCode = "def new_multi_component_function():\n    return 42"

            val ambiguous =
                client.callTool(
                    name = "cpg_add_code",
                    arguments =
                        mapOf(
                            "code" to newCode,
                            "languageFileEnding" to "py",
                            "projectName" to "multi-component-project",
                        ),
                )
            val ambiguousContent = ambiguous.content.firstOrNull()
            assertIs<TextContent>(ambiguousContent, "Result content should not be null")
            assertTrue(ambiguousContent.text?.contains("'component' is required") == true)
            assertTrue(ambiguousContent.text?.contains("compA") == true)
            assertTrue(ambiguousContent.text?.contains("compB") == true)

            val compAComponent = result.components.single { it.name.toString() == "compA" }
            val compBComponent = result.components.single { it.name.toString() == "compB" }
            val compATusBefore = compAComponent.translationUnits.size
            val compBTusBefore = compBComponent.translationUnits.size

            val success =
                client.callTool(
                    name = "cpg_add_code",
                    arguments =
                        mapOf(
                            "code" to newCode,
                            "languageFileEnding" to "py",
                            "component" to "compA",
                            "projectName" to "multi-component-project",
                        ),
                )
            val successContent = success.content.firstOrNull()
            assertIs<TextContent>(successContent, "Result content should not be null")
            assertEquals("The source code has been added to the CPG.", successContent.text)

            // The new code must have landed in the explicitly requested component only.
            assertEquals(compATusBefore + 1, compAComponent.translationUnits.size)
            assertEquals(compBTusBefore, compBComponent.translationUnits.size)
        }
    }

    @Test
    fun cpgAddCodeResolvesCallsAcrossChainedAdds() =
        withClient(
            registerTools = {
                addAnalyzeTool()
                addAddCodeTool()
            }
        ) { client ->
            client.callTool(
                name = "cpg_analyze",
                arguments =
                    mapOf(
                        "path" to "src/integrationTest/resources/addCodeTool/simple_config.py",
                        "keepLive" to true,
                    ),
            )
            val session = getSession()
            assertNotNull(session, "Result should be set after tool execution")

            // Generation 1: supply the real definition of create_config_list, reconciling the
            // inferred stub left over from the initial analysis.
            val definitionCode =
                "def create_config_list(api_key):\n" +
                    "    config_items = [\"app_name\", api_key, \"version_1.0\"]\n" +
                    "    return config_items"
            client.callTool(
                name = "cpg_add_code",
                arguments = mapOf("code" to definitionCode, "languageFileEnding" to "py"),
            )

            val realDefinition =
                session.translationResult?.functions?.singleOrNull {
                    it.name.localName == "create_config_list" && !it.isInferred
                }
            assertNotNull(realDefinition, "Expected generation 1 to add the real definition")

            // Generation 2: a second, separate cpg_add_code call with new code that calls the
            // function declared by generation 1 -- this must resolve against generation 1's real
            // declaration, not re-infer a fresh stub.
            val wrapperCode =
                "def wrapper_uses_config_list():\n" + "    return create_config_list(\"fresh-key\")"
            val addResult =
                client.callTool(
                    name = "cpg_add_code",
                    arguments = mapOf("code" to wrapperCode, "languageFileEnding" to "py"),
                )
            val content = addResult.content.firstOrNull()
            assertIs<TextContent>(content, "Result content should not be null")
            assertEquals("The source code has been added to the CPG.", content.text)

            val wrapper =
                session.translationResult?.functions?.singleOrNull {
                    it.name.localName == "wrapper_uses_config_list"
                }
            assertNotNull(wrapper, "Expected generation 2's wrapper function to have been added")

            val wrapperCall = wrapper.calls.singleOrNull()
            assertNotNull(wrapperCall, "Expected the wrapper to contain exactly one call")
            val invoked = wrapperCall.invokes.singleOrNull()
            assertNotNull(invoked, "Expected the call to resolve to exactly one candidate")
            assertSame(
                realDefinition,
                invoked,
                "Expected generation 2's call to resolve directly to the real function added in " +
                    "generation 1, not a fresh inferred stub",
            )
        }
}
