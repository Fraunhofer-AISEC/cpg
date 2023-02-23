/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.function
import de.fraunhofer.aisec.cpg.graph.builder.translationResult
import de.fraunhofer.aisec.cpg.graph.builder.translationUnit
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.types.TupleType
import de.fraunhofer.aisec.cpg.passes.DFGPass
import kotlin.test.*

class AssignExpressionTest {
    @Test
    fun propagateSimple() {
        with(TestLanguage()) {
            val refA = newDeclaredReferenceExpression("a")
            val refB = newDeclaredReferenceExpression("b")

            // Simple assignment from "b" to "a". Both types are unknown at this point
            val stmt = newAssignExpression(lhs = listOf(refA), rhs = listOf(refB))

            // Type listeners should be configured
            assertContains(refB.typeListeners, stmt)

            // Suddenly, we now we know the type of b.
            refB.type = parseType("MyClass")
            // It should now propagate to a
            assertLocalName("MyClass", refA.type)

            val assignments = stmt.assignments
            assertEquals(1, assignments.size)
        }
    }

    @Test
    fun propagateTuple() {
        with(TestLanguageFrontend()) {
            val result = build {
                translationResult(TranslationConfiguration.builder().build()) {
                    translationUnit {
                        val func =
                            function(
                                "func",
                                returnTypes = listOf(parseType("MyClass"), parseType("error"))
                            )
                        function("main") {
                            val refA = newDeclaredReferenceExpression("a")
                            val refErr = newDeclaredReferenceExpression("err")
                            val refFunc = newDeclaredReferenceExpression("func")
                            refFunc.refersTo = func
                            val call = newCallExpression(refFunc)

                            // Assignment from "func()" to "a" and "err".
                            val stmt =
                                newAssignExpression(lhs = listOf(refA, refErr), rhs = listOf(call))

                            body = newCompoundStatement()
                            body as CompoundStatement += stmt
                        }
                    }
                }
            }

            val tu = result.translationUnits.firstOrNull()
            val call = tu.calls["func"]
            val func = tu.functions["func"]
            val refA = tu.refs["a"]
            val refErr = tu.refs["err"]

            assertNotNull(call)
            assertNotNull(func)
            assertNotNull(refA)
            assertNotNull(refErr)

            // This should now set the correct type of the call expression
            call.invokes = listOf(func)
            assertIs<TupleType>(call.type)

            assertLocalName("MyClass", refA.type)
            assertLocalName("error", refErr.type)

            // Invoke the DFG pass
            DFGPass().accept(result)

            assertTrue(refA.prevDFG.contains(call))
            assertTrue(refErr.prevDFG.contains(call))

            val assignments = tu.assignments
            assertEquals(2, assignments.size)
        }
    }
}
