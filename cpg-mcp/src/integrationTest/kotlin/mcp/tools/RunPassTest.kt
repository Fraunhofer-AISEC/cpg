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
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.ctx
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.PassInfo
import de.fraunhofer.aisec.cpg.mcp.utils.withMcpServer
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
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

    private suspend fun translate(
        client: Client,
        content: String = "def hello(x: int):\n    y = x\n    print(y)",
    ) {
        client.callTool(
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_translate",
                    arguments =
                        buildJsonObject {
                            put("content", content)
                            put("extension", "py")
                        },
                )
            )
        )
    }

    @Test
    fun serverAddsToolsAndRunsPassOnSelectedNode() = runTest {
        withMcpServer { server, client ->
            assertTrue(server.tools.keys.contains("cpg_translate"))
            assertTrue(server.tools.keys.contains("cpg_list_passes"))
            assertTrue(server.tools.keys.contains("cpg_run_pass"))

            translate(client)

            val globalAnalysisResult = globalAnalysisResult
            assertNotNull(globalAnalysisResult)

            val printCall = globalAnalysisResult.calls["print"]
            assertNotNull(printCall)
            val nodeId = printCall.id.toString()

            val result =
                client.callTool(
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
                )
            val content = (result.content.firstOrNull() as? TextContent)?.text
            assertNotNull(content)
            assertTrue(content.contains("Successfully ran"))
        }
    }

    @Test
    fun serverListsPassesWithRequiredNodeType() = runTest {
        withMcpServer { _, client ->
            val result =
                client.callTool(
                    CallToolRequest(
                        CallToolRequestParams(
                            name = "cpg_list_passes",
                            arguments = buildJsonObject {},
                        )
                    )
                )
            assertTrue(result.content.isNotEmpty())

            val texts = result.content.map { (it as TextContent).text }
            val fqns = texts.map { Json.decodeFromString<PassInfo>(it).fqn }
            assertTrue(fqns.contains("de.fraunhofer.aisec.cpg.passes.TypeResolver"))
            assertTrue(fqns.contains("de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass"))
        }
    }

    @Test
    fun serverTranslateToolProducesNodes() = runTest {
        withMcpServer { _, client ->
            val result =
                client.callTool(
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
                )
            val text = (result.content.firstOrNull() as? TextContent)?.text
            assertNotNull(text)
            val parsed = Json.decodeFromString<CpgAnalysisResult>(text)
            assertNotNull(parsed.functionSummaries)
            assertTrue(
                parsed.functionSummaries.any {
                    it.name.contains("hello") || it.name.contains("print")
                }
            )
        }
    }

    @Test
    fun testNoCtx() = runTest {
        withMcpServer { _, client ->
            translate(client)

            val globalAnalysisResult = globalAnalysisResult
            assertNotNull(globalAnalysisResult)

            val printCall = globalAnalysisResult.calls["print"]
            assertNotNull(printCall)
            val nodeId = printCall.id.toString()

            ctx = null
            val result =
                client.callTool(
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
                )
            val content = (result.content.firstOrNull() as? TextContent)?.text
            assertNotNull(content)
            assertTrue(content.contains("Cannot run run_pass without translation context."))
        }
    }

    @Test
    fun testInvalidNodeId() = runTest {
        withMcpServer { _, client ->
            translate(client)

            assertNotNull(globalAnalysisResult)

            ctx = null
            val result =
                client.callTool(
                    CallToolRequest(
                        CallToolRequestParams(
                            name = "cpg_run_pass",
                            arguments =
                                buildJsonObject {
                                    put("passName", "de.fraunhofer.aisec.cpg.passes.TypeResolver")
                                    put("nodeId", "0")
                                },
                        )
                    )
                )
            val content = (result.content.firstOrNull() as? TextContent)?.text
            assertNotNull(content)
            assertTrue(content.contains("Could not find any node with the ID 0."))
        }
    }

    @Test
    fun testInvalidPassName() = runTest {
        withMcpServer { _, client ->
            translate(client)

            val globalAnalysisResult = globalAnalysisResult
            assertNotNull(globalAnalysisResult)

            ctx = null
            val result =
                client.callTool(
                    CallToolRequest(
                        CallToolRequestParams(
                            name = "cpg_run_pass",
                            arguments =
                                buildJsonObject {
                                    put(
                                        "passName",
                                        "de.fraunhofer.aisec.cpg.passes.SomeUnknownPass",
                                    )
                                    put(
                                        "nodeId",
                                        globalAnalysisResult.calls["print"]!!.id.toString(),
                                    )
                                },
                        )
                    )
                )
            val content = (result.content.firstOrNull() as? TextContent)?.text
            assertNotNull(content)
            assertTrue(
                content.contains(
                    "Could not find the pass de.fraunhofer.aisec.cpg.passes.SomeUnknownPass."
                )
            )
        }
    }

    @Test
    fun testExecuteSamePassTwice() = runTest {
        withMcpServer { _, client ->
            translate(client)

            val globalAnalysisResult = globalAnalysisResult
            assertNotNull(globalAnalysisResult)

            val printCall = globalAnalysisResult.calls["print"]
            assertNotNull(printCall)
            val nodeId = printCall.id.toString()

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

            val result = client.callTool(request)
            assertTrue(
                result.content.size > 1,
                "We expect multiple entries in the result content, meaning some passes were actually executed.",
            )
            val content = (result.content.first() as? TextContent)?.text
            assertNotNull(content)
            assertTrue(content.contains("Successfully ran"))

            val result2 = client.callTool(request)
            assertEquals(
                1,
                result2.content.size,
                "Second run should only produce one entry since passes were already executed.",
            )
            val content2 = (result2.content.first() as? TextContent)?.text
            assertNotNull(content2)
            assertTrue(content2.contains("Successfully ran"))
        }
    }
}
