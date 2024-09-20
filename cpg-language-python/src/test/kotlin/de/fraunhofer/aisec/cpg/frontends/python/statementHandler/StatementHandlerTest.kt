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
package de.fraunhofer.aisec.cpg.frontends.python.statementHandler

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.AssertStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.test.assertResolvedType
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatementHandlerTest {

    private lateinit var topLevel: Path
    private lateinit var result: TranslationResult

    @BeforeAll
    fun setup() {
        topLevel = Path.of("src", "test", "resources", "python")
    }

    fun analyzeFile(fileName: String) {
        result =
            analyze(listOf(topLevel.resolve(fileName).toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
    }

    @Test
    fun testTry() {
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("try.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val tryAll = tu.functions["tryAll"]?.trys?.singleOrNull()
        assertNotNull(tryAll)

        assertEquals(1, tryAll.tryBlock?.statements?.size)

        assertEquals(3, tryAll.catchClauses.size)
        assertLocalName("_", tryAll.catchClauses[0].parameter)
        assertLocalName("e", tryAll.catchClauses[1].parameter)
        assertNull(tryAll.catchClauses[2].parameter)

        assertEquals(1, tryAll.elseBlock?.statements?.size)
        assertEquals(1, tryAll.finallyBlock?.statements?.size)
    }

    @Test
    fun testAsync() {
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("async.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val myFunc = tu.functions["my_func"]
        assertNotNull(myFunc)
        assertEquals(1, myFunc.parameters.size)

        val myOtherFunc = tu.functions["my_other_func"]
        assertNotNull(myOtherFunc)
        assertEquals(1, myOtherFunc.parameters.size)
    }

    @Test
    fun testOperatorOverload() {
        analyzeFile("operator.py")

        with(result) {
            val numberType = assertResolvedType("operator.Number")
            val strType = assertResolvedType("str")

            // we should have an operator call to __add__ (+) now
            var opCall = result.operatorCalls("+").getOrNull(0)
            assertNotNull(opCall)
            assertEquals(numberType, opCall.type)

            val add = result.operators["__add__"]
            assertNotNull(add)
            assertEquals(add, opCall.invokes.singleOrNull())

            // ... and one to __pos__ (+)
            opCall = result.operatorCalls("+").getOrNull(1)
            assertNotNull(opCall)
            assertEquals(strType, opCall.type)

            val pos = result.operators["__pos__"]
            assertNotNull(pos)
            assertEquals(pos, opCall.invokes.singleOrNull())
        }
    }

    @Test
    fun testAssert() {
        analyzeFile("assert.py")

        val func = result.functions["test_assert"]
        assertNotNull(func, "Function 'test_assert' should be found")

        val assertStatement =
            func.body.statements.firstOrNull { it is AssertStatement } as? AssertStatement
        assertNotNull(assertStatement, "Assert statement should be found")

        val condition = assertStatement.condition
        assertNotNull(condition, "Assert statement should have a condition")
        assertEquals("1 == 1", condition.code, "The condition is incorrect")

        val message = assertStatement.message as? Literal<*>
        assertNotNull(message, "Assert statement should have a message")
        assertEquals("Test message", message.value, "The assert message is incorrect")
    }
}
