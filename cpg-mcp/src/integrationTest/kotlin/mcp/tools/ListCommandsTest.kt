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

import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.getAllArgs
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.getArgByIndexOrName
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listAvailableConcepts
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listAvailableOperations
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listCalls
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listCallsTo
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listConceptsAndOperations
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listFunctions
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listRecords
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CallSummary
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.FunctionSummary
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.RecordSummary
import de.fraunhofer.aisec.cpg.serialization.NodeJSON
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
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ListCommandsTest {
    private lateinit var server: Server

    @BeforeEach
    fun initializeServer() {
        val payload =
            CpgAnalyzePayload(
                content =
                    "class Foo:\n    secretKey = '0000'\ndef hello():\n    print('Hello World')",
                extension = "py",
            )
        val analysisResult = runCpgAnalyze(payload, runPasses = true, cleanup = true)
        assertNotNull(globalAnalysisResult, "Result should be set after tool execution")
        assertEquals(2, analysisResult.functions)
        assertEquals(1, analysisResult.callExpressions)
        assertNotNull(analysisResult.functionSummaries)
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
        val result =
            tool.handler(
                CallToolRequest(
                    CallToolRequestParams(
                        name = "cpg_list_functions",
                        arguments = buildJsonObject {},
                    )
                )
            )

        assertNotNull(result, "Result should not be null")
        assertEquals(2, result.content.size, "Should return two function declarations")
        val functionNames =
            result.content.map {
                assertIs<TextContent>(it)
                Json.decodeFromString<FunctionSummary>(it.text).name
            }
        assertNotNull(
            functionNames.singleOrNull { it == "print" },
            "There is exactly one function declaration with name print",
        )
        assertNotNull(
            functionNames.singleOrNull { it.endsWith("hello") },
            "There is exactly one function declaration with local name hello",
        )
    }

    @Test
    fun listRecordsTest() = runTest {
        server.listRecords()
        val tool = server.tools["cpg_list_records"] ?: error("Tool not registered")
        val result =
            tool.handler(
                CallToolRequest(
                    CallToolRequestParams(name = "cpg_list_records", arguments = buildJsonObject {})
                )
            )
        assertNotNull(result)
        assertTrue(
            result.content.isNotEmpty(),
            "There is a record declaration \"Foo\" in the test code",
        )
        assertDoesNotThrow {
            Json.decodeFromString<RecordSummary>(
                (result.content.singleOrNull() as? TextContent)?.text.orEmpty()
            )
        }
    }

    @Test
    fun listCallsTest() = runTest {
        server.listCalls()
        val tool = server.tools["cpg_list_calls"] ?: error("Tool not registered")
        val result =
            tool.handler(
                CallToolRequest(
                    CallToolRequestParams(name = "cpg_list_calls", arguments = buildJsonObject {})
                )
            )
        assertNotNull(result)
        assertEquals(1, result.content.size, "Should return one call expression")
    }

    @Test
    fun listCallsToTest() = runTest {
        server.listCallsTo()
        val tool = server.tools["cpg_list_calls_to"] ?: error("Tool not registered")
        val result =
            tool.handler(
                CallToolRequest(
                    CallToolRequestParams(
                        name = "cpg_list_calls_to",
                        arguments = buildJsonObject { put("name", "print") },
                    )
                )
            )
        assertNotNull(result)
        assertTrue(result.content.isNotEmpty(), "Should return calls to 'print'")
    }

    @Test
    fun getAllArgsTest() = runTest {
        server.listCalls()
        val callsTool = server.tools["cpg_list_calls"] ?: error("Tool not registered")
        val callsResult =
            callsTool.handler(
                CallToolRequest(
                    CallToolRequestParams(name = "cpg_list_calls", arguments = buildJsonObject {})
                )
            )
        val callId =
            Json.decodeFromString<CallSummary>((callsResult.content.first() as TextContent).text).id

        server.getAllArgs()
        val argsTool = server.tools["cpg_list_call_args"] ?: error("Tool not registered")
        val argsResult =
            argsTool.handler(
                CallToolRequest(
                    CallToolRequestParams(
                        name = "cpg_list_call_args",
                        arguments = buildJsonObject { put("id", callId) },
                    )
                )
            )
        assertNotNull(argsResult)
        assertTrue(argsResult.content.isNotEmpty(), "Should return arguments for the call")
        assertDoesNotThrow {
            Json.decodeFromString<NodeJSON>(
                (argsResult.content.singleOrNull() as? TextContent)?.text.orEmpty()
            )
        }

        val wrongArgsResult =
            argsTool.handler(
                CallToolRequest(
                    CallToolRequestParams(
                        name = "cpg_list_call_args",
                        arguments = buildJsonObject { put("nodeId", callId) },
                    )
                )
            )
        assertNotNull(wrongArgsResult)
        assertTrue(wrongArgsResult.content.isNotEmpty(), "Should return arguments for the call")
        assertThrows<IllegalArgumentException> {
            Json.decodeFromString<NodeJSON>((wrongArgsResult.content.first() as TextContent).text)
        }
    }

    @Test
    fun getArgByIndexOrNameTest() = runTest {
        server.listCalls()
        val callsTool = server.tools["cpg_list_calls"] ?: error("Tool not registered")
        val callsResult =
            callsTool.handler(
                CallToolRequest(
                    CallToolRequestParams(name = "cpg_list_calls", arguments = buildJsonObject {})
                )
            )
        val callId =
            Json.decodeFromString<CallSummary>((callsResult.content.first() as TextContent).text).id

        server.getArgByIndexOrName()
        val argTool =
            server.tools["cpg_list_call_arg_by_name_or_index"] ?: error("Tool not registered")
        val argResultByIndex =
            argTool.handler(
                CallToolRequest(
                    CallToolRequestParams(
                        name = "cpg_list_call_arg_by_name_or_index",
                        arguments =
                            buildJsonObject {
                                put("nodeId", callId)
                                put("index", 0)
                            },
                    )
                )
            )
        assertNotNull(argResultByIndex)
        assertTrue(argResultByIndex.content.isNotEmpty(), "Should return the argument at index 0")
        assertDoesNotThrow {
            Json.decodeFromString<NodeJSON>(
                (argResultByIndex.content.singleOrNull() as? TextContent)?.text.orEmpty()
            )
        }

        val argResultByName =
            argTool.handler(
                CallToolRequest(
                    CallToolRequestParams(
                        name = "cpg_list_call_arg_by_name_or_index",
                        arguments = buildJsonObject { put("id", callId) },
                    )
                )
            )
        assertNotNull(argResultByName)
        assertTrue(argResultByName.content.isNotEmpty(), "Should return the error message")
        assertThrows<IllegalArgumentException> {
            Json.decodeFromString<NodeJSON>(
                (argResultByName.content.singleOrNull() as? TextContent)?.text.orEmpty()
            )
        }
    }

    @Test
    fun listAvailableConceptsTest() = runTest {
        server.listAvailableConcepts()
        val tool = server.tools["cpg_list_available_concepts"] ?: error("Tool not registered")
        val result =
            tool.handler(
                CallToolRequest(
                    CallToolRequestParams(
                        name = "cpg_list_available_concepts",
                        arguments = buildJsonObject {},
                    )
                )
            )
        assertNotNull(result)
        assertTrue(result.content.isNotEmpty(), "Should return available concepts")
    }

    @Test
    fun listAvailableOperationsTest() = runTest {
        server.listAvailableOperations()
        val tool = server.tools["cpg_list_available_operations"] ?: error("Tool not registered")
        val result =
            tool.handler(
                CallToolRequest(
                    CallToolRequestParams(
                        name = "cpg_list_available_operations",
                        arguments = buildJsonObject {},
                    )
                )
            )
        assertNotNull(result)
        assertTrue(result.content.isNotEmpty(), "Should return available operations")
    }

    @Test
    fun listAvailableConceptsAndOperationsTest() = runTest {
        server.listConceptsAndOperations()
        val tool = server.tools["cpg_list_concepts_and_operations"] ?: error("Tool not registered")
        val result =
            tool.handler(
                CallToolRequest(
                    CallToolRequestParams(
                        name = "cpg_list_concepts_and_operations",
                        arguments = buildJsonObject {},
                    )
                )
            )
        assertNotNull(result)
        assertTrue(
            result.content.isEmpty(),
            "We did not apply any concepts or operations, so it should be empty",
        )
    }
}
