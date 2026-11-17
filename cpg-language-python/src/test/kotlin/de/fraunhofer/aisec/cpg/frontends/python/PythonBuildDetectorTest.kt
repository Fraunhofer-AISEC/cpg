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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.project.Architecture
import de.fraunhofer.aisec.cpg.project.OperatingSystem
import de.fraunhofer.aisec.cpg.project.TargetEnvironment
import de.fraunhofer.aisec.cpg.project.ToolRunner
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class PythonBuildDetectorTest {
    private val environment = TargetEnvironment.host()

    private fun Path.file(name: String, content: String = ""): Path {
        val file = resolve(name)
        file.parent.createDirectories()
        file.writeText(content)
        return file
    }

    @Test
    fun testNotAPythonProject(@TempDir tmp: Path) {
        tmp.file("main.py")

        assertNull(PythonBuildDetector(install = false).detect(tmp, environment))
    }

    @Test
    fun testProjectWithoutEnvironment(@TempDir tmp: Path) {
        tmp.file("pyproject.toml")
        val main = tmp.file("main.py")

        val result = assertNotNull(PythonBuildDetector(install = false).detect(tmp, environment))

        assertEquals(listOf(), result.includePaths)
        assertFalse(result.loadIncludes)
        assertContentEquals(
            listOf(main.toRealPath()),
            result.components.single().sources.map { it.toRealPath() },
        )
    }

    @Test
    fun testVirtualEnvironment(@TempDir tmp: Path) {
        tmp.file("requirements.txt")
        val main = tmp.file("app/main.py")
        tmp.file("tests/__pycache__/cached.py")
        tmp.file(".venv/pyvenv.cfg")
        tmp.file(".venv/lib/python3.12/site-packages/cryptography/__init__.py")
        tmp.file(".venv/lib/python3.12/os.py")

        val result = assertNotNull(PythonBuildDetector(install = false).detect(tmp, environment))

        assertEquals(
            listOf(tmp.resolve(".venv/lib/python3.12/site-packages").toRealPath()),
            result.includePaths.map { it.toRealPath() },
        )
        assertTrue(result.loadIncludes)
        val component = result.components.single()
        assertEquals(tmp.name, component.name)
        // Neither the environment nor caches are part of the project's sources
        assertContentEquals(listOf(main.toRealPath()), component.sources.map { it.toRealPath() })
    }

    @Test
    fun testPipTargetFolder(@TempDir tmp: Path) {
        tmp.file("pyproject.toml")
        val main = tmp.file("main.py")
        tmp.file("vendor/nacl-1.5.0.dist-info/METADATA")
        tmp.file("vendor/nacl/__init__.py")

        val result = assertNotNull(PythonBuildDetector(install = false).detect(tmp, environment))

        assertEquals(
            listOf(tmp.resolve("vendor").toRealPath()),
            result.includePaths.map { it.toRealPath() },
        )
        assertContentEquals(
            listOf(main.toRealPath()),
            result.components.single().sources.map { it.toRealPath() },
        )
    }

    @Test
    fun testExplicitEnvironment(@TempDir tmp: Path) {
        val main = tmp.file("main.py")
        tmp.file("other-env/Lib/site-packages/paramiko/__init__.py")

        val result =
            assertNotNull(
                PythonBuildDetector(Path.of("other-env"), install = false).detect(tmp, environment)
            )

        assertEquals(
            listOf(tmp.resolve("other-env/Lib/site-packages").toRealPath()),
            result.includePaths.map { it.toRealPath() },
        )
        // The explicit environment is not part of the sources, even without `pyvenv.cfg`
        assertContentEquals(
            listOf(main.toRealPath()),
            result.components.single().sources.map { it.toRealPath() },
        )
    }

    /**
     * A [ToolRunner] that does not run anything but records the commands. It reports that the tools
     * in [available] exist, and "installs" packages by creating a `dist-info` folder in the
     * `--target` of an install command.
     */
    private class FakeRunner(
        private val available: Set<String>,
        private val installSucceeds: Boolean = true,
    ) : ToolRunner {
        val commands = mutableListOf<List<String>>()

        override fun run(
            command: List<String>,
            directory: Path?,
            environment: Map<String, String>,
        ): List<String>? {
            commands += command
            if ("--version" in command) {
                val exists = command.first() in available || command.getOrNull(2) in available
                return if (exists) listOf() else null
            }
            if (command.contains("export")) {
                Path.of(command[command.indexOf("--output-file") + 1]).file("", "requirements")
                return listOf()
            }
            if (command.contains("install")) {
                if (!installSucceeds) return null
                // "Installs" the package that is required
                val required = Path.of(command[command.indexOf("-r") + 1]).readText().trim()
                Path.of(command[command.indexOf("--target") + 1])
                    .file("${required.substringBefore("=")}.dist-info/METADATA")
            }
            return listOf()
        }

        private fun Path.file(name: String, content: String = ""): Path {
            val file = if (name.isEmpty()) this else resolve(name)
            file.parent.createDirectories()
            file.writeText(content)
            return file
        }
    }

    @Test
    fun testInstallWithPip(@TempDir tmp: Path, @TempDir target: Path) {
        tmp.file("requirements.txt", "pynacl==1.5.0 --hash=sha256:abc\n")
        val runner = FakeRunner(available = setOf("pip"))

        val result =
            assertNotNull(
                PythonBuildDetector(installDirectory = target, runner = runner)
                    .detect(tmp, environment)
            )

        assertEquals(listOf(target.toRealPath()), result.includePaths.map { it.toRealPath() })
        val command = runner.commands.last()
        assertEquals(listOf("python3", "-m", "pip", "install"), command.take(4))
        // Only wheels are installed (no code of a package is ever built) and hashes are verified
        assertContains(command, "--only-binary")
        assertContains(command, ":all:")
        assertContains(command, "--require-hashes")
        assertContains(command, "--no-deps")
    }

    @Test
    fun testInstallWithUvFromLockfile(@TempDir tmp: Path, @TempDir target: Path) {
        tmp.file("uv.lock")
        val runner = FakeRunner(available = setOf("uv"))
        val linux = TargetEnvironment(OperatingSystem.LINUX, Architecture.ARM64)

        val result =
            assertNotNull(
                PythonBuildDetector(installDirectory = target, runner = runner).detect(tmp, linux)
            )

        assertEquals(listOf(target.toRealPath()), result.includePaths.map { it.toRealPath() })
        assertTrue(runner.commands.any { it.take(2) == listOf("uv", "export") })
        val command = runner.commands.last()
        assertEquals(listOf("uv", "pip", "install"), command.take(3))
        // The packages are chosen for the target environment, not the host
        assertEquals("aarch64-unknown-linux-gnu", command[command.indexOf("--python-platform") + 1])
    }

    @Test
    fun testInstallIsReused(@TempDir tmp: Path, @TempDir target: Path) {
        tmp.file("requirements.txt", "pynacl==1.5.0\n")
        val runner = FakeRunner(available = setOf("pip"))
        val detector = PythonBuildDetector(installDirectory = target, runner = runner)

        detector.detect(tmp, environment)
        val installs = runner.commands.count { "install" in it }
        detector.detect(tmp, environment)

        assertEquals(1, installs)
        assertEquals(installs, runner.commands.count { "install" in it })
    }

    @Test
    fun testInstallDisabledNeverRunsATool(@TempDir tmp: Path) {
        tmp.file("requirements.txt", "pynacl==1.5.0\n")
        val runner = FakeRunner(available = setOf("pip", "uv"))

        val result =
            assertNotNull(
                PythonBuildDetector(install = false, runner = runner).detect(tmp, environment)
            )

        assertEquals(listOf(), result.includePaths)
        assertEquals(listOf(), runner.commands)
    }

    @Test
    fun testExistingEnvironmentIsPreferredToInstalling(@TempDir tmp: Path, @TempDir target: Path) {
        tmp.file("requirements.txt", "pynacl==1.5.0\n")
        tmp.file(".venv/lib/python3.12/site-packages/nacl/__init__.py")
        val runner = FakeRunner(available = setOf("pip"))

        PythonBuildDetector(installDirectory = target, runner = runner).detect(tmp, environment)

        assertEquals(listOf(), runner.commands)
    }

    @Test
    fun testFailedInstallOrMissingToolsAreNotFatal(@TempDir tmp: Path, @TempDir target: Path) {
        tmp.file("requirements.txt", "pynacl==1.5.0\n")

        val failed =
            PythonBuildDetector(
                    installDirectory = target,
                    runner = FakeRunner(setOf("pip"), installSucceeds = false),
                )
                .detect(tmp, environment)
        val noTools =
            PythonBuildDetector(installDirectory = target, runner = FakeRunner(setOf()))
                .detect(tmp, environment)

        assertEquals(listOf(), assertNotNull(failed).includePaths)
        assertEquals(listOf(), assertNotNull(noTools).includePaths)
    }

    @Test
    fun testNothingToInstall(@TempDir tmp: Path, @TempDir target: Path) {
        tmp.file("pyproject.toml")
        val runner = FakeRunner(available = setOf("pip"))

        val result =
            assertNotNull(
                PythonBuildDetector(installDirectory = target, runner = runner)
                    .detect(tmp, environment)
            )

        assertEquals(listOf(), result.includePaths)
        assertTrue(runner.commands.none { "install" in it })
    }

    @Test
    fun testNothingHappensForOtherProjects(@TempDir tmp: Path, @TempDir parent: Path) {
        tmp.file("main.go")
        val target = parent.resolve("not-created")
        val runner = FakeRunner(available = setOf("pip", "uv"))

        val result =
            PythonBuildDetector(installDirectory = target, runner = runner).detect(tmp, environment)

        // Neither a tool is started nor a folder created for a project that is not a Python project
        assertNull(result)
        assertEquals(listOf(), runner.commands)
        assertFalse(target.exists())
    }

    @Test
    fun testNothingToInstallDoesNotCreateAFolder(@TempDir tmp: Path, @TempDir parent: Path) {
        tmp.file("pyproject.toml")
        val target = parent.resolve("not-created")
        val runner = FakeRunner(available = setOf("pip"))

        PythonBuildDetector(installDirectory = target, runner = runner).detect(tmp, environment)

        assertEquals(listOf(), runner.commands)
        assertFalse(target.exists())
    }

    @Test
    fun testDirectReferencesAreNotInstalled(@TempDir tmp: Path, @TempDir target: Path) {
        // `--only-binary` does not prevent that a local package is built, so this must not be run
        for (requirement in listOf("-e ./pkg", "./pkg", "pkg @ https://example.com/pkg.tar.gz")) {
            tmp.file("requirements.txt", "pynacl==1.5.0\n$requirement\n")
            val runner = FakeRunner(available = setOf("pip"))

            val result =
                assertNotNull(
                    PythonBuildDetector(installDirectory = target, runner = runner)
                        .detect(tmp, environment)
                )

            assertEquals(listOf(), result.includePaths, requirement)
            assertTrue(runner.commands.none { "install" in it }, requirement)
        }
    }

    @Test
    fun testChangedRequirementsAreInstalledAgain(@TempDir tmp: Path, @TempDir target: Path) {
        val runner = FakeRunner(available = setOf("pip"))
        val detector = PythonBuildDetector(installDirectory = target, runner = runner)

        tmp.file("requirements.txt", "pynacl==1.5.0\n")
        detector.detect(tmp, environment)
        tmp.file("requirements.txt", "pynacl==1.4.0\n")
        detector.detect(tmp, environment)

        assertEquals(2, runner.commands.count { "install" in it })
    }

    @Test
    fun testDefaultInstallDirectoryBelongsToTheUser(@TempDir tmp: Path, @TempDir home: Path) {
        tmp.file("requirements.txt", "pynacl==1.5.0\n")
        val runner = FakeRunner(available = setOf("pip"))
        val previousHome = System.getProperty("user.home")
        System.setProperty("user.home", home.toString())

        try {
            val result =
                assertNotNull(PythonBuildDetector(runner = runner).detect(tmp, environment))

            val installed = result.includePaths.single().toRealPath()
            assertTrue(installed.startsWith(home.resolve(".cache/cpg/python").toRealPath()))
            if ("posix" in installed.fileSystem.supportedFileAttributeViews()) {
                assertEquals(
                    "rwx------",
                    PosixFilePermissions.toString(Files.getPosixFilePermissions(installed)),
                )
            }
        } finally {
            System.setProperty("user.home", previousHome)
        }
    }

    @Test
    fun testPackagesThatAreNotRequiredAnymoreAreRemoved(@TempDir tmp: Path, @TempDir target: Path) {
        val runner = FakeRunner(available = setOf("pip"))
        val detector = PythonBuildDetector(installDirectory = target, runner = runner)

        tmp.file("requirements.txt", "pynacl==1.5.0\n")
        detector.detect(tmp, environment)
        assertTrue(target.resolve("pynacl.dist-info").exists())

        tmp.file("requirements.txt", "six==1.16.0\n")
        val result = assertNotNull(detector.detect(tmp, environment))

        // The packages of the previous requirements are not analyzed anymore
        assertEquals(listOf(target.toRealPath()), result.includePaths.map { it.toRealPath() })
        assertFalse(target.resolve("pynacl.dist-info").exists())
        assertTrue(target.resolve("six.dist-info").exists())
    }

    @Test
    fun testFolderWithOtherContentIsNeverEmptied(@TempDir tmp: Path, @TempDir target: Path) {
        tmp.file("requirements.txt", "pynacl==1.5.0\n")
        val important = target.resolve("important.txt")
        important.writeText("keep me")
        val runner = FakeRunner(available = setOf("pip"))

        val result =
            assertNotNull(
                PythonBuildDetector(installDirectory = target, runner = runner)
                    .detect(tmp, environment)
            )

        assertEquals(listOf(), result.includePaths)
        assertTrue(runner.commands.none { "install" in it })
        assertTrue(important.exists())
    }

    @Test
    fun testFailedReinstallIsNotMistakenForAnInstallation(
        @TempDir tmp: Path,
        @TempDir target: Path,
    ) {
        tmp.file("requirements.txt", "pynacl==1.5.0\n")
        PythonBuildDetector(installDirectory = target, runner = FakeRunner(setOf("pip")))
            .detect(tmp, environment)

        // The requirements change and the installation fails
        tmp.file("requirements.txt", "six==1.16.0\n")
        val failed =
            PythonBuildDetector(
                    installDirectory = target,
                    runner = FakeRunner(setOf("pip"), installSucceeds = false),
                )
                .detect(tmp, environment)
        assertEquals(listOf(), assertNotNull(failed).includePaths)

        // The next run installs again instead of reusing what is left
        val runner = FakeRunner(setOf("pip"))
        val result =
            assertNotNull(
                PythonBuildDetector(installDirectory = target, runner = runner)
                    .detect(tmp, environment)
            )
        assertEquals(1, runner.commands.count { "install" in it })
        assertEquals(listOf(target.toRealPath()), result.includePaths.map { it.toRealPath() })
    }

    @Test
    fun testOnlyTheDependenciesOfTheProjectAreExportedFromTheLockfile(
        @TempDir tmp: Path,
        @TempDir target: Path,
    ) {
        tmp.file("uv.lock")
        val runner = FakeRunner(available = setOf("uv"))

        PythonBuildDetector(installDirectory = target, runner = runner).detect(tmp, environment)

        // Dependencies for development (tests, documentation, ...) are not installed and analyzed
        val export = runner.commands.single { it.take(2) == listOf("uv", "export") }
        assertContains(export, "--no-dev")
    }

    @Test
    fun testNothingIsWrittenToAFolderThatCannotBeUsed(@TempDir tmp: Path, @TempDir target: Path) {
        tmp.file("uv.lock")
        target.resolve("important.txt").writeText("keep me")
        val runner = FakeRunner(available = setOf("uv"))

        PythonBuildDetector(installDirectory = target, runner = runner).detect(tmp, environment)

        // The exported lock file does not end up in the folder with other content either
        assertEquals(
            listOf("important.txt"),
            target.listDirectoryEntries().map { it.fileName.toString() },
        )
        assertTrue(runner.commands.none { "install" in it })
    }
}
