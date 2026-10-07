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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.*
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CallInfo
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.NodePaths
import de.fraunhofer.aisec.cpg.ai.mcp.utils.withClient
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach

class CpgDfgPathsToolTest {
    @BeforeEach
    fun setAnalysisResult() {
        val payload =
            CpgAnalyzePayload(
                content = "def hello():\n    foo = bar\n    print(foo)",
                extension = "py",
            )
        runCpgAnalyze(payload, runPasses = true, cleanup = true)
    }

    @Test
    fun dfgPathsToolTest() =
        withClient(
            registerTools = {
                listCalls()
                addDfgBackwardTool()
                addDfgForwardTool()
            }
        ) { client ->
            val callsResult = client.callTool(name = "cpg_list_calls", arguments = emptyMap())
            assertNotNull(callsResult)
            val callInfo =
                Json.decodeFromString<CallInfo>((callsResult.content.first() as TextContent).text)

            // print(foo) gets its value from bar, through foo
            val backward = callTool(client, "cpg_dfg_backward", callInfo.nodeId)
            assertEquals("dataflow", backward.kind)
            assertEquals(callInfo.nodeId, backward.start.nodeId)
            assertTrue(backward.paths.isNotEmpty(), "no paths")
            for (path in backward.paths) {
                assertEquals(callInfo.nodeId, path.last().nodeId, "a path ends at the call")
            }
            assertTrue(backward.paths.any { path -> path.any { it.name == "bar" } })

            // The value of bar flows to the call, so the path starts at bar
            val bar = backward.paths.flatten().first { it.name == "bar" }
            val forward = callTool(client, "cpg_dfg_forward", bar.nodeId)
            for (path in forward.paths) {
                assertEquals(bar.nodeId, path.first().nodeId, "a path starts at bar")
            }
            assertTrue(forward.paths.any { path -> path.any { it.nodeId == callInfo.nodeId } })
        }

    private suspend fun callTool(client: Client, name: String, id: String): NodePaths {
        val result = client.callTool(name = name, arguments = mapOf("id" to id))
        assertNotNull(result)
        val content = result.content.single()
        assertIs<TextContent>(content)
        return Json.decodeFromString<NodePaths>(content.text)
    }
}
