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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.test.analyze
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import org.junit.jupiter.api.io.TempDir

class RunOnSessionTest {

    @AfterTest
    fun dropSessions() {
        // The sessions are global; don't leave this test's behind.
        analysisSessions.clear()
    }

    private fun analyzeSnippet(dir: Path): TranslationResult {
        val source = dir.resolve("main.c")
        source.writeText("int main() { return 0; }")
        return analyze(listOf(source.toFile()), dir, true) { it.registerLanguage<CLanguage>() }
    }

    /**
     * A re-analysis replaces the project's session while holding the old one's write lock. A call
     * waiting for that lock meanwhile must run on the new session, not on the stale one.
     */
    @Test
    fun aCallThatWaitedForAReplacedSessionRunsOnTheNewOne(@TempDir dir: Path) {
        analysisSessions.clear()
        val result = analyzeSnippet(dir)
        registerSession("project", result)
        val old = assertNotNull(getSession("project"))

        val lockHeld = CountDownLatch(1)
        val release = CountDownLatch(1)
        val holder = thread {
            old.lock.write {
                lockHeld.countDown()
                release.await()
            }
        }
        lockHeld.await()
        val used = AtomicReference<CpgSession>()
        val caller = thread {
            CpgProjectPayload("project").runOnSession { session, _ ->
                used.set(session)
                CallToolResult(content = emptyList())
            }
        }
        // Wait until the call blocks on the old session's lock.
        val deadline = System.nanoTime() + 10_000_000_000L
        while (caller.state != Thread.State.WAITING) {
            check(System.nanoTime() < deadline) { "the call never waited for the lock" }
            Thread.sleep(1)
        }

        registerSession("project", result)
        val replacement = assertNotNull(getSession("project"))
        release.countDown()
        holder.join()
        caller.join()

        assertNotSame(old, replacement)
        assertSame(replacement, used.get())
    }

    @Test
    fun withoutASessionTheCallSaysWhy() {
        analysisSessions.clear()

        val result = CpgProjectPayload("project").runOnSession { _, _ -> error("not called") }

        assertContains(
            (result.content.single() as TextContent).text,
            "No project has been analyzed yet",
        )
    }
}
