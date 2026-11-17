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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.project.Architecture
import de.fraunhofer.aisec.cpg.project.OperatingSystem
import de.fraunhofer.aisec.cpg.project.TargetEnvironment
import de.fraunhofer.aisec.cpg.project.ToolRunner
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class GoBuildDetectorTest {
    @Test
    fun testNoGoModReturnsNull(@TempDir tmp: Path) {
        tmp.resolve("main.go").writeText("package main")

        val result = GoBuildDetector().detect(tmp, TargetEnvironment.host())

        assertNull(result)
    }

    /** Records the commands and answers `go env GOROOT` and `go list`. */
    private class FakeGo(private val goRoot: Path, private val packages: List<String>?) :
        ToolRunner {
        val calls = mutableListOf<Triple<List<String>, Path?, Map<String, String>>>()

        override fun run(
            command: List<String>,
            directory: Path?,
            environment: Map<String, String>,
        ): List<String>? {
            calls += Triple(command, directory, environment)
            return when (command.take(2)) {
                listOf("go", "env") -> listOf(goRoot.toString())
                listOf("go", "list") -> packages
                else -> null
            }
        }
    }

    private fun Path.file(name: String, content: String = "package x\n") {
        val file = resolve(name)
        file.parent.createDirectories()
        file.writeText(content)
    }

    @Test
    fun testDependenciesAreResolvedWithTheGoTool(@TempDir tmp: Path, @TempDir goRoot: Path) {
        tmp.file("go.mod", "module example.com/app\n\ngo 1.22\n")
        tmp.file("internal/util/util.go")
        tmp.file("vendor/github.com/foo/bar/bar.go")
        goRoot.file("src/fmt/print.go")
        val go =
            FakeGo(goRoot, listOf("fmt", "example.com/app/internal/util", "github.com/foo/bar"))

        val result =
            assertNotNull(
                GoBuildDetector(runner = go)
                    .detect(tmp, TargetEnvironment(OperatingSystem.LINUX, Architecture.ARM64))
            )

        // The standard library, the module itself and its vendored dependencies are sources...
        val sources = result.components.single().sources.map { it.toRealPath().toString() }
        assertTrue(sources.any { it.endsWith("src/fmt/print.go") }, sources.toString())
        assertTrue(sources.any { it.endsWith("internal/util/util.go") }, sources.toString())
        assertTrue(
            sources.any { it.endsWith("vendor/github.com/foo/bar/bar.go") },
            sources.toString(),
        )
        // ... and the standard library and the vendor folder are include paths
        assertEquals(
            listOf(goRoot.resolve("src"), tmp.resolve("vendor")).map { it.toRealPath() },
            result.includePaths.map { it.toRealPath() },
        )

        // `go list` is run in the module for the target environment, not the host's
        val (list, directory, environment) =
            go.calls.single { it.first.take(2) == listOf("go", "list") }
        assertContains(list, "-deps")
        assertEquals(tmp.toRealPath(), directory?.toRealPath())
        assertEquals(mapOf("GOOS" to "linux", "GOARCH" to "arm64"), environment)
    }

    @Test
    fun testNoResultIfTheGoToolFails(@TempDir tmp: Path, @TempDir goRoot: Path) {
        tmp.file("go.mod", "module example.com/app\n")

        // `go list` fails
        assertNull(
            GoBuildDetector(runner = FakeGo(goRoot, null)).detect(tmp, TargetEnvironment.host())
        )
        // `go` is not available at all
        assertNull(
            GoBuildDetector(runner = { _, _, _ -> null }).detect(tmp, TargetEnvironment.host())
        )
    }

    @Test
    fun testPackagesOfOtherModulesWithTheSamePrefix(@TempDir tmp: Path, @TempDir goRoot: Path) {
        tmp.file("go.mod", "module example.com/app\n\ngo 1.22\n")
        tmp.file("main.go")
        tmp.file("vendor/example.com/app2/x/x.go")
        val go = FakeGo(goRoot, listOf("example.com/app", "example.com/app2/x"))

        val result =
            assertNotNull(GoBuildDetector(runner = go).detect(tmp, TargetEnvironment.host()))

        // `example.com/app2` is another module, which is vendored, and not part of this one
        val sources = result.components.single().sources.map { it.toRealPath().toString() }
        assertTrue(
            sources.any { it.endsWith("vendor/example.com/app2/x/x.go") },
            sources.toString(),
        )
        // The package in the root of the module is
        assertTrue(
            sources.any { it.endsWith("${tmp.toRealPath().fileName}/main.go") },
            sources.toString(),
        )
    }

    @Test
    fun testPackageInCmdIsOnlyASourceOnce(@TempDir tmp: Path, @TempDir goRoot: Path) {
        tmp.file("go.mod", "module example.com/app\n\ngo 1.22\n")
        tmp.file("cmd/tool/main.go")
        // `go list` reports the package, and the files in `cmd` are gathered anyway
        val go = FakeGo(goRoot, listOf("example.com/app/cmd/tool"))

        val result =
            assertNotNull(GoBuildDetector(runner = go).detect(tmp, TargetEnvironment.host()))

        val sources = result.components.single().sources.map { it.toRealPath() }
        assertEquals(sources.distinct(), sources)
        assertEquals(1, sources.count { it.endsWith("cmd/tool/main.go") })
    }
}
