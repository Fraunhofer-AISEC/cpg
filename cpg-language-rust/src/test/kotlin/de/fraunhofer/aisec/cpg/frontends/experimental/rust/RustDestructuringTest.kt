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
    fun testTupleDestructuring() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("destructuring.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_tuple_destructuring"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val tuples = body.allChildren<TupleDeclaration>()
        assertEquals(1, tuples.size, "Should have one TupleDeclaration")

        val tuple = tuples.first()
        assertEquals(2, tuple.elements.size, "Tuple should have 2 elements")
        assertEquals("a", tuple.elements.getOrNull(0)?.name?.localName)
        assertEquals("b", tuple.elements.getOrNull(1)?.name?.localName)
        assertNotNull(tuple.initializer, "Tuple should have an initializer")
    }

    @Test
    fun testSimpleLetStillWorks() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("destructuring.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_simple_let"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val vars = body.allChildren<VariableDeclaration>()
        val x = vars.firstOrNull { it.name.localName == "x" }
        assertNotNull(x, "Simple let should still create VariableDeclaration")

        val literal = x.initializer as? Literal<*>
        assertNotNull(literal, "x should have a literal initializer")
        assertEquals(42L, literal.value)
    }
}
