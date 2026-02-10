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
import de.fraunhofer.aisec.cpg.graph.statements.WhileStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustControlFlowTest : BaseTest() {
    @Test
    fun testIfLet() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("control_flow.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val ifLetFunc = tu.functions["if_let"]
        assertNotNull(ifLetFunc)
        val body = ifLetFunc.body as? Block
        assertNotNull(body)

        val ifStmt = body.statements[1] as? IfStatement
        assertNotNull(ifStmt, "Expected second statement to be IfStatement")

        assertNotNull(ifStmt.condition)
        assertNotNull(ifStmt.thenStatement)

        val thenBlock = ifStmt.thenStatement as? Block
        assertNotNull(thenBlock)

        val declStmt = thenBlock.statements[0] as? DeclarationStatement
        assertNotNull(declStmt)
        val y = declStmt.declarations[0] as? VariableDeclaration
        assertEquals("y", y?.name?.localName)

        val init = y?.initializer
        assertNotNull(init)
        // We expect 'x' to be available here
        assertEquals("x", init.name.localName)
    }

    @Test
    fun testWhileLet() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("control_flow.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val whileLetFunc = tu.functions["while_let"]
        assertNotNull(whileLetFunc)
        val body = whileLetFunc.body as? Block
        assertNotNull(body)

        val whileStmt = body.statements[1] as? WhileStatement
        assertNotNull(whileStmt, "Expected second statement to be WhileStatement")

        assertNotNull(whileStmt.condition)
        assertNotNull(whileStmt.statement)

        val loopBody = whileStmt.statement as? Block
        assertNotNull(loopBody)

        val declStmt = loopBody.statements[0] as? DeclarationStatement
        assertNotNull(declStmt)
        val y = declStmt.declarations[0] as? VariableDeclaration
        assertEquals("y", y?.name?.localName)

        val init = y?.initializer
        assertNotNull(init)
        assertEquals("x", init.name.localName)
    }
}
