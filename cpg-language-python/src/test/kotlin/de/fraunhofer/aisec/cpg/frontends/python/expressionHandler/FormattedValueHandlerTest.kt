/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.python.expressionHandler

import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Call
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import de.fraunhofer.aisec.cpg.test.assertLocalName
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FormattedValueHandlerTest {

    private lateinit var topLevel: Path
    private lateinit var result: TranslationUnit

    @BeforeAll
    fun setup() {
        topLevel = Path.of("src", "test", "resources", "python")
        analyzeFile()
    }

    fun analyzeFile() {
        result =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("formatted_values.py").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
    }

    @Test
    fun testFormattedValues() {
        // Test for a = f'Number: {42:.2f}'
        val aAssExpression = result.variables["a"]?.astParent
        assertIs<Assign>(aAssExpression)
        val aExprRhs = aAssExpression.rhs.singleOrNull()
        assertIs<BinaryOperator>(aExprRhs)
        val aFormatCall = aExprRhs.rhs
        assertIs<Call>(aFormatCall)
        assertLocalName("format", aFormatCall)
        val aArguments = aFormatCall.arguments
        assertEquals(2, aArguments.size)
        assertIs<Literal<*>>(aArguments[0])
        assertLiteralValue(42.toLong(), aArguments[0])
        assertIs<Literal<*>>(aArguments[1])
        assertLiteralValue(".2f", aArguments[1])

        // Test for b = f'Hexadecimal: {255:#x}'
        val bAssExpression = result.variables["b"]?.astParent
        assertIs<Assign>(bAssExpression)
        val bExprRhs = bAssExpression.rhs.singleOrNull()
        assertIs<BinaryOperator>(bExprRhs)
        val bFormatCall = bExprRhs.rhs
        assertIs<Call>(bFormatCall)
        assertLocalName("format", bFormatCall)
        val bArguments = bFormatCall.arguments
        assertEquals(2, bArguments.size)
        assertIs<Literal<*>>(bArguments[0])
        assertLiteralValue(255L, bArguments[0])
        //        assertIs<Literal<*>>(bArguments[1])
        assertLiteralValue("#x", bArguments[1])

        // Test for c = f'String with conversion: {"Hello, world!"!r}'
        val cAssExpression = result.variables["c"]?.astParent
        assertIs<Assign>(cAssExpression)
        val cExprRhs = cAssExpression.rhs.singleOrNull()
        assertIs<BinaryOperator>(cExprRhs)
        val cConversionCall = cExprRhs.rhs
        assertIs<Call>(cConversionCall)
        assertLocalName("repr", cConversionCall)
        val cArguments = cConversionCall.arguments.singleOrNull()
        assertNotNull(cArguments)
        assertLiteralValue("Hello, world!", cArguments)

        // Test for d = f'ASCII representation: {"50$"!a}'
        val dAssExpression = result.variables["d"]?.astParent
        assertIs<Assign>(dAssExpression)
        val dExprRhs = dAssExpression.rhs.singleOrNull()
        assertIs<BinaryOperator>(dExprRhs)
        val dConversionCall = dExprRhs.rhs
        assertIs<Call>(dConversionCall)
        assertLocalName("ascii", dConversionCall)
        val dArguments = dConversionCall.arguments.singleOrNull()
        assertNotNull(dArguments)
        assertLiteralValue("50$", dArguments)

        // Test for e = f'Combined: {42!s:10}'
        // This is translated to `'Combined: ' +  format(str(b), "10")`
        val eAssExpression = result.variables["e"]?.astParent
        assertIs<Assign>(eAssExpression)
        val eExprRhs = eAssExpression.rhs.singleOrNull()
        assertIs<BinaryOperator>(eExprRhs)
        val eFormatCall = eExprRhs.rhs
        assertIs<Call>(eFormatCall)
        assertLocalName("format", eFormatCall)
        val eArguments = eFormatCall.arguments
        assertEquals(2, eArguments.size)
        val eConversionCall = eArguments[0]
        assertIs<Call>(eConversionCall)
        assertLocalName("str", eConversionCall)
        assertLiteralValue("42".toLong(), eConversionCall.arguments.singleOrNull())
        assertLiteralValue("10", eArguments[1])
    }
}
