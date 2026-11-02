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
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.analysisSessions
import de.fraunhofer.aisec.cpg.ai.mcp.utils.withClient
import de.fraunhofer.aisec.cpg.graph.refs
import de.fraunhofer.aisec.cpg.serialization.NodeSlice
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach

class CpgSliceToolTest {
    @BeforeEach
    fun setAnalysisResult() {
        // The sessions are global, so drop the ones previous tests left behind
        analysisSessions.clear()
        val payload =
            CpgAnalyzePayload(
                content = "def f(c):\n    a = 1\n    if c:\n        a = 2\n    return a",
                extension = "py",
            )
        runCpgAnalyze(payload, runPasses = true, cleanup = true)
    }

    /** The ID of `a` in `return a`. */
    private fun returnedA(): String =
        analysisSessions.values
            .single()
            .translationResult
            .refs
            .last { it.name.localName == "a" }
            .id
            .toString()

    @Test
    fun sliceToolTest() =
        withClient(registerTools = { addSliceTool() }) { client ->
            val id = returnedA()
            val slice =
                Json.decodeFromString<NodeSlice>(callTool(client, mapOf("id" to id, "hops" to 100)))

            assertEquals(id, slice.start.nodeId)
            assertEquals(5, slice.hops, "the hops are capped")
            assertTrue(slice.statements.any { "a = 2" in it.code }, "the assignment in the branch")
            assertTrue(slice.dependences.isNotEmpty(), "no dependences")
        }

    @Test
    fun sliceToolReportsAnUnknownNodeAndGraph() =
        withClient(registerTools = { addSliceTool() }) { client ->
            val unknownNode =
                callTool(client, mapOf("id" to "00000000-0000-0000-0000-000000000000"))
            assertTrue(unknownNode.startsWith("No node found"), unknownNode)

            val unknownGraph = callTool(client, mapOf("id" to returnedA(), "graph" to "EOG"))
            assertTrue(unknownGraph.startsWith("Unknown graph"), unknownGraph)
        }

    private suspend fun callTool(client: Client, arguments: Map<String, Any>): String {
        val result = assertNotNull(client.callTool(name = "cpg_slice", arguments = arguments))
        return (result.content.single() as TextContent).text
    }
}
