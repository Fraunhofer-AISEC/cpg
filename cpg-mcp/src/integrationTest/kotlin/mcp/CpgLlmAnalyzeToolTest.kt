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

import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addCpgLlmAnalyzeTool
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.CpgAnalyzePayload
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.BeforeEach

class CpgLlmAnalyzeToolTest {

    private lateinit var server: Server

    @BeforeEach
    fun initializeServer() {
        val payload =
            CpgAnalyzePayload(content = "def hello():\n    print('Hello World')", extension = "py")
        val analysisResult = runCpgAnalyze(payload, runPasses = true, cleanup = true)
        assertNotNull(globalAnalysisResult, "Result should be set after tool execution")

        assertEquals(2, analysisResult.functions)
        assertEquals(1, analysisResult.callExpressions)
        assertNotNull(analysisResult.nodes)
        val info = Implementation(name = "test-cpg-server", version = "1.0.0")
        val options =
            ServerOptions(
                capabilities =
                    ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
            )
        server = Server(info, options)
    }

    @Test
    fun cpgLlmAnalyzeNoPayload() = runTest {
        val info = Implementation(name = "test-cpg-server", version = "1.0.0")
        val options =
            ServerOptions(
                capabilities =
                    ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
            )
        server = Server(info, options)

        server.addCpgLlmAnalyzeTool()

        val inputSchema = buildJsonObject {}

        val request =
            CallToolRequest(
                CallToolRequestParams(name = "cpg_llm_analyze", arguments = inputSchema)
            )

        val tool = server.tools["cpg_llm_analyze"] ?: error("Tool not registered")
        val result = tool.handler(request)

        val resultContent = result.content.firstOrNull()
        assertIs<TextContent>(resultContent)
        val resultText = resultContent.text
        assertNotNull(resultText, "Result content should not be null")
        assertFalse(
            "## Additional Context" in resultText,
            "Result content should not contain the section 'Additional Context'",
        )
    }

    @Test
    fun cpgLlmAnalyzeWithPayload() = runTest {
        val info = Implementation(name = "test-cpg-server", version = "1.0.0")
        val options =
            ServerOptions(
                capabilities =
                    ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
            )
        server = Server(info, options)

        server.addCpgLlmAnalyzeTool()

        val inputSchema = buildJsonObject {
            put("description", "We have some additional context here.")
        }

        val request =
            CallToolRequest(
                CallToolRequestParams(name = "cpg_llm_analyze", arguments = inputSchema)
            )

        val tool = server.tools["cpg_llm_analyze"] ?: error("Tool not registered")
        val result = tool.handler(request)

        val resultContent = result.content.firstOrNull()
        assertIs<TextContent>(resultContent)
        val resultText = resultContent.text
        assertNotNull(resultText, "Result content should not be null")
        assertTrue(
            "## Additional Context" in resultText,
            "Result content should not contain the section 'Additional Context'",
        )
    }
}
