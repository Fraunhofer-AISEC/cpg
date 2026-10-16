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
package de.fraunhofer.aisec.cpg.ai.mcp

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.getSession
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import org.junit.jupiter.api.io.TempDir

class CpgAnalyzeToolTest {

    @Test
    fun testReanalyze(@TempDir tempDir: Path) {
        val project = tempDir.resolve("hello").createDirectory()
        project.resolve("main.py").writeText("def hello():\n    print('X')")
        val payload = CpgAnalyzePayload(path = project.toString())

        // Build a small CPG without passes
        val first = runCpgAnalyze(payload, runPasses = false, cleanup = true)
        val oldSession = getSession(first.projectName)
        assertNotNull(oldSession)

        // Build the CPG of the same project again; we expect its session to be replaced by one
        // with a new result and a new context
        val second = runCpgAnalyze(payload, runPasses = false, cleanup = true)
        assertEquals(first.projectName, second.projectName)
        val newSession = getSession(second.projectName)
        assertNotNull(newSession)
        assertNotSame(oldSession.translationResult, newSession.translationResult)
        assertNotSame(oldSession.translationContext, newSession.translationContext)
    }
}
