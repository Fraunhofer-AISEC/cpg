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
import de.fraunhofer.aisec.cpg.project.TargetEnvironment
import java.nio.file.Path
import kotlin.io.path.exists
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
 * Detects a Go module in [directory] by looking for a `go.mod` file. The component is named after
 * the last segment of the module path.
 */
internal fun detectGoModule(directory: Path): List<ComponentDefinition> {
    val goMod = directory.resolve("go.mod")
    if (!goMod.exists()) {
        return listOf()
    }

    return listOf(
        ComponentDefinition(name = parseGoMod(goMod)?.name ?: directory.name, root = directory)
    )
}

/**
 * Detects Go-wide project settings: if [directory] looks like a Go project (it contains a `go.mod`,
 * a `go.work` or Go source files), the `GOOS` and `GOARCH` symbols are derived from the target
 * [environment] so that build constraints are evaluated for the correct target rather than the
 * machine the analysis runs on.
 */
internal fun detectGoSettings(directory: Path, environment: TargetEnvironment): DetectionResult? {
    val looksLikeGo =
        directory.resolve("go.mod").exists() ||
            directory.resolve("go.work").exists() ||
            directory.listDirectoryEntries("*.go").isNotEmpty()
    if (!looksLikeGo) {
        return null
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

    return DetectionResult(detector = "go", symbols = symbols, notes = notes)
}
