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

import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addCpgAnalyzeTool
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CpgAnalyzeToolTest {

    private lateinit var server: Server

    @Test
    fun cpgAnalyzeToolIntegrationTest() = runTest {
        val info = Implementation(name = "test-cpg-server", version = "1.0.0")
        val options =
            ServerOptions(
                capabilities =
                    ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
            )
        server = Server(info, options)

        server.addCpgAnalyzeTool()

        val inputSchema = buildJsonObject {
            put("content", "def hello():\n    print('Hello World')")
            put("extension", "py")
        }

        val request =
            CallToolRequest(CallToolRequestParams(name = "cpg_analyze", arguments = inputSchema))

        val tool = server.tools["cpg_analyze"] ?: error("Tool not registered")
        val result = tool.handler(request)

        assertNotNull(globalAnalysisResult, "Result should be set after tool execution")

        val resultContent = result.content.firstOrNull()
        assertIs<TextContent>(resultContent)
        val resultText = resultContent.text
        assertNotNull(resultText, "Result content should not be null")

        val analysisResult = Json.decodeFromString<CpgAnalysisResult>(resultText)

        assertEquals(2, analysisResult.functions)
        assertEquals(1, analysisResult.callExpressions)
        assertNotNull(analysisResult.nodes)
    }

    @Test
    fun cpgAnalyzeToolUnitTest() {
        val payload =
            CpgAnalyzePayload(content = "def hello():\n    print('Hello World')", extension = "py")
        val analysisResult = runCpgAnalyze(payload)
        assertNotNull(globalAnalysisResult, "Result should be set after tool execution")

        assertEquals(2, analysisResult.functions)
        assertEquals(1, analysisResult.callExpressions)
        assertNotNull(analysisResult.nodes)
    }
}
