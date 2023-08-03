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
package de.fraunhofer.aisec.cpg.graph.types

import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import kotlin.test.*

class TypePropagationTest {
    @Test
    fun testBinopTypePropagation() {
        val result =
            TestLanguageFrontend().build {
                translationResult {
                    translationUnit("test") {
                        function("main", t("int")) {
                            body {
                                declare { variable("intVar", t("int")) {} }
                                declare { variable("intVar2", t("int")) { literal(5) } }
                                declare {
                                    variable("addResult", t("int")) {
                                        ref("intVar") + ref("intVar2")
                                    }
                                }
                                returnStmt { literal(0) }
                            }
                        }
                    }
                }
            }

        VariableUsageResolver(result.finalCtx).accept(result.components.first())

        val intVar = result.variables["intVar"]
        assertNotNull(intVar)
        assertLocalName("int", intVar.type)

        val intVarRef = result.refs["intVar"]
        assertNotNull(intVarRef)
        assertLocalName("int", intVarRef.type)

        val addResult = result.variables["addResult"]
        assertNotNull(addResult)

        val binaryOp = addResult.initializer
        assertNotNull(binaryOp)

        assertTrue(binaryOp.type is IntegerType)
        assertEquals("int", (binaryOp.type as IntegerType).name.toString())
        assertEquals(32, (binaryOp.type as IntegerType).bitWidth)

        assertTrue(result.finalCtx.typeObserverInvocations.get() < 20)
    }

    @Test
    fun testAssignTypePropagation() {
        val frontend = TestLanguageFrontend()

        /**
         * This roughly represents the following program in C:
         * ```c
         * int main() {
         *   int intVar;
         *   short shortVar;
         *   shortVar = intVar;
         *   return shortVar;
         * }
         * ```
         *
         * `shortVar` and `intVar` should hold `short` and `int` as their respective [HasType.type].
         * The assignment will then propagate `int` as the [HasType.assignedTypes] to `shortVar`.
         */
        val result =
            frontend.build {
                translationResult {
                    translationUnit("test") {
                        function("main", t("int")) {
                            body {
                                declare { variable("intVar", t("int")) {} }
                                declare { variable("shortVar", t("short")) {} }
                                ref("shortVar") assign ref("intVar")
                                returnStmt { ref("shortVar") }
                            }
                        }
                    }
                }
            }

        VariableUsageResolver(result.finalCtx).accept(result.components.first())
        EvaluationOrderGraphPass(result.finalCtx)
            .accept(result.components.first().translationUnits.first())
        ControlFlowSensitiveDFGPass(result.finalCtx)
            .accept(result.components.first().translationUnits.first())

        with(frontend) {
            val main = result.functions["main"]
            assertNotNull(main)

            val assign = (main.body as? CompoundStatement)?.statements?.get(2) as? AssignExpression
            assertNotNull(assign)

            val shortVar = main.variables["shortVar"]
            assertNotNull(shortVar)
            // At this point, shortVar should only have "short" as type and assigned types
            assertEquals(primitiveType("short"), shortVar.type)
            assertEquals(setOf(primitiveType("short")), shortVar.assignedTypes)

            val rhs = assign.rhs.firstOrNull() as? DeclaredReferenceExpression
            assertNotNull(rhs)
            assertIs<IntegerType>(rhs.type)
            assertLocalName("int", rhs.type)
            assertEquals(32, (rhs.type as IntegerType).bitWidth)

            val shortVarRefLhs = assign.lhs.firstOrNull() as? DeclaredReferenceExpression
            assertNotNull(shortVarRefLhs)
            // At this point, shortVar was target of an assignment of an int variable, therefore
            // assigned type should contain short and int. the "type" is still "short", since it is
            // only determined by the declaration
            assertEquals(primitiveType("short"), shortVarRefLhs.type)
            assertEquals(
                setOf(primitiveType("int"), primitiveType("short")),
                shortVarRefLhs.assignedTypes
            )

            val shortVarRefReturnValue =
                main.allChildren<ReturnStatement>().firstOrNull()?.returnValue
            assertNotNull(shortVarRefReturnValue)
            // Finally, the assigned types should propagate along the DFG, meaning that the
            // reference to shortVar in the return statement should also hold short and int as the
            // assigned types
            assertEquals(
                setOf(primitiveType("int"), primitiveType("short")),
                shortVarRefLhs.assignedTypes
            )

            val refersTo = shortVarRefLhs.refersTo as? VariableDeclaration
            assertNotNull(refersTo)
            assertIs<IntegerType>(refersTo.type)
            assertLocalName("short", refersTo.type)
            assertEquals(16, (refersTo.type as IntegerType).bitWidth)
        }
    }

    @Test
    fun testNewPropagation() {
        val frontend = TestLanguageFrontend()

        /**
         * This roughly represents the following C++ code:
         * ```cpp
         * int main() {
         *   BaseClass *b = new DerivedClass();
         *   b.doSomething();
         * }
         * ```
         */
        val result =
            frontend.build {
                translationResult {
                    translationUnit("test") {
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("b", t("BaseClass").pointer()) {
                                        new {
                                            construct("DerivedClass")
                                            type = t("DerivedClass").pointer()
                                        }
                                    }
                                }
                                call("b.doSomething")
                            }
                        }
                    }
                }
            }

        VariableUsageResolver(result.finalCtx).accept(result.components.first())

        with(frontend) {
            val main = result.functions["main"]
            assertNotNull(main)

            val b = main.variables["b"]
            assertNotNull(b)
            assertEquals(objectType("BaseClass").pointer(), b.type)
            assertEquals(
                setOf(
                    objectType("BaseClass").pointer(),
                    objectType("DerivedClass").pointer(),
                ),
                b.assignedTypes
            )

            val bRef = main.refs["b"]
            assertNotNull(bRef)
            assertEquals(b.type, bRef.type)
            assertEquals(b.assignedTypes, bRef.assignedTypes)
        }
    }
}