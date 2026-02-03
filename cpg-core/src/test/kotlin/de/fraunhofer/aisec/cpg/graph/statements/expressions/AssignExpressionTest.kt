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

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.function
import de.fraunhofer.aisec.cpg.graph.builder.translationResult
import de.fraunhofer.aisec.cpg.graph.builder.translationUnit
import de.fraunhofer.aisec.cpg.graph.types.TupleType
import de.fraunhofer.aisec.cpg.passes.DFGPass
import kotlin.test.*

class AssignExpressionTest {
    @Test
    fun propagateSimple() {
        with(TestLanguageFrontend()) {
            val refA = newReference("a")
            val refB = newReference("b")

            // Simple assignment from "b" to "a". Both types are unknown at this point
            val stmt = newAssignExpression(lhs = listOf(refA), rhs = listOf(refB))

            // Type listeners should be configured
            assertContains(refB.typeObservers, stmt)

            // Suddenly, we now we know the type of "b".
            refB.type = objectType("MyClass")
            // It should now propagate to the assigned type of "a"
            assertContains(refA.assignedTypes, objectType("MyClass"))

            val assignments = stmt.assignments
            assertEquals(1, assignments.size)
        }
    }

    @Test
    fun propagateTuple() {
        with(TestLanguageFrontend()) {
            val result = build {
                translationResult {
                    translationUnit {
                        val func =
                            function(
                                "func",
                                returnTypes = listOf(objectType("MyClass"), objectType("error")),
                            )

                        function("main") {
                            val refA = newReference("a")
                            val refErr = newReference("err")
                            val refFunc = newReference("func")
                            refFunc.refersTo = func
                            val call = newCallExpression(refFunc)

                            // Assignment from "func()" to "a" and "err".
                            val stmt =
                                newAssignExpression(lhs = listOf(refA, refErr), rhs = listOf(call))

                            body = newBlock()
                            body as Block += stmt
                        }
                    }
                }
            }

            val tu = result.components.flatMap { it.translationUnits }.firstOrNull()
            with(tu) {
                val call = tu.calls["func"]
                val func = tu.functions["func"]
                val refA = tu.refs["a"]
                val refErr = tu.refs["err"]

                assertNotNull(call)
                assertNotNull(func)
                assertNotNull(refA)
                assertNotNull(refErr)

                // This should now set the correct type of the call expression
                call.invokes = mutableListOf(func)
                assertIs<TupleType>(call.type)

                // We should at least know the "assigned" type of the references. Their declared
                // type is
                // still unknown to us, because we don't know the declarations.
                assertContains(refA.assignedTypes, objectType("MyClass"))
                assertContains(refErr.assignedTypes, objectType("error"))

                // Invoke the DFG pass
                DFGPass(ctx).accept(result.components.first())

                assertTrue(refA.prevDFG.contains(call))
                assertTrue(refErr.prevDFG.contains(call))

                val assignments = tu.assignments
                assertEquals(2, assignments.size)
            }
        }
    }
}
