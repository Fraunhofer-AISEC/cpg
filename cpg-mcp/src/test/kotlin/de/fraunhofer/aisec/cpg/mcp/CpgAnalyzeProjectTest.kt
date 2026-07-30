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

import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir

class CpgAnalyzeProjectTest {

    @Test
    fun testAnalyzeProjectWithCompilationDatabase(@TempDir tmp: Path) {
        assumeTrue(
            runCatching { Class.forName("de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage") }
                .isSuccess,
            "The C/C++ frontend is not on the classpath",
        )

        val libFoo = tmp.resolve("src/libfoo")
        val tool = tmp.resolve("src/tool")
        libFoo.createDirectories()
        tool.createDirectories()
        libFoo.resolve("foo.c").writeText("int foo() { return 1; }")
        tool.resolve("main.c").writeText("int main() { return foo(); }")

        tmp.resolve("compile_commands.json")
            .writeText(
                """
                [
                  {
                    "directory": "$libFoo",
                    "command": "gcc -c foo.c",
                    "file": "$libFoo/foo.c",
                    "output": "foo.o"
                  },
                  {
                    "directory": "$tool",
                    "command": "gcc -c main.c",
                    "file": "$tool/main.c",
                    "output": "main.o"
                  }
                ]
                """
                    .trimIndent()
            )

        val result =
            runCpgAnalyze(
                CpgAnalyzePayload(path = tmp.toString()),
                runPasses = true,
                cleanup = true,
            )

        // The components are derived from the compilation database
        assertEquals(listOf("libfoo", "tool"), result.components.sorted())
        assertTrue(
            result.detectionNotes.any { it.startsWith("compile_commands.json:") },
            "expected a detection note about the compilation database, but got ${result.detectionNotes}",
        )
        assertEquals(2, result.functions)
        assertContains(1..Int.MAX_VALUE, result.callExpressions)
    }

    @Test
    fun testAnalyzeInvalidPath() {
        assertFailsWith<IllegalArgumentException> {
            runCpgAnalyze(
                CpgAnalyzePayload(path = "/does/not/exist"),
                runPasses = false,
                cleanup = true,
            )
        }
    }
}
