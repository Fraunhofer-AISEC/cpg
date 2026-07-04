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

import de.fraunhofer.aisec.cpg.frontends.CompilationDatabase
import de.fraunhofer.aisec.cpg.frontends.Language
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * The common interface for all project auto-detection hooks. There are two ways to contribute a
 * detector:
 * - A [Language] can implement one (or both) of the sub-interfaces [ComponentDetector] and
 *   [ProjectDetector]. All registered languages are asked automatically when a [Project] is created
 *   from a directory (see [Project.from]).
 * - A standalone detector (not tied to a language) can be added with [ProjectBuilder.detector], for
 *   example the [DirectoryComponentDetector].
 *
 * Detection is split into two concerns:
 * - [ComponentDetector]s answer *"which units (components) does this project consist of?"*. They
 *   are invoked for the project directory and every (non-excluded) subdirectory during a central
 *   directory walk, so a detector only needs to recognize a single directory (e.g., "does it
 *   contain a `go.mod`?").
 * - [ProjectDetector]s answer *"which settings apply to this project?"* (pre-defined symbols,
 *   include paths, a compilation database). They are invoked once on the project directory.
 *
 * All detection results are only suggestions: they are recorded in [Project.detectionResults] for
 * inspection and any configuration explicitly made by the user in the [ProjectBuilder] takes
 * precedence.
 */
sealed interface Detector

/**
 * Detects the individual components of a [Project], such as Go modules, services in a monorepo or
 * the targets of a compilation database. See [Detector] for how detection is organized.
 */
interface ComponentDetector : Detector {
    /**
     * Inspects a single [directory] and returns the components rooted in it, or an empty list if
     * this detector does not recognize it. This function is called for the project directory and
     * each of its (non-excluded) subdirectories; the surrounding walk is handled centrally by the
     * [ProjectBuilder].
     */
    fun detectComponents(directory: Path, environment: TargetEnvironment): List<ComponentDefinition>
}

/**
 * Detects project-wide settings, such as pre-defined symbols, include paths or a compilation
 * database. See [Detector] for how detection is organized.
 */
interface ProjectDetector : Detector {
    /**
     * Inspects the project [directory] and returns a [DetectionResult] if this detector recognizes
     * the project, or `null` otherwise. In contrast to [ComponentDetector.detectComponents], this
     * function is only invoked once, on the project directory itself.
     *
     * The [environment] describes the target environment of the project and can be used to make
     * environment-specific decisions (e.g., deriving Go build constraints from the target
     * architecture).
     */
    fun detect(directory: Path, environment: TargetEnvironment): DetectionResult?
}

/**
 * The result of a [ProjectDetector.detect] invocation. All properties are suggestions that are
 * merged into the [Project] during resolution, unless the user has explicitly configured
 * conflicting values. If multiple detectors produce a result with the same [detector] name (e.g.,
 * because two related languages share the detection logic), only the first result is used.
 */
class DetectionResult(
    /** A human-readable name of the detector that produced this result. */
    val detector: String,
    /** Additional include paths, e.g., derived from a build configuration. */
    val includePaths: List<Path> = listOf(),
    /** Additional pre-defined symbols, e.g., `GOOS`/`GOARCH` derived from the environment. */
    val symbols: Map<String, String> = mapOf(),
    /**
     * A detected [CompilationDatabase] (e.g., from a `compile_commands.json`), which supplies
     * per-file include paths and defines to the C/C++ frontend.
     */
    val compilationDatabase: CompilationDatabase? = null,
    /** Human-readable notes about what was detected, mainly for diagnostic purposes. */
    val notes: List<String> = listOf(),
) {
    override fun toString(): String {
        return "DetectionResult(detector=$detector, symbols=$symbols, notes=$notes)"
    }
}

/**
 * A standalone [ComponentDetector] that derives one component per direct subdirectory of [folder].
 * This is useful for repositories that follow a convention-based layout, such as the `components`
 * folder used by Codyze projects or a monorepo with one service per directory.
 *
 * This detector is not active by default; add it with [ProjectBuilder.detector]:
 * ```kotlin
 * project(dir) { detector(DirectoryComponentDetector("services")) }
 * ```
 */
class DirectoryComponentDetector(private val folder: String = "components") : ComponentDetector {
    override fun detectComponents(
        directory: Path,
        environment: TargetEnvironment,
    ): List<ComponentDefinition> {
        val base = directory.resolve(folder)
        if (!base.isDirectory()) {
            return listOf()
        }

        return base
            .listDirectoryEntries()
            .filter { it.isDirectory() && !it.name.startsWith(".") }
            .sortedBy { it.name }
            .map { ComponentDefinition(it.name, root = it) }
    }
}
