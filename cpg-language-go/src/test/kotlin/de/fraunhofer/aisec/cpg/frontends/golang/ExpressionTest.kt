/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.*

class ExpressionTest {

    @Test
    fun testCast() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("type_assert.go").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val main = tu.namespaces["main"]
        assertNotNull(main)

        val mainFunc = main.functions["main"]
        assertNotNull(mainFunc)

        val f = mainFunc.variables["f"]
        assertNotNull(f)

        val s = mainFunc.variables["s"]
        assertNotNull(s)

        val cast = s.initializer as? Cast
        assertNotNull(cast)
        assertFullName("main.MyStruct", cast.castType)
        assertSame(f, (cast.expression as? Reference)?.refersTo)

        val ignored = main.variables("_")
        ignored.forEach { assertIs<Cast>(it.initializer) }
    }

    @Test
    fun testSliceExpression() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("slices.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val a = tu.variables["a"]
        assertNotNull(a)
        assertLocalName("int[]", a.type)

        val b = tu.variables["b"]
        assertNotNull(b)
        assertLocalName("int[]", b.type)

        // [:1]
        var slice = assertIs<Range>(assertIs<Subscript>(b.firstAssignment).subscriptExpression)
        assertNull(slice.floor)
        assertLiteralValue(1, slice.ceiling)
        assertNull(slice.third)

        val c = tu.variables["c"]
        assertNotNull(c)
        assertLocalName("int[]", c.type)

        // [1:]
        slice = assertIs(assertIs<Subscript>(c.firstAssignment).subscriptExpression)
        assertLiteralValue(1, slice.floor)
        assertNull(slice.ceiling)
        assertNull(slice.third)

        val d = tu.variables["d"]
        assertNotNull(d)
        assertLocalName("int[]", d.type)

        // [0:1]
        slice = assertIs(assertIs<Subscript>(d.firstAssignment).subscriptExpression)
        assertLiteralValue(0, slice.floor)
        assertLiteralValue(1, slice.ceiling)
        assertNull(slice.third)

        val e = tu.variables["e"]
        assertNotNull(e)
        assertLocalName("int[]", e.type)

        // [0:1:1]
        slice = assertIs(assertIs<Subscript>(e.firstAssignment).subscriptExpression)
        assertLiteralValue(0, slice.floor)
        assertLiteralValue(1, slice.ceiling)
        assertLiteralValue(1, slice.third)
    }

    @Test
    fun testSendStmt() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("chan.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        with(tu) {
            val main = tu.functions["main"]
            assertNotNull(main)

            val v = main.variables["v"]
            assertNotNull(v)
            assertEquals(primitiveType("int"), v.type)

            val ch = main.variables["ch"]
            assertNotNull(ch)
            assertEquals(objectType("chan", generics = listOf(primitiveType("int"))), ch.type)

            val binOp = main.bodyOrNull<BinaryOperator>(2)
            assertNotNull(binOp)
            assertRefersTo(binOp.lhs, ch)
            assertRefersTo(binOp.rhs, v)

            val unaryOp = main.bodyOrNull<UnaryOperator>(3)
            assertNotNull(unaryOp)
            assertRefersTo(unaryOp.input, ch)
        }
    }

    @Test
    fun testShortAssign() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("short_assign.go").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val lit5 = tu.literals.firstOrNull()
        assertNotNull(lit5)
        println(lit5.printDFG())

        val x = tu.refs("x").lastOrNull()
        assertNotNull(x)

        val paths = x.followPrevFullDFGEdgesUntilHit { it == lit5 }
        assertEquals(3, paths.fulfilled.firstOrNull()?.nodes?.size)

        assertEquals(5, x.evaluate())
    }
}
