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
package de.fraunhofer.aisec.cpg.mcp

import de.fraunhofer.aisec.cpg.graph.assigns
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.returns
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.getLastWrite
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgIdPayload
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CpgGetLastWriteToolTest {

    @Test
    fun returnsBothBranchWritesAtMergePoint() {
        val source =
            """
            def foo(x):
                if x:
                    y = 1
                else:
                    y = 2
                return y
            """
                .trimIndent()

        runCpgAnalyze(CpgAnalyzePayload(source, "py"), runPasses = true, cleanup = true)
        val analysisResult = globalAnalysisResult
        assertNotNull(analysisResult)

        val foo = analysisResult.functions["foo"]
        assertNotNull(foo)
        val readRef = foo.returns.single().returnValue
        assertNotNull(readRef)

        // Both branch writes reach the merge point, so both of
        // their write-node IDs should show up in the result
        val writeIds = foo.assigns.map { it.lhs.single().id.toString() }
        assertEquals(2, writeIds.size)

        val result = getLastWrite(analysisResult, CpgIdPayload(id = readRef.id.toString()))
        val text = (result.content.single() as TextContent).text
        assertNotNull(text)
        writeIds.forEach { writeId -> assertTrue(text.contains(writeId)) }
    }

    @Test
    fun returnsEmptyListForALiteral() {
        val source = "def foo():\n    return 1"

        runCpgAnalyze(CpgAnalyzePayload(source, "py"), runPasses = true, cleanup = true)
        val analysisResult = globalAnalysisResult
        assertNotNull(analysisResult)

        val foo = analysisResult.functions["foo"]
        assertNotNull(foo)
        val readRef = foo.returns.single().returnValue
        assertNotNull(readRef)

        val result = getLastWrite(analysisResult, CpgIdPayload(id = readRef.id.toString()))
        val text = (result.content.single() as TextContent).text
        assertEquals("[]", text)
    }
}
