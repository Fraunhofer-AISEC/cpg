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

import de.fraunhofer.aisec.codyze.console.CpgQueryScript
import de.fraunhofer.aisec.codyze.console.emptyService
import de.fraunhofer.aisec.codyze.console.mockService
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReplServiceTest {

    @Test
    fun testEvalWithoutAnalysis() {
        val service = ReplService(emptyService)
        assertIs<ReplEvalResult.RuntimeError>(service.eval("1 + 1"))
    }

    @Test
    fun testEvalValue() {
        val service = ReplService(mockService)

        assertEquals(ReplEvalResult.Value("2"), service.eval("1 + 1"))
        assertEquals(2, service.lastValue)
    }

    @Test
    fun testEvalDeclaration() {
        val service = ReplService(mockService)

        assertEquals(ReplEvalResult.UnitResult, service.eval("val n = 41"))
        // A declaration does not replace the last value
        assertNull(service.lastValue)

        assertEquals(ReplEvalResult.Value("42"), service.eval("n + 1"))
    }

    @Test
    fun testEvalQuery() {
        val service = ReplService(mockService)

        assertEquals(ReplEvalResult.Value("1"), service.eval("result.functions.size"))
    }

    @Test
    fun testDefaultImports() {
        val service = ReplService(mockService)

        // One type from each default-imported package, so a moved package fails here instead of
        // silently dropping out of scope.
        assertIs<ReplEvalResult.UnitResult>(
            service.eval(
                "val types = listOf(TranslationResult::class, Node::class, Function::class, " +
                    "Call::class, ObjectType::class, QueryTree::class)"
            )
        )
        assertEquals(ReplEvalResult.Value("6"), service.eval("types.size"))
    }

    @Test
    fun testReplOnlyImports() {
        // The REPL helpers are in scope in the REPL, and :imports lists them ...
        val service = ReplService(mockService)
        assertIs<ReplEvalResult.UnitResult>(service.eval("val f = ::openDFG"))
        assertTrue(
            service.defaultImports.any { it.endsWith(".openDFG") },
            "${service.defaultImports}",
        )

        // ... but not in the query scripts of the web console.
        val queryImports =
            createJvmCompilationConfigurationFromTemplate<CpgQueryScript>()[
                    ScriptCompilationConfiguration.defaultImports]
                .orEmpty()
        assertTrue(queryImports.none { it.contains("openDFG") }, queryImports.toString())
        assertTrue(service.defaultImports.containsAll(queryImports))
    }

    @Test
    fun testCompileError() {
        val service = ReplService(mockService)

        val error = assertIs<ReplEvalResult.CompileError>(service.eval("undefinedName + 1"))
        assertTrue(error.message.contains("undefinedName"), error.message)
    }

    @Test
    fun testRuntimeError() {
        val service = ReplService(mockService)

        val error = assertIs<ReplEvalResult.RuntimeError>(service.eval("error(\"boom\")"))
        assertTrue(error.message.contains("boom"), error.message)

        assertEquals(ReplEvalResult.Value("3"), service.eval("1 + 2"))
    }

    @Test
    fun testResetSession() {
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
