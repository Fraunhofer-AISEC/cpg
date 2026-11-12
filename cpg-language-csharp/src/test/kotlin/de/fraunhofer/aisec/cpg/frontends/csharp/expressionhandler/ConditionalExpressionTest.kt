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
package de.fraunhofer.aisec.cpg.frontends.csharp.expressionhandler

import de.fraunhofer.aisec.cpg.frontends.csharp.CSharpLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Conditional
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ConditionalExpressionTest : BaseTest() {

    @Test
    fun testConditionalExpression() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ConditionalExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["ConditionalExpression"]
        assertNotNull(record)

        val method = record.methods["Max"]
        assertNotNull(method)
        val paramA = method.parameters["a"]
        assertNotNull(paramA)
        val paramB = method.parameters["b"]
        assertNotNull(paramB)
        val body = method.body
        assertIs<Block>(body)

        // return a > b ? a : b;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val conditional = ret.returnValue
        assertIs<Conditional>(conditional)

        val condition = conditional.condition
        assertIs<BinaryOperator>(condition)
        assertEquals(">", condition.operatorCode)

        val thenExpression = conditional.thenExpression
        assertIs<Reference>(thenExpression)
        assertRefersTo(thenExpression, paramA)

        val elseExpression = conditional.elseExpression
        assertIs<Reference>(elseExpression)
        assertRefersTo(elseExpression, paramB)

        // the type is the common type of both branches, here `int` on both sides
        assertIs<IntegerType>(conditional.type)
    }
}
