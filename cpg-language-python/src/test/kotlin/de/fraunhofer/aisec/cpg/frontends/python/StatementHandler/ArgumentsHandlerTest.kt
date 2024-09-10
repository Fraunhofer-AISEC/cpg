/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.python.StatementHandler

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeAll

class ArgumentsHandlerTest {

    companion object {
        private lateinit var topLevel: Path
        private lateinit var result: TranslationResult

        @JvmStatic
        @BeforeAll
        fun setup() {
            topLevel = Path.of("src", "test", "resources", "python")
            analyzeFile("arguments.py")
        }

        fun analyzeFile(fileName: String) {
            result =
                analyze(listOf(topLevel.resolve(fileName).toFile()), topLevel, true) {
                    it.registerLanguage<PythonLanguage>()
                }
            assertNotNull(result)
        }
    }

    @Test
    fun testPosOnlyArguments() {
        var func = result.functions["pos_only_and_args"]
        assertNotNull(func)

        val list = mapOf("a" to true, "b" to true, "c" to false)
        list.keys.forEachIndexed { idx, name ->
            var param = func.parameterEdges.firstOrNull { it.end.name.localName == name }
            assertNotNull(param, "$name should not be empty")
            if (list[name] == true) {
                assertContains(
                    param.end.modifiers,
                    PythonLanguage.Companion.MODIFIER_POSITIONAL_ONLY_ARGUMENT
                )
            }
            assertEquals(idx, param.index)
        }
    }

    @Test
    fun testVarargsArguments() {
        var func = result.functions["test_varargs"]
        assertNotNull(func, "Function 'test_varargs' should be found")

        var variadicArg = func.parameters["args"]
        assertNotNull(variadicArg, "Failed to find variadic args")
        assertEquals(true, variadicArg.isVariadic)
    }

    @Test
    fun testKwOnlyArguments() {
        var func = result.functions["kwd_only_arg"]
        assertNotNull(func, "Function 'kwd_only_arg' should be found")

        var kwOnlyArg = func.parameters["arg"]
        assertNotNull(kwOnlyArg, "Failed to find keyword only args")
        assertContains(kwOnlyArg.modifiers, PythonLanguage.Companion.MODIFIER_KEYWORD_ONLY_ARGUMENT)
    }

    @Test
    fun testKwDefaultArguments() {
        var func = result.functions["kw_defaults"]
        assertNotNull(func, "Function 'kw_defaults' should be found")

        var kwOnlyParams = mapOf("c" to 2, "d" to null, "e" to 3)

        for ((paramName, expectedDefaultValue) in kwOnlyParams) {
            var param = func.parameters[paramName]
            assertNotNull(param, "Failed to find keyword-only argument '$paramName'")

            assertContains(param.modifiers, PythonLanguage.MODIFIER_KEYWORD_ONLY_ARGUMENT)

            if (expectedDefaultValue != null) {
                assertNotNull(param.default, "Parameter '$paramName' should have a default value")
                assertEquals(
                    expectedDefaultValue.toLong(),
                    param.default?.evaluate(),
                    "Default value for parameter '$paramName' is incorrect"
                )
            } else {
                assertNull(param.default, "Parameter '$paramName' should not have a default value")
            }
        }
    }

    @Test
    fun testDefaultsArguments() {
        var func = result.functions["defaults"]
        assertNotNull(func, "Function 'defaults' should be found")

        var defaults = mapOf("b" to 1, "c" to 2)
        for ((paramName, expectedDefaultValue) in defaults) {
            var param = func.parameters[paramName]
            assertNotNull(param, "Failed to find keyword-only argument '$paramName'")

            assertContains(param.modifiers, PythonLanguage.MODIFIER_KEYWORD_ONLY_ARGUMENT)

            assertNotNull(param.default, "Parameter '$paramName' should have a default value")
            assertEquals(
                expectedDefaultValue.toLong(),
                param.default?.evaluate(),
                "Default value for parameter '$paramName' is incorrect"
            )
        }
    }

    @Test
    fun testNonAndDefaultArguments() {
        var func = result.functions["foo"]
        assertNotNull(func, "Function 'foo' should be found")

        var defaults = mapOf("a" to null, "b" to null, "c" to 3, "d" to 4)
        for ((paramName, expectedDefaultValue) in defaults) {
            var param = func.parameters[paramName]
            assertNotNull(param, "Failed to find argument '$paramName'")

            expectedDefaultValue?.let {
                assertContains(param.modifiers, PythonLanguage.MODIFIER_KEYWORD_ONLY_ARGUMENT)
                assertNotNull(param.default, "Parameter '$paramName' should have a default value")
                assertEquals(
                    expectedDefaultValue.toLong(),
                    param.default?.evaluate(),
                    "Default value for parameter '$paramName' is incorrect"
                )
            }
        }
    }

    @Test
    fun testDefaultsArgumentsWithReceiver() {
        var func = result.functions["bar"]
        assertNotNull(func, "Function 'bar' should be found")

        var defaults = mapOf("b" to 1, "c" to 2)
        for ((paramName, expectedDefaultValue) in defaults) {
            var param = func.parameters[paramName]
            assertNotNull(param, "Failed to find keyword-only argument '$paramName'")

            assertNotNull(param.default, "Parameter '$paramName' should have a default value")
            assertEquals(
                expectedDefaultValue.toLong(),
                param.default?.evaluate(),
                "Default value for parameter '$paramName' is incorrect"
            )
        }
    }

    @Test
    fun testKwArguments() {
        var func = result.functions["kw_args"]
        assertNotNull(func, "Function 'kw_args' should be found")

        var kwArgs = func.parameters["kwargs"]
        assertNotNull(kwArgs, "Failed to find kw args")
        assertEquals(false, kwArgs.isVariadic)
    }

    @Test
    fun testMethodDefaults() {
        val func = result.functions["method_with_some_defaults"]

        assertEquals(3, func.parameters.size)
        val parameterA = func.parameters["a"]
        val parameterB = func.parameters["b"]
        val parameterC = func.parameters["c"]
        assertNull(parameterA?.default, "Expected the parameter `a` to not have a default value.")
        assertNotNull(parameterB?.default, "Expected the parameter `b` to have a default value.")
        assertEquals(1.toLong(), parameterB.default?.evaluate())
        assertNotNull(parameterC?.default, "Expected the parameter `c` to have a default value.")
        assertEquals(2.toLong(), parameterB.default?.evaluate())
    }
}
