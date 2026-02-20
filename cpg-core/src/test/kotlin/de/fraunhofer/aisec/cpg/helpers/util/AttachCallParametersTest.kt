/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.helpers.util

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.newCall
import de.fraunhofer.aisec.cpg.graph.newFunction
import de.fraunhofer.aisec.cpg.graph.newLiteral
import de.fraunhofer.aisec.cpg.graph.newParameter
import de.fraunhofer.aisec.cpg.graph.primitiveType
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.helpers.Util.attachCallParameters
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

class AttachCallParametersTest {
    private val frontend = TestLanguageFrontend()

    @Test
    fun testPositionalNamedAndKwargs() {
        with(frontend) {
            /** Function: default_and_variadic(a, b=False, **kwargs) * */
            val func = getFuncWithDefaultAndVariadicParameters()

            /**
             * Call: default_and_variadic("test", b=True, foo='1', bar='2')
             *
             * Case: Positional 'a', named 'b', and variadic keyword arguments*
             */
            val call =
                newCall().apply {
                    argumentEdges.add(newLiteral("test", primitiveType("string"))) {
                        this.index = 0
                    }
                    argumentEdges.add(newLiteral(true, primitiveType("boolean"))) {
                        this.name = "b"
                        this.index = 1
                    }
                    argumentEdges.add(newLiteral("1", primitiveType("string"))) {
                        this.name = "foo"
                        this.index = 2
                    }
                    argumentEdges.add(newLiteral("2", primitiveType("string"))) {
                        this.name = "bar"
                        this.index = 3
                    }
                }

            attachCallParameters(func, call)

            val aParam = func.parameters[0]
            val bParam = func.parameters[1]
            val kwargsParam = func.parameters[2]

            assertContains(
                aParam.prevDFG,
                call.argumentEdges[0].end,
                "Parameter 'a' should map to 'test'",
            )
            assertContains(
                bParam.prevDFG,
                call.argumentEdges[1].end,
                "Parameter 'b' should map to 'true'",
            )
            assertContains(
                kwargsParam.prevDFG,
                call.argumentEdges[2].end,
                "First variadic '**kwargs' parm should include 'foo=1'",
            )
            assertContains(
                kwargsParam.prevDFG,
                call.argumentEdges[3].end,
                "Second variadic '**kwargs' param should include 'bar=1'",
            )
        }
    }

    @Test
    fun testPositionalDefaultAndKwargs() {
        with(frontend) {
            /** Function: default_and_variadic(a, b=False, **kwargs) * */
            val func = getFuncWithDefaultAndVariadicParameters()

            /**
             * Call 2: default_and_variadic("test", foo='1', bar='2')
             *
             * Case: Positional 'a', default 'b', and variadic keyword arguments
             */
            val call =
                newCall().apply {
                    argumentEdges.add(newLiteral("test", primitiveType("string"))) {
                        this.index = 0
                    }
                    argumentEdges.add(newLiteral("1", primitiveType("string"))) {
                        this.name = "foo"
                        this.index = 1
                    }
                    argumentEdges.add(newLiteral("2", primitiveType("string"))) {
                        this.name = "bar"
                        this.index = 2
                    }
                }

            attachCallParameters(func, call)

            val aParam = func.parameters[0]
            val bParam = func.parameters[1]
            val bParamDefault = bParam.default
            val kwargsParam = func.parameters[2]

            assertContains(
                aParam.prevDFG,
                call.argumentEdges[0].end,
                "Parameter 'a' should map to 'test'",
            )
            assertNotNull(bParamDefault)
            assertIs<Literal<*>>(bParamDefault)
            assertContains(bParam.prevDFG, bParamDefault, "Parameter 'b' should map to 'true'")

            assertEquals(2, kwargsParam.prevDFG.size)
            assertContains(
                kwargsParam.prevDFG,
                call.argumentEdges[1].end,
                "First variadic '**kwargs' param should include 'foo=1'",
            )
            assertContains(
                kwargsParam.prevDFG,
                call.argumentEdges[2].end,
                "Second variadic '**kwargs' param should include 'bar=1'",
            )
        }
    }

    @Test
    fun testNamedParametersOnly() {
        with(frontend) {
            /** Function: default_and_variadic(a, b=False, **kwargs) * */
            val func = getFuncWithDefaultAndVariadicParameters()

            /**
             * Call: default_and_variadic(b=False, a="test")
             *
             * Case: Named 'b' and 'a', no variadic keyword arguments
             */
            val call =
                newCall().apply {
                    argumentEdges.add(newLiteral(false, primitiveType("boolean"))) {
                        this.name = "b"
                        this.index = 0
                    }
                    argumentEdges.add(newLiteral("test", primitiveType("string"))) {
                        this.name = "a"
                        this.index = 1
                    }
                }

            attachCallParameters(func, call)

            val aParam = func.parameters[0]
            val bParam = func.parameters[1]
            val kwargsParam = func.parameters[2]

            assertContains(
                aParam.prevDFG,
                call.argumentEdges[1].end,
                "Parameter 'a' should map to 'test'",
            )
            assertContains(
                bParam.prevDFG,
                call.argumentEdges[0].end,
                "Parameter 'b' should map to 'false'",
            )
            assertEquals(0, kwargsParam.prevDFG.size, "Parameter '**kwargs' should be empty")
            assertEquals(1, aParam.prevDFG.size, "Parameter 'a' should have a prevDFG")
            assertEquals(1, bParam.prevDFG.size, "Parameter 'b' should have a prevDFG")
            assertEquals(0, kwargsParam.prevDFG.size, "Parameter '**kwargs' should have no prevDFG")
        }
    }

    @Test
    fun testVariadicParamsArgsOnly() {
        with(frontend) {
            /** Function: variadic_params(a, *args, **kwargs) * */
            val func = getFuncWithArgsAndKwargs()

            /**
             * Call: variadic_params("test", "arg1", "arg2")
             *
             * Case: Positional 'a' and *args, no **kwargs
             */
            val call =
                newCall().apply {
                    argumentEdges.add(newLiteral("test", primitiveType("string"))) {
                        this.index = 0
                    }
                    argumentEdges.add(newLiteral("arg1", primitiveType("string"))) {
                        this.index = 1
                    }
                    argumentEdges.add(newLiteral("arg2", primitiveType("string"))) {
                        this.index = 2
                    }
                }
            attachCallParameters(func, call)

            val aParam = func.parameters[0]
            val argsParam = func.parameters[1]
            val kwargsParam = func.parameters[2]

            assertContains(
                aParam.prevDFG,
                call.argumentEdges[0].end,
                "Parameter 'a' should map to 'test'",
            )
            assertContains(
                argsParam.prevDFG,
                call.argumentEdges[1].end,
                "Parameter '*args' should include 'arg1'",
            )
            assertContains(
                argsParam.prevDFG,
                call.argumentEdges[2].end,
                "Parameter '*args' should include 'arg2'",
            )

            assertEquals(1, aParam.prevDFG.size)
            assertEquals(2, argsParam.prevDFG.size)
            assertEquals(0, kwargsParam.prevDFG.size)
        }
    }

    @Test
    fun testVariadicParamsKwargsOnly() {
        with(frontend) {
            /** Function: variadic_params(a, *args, **kwargs) * */
            val func = getFuncWithArgsAndKwargs()

            /**
             * Call: variadic_params("test", foo='1', bar='2')
             *
             * Case: Positional 'a', no *args only **kwargs
             */
            val call =
                newCall().apply {
                    argumentEdges.add(newLiteral("test", primitiveType("string"))) {
                        this.index = 0
                    }
                    argumentEdges.add(newLiteral("1", primitiveType("string"))) {
                        this.name = "foo"
                        this.index = 1
                    }
                    argumentEdges.add(newLiteral("2", primitiveType("string"))) {
                        this.name = "bar"
                        this.index = 2
                    }
                }
            attachCallParameters(func, call)
            val aParam = func.parameters[0]
            val argsParam = func.parameters[1]
            val kwargsParam = func.parameters[2]
            assertContains(
                aParam.prevDFG,
                call.argumentEdges[0].end,
                "Parameter 'a' should map to 'test'",
            )
            assertEquals(0, argsParam.prevDFG.size, "Parameter '*args' should be empty")
            assertContains(
                kwargsParam.prevDFG,
                call.argumentEdges[1].end,
                "Parameter '**kwargs' should include 'foo=1'",
            )
            assertContains(
                kwargsParam.prevDFG,
                call.argumentEdges[2].end,
                "Parameter '**kwargs' should include 'bar=2'",
            )
            assertEquals(1, aParam.prevDFG.size)
            assertEquals(0, argsParam.prevDFG.size)
            assertEquals(2, kwargsParam.prevDFG.size)
        }
    }

    @Test
    fun testVariadicParamsArgsAndKwargs() {
        with(frontend) {
            /** Function: variadic_params(a, *args, **kwargs) * */
            val func = getFuncWithArgsAndKwargs()

            /**
             * Call: variadic_params("test", "arg1", "arg2", foo='1', bar='2').
             *
             * Case: Positional 'a', *args, and **kwargs
             */
            val call =
                newCall().apply {
                    argumentEdges.add(newLiteral("test", primitiveType("string"))) {
                        this.index = 0
                    }
                    argumentEdges.add(newLiteral("arg1", primitiveType("string"))) {
                        this.index = 1
                    }
                    argumentEdges.add(newLiteral("arg2", primitiveType("string"))) {
                        this.index = 2
                    }
                    argumentEdges.add(newLiteral("1", primitiveType("string"))) {
                        this.name = "foo"
                        this.index = 3
                    }
                    argumentEdges.add(newLiteral("2", primitiveType("string"))) {
                        this.name = "bar"
                        this.index = 4
                    }
                }
            attachCallParameters(func, call)
            val aParam = func.parameters[0]
            val argsParam = func.parameters[1]
            val kwargsParam = func.parameters[2]
            assertContains(
                aParam.prevDFG,
                call.argumentEdges[0].end,
                "Parameter 'a' should map to 'test'",
            )
            assertContains(
                argsParam.prevDFG,
                call.argumentEdges[1].end,
                "Parameter '*args' should include 'arg1'",
            )
            assertContains(
                argsParam.prevDFG,
                call.argumentEdges[2].end,
                "Parameter '*args' should include 'arg2'",
            )
            assertContains(
                kwargsParam.prevDFG,
                call.argumentEdges[3].end,
                "Parameter '**kwargs' should include 'foo=1'",
            )
            assertContains(
                kwargsParam.prevDFG,
                call.argumentEdges[4].end,
                "Parameter '**kwargs' should include 'bar=2'",
            )
            assertEquals(1, aParam.prevDFG.size)
            assertEquals(2, argsParam.prevDFG.size)
            assertEquals(2, kwargsParam.prevDFG.size)
        }
    }

    /**
     * Returns a Function with the following parameters:
     * - 'a': A required positional parameter.
     * - 'b': An optional parameter with a default value of true.
     * - 'kwargs': A variadic parameter that captures additional keyword arguments.
     */
    private fun getFuncWithDefaultAndVariadicParameters(): Function {
        with(frontend) {
            val func = newFunction("kw_args_and_default")
            func.parameters =
                mutableListOf(
                    newParameter("a", primitiveType("string")),
                    newParameter("b", primitiveType("boolean")).apply {
                        default = newLiteral(true, primitiveType("boolean"))
                    },
                    newParameter("**kwargs").apply { isVariadic = true },
                )
            return func
        }
    }

    /**
     * Returns a Function with the following parameters:
     * - 'a': A required positional parameter.
     * - 'args': A variadic parameter that captures additional positional arguments.
     * - 'kwargs': A variadic parameter that captures additional keyword arguments.
     */
    private fun getFuncWithArgsAndKwargs(): Function {
        with(frontend) {
            val func = newFunction("variadic_params")
            func.parameters =
                mutableListOf(
                    newParameter("a", primitiveType("string")),
                    newParameter("*args", primitiveType("string")).apply { isVariadic = true },
                    newParameter("**kwargs", primitiveType("string")).apply { isVariadic = true },
                )
            return func
        }
    }
}
