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
package de.fraunhofer.aisec.cpg.evaluation

import de.fraunhofer.aisec.cpg.frontends.TestHandler
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import kotlin.test.*

class MultiValueEvaluatorTest {
    @Test
    fun testSingleValue() {
        val tu = ValueEvaluationTests.getExample().components.first().translationUnits.first()

        val main = tu.allFunctions["main"]
        assertNotNull(main)

        val b = main.allVariables["b"]
        assertNotNull(b)

        var value = b.evaluate()
        assertEquals(2, value)

        val printB =
            main.allCalls[
                    {
                        it.name.localName == "println" &&
                            it.arguments.firstOrNull()?.name?.localName == "b"
                    }]
        assertNotNull(printB)

        val evaluator = MultiValueEvaluator()
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(value.min(), value.max())
        assertEquals(2, value.min())

        val path = evaluator.path
        assertEquals(5, path.size)

        val printA = main.bodyOrNull<CallExpression>(1)
        assertNotNull(printA)

        value = evaluator.evaluate(printA.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(value.min(), value.max())
        assertEquals(2, value.min())

        val c = main.allVariables["c"]
        assertNotNull(c)

        value = evaluator.evaluate(c)
        assertEquals(3, value)

        val d = main.allVariables["d"]
        assertNotNull(d)

        value = evaluator.evaluate(d)
        assertEquals(2, value)

        val e = main.allVariables["e"]
        assertNotNull(e)
        value = evaluator.evaluate(e)
        assertEquals(3.5, value)

        val f = main.allVariables["f"]
        assertNotNull(f)
        value = evaluator.evaluate(f)
        assertEquals(10, value)

        val g = main.allVariables["g"]
        assertNotNull(g)
        value = evaluator.evaluate(g) as ConcreteNumberSet
        assertEquals(value.min(), value.max())
        assertEquals(-3, value.min())

        val i = main.allVariables["i"]
        assertNotNull(i)
        value = evaluator.evaluate(i)
        assertFalse(value as Boolean)

        val j = main.allVariables["j"]
        assertNotNull(j)
        value = evaluator.evaluate(j)
        assertFalse(value as Boolean)

        val k = main.allVariables["k"]
        assertNotNull(k)
        value = evaluator.evaluate(k)
        assertFalse(value as Boolean)

        val l = main.allVariables["l"]
        assertNotNull(l)
        value = evaluator.evaluate(l)
        assertFalse(value as Boolean)

        val m = main.allVariables["m"]
        assertNotNull(m)
        value = evaluator.evaluate(m)
        assertFalse(value as Boolean)
    }

    @Test
    fun testMultipleValues() {
        val tu = ValueEvaluationTests.getCfExample().components.first().translationUnits.first()

        val main = tu.allFunctions["main"]
        assertNotNull(main)

        val b = main.allVariables["b"]
        assertNotNull(b)

        var printB = main.allCalls("println")[0]
        assertNotNull(printB)

        val evaluator = MultiValueEvaluator()
        var value = printB.arguments.firstOrNull()?.evaluate()
        assertTrue(value is String) // could not evaluate

        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(1, 2), value.values)

        printB = main.allCalls("println")[1]
        assertNotNull(printB)
        evaluator.clearPath()
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(0, 1, 2), value.values)

        printB = main.allCalls("println")[2]
        assertNotNull(printB)
        evaluator.clearPath()
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(0, 1, 2, 4), value.values)

        printB = main.allCalls("println")[3]
        assertNotNull(printB)
        evaluator.clearPath()
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(-4, -2, -1, 0, 1, 2, 4), value.values)

        printB = main.allCalls("println")[4]
        assertNotNull(printB)
        evaluator.clearPath()
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(3, 6), value.values)
    }

    @Test
    fun testLoop() {
        val tu = ValueEvaluationTests.getCfExample().components.first().translationUnits.first()

        val loop = tu.allFunctions["loop"]
        assertNotNull(loop)

        val forLoop = loop.allForLoops.firstOrNull()
        assertNotNull(forLoop)

        val evaluator = MultiValueEvaluator()
        val iVarList = ((forLoop.statement as Block).statements[0] as AssignExpression).rhs
        assertEquals(1, iVarList.size)
        val iVar = iVarList.first()
        val value = evaluator.evaluate(iVar) as ConcreteNumberSet
        assertEquals(setOf<Long>(0, 1, 2, 3, 4, 5), value.values)
    }

    @Test
    fun testHandleUnary() {
        val evaluator = MultiValueEvaluator()

        with(TestHandler(TestLanguageFrontend())) {
            // Construct a fake DFG flow
            val three = newLiteral(3, primitiveType("int"))
            val four = newLiteral(4, primitiveType("int"))

            val ref = newReference("a")
            ref.prevDFG = mutableSetOf(three, four)

            val neg = newUnaryOperator("-", false, true)
            neg.input = ref
            assertEquals(ConcreteNumberSet(mutableSetOf(-3, -4)), evaluator.evaluate(neg))

            neg.input = newLiteral(3.5, primitiveType("double"))
            assertEquals(-3.5, evaluator.evaluate(neg))

            val plusplus = newUnaryOperator("++", true, false)
            plusplus.input = newLiteral(3, primitiveType("int"))
            assertEquals(4, evaluator.evaluate(plusplus))

            plusplus.input = newLiteral(3.5, primitiveType("double"))
            assertEquals(4.5, evaluator.evaluate(plusplus))

            plusplus.input = newLiteral(3.5f, primitiveType("float"))
            assertEquals(4.5f, evaluator.evaluate(plusplus))

            val minusminus = newUnaryOperator("--", true, false)
            minusminus.input = newLiteral(3, primitiveType("int"))
            assertEquals(2, evaluator.evaluate(minusminus))

            minusminus.input = newLiteral(3.5, primitiveType("double"))
            assertEquals(2.5, evaluator.evaluate(minusminus))

            minusminus.input = newLiteral(3.5f, primitiveType("float"))
            assertEquals(2.5f, evaluator.evaluate(minusminus))
        }
    }

    @Test
    fun testInterval() {
        val interval = Interval()
        interval.addValue(0)
        assertEquals(0, interval.min())
        assertEquals(0, interval.max())
        interval.addValue(3)
        interval.addValue(2)
        assertEquals(0, interval.min())
        assertEquals(3, interval.max())
        interval.addValue(-5)
        assertEquals(-5, interval.min())
        assertEquals(3, interval.max())
        interval.clear()
        assertEquals(Long.MAX_VALUE, interval.min())
        assertEquals(Long.MIN_VALUE, interval.max())
    }

    @Test
    fun testConcreteNumberSet() {
        val values = ConcreteNumberSet()
        values.addValue(0)
        assertEquals(setOf<Long>(0), values.values)
        values.addValue(3)
        values.addValue(2)
        assertEquals(setOf<Long>(0, 2, 3), values.values)
        assertEquals(0, values.min())
        assertEquals(3, values.max())
        values.addValue(-5)
        assertEquals(setOf<Long>(-5, 0, 2, 3), values.values)
        assertEquals(-5, values.min())
        assertEquals(3, values.max())
        assertTrue(values.maybe(3))
        assertFalse(values.maybe(1))
        values.clear()
        assertEquals(Long.MAX_VALUE, values.min())
        assertEquals(Long.MIN_VALUE, values.max())
    }
}
