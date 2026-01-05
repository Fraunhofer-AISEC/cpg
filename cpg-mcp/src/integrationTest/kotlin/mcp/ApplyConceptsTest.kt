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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.literals
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addCpgApplyConceptsTool
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.listConceptsAndOperations
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.BeforeEach

class ApplyConceptsTest {
    private lateinit var server: Server

    @BeforeEach
    fun initializeServer() {
        val payload =
            CpgAnalyzePayload(
                content =
                    "class Foo:\n    secretKey = '0000'\ndef hello():\n    print('Hello World')",
                extension = "py",
            )
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
    fun applyConceptAndListAgain() = runTest {
        server.listConceptsAndOperations()
        server.addCpgApplyConceptsTool()
        val applyTool = server.tools["cpg_apply_concepts"] ?: error("Tool not registered")
        val secretInitializer = globalAnalysisResult?.literals?.singleOrNull { it.value == "0000" }
        assertNotNull(secretInitializer)

        val applyRequest =
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_apply_concepts",
                    arguments =
                        buildJsonObject {
                            putJsonArray("assignments") {
                                add(
                                    buildJsonObject {
                                        put("nodeId", secretInitializer.id.toString())
                                        put(
                                            "overlay",
                                            "de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Secret",
                                        )
                                        put("overlayType", "Concept")
                                    }
                                )
                            }
                        },
                )
            )
        val applyResult = applyTool.handler(applyRequest)
        assertNotNull(applyResult)
        assertTrue(applyResult.content.isNotEmpty(), "We did apply a concepts")
        assertTrue(
            "Applied 1 concept(s):" in
                (applyResult.content.singleOrNull() as? TextContent)?.text.orEmpty()
        )

        val tool = server.tools["cpg_list_concepts_and_operations"] ?: error("Tool not registered")
        val request =
            CallToolRequest(
                CallToolRequestParams(
                    name = "cpg_list_concepts_and_operations",
                    arguments = buildJsonObject {},
                )
            )
        val result = tool.handler(request)
        assertNotNull(result)
        assertTrue(result.content.isNotEmpty(), "We did apply a, so it should not be empty")
    }
}
