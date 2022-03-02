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
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
        assertEquals(4, path.size)

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
        assertTrue(value as Boolean)
    }
}
