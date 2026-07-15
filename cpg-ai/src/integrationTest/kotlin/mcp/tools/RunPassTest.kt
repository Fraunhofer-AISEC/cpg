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

import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addCpgTranslate
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addListPasses
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addRunPass
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.ctx
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.PassInfo
import de.fraunhofer.aisec.cpg.mcp.utils.withClient
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class RunPassTest {
    private val payload =
        mapOf("content" to "def hello(x: int):\n    y = x\n    print(y)", "extension" to "py")

    @Test
    fun serverAddsToolsAndRunsPassOnSelectedNode() =
        withClient(
            registerTools = {
                addCpgTranslate()
                addListPasses()
                addRunPass()
            }
        ) { client ->
            // Run translation to populate the CPG properly
            client.callTool(name = "cpg_translate", arguments = payload)

            val globalAnalysisResult = globalAnalysisResult
            assertNotNull(globalAnalysisResult)

            // Find the call expression node to run a pass on
            val printCall = globalAnalysisResult.calls["print"]
            assertNotNull(printCall)
            val nodeId = printCall.id.toString()
            assertNotNull(nodeId)

            // Execute a simple pass that should affect EOG/DFG, e.g., TypeResolver (safe) or EOG
            val result =
                client.callTool(
                    name = "cpg_run_pass",
                    arguments =
                        mapOf(
                            "passName" to "de.fraunhofer.aisec.cpg.passes.TypeResolver",
                            "nodeId" to nodeId,
                        ),
                )
            val content = (result.content.firstOrNull() as? TextContent)?.text
            assertNotNull(content)
            assertTrue(content.contains("Successfully ran"))
        }

    @Test
    fun serverListsPassesWithRequiredNodeType() =
        withClient(registerTools = { addListPasses() }) { client ->
            val result = client.callTool(name = "cpg_list_passes", arguments = emptyMap())
            assertTrue(result.content.isNotEmpty())

            // Ensure some known passes are present
            val texts = result.content.map { (it as TextContent).text }
            val fqns = texts.map { Json.decodeFromString<PassInfo>(it).fqn }
            assertTrue(fqns.contains("de.fraunhofer.aisec.cpg.passes.TypeResolver"))
            assertTrue(fqns.contains("de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass"))
        }

    @Test
    fun serverTranslateToolProducesNodes() =
        withClient(registerTools = { addCpgTranslate() }) { client ->
            val result =
                client.callTool(
                    name = "cpg_translate",
                    arguments =
                        mapOf(
                            "content" to "def hello():\n    print('Hello World')",
                            "extension" to "py",
                        ),
                )
            val text = (result.content.firstOrNull() as? TextContent)?.text
            assertNotNull(text)
            val parsed = Json.decodeFromString<CpgAnalysisResult>(text)
            assertTrue(parsed.functions > 0)
        }

    @Test
    fun testNoCtx() =
        withClient(
            registerTools = {
                addCpgTranslate()
                addListPasses()
                addRunPass()
            }
        ) { client ->
            // Run translation to populate the CPG properly
            client.callTool(name = "cpg_translate", arguments = payload)

            val globalAnalysisResult = globalAnalysisResult
            assertNotNull(globalAnalysisResult)

            // Find the call expression node to run a pass on
            val printCall = globalAnalysisResult.calls["print"]
            assertNotNull(printCall)
            val nodeId = printCall.id.toString()
            assertNotNull(nodeId)

            // Execute a simple pass that should affect EOG/DFG, e.g., TypeResolver (safe) or EOG
            val request =
                mapOf(
                    "passName" to "de.fraunhofer.aisec.cpg.passes.TypeResolver",
                    "nodeId" to nodeId,
                )
            // Set context to null to simulate missing context. We now expect an error message but
            // no crash
            ctx = null
            val result = client.callTool(name = "cpg_run_pass", arguments = request)
            val content = (result.content.firstOrNull() as? TextContent)?.text
            assertNotNull(content)
            assertTrue(content.contains("Cannot run run_pass without translation context."))
        }

    @Test
    fun testInvalidNodeId() =
        withClient(
            registerTools = {
                addCpgTranslate()
                addListPasses()
                addRunPass()
            }
        ) { client ->
            // Run translation to populate the CPG properly
            client.callTool(name = "cpg_translate", arguments = payload)

            val globalAnalysisResult = globalAnalysisResult
            assertNotNull(globalAnalysisResult)

            // Find the call expression node to run a pass on
            val printCall = globalAnalysisResult.calls["print"]
            assertNotNull(printCall)
            val nodeId = printCall.id.toString()
            assertNotNull(nodeId)

            // Execute a simple pass that should affect EOG/DFG, e.g., TypeResolver (safe) or EOG
            val result =
                client.callTool(
                    name = "cpg_run_pass",
                    arguments =
                        mapOf(
                            "passName" to "de.fraunhofer.aisec.cpg.passes.TypeResolver",
                            "nodeId" to "0", // Invalid node ID to trigger error
                        ),
                )
            val content = (result.content.firstOrNull() as? TextContent)?.text
            assertNotNull(content)
            assertTrue(content.contains("Could not find any node with the ID 0."))
        }

    @Test
    fun testInvalidPassName() =
        withClient(
            registerTools = {
                addCpgTranslate()
                addListPasses()
                addRunPass()
            }
        ) { client ->
            // Run translation to populate the CPG properly
            client.callTool(name = "cpg_translate", arguments = payload)

            val globalAnalysisResult = globalAnalysisResult
            assertNotNull(globalAnalysisResult)

            // Find the call expression node to run a pass on
            val printCall = globalAnalysisResult.calls["print"]
            assertNotNull(printCall)
            val nodeId = printCall.id.toString()
            assertNotNull(nodeId)

            // Execute a simple pass that should affect EOG/DFG, e.g., TypeResolver (safe) or EOG
            val result =
                client.callTool(
                    name = "cpg_run_pass",
                    arguments =
                        mapOf(
                            "passName" to "de.fraunhofer.aisec.cpg.passes.SomeUnknownPass",
                            "nodeId" to nodeId, // Invalid node ID to trigger error
                        ),
                )
            val content = (result.content.firstOrNull() as? TextContent)?.text
            assertNotNull(content)
            assertTrue(
                content.contains(
                    "Could not find the pass de.fraunhofer.aisec.cpg.passes.SomeUnknownPass."
                )
            )
        }

    @Test
    fun testExecuteSamePassTwice() =
        withClient(
            registerTools = {
                addCpgTranslate()
                addListPasses()
                addRunPass()
            }
        ) { client ->
            // Run translation to populate the CPG properly
            client.callTool(name = "cpg_translate", arguments = payload)

            val globalAnalysisResult = globalAnalysisResult
            assertNotNull(globalAnalysisResult)

            // Find the call expression node to run a pass on
            val printCall = globalAnalysisResult.calls["print"]
            assertNotNull(printCall)
            val nodeId = printCall.id.toString()
            assertNotNull(nodeId)

            // Execute a simple pass that should affect EOG/DFG, e.g., TypeResolver (safe) or EOG
            val result =
                client.callTool(
                    name = "cpg_run_pass",
                    arguments =
                        mapOf(
                            "passName" to "de.fraunhofer.aisec.cpg.passes.TypeResolver",
                            "nodeId" to nodeId,
                        ),
                )
            assertTrue(
                result.content.size > 1,
                "We expect that multple entries exist in the result content which means that some passes were actually executed.",
            )
            val content = (result.content.first() as? TextContent)?.text
            assertNotNull(content)
            assertTrue(content.contains("Successfully ran"))
            assertTrue(
                result.content.size > 1,
                "We expect that multple entries exist in the result content which means that some passes were actually executed.",
            )

            val result2 =
                client.callTool(
                    name = "cpg_run_pass",
                    arguments =
                        mapOf(
                            "passName" to "de.fraunhofer.aisec.cpg.passes.TypeResolver",
                            "nodeId" to nodeId,
                        ),
                )
            assertEquals(
                1,
                result2.content.size,
                "We expect that multple entries exist in the result content which means that some passes were actually executed.",
            )
            val content2 = (result2.content.first() as? TextContent)?.text
            assertNotNull(content2)
            assertTrue(content2.contains("Successfully ran"))
        }
}
