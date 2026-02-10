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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustFrontendTest : BaseTest() {

    @Test
    fun testHelloWorld() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("main.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val main = tu.functions["main"]
        assertNotNull(main)
        assertEquals("main", main.name.localName)

        val body = main.body as? Block
        assertNotNull(body)

        val letX = body.statements[0] as? DeclarationStatement
        assertNotNull(letX)
        val x = letX.declarations[0] as? VariableDeclaration
        assertNotNull(x)
        assertEquals("x", x.name.localName)

        val init = x.initializer as? Literal<*>
        assertNotNull(init)
        assertEquals(1L, init.value)
    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("if.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.functions["foo"]
        assertNotNull(foo)

        val body = foo.body as? Block
        assertNotNull(body)

        val ifStmt = body.statements[0] as? IfStatement
        assertNotNull(ifStmt)
        assertNotNull(ifStmt.condition)
        assertNotNull(ifStmt.thenStatement)
        assertNotNull(ifStmt.elseStatement)
    }
}
