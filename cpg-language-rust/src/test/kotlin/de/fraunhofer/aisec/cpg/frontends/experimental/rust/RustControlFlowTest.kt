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
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
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

        val ifStmt = body.statements.getOrNull(1) as? IfStatement
        assertNotNull(ifStmt, "Expected second statement to be IfStatement")

        assertNotNull(ifStmt.condition)
        assertNotNull(ifStmt.thenStatement)

        val thenBlock = ifStmt.thenStatement as? Block
        assertNotNull(thenBlock)

        // Statement 0: Binding for x
        val declX = thenBlock.statements.getOrNull(0) as? DeclarationStatement
        assertNotNull(declX)
        val xVar = declX.declarations[0] as? VariableDeclaration
        assertEquals("x", xVar?.name?.localName)

        // Statement 1: let y = x
        val declStmt = thenBlock.statements.getOrNull(1) as? DeclarationStatement
        assertNotNull(declStmt)
        val y = declStmt.declarations[0] as? VariableDeclaration
        assertEquals("y", y?.name?.localName)

        val init = y?.initializer
        assertNotNull(init)
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

        val whileStmt = body.statements.getOrNull(1) as? WhileStatement
        assertNotNull(whileStmt, "Expected second statement to be WhileStatement")

        assertNotNull(whileStmt.condition)
        assertNotNull(whileStmt.statement)

        val loopBody = whileStmt.statement as? Block
        assertNotNull(loopBody)

        // Statement 0: Binding for x
        val declX = loopBody.statements.getOrNull(0) as? DeclarationStatement
        assertNotNull(declX)
        val xVar = declX.declarations[0] as? VariableDeclaration
        assertEquals("x", xVar?.name?.localName)

        // Statement 1: let y = x
        val declStmtLocal = loopBody.statements.getOrNull(1) as? DeclarationStatement
        assertNotNull(declStmtLocal)
        val y = declStmtLocal.declarations[0] as? VariableDeclaration
        assertEquals("y", y?.name?.localName)

        val init = y?.initializer
        assertNotNull(init)
        assertEquals("x", init.name.localName)
    }

    @Test
    fun testLoopLabels() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("loop_labels.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["loop_labels"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // Outer loop (loop { ... })
        // Mapped to LabelStatement -> WhileStatement
        val outerLabel = body.statements.getOrNull(0) as? LabelStatement
        assertNotNull(outerLabel, "Expected outer to be LabelStatement")
        assertEquals("outer", outerLabel.label)

        val outerLoop = outerLabel.subStatement as? WhileStatement
        assertNotNull(outerLoop, "Expected outer loop to be WhileStatement")

        val innerBlock = outerLoop.statement as? Block
        assertNotNull(innerBlock)

        // Inner loop (while true { ... })
        val innerLabel = innerBlock.statements.getOrNull(0) as? LabelStatement
        assertNotNull(innerLabel, "Expected inner to be LabelStatement")
        assertEquals("inner", innerLabel.label)

        val innerLoop = innerLabel.subStatement as? WhileStatement
        assertNotNull(innerLoop, "Expected inner loop to be WhileStatement")

        val innerBody = innerLoop.statement as? Block
        assertNotNull(innerBody, "Expected inner body to be Block")

        val breakStmt = innerBody.statements.getOrNull(0) as? BreakStatement
        assertNotNull(breakStmt, "Expected break statement")
        assertEquals("outer", breakStmt.label)
    }

    @Test
    fun testAsync() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("async.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val asyncFn = tu.functions["async_fn"]
        assertNotNull(asyncFn)
        assertTrue(asyncFn.annotations.any { it.name.localName == "Async" })

        val caller = tu.functions["caller"]
        assertNotNull(caller)
        assertTrue(caller.annotations.any { it.name.localName == "Async" })

        val body = caller.body as? Block
        assertNotNull(body)

        val expr = body.statements.getOrNull(0)
        val awaitExpr = expr as? UnaryOperator
        assertNotNull(awaitExpr)
        assertEquals("await", awaitExpr.operatorCode)
        assertTrue(awaitExpr.isPostfix)

        val call = awaitExpr.input as? CallExpression
        assertNotNull(call)
        assertEquals("async_fn", call.name.localName)
    }
}
