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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertLocalName
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExceptionsTest : BaseTest() {

    private lateinit var topLevel: Path
    private lateinit var result: TranslationResult

    @BeforeAll
    fun setup() {
        topLevel = Path.of("src", "test", "resources", "python")

        result =
            analyze(listOf(topLevel.resolve("raise.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
    }

    @Test
    fun testWithExceptionAndTry() {
        val function = result.allFunctions["raise_in_try"]
        assertNotNull(function)
        val throwStmt = function.allThrows.singleOrNull()
        assertNotNull(throwStmt)

        val exception = throwStmt.exception
        assertIs<CallExpression>(exception)
        assertLocalName("Exception", exception)
        assertNull(throwStmt.parentException)
        assertEquals(listOf<Node>(exception), throwStmt.prevEOG)

        val catchClause =
            function.allTrys.singleOrNull()?.catchClauses?.singleOrNull {
                it.parameter?.type?.name?.localName == "Exception"
            }
        assertNotNull(catchClause)
        assertEquals(listOf<Node>(catchClause), throwStmt.nextEOG)
    }

    @Test
    fun testWithExceptionAndTry2() {
        val function = result.allFunctions["raise_in_try2"]
        assertNotNull(function)
        val throwStmt = function.allThrows.singleOrNull()
        assertNotNull(throwStmt)

        val exception = throwStmt.exception
        assertIs<CallExpression>(exception)
        assertLocalName("Exception", exception)
        assertNull(throwStmt.parentException)
        assertEquals(listOf<Node>(exception), throwStmt.prevEOG)

        val catchClause =
            function.allTrys.singleOrNull()?.catchClauses?.singleOrNull {
                it.parameter?.type?.name?.localName == "Exception"
            }
        assertNotNull(catchClause)
        assertEquals(listOf<Node>(catchClause), throwStmt.nextEOG)
    }

    @Test
    fun testWithoutTry() {
        val function = result.allFunctions["raise_without_try"]
        assertNotNull(function)
        val throwStmt = function.allThrows.singleOrNull()
        assertNotNull(throwStmt)

        val exception = throwStmt.exception
        assertIs<CallExpression>(exception)
        assertLocalName("Exception", exception)
        assertNull(throwStmt.parentException)
        assertEquals(listOf<Node>(exception), throwStmt.prevEOG)

        val functionBody = function.body
        assertNotNull(functionBody)
        assertEquals(listOf<Node>(functionBody), throwStmt.nextEOG.toList())
    }

    @Test
    fun testWithParent() {
        val function = result.allFunctions["raise_with_parent"]
        assertNotNull(function)
        val throwStmt = function.allThrows.singleOrNull()
        assertNotNull(throwStmt)

        val exception = throwStmt.exception
        assertIs<CallExpression>(exception)
        assertLocalName("Exception", exception)
        val parent = throwStmt.parentException
        assertIs<CallExpression>(parent)
        assertLocalName("A", parent)
        assertEquals(listOf<Node>(parent), throwStmt.prevEOG)
        assertEquals(listOf<Node>(exception), parent.prevEOG)

        val functionBody = function.body
        assertNotNull(functionBody)
        assertEquals(listOf<Node>(functionBody), throwStmt.nextEOG.toList())
    }

    @Test
    fun testEmpty() {
        val function = result.allFunctions["raise_empty"]
        assertNotNull(function)
        val throwStmt = function.allThrows.singleOrNull()
        assertNotNull(throwStmt)

        assertNull(throwStmt.exception)
        assertNull(throwStmt.parentException)

        val functionBody = function.body
        assertNotNull(functionBody)
        // TODO: This doesn't work yet. Needs fix in the EOG pass!
        // assertEquals(listOf<Node>(functionBody), throwStmt.nextEOG.toList())
    }
}
