/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ValueEvaluatorTest {

    @Test
    fun test() {
        val topLevel = Path.of("src", "test", "resources", "value_evaluation")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("example.cpp").toFile()),
                topLevel,
                true
            )

        assertNotNull(tu)

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val b = main.bodyOrNull<DeclarationStatement>()?.singleDeclaration
        assertNotNull(b)

        var value = b.evaluate()
        assertEquals(2L, value)

        val printB = main.bodyOrNull<CallExpression>()
        assertNotNull(printB)

        val evaluator = ValueEvaluator()
        value = evaluator.evaluate(printB.arguments.firstOrNull())
        assertEquals(2L, value)

        val path = evaluator.path
        assertEquals(4, path.size)

        val printA = main.bodyOrNull<CallExpression>(1)
        assertNotNull(printA)

        value = printA.arguments.firstOrNull()?.evaluate()
        assertEquals(2, value)

        val c = main.bodyOrNull<DeclarationStatement>(2)?.singleDeclaration
        assertNotNull(c)

        value = c.evaluate()
        assertEquals(3L, value)

        val d = main.bodyOrNull<DeclarationStatement>(3)?.singleDeclaration
        assertNotNull(d)

        value = d.evaluate()
        assertEquals(2L, value)

        val e = main.bodyOrNull<DeclarationStatement>(4)?.singleDeclaration
        assertNotNull(e)
        value = e.evaluate()
        assertEquals(3.5, value)

        val f = main.bodyOrNull<DeclarationStatement>(5)?.singleDeclaration
        assertNotNull(f)
        value = f.evaluate()
        assertEquals(10L, value)

        val printHelloWorld = main.bodyOrNull<CallExpression>(2)
        assertNotNull(printHelloWorld)

        value = printHelloWorld.arguments.firstOrNull()?.evaluate()
        assertEquals("Hello world", value)

        val g = main.bodyOrNull<DeclarationStatement>(6)?.singleDeclaration
        assertNotNull(g)
        value = g.evaluate()
        assertEquals(-3L, value)

        val h = main.bodyOrNull<DeclarationStatement>(7)?.singleDeclaration
        assertNotNull(h)
        value = h.evaluate()
        assertFalse(value as Boolean)

        val i = main.bodyOrNull<DeclarationStatement>(8)?.singleDeclaration
        assertNotNull(i)
        value = i.evaluate()
        assertFalse(value as Boolean)

        val j = main.bodyOrNull<DeclarationStatement>(9)?.singleDeclaration
        assertNotNull(j)
        value = j.evaluate()
        assertFalse(value as Boolean)

        val k = main.bodyOrNull<DeclarationStatement>(10)?.singleDeclaration
        assertNotNull(k)
        value = k.evaluate()
        assertFalse(value as Boolean)

        val l = main.bodyOrNull<DeclarationStatement>(11)?.singleDeclaration
        assertNotNull(l)
        value = l.evaluate()
        assertFalse(value as Boolean)

        val m = main.bodyOrNull<DeclarationStatement>(12)?.singleDeclaration
        assertNotNull(m)
        value = m.evaluate()
        assertFalse(value as Boolean)
    }

    @Test
    fun testHandlePlus() {
        val binOp = NodeBuilder.newBinaryOperator("+")
        binOp.lhs = NodeBuilder.newLiteral(3, TypeParser.createFrom("int", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(5L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(5.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral(3L, TypeParser.createFrom("long", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(5L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(5.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral((3).toShort(), TypeParser.createFrom("short", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(5L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(5.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral((3).toByte(), TypeParser.createFrom("byte", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(5L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(5.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral(3.0, TypeParser.createFrom("double", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(5.0, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(5.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral(3.0f, TypeParser.createFrom("float", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(5.0, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(5.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral("Hello", TypeParser.createFrom("String", true))
        binOp.rhs = NodeBuilder.newLiteral(" world", TypeParser.createFrom("String", true))
        assertEquals("Hello world", ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))
        assertEquals("Hello2", ValueEvaluator().evaluate(binOp))
    }

    @Test
    fun testHandleMinus() {
        val binOp = NodeBuilder.newBinaryOperator("-")
        binOp.lhs = NodeBuilder.newLiteral(3, TypeParser.createFrom("int", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(1L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 - 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral(3L, TypeParser.createFrom("long", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(1L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 - 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral((3).toShort(), TypeParser.createFrom("short", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(1L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 - 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral((3).toByte(), TypeParser.createFrom("byte", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(1L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 - 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral(3.0, TypeParser.createFrom("double", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(1.0, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 - 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral(3.0f, TypeParser.createFrom("float", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(1.0, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 - 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral("Hello", TypeParser.createFrom("String", true))
        binOp.rhs = NodeBuilder.newLiteral(" world", TypeParser.createFrom("String", true))
        assertEquals("{-}", ValueEvaluator().evaluate(binOp))
    }

    @Test
    fun testHandleTimes() {
        val binOp = NodeBuilder.newBinaryOperator("*")
        binOp.lhs = NodeBuilder.newLiteral(3, TypeParser.createFrom("int", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(6L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 * 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral(3L, TypeParser.createFrom("long", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(6L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 * 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral((3).toShort(), TypeParser.createFrom("short", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(6L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 * 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral((3).toByte(), TypeParser.createFrom("byte", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(6L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 * 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral(3.0, TypeParser.createFrom("double", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(6.0, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 * 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral(3.0f, TypeParser.createFrom("float", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(6.0, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 * 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral("Hello", TypeParser.createFrom("String", true))
        binOp.rhs = NodeBuilder.newLiteral(" world", TypeParser.createFrom("String", true))
        assertEquals("{*}", ValueEvaluator().evaluate(binOp))
    }

    @Test
    fun testHandleDiv() {
        // For two integer values, we keep the result as a long.
        val binOp = NodeBuilder.newBinaryOperator("/")
        binOp.lhs = NodeBuilder.newLiteral(3, TypeParser.createFrom("int", true))
        binOp.rhs = NodeBuilder.newLiteral(0, TypeParser.createFrom("int", true))
        assertEquals("{/}", ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral(3, TypeParser.createFrom("int", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(1L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 / 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral(3L, TypeParser.createFrom("long", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(1L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 / 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral((3).toShort(), TypeParser.createFrom("short", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(1L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 / 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral((3).toByte(), TypeParser.createFrom("byte", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(1L, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 / 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral(3.0, TypeParser.createFrom("double", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(1.5, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 / 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral(3.0f, TypeParser.createFrom("float", true))
        binOp.rhs = NodeBuilder.newLiteral(2, TypeParser.createFrom("int", true))

        assertEquals(1.5, ValueEvaluator().evaluate(binOp))

        binOp.rhs = NodeBuilder.newLiteral(2.4, TypeParser.createFrom("double", true))
        assertEquals(3 / 2.4, ValueEvaluator().evaluate(binOp))

        binOp.lhs = NodeBuilder.newLiteral("Hello", TypeParser.createFrom("String", true))
        binOp.rhs = NodeBuilder.newLiteral(" world", TypeParser.createFrom("String", true))
        assertEquals("{/}", ValueEvaluator().evaluate(binOp))
    }

    @Test
    fun testHandleUnary() {
        val neg = NodeBuilder.newUnaryOperator("-", false, true)
        neg.input = NodeBuilder.newLiteral(3, TypeParser.createFrom("int", true))
        assertEquals(-3, ValueEvaluator().evaluate(neg))

        neg.input = NodeBuilder.newLiteral(3.5, TypeParser.createFrom("double", true))
        assertEquals(-3.5, ValueEvaluator().evaluate(neg))

        val plusplus = NodeBuilder.newUnaryOperator("++", true, false)
        plusplus.input = NodeBuilder.newLiteral(3, TypeParser.createFrom("int", true))
        assertEquals(4L, ValueEvaluator().evaluate(plusplus))

        plusplus.input = NodeBuilder.newLiteral(3.5, TypeParser.createFrom("double", true))
        assertEquals(4.5, ValueEvaluator().evaluate(plusplus))

        plusplus.input = NodeBuilder.newLiteral(3.5f, TypeParser.createFrom("float", true))
        assertEquals(4.5f, ValueEvaluator().evaluate(plusplus))

        val minusminus = NodeBuilder.newUnaryOperator("--", true, false)
        minusminus.input = NodeBuilder.newLiteral(3, TypeParser.createFrom("int", true))
        assertEquals(2L, ValueEvaluator().evaluate(minusminus))

        minusminus.input = NodeBuilder.newLiteral(3.5, TypeParser.createFrom("double", true))
        assertEquals(2.5, ValueEvaluator().evaluate(minusminus))

        minusminus.input = NodeBuilder.newLiteral(3.5f, TypeParser.createFrom("float", true))
        assertEquals(2.5f, ValueEvaluator().evaluate(minusminus))
    }
}
