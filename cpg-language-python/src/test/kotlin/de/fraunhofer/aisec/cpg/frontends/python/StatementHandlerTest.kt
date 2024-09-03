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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StatementHandlerTest {

    @Test
    fun testPosOnlyArguments() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("arguments.py").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        var myClass = result.records["MyClass"]
        assertNotNull(myClass)

        var func = result.functions["pos_only_and_args"]
        assertNotNull(func)

        val list = mapOf("a" to true, "b" to true, "c" to false)
        list.keys.forEachIndexed { idx, name ->
            var param = func.parameterEdges.firstOrNull { it.end.name.localName == name }
            assertNotNull(param, "$name should not be empty")
            if (list[name] == true) {
                assertContains(
                    param.end.modifiers,
                    PythonLanguage.MODIFIER_POSITIONAL_ONLY_ARGUMENT
                )
            }
            assertEquals(idx, param.index)
        }
    }

    @Test
    fun testVarargsArguments() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val file = topLevel.resolve("varargs.py").toFile()

        val result = analyze(listOf(file), topLevel, true) { it.registerLanguage<PythonLanguage>() }

        assertNotNull(result)

        val func = result.functions["test_varargs"]
        assertNotNull(func, "Function 'test_varargs' should be found")

        val variadicArg = func.parameters["args"]
        assertNotNull(variadicArg, "Failed to find variadic args")
        assertEquals(true, variadicArg.isVariadic)
    }

    @Test
    fun testKwOnlyArguments() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val file = topLevel.resolve("arguments.py").toFile()

        val result = analyze(listOf(file), topLevel, true) { it.registerLanguage<PythonLanguage>() }

        assertNotNull(result)

        val func = result.functions["kwd_only_arg"]
        assertNotNull(func, "Function 'kwd_only_arg' should be found")

        val kwOnlyArg = func.parameters["arg"]
        assertNotNull(kwOnlyArg, "Failed to find keyword only args")
        assertContains(kwOnlyArg.modifiers, PythonLanguage.MODIFIER_KEYWORD_ONLY_ARGUMENT)
    }

    @Test
    fun testKwDefaultArguments() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val file = topLevel.resolve("arguments.py").toFile()

        val result = analyze(listOf(file), topLevel, true) { it.registerLanguage<PythonLanguage>() }

        assertNotNull(result)

        val func = result.functions["kw_defaults"]
        assertNotNull(func, "Function 'kw_defaults' should be found")

        //        val kwOnlyArg = func.parameters["arg"]
        //        assertNotNull(kwOnlyArg, "Failed to find keyword only args")
        //        assertContains(kwOnlyArg.modifiers, PythonLanguage.MODIFIER_KEYWORD_ONLY_ARGUMENT)

        val kwOnlyParams = listOf("c", "d", "e")
        for (paramName in kwOnlyParams) {
            val param = func.parameters[paramName]
            assertNotNull(param, "Failed to find keyword-only argument '$paramName'")
            assertContains(param.modifiers, PythonLanguage.MODIFIER_KEYWORD_ONLY_ARGUMENT)
        }
    }

    @Test
    fun testKwArguments() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val file = topLevel.resolve("arguments.py").toFile()

        val result = analyze(listOf(file), topLevel, true) { it.registerLanguage<PythonLanguage>() }

        assertNotNull(result)

        val func = result.functions["kw_args"]
        assertNotNull(func, "Function 'kw_args' should be found")

        val kwArgs = func.parameters["kwargs"]
        assertNotNull(kwArgs, "Failed to find kw args")
        assertEquals(true, kwArgs.isVariadic)
    }
}
