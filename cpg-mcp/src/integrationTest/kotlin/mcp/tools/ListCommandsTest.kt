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

import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CallSummary
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.FunctionSummary
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.RecordSummary
import de.fraunhofer.aisec.cpg.mcp.utils.withMcpServer
import de.fraunhofer.aisec.cpg.serialization.NodeJSON
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ListCommandsTest {

    private fun setupAnalysis() {
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
    }

    @Test
    fun listFunctionsTest() = withMcpServer { _, client ->
        setupAnalysis()
        val result =
            client.callTool(
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
    fun listRecordsTest() = withMcpServer { _, client ->
        setupAnalysis()
        val result =
            client.callTool(
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
    fun listCallsTest() = withMcpServer { _, client ->
        setupAnalysis()
        val result =
            client.callTool(
                CallToolRequest(
                    CallToolRequestParams(name = "cpg_list_calls", arguments = buildJsonObject {})
                )
            )
        assertNotNull(result)
        assertEquals(1, result.content.size, "Should return one call expression")
    }

    @Test
    fun listCallsToTest() = withMcpServer { _, client ->
        setupAnalysis()
        val result =
            client.callTool(
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
    fun getAllArgsTest() = withMcpServer { _, client ->
        setupAnalysis()
        val callsResult =
            client.callTool(
                CallToolRequest(
                    CallToolRequestParams(name = "cpg_list_calls", arguments = buildJsonObject {})
                )
            )
        val callId =
            Json.decodeFromString<CallSummary>((callsResult.content.first() as TextContent).text).id

        val argsResult =
            client.callTool(
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
            client.callTool(
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
    fun getArgByIndexOrNameTest() = withMcpServer { _, client ->
        setupAnalysis()
        val callsResult =
            client.callTool(
                CallToolRequest(
                    CallToolRequestParams(name = "cpg_list_calls", arguments = buildJsonObject {})
                )
            )
        val callId =
            Json.decodeFromString<CallSummary>((callsResult.content.first() as TextContent).text).id

        val argResultByIndex =
            client.callTool(
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
            client.callTool(
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
    fun listAvailableConceptsTest() = withMcpServer { _, client ->
        val result =
            client.callTool(
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
    fun listAvailableOperationsTest() = withMcpServer { _, client ->
        val result =
            client.callTool(
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
    fun listAvailableConceptsAndOperationsTest() = withMcpServer { _, client ->
        val result =
            client.callTool(
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
