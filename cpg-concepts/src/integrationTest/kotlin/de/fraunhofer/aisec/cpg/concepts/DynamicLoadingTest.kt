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
package de.fraunhofer.aisec.cpg.concepts

import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.memory.LoadLibrary
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.passes.concepts.memory.CXXDynamicLoadingPass
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DynamicLoadingTest {
    @Test
    fun testCXX() {
        val topLevel = File("src/integrationTest/resources/c")
        val result =
            analyze(listOf(), topLevel.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.registerPass<CXXDynamicLoadingPass>()
                it.softwareComponents(
                    mutableMapOf(
                        "main" to listOf(topLevel.resolve("main")),
                        "libexample" to listOf(topLevel.resolve("libexample")),
                    )
                )
            }
        assertNotNull(result)

        val libExample = result.components["libexample"]
        assertNotNull(libExample)

        val lib = result.variables["lib"]
        assertNotNull(lib)

        val path =
            lib.followPrevDFG { it is CallExpression && it.overlays.any { it is LoadLibrary } }
        assertNotNull(path)

        val loadLibrary =
            path.lastOrNull()?.operationNodes?.filterIsInstance<LoadLibrary>()?.singleOrNull()
        assertNotNull(loadLibrary)
        assertEquals(libExample, loadLibrary.what)

        val myFuncCall = result.calls["myfunc"]
        assertNotNull(myFuncCall)
    }
}
