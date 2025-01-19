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
import de.fraunhofer.aisec.cpg.passes.UnreachableEOGPass
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConstEvalIfTest {
    @Test
    fun testSysPlatform() {
        val topLevel = File("src/test/resources/python/consteval")
        val result =
            analyze(listOf(topLevel.resolve("platform.py")), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<UnreachableEOGPass>()
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
    }

    @Test
    fun testSysVersionInfo() {
        val topLevel = File("src/test/resources/python/consteval")
        val result =
            analyze(listOf(topLevel.resolve("version_info.py")), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<UnreachableEOGPass>()
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
            val result = it.condition?.evaluate(PythonValueEvaluator()) as? Boolean
            assertNotNull(result)
            assertTrue(result)
        }
    }
}
