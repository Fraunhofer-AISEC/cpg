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

import de.fraunhofer.aisec.cpg.mcp.mcpserver.configureServer
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.ctx
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.CpgAnalyzePayload
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

class CpgAnalyzeToolTest {

    private lateinit var server: Server

    @Test
    fun testConfigureServer() {
        val testServer = configureServer()
        assertEquals(
            setOf(
                "cpg_translate",
                "cpg_list_passes",
                "cpg_run_pass",
                "cpg_analyze",
                "cpg_llm_analyze",
                "cpg_apply_concepts",
                "cpg_dataflow",
                "cpg_list_functions",
                "cpg_list_records",
                "cpg_list_concepts_and_operations",
                "cpg_list_calls",
                "cpg_list_calls_to",
                "cpg_list_call_args",
                "cpg_list_call_arg_by_name_or_index",
                "cpg_list_available_concepts",
                "cpg_list_available_operations",
            ),
            testServer.tools.keys,
        )
    }

    @Test
    fun testReanalyze() {
        // Build a small CPG without passes
        runCpgAnalyze(
            CpgAnalyzePayload("def hello():\n    print('X')", "py"),
            runPasses = false,
            cleanup = true,
        )
        val oldGlobalAnalysisResult = globalAnalysisResult
        val oldCtx = ctx
        assertNotNull(oldGlobalAnalysisResult)
        assertNotNull(oldCtx)

        // Bild the CPG again but we expect a new ctx and globalAnalysisResult
        runCpgAnalyze(
            CpgAnalyzePayload("def hello():\n    print('X')", "py"),
            runPasses = false,
            cleanup = true,
        )
        assertNotSame(oldCtx, ctx)
        assertNotSame(oldGlobalAnalysisResult, globalAnalysisResult)
    }
}
