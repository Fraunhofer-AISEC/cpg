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
package de.fraunhofer.aisec.cpg.frontends.experimental.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustDestructuringTest : BaseTest() {
    @Test
    fun testDestructuring() {
        val topLevel = Path.of("src", "test", "resources", "rust", "patterns")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("destructuring.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_destructuring"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val vars = body.variables
        assertEquals(5, vars.size, "Should have five VariableDeclarations: (a,b), a, b, sum, _)")

        val a = vars["a"]
        assertNotNull(a, "Should have binding 'a'")

        val b = vars["b"]
        assertNotNull(b, "Should have binding 'b'")
    }

    @Test
    fun testDestructuringNested() {
        val topLevel = Path.of("src", "test", "resources", "rust", "patterns")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("destructuring.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_destructuring_nested"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val vars = body.variables
        assertEquals(6, vars.size,"Should have fix VariableDeclarations: (x,y,z), x, y, z, sum, _)")

        val x = vars.firstOrNull { it.name.localName == "x" }
        assertNotNull(x, "Should have binding 'x'")

        val y = vars.firstOrNull { it.name.localName == "y" }
        assertNotNull(y, "Should have binding 'y'")

        val z = vars.firstOrNull { it.name.localName == "z" }
        assertNotNull(z, "Should have binding 'z'")
    }
}
