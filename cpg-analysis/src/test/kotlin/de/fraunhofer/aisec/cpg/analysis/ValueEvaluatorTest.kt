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
import de.fraunhofer.aisec.cpg.frontends.TestHandler
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.testcases.ValueEvaluationTests
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import org.junit.jupiter.api.assertThrows

class NotReallyANumber : Number() {
    override fun toByte(): Byte {
        TODO("Not yet implemented")
    }

    override fun toDouble(): Double {
        TODO("Not yet implemented")
    }

    override fun toFloat(): Float {
        TODO("Not yet implemented")
    }

    override fun toInt(): Int {
        TODO("Not yet implemented")
    }

    override fun toLong(): Long {
        TODO("Not yet implemented")
    }

    override fun toShort(): Short {
        TODO("Not yet implemented")
    }
}

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
        assertEquals(2, value)

        val printB = main.bodyOrNull<CallExpression>()
        assertNotNull(printB)

        val evaluator = ValueEvaluator()
        value = evaluator.evaluate(printB.arguments.firstOrNull())
        assertEquals(2, value)

        val path = evaluator.path
        assertEquals(5, path.size)

        val printA = main.bodyOrNull<CallExpression>(1)
        assertNotNull(printA)

        value = printA.arguments.firstOrNull()?.evaluate()
        assertEquals(2, value)

        val c = main.bodyOrNull<DeclarationStatement>(2)?.singleDeclaration
        assertNotNull(c)

        value = c.evaluate()
        assertEquals(3, value)

        val d = main.bodyOrNull<DeclarationStatement>(3)?.singleDeclaration
        assertNotNull(d)

        value = d.evaluate()
        assertEquals(2, value)

        val e = main.bodyOrNull<DeclarationStatement>(4)?.singleDeclaration
        assertNotNull(e)
        value = e.evaluate()
        assertEquals(3.5, value)

        val f = main.bodyOrNull<DeclarationStatement>(5)?.singleDeclaration
        assertNotNull(f)
        value = f.evaluate()
        assertEquals(10, value)

        val printHelloWorld = main.bodyOrNull<CallExpression>(2)
        assertNotNull(printHelloWorld)

        value = printHelloWorld.arguments.firstOrNull()?.evaluate()
        assertEquals("Hello world", value)

        val g = main.bodyOrNull<DeclarationStatement>(6)?.singleDeclaration
        assertNotNull(g)
        value = g.evaluate()
        assertEquals(-3, value)

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

        m.fields
    }

    @Test
    fun testComplex() {
        val tu =
            ValueEvaluationTests.getComplexExample().components.first().translationUnits.first()

        assertNotNull(tu)

        val mainClass = tu.records["MainClass"]
        assertNotNull(mainClass)

        val main = mainClass.functions["main"]
        assertNotNull(main)

        val s = main.refs("s").last()
        assertNotNull(s)

        var value = s.evaluate()
        assertEquals("{s}!?", value)

        value = s.evaluate(MultiValueEvaluator())
        assertEquals(setOf("big!?", "small!?"), value)

        val i = main.refs("i").last()
        assertNotNull(i)

        value = i.evaluate()
        assertEquals(4, value)
    }

    @Test
    fun testHandlePlus() {
        with(TestHandler(TestLanguageFrontend())) {
            val binOp = newBinaryOperator("+")
            // Int.plus
            binOp.lhs = newLiteral(3, primitiveType("int"))
            binOp.rhs = newLiteral(2, primitiveType("int"))
            assertEquals(5, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toByte(), primitiveType("byte"))
            assertEquals(5, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("long"))
            assertEquals(5L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toShort(), primitiveType("short"))
            assertEquals(5, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4f, primitiveType("float"))
            assertEquals(5.4f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(5.4, ValueEvaluator().evaluate(binOp))

            assertThrows<UnsupportedOperationException> {
                binOp.rhs = newLiteral(NotReallyANumber(), objectType("fake"))
                ValueEvaluator().evaluate(binOp)
            }

            // Byte.plus
            binOp.lhs = newLiteral(3.toByte(), primitiveType("byte"))
            binOp.rhs = newLiteral(2, primitiveType("int"))
            assertEquals(5, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toByte(), primitiveType("byte"))
            assertEquals(5, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("long"))
            assertEquals(5L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toShort(), primitiveType("short"))
            assertEquals(5, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4f, primitiveType("float"))
            assertEquals(5.4f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(5.4, ValueEvaluator().evaluate(binOp))

            assertThrows<UnsupportedOperationException> {
                binOp.rhs = newLiteral(NotReallyANumber(), objectType("fake"))
                ValueEvaluator().evaluate(binOp)
            }

            // Long.plus
            binOp.lhs = newLiteral(3L, primitiveType("long"))
            binOp.rhs = newLiteral(2, primitiveType("int"))
            assertEquals(5L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toByte(), primitiveType("byte"))
            assertEquals(5L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("long"))
            assertEquals(5L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toShort(), primitiveType("short"))
            assertEquals(5L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4f, primitiveType("float"))
            assertEquals(5.4f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(5.4, ValueEvaluator().evaluate(binOp))

            assertThrows<UnsupportedOperationException> {
                binOp.rhs = newLiteral(NotReallyANumber(), objectType("fake"))
                ValueEvaluator().evaluate(binOp)
            }

            // Short.plus
            binOp.lhs = newLiteral(3.toShort(), primitiveType("short"))
            binOp.rhs = newLiteral(2, primitiveType("int"))
            assertEquals(5, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toByte(), primitiveType("byte"))
            assertEquals(5, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("long"))
            assertEquals(5L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toShort(), primitiveType("short"))
            assertEquals(5, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4f, primitiveType("float"))
            assertEquals(5.4f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(5.4, ValueEvaluator().evaluate(binOp))

            assertThrows<UnsupportedOperationException> {
                binOp.rhs = newLiteral(NotReallyANumber(), objectType("fake"))
                ValueEvaluator().evaluate(binOp)
            }

            // Float.plus
            binOp.lhs = newLiteral(3.0f, primitiveType("float"))
            binOp.rhs = newLiteral(2, primitiveType("int"))
            assertEquals(5.0f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toByte(), primitiveType("byte"))
            assertEquals(5.0f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("long"))
            assertEquals(5.0f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toShort(), primitiveType("short"))
            assertEquals(5.0f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4f, primitiveType("float"))
            assertEquals(5.4f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(5.4, ValueEvaluator().evaluate(binOp))

            assertThrows<UnsupportedOperationException> {
                binOp.rhs = newLiteral(NotReallyANumber(), objectType("fake"))
                ValueEvaluator().evaluate(binOp)
            }

            // Double.plus
            binOp.lhs = newLiteral(3.0, primitiveType("double"))
            binOp.rhs = newLiteral(2, primitiveType("int"))
            assertEquals(5.0, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toByte(), primitiveType("byte"))
            assertEquals(5.0, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("long"))
            assertEquals(5.0, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toShort(), primitiveType("short"))
            assertEquals(5.0, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4f, primitiveType("float"))
            assertEquals((3.0 + 2.4f), ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(5.4, ValueEvaluator().evaluate(binOp))

            assertThrows<UnsupportedOperationException> {
                binOp.rhs = newLiteral(NotReallyANumber(), objectType("fake"))
                ValueEvaluator().evaluate(binOp)
            }

            // String.plus
            binOp.lhs = newLiteral("Hello", primitiveType("string"))
            binOp.rhs = newLiteral(" world", primitiveType("string"))
            assertEquals("Hello world", ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2, primitiveType("int"))
            assertEquals("Hello2", ValueEvaluator().evaluate(binOp))
        }
    }

    @Test
    fun testHandleMinus() {
        with(TestHandler(TestLanguageFrontend())) {
            val binOp = newBinaryOperator("-")
            binOp.lhs = newLiteral(3, primitiveType("int"))
            binOp.rhs = newLiteral(2, primitiveType("int"))

            assertEquals(1, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 - 2.4, ValueEvaluator().evaluate(binOp))

            binOp.lhs = newLiteral(3L, primitiveType("long"))
            binOp.rhs = newLiteral(2, primitiveType("int"))

            assertEquals(1L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 - 2.4, ValueEvaluator().evaluate(binOp))

            binOp.lhs = newLiteral((3).toShort(), primitiveType("short"))
            binOp.rhs = newLiteral(2, primitiveType("int"))

            assertEquals(1, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 - 2.4, ValueEvaluator().evaluate(binOp))

            binOp.lhs = newLiteral((3).toByte(), primitiveType("byte"))
            binOp.rhs = newLiteral(2, primitiveType("int"))

            assertEquals(1, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 - 2.4, ValueEvaluator().evaluate(binOp))

            binOp.lhs = newLiteral(3.0, primitiveType("double"))
            binOp.rhs = newLiteral(2, primitiveType("int"))

            assertEquals(1.0, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 - 2.4, ValueEvaluator().evaluate(binOp))

            binOp.lhs = newLiteral(3.0f, primitiveType("float"))
            binOp.rhs = newLiteral(2, primitiveType("int"))

            assertEquals(1.0f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 - 2.4, ValueEvaluator().evaluate(binOp))

            binOp.lhs = newLiteral("Hello", primitiveType("string"))
            binOp.rhs = newLiteral(" world", primitiveType("string"))
            assertEquals("{-}", ValueEvaluator().evaluate(binOp))
        }
    }

    @Test
    fun testHandleTimes() {
        with(TestHandler(TestLanguageFrontend())) {
            // Int.times
            val binOp = newBinaryOperator("*")
            binOp.lhs = newLiteral(3, primitiveType("int"))
            binOp.rhs = newLiteral(2, primitiveType("int"))
            assertEquals(6, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toByte(), primitiveType("byte"))
            assertEquals(6, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("long"))
            assertEquals(6L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toShort(), primitiveType("short"))
            assertEquals(6, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4f, primitiveType("float"))
            assertEquals(3 * 2.4f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 * 2.4, ValueEvaluator().evaluate(binOp))

            assertThrows<UnsupportedOperationException> {
                binOp.rhs = newLiteral(NotReallyANumber(), objectType("fake"))
                ValueEvaluator().evaluate(binOp)
            }

            // Byte.times
            binOp.lhs = newLiteral(3.toByte(), primitiveType("byte"))
            binOp.rhs = newLiteral(2, primitiveType("int"))
            assertEquals(6, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toByte(), primitiveType("byte"))
            assertEquals(6, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("long"))
            assertEquals(6L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toShort(), primitiveType("short"))
            assertEquals(6, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4f, primitiveType("float"))
            assertEquals(3 * 2.4f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 * 2.4, ValueEvaluator().evaluate(binOp))

            assertThrows<UnsupportedOperationException> {
                binOp.rhs = newLiteral(NotReallyANumber(), objectType("fake"))
                ValueEvaluator().evaluate(binOp)
            }

            // Short.times
            binOp.lhs = newLiteral(3.toShort(), primitiveType("short"))
            binOp.rhs = newLiteral(2, primitiveType("int"))
            assertEquals(6, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toShort(), primitiveType("byte"))
            assertEquals(6, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("long"))
            assertEquals(6L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.toShort(), primitiveType("short"))
            assertEquals(6, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4f, primitiveType("float"))
            assertEquals(3 * 2.4f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 * 2.4, ValueEvaluator().evaluate(binOp))

            assertThrows<UnsupportedOperationException> {
                binOp.rhs = newLiteral(NotReallyANumber(), objectType("fake"))
                ValueEvaluator().evaluate(binOp)
            }

            // Long.times
            binOp.lhs = newLiteral(3L, primitiveType("long"))
            binOp.rhs = newLiteral(2, primitiveType("int"))
            assertEquals(6L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("byte"))
            assertEquals(6L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("long"))
            assertEquals(6L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("short"))
            assertEquals(6L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4f, primitiveType("float"))
            assertEquals(3L * 2.4f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3L * 2.4, ValueEvaluator().evaluate(binOp))

            assertThrows<UnsupportedOperationException> {
                binOp.rhs = newLiteral(NotReallyANumber(), objectType("fake"))
                ValueEvaluator().evaluate(binOp)
            }

            // Float.times
            binOp.lhs = newLiteral(3.0f, primitiveType("float"))
            binOp.rhs = newLiteral(2, primitiveType("int"))
            assertEquals(6.0f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("byte"))
            assertEquals(6.0f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("long"))
            assertEquals(6.0f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("short"))
            assertEquals(6.0f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4f, primitiveType("float"))
            assertEquals(3.0f * 2.4f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3.0f * 2.4, ValueEvaluator().evaluate(binOp))

            assertThrows<UnsupportedOperationException> {
                binOp.rhs = newLiteral(NotReallyANumber(), objectType("fake"))
                ValueEvaluator().evaluate(binOp)
            }

            // Double.times
            binOp.lhs = newLiteral(3.0, primitiveType("double"))
            binOp.rhs = newLiteral(2, primitiveType("int"))
            assertEquals(6.0, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("byte"))
            assertEquals(6.0, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("long"))
            assertEquals(6.0, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2L, primitiveType("short"))
            assertEquals(6.0, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4f, primitiveType("float"))
            assertEquals(3.0 * 2.4f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3.0 * 2.4, ValueEvaluator().evaluate(binOp))

            assertThrows<UnsupportedOperationException> {
                binOp.rhs = newLiteral(NotReallyANumber(), objectType("fake"))
                ValueEvaluator().evaluate(binOp)
            }

            // String.times
            binOp.lhs = newLiteral("Hello", primitiveType("string"))
            binOp.rhs = newLiteral(" world", primitiveType("string"))
            assertEquals("{*}", ValueEvaluator().evaluate(binOp))
        }
    }

    @Test
    fun testHandleDiv() {
        with(TestHandler(TestLanguageFrontend())) {
            // For two integer values, we keep the result as a long.
            val binOp = newBinaryOperator("/")
            binOp.lhs = newLiteral(3, primitiveType("int"))
            binOp.rhs = newLiteral(0, primitiveType("int"))
            assertEquals("{/}", ValueEvaluator().evaluate(binOp))

            binOp.lhs = newLiteral(3, primitiveType("int"))
            binOp.rhs = newLiteral(2, primitiveType("int"))

            assertEquals(1, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 / 2.4, ValueEvaluator().evaluate(binOp))

            binOp.lhs = newLiteral(3L, primitiveType("long"))
            binOp.rhs = newLiteral(2, primitiveType("int"))

            assertEquals(1L, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 / 2.4, ValueEvaluator().evaluate(binOp))

            binOp.lhs = newLiteral((3).toShort(), primitiveType("short"))
            binOp.rhs = newLiteral(2, primitiveType("int"))

            assertEquals(1, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 / 2.4, ValueEvaluator().evaluate(binOp))

            binOp.lhs = newLiteral((3).toByte(), primitiveType("byte"))
            binOp.rhs = newLiteral(2, primitiveType("int"))

            assertEquals(1, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 / 2.4, ValueEvaluator().evaluate(binOp))

            binOp.lhs = newLiteral(3.0, primitiveType("double"))
            binOp.rhs = newLiteral(2, primitiveType("int"))

            assertEquals(1.5, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 / 2.4, ValueEvaluator().evaluate(binOp))

            binOp.lhs = newLiteral(3.0f, primitiveType("float"))
            binOp.rhs = newLiteral(2, primitiveType("int"))

            assertEquals(1.5f, ValueEvaluator().evaluate(binOp))

            binOp.rhs = newLiteral(2.4, primitiveType("double"))
            assertEquals(3 / 2.4, ValueEvaluator().evaluate(binOp))

            binOp.lhs = newLiteral("Hello", primitiveType("string"))
            binOp.rhs = newLiteral(" world", primitiveType("string"))
            assertEquals("{/}", ValueEvaluator().evaluate(binOp))
        }
    }

    @Test
    fun testHandleUnary() {
        with(TestHandler(TestLanguageFrontend())) {
            val neg = newUnaryOperator("-", false, true)
            neg.input = newLiteral(3, primitiveType("int"))
            assertEquals(-3, ValueEvaluator().evaluate(neg))

            neg.input = newLiteral(3.5, primitiveType("double"))
            assertEquals(-3.5, ValueEvaluator().evaluate(neg))

            val plusplus = newUnaryOperator("++", true, false)
            plusplus.input = newLiteral(3, primitiveType("int"))
            assertEquals(4, ValueEvaluator().evaluate(plusplus))

            plusplus.input = newLiteral(3.5, primitiveType("double"))
            assertEquals(4.5, ValueEvaluator().evaluate(plusplus))

            plusplus.input = newLiteral(3.5f, primitiveType("float"))
            assertEquals(4.5f, ValueEvaluator().evaluate(plusplus))

            val minusminus = newUnaryOperator("--", true, false)
            minusminus.input = newLiteral(3, primitiveType("int"))
            assertEquals(2, ValueEvaluator().evaluate(minusminus))

            minusminus.input = newLiteral(3.5, primitiveType("double"))
            assertEquals(2.5, ValueEvaluator().evaluate(minusminus))

            minusminus.input = newLiteral(3.5f, primitiveType("float"))
            assertEquals(2.5f, ValueEvaluator().evaluate(minusminus))
        }
    }
}
