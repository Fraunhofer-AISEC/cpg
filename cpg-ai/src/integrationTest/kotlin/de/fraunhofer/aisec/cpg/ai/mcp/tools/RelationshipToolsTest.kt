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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.describeRelationships
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.getRelatedNodes
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.listCalls
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.ai.mcp.utils.withClient
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.BeforeEach

/** Decodes a `cpg_list_calls`/`cpg_get_related_nodes` result item as a generic McpNodeView. */
private fun TextContent.toView(): JsonObject = Json.decodeFromString<JsonObject>(text)

private fun JsonObject.nodeId(): String = getValue("nodeId").jsonPrimitive.content

private fun JsonObject.name(): String = getValue("name").jsonPrimitive.content

class RelationshipToolsTest {
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
    fun describeRelationshipsListsArgumentAndInvoke() =
        withClient(
            registerTools = {
                listCalls()
                describeRelationships()
            }
        ) { client ->
            val callsResult = client.callTool(name = "cpg_list_calls", arguments = emptyMap())
            val printfCallId =
                callsResult.content
                    .map { (it as TextContent).toView() }
                    .single { it.name() == "printf" }
                    .nodeId()

            val result =
                client.callTool(
                    name = "cpg_describe_relationships",
                    arguments = mapOf("id" to printfCallId),
                )
            val names =
                Json.decodeFromString<List<String>>((result.content.single() as TextContent).text)

            assertTrue("argument" in names, "Should list the call's arguments as a relationship")
            assertTrue("invoke" in names, "Should list the call's invokes as a relationship")
        }

    @Test
    fun getRelatedNodesReturnsArguments() =
        withClient(
            registerTools = {
                listCalls()
                getRelatedNodes()
            }
        ) { client ->
            val callsResult = client.callTool(name = "cpg_list_calls", arguments = emptyMap())
            val printfCallId =
                callsResult.content
                    .map { (it as TextContent).toView() }
                    .single { it.name() == "printf" }
                    .nodeId()

            val result =
                client.callTool(
                    name = "cpg_get_related_nodes",
                    arguments = mapOf("nodeId" to printfCallId, "relationship" to "argument"),
                )
            assertTrue(result.content.isNotEmpty(), "printf(...) has one argument")
            val view = (result.content.single() as TextContent).toView()
            assertNotNull(view["nodeId"], "Should return an McpNodeView for the argument")
        }

    @Test
    fun getRelatedNodesResolvesInvokeToTheFunctionDeclaration() =
        withClient(
            registerTools = {
                listCalls()
                getRelatedNodes()
            }
        ) { client ->
            val callsResult = client.callTool(name = "cpg_list_calls", arguments = emptyMap())
            val helloCallId =
                callsResult.content
                    .map { (it as TextContent).toView() }
                    .single { it.name().endsWith("hello") }
                    .nodeId()

            val result =
                client.callTool(
                    name = "cpg_get_related_nodes",
                    arguments = mapOf("nodeId" to helloCallId, "relationship" to "invoke"),
                )
            assertTrue(result.content.isNotEmpty(), "hello() invokes the hello function")
            val view = (result.content.single() as TextContent).toView()
            assertTrue(
                view.name().endsWith("hello"),
                "Should resolve to the 'hello' function declaration",
            )
        }

    @Test
    fun getRelatedNodesReportsUnknownRelationship() =
        withClient(
            registerTools = {
                listCalls()
                getRelatedNodes()
            }
        ) { client ->
            val callsResult = client.callTool(name = "cpg_list_calls", arguments = emptyMap())
            val callId = (callsResult.content.first() as TextContent).toView().nodeId()

            val result =
                client.callTool(
                    name = "cpg_get_related_nodes",
                    arguments = mapOf("nodeId" to callId, "relationship" to "doesNotExist"),
                )
            val text = (result.content.single() as TextContent).text
            assertTrue(
                text.contains("Unknown relationship"),
                "Should report the unknown relationship",
            )
        }
}
