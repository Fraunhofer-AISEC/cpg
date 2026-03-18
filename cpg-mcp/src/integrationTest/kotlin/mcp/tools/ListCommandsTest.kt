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
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.getNode
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listAvailableConcepts
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listAvailableOperations
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listCalls
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listCallsTo
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listConceptsAndOperations
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listFunctions
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listRecords
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CallInfo
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.FunctionInfo
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.RecordInfo
import de.fraunhofer.aisec.cpg.mcp.utils.withClient
import de.fraunhofer.aisec.cpg.serialization.NodeJSON
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ListCommandsTest {
    @BeforeEach
    fun setAnalysisResult() {
        val payload =
            CpgAnalyzePayload(
                content =
                    "class Foo:\n    secretKey = '0000'\ndef hello():\n    print('Hello World')",
                extension = "py",
            )
        runCpgAnalyze(payload, runPasses = true, cleanup = true)
    }

    @Test
    fun listFunctionsTest() =
        withClient(registerTools = { listFunctions() }) { client ->
            val result = client.callTool(name = "cpg_list_functions", arguments = emptyMap())

            assertNotNull(result, "Result should not be null")
            assertEquals(2, result.content.size, "Should return two function declarations")
            val functionNames =
                result.content.map {
                    assertIs<TextContent>(it)
                    Json.decodeFromString<FunctionInfo>(it.text).name
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
    fun listRecordsTest() =
        withClient(registerTools = { listRecords() }) { client ->
            val result = client.callTool(name = "cpg_list_records", arguments = emptyMap())
            assertNotNull(result)
            assertTrue(
                result.content.isNotEmpty(),
                "There is a record declaration \"Foo\" in the test code",
            )
            assertDoesNotThrow {
                Json.decodeFromString<RecordInfo>(
                    (result.content.singleOrNull() as? TextContent)?.text.orEmpty()
                )
            }
        }

    @Test
    fun listCallsTest() =
        withClient(registerTools = { listCalls() }) { client ->
            val result = client.callTool(name = "cpg_list_calls", arguments = emptyMap())
            assertNotNull(result)
            assertEquals(1, result.content.size, "Should return one call expression")
        }

    @Test
    fun listCallsToTest() =
        withClient(registerTools = { listCallsTo() }) { client ->
            val result =
                client.callTool(name = "cpg_list_calls_to", arguments = mapOf("name" to "print"))
            assertNotNull(result)
            assertTrue(result.content.isNotEmpty(), "Should return calls to 'print'")
        }

    @Test
    fun getAllArgsTest() =
        withClient(
            registerTools = {
                listCalls()
                getAllArgs()
            }
        ) { client ->
            val callsResult = client.callTool(name = "cpg_list_calls", arguments = emptyMap())
            val callId =
                Json.decodeFromString<CallInfo>((callsResult.content.first() as TextContent).text)
                    .nodeId

            val argsResult =
                client.callTool(name = "cpg_list_call_args", arguments = mapOf("id" to callId))
            assertNotNull(argsResult)
            assertTrue(argsResult.content.isNotEmpty(), "Should return arguments for the call")
            assertDoesNotThrow {
                Json.decodeFromString<NodeJSON>(
                    (argsResult.content.singleOrNull() as? TextContent)?.text.orEmpty()
                )
            }

            val wrongArgsResult =
                client.callTool(name = "cpg_list_call_args", arguments = mapOf("nodeId" to callId))
            assertNotNull(wrongArgsResult)
            assertTrue(wrongArgsResult.content.isNotEmpty(), "Should return arguments for the call")
            assertThrows<IllegalArgumentException> {
                Json.decodeFromString<NodeJSON>(
                    (wrongArgsResult.content.first() as TextContent).text
                )
            }
        }

    @Test
    fun getArgByIndexOrNameTest() =
        withClient(
            registerTools = {
                listCalls()
                getArgByIndexOrName()
            }
        ) { client ->
            val callsResult = client.callTool(name = "cpg_list_calls", arguments = emptyMap())
            val callId =
                Json.decodeFromString<CallInfo>((callsResult.content.first() as TextContent).text)
                    .nodeId

            val argResultByIndex =
                client.callTool(
                    name = "cpg_list_call_arg_by_name_or_index",
                    arguments = mapOf("nodeId" to callId, "index" to 0),
                )
            assertNotNull(argResultByIndex)
            assertTrue(
                argResultByIndex.content.isNotEmpty(),
                "Should return the argument at index 0",
            )
            assertDoesNotThrow {
                Json.decodeFromString<NodeJSON>(
                    (argResultByIndex.content.singleOrNull() as? TextContent)?.text.orEmpty()
                )
            }

            val argResultByName =
                client.callTool(
                    name = "cpg_list_call_arg_by_name_or_index",
                    arguments = mapOf("id" to callId),
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
    fun listAvailableConceptsTest() =
        withClient(registerTools = { listAvailableConcepts() }) { client ->
            val result =
                client.callTool(name = "cpg_list_available_concepts", arguments = emptyMap())
            assertNotNull(result)
            assertTrue(result.content.isNotEmpty(), "Should return available concepts")
        }

    @Test
    fun listAvailableOperationsTest() =
        withClient(registerTools = { listAvailableOperations() }) { client ->
            val result =
                client.callTool(name = "cpg_list_available_operations", arguments = emptyMap())
            assertNotNull(result)
            assertTrue(result.content.isNotEmpty(), "Should return available operations")
        }

    @Test
    fun listAvailableConceptsAndOperationsTest() =
        withClient(registerTools = { listConceptsAndOperations() }) { client ->
            val result =
                client.callTool(name = "cpg_list_concepts_and_operations", arguments = emptyMap())
            assertNotNull(result)
            assertTrue(
                result.content.isEmpty(),
                "We did not apply any concepts or operations, so it should be empty",
            )
        }

    @Test
    fun getNodeTest() =
        withClient(
            registerTools = {
                listFunctions()
                getNode()
            }
        ) { client ->
            val listResult = client.callTool(name = "cpg_list_functions", arguments = emptyMap())
            assertNotNull(listResult)
            assertTrue(listResult.content.isNotEmpty(), "Should have function declarations")

            val nodeJson =
                Json.decodeFromString<NodeJSON>((listResult.content.first() as TextContent).text)

            val result =
                client.callTool(name = "cpg_get_node", arguments = mapOf("id" to nodeJson.id))
            assertNotNull(result)
            assertTrue(result.content.isNotEmpty(), "Should return the node")

            val content = result.content.single()
            assertIs<TextContent>(content)
            assertDoesNotThrow { Json.decodeFromString<NodeJSON>(content.text) }
        }
}
