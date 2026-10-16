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

import de.fraunhofer.aisec.codyze.console.emptyService
import de.fraunhofer.aisec.codyze.console.mockService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReplServiceTest {

    @Test
    fun `reports a missing analysis instead of throwing`() {
        val service = ReplService(emptyService)
        assertIs<ReplEvalResult.RuntimeError>(service.eval("1 + 1"))
    }

    @Test
    fun `evaluates values and remembers the last one`() {
        val service = ReplService(mockService)

        assertEquals(ReplEvalResult.Value("2"), service.eval("1 + 1"))
        assertEquals(2, service.lastValue)
    }

    @Test
    fun `declarations are Unit results and visible in later snippets`() {
        val service = ReplService(mockService)

        assertEquals(ReplEvalResult.UnitResult, service.eval("val n = 41"))
        // A declaration does not replace the last value
        assertNull(service.lastValue)

        assertEquals(ReplEvalResult.Value("42"), service.eval("n + 1"))
    }

    @Test
    fun `evaluates queries against the analysis result`() {
        val service = ReplService(mockService)

        assertEquals(ReplEvalResult.Value("1"), service.eval("result.functions.size"))
    }

    @Test
    fun `reports compile errors`() {
        val service = ReplService(mockService)

        val error = assertIs<ReplEvalResult.CompileError>(service.eval("undefinedName + 1"))
        assertTrue(error.message.contains("undefinedName"), error.message)
    }

    @Test
    fun `reports runtime errors and keeps the session usable`() {
        val service = ReplService(mockService)

        val error = assertIs<ReplEvalResult.RuntimeError>(service.eval("error(\"boom\")"))
        assertTrue(error.message.contains("boom"), error.message)

        assertEquals(ReplEvalResult.Value("3"), service.eval("1 + 2"))
    }

    @Test
    fun `resetting the session after a reload forgets previous declarations`() {
        val service = ReplService(mockService)
        service.eval("val n = result.functions.first()")
        service.eval("n.name.localName")

        service.resetSession()

        assertNull(service.lastValue)
        assertIs<ReplEvalResult.CompileError>(service.eval("n"))
        // The new session works as usual
        assertEquals(ReplEvalResult.Value("1"), service.eval("result.functions.size"))
    }
}
