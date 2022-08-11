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
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SizeEvaluatorTest {
    @Test
    fun testArraySize() {
        val topLevel = Path.of("src", "test", "resources", "value_evaluation")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("size.java").toFile()),
                topLevel,
                true
            )

        assertNotNull(tu)

        val mainClass = tu.byNameOrNull<RecordDeclaration>("MainClass")
        assertNotNull(mainClass)
        val main = mainClass.byNameOrNull<MethodDeclaration>("main")
        assertNotNull(main)

        val array = main.bodyOrNull<DeclarationStatement>()?.singleDeclaration
        assertNotNull(array)

        val evaluator = SizeEvaluator()
        var value = evaluator.evaluate(array)
        assertEquals(3, value)

        val printCall = main.bodyOrNull<CallExpression>(0)
        assertNotNull(printCall)

        value = evaluator.evaluate(printCall.arguments.firstOrNull()) as Int
        assertEquals(3, value)
    }

    @Test
    fun testArraySizeFromSubscript() {
        val topLevel = Path.of("src", "test", "resources", "value_evaluation")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("size.java").toFile()),
                topLevel,
                true
            )

        assertNotNull(tu)

        val mainClass = tu.byNameOrNull<RecordDeclaration>("MainClass")
        assertNotNull(mainClass)
        val main = mainClass.byNameOrNull<MethodDeclaration>("main")
        assertNotNull(main)

        val array = main.bodyOrNull<DeclarationStatement>()?.singleDeclaration
        assertNotNull(array)

        val evaluator = SizeEvaluator()
        var value = evaluator.evaluate(array)
        assertEquals(3, value)

        val forLoop = main.bodyOrNull<ForStatement>(0)
        assertNotNull(forLoop)

        val subscriptExpr =
            ((forLoop.statement as CompoundStatement).statements[0] as BinaryOperator).lhs

        value = evaluator.evaluate(subscriptExpr) as Int
        assertEquals(3, value)
    }

    @Test
    fun testStringSize() {
        val topLevel = Path.of("src", "test", "resources", "value_evaluation")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("size.java").toFile()),
                topLevel,
                true
            )

        assertNotNull(tu)

        val mainClass = tu.byNameOrNull<RecordDeclaration>("MainClass")
        assertNotNull(mainClass)
        val main = mainClass.byNameOrNull<MethodDeclaration>("main")
        assertNotNull(main)
        val printCall = main.bodyOrNull<CallExpression>(1)
        assertNotNull(printCall)

        val evaluator = SizeEvaluator()
        val value = evaluator.evaluate(printCall.arguments.firstOrNull()) as Int
        assertEquals(5, value)

        val strValue = evaluator.evaluate("abcd") as Int
        assertEquals(4, strValue)
    }
}
