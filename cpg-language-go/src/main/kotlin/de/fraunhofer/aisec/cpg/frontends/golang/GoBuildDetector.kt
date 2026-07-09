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

import de.fraunhofer.aisec.cpg.project.ComponentDefinition
import de.fraunhofer.aisec.cpg.project.DetectionResult
import de.fraunhofer.aisec.cpg.project.Detector
import de.fraunhofer.aisec.cpg.project.TargetEnvironment
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.name
import org.slf4j.LoggerFactory

/**
 * A tool-backed detector that emulates building a Go project using an installed Go toolchain. It
 * invokes `go list` to resolve the package dependencies of a module (currently limited to packages
 * of the standard library and vendored dependencies) and contributes their source files to the
 * component, so that dependency code is analyzed together with the module. Additionally, the
 * standard library sources (`GOROOT/src`) and a possible `vendor` folder are contributed as include
 * paths.
 *
 * In contrast to the detection built into [GoLanguage], this detector requires a `go` binary on the
 * path and is therefore not active by default; add it with
 * [de.fraunhofer.aisec.cpg.project.ProjectBuilder.detector]:
 * ```kotlin
 * project(dir) { detector(GoBuildDetector()) }
 * ```
 */
class GoBuildDetector(
    /** Additional build tags to pass to `go list` and the build constraint evaluation. */
    private val extraTags: List<String> = listOf()
) : Detector {

    /** The Go installation root, determined by `go env GOROOT`. */
    private val goRoot: File? by lazy {
        runGo(listOf("go", "env", "GOROOT"), directory = null)?.firstOrNull()?.let(::File)
    }

    override fun detect(root: Path, environment: TargetEnvironment): DetectionResult? {
        val goMod = root.resolve("go.mod")
        if (!goMod.exists()) {
            return null
        }

        val module = parseGoMod(goMod)
        val syms = symbols(module, environment)
        val topLevel = root.toFile()
        val stdLib = goRoot?.resolve("src") ?: return null
        val deps = goList(topLevel, environment) ?: return null

        log.debug("Identified {} package dependencies (stdlib only)", deps.size)

        val dirs =
            deps.mapNotNull {
                if (!it.contains(".")) {
                    stdLib.resolve(it)
                } else if (it.startsWith("vendor")) {
                    null
                } else if (module != null && it.startsWith(module.path)) {
                    topLevel.resolve(it.substringAfter(module.path))
                } else {
                    topLevel.resolve("vendor").resolve(it)
                }
            }

        var files = dirs.flatMap { gatherGoFiles(it, false) }.toMutableList()
        files += gatherGoFiles(topLevel.resolve("cmd"))
        val sources = files.filter { shouldBeBuild(it, syms) }.map(File::toPath)

        return DetectionResult(
            detector = "go-build",
            components =
                listOf(
                    ComponentDefinition(
                        name = module?.name ?: root.name,
                        root = root,
                        sources = sources,
                    )
                ),
            symbols = syms,
            includePaths =
                listOfNotNull(stdLib.toPath(), root.resolve("vendor").takeIf { it.exists() }),
            notes = listOf("using Go standard library at $stdLib"),
        )
    }

    /**
     * Derives the symbols used for build constraint evaluation: `GOOS`/`GOARCH` from the target
     * [environment] and the build tags from [extraTags] plus the Go version tags (`go1.1` ..
     * `go1.N`) based on the `go` directive of the [module].
     */
    private fun symbols(module: GoModule?, environment: TargetEnvironment): Map<String, String> {
        val tags = extraTags.toMutableList()
        module?.minorVersion?.let { minor ->
            for (i in 1..minor) {
                tags += "go1.$i"
            }
        }

        val symbols = mutableMapOf<String, String>()
        environment.os.goos?.let { symbols["GOOS"] = it }
        environment.architecture.goarch?.let { symbols["GOARCH"] = it }
        symbols["-tags"] = tags.joinToString(" ")

        return symbols
    }

    /** Invokes `go list` to gather the package dependencies of the module in [topLevel]. */
    private fun goList(topLevel: File, environment: TargetEnvironment): List<String>? {
        return runGo(
            listOf("go", "list", "-deps", extraTags.joinToString(",", "-tags="), "all"),
            directory = topLevel,
            environment = environment,
        )
    }

    /** Runs a go [command] and returns its standard output lines, or null if it failed. */
    private fun runGo(
        command: List<String>,
        directory: File?,
        environment: TargetEnvironment? = null,
    ): List<String>? {
        return try {
            val pb = ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.PIPE)
            directory?.let { pb.directory(it) }
            environment?.os?.goos?.let { pb.environment()["GOOS"] = it }
            environment?.architecture?.goarch?.let { pb.environment()["GOARCH"] = it }

            val proc = pb.start()
            proc.waitFor(5, TimeUnit.MINUTES)
            if (proc.exitValue() != 0) {
                log.warn(
                    "'{}' failed: {}",
                    command.joinToString(" "),
                    proc.errorStream.bufferedReader().readLine(),
                )
                return null
            }

            proc.inputStream.bufferedReader().readLines()
        } catch (e: Exception) {
            log.warn("Could not run '{}': {}", command.joinToString(" "), e.message)
            null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GoBuildDetector::class.java)
    }
}
