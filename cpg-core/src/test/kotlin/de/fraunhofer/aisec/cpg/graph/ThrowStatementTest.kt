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

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.statements.ThrowStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import kotlin.test.*

class ThrowStatementTest {
    @Test
    fun testThrow() {
        val result =
            TestLanguageFrontend().build {
                translationResult {
                    translationUnit("some.file") {
                        function("foo", t("void")) {
                            body {
                                `throw` {}
                                `throw` { call("SomeError") }
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
        assertIs<ThrowStatement>(emptyThrow)
        assertNull(emptyThrow.exception)

        val throwWithExc = body.statements.getOrNull(1)
        assertIs<ThrowStatement>(throwWithExc)
        val throwCall = throwWithExc.exception
        assertIs<CallExpression>(throwCall)
        assertEquals("SomeError", throwCall.name.localName)
    }
}