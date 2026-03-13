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

import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addDfgBackwardTool
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listCalls
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CallSummary
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.QueryTreeNode
import de.fraunhofer.aisec.cpg.mcp.utils.withClient
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach

class CpgDfgBackwardToolTest {
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
    fun dfgBackwardToolTest() =
        withClient(
            registerTools = {
                listCalls()
                addDfgBackwardTool()
            }
        ) { client ->
            val callsResult = client.callTool(name = "cpg_list_calls", arguments = emptyMap())
            assertNotNull(callsResult)
            assertTrue(callsResult.content.isNotEmpty(), "Should have call expressions")

            val callSummary =
                Json.decodeFromString<CallSummary>(
                    (callsResult.content.first() as TextContent).text
                )

            val result =
                client.callTool(
                    name = "cpg_dfg_backward",
                    arguments = mapOf("id" to callSummary.id),
                )
            assertNotNull(result)
            assertTrue(result.content.isNotEmpty())

            val content = result.content.single()
            assertIs<TextContent>(content)

            val queryTreeNode = Json.decodeFromString<QueryTreeNode>(content.text)
            assertNotNull(queryTreeNode)
        }
}
