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
package de.fraunhofer.aisec.cpg.mcp.tools

import de.fraunhofer.aisec.cpg.mcp.utils.McpTestSetup

class CpgAnalyzeToolTest : McpTestSetup() {

    //    @Test
    //    fun cpgAnalyzeToolIntegrationTest() = runTest {
    //        val result =
    //            client.callTool(
    //                CallToolRequest(
    //                    CallToolRequestParams(
    //                        name = "cpg_analyze",
    //                        arguments =
    //                            buildJsonObject {
    //                                put("content", "def hello():\n    print('Hello World')")
    //                                put("extension", "py")
    //                            },
    //                    )
    //                )
    //            )
    //
    //        assertNotNull(globalAnalysisResult, "Result should be set after tool execution")
    //
    //        val resultContent = result.content.firstOrNull()
    //        assertIs<TextContent>(resultContent)
    //        val resultText = resultContent.text
    //        assertNotNull(resultText, "Result content should not be null")
    //
    //        val analysisResult = Json.decodeFromString<CpgAnalysisResult>(resultText)
    //
    //        assertEquals(2, analysisResult.functions)
    //        assertEquals(1, analysisResult.callExpressions)
    //        assertNotNull(analysisResult.functions)
    //    }
    //
    //    @Test
    //    fun cpgAnalyzeToolUnitTest() {
    //        val payload =
    //            CpgAnalyzePayload(content = "def hello():\n    print('Hello World')", extension =
    // "py")
    //        val analysisResult = runCpgAnalyze(payload, runPasses = true, cleanup = true)
    //        assertNotNull(globalAnalysisResult, "Result should be set after tool execution")
    //
    //        assertEquals(2, analysisResult.functions)
    //        assertEquals(1, analysisResult.callExpressions)
    //        assertNotNull(analysisResult.functionSummaries)
    //    }
}
