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
package de.fraunhofer.aisec.cpg.analysis

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDecl
import de.fraunhofer.aisec.cpg.graph.evaluate
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStmt
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStmt
import de.fraunhofer.aisec.cpg.graph.statements.ForStmt
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpr
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpr
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MultiValueEvaluatorTest {
    @Test
    fun testSingleValue() {
        val topLevel = Path.of("src", "test", "resources", "value_evaluation")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("example.cpp").toFile()),
                topLevel,
                true
            )

        assertNotNull(tu)

        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)

        val b = main.bodyOrNull<DeclarationStmt>()?.singleDeclaration
        assertNotNull(b)

        var value = b.evaluate()
        assertEquals(2L, value)

        val printB = main.bodyOrNull<CallExpr>()
        assertNotNull(printB)

        val evaluator = MultiValueEvaluator()
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(value.min(), value.max())
        assertEquals(2L, value.min())

        val path = evaluator.path
        assertEquals(5, path.size)

        val printA = main.bodyOrNull<CallExpr>(1)
        assertNotNull(printA)

        value = evaluator.evaluate(printA.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(value.min(), value.max())
        assertEquals(2, value.min())

        val c = main.bodyOrNull<DeclarationStmt>(2)?.singleDeclaration
        assertNotNull(c)

        value = evaluator.evaluate(c)
        assertEquals(3L, value)

        val d = main.bodyOrNull<DeclarationStmt>(3)?.singleDeclaration
        assertNotNull(d)

        value = evaluator.evaluate(d)
        assertEquals(2L, value)

        val e = main.bodyOrNull<DeclarationStmt>(4)?.singleDeclaration
        assertNotNull(e)
        value = evaluator.evaluate(e)
        assertEquals(3.5, value)

        val f = main.bodyOrNull<DeclarationStmt>(5)?.singleDeclaration
        assertNotNull(f)
        value = evaluator.evaluate(f)
        assertEquals(10L, value)

        val g = main.bodyOrNull<DeclarationStmt>(6)?.singleDeclaration
        assertNotNull(g)
        value = evaluator.evaluate(g) as ConcreteNumberSet
        assertEquals(value.min(), value.max())
        assertEquals(-3L, value.min())

        val i = main.bodyOrNull<DeclarationStmt>(8)?.singleDeclaration
        assertNotNull(i)
        value = evaluator.evaluate(i)
        assertFalse(value as Boolean)

        val j = main.bodyOrNull<DeclarationStmt>(9)?.singleDeclaration
        assertNotNull(j)
        value = evaluator.evaluate(j)
        assertFalse(value as Boolean)

        val k = main.bodyOrNull<DeclarationStmt>(10)?.singleDeclaration
        assertNotNull(k)
        value = evaluator.evaluate(k)
        assertFalse(value as Boolean)

        val l = main.bodyOrNull<DeclarationStmt>(11)?.singleDeclaration
        assertNotNull(l)
        value = evaluator.evaluate(l)
        assertFalse(value as Boolean)

        val m = main.bodyOrNull<DeclarationStmt>(12)?.singleDeclaration
        assertNotNull(m)
        value = evaluator.evaluate(m)
        assertFalse(value as Boolean)
    }

    @Test
    fun testMultipleValues() {
        val topLevel = Path.of("src", "test", "resources", "value_evaluation")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("cfexample.cpp").toFile()),
                topLevel,
                true
            )

        assertNotNull(tu)

        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)

        val b = main.bodyOrNull<DeclarationStmt>()?.singleDeclaration
        assertNotNull(b)

        var printB = main.bodyOrNull<CallExpr>()
        assertNotNull(printB)

        val evaluator = MultiValueEvaluator()
        var value = printB.arguments.firstOrNull()?.evaluate()
        assertTrue(value is String) // could not evaluate

        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(1, 2), value.values)

        printB = main.bodyOrNull<CallExpr>(1)
        assertNotNull(printB)
        evaluator.clearPath()
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(0, 1, 2), value.values)

        printB = main.bodyOrNull<CallExpr>(2)
        assertNotNull(printB)
        evaluator.clearPath()
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(0, 1, 2, 4), value.values)

        printB = main.bodyOrNull<CallExpr>(3)
        assertNotNull(printB)
        evaluator.clearPath()
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(-4, -2, -1, 0, 1, 2, 4), value.values)

        printB = main.bodyOrNull<CallExpr>(4)
        assertNotNull(printB)
        evaluator.clearPath()
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(3, 6), value.values)
    }

    @Test
    fun testLoop() {
        val topLevel = Path.of("src", "test", "resources", "value_evaluation")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("cfexample.cpp").toFile()),
                topLevel,
                true
            ) {
                it.registerPass<EdgeCachePass>()
            }

        assertNotNull(tu)

        val main = tu.byNameOrNull<FunctionDecl>("loop")
        assertNotNull(main)

        val forLoop = main.bodyOrNull<ForStmt>()
        assertNotNull(forLoop)

        val evaluator = MultiValueEvaluator()
        val iVarList =
            ((forLoop.statement as CompoundStmt).statements[0] as AssignExpr).rhs
        assertEquals(1, iVarList.size)
        val iVar = iVarList.first()
        val value = evaluator.evaluate(iVar) as ConcreteNumberSet
        assertEquals(setOf<Long>(0, 1, 2, 3, 4, 5), value.values)
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
