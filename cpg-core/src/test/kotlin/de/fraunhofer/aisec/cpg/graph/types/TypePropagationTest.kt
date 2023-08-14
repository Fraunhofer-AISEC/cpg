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

import de.fraunhofer.aisec.cpg.*
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
            // At this point, shortVar was target of an assignment of an int variable, however, the
            // int gets truncated into a short, so only short is part of the assigned types.
            assertEquals(primitiveType("short"), shortVarRefLhs.type)
            assertEquals(setOf(primitiveType("short")), shortVarRefLhs.assignedTypes)

            val shortVarRefReturnValue =
                main.allChildren<ReturnStatement>().firstOrNull()?.returnValue
            assertNotNull(shortVarRefReturnValue)
            // Finally, the assigned types should propagate along the DFG
            assertEquals(setOf(primitiveType("short")), shortVarRefLhs.assignedTypes)

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

    @Test
    fun testComplexPropagation() {
        val frontend =
            TestLanguageFrontend(
                ctx =
                    TranslationContext(
                        TranslationConfiguration.builder().defaultPasses().build(),
                        ScopeManager(),
                        TypeManager()
                    )
            )

        /**
         * This is a more complex scenario in which we want to follow some of the data-flows and see
         * how the types propagate.
         *
         * This roughly represents the following C++ code:
         * ```cpp
         * class BaseClass {
         *   virtual void doSomething();
         * };
         * class DerivedClassA : public BaseClass {
         *   void doSomething();
         * };
         * class DerivedClassB : public BaseClass {
         *   void doSomething();
         * };
         *
         * BaseClass *create(bool flip)
         * {
         *   // Create memory for a pointer to the base class
         *   BaseClass *b;
         *   // Create either DerivedClassA or DerivedClassB. This should assign both to the assigned types
         *   b = (flip == true) ? (BaseClass *)new DerivedClassA() : (BaseClass *)new DerivedClassB();
         *
         *   // Create a new array of pointers and assign our base class pointer to it
         *   auto bb = {b}
         *
         *   // Return the first element again with an array subscription expression
         *   return bb[0];
         * }
         *
         * int main() {
         *   // Call the create function. We don't know which of the derived classes we return
         *   BaseClass *b = create(random);
         *   b->doSomething();
         * }
         * ```
         */
        val result =
            frontend.build {
                translationResult {
                    translationUnit("test") {
                        record("BaseClass") { method("doSomething") }
                        record("DerivedClassA") {
                            superClasses = mutableListOf(t("BaseClass"))
                            method("doSomething")
                        }
                        record("DerivedClassB") {
                            superClasses = mutableListOf(t("BaseClass"))
                            method("doSomething")
                        }
                        function("create", t("BaseClass").pointer().pointer()) {
                            param("flip", t("bool"))
                            body {
                                declare { variable("b", t("BaseClass").pointer()) }
                                ref("b") assign
                                    {
                                        conditional(
                                            ref("flip") eq literal(true),
                                            cast(t("BaseClass").pointer()) {
                                                new {
                                                    construct("DerivedClassA")
                                                    type = t("DerivedClassA").pointer()
                                                }
                                            },
                                            cast(t("BaseClass").pointer()) {
                                                new {
                                                    construct("DerivedClassB")
                                                    type = t("DerivedClassB").pointer()
                                                }
                                            },
                                        )
                                    }
                                declare {
                                    variable("bb", autoType()) {
                                        ile(t("BaseClass").pointer().array()) { ref("b") }
                                    }
                                }
                                returnStmt {
                                    ase {
                                        ref("bb")
                                        literal(1)
                                    }
                                }
                            }
                        }
                        function("main", t("int")) {
                            body {
                                declare { variable("random", t("bool")) }
                                declare {
                                    variable("b", t("BaseClass").pointer()) {
                                        call("create") { ref("random") }
                                    }
                                }
                                call("b.doSomething")
                            }
                        }
                    }
                }
            }

        val baseClass = result.records["BaseClass"]
        assertNotNull(baseClass)

        val derivedClassA = result.records["DerivedClassA"]
        assertNotNull(derivedClassA)
        assertContains(derivedClassA.superTypeDeclarations, baseClass)

        val derivedClassB = result.records["DerivedClassB"]
        assertNotNull(derivedClassB)
        assertContains(derivedClassB.superTypeDeclarations, baseClass)

        val create = result.functions["create"]
        assertNotNull(create)

        with(create) {
            val b = variables["b"]
            assertNotNull(b)
            assertEquals(objectType("BaseClass").pointer(), b.type)

            val bRefs = refs("b")
            bRefs.forEach {
                // The "type" of a reference must always be the same as its declaration
                assertEquals(b.type, it.type)
                // The assigned types should now contain both classes and the base class
                assertEquals(
                    setOf(
                        objectType("BaseClass").pointer(),
                        objectType("DerivedClassA").pointer(),
                        objectType("DerivedClassB").pointer()
                    ),
                    it.assignedTypes
                )
            }

            val baseClassType = (b.type as? PointerType)?.elementType
            assertNotNull(baseClassType)

            assertEquals(
                baseClassType.array(),
                setOf(
                        baseClassType.array(),
                        derivedClassA.toType().array(),
                        derivedClassB.toType().array(),
                    )
                    .commonType
            )

            val assign = (body as CompoundStatement).statements<AssignExpression>(1)
            assertNotNull(assign)

            val bb = variables["bb"]
            assertNotNull(bb)
            // Auto type based on the initializer's type
            assertEquals(objectType("BaseClass").pointer().array(), bb.type)
            // Assigned types should additionally contain our two derived classes
            assertEquals(
                setOf(
                    objectType("BaseClass").pointer().array(),
                    objectType("DerivedClassA").pointer().array(),
                    objectType("DerivedClassB").pointer().array()
                ),
                bb.assignedTypes
            )

            val returnStatement = (body as CompoundStatement).statements<ReturnStatement>(3)
            assertNotNull(returnStatement)

            val returnValue = returnStatement.returnValue
            assertNotNull(returnValue)
            assertEquals(objectType("BaseClass").pointer(), returnValue.type)
            // The assigned types should now contain both classes and the base class in a non-array
            // form, since we are using a single element of the array
            assertEquals(
                setOf(
                    objectType("BaseClass").pointer(),
                    objectType("DerivedClassA").pointer(),
                    objectType("DerivedClassB").pointer()
                ),
                returnValue.assignedTypes
            )

            // At this point we stop for now since we do not properly propagate the types across
            // functions (yet)
        }

        val main = result.functions["main"]
        assertNotNull(main)

        with(main) {
            val createCall = main.calls["create"]
            assertNotNull(createCall)
            assertContains(createCall.invokes, create)

            val b = main.variables["b"]
            assertNotNull(b)
            assertEquals(objectType("BaseClass").pointer(), b.type)
        }
    }
}
