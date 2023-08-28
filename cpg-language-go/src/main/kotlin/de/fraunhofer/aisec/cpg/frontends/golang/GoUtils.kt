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

import java.io.File
import java.util.concurrent.TimeUnit

fun shouldBuild(file: File, symbols: Map<String, String>): Boolean {
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
            .firstOrNull() { it.startsWith("//go:build") }
            ?: return true

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
                    "solaris"
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

fun gatherGoFiles(root: File, includeSubDir: Boolean = true): List<File> {
    return root
        .walkTopDown()
        .onEnter { (it == root || includeSubDir) && !it.name.contains(".go") }
        .filter {
            // skip tests for now
            it.extension == "go" && !it.name.endsWith("_test.go")
        }
        .toList()
}

class Project {
    var symbols: Map<String, String> = mutableMapOf()

    var components: MutableMap<String, List<File>> = mutableMapOf()

    var includePaths: List<File> = mutableListOf()
}

fun buildProject(
    modulePath: String,
    goos: String? = null,
    goarch: String? = null,
    tags: List<String> = listOf()
): Project {
    val project = Project()
    val symbols = mutableMapOf<String, String>()
    var files = mutableListOf<File>()

    val topLevel = File(modulePath)

    var proc =
        ProcessBuilder("go", "list", "all")
            .directory(topLevel)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
    proc.waitFor(5, TimeUnit.MINUTES)

    // For now, we only support deps in the standard library
    val deps = proc.inputStream.bufferedReader().readLines().filter { !it.contains(".") }

    proc =
        ProcessBuilder("go", "env", "GOROOT").redirectOutput(ProcessBuilder.Redirect.PIPE).start()
    proc.waitFor(5, TimeUnit.MINUTES)

    val stdLib = File(proc.inputStream.bufferedReader().readLine()).resolve("src")

    files += deps.flatMap { gatherGoFiles(stdLib.resolve(it), false) }
    files += gatherGoFiles(topLevel)

    goos?.let { symbols["GOOS"] = it }
    goarch?.let { symbols["GOARCH"] = it }
    tags.let { symbols["-tags"] = tags.joinToString { " " } }

    // Pre-filter any files we are not building anyway based on our symbols
    files = files.filter { shouldBuild(it, symbols) }.toMutableList()

    // TODO(oxisto): look for binaries
    project.components["app"] = files
    project.symbols = symbols
    // TODO(oxisto): support vendor includes
    project.includePaths = listOf(stdLib)

    return project
}
