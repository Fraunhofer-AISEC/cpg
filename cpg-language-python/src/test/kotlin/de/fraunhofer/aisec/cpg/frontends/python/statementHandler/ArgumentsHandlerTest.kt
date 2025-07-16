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
package de.fraunhofer.aisec.cpg.frontends.python.statementHandler

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.dParameters
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
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArgumentsHandlerTest {

    private lateinit var topLevel: Path
    private lateinit var result: TranslationResult

    @BeforeAll
    fun setup() {
        topLevel = Path.of("src", "test", "resources", "python")
        analyzeFile()
    }

    fun analyzeFile() {
        result =
            analyze(listOf(topLevel.resolve("arguments.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
    }

    @Test
    fun testPosOnlyArguments() {
        val func = result.dFunctions["pos_only_and_args"]
        assertNotNull(func)

        val list = mapOf("a" to true, "b" to true, "c" to false)
        list.keys.forEachIndexed { idx, name ->
            val param = func.parameterEdges.firstOrNull { it.end.name.localName == name }
            assertNotNull(param, "$name should not be empty")
            if (list[name] == true) {
                assertContains(
                    param.end.modifiers,
                    PythonLanguage.Companion.MODIFIER_POSITIONAL_ONLY_ARGUMENT,
                )
            }
            assertEquals(idx, param.index)
        }
    }

    @Test
    fun testVarargsArguments() {
        val func = result.dFunctions["test_varargs"]
        assertNotNull(func, "Function 'test_varargs' should be found")

        val variadicArg = func.parameters["args"]
        assertNotNull(variadicArg, "Failed to find variadic args")
        assertEquals(true, variadicArg.isVariadic)
    }

    @Test
    fun testKwOnlyArguments() {
        val func = result.dFunctions["kwd_only_arg"]
        assertNotNull(func, "Function 'kwd_only_arg' should be found")

        val kwOnlyArg = func.parameters["arg"]
        assertNotNull(kwOnlyArg, "Failed to find keyword only args")
        assertContains(kwOnlyArg.modifiers, PythonLanguage.Companion.MODIFIER_KEYWORD_ONLY_ARGUMENT)
    }

    @Test
    fun testKwDefaultArguments() {
        val func = result.dFunctions["kw_defaults"]
        assertNotNull(func, "Function 'kw_defaults' should be found")

        assertEquals(4, func.parameters.size)
        val kwOnlyParams = mapOf("c" to 2, "d" to null, "e" to 3)

        for ((paramName, expectedDefaultValue) in kwOnlyParams) {
            val param = func.parameters[paramName]
            assertNotNull(param, "Failed to find keyword-only argument '$paramName'")

            assertContains(param.modifiers, PythonLanguage.MODIFIER_KEYWORD_ONLY_ARGUMENT)

            if (expectedDefaultValue != null) {
                assertNotNull(param.default, "Parameter '$paramName' should have a default value")
                assertEquals(
                    expectedDefaultValue.toLong(),
                    param.default?.evaluate(),
                    "Default value for parameter '$paramName' is incorrect",
                )
            } else {
                assertNull(param.default, "Parameter '$paramName' should not have a default value")
            }
        }
    }

    @Test
    fun testDefaultsArguments() {
        val func = result.dFunctions["defaults"]
        assertNotNull(func, "Function 'defaults' should be found")

        assertEquals(4, func.parameters.size)
        val defaults = mapOf("b" to 1, "c" to 2)
        for ((paramName, expectedDefaultValue) in defaults) {
            val param = func.parameters[paramName]
            assertNotNull(param, "Failed to find keyword-only argument '$paramName'")

            assertNotNull(param.default, "Parameter '$paramName' should have a default value")
            assertEquals(
                expectedDefaultValue.toLong(),
                param.default?.evaluate(),
                "Default value for parameter '$paramName' is incorrect",
            )
        }
    }

    @Test
    fun testReceiverWithDefault() {
        val func = result.dFunctions["my_method"]
        assertNotNull(func, "Function 'my_method' should be found")

        assertEquals(2, func.parameters.size)
        assertNotNull(func.dMethods[0].receiver)

        val parameterD = func.parameters["d"]
        assertNotNull(parameterD?.default, "Expected the parameter `d` to have a default value.")
        assertEquals(3.toLong(), parameterD.default?.evaluate())

        val parameterE = func.parameters["e"]
        assertNotNull(parameterE?.default, "Expected the parameter `e` to have a default value.")
        assertEquals(1.toLong(), parameterE.default?.evaluate())
    }

    @Test
    fun testKwArguments() {
        val func = result.dFunctions["kw_args"]
        assertNotNull(func, "Function 'kw_args' should be found")

        val kwArgs = func.parameters["kwargs"]
        assertNotNull(kwArgs, "Failed to find kw args")
        assertEquals(true, kwArgs.isVariadic)
    }

    @Test
    fun testMethodDefaults() {
        val func = result.dFunctions["method_with_some_defaults"]

        assertEquals(3, func.dParameters.size)
        val parameterA = func.dParameters["a"]
        val parameterB = func.dParameters["b"]
        val parameterC = func.dParameters["c"]
        assertNotNull(
            parameterA,
            "Failed to find parameter `a` -> cannot test for non-existing default value.",
        )
        assertNull(parameterA.default, "Expected the parameter `a` to not have a default value.")

        assertNotNull(parameterB?.default, "Expected the parameter `b` to have a default value.")
        assertEquals(1.toLong(), parameterB.default?.evaluate())
        assertNotNull(parameterC?.default, "Expected the parameter `c` to have a default value.")
        assertEquals(2.toLong(), parameterC.default?.evaluate())
    }

    @Test
    fun testSignatureMatch() {
        val func = result.dFunctions["kw_args_and_default"]
        assertNotNull(func)
        val funcDefault = func.parameters[1]
        val funcKwargs = func.parameters[2]

        val call = result.dMethods["call"]?.dCalls?.firstOrNull()
        assertNotNull(call)
        assertContains(func.nextDFG, call)

        val call2 = result.dMethods["call2"]?.dCalls?.firstOrNull()
        assertNotNull(call2)
        assertContains(call2.invokes, func)
        assertEquals(2, call2.arguments.size)

        val defaultArg2 = call2.arguments[1]
        assertContains(defaultArg2.nextDFG, funcDefault)

        val call3 = result.dMethods["call3"]?.dCalls?.firstOrNull()
        assertNotNull(call3)

        listOf("foo", "bar", "baz").forEach { argName ->
            val arg = call3.argumentEdges.find { it.name == argName }?.end
            assertNotNull(arg)
            assertContains(arg.nextDFG, funcKwargs)
        }

        val call4 = result.dMethods["call4"]?.dCalls?.firstOrNull()
        assertNotNull(call4)
        val defaultArg4 = call4.arguments[1]
        assertNotNull(defaultArg4)
        assertContains(defaultArg4.nextDFG, funcDefault)
    }
}
