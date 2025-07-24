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
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TypeTest {
    @Test
    fun testDFGBasedType() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("dfg_type.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val my = result.allVariables["my"]
        assertNotNull(my)

        val funcBar = result.allFunctions["bar"]
        assertNotNull(funcBar)
        assertEquals(
            setOf("bar()dynamic", "bar()dfg_type.Bar"),
            funcBar.assignedTypes.map { it.typeName }.toSet(),
        )

        val barCall = result.allMCalls["bar"]
        assertNotNull(barCall)
        assertEquals(setOf("dfg_type.Bar"), barCall.assignedTypes.map { it.typeName }.toSet())

        val bar = result.allVariables["bar"]
        assertNotNull(bar)
        assertEquals(setOf("dfg_type.Bar"), bar.assignedTypes.map { it.typeName }.toSet())

        val a = result.allVariables["a"]
        assertNotNull(a)
        assertEquals(setOf("str", "int"), a.assignedTypes.map { it.typeName }.toSet())
    }
}
