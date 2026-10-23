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
import de.fraunhofer.aisec.cpg.project.Project
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class GoProjectDetectionTest {
    @Test
    fun testDetectModules(@TempDir tmp: Path) {
        tmp.resolve("go.mod").writeText("module example.com/app // main module\n\ngo 1.22\n")
        tmp.resolve("main.go").writeText("package main")

        val worker = tmp.resolve("services/worker")
        worker.createDirectories()
        worker.resolve("go.mod").writeText("module example.com/worker\n")
        worker.resolve("main.go").writeText("package main")

        val project =
            Project.from(tmp) {
                registerLanguage<GoLanguage>()
                environment {
                    os = OperatingSystem.LINUX
                    architecture = Architecture.ARM64
                }
            }

        // One component per detected Go module, named after the module path
        assertEquals(listOf("app", "worker"), project.components.map { it.name }.sorted())
        assertEquals(tmp.toFile(), project.config.topLevels["app"])
        assertEquals(worker.toFile(), project.config.topLevels["worker"])

        // GOOS/GOARCH are derived from the target environment, not the host
        assertEquals("linux", project.config.symbols["GOOS"])
        assertEquals("arm64", project.config.symbols["GOARCH"])

        val result = project.detectionResults.singleOrNull()
        assertNotNull(result)
        assertEquals("go", result.detector)
    }

    @Test
    fun testNoGoProject(@TempDir tmp: Path) {
        tmp.resolve("README.md").writeText("nothing to see here")

        val project = Project.from(tmp) { registerLanguage<GoLanguage>() }

        assertTrue(project.detectionResults.isEmpty())
        assertEquals(listOf("application"), project.components.map { it.name })
    }

    @Test
    fun testGoWorkspaceWithoutModuleIsDetected(@TempDir tmp: Path) {
        tmp.resolve("go.work").writeText("go 1.22\n")

        val project = Project.from(tmp) { registerLanguage<GoLanguage>() }

        val result = project.detectionResults.singleOrNull()
        assertNotNull(result)
        assertEquals("go", result.detector)
        assertTrue(result.components.isEmpty())
    }

    @Test
    fun testBareGoFilesWithoutModuleAreDetected(@TempDir tmp: Path) {
        tmp.resolve("main.go").writeText("package main")

        val project = Project.from(tmp) { registerLanguage<GoLanguage>() }

        val result = project.detectionResults.singleOrNull()
        assertNotNull(result)
        assertTrue(result.components.isEmpty())
    }

    @Test
    fun testUnparseableGoModFallsBackToDirectoryName(@TempDir tmp: Path) {
        val module = tmp.resolve("services/worker")
        module.createDirectories()
        // No "module " directive, so parseGoMod() cannot determine a module path.
        module.resolve("go.mod").writeText("go 1.22\n")

        val project = Project.from(tmp) { registerLanguage<GoLanguage>() }

        assertEquals(listOf("worker"), project.components.map { it.name })
    }

    @Test
    fun testUnknownOsAndArchitectureYieldNoSymbols(@TempDir tmp: Path) {
        tmp.resolve("go.mod").writeText("module example.com/app\n")

        val project =
            Project.from(tmp) {
                registerLanguage<GoLanguage>()
                environment {
                    os = OperatingSystem.UNKNOWN
                    architecture = Architecture.UNKNOWN
                }
            }

        assertTrue("GOOS" !in project.config.symbols)
        assertTrue("GOARCH" !in project.config.symbols)
    }

    @Test
    fun testGoosMapping() {
        assertEquals("linux", OperatingSystem.LINUX.goos)
        assertEquals("darwin", OperatingSystem.MACOS.goos)
        assertEquals("windows", OperatingSystem.WINDOWS.goos)
        assertEquals("freebsd", OperatingSystem.FREEBSD.goos)
        assertEquals(null, OperatingSystem.UNKNOWN.goos)
    }

    @Test
    fun testGoarchMapping() {
        assertEquals("amd64", Architecture.X86_64.goarch)
        assertEquals("arm64", Architecture.ARM64.goarch)
        assertEquals("386", Architecture.X86.goarch)
        assertEquals("arm", Architecture.ARM.goarch)
        assertEquals("riscv64", Architecture.RISCV64.goarch)
        assertEquals(null, Architecture.UNKNOWN.goarch)
    }
}
