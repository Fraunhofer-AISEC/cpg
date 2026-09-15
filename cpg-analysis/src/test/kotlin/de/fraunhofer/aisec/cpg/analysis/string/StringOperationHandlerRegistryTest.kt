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
package de.fraunhofer.aisec.cpg.analysis.string

import de.fraunhofer.aisec.cpg.analysis.string.jvm.JvmStringOperationHandler
import de.fraunhofer.aisec.cpg.analysis.string.python.PythonStringOperationHandler
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach

/**
 * [StringOperationHandlerRegistry.forLanguage] keys purely off [Language::class.simpleName] (see
 * the registry's KDoc for why), so these fakes only need to *be classes with the right simple
 * name* - reusing [TestLanguage] as a base (rather than implementing [Language] from scratch) is
 * enough, since `PythonLanguage`/`JavaLanguage` here are distinct classes from the real
 * `de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage`/
 * `de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage` (different package), yet share the exact
 * same simple name the registry keys on.
 */
private class PythonLanguage : TestLanguage()

private class JavaLanguage : TestLanguage()

private class UnregisteredTestLanguage : TestLanguage()

private class PseudoLanguageForRegistrationTest : TestLanguage()

class StringOperationHandlerRegistryTest {

    /**
     * [testRegisterAddsWithoutOverwriting] registers throwaway handlers under
     * `"PseudoLanguageForRegistrationTest"` in the process-lifetime
     * [StringOperationHandlerRegistry] singleton, which never removes entries on its own - without
     * this teardown, re-running that test in the same JVM (retries, `--rerun-tasks`, CI
     * flaky-retry) would see both this run's and every prior run's handlers accumulate under the
     * same key, and the exact-list `assertEquals` would spuriously fail.
     */
    @AfterEach
    fun tearDown() {
        StringOperationHandlerRegistry.clear("PseudoLanguageForRegistrationTest")
    }

    @Test
    fun testBuiltinsPreRegistered() {
        assertTrue(
            StringOperationHandlerRegistry.forLanguage(PythonLanguage()).any {
                it is PythonStringOperationHandler
            }
        )
        assertTrue(
            StringOperationHandlerRegistry.forLanguage(JavaLanguage()).any {
                it is JvmStringOperationHandler
            }
        )
    }

    /**
     * A language with no registered handler must yield an empty list, not throw - this is the
     * normal case for any frontend nobody has written a [StringOperationHandler] for yet (e.g. C++
     * at the time of writing).
     */
    @Test
    fun testUnregisteredLanguageReturnsEmptyList() {
        assertEquals(
            emptyList(),
            StringOperationHandlerRegistry.forLanguage(UnregisteredTestLanguage()),
        )
        // TestLanguage itself is never registered either.
        assertEquals(emptyList(), StringOperationHandlerRegistry.forLanguage(TestLanguage()))
    }

    @Test
    fun testRegisterAddsWithoutOverwriting() {
        val handlerA =
            object : StringOperationHandler {
                override fun handleCall(call: Call, evaluate: (Node) -> StringPattern) = null

                override fun handleBinaryOperator(
                    op: BinaryOperator,
                    evaluate: (Node) -> StringPattern,
                ) = null
            }
        val handlerB =
            object : StringOperationHandler {
                override fun handleCall(call: Call, evaluate: (Node) -> StringPattern) = null

                override fun handleBinaryOperator(
                    op: BinaryOperator,
                    evaluate: (Node) -> StringPattern,
                ) = null
            }

        StringOperationHandlerRegistry.register("PseudoLanguageForRegistrationTest", handlerA)
        StringOperationHandlerRegistry.register("PseudoLanguageForRegistrationTest", handlerB)

        val registered =
            StringOperationHandlerRegistry.forLanguage(PseudoLanguageForRegistrationTest())
        assertEquals(listOf(handlerA, handlerB), registered)
    }

    /**
     * Explicitly passing `operationHandlers` to [StringEvaluator] must win over whatever (if
     * anything) is registered for the node's language, exactly as before this registry existed -
     * this is what keeps the pre-existing `PythonStringOperationHandlerTest`/
     * `JvmStringOperationHandlerTest` (which construct `StringEvaluator(operationHandlers = ...)`
     * against `TestLanguage`, a language with nothing registered for it) working unchanged.
     */
    @Test
    fun testExplicitHandlersOverrideRegistry() {
        val evaluator = StringEvaluator(operationHandlers = listOf(PythonStringOperationHandler()))
        assertIs<PythonStringOperationHandler>(evaluator.operationHandlers.single())
    }
}
