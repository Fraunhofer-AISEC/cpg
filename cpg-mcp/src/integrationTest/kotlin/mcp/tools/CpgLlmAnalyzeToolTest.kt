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

import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addCpgLlmAnalyzeTool
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.utils.withClient
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach

class CpgLlmAnalyzeToolTest {
    @BeforeEach
    fun setAnalysisResult() {
        val payload =
            CpgAnalyzePayload(content = "def hello():\n    print('Hello World')", extension = "py")
        runCpgAnalyze(payload, runPasses = true, cleanup = true)
    }

    @Test
    fun cpgLlmAnalyzeNoPayload() =
        withClient(registerTools = { addCpgLlmAnalyzeTool() }) { client ->
            val result = client.callTool(name = "cpg_llm_analyze", arguments = emptyMap())

            val resultContent = result.content.firstOrNull()
            assertIs<TextContent>(resultContent)
            assertNotNull(resultContent.text, "Result content should not be null")
            assertFalse(
                "## Additional Context" in resultContent.text,
                "Result content should not contain the section 'Additional Context'",
            )
        }

    @Test
    fun cpgLlmAnalyzeWithPayload() =
        withClient(registerTools = { addCpgLlmAnalyzeTool() }) { client ->
            val result =
                client.callTool(
                    name = "cpg_llm_analyze",
                    arguments = mapOf("description" to "We have some additional context here."),
                )

            val resultContent = result.content.firstOrNull()
            assertIs<TextContent>(resultContent)
            assertNotNull(resultContent.text, "Result content should not be null")
            assertTrue(
                "## Additional Context" in resultContent.text,
                "Result content should contain the section 'Additional Context'",
            )
        }
}
