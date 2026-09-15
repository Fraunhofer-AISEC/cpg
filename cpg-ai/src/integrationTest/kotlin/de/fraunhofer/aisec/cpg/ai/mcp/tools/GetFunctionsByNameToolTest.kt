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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.getFunctionsByName
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.FunctionInfo
import de.fraunhofer.aisec.cpg.ai.mcp.utils.withClient
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach

/**
 * Covers the [de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.getFunctionsByName] `includeCode`
 * option - unlike the bulk-listing tools, this one is meant to return full details (including code)
 * by default since it's used for a small, already-known batch of names, but callers that only need
 * to confirm existence/signature should be able to opt out.
 */
class GetFunctionsByNameToolTest {
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
    fun includesCodeByDefault() =
        withClient(registerTools = { getFunctionsByName() }) { client ->
            val result =
                client.callTool(
                    name = "cpg_get_functions_by_name",
                    arguments = mapOf("names" to listOf("hello")),
                )
            assertTrue(result.content.isNotEmpty())
            result.content.forEach {
                assertIs<TextContent>(it)
                val info = Json.decodeFromString<FunctionInfo>(it.text)
                assertNotNull(info.code, "cpg_get_functions_by_name should include code by default")
            }
        }

    @Test
    fun omitsCodeWhenRequested() =
        withClient(registerTools = { getFunctionsByName() }) { client ->
            val result =
                client.callTool(
                    name = "cpg_get_functions_by_name",
                    arguments = mapOf("names" to listOf("hello"), "includeCode" to false),
                )
            assertTrue(result.content.isNotEmpty())
            result.content.forEach {
                assertIs<TextContent>(it)
                val info = Json.decodeFromString<FunctionInfo>(it.text)
                assertNull(info.code, "cpg_get_functions_by_name should omit code when requested")
            }
        }
}
