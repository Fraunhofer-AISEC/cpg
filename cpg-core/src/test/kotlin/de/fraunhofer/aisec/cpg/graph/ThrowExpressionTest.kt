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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.ast.statements.ThrowExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.test.assertLocalName
import kotlin.test.*

class ThrowExpressionTest {
    @Test
    fun testThrow() {
        val result =
            testFrontend(
                    TranslationConfiguration.builder()
                        .defaultPasses()
                        .registerLanguage<TestLanguage>()
                        .build()
                )
                .build {
                    translationResult {
                        translationUnit("some.file") {
                            function("foo", t("void")) {
                                body {
                                    `throw` {}
                                    `throw` { call("SomeError") }
                                    `throw` {
                                        call("SomeError")
                                        call("SomeError2")
                                    }
                                }
                            }
                        }
                    }
                }

        // Let's assert that we did this correctly
        val main = result.functions["foo"]
        assertNotNull(main)
        val body = main.body
        assertIs<Block>(body)

        val emptyThrow = body.statements.getOrNull(0)
        assertIs<ThrowExpression>(emptyThrow)
        println(emptyThrow.toString()) // This is only here to simulate a higher test coverage
        assertNull(emptyThrow.exception)
        assertTrue(emptyThrow.prevDFG.isEmpty())

        val throwWithExc = body.statements.getOrNull(1)
        assertIs<ThrowExpression>(throwWithExc)
        println(throwWithExc.toString()) // This is only here to simulate a higher test coverage
        val throwCall = throwWithExc.exception
        assertIs<CallExpression>(throwCall)
        assertLocalName("SomeError", throwCall)
        assertEquals(setOf<Node>(throwCall), throwWithExc.prevDFG.toSet())

        val throwWithExcAndParent = body.statements.getOrNull(2)
        assertIs<ThrowExpression>(throwWithExcAndParent)
        println(
            throwWithExcAndParent.toString()
        ) // This is only here to simulate a higher test coverage
        val throwCallException = throwWithExcAndParent.exception
        assertIs<CallExpression>(throwCallException)
        assertLocalName("SomeError", throwCallException)
        val throwCallParent = throwWithExcAndParent.parentException
        assertIs<CallExpression>(throwCallParent)
        assertLocalName("SomeError2", throwCallParent)
        assertEquals(
            setOf<Node>(throwCallException, throwCallParent),
            throwWithExcAndParent.prevDFG.toSet(),
        )
    }
}
