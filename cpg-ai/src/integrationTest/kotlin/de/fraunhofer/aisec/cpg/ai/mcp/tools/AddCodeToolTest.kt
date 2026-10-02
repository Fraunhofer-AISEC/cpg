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
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.addCpgAnalyzeTool
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.ai.mcp.utils.withClient
import de.fraunhofer.aisec.cpg.graph.calls
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AddCodeToolTest {
    @Test
    fun cpgAnalyzeToolIntegrationTest() =
        withClient(
            registerTools = {
                addCpgAnalyzeTool()
                addAddCodeTool()
            }
        ) { client ->
            val result =
                client.callTool(
                    name = "cpg_analyze",
                    arguments =
                        mapOf(
                            "path" to "src/integrationTest/resources/addCodeTool/simple_config.py"
                        ),
                )

            assertNotNull(globalAnalysisResult, "Result should be set after tool execution")

            val resultContent = result.content.firstOrNull()
            assertIs<TextContent>(resultContent)
            assertNotNull(resultContent.text, "Result content should not be null")

            var inferredCall =
                globalAnalysisResult.calls.singleOrNull {
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
                globalAnalysisResult.calls.singleOrNull {
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
}
