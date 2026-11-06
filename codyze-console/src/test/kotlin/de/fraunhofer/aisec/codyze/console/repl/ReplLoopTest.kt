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
package de.fraunhofer.aisec.codyze.console.repl

import de.fraunhofer.aisec.codyze.console.ConsoleService
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertIs
import org.junit.jupiter.api.io.TempDir

class ReplLoopTest {

    /** Runs a meta-command like the REPL does and returns what it printed. */
    private fun ReplLoop.meta(input: String): String {
        val output = StringWriter()
        PrintWriter(output).use { handleMeta(input, it) }
        return output.toString()
    }

    @Test
    fun testReloadMissingDirectory(@TempDir project: Path) {
        val consoleService = ConsoleService()
        val replService = ReplService(consoleService)
        val loop = ReplLoop(consoleService, replService)
        loop.meta(":reload $project")
        assertIs<ReplEvalResult.UnitResult>(replService.eval("val kept = 1"))

        assertContains(loop.meta(":reload /does/not/exist"), "is not a directory")

        // The current session is kept, so earlier declarations still resolve.
        assertIs<ReplEvalResult.Value>(replService.eval("kept"))
    }

    @Test
    fun testReloadResetsSession(@TempDir project: Path) {
        val consoleService = ConsoleService()
        val replService = ReplService(consoleService)
        val loop = ReplLoop(consoleService, replService)
        loop.meta(":reload $project")
        assertIs<ReplEvalResult.UnitResult>(replService.eval("val dropped = 1"))

        assertContains(loop.meta(":reload"), "Analysis complete.")

        assertIs<ReplEvalResult.CompileError>(replService.eval("dropped"))
    }

    @Test
    fun testReloadWithoutPreviousAnalysis() {
        val loop = ReplLoop(ConsoleService())
        assertContains(loop.meta(":reload"), "No previous analysis to reload")
    }
}
