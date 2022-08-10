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
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.evaluate
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
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

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val b = main.bodyOrNull<DeclarationStatement>()?.singleDeclaration
        assertNotNull(b)

        var value = b.evaluate()
        assertEquals(2L, value)

        val printB = main.bodyOrNull<CallExpression>()
        assertNotNull(printB)

        val evaluator = MultiValueEvaluator()
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(value.min(), value.max())
        assertEquals(2L, value.min())

        val path = evaluator.path
        assertEquals(4, path.size)

        val printA = main.bodyOrNull<CallExpression>(1)
        assertNotNull(printA)

        value = evaluator.evaluate(printA.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(value.min(), value.max())
        assertEquals(2, value.min())

        val c = main.bodyOrNull<DeclarationStatement>(2)?.singleDeclaration
        assertNotNull(c)

        value = evaluator.evaluate(c)
        assertEquals(3L, value)

        val d = main.bodyOrNull<DeclarationStatement>(3)?.singleDeclaration
        assertNotNull(d)

        value = evaluator.evaluate(d)
        assertEquals(2L, value)

        val e = main.bodyOrNull<DeclarationStatement>(4)?.singleDeclaration
        assertNotNull(e)
        value = evaluator.evaluate(e)
        assertEquals(3.5, value)

        val f = main.bodyOrNull<DeclarationStatement>(5)?.singleDeclaration
        assertNotNull(f)
        value = evaluator.evaluate(f)
        assertEquals(10L, value)

        val g = main.bodyOrNull<DeclarationStatement>(6)?.singleDeclaration
        assertNotNull(g)
        value = evaluator.evaluate(g) as ConcreteNumberSet
        assertEquals(value.min(), value.max())
        assertEquals(-3L, value.min())

        val i = main.bodyOrNull<DeclarationStatement>(8)?.singleDeclaration
        assertNotNull(i)
        value = evaluator.evaluate(i)
        assertFalse(value as Boolean)

        val j = main.bodyOrNull<DeclarationStatement>(9)?.singleDeclaration
        assertNotNull(j)
        value = evaluator.evaluate(j)
        assertFalse(value as Boolean)

        val k = main.bodyOrNull<DeclarationStatement>(10)?.singleDeclaration
        assertNotNull(k)
        value = evaluator.evaluate(k)
        assertFalse(value as Boolean)

        val l = main.bodyOrNull<DeclarationStatement>(11)?.singleDeclaration
        assertNotNull(l)
        value = evaluator.evaluate(l)
        assertFalse(value as Boolean)

        val m = main.bodyOrNull<DeclarationStatement>(12)?.singleDeclaration
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

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val b = main.bodyOrNull<DeclarationStatement>()?.singleDeclaration
        assertNotNull(b)

        var printB = main.bodyOrNull<CallExpression>()
        assertNotNull(printB)

        val evaluator = MultiValueEvaluator()
        var value = printB.arguments.firstOrNull()?.evaluate()
        assertTrue(value is String) // could not evaluate

        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(1, 2), value.values)

        printB = main.bodyOrNull<CallExpression>(1)
        assertNotNull(printB)
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(0, 1, 2), value.values)

        printB = main.bodyOrNull<CallExpression>(2)
        assertNotNull(printB)
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(0, 1, 2, 4), value.values)

        printB = main.bodyOrNull<CallExpression>(3)
        assertNotNull(printB)
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(-4, -2, -1, 0, 1, 2, 4), value.values)

        printB = main.bodyOrNull<CallExpression>(4)
        assertNotNull(printB)
        value = evaluator.evaluate(printB.arguments.firstOrNull()) as ConcreteNumberSet
        assertEquals(setOf<Long>(3, 6), value.values)
    }
}
