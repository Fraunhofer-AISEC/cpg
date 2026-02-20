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
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustFrontendTest : BaseTest() {

    @Test
    fun testHelloWorld() {
        val topLevel = Path.of("src", "test", "resources", "rust", "integration")
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

        val letX = body.statements.getOrNull(0) as? DeclarationStatement
        assertNotNull(letX)
        val x = letX.declarations.getOrNull(0) as? Variable
        assertNotNull(x)
        assertEquals("x", x.name.localName)

        val init = x.initializer as? Literal<*>
        assertNotNull(init)
        assertEquals(1L, init.value)
    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("if.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.functions["foo"]
        assertNotNull(foo)

        val body = foo.body as? Block
        assertNotNull(body)

        // In Rust, `if` with an `else` clause is an expression that returns a value,
        // so it is modeled as a ConditionalExpression rather than an IfStatement.
        val condExpr = body.statements.getOrNull(0) as? ConditionalExpression
        assertNotNull(condExpr)
        assertNotNull(condExpr.condition)
        assertNotNull(condExpr.thenExpression)
        assertNotNull(condExpr.elseExpression)
    }

    @Test
    fun testExpressions() {
        val topLevel = Path.of("src", "test", "resources", "rust", "expressions")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val main = tu.functions["main"]
        assertNotNull(main)
        val body = main.body as? Block
        assertNotNull(body)

        // a = 1 + 2
        val letA = body.statements.getOrNull(0) as? DeclarationStatement
        assertNotNull(letA)
        assertFalse(letA.declarations.isEmpty(), "Declarations in letA should not be empty")
        val a = letA.declarations.getOrNull(0) as? Variable
        assertNotNull(a)
        assertIs<BinaryOperator>(a.initializer)

        // b = !true
        val letB = body.statements.getOrNull(1) as? DeclarationStatement
        assertNotNull(letB)
        assertFalse(letB.declarations.isEmpty(), "Declarations in letB should not be empty")
        val b = letB.declarations.getOrNull(0) as? Variable
        assertNotNull(b)
        assertIs<UnaryOperator>(b.initializer)

        // c = (1, 2)
        val letC = body.statements.getOrNull(2) as? DeclarationStatement
        assertNotNull(letC)
        assertFalse(letC.declarations.isEmpty(), "Declarations in letC should not be empty")
        val c = letC.declarations.getOrNull(0) as? Variable
        assertNotNull(c)
        assertIs<InitializerListExpression>(c.initializer)

        // d = [1, 2, 3]
        val letD = body.statements.getOrNull(3) as? DeclarationStatement
        assertNotNull(letD)
        assertFalse(letD.declarations.isEmpty(), "Declarations in letD should not be empty")
        val d = letD.declarations.getOrNull(0) as? Variable
        assertNotNull(d)
        assertIs<InitializerListExpression>(d.initializer)

        // x = 2
        val assignX = body.statements.getOrNull(5) as? AssignExpression
        assertNotNull(assignX)
        assertEquals("=", assignX.operatorCode)

        // x += 1
        val compoundX = body.statements.getOrNull(6) as? AssignExpression
        assertNotNull(compoundX)
        assertEquals("+=", compoundX.operatorCode)
    }

    @Test
    fun testTypes() {
        val topLevel = Path.of("src", "test", "resources", "rust", "types")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("types.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.functions["foo"]
        assertNotNull(foo)

        assertEquals("i32", foo.parameters.getOrNull(0)?.type?.name?.localName)
        assertTrue(foo.parameters.getOrNull(1)?.type?.name?.toString()?.contains("str") == true)
        assertEquals("Vec", foo.parameters.getOrNull(2)?.type?.name?.localName)
    }

    @Test
    fun testMatch() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("match.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.functions["foo"]
        assertNotNull(foo)
        val body = foo.body as? Block
        assertNotNull(body)

        val match = body.statements.getOrNull(0) as? SwitchStatement
        assertNotNull(match)
        assertEquals("x", match.selector?.name?.localName)

        val block = match.statement as? Block
        assertNotNull(block)
        // 3 arms, each with CaseStatement, value, and break = 9 statements
        assertEquals(9, block.statements.size)
    }

    @Test
    fun testCoverage() {
        val topLevel = Path.of("src", "test", "resources", "rust", "integration")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("coverage.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val myMod = tu.namespaces["my_mod"]
        assertNotNull(myMod)

        val innerFunc = myMod.functions["inner_func"]
        assertNotNull(innerFunc)
        val returnStmt = (innerFunc.body as? Block)?.statements?.getOrNull(0) as? ReturnStatement
        assertNotNull(returnStmt)
        assertNotNull(returnStmt.returnValue)
    }

    @Test
    fun testComplex1() {
        val topLevel = Path.of("src", "test", "resources", "rust", "integration")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("complex_1.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val main = tu.functions["main"]
        assertNotNull(main)

        val point = tu.records["Point"]
        assertNotNull(point)
        val pointType = point.toType()
        assertEquals(2, pointType.methods.size)

        val genericId =
            tu.declarations.filterIsInstance<FunctionTemplate>().firstOrNull {
                it.name.localName == "generic_id"
            }
        assertNotNull(genericId, "Should find a FunctionTemplateDeclaration named generic_id")
        // Check if we handled the generic parameter T
        assertEquals(1, genericId.parameters.size, "Should have 1 parameter (T)")
        assertEquals("T", genericId.parameters.getOrNull(0)?.name?.localName)

        val func = genericId.realization.getOrNull(0)
        assertNotNull(func)
        assertEquals("generic_id", func.name.localName)
    }
}
