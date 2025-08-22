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

import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listFunctions
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.FunctionInfo
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.BeforeEach

class ListCommandsTest {
    private lateinit var server: Server

    @BeforeEach
    fun initializeServer() {
        val payload =
            CpgAnalyzePayload(content = "def hello():\n    print('Hello World')", extension = "py")
        val analysisResult = runCpgAnalyze(payload)
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
    fun listFunctionsTest() = runTest {
        server.listFunctions()

        val tool = server.tools["cpg_list_functions"] ?: error("Tool not registered")
        val request = CallToolRequest(name = "cpg_list_functions", arguments = buildJsonObject {})
        val result = tool.handler(request)

        assertNotNull(result, "Result should not be null")
        assertEquals(2, result.content.size, "Should return two function declarations")
        val functionNames =
            result.content.map {
                assertIs<TextContent>(it)
                Json.decodeFromString<FunctionInfo>(it.text ?: "").name
            }
        assertNotNull(
            functionNames.singleOrNull { it == "print" },
            "There is exactly one function declaration with name print",
        )
        assertNotNull(
            functionNames.singleOrNull { it.endsWith(".hello") },
            "There is exactly one function declaration with local name hello",
        )
    }
}
