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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.SubscriptExpression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SizeEvaluatorTest {
    private lateinit var tu: TranslationUnitDeclaration

    @BeforeAll
    fun beforeAll() {
        tu = ValueEvaluationTests.getSizeExample().components.first().translationUnits.first()
    }

    @Test
    fun testArraySize() {
        val mainClass = tu.allRecords["MainClass"]
        assertNotNull(mainClass)
        val main = mainClass.methods["main"]
        assertNotNull(main)

        val array = main.bodyOrNull<DeclarationStatement>()?.singleDeclaration
        assertNotNull(array)

        val evaluator = SizeEvaluator()
        var value = evaluator.evaluate(array)
        assertEquals(3, value)

        val printCall = main.allCalls["println"]
        assertNotNull(printCall)

        value = evaluator.evaluate(printCall.arguments.firstOrNull()) as Int
        assertEquals(3, value)
    }

    @Test
    fun testArraySizeFromSubscript() {
        val mainClass = tu.allRecords["MainClass"]
        assertNotNull(mainClass)

        val main = mainClass.methods["main"]
        assertNotNull(main)

        val array = main.bodyOrNull<DeclarationStatement>()?.singleDeclaration
        assertNotNull(array)

        val evaluator = SizeEvaluator()
        var value = evaluator.evaluate(array)
        assertEquals(3, value)

        val forLoop = main.allForLoops.firstOrNull()
        assertNotNull(forLoop)

        val subscriptExpr =
            ((forLoop.statement as Block).statements[0] as AssignExpression).lhs<
                SubscriptExpression
            >()

        value = evaluator.evaluate(subscriptExpr) as Int
        assertEquals(3, value)
    }

    @Test
    fun testStringSize() {
        val mainClass = tu.allRecords["MainClass"]
        assertNotNull(mainClass)

        val main = mainClass.methods["main"]
        assertNotNull(main)

        val printCall = main.allCalls("println").getOrNull(1)
        assertNotNull(printCall)

        val evaluator = SizeEvaluator()
        val value = evaluator.evaluate(printCall.arguments.firstOrNull()) as Int
        assertEquals(5, value)

        val strValue = evaluator.evaluate("abcd") as Int
        assertEquals(4, strValue)
    }
}
