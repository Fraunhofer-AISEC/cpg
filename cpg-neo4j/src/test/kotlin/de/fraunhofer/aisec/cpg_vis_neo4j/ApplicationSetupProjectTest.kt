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
package de.fraunhofer.aisec.cpg_vis_neo4j

import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir
import picocli.CommandLine

private fun application(vararg args: String): Application {
    val cmd = CommandLine(Application::class.java)
    cmd.parseArgs(*args)
    return cmd.getCommand<Application>()
}

class ApplicationSetupProjectTest {
    @Test
    fun testSingleFile(@TempDir tmp: Path) {
        val file = tmp.resolve("main.c")
        file.writeText("int main() { return 0; }")

        val project = application(file.toString()).setupProject()

        val component = project.components.singleOrNull()
        assertNotNull(component)
        assertEquals("application", component.name)
        // A single file's top level defaults to its own parent directory.
        assertEquals(tmp, component.root)
    }

    @Test
    fun testSingleDirectoryAutoDetects(@TempDir tmp: Path) {
        tmp.resolve("main.c").writeText("int main() { return 0; }")

        val project = application(tmp.toString()).setupProject()

        // No --top-level, no --softwareComponents, no compilation database: a bare directory
        // goes through Project's normal auto-detection instead of a forced flat component.
        val component = project.components.singleOrNull()
        assertNotNull(component)
        assertEquals(tmp, component.root)
    }

    @Test
    fun testTopLevelOverridesRoot(@TempDir tmp: Path) {
        val src = tmp.resolve("src")
        src.createDirectories()
        val file = src.resolve("main.c")
        file.writeText("int main() { return 0; }")

        val project = application("--top-level", tmp.toString(), file.toString()).setupProject()

        val component = project.components.singleOrNull()
        assertNotNull(component)
        assertEquals(tmp, component.root)
    }

    @Test
    fun testSoftwareComponents(@TempDir tmp: Path) {
        val libFoo = tmp.resolve("libfoo")
        val tool = tmp.resolve("tool")
        libFoo.createDirectories()
        tool.createDirectories()
        val foo = libFoo.resolve("foo.c").apply { writeText("int foo() { return 1; }") }
        val main = tool.resolve("main.c").apply { writeText("int main() { return 0; }") }

        val project =
            application("--softwareComponents", "libfoo=$foo", "--softwareComponents", "tool=$main")
                .setupProject()

        assertEquals(listOf("libfoo", "tool"), project.components.map { it.name }.sorted())
        assertEquals(libFoo, project.components.single { it.name == "libfoo" }.root)
        assertEquals(tool, project.components.single { it.name == "tool" }.root)
    }

    @Test
    fun testJsonCompilationDatabase(@TempDir tmp: Path) {
        val foo = tmp.resolve("foo.c")
        foo.writeText("int foo() { return 1; }")

        val cc = tmp.resolve("compile_commands.json")
        cc.writeText(
            """
            [
              {
                "directory": "$tmp",
                "command": "gcc -c foo.c",
                "file": "$foo",
                "output": "foo.o"
              }
            ]
            """
                .trimIndent()
        )

        val project = application("--json-compilation-database", cc.toString()).setupProject()

        val component = project.components.singleOrNull()
        assertNotNull(component)
        assertNotNull(project.config.compilationDatabase)
    }

    @Test
    fun testNoDefaultPassesSkipsExtraPasses(@TempDir tmp: Path) {
        val file = tmp.resolve("main.c")
        file.writeText("int main() { return 0; }")

        val project = application("--no-default-passes", file.toString()).setupProject()

        assertTrue(ControlDependenceGraphPass::class !in project.config.registeredPasses.flatten())
    }

    @Test
    fun testInferNodesSetsInferenceConfiguration(@TempDir tmp: Path) {
        val file = tmp.resolve("main.c")
        file.writeText("int main() { return 0; }")

        val withFlag = application("--infer-nodes", file.toString()).setupProject()
        assertTrue(withFlag.config.inferenceConfiguration.inferRecords)
    }

    @Test
    fun testNoFilesFailsFast() {
        // Bypass picocli's own ArgGroup validation (which would normally reject this) to make sure
        // setupProject() itself fails fast with a clear error instead of an internal
        // NoSuchElementException.
        val application = Application()
        application.mutuallyExclusiveParameters = Application.Exclusive()

        assertFailsWith<IllegalArgumentException> { application.setupProject() }
    }

    @Test
    fun testEmptyJsonCompilationDatabaseFailsFast(@TempDir tmp: Path) {
        val cc = tmp.resolve("compile_commands.json")
        cc.writeText("[]")

        assertFailsWith<IllegalArgumentException> {
            application("--json-compilation-database", cc.toString()).setupProject()
        }
    }
}
