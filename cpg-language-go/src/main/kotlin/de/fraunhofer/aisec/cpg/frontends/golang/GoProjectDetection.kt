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
import de.fraunhofer.aisec.cpg.project.ComponentDefinition
import de.fraunhofer.aisec.cpg.project.DetectionResult
import de.fraunhofer.aisec.cpg.project.OperatingSystem
import de.fraunhofer.aisec.cpg.project.SKIPPED_DIRECTORIES
import de.fraunhofer.aisec.cpg.project.TargetEnvironment
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readLines

/** The `GOOS` value corresponding to this [OperatingSystem], or null if there is none. */
val OperatingSystem.goos: String?
    get() =
        when (this) {
            OperatingSystem.LINUX -> "linux"
            OperatingSystem.MACOS -> "darwin"
            OperatingSystem.WINDOWS -> "windows"
            OperatingSystem.FREEBSD -> "freebsd"
            OperatingSystem.UNKNOWN -> null
        }

/** The `GOARCH` value corresponding to this [Architecture], or null if there is none. */
val Architecture.goarch: String?
    get() =
        when (this) {
            Architecture.X86_64 -> "amd64"
            Architecture.ARM64 -> "arm64"
            Architecture.X86 -> "386"
            Architecture.ARM -> "arm"
            Architecture.RISCV64 -> "riscv64"
            Architecture.UNKNOWN -> null
        }

/** Information parsed from a `go.mod` file. */
internal data class GoModule(
    /** The module path, e.g., `example.com/app`. */
    val path: String,
    /** The minor part of the version in the `go` directive, e.g., 22 for `go 1.22.1`. */
    val minorVersion: Int?,
) {
    /** The short name of the module, i.e., the last segment of the module path. */
    val name: String
        get() = path.substringAfterLast('/')
}

/**
 * Parses the `module` and `go` directives out of the given [goMod] file, or returns null if it
 * cannot be parsed. This is intentionally a lightweight line-based parser, so that project
 * detection does not require the native Go helper library.
 */
internal fun parseGoMod(goMod: Path): GoModule? {
    val lines =
        try {
            goMod.readLines()
        } catch (_: Exception) {
            return null
        }

    val path =
        lines
            .firstOrNull { it.startsWith("module ") }
            ?.removePrefix("module ")
            ?.substringBefore("//")
            ?.trim() ?: return null
    val version =
        lines
            .map { it.trim() }
            .firstOrNull { it.startsWith("go ") }
            ?.removePrefix("go ")
            ?.substringBefore("//")
            ?.trim()

    return GoModule(path, version?.split('.')?.getOrNull(1)?.toIntOrNull())
}

/**
 * Detects Go project structure and settings in one pass. Walks [root] looking for `go.mod` files
 * (each one becomes a component) and derives `GOOS`/`GOARCH` symbols from the target [environment].
 * Returns `null` if [root] does not look like a Go project at all.
 */
internal fun detectGo(root: Path, environment: TargetEnvironment): DetectionResult? {
    // Walk for go.mod files to find all modules (supports monorepos with multiple modules).
    val moduleRoots =
        root
            .toFile()
            .walkTopDown()
            .onEnter { !it.name.startsWith(".") && it.name !in SKIPPED_DIRECTORIES }
            .filter { it.name == "go.mod" && it.isFile }
            .map { it.parentFile.toPath() }
            .toList()

    val looksLikeGo =
        moduleRoots.isNotEmpty() ||
            root.resolve("go.work").exists() ||
            (root.isDirectory() && root.listDirectoryEntries("*.go").isNotEmpty())

    if (!looksLikeGo) return null

    val components =
        moduleRoots.map { moduleRoot ->
            val goMod = moduleRoot.resolve("go.mod")
            ComponentDefinition(
                name = parseGoMod(goMod)?.name ?: moduleRoot.name,
                root = moduleRoot,
            )
        }

    val symbols = mutableMapOf<String, String>()
    val notes = mutableListOf<String>()
    environment.os.goos?.let {
        symbols["GOOS"] = it
        notes += "derived GOOS=$it from target environment"
    }
    environment.architecture.goarch?.let {
        symbols["GOARCH"] = it
        notes += "derived GOARCH=$it from target environment"
    }

    return DetectionResult(
        detector = "go",
        components = components,
        symbols = symbols,
        notes = notes,
    )
}
