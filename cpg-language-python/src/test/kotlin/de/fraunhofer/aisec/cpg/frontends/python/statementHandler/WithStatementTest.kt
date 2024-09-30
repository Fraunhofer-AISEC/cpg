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
import de.fraunhofer.aisec.cpg.graph.statements
import de.fraunhofer.aisec.cpg.graph.statements.TryStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WithStatementTest : BaseTest() {

    private lateinit var topLevel: Path
    private lateinit var result: TranslationResult

    @BeforeAll
    fun setup() {
        topLevel = Path.of("src", "test", "resources", "python")
        analyzeFile()
    }

    fun analyzeFile() {
        result =
            analyze(listOf(topLevel.resolve("with.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
    }

    @Test
    fun testWithSingleStatement() {
        val tryStatements = result.statements.filterIsInstance<TryStatement>()
        assertEquals(3, tryStatements.size)

        // Test: with open("file.txt", "r") as file:
        val tryStatement = tryStatements[0]
        val callExpressionsFirst = tryStatement.resources.filterIsInstance<CallExpression>()
        assertEquals(1, callExpressionsFirst.size)

        val reference = tryStatement.resources.filterIsInstance<Reference>()
        assertEquals(1, reference.size)

        val finallyBlock = tryStatement.finallyBlock
        assertNotNull(finallyBlock)
        assertEquals(true, finallyBlock.isImplicit)

        val memberCallExpression = finallyBlock.statements.filterIsInstance<MemberCallExpression>()
        assertEquals(1, memberCallExpression.size)
    }

    @Test
    fun testWithMultipleStatements() {
        val tryStatements = result.statements.filterIsInstance<TryStatement>()
        assertEquals(3, tryStatements.size)

        // Test: with open('file1.txt') as f1, open('file2.txt') as f2:
        val tryStatement = tryStatements[1]

        val callExpression = tryStatement.resources.filterIsInstance<CallExpression>()
        assertEquals(2, callExpression.size)

        val reference = tryStatement.resources.filterIsInstance<Reference>()
        assertEquals(2, reference.size)

        val finallyBlock = tryStatement.finallyBlock
        assertNotNull(finallyBlock)
        assertEquals(true, finallyBlock.isImplicit)

        val memberCallExpression = finallyBlock.statements.filterIsInstance<MemberCallExpression>()
        assertEquals(1, memberCallExpression.size)
    }

    @Test
    fun testWithTypeComment() {
        with(result) {
            val tryStatements = statements.filterIsInstance<TryStatement>()
            assertEquals(3, tryStatements.size)

            // Test: with MyCustomType() as my_type: #type: MyCustomType
            val tryStatement = tryStatements[2]
            val callExpressionsFirst = tryStatement.resources.filterIsInstance<CallExpression>()
            assertEquals(1, callExpressionsFirst.size)

            val reference = tryStatement.resources.filterIsInstance<Reference>()
            assertEquals(1, reference.size)
            assertContains("MyCustomType", reference.first().type.name.localName)

            val finallyBlock = tryStatement.finallyBlock
            assertNotNull(finallyBlock)
            assertEquals(true, finallyBlock.isImplicit)

            val memberCallExpression =
                finallyBlock.statements.filterIsInstance<MemberCallExpression>()
            assertEquals(1, memberCallExpression.size)
        }
    }
}
