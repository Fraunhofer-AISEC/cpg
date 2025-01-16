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
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ConstEvalIfTest {
    @Test
    fun testSysPlatform() {
        val topLevel = File("src/test/resources/python/consteval")
        val result =
            analyze(listOf(topLevel.resolve("platform.py")), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "win32"))
            }
        assertNotNull(result)

        val theFunc = result.calls["the_func"]
        assertNotNull(theFunc)
        assertEquals(1, theFunc.invokes.size)
    }

    @Test
    fun testSysVersionInfo() {
        val topLevel = File("src/test/resources/python/consteval")
        val result =
            analyze(listOf(topLevel.resolve("version_info.py")), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
                it.symbols(
                    mapOf(
                        "PYTHON_VERSION_MAJOR" to "3",
                        "PYTHON_VERSION_MINOR" to "12",
                        "PYTHON_VERSION_MICRO" to "0",
                    )
                )
            }
        assertNotNull(result)

        val shouldNotBeReachable = result.functions("should_not_be_reachable")
        val edges = shouldNotBeReachable.flatMap { it.astParent?.prevEOGEdges ?: listOf() }
        edges.forEach { edge -> assertEquals(true, edge.unreachable) }
    }
}
