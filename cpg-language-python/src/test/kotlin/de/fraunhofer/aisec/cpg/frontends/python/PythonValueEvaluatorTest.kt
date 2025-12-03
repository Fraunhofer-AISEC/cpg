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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.PythonUnreachableEOGPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.UnreachableEOGPass
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PythonValueEvaluatorTest {
    @Test
    fun testSysPlatform() {
        val topLevel = File("src/test/resources/python/consteval")
        val result =
            analyze(listOf(topLevel.resolve("platform.py")), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonUnreachableEOGPass>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(
                        skipUnreachableEOG = true,
                        ignoreUnreachableDeclarations = true,
                    )
                )
                it.symbols(
                    mapOf(
                        "PYTHON_PLATFORM" to "win32",
                        "PYTHON_VERSION_MAJOR" to "3",
                        "PYTHON_VERSION_MINOR" to "12",
                        "PYTHON_VERSION_MICRO" to "0",
                    )
                )
            }
        assertNotNull(result)

        val ifs = result.ifs
        ifs.forEach {
            val value = it.condition?.evaluate(PythonValueEvaluator()) as? Boolean
            assertNotNull(value)
            assertTrue(value)
        }

        val weirdCompares = result.variables["weird_compare"]?.assignments
        weirdCompares?.forEach {
            val value = it.value.evaluate(PythonValueEvaluator())
            assertEquals("{==}", value)
        }

        val theFuncCall = result.calls["the_func"]
        assertNotNull(theFuncCall)
        assertEquals(1, theFuncCall.invokes.size)
    }

    @Test
    fun testSysVersionInfo() {
        val topLevel = File("src/test/resources/python/consteval")
        val result =
            analyze(listOf(topLevel.resolve("version_info.py")), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonUnreachableEOGPass>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(
                        skipUnreachableEOG = true,
                        ignoreUnreachableDeclarations = true,
                    )
                )
                it.symbols(
                    mapOf(
                        "PYTHON_VERSION_MAJOR" to "3",
                        "PYTHON_VERSION_MINOR" to "12",
                        "PYTHON_VERSION_MICRO" to "0",
                    )
                )
            }
        assertNotNull(result)

        val ifs = result.ifs
        ifs.forEach {
            val value = it.condition?.evaluate(PythonValueEvaluator()) as? Boolean
            assertNotNull(value)
            assertTrue(value)
        }
    }

    @Test
    fun testNullSysInfo() {
        val topLevel = File("src/test/resources/python/consteval")
        val result =
            analyze(listOf(topLevel.resolve("version_info.py")), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<UnreachableEOGPass>()
            }

        val binOps = result.allChildren<BinaryOperator>()
        binOps.forEach {
            val value = it.evaluate(PythonValueEvaluator())
            assertEquals("{${it.operatorCode}}", value)
        }
    }

    @Test
    fun testArithmetic() {
        val topLevel = File("src/test/resources/python/consteval")
        val result =
            analyze(listOf(topLevel.resolve("arithmetic.py")), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<ControlFlowSensitiveDFGPass>()
            }
        assertNotNull(result)

        val b = result.variables["b"]?.firstAssignment
        assertNotNull(b)

        var value = b.evaluate(PythonValueEvaluator())
        assertEquals(12L, value)

        val c = result.variables["c"]?.firstAssignment
        assertNotNull(c)

        value = c.evaluate(PythonValueEvaluator())
        assertEquals(16.0, value)
    }
}
