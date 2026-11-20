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
import de.fraunhofer.aisec.cpg.project.ComponentDefinition
import de.fraunhofer.aisec.cpg.project.DetectionResult
import de.fraunhofer.aisec.cpg.project.Detector
import de.fraunhofer.aisec.cpg.project.OperatingSystem
import de.fraunhofer.aisec.cpg.project.ProcessToolRunner
import de.fraunhofer.aisec.cpg.project.SKIPPED_DIRECTORIES
import de.fraunhofer.aisec.cpg.project.TargetEnvironment
import de.fraunhofer.aisec.cpg.project.ToolRunner
import de.fraunhofer.aisec.cpg.project.run
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.slf4j.LoggerFactory

/**
 * A tool-backed detector that makes the dependencies of a Python project available for analysis. It
 * contributes the `site-packages` folder of a populated Python environment as an include path. The
 * Python frontend resolves `import` statements against the include paths and only translates the
 * modules that are actually (transitively) imported, so the dependencies of a project are analyzed
 * without parsing the whole environment. For this, the detector also enables
 * [de.fraunhofer.aisec.cpg.TranslationConfiguration.loadIncludes].
 *
 * The environment is looked up in the following order:
 * 1. [existingEnvironment], if given. It can either be the root of a virtual environment or a
 *    folder that directly contains the installed packages.
 * 1. A `.venv`, `venv` or `env` folder in the project root.
 * 1. A `vendor` folder in the project root, if it contains installed packages.
 * 1. If [install] is set, the project is a Python project and none of the above exists, the
 *    dependencies are installed from a `uv.lock` or `requirements.txt` into [installDirectory]
 *    using `uv` or, if it is not available, `pip`.
 *
 * Installing is restricted to what does not run code of the packages: only binary distributions
 * (wheels) are installed, and a source distribution is never built. If the requirements carry
 * hashes, they are verified. This also means that packages that are only available as a source
 * distribution are not part of the analysis. Because `--only-binary` does not cover them, nothing
 * is installed if the requirements contain a local path, an editable install, a URL or an option
 * (see [PythonRequirements]); only plain packages from an index are accepted.
 *
 * Note that [de.fraunhofer.aisec.cpg.TranslationConfiguration.loadIncludes] is a setting of the
 * whole project: in a project that also contains other languages, such as C/C++, enabling it
 * changes how their include files are handled as well.
 *
 * The files of the project itself (everything that is not part of the environment) are contributed
 * as sources of a single component, so that the environment is not mistaken for project code. The
 * detector is not active by default; add it with
 * [de.fraunhofer.aisec.cpg.project.ProjectBuilder.detector]:
 * ```kotlin
 * project(dir) { detector(PythonBuildDetector()) }
 * ```
 */
class PythonBuildDetector(
    /**
     * An explicit environment, either the root of a virtual environment or a folder containing
     * installed packages. If it is relative, it is resolved against the project root.
     */
    private val existingEnvironment: Path? = null,
    /**
     * Whether the dependencies are installed with `uv` or `pip` if no existing environment was
     * found. Set it to false to only use an existing environment and to never run a tool.
     */
    private val install: Boolean = true,
    /**
     * The folder that dependencies are installed into. By default, this is a folder in the cache of
     * the current user (`~/.cache/cpg/python`) that is specific to the project, so that the project
     * itself stays clean and a previous installation can be reused. It is only used if it is owned
     * by the current user and not a symbolic link.
     */
    private val installDirectory: Path? = null,
    /** Runs `uv` or `pip`. */
    private val runner: ToolRunner = ProcessToolRunner(),
) : Detector {

    override fun detect(root: Path, environment: TargetEnvironment): DetectionResult? {
        val found = findSitePackages(root)
        val python = isPythonProject(root)
        // Nothing is run or created for a project that is not a Python project
        val installed =
            if (found == null && python && install) installDependencies(root, environment) else null
        val sitePackages = found ?: installed
        if (sitePackages == null && !python) {
            return null
        }

        val absoluteRoot = root.toAbsolutePath().normalize()
        val sources =
            absoluteRoot
                .toFile()
                .walkTopDown()
                .onEnter { it.toPath() == absoluteRoot || !isExcluded(it.toPath(), sitePackages) }
                .filter { it.isFile && it.extension == "py" }
                .map { it.toPath() }
                .sorted()
                .toList()

        return DetectionResult(
            detector = "python-build",
            components = listOf(ComponentDefinition(root.name, root = root, sources = sources)),
            includePaths = listOfNotNull(sitePackages),
            // Otherwise, imports of the dependencies would only result in inferred declarations
            loadIncludes = sitePackages != null,
            notes =
                listOf(
                    when {
                        found != null -> "using installed packages in $found"
                        installed != null -> "installed packages into $installed"
                        else -> "no Python environment found, dependencies are not resolved"
                    }
                ),
        )
    }

    /**
     * Installs the dependencies of the project in [root] into [installDirectory] and returns the
     * folder containing the installed packages, or null if there is nothing to install or the
     * installation failed.
     */
    private fun installDependencies(root: Path, environment: TargetEnvironment): Path? {
        val lockFile = root.resolve("uv.lock").takeIf { it.exists() }
        val requirementsFile = root.resolve("requirements.txt").takeIf { it.exists() }
        if (lockFile == null && requirementsFile == null) {
            log.info("No uv.lock or requirements.txt in {}, nothing to install", root)
            return null
        }

        val uv = runner.run(listOf("uv", "--version")) != null
        if (!uv && runner.run(listOf("python3", "-m", "pip", "--version")) == null) {
            log.warn("Neither uv nor pip is available, cannot install the dependencies")
            return null
        }

        val target =
            installDirectory?.also { it.createDirectories() } ?: defaultInstallDirectory(root)
        if (target == null) {
            return null
        }

        // A lock file is preferred, because it pins all dependencies including their hashes. It is
        // exported to a file of its own, because nothing must be written to the install folder
        // before it is known that it can be used.
        val exported = if (uv && lockFile != null) exportLock(root) else null
        try {
            val requirements = exported ?: requirementsFile ?: return null
            return install(root, target, uv, requirements, environment)
        } finally {
            exported?.deleteIfExists()
        }
    }

    /**
     * Exports the `uv.lock` of the project in [root] to a requirements file, or returns null if
     * that failed. Only the dependencies of the project are exported, not those for development
     * (tests, documentation, ...), because the project itself does not need them.
     */
    private fun exportLock(root: Path): Path? {
        val exported = createTempFile("cpg-requirements", ".txt")
        val ok =
            runner.run(
                listOf(
                    "uv",
                    "export",
                    "--frozen",
                    "--no-dev",
                    "--no-emit-project",
                    "--format",
                    "requirements-txt",
                    "--output-file",
                    exported.toString(),
                ),
                root,
            ) != null
        if (!ok) {
            exported.deleteIfExists()
            return null
        }

        return exported
    }

    /** Installs the packages of the [requirements] into [target], see [installDependencies]. */
    private fun install(
        root: Path,
        target: Path,
        uv: Boolean,
        requirements: Path,
        environment: TargetEnvironment,
    ): Path? {
        val text = requirements.readText()
        PythonRequirements.findUnsupported(text)?.let {
            log.warn(
                "Not installing the dependencies of {}: '{}' is not a plain package from an index",
                root,
                it,
            )
            return null
        }

        // Without hashes, the requirements are not necessarily complete, so dependencies of the
        // listed packages must be resolved as well
        val locked = text.contains("--hash=")
        val common =
            listOf(
                "--target",
                target.toString(),
                "--only-binary",
                ":all:",
                "-r",
                requirements.toString(),
            ) + (if (locked) listOf("--no-deps", "--require-hashes") else listOf())
        val command =
            if (uv) {
                listOf("uv", "pip", "install") + common + platformOptions(environment)
            } else {
                listOf("python3", "-m", "pip", "install") + common
            }

        // If neither the requirements nor the command changed, a previous installation is reused.
        // The requirements path is excluded from the fingerprint because it is a freshly created
        // temporary file with a random name for `uv.lock` projects, which would otherwise make the
        // fingerprint differ on every run even though the requirements content (`text`) is the
        // same.
        val marker = target.resolve(MARKER)
        val fingerprint =
            sha256(
                text +
                    "\u0000" +
                    command.filterNot { it == requirements.toString() }.joinToString(" ")
            )
        if (marker.exists() && marker.readText() == fingerprint) {
            return sitePackagesOf(target)
        }

        // `--target` installs on top of what is already there. Packages that are not required
        // anymore must not stay behind, because they would be analyzed as well. Therefore, the
        // folder is emptied before installing, but only if it was created by this detector, so
        // that a folder with other content is never touched.
        val previous = target.listDirectoryEntries()
        if (!marker.exists() && previous.any { it.name != MARKER }) {
            log.warn("Not installing into {}: it is not empty and was not created for this", target)
            return null
        }
        // Without the marker, a partial installation is never mistaken for a complete one
        previous.forEach { it.toFile().deleteRecursively() }

        if (runner.run(command, root) == null) {
            return null
        }

        marker.writeText(fingerprint)
        return sitePackagesOf(target)
    }

    /**
     * Selects the platform of the [environment], so that matching binary distributions are used.
     */
    private fun platformOptions(environment: TargetEnvironment): List<String> {
        val platform =
            when (environment.os to environment.architecture) {
                OperatingSystem.LINUX to Architecture.X86_64 -> "x86_64-unknown-linux-gnu"
                OperatingSystem.LINUX to Architecture.ARM64 -> "aarch64-unknown-linux-gnu"
                OperatingSystem.MACOS to Architecture.X86_64 -> "x86_64-apple-darwin"
                OperatingSystem.MACOS to Architecture.ARM64 -> "aarch64-apple-darwin"
                OperatingSystem.WINDOWS to Architecture.X86_64 -> "x86_64-pc-windows-msvc"
                else -> null
            }
        return platform?.let { listOf("--python-platform", it) } ?: listOf()
    }

    /**
     * A folder in the cache of the current user that is specific to the project in [root]. It is
     * created readable by the user only, and not used if it already exists but belongs to somebody
     * else (or is a symbolic link), because the packages in it are analyzed. Returns null in this
     * case.
     */
    private fun defaultInstallDirectory(root: Path): Path? {
        val id = sha256(root.toAbsolutePath().normalize().toString()).take(32)
        val dir = Path.of(System.getProperty("user.home"), ".cache", "cpg", "python", id)

        return try {
            if (dir.isSymbolicLink()) {
                throw IllegalStateException("it is a symbolic link")
            }
            if (!dir.exists()) {
                dir.parent.createDirectories()
                if ("posix" in FileSystems.getDefault().supportedFileAttributeViews()) {
                    Files.createDirectory(
                        dir,
                        PosixFilePermissions.asFileAttribute(
                            PosixFilePermissions.fromString("rwx------")
                        ),
                    )
                } else {
                    Files.createDirectory(dir)
                }
            }
            if (!dir.isDirectory() || Files.getOwner(dir).name != System.getProperty("user.name")) {
                throw IllegalStateException("it is not a folder of the current user")
            }
            dir
        } catch (e: Exception) {
            log.warn("Not using {} to install the dependencies: {}", dir, e.message)
            null
        }
    }

    private fun sha256(text: String): String {
        return MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
    }

    /** Returns true if [root] contains a well-known Python project or dependency file. */
    private fun isPythonProject(root: Path): Boolean {
        return PROJECT_FILES.any { root.resolve(it).exists() } ||
            root.listDirectoryEntries("requirements*.txt").isNotEmpty()
    }

    /** Returns the folder with the installed packages, if there is a populated environment. */
    private fun findSitePackages(root: Path): Path? {
        val candidates =
            if (existingEnvironment != null) {
                listOf(root.resolve(existingEnvironment))
            } else {
                ENVIRONMENT_FOLDERS.map { root.resolve(it) }
            }

        return candidates.firstNotNullOfOrNull { sitePackagesOf(it) }?.toAbsolutePath()?.normalize()
    }

    /**
     * Returns the folder with the installed packages for a folder that is either a virtual
     * environment (`lib/pythonX.Y/site-packages`, `Lib/site-packages`) or a folder that contains
     * the installed packages itself (`pip install --target`).
     */
    private fun sitePackagesOf(folder: Path): Path? {
        if (!folder.isDirectory()) {
            return null
        }

        // POSIX virtual environment, the version of the interpreter is part of the path
        folder
            .resolve("lib")
            .takeIf { it.isDirectory() }
            ?.listDirectoryEntries("python3*")
            ?.sortedByDescending { it.name }
            ?.map { it.resolve("site-packages") }
            ?.firstOrNull { it.isDirectory() }
            ?.let {
                return it
            }

        // Windows virtual environment
        folder
            .resolve("Lib")
            .resolve("site-packages")
            .takeIf { it.isDirectory() }
            ?.let {
                return it
            }

        // A folder with installed packages, such as `pip install --target` creates it
        return folder.takeIf { f ->
            f.name == "site-packages" ||
                f.listDirectoryEntries("*.dist-info").isNotEmpty() ||
                f.listDirectoryEntries("*.egg-info").isNotEmpty()
        }
    }

    /**
     * Returns true if the folder [dir] is not part of the project itself: it is (or is inside) the
     * folder with the installed packages, a virtual environment (detected by its `pyvenv.cfg`), a
     * folder that is skipped by all detectors, or a cache or hidden folder. The project root is
     * never excluded.
     */
    private fun isExcluded(dir: Path, sitePackages: Path?): Boolean {
        val name = dir.name
        return (sitePackages != null &&
            dir.toAbsolutePath().normalize().startsWith(sitePackages)) ||
            name in SKIPPED_DIRECTORIES ||
            name == "__pycache__" ||
            dir.resolve("pyvenv.cfg").exists() ||
            (name.startsWith(".") && name.length > 1)
    }

    companion object {
        private val log = LoggerFactory.getLogger(PythonBuildDetector::class.java)

        /** Records the requirements that were installed, so that they are not installed twice. */
        private const val MARKER = ".cpg-installed"

        /** Files whose presence identifies a folder as the root of a Python project. */
        private val PROJECT_FILES =
            listOf("pyproject.toml", "setup.py", "setup.cfg", "Pipfile", "poetry.lock", "uv.lock")

        /** Folders in the project root that are searched for an environment. */
        private val ENVIRONMENT_FOLDERS = listOf(".venv", "venv", "env", "vendor")
    }
}
