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
package de.fraunhofer.aisec.cpg.project

import de.fraunhofer.aisec.cpg.TranslationResult.Companion.DEFAULT_APPLICATION_NAME
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/** A [TestLanguage] that detects projects marked by a `test.mod` file. */
class DetectingTestLanguage : TestLanguage(), ComponentDetector, ProjectDetector {
    override fun detectComponents(
        directory: Path,
        environment: TargetEnvironment,
    ): List<ComponentDefinition> {
        if (!directory.resolve("test.mod").exists()) {
            return listOf()
        }

        return listOf(ComponentDefinition("module", root = directory))
    }

    override fun detect(directory: Path, environment: TargetEnvironment): DetectionResult? {
        if (!directory.resolve("test.mod").exists()) {
            return null
        }

        return DetectionResult(
            detector = "test.mod",
            symbols = mapOf("TEST_OS" to environment.os.name.lowercase()),
            notes = listOf("found test.mod"),
        )
    }
}

class ProjectTest {
    @Test
    fun testSingleFile(@TempDir tmp: Path) {
        val file = tmp.resolve("main.test")
        file.writeText("")

        val project = Project.from(file) { registerLanguage<TestLanguage>() }

        assertNull(project.directory, "a single-file project should be an ad-hoc project")
        assertEquals(listOf(file.toFile()), project.config.sourceLocations)
        assertEquals(tmp.toFile(), project.config.topLevels[DEFAULT_APPLICATION_NAME])
        assertTrue(project.config.registeredPasses.isNotEmpty())
    }

    @Test
    fun testDirectory(@TempDir tmp: Path) {
        tmp.resolve("main.test").writeText("")

        val project =
            project(tmp) {
                registerLanguage<TestLanguage>()
                exclude("tests")
                environment {
                    os = OperatingSystem.LINUX
                    architecture = Architecture.ARM64
                }
            }

        assertEquals(tmp, project.directory)

        val component = project.components.singleOrNull()
        assertNotNull(component)
        assertEquals(DEFAULT_APPLICATION_NAME, component.name)
        assertEquals(tmp, component.root)

        assertContains(project.config.exclusionPatternsByString, "tests")
        assertEquals(OperatingSystem.LINUX, project.config.targetEnvironment.os)
        assertEquals(64, project.config.targetEnvironment.architecture.bits)
    }

    @Test
    fun testDetection(@TempDir tmp: Path) {
        tmp.resolve("test.mod").writeText("module test")
        tmp.resolve("main.test").writeText("")

        val project =
            Project.from(tmp) {
                registerLanguage<DetectingTestLanguage>()
                environment { os = OperatingSystem.LINUX }
            }

        val result = project.detectionResults.singleOrNull()
        assertNotNull(result)
        assertEquals("test.mod", result.detector)

        val component = project.components.singleOrNull()
        assertNotNull(component)
        assertEquals("module", component.name)
        assertEquals("linux", project.config.symbols["TEST_OS"])
    }

    @Test
    fun testUserConfigurationWinsOverDetection(@TempDir tmp: Path) {
        tmp.resolve("test.mod").writeText("module test")

        val project =
            Project.from(tmp) {
                // Explicit components disable auto-detection entirely.
                languages { use<DetectingTestLanguage>() }
                components { component("backend", root = tmp) }
            }

        assertEquals(listOf("backend"), project.components.map { it.name })
        assertTrue(project.detectionResults.isEmpty())
    }

    @Test
    fun testDetectionRunsAlongsideExplicitComponents(@TempDir tmp: Path) {
        tmp.resolve("test.mod").writeText("module test")

        val project =
            Project.from(tmp) {
                // default() keeps auto-detection running even with an explicit component.
                languages { use<DetectingTestLanguage>() }
                components {
                    default()
                    component("backend", root = tmp)
                }
            }

        // Detection results are still recorded when default() is used.
        assertEquals(1, project.detectionResults.size)
        // Explicit component wins over the detected one.
        assertEquals(listOf("backend"), project.components.map { it.name })
    }

    @Test
    fun testDirectoryComponentDetector(@TempDir tmp: Path) {
        tmp.resolve("components/backend").createDirectories()
        tmp.resolve("components/frontend").createDirectories()
        tmp.resolve("components/backend/main.test").writeText("")

        val project =
            Project.from(tmp) {
                registerLanguage<TestLanguage>()
                detector(DirectoryComponentDetector())
            }

        assertEquals(listOf("backend", "frontend"), project.components.map { it.name }.sorted())
        assertEquals(
            tmp.resolve("components/backend").toFile(),
            project.config.topLevels["backend"],
        )
    }

    @Test
    fun testLanguageAutoDetection(@TempDir tmp: Path) {
        // FooLanguage handles .foo, BarLanguage handles .bar. Only .foo files exist.
        class FooLanguage : TestLanguage() {
            override val fileExtensions = listOf("foo")
        }
        class BarLanguage : TestLanguage() {
            override val fileExtensions = listOf("bar")
        }

        tmp.resolve("main.foo").writeText("")

        // Auto-mode: no languages {} block. The builder is given a custom available set so the
        // test does not depend on what language modules are on the classpath.
        val project =
            ProjectBuilder(tmp)
                .apply { defaultLanguagesOverride = setOf(FooLanguage::class, BarLanguage::class) }
                .resolve()

        // Only FooLanguage should be registered because no .bar file exists.
        assertEquals(setOf(FooLanguage::class), project.config.languages)
    }

    @Test
    fun testLanguageAutoDetectionAlwaysIncludesExtensionlessLanguages(@TempDir tmp: Path) {
        // A language with no declared extensions is always included regardless of what's in the
        // dir.
        class NoExtLanguage : TestLanguage() {
            override val fileExtensions = listOf<String>()
        }
        class FooLanguage : TestLanguage() {
            override val fileExtensions = listOf("foo")
        }

        tmp.resolve("unrelated.xyz").writeText("")

        val project =
            ProjectBuilder(tmp)
                .apply {
                    defaultLanguagesOverride = setOf(NoExtLanguage::class, FooLanguage::class)
                }
                .resolve()

        // NoExtLanguage is always active; FooLanguage is not because no .foo file exists.
        assertEquals(setOf(NoExtLanguage::class), project.config.languages)
    }

    @Test
    fun testAutoDetectCanBeDisabled(@TempDir tmp: Path) {
        tmp.resolve("test.mod").writeText("module test")

        val project =
            Project.from(tmp) {
                // An explicit empty components block disables auto-detection.
                languages { use<DetectingTestLanguage>() }
                components {}
            }

        assertTrue(project.detectionResults.isEmpty())
        assertEquals(listOf(DEFAULT_APPLICATION_NAME), project.components.map { it.name })
    }
}
