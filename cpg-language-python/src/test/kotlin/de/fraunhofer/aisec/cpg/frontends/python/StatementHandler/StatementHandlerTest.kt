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
package de.fraunhofer.aisec.cpg.frontends.python.StatementHandler

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.statements.AssertStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeAll

class StatementHandlerTest {

    companion object {
        private lateinit var topLevel: Path
        private lateinit var result: TranslationResult

        @JvmStatic
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
