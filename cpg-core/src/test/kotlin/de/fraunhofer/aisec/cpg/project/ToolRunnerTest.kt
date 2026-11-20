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

import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir

class ToolRunnerTest {
    private val posix = !System.getProperty("os.name").lowercase().contains("windows")

    @Test
    fun testReturnsStandardOutputOnly() {
        assumeTrue(posix)

        val output =
            ProcessToolRunner().run(listOf("sh", "-c", "echo one; echo ignored >&2; echo two"))

        // The standard error output must not be mixed into the result
        assertEquals(listOf("one", "two"), output)
    }

    @Test
    fun testFailingCommandReturnsNull() {
        assumeTrue(posix)

        assertNull(ProcessToolRunner().run(listOf("sh", "-c", "echo out; echo broken >&2; exit 3")))
    }

    @Test
    fun testCommandThatDoesNotExistReturnsNull() {
        assertNull(ProcessToolRunner().run(listOf("cpg-tool-that-does-not-exist")))
    }

    @Test
    fun testRunsInDirectory(@TempDir tmp: Path) {
        assumeTrue(posix)
        tmp.resolve("marker").writeText("")

        assertEquals(listOf(), ProcessToolRunner().run(listOf("sh", "-c", "test -f marker"), tmp))
        assertNull(ProcessToolRunner().run(listOf("sh", "-c", "test -f marker")))
    }

    @Test
    fun testEnvironment() {
        assumeTrue(posix)

        val output =
            ProcessToolRunner()
                .run(
                    listOf("sh", "-c", "echo \$CPG_TEST_VALUE"),
                    directory = null,
                    environment = mapOf("CPG_TEST_VALUE" to "42"),
                )

        assertEquals(listOf("42"), output)
    }

    @Test
    fun testLotsOfOutputDoesNotBlock() {
        assumeTrue(posix)

        // More than a pipe buffer, which would block the process if the output was not consumed
        val output =
            ProcessToolRunner().run(listOf("sh", "-c", "head -c 1000000 /dev/zero | tr '\\0' x"))

        assertEquals(1, output?.size)
        assertEquals(1_000_000, output?.single()?.length)
    }

    @Test
    fun testTimeout(@TempDir tmp: Path) {
        assumeTrue(posix)

        val runner = ProcessToolRunner(timeout = Duration.ofMillis(300))
        val started = System.nanoTime()

        assertNull(runner.run(listOf("sh", "-c", "sleep 30; touch ${tmp.resolve("finished")}")))
        assertTrue(Duration.ofNanos(System.nanoTime() - started) < Duration.ofSeconds(20))
        assertFalse(tmp.resolve("finished").exists())
    }

    @Test
    fun testTimeoutAlsoStopsProcessesOfTheTool(@TempDir tmp: Path) {
        assumeTrue(posix)
        val pidFile = tmp.resolve("pid")

        // The shell starts a child and waits for it, so killing the shell alone would orphan it
        val runner = ProcessToolRunner(timeout = Duration.ofMillis(500))
        assertNull(runner.run(listOf("sh", "-c", "sleep 30 & echo \$! > $pidFile; wait")))

        val pid = pidFile.readText().trim().toLong()
        val deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos()
        while (
            ProcessHandle.of(pid).map { it.isAlive }.orElse(false) && System.nanoTime() < deadline
        ) {
            Thread.sleep(50)
        }
        assertFalse(ProcessHandle.of(pid).map { it.isAlive }.orElse(false))
    }
}
