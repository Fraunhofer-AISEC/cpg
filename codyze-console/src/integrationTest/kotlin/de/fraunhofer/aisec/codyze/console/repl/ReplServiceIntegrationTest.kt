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
package de.fraunhofer.aisec.codyze.console.repl

import de.fraunhofer.aisec.codyze.console.AnalyzeRequestJSON
import de.fraunhofer.aisec.codyze.console.ConsoleService
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * Exercises [ReplService] against a real analyzed [de.fraunhofer.aisec.cpg.TranslationResult] and a
 * real Kotlin scripting compiler/evaluator, since both are what actually drive the REPL's behavior
 * and a mock would not catch snippet-chaining or session-reset regressions.
 */
class ReplServiceIntegrationTest {

    private val sourceDir =
        "../codyze-compliance/src/integrationTest/resources/demo-app/components/auth/auth"
    private val topLevel =
        "../codyze-compliance/src/integrationTest/resources/demo-app/components/auth"

    private fun analyzedReplService(): ReplService {
        val consoleService = ConsoleService()
        runBlocking {
            consoleService.analyze(AnalyzeRequestJSON(sourceDir = sourceDir, topLevel = topLevel))
        }
        return ReplService(consoleService)
    }

    @Test
    fun `eval handles declarations, values, Unit, and compile-time and runtime errors`() {
        val repl = analyzedReplService()

        // A value-producing expression renders a Value and updates lastValue.
        val countResult = repl.eval("result.components.size")
        assertIs<ReplEvalResult.Value>(countResult)
        assertTrue(repl.lastValue is Int)

        // A declaration alone produces Unit, not a Value.
        assertIs<ReplEvalResult.UnitResult>(repl.eval("val tracked = result.components.size"))

        // Declarations persist across snippets — this is what makes the REPL a REPL. If a later
        // :reload didn't reset the compiler/evaluator chain, `tracked` would also keep resolving
        // after the TranslationResult it captured was replaced (see the reset test below).
        val trackedResult = repl.eval("tracked + 1")
        assertIs<ReplEvalResult.Value>(trackedResult)

        // Invalid syntax is a compile error, not an exception thrown out of eval().
        assertIs<ReplEvalResult.CompileError>(repl.eval("val x = "))

        // An exception at runtime is a RuntimeError, not an exception thrown out of eval().
        assertIs<ReplEvalResult.RuntimeError>(repl.eval("""throw RuntimeException("boom")"""))
    }

    @Test
    fun `resetSession discards prior declarations so they no longer resolve`() {
        val repl = analyzedReplService()

        assertIs<ReplEvalResult.UnitResult>(repl.eval("val n = result.components.size"))
        assertIs<ReplEvalResult.Value>(repl.eval("n"))

        repl.resetSession()
        assertNull(repl.lastValue)

        // `n` was declared in the discarded compiler/evaluator chain — referencing it now must be
        // a compile error (unresolved reference), not a stale value from before the reset.
        assertIs<ReplEvalResult.CompileError>(repl.eval("n"))

        // The session is still otherwise usable after a reset.
        assertIs<ReplEvalResult.Value>(repl.eval("result.components.size"))
    }
}
