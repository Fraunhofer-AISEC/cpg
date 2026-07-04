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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.project.Project
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.io.TempDir

class CXXProjectDetectionTest {
    @Test
    fun testDetectCompilationDatabase(@TempDir tmp: Path) {
        val libFoo = tmp.resolve("src/libfoo")
        val tool = tmp.resolve("src/tool")
        libFoo.createDirectories()
        tool.createDirectories()
        libFoo.resolve("foo.c").writeText("int foo() { return 1; }")
        tool.resolve("main.c").writeText("int main() { return 0; }")

        tmp.resolve("compile_commands.json")
            .writeText(
                """
                [
                  {
                    "directory": "$libFoo",
                    "command": "gcc -DFOO=1 -c foo.c",
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

        val project =
            Project.from(tmp) {
                registerLanguage<CLanguage>()
                registerLanguage<CPPLanguage>()
            }

        // One component per compilation database component (derived from the src/ layout here)
        assertEquals(listOf("libfoo", "tool"), project.components.map { it.name }.sorted())

        // Even though both CLanguage and CPPLanguage share the detection logic, the result must
        // only appear once
        val result = project.detectionResults.singleOrNull()
        assertNotNull(result)
        assertEquals("compile_commands.json", result.detector)

        val db = project.config.compilationDatabase
        assertNotNull(db)
        assertEquals(2, db.size)
        assertEquals(mapOf("FOO" to "1"), db.getAllSymbols("libfoo").filterKeys { it == "FOO" })
    }
}
