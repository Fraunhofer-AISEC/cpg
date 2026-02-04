/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addCpgTranslate
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addListPasses
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addRunPass
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.ctx
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.CpgAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.PassInfo
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RunPassTest {
    @Test
    fun serverAddsToolsAndRunsPassOnSelectedNode() = runTest {
        // Spin up server and register tools
        val server =
            Server(
                Implementation(name = "test-cpg-server", version = "1.0.0"),
                ServerOptions(
                    ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
                ),
            )
        server.addCpgTranslate()
        server.addListPasses()
        server.addRunPass()

        // Tools should be listed
        assertTrue(server.tools.keys.contains("cpg_translate"))
        assertTrue(server.tools.keys.contains("cpg_list_passes"))
        assertTrue(server.tools.keys.contains("cpg_run_pass"))

        // Run translation to populate the CPG properly

        val translateTool = server.tools["cpg_translate"]
        assertNotNull(translateTool)
        translateTool.handler(
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_translate",
                    arguments =
                        buildJsonObject {
                            put("content", "def hello(x: int):\n    y = x\n    print(y)")
                            put("extension", "py")
                        },
                )
            )
        )

        val globalAnalysisResult = globalAnalysisResult
        assertNotNull(globalAnalysisResult)

        // Find the call expression node to run a pass on
        val printCall = globalAnalysisResult.calls["print"]
        assertNotNull(printCall)
        val nodeId = printCall.id.toString()
        assertNotNull(nodeId)

        // Execute a simple pass that should affect EOG/DFG, e.g., TypeResolver (safe) or EOG
        val request =
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_run_pass",
                    arguments =
                        buildJsonObject {
                            put("passName", "de.fraunhofer.aisec.cpg.passes.TypeResolver")
                            put("nodeId", nodeId)
                        },
                )
            )
        val runPassTool = server.tools["cpg_run_pass"]
        assertNotNull(runPassTool)
        val result = runPassTool.handler(request)
        val content = (result.content.firstOrNull() as? TextContent)?.text
        assertNotNull(content)
        assertTrue(content.contains("Successfully ran"))
    }

    @Test
    fun serverListsPassesWithRequiredNodeType() = runTest {
        val server =
            Server(
                Implementation(name = "test-cpg-server", version = "1.0.0"),
                ServerOptions(
                    ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
                ),
            )
        server.addListPasses()

        val tool = server.tools["cpg_list_passes"]
        assertNotNull(tool)
        val request =
            CallToolRequest(
                CallToolRequestParams(name = "cpg_list_passes", arguments = buildJsonObject {})
            )
        val result = tool.handler(request)
        assertTrue(result.content.isNotEmpty())

        // Ensure some known passes are present
        val texts = result.content.map { (it as TextContent).text }
        val fqns = texts.map { Json.decodeFromString<PassInfo>(it).fqn }
        assertTrue(fqns.contains("de.fraunhofer.aisec.cpg.passes.TypeResolver"))
        assertTrue(fqns.contains("de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass"))
    }

    @Test
    fun serverTranslateToolProducesNodes() = runTest {
        val server =
            Server(
                Implementation(name = "test-cpg-server", version = "1.0.0"),
                ServerOptions(
                    ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
                ),
            )
        server.addCpgTranslate()

        val request =
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_translate",
                    arguments =
                        buildJsonObject {
                            put("content", "def hello():\n    print('Hello World')")
                            put("extension", "py")
                        },
                )
            )
        val tool = server.tools["cpg_translate"]
        assertNotNull(tool)
        val result = tool.handler(request)
        val text = (result.content.firstOrNull() as? TextContent)?.text
        assertNotNull(text)
        val parsed = Json.decodeFromString<CpgAnalysisResult>(text)
        assertNotNull(parsed.nodes)
        assertTrue(parsed.nodes.any { it.name.contains("hello") || it.name.contains("print") })
    }

    @Test
    fun testNoCtx() = runTest {
        // Spin up server and register tools
        val server =
            Server(
                Implementation(name = "test-cpg-server", version = "1.0.0"),
                ServerOptions(
                    ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
                ),
            )
        server.addCpgTranslate()
        server.addListPasses()
        server.addRunPass()

        // Tools should be listed
        assertTrue(server.tools.keys.contains("cpg_translate"))
        assertTrue(server.tools.keys.contains("cpg_list_passes"))
        assertTrue(server.tools.keys.contains("cpg_run_pass"))

        // Run translation to populate the CPG properly

        val translateTool = server.tools["cpg_translate"]
        assertNotNull(translateTool)
        translateTool.handler(
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_translate",
                    arguments =
                        buildJsonObject {
                            put("content", "def hello(x: int):\n    y = x\n    print(y)")
                            put("extension", "py")
                        },
                )
            )
        )

        val globalAnalysisResult = globalAnalysisResult
        assertNotNull(globalAnalysisResult)

        // Find the call expression node to run a pass on
        val printCall = globalAnalysisResult.calls["print"]
        assertNotNull(printCall)
        val nodeId = printCall.id.toString()
        assertNotNull(nodeId)

        // Execute a simple pass that should affect EOG/DFG, e.g., TypeResolver (safe) or EOG
        val request =
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_run_pass",
                    arguments =
                        buildJsonObject {
                            put("passName", "de.fraunhofer.aisec.cpg.passes.TypeResolver")
                            put("nodeId", nodeId)
                        },
                )
            )
        val runPassTool = server.tools["cpg_run_pass"]
        assertNotNull(runPassTool)
        // Set context to null to simulate missing context. We now expect an error message but no
        // crash
        ctx = null
        val result = runPassTool.handler(request)
        val content = (result.content.firstOrNull() as? TextContent)?.text
        assertNotNull(content)
        assertTrue(content.contains("Cannot run run_pass without translation context."))
    }

    @Test
    fun testInvalidNodeId() = runTest {
        // Spin up server and register tools
        val server =
            Server(
                Implementation(name = "test-cpg-server", version = "1.0.0"),
                ServerOptions(
                    ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
                ),
            )
        server.addCpgTranslate()
        server.addListPasses()
        server.addRunPass()

        // Tools should be listed
        assertTrue(server.tools.keys.contains("cpg_translate"))
        assertTrue(server.tools.keys.contains("cpg_list_passes"))
        assertTrue(server.tools.keys.contains("cpg_run_pass"))

        // Run translation to populate the CPG properly

        val translateTool = server.tools["cpg_translate"]
        assertNotNull(translateTool)
        translateTool.handler(
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_translate",
                    arguments =
                        buildJsonObject {
                            put("content", "def hello(x: int):\n    y = x\n    print(y)")
                            put("extension", "py")
                        },
                )
            )
        )

        val globalAnalysisResult = globalAnalysisResult
        assertNotNull(globalAnalysisResult)

        // Find the call expression node to run a pass on
        val printCall = globalAnalysisResult.calls["print"]
        assertNotNull(printCall)
        val nodeId = printCall.id.toString()
        assertNotNull(nodeId)

        // Execute a simple pass that should affect EOG/DFG, e.g., TypeResolver (safe) or EOG
        val request =
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_run_pass",
                    arguments =
                        buildJsonObject {
                            put("passName", "de.fraunhofer.aisec.cpg.passes.TypeResolver")
                            put("nodeId", "0") // Invalid node ID to trigger error
                        },
                )
            )
        val runPassTool = server.tools["cpg_run_pass"]
        assertNotNull(runPassTool)
        // Set context to null to simulate missing context. We now expect an error message but no
        // crash
        ctx = null
        val result = runPassTool.handler(request)
        val content = (result.content.firstOrNull() as? TextContent)?.text
        assertNotNull(content)
        assertTrue(content.contains("Could not find any node with the ID 0."))
    }

    @Test
    fun testInvalidPassName() = runTest {
        // Spin up server and register tools
        val server =
            Server(
                Implementation(name = "test-cpg-server", version = "1.0.0"),
                ServerOptions(
                    ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
                ),
            )
        server.addCpgTranslate()
        server.addListPasses()
        server.addRunPass()

        // Tools should be listed
        assertTrue(server.tools.keys.contains("cpg_translate"))
        assertTrue(server.tools.keys.contains("cpg_list_passes"))
        assertTrue(server.tools.keys.contains("cpg_run_pass"))

        // Run translation to populate the CPG properly

        val translateTool = server.tools["cpg_translate"]
        assertNotNull(translateTool)
        translateTool.handler(
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_translate",
                    arguments =
                        buildJsonObject {
                            put("content", "def hello(x: int):\n    y = x\n    print(y)")
                            put("extension", "py")
                        },
                )
            )
        )

        val globalAnalysisResult = globalAnalysisResult
        assertNotNull(globalAnalysisResult)

        // Find the call expression node to run a pass on
        val printCall = globalAnalysisResult.calls["print"]
        assertNotNull(printCall)
        val nodeId = printCall.id.toString()
        assertNotNull(nodeId)

        // Execute a simple pass that should affect EOG/DFG, e.g., TypeResolver (safe) or EOG
        val request =
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_run_pass",
                    arguments =
                        buildJsonObject {
                            put("passName", "de.fraunhofer.aisec.cpg.passes.SomeUnknownPass")
                            put("nodeId", nodeId) // Invalid node ID to trigger error
                        },
                )
            )
        val runPassTool = server.tools["cpg_run_pass"]
        assertNotNull(runPassTool)
        // Set context to null to simulate missing context. We now expect an error message but no
        // crash
        ctx = null
        val result = runPassTool.handler(request)
        val content = (result.content.firstOrNull() as? TextContent)?.text
        assertNotNull(content)
        assertTrue(
            content.contains(
                "Could not find the pass de.fraunhofer.aisec.cpg.passes.SomeUnknownPass."
            )
        )
    }

    @Test
    fun testExecuteSamePassTwice() = runTest {
        // Spin up server and register tools
        val server =
            Server(
                Implementation(name = "test-cpg-server", version = "1.0.0"),
                ServerOptions(
                    ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
                ),
            )
        server.addCpgTranslate()
        server.addListPasses()
        server.addRunPass()

        // Tools should be listed
        assertTrue(server.tools.keys.contains("cpg_translate"))
        assertTrue(server.tools.keys.contains("cpg_list_passes"))
        assertTrue(server.tools.keys.contains("cpg_run_pass"))

        // Run translation to populate the CPG properly

        val translateTool = server.tools["cpg_translate"]
        assertNotNull(translateTool)
        translateTool.handler(
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_translate",
                    arguments =
                        buildJsonObject {
                            put("content", "def hello(x: int):\n    y = x\n    print(y)")
                            put("extension", "py")
                        },
                )
            )
        )

        val globalAnalysisResult = globalAnalysisResult
        assertNotNull(globalAnalysisResult)

        // Find the call expression node to run a pass on
        val printCall = globalAnalysisResult.calls["print"]
        assertNotNull(printCall)
        val nodeId = printCall.id.toString()
        assertNotNull(nodeId)

        // Execute a simple pass that should affect EOG/DFG, e.g., TypeResolver (safe) or EOG
        val request =
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_run_pass",
                    arguments =
                        buildJsonObject {
                            put("passName", "de.fraunhofer.aisec.cpg.passes.TypeResolver")
                            put("nodeId", nodeId)
                        },
                )
            )
        val runPassTool = server.tools["cpg_run_pass"]
        assertNotNull(runPassTool)
        val result = runPassTool.handler(request)
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

        val result2 = runPassTool.handler(request)
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
