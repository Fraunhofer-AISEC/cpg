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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.addOverviewTool
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgOverview
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.analysisSessions
import de.fraunhofer.aisec.cpg.ai.mcp.utils.withClient
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach

class OverviewToolTest {
    @BeforeEach
    fun setAnalysisResult() {
        // The sessions are global, so drop the ones previous tests left behind
        analysisSessions.clear()
        val payload =
            CpgAnalyzePayload(
                content =
                    "class Foo:\n    secretKey = '0000'\ndef hello():\n    print('Hello World')",
                extension = "py",
            )
        runCpgAnalyze(payload, runPasses = true, cleanup = true)
    }

    @Test
    fun overviewTest() =
        withClient(registerTools = { addOverviewTool() }) { client ->
            val result = client.callTool(name = "cpg_overview", arguments = emptyMap())
            val overview =
                Json.decodeFromString<CpgOverview>((result.content.single() as TextContent).text)

            assertTrue(
                overview.externalFunctionsCalled.any { it.name == "print" },
                "print is called, but not defined in the code: $overview",
            )
            assertTrue(
                overview.entryPointCandidates.any { it.name.endsWith("hello") },
                "hello is defined in the code and never called: $overview",
            )
        }
}
