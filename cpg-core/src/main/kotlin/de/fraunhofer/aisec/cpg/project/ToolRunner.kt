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

import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempFile
import org.slf4j.LoggerFactory

/**
 * Runs an external tool on behalf of a tool-backed [Detector], e.g., to resolve or fetch the
 * dependencies of a project. It is an interface so that detectors can be tested without the tool
 * being installed.
 */
fun interface ToolRunner {
    /**
     * Runs [command] in [directory] (or the current directory, if it is null) with the additional
     * environment variables [environment] and returns the lines it wrote to its standard output, or
     * null if it could not be started, did not finish in time or finished unsuccessfully.
     */
    fun run(
        command: List<String>,
        directory: Path?,
        environment: Map<String, String>,
    ): List<String>?
}

/**
 * Runs [command] in [directory] (or the current directory, if it is null), see [ToolRunner.run].
 */
fun ToolRunner.run(command: List<String>, directory: Path? = null): List<String>? =
    run(command, directory, mapOf())

/** A [ToolRunner] that starts a process on the host and gives up after [timeout]. */
class ProcessToolRunner(private val timeout: Duration = Duration.ofMinutes(10)) : ToolRunner {
    override fun run(
        command: List<String>,
        directory: Path?,
        environment: Map<String, String>,
    ): List<String>? {
        // The output goes to files, so that a full pipe cannot block the process and the timeout
        // below is enforced even if the process hangs
        var stdout: File? = null
        var stderr: File? = null
        return try {
            stdout = createTempFile("tool", ".out").toFile()
            stderr = createTempFile("tool", ".err").toFile()
            val pb =
                ProcessBuilder(command).redirectOutput(stdout).redirectError(stderr).also {
                    directory?.let { dir -> it.directory(dir.toFile()) }
                    it.environment().putAll(environment)
                }

            val process = pb.start()
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                // The tool may have started processes of its own, which must be collected before
                // their parent is gone
                process.descendants().toList().forEach { it.destroyForcibly() }
                process.destroyForcibly()
                log.warn("'{}' timed out after {}", command.joinToString(" "), timeout)
                return null
            }
            if (process.exitValue() != 0) {
                log.warn(
                    "'{}' failed: {}",
                    command.joinToString(" "),
                    stderr.readLines().firstOrNull(),
                )
                return null
            }

            stdout.readLines()
        } catch (e: Exception) {
            log.warn("Could not run '{}': {}", command.joinToString(" "), e.message)
            null
        } finally {
            stdout?.delete()
            stderr?.delete()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProcessToolRunner::class.java)
    }
}
