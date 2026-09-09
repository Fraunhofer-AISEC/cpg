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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.getNode
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.listCalls
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.listCallsTo
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.listFunctions
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CallInfo
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.FunctionInfo
import de.fraunhofer.aisec.cpg.ai.mcp.utils.withClient
import de.fraunhofer.aisec.cpg.serialization.NodeJSON
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach

/**
 * Covers the bulk-listing code-omission change: `cpg_list_functions`/`cpg_list_calls` are for
 * finding candidates by name/signature, not for reading every returned item's full body, so they
 * should omit [FunctionInfo.code]/[CallInfo.code] - `cpg_get_node` remains the way to fetch full
 * details (code included) for a specific node once picked.
 */
class BulkListingCodeOmissionTest {
    @BeforeEach
    fun setAnalysisResult() {
        val payload =
            CpgAnalyzePayload(
                content =
                    "void hello() { printf(\"Hello World\"); }\nint main() { hello(); return 0; }",
                extension = "c",
            )
        runCpgAnalyze(payload, runPasses = true, cleanup = true)
    }

    @Test
    fun listFunctionsOmitsCode() =
        withClient(registerTools = { listFunctions() }) { client ->
            val result = client.callTool(name = "cpg_list_functions", arguments = emptyMap())
            assertTrue(result.content.isNotEmpty(), "Should have function declarations")
            result.content.forEach {
                assertIs<TextContent>(it)
                val info = Json.decodeFromString<FunctionInfo>(it.text)
                assertNull(info.code, "cpg_list_functions should omit code for ${info.name}")
            }
        }

    @Test
    fun listCallsOmitsCode() =
        withClient(registerTools = { listCalls() }) { client ->
            val result = client.callTool(name = "cpg_list_calls", arguments = emptyMap())
            assertTrue(result.content.isNotEmpty(), "Should have call expressions")
            result.content.forEach {
                assertIs<TextContent>(it)
                val info = Json.decodeFromString<CallInfo>(it.text)
                assertNull(info.code, "cpg_list_calls should omit code for ${info.name}")
            }
        }

    @Test
    fun listCallsToOmitsCode() =
        withClient(registerTools = { listCallsTo() }) { client ->
            val result =
                client.callTool(name = "cpg_list_calls_to", arguments = mapOf("name" to "hello"))
            assertTrue(result.content.isNotEmpty(), "Should have call expressions")
            result.content.forEach {
                assertIs<TextContent>(it)
                val info = Json.decodeFromString<CallInfo>(it.text)
                assertNull(info.code, "cpg_list_calls_to should omit code for ${info.name}")
            }
        }

    @Test
    fun getNodeStillReturnsCode() =
        withClient(
            registerTools = {
                listFunctions()
                getNode()
            }
        ) { client ->
            val listResult = client.callTool(name = "cpg_list_functions", arguments = emptyMap())
            val functionInfo =
                Json.decodeFromString<FunctionInfo>(
                    (listResult.content.first() as TextContent).text
                )

            val result =
                client.callTool(
                    name = "cpg_get_node",
                    arguments = mapOf("id" to functionInfo.nodeId),
                )
            val node =
                Json.decodeFromString<NodeJSON>((result.content.single() as TextContent).text)
            assertNotNull(node.code, "cpg_get_node should still return the full source")
            assertTrue(node.code.isNotBlank())
        }
}
