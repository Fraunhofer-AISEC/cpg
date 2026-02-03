/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.CompilationDatabase
import java.io.File
import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This functions checks whether the file specified in [file] should be processed in the
 * [GoLanguageFrontend]. It mainly checks for build constraints, which can either be part of the
 * filename or specified in the file (see [BuildConstraintExpression]).
 *
 * Note: This is currently specific to Go, but could be integrated into the [TranslationManager] for
 * other languages using an appropriate interface.
 */
internal fun shouldBeBuild(file: File, symbols: Map<String, String>): Boolean {
    // First, we need to check, whether the filename ends with a possible _$GOOS.go, $_GOARCH.go
    // or $_GOOS_$GOARCH.go
    val parts = file.nameWithoutExtension.split("_")

    // If the last part is a possible GOOS value, then we need to check, if this is equal to our
    // current GOOS
    if (
        parts.lastOrNull() in GoLanguageFrontend.goosValues && parts.lastOrNull() != symbols["GOOS"]
    ) {
        // Skip the contents
        return false
    }

    // If the last part is a possible GOARCH value, then we need to check, if this is equal to
    // our current GOARCH
    if (
        parts.lastOrNull() in GoLanguageFrontend.goarchValues &&
            parts.lastOrNull() != symbols["GOARCH"]
    ) {
        return false
    }

    val sub = parts.subList((parts.size - 2).coerceAtLeast(0), parts.size.coerceAtLeast(0))
    if (
        sub.size == 2 &&
            ((sub[0] in GoLanguageFrontend.goosValues && sub[0] != symbols["GOOS"]) ||
                (sub[1] in GoLanguageFrontend.goarchValues && sub[1] != symbols["GOARCH"]))
    ) {
        return false
    }

    // Next, we need to peek into the file, to see whether any build tags are present. The
    // fastest way
    // to do that is to read the file and look for a go:build line
    val goBuildLine =
        file
            .bufferedReader()
            .useLines { lines -> lines.take(50).toList() }
            .firstOrNull { it.startsWith("//go:build") } ?: return true

    val constraint = BuildConstraintExpression.fromString(goBuildLine.substringAfter("//go:build "))

    return constraint?.evaluate(symbols.buildTags) == true
}

private val Map<String, String>.buildTags: Set<String>
    get() {
        val tags = mutableSetOf<String>()
        val goos = this["GOOS"]
        val goarch = this["GOARCH"]

        // Add GOOS and GOARCH
        goos?.let { tags += it }
        goarch?.let { tags += it }

        // We need to derive some more build tags based on the GOOS.
        // See
        // https://github.com/golang/go/blob/release-branch.go1.21/src/go/build/syslist.go#L39
        if (
            goos in
                listOf(
                    "aix",
                    "android",
                    "darwin",
                    "dragonfly",
                    "freebsd",
                    "hurd",
                    "illumos",
                    "ios",
                    "linux",
                    "netbsd",
                    "openbsd",
                    "solaris",
                )
        ) {
            tags += "unix"
        }

        // Additional "derived" operating systems
        when (goos) {
            "android" -> tags += "linux"
            "illumos" -> tags += "solaris"
            "ios" -> tags += "darwin"
        }

        // Add remaining tags
        this["-tags"]?.split(" ")?.let { tags += it }

        return tags
    }

internal fun gatherGoFiles(root: File, includeSubDir: Boolean = true): List<File> {
    return root
        .walkTopDown()
        .onEnter { (it == root || includeSubDir) && !it.name.endsWith(".go") }
        .filter {
            // skip tests for now
            it.extension == "go" && !it.name.endsWith("_test.go")
        }
        .toList()
}

/**
 * Represents a Go project. This could potentially be extended and moved to cpg-core to be available
 * for other languages. It shares some fields with a [CompilationDatabase] and both could
 * potentially be merged.
 */
class Project {
    var symbols: Map<String, String> = mutableMapOf()

    var components: MutableMap<String, List<File>> = mutableMapOf()

    var includePaths: List<File> = mutableListOf()

    var topLevel: File? = null

    companion object {
        val log: Logger = LoggerFactory.getLogger(Project::class.java)

        /**
         * This function emulates building a Go project. It requires an installed Go environment and
         * uses the Go binary to compile a list of package dependencies, which are then included
         * into the [Project] as includes.
         *
         * Note: This currently is limited to packages of the standard library
         */
        fun buildProject(
            modulePath: String,
            goos: String? = null,
            goarch: String? = null,
            goVersion: Int? = null,
            tags: MutableList<String> = mutableListOf(),
        ): Project {
            val project = Project()
            val symbols = mutableMapOf<String, String>()
            var files = mutableListOf<File>()

            val topLevel = File(modulePath)

            val goModFile = topLevel.resolve("go.mod")
            val module =
                GoStandardLibrary.Modfile.parse(goModFile.absolutePath, goModFile.readText())

            val pb =
                ProcessBuilder("go", "list", "-deps", tags.joinToString(",", "-tags="), "all")
                    .directory(topLevel)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)

            val env = pb.environment()
            env["GOOS"] = goos
            env["GOARCH"] = goarch

            var proc = pb.start()
            proc.waitFor(5, TimeUnit.MINUTES)
            if (proc.exitValue() != 0) {
                log.debug(proc.errorStream.bufferedReader().readLine())
            }

            // Read deps from standard input
            val deps = proc.inputStream.bufferedReader().readLines()

            log.debug("Identified {} package dependencies (stdlib only)", deps.size)

            proc =
                ProcessBuilder("go", "env", "GOROOT")
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .start()
            proc.waitFor(5, TimeUnit.MINUTES)
            if (proc.exitValue() != 0) {
                log.debug(proc.errorStream.bufferedReader().readLine())
            }

            val stdLib = File(proc.inputStream.bufferedReader().readLine()).resolve("src")

            log.debug("GOROOT/src is located @ {}", stdLib)

            // Build directories out of deps
            val dirs: List<File> =
                deps.mapNotNull {
                    if (!it.contains(".")) {
                        // without a dot, it is a stdlib package
                        stdLib.resolve(it)
                    } else if (it.startsWith("vendor")) {
                        // if the dependency path starts with "vendor", then it is a dependency that
                        // is vendored within the standard library (and not in the project). we
                        // don't really include these
                        // for now since they blow up the stdlib
                        null
                    } else if (it.startsWith(module.module.mod.path)) {
                        topLevel.resolve(it.substringAfter(module.module.mod.path))
                    } else {
                        // for all other dependencies, we try whether they are vendored within the
                        // current project. Note, this differs from the above case, where a
                        // dependency is vendored in the stdlib.
                        topLevel.resolve("vendor").resolve(it)
                    }
                }

            files += dirs.flatMap { gatherGoFiles(it, false) }
            // add cmd folder
            files += gatherGoFiles(topLevel.resolve("cmd"))

            goos?.let { symbols["GOOS"] = it }
            goarch?.let { symbols["GOARCH"] = it }

            if (goVersion != null) {
                // Populate tags with go-version
                for (i in 1..goVersion) {
                    tags += "go1.$i"
                }
            }

            tags.let { symbols["-tags"] = tags.joinToString(" ") }

            // Pre-filter any files we are not building anyway based on our symbols
            files = files.filter { shouldBeBuild(it, symbols) }.toMutableList()

            // TODO(oxisto): look for binaries in cmd folder
            project.components[TranslationResult.DEFAULT_APPLICATION_NAME] = files
            project.symbols = symbols
            // TODO(oxisto): support vendor includes
            project.includePaths = listOf(stdLib, topLevel.resolve("vendor"))
            project.topLevel = File(modulePath)

            return project
        }
    }
}
