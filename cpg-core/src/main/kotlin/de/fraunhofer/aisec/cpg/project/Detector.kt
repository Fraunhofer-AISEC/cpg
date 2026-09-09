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
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * Directory names that detection walks should never enter. Exported so that language-specific
 * [Detector] implementations can reuse the same skip list.
 */
val SKIPPED_DIRECTORIES = setOf("vendor", "node_modules", "testdata")

/**
 * A project auto-detector. Detectors answer two related questions about the project rooted at a
 * given directory:
 * - *Which components does it consist of?* (e.g., Go modules, CMake targets)
 * - *Which project-wide settings apply?* (pre-defined symbols, include paths, a compilation
 *   database)
 *
 * Both answers are returned together from a single [detect] call on the project root. The detector
 * is responsible for any sub-directory traversal it needs (e.g., walking for `go.mod` files).
 *
 * There are two ways to contribute a detector:
 * - A [de.fraunhofer.aisec.cpg.frontends.Language] can implement [Detector]. All registered
 *   languages are asked automatically when a [Project] is created from a directory.
 * - A standalone detector (not tied to a language) can be added with [ProjectBuilder.detector], for
 *   example the [DirectoryComponentDetector].
 *
 * All detection results are only suggestions: they are recorded in [Project.detectionResults] for
 * inspection, and any configuration explicitly made by the user in the [ProjectBuilder] takes
 * precedence.
 */
interface Detector {
    /**
     * Inspects the project [root] directory and returns a [DetectionResult] describing the detected
     * components and project-wide settings, or `null` if this detector does not recognise the
     * project. Called at most once per [Project] resolution, on the project root.
     *
     * The [environment] describes the target environment and can be used to make
     * environment-specific decisions (e.g., deriving Go build constraints from the target
     * architecture).
     */
    fun detect(root: Path, environment: TargetEnvironment): DetectionResult?
}

/**
 * The combined result of a [Detector.detect] invocation. All properties are suggestions that are
 * merged into the [Project] during resolution, unless the user has explicitly configured
 * conflicting values. If multiple detectors produce a result with the same [detector] name, only
 * the first result is used.
 */
class DetectionResult(
    /** A human-readable name of the detector that produced this result. */
    val detector: String,
    /**
     * The components detected in the project, e.g., Go modules or CMake targets. If empty,
     * detection falls through to subsequent detectors or the default single-component behaviour.
     */
    val components: List<ComponentDefinition> = listOf(),
    /** Additional pre-defined symbols, e.g., `GOOS`/`GOARCH` derived from the environment. */
    val symbols: Map<String, String> = mapOf(),
    /** Additional include paths, e.g., derived from a build configuration. */
    val includePaths: List<Path> = listOf(),
    /**
     * A detected [CompilationDatabase] (e.g., from a `compile_commands.json`), which supplies
     * per-file include paths and defines to the C/C++ frontend.
     */
    val compilationDatabase: CompilationDatabase? = null,
    /** Human-readable notes about what was detected, mainly for diagnostic purposes. */
    val notes: List<String> = listOf(),
) {
    override fun toString(): String {
        return "DetectionResult(detector=$detector, components=${components.map { it.name }}, " +
            "symbols=$symbols, notes=$notes)"
    }
}

/**
 * A standalone [Detector] that derives one component per direct subdirectory of [folder] under the
 * project root. Useful for repositories that follow a convention-based layout, such as a monorepo
 * with one service per directory.
 *
 * This detector is not active by default; add it with [ProjectBuilder.detector]:
 * ```kotlin
 * project(dir) { components { detector(DirectoryComponentDetector("services")) } }
 * ```
 */
class DirectoryComponentDetector(private val folder: String = "components") : Detector {
    override fun detect(root: Path, environment: TargetEnvironment): DetectionResult? {
        val base = root.resolve(folder)
        if (!base.isDirectory()) {
            return null
        }

        val components =
            base
                .listDirectoryEntries()
                .filter { it.isDirectory() && !it.name.startsWith(".") }
                .sortedBy { it.name }
                .map { ComponentDefinition(it.name, root = it) }

        if (components.isEmpty()) {
            return null
        }

        return DetectionResult(detector = "directory/$folder", components = components)
    }
}
