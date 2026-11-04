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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.Cast
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.*

class StdlibProfileTest : BaseTest() {
    @Test
    fun testVaArg() {
        val file = File("src/test/resources/c/va_arg.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        // Without <stdarg.h>, va_arg(ap, int) is not an expression the parser understands, because
        // a type is not a valid argument. The profile defines it as a cast.
        assertEquals(emptyList(), tu.problems)

        val next = tu.functions["next"]
        assertNotNull(next)

        val v = next.variables["v"]
        assertNotNull(v)
        val vInit = v.initializer
        assertIs<Cast>(vInit)
        assertEquals("int", vInit.castType.name.toString())

        val s = next.variables["s"]
        assertNotNull(s)
        val sInit = s.initializer
        assertIs<Cast>(sInit)
        assertEquals("char*", sInit.castType.name.toString())
    }
}
