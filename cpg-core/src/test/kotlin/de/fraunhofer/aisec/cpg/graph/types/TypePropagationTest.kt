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
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import de.fraunhofer.aisec.cpg.test.*
import kotlin.test.*

class TypePropagationTest {
    @Test
    fun testBinopTypePropagation() {
        val frontend =
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )
        val result =
            frontend.build {
                val tu = newTranslationUnit("test")
                scopeManager.resetToGlobal(tu)

                val mainReturnType = objectType("int")
                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(mainReturnType)
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val intVarDeclStmt = newDeclarationStatement()
                            val intVar = newVariable("intVar", objectType("int"))
                            intVarDeclStmt.declarations += intVar
                            scopeManager.addDeclaration(intVar)
                            block.statements += intVarDeclStmt

                            val intVar2DeclStmt = newDeclarationStatement()
                            val intVar2 =
                                newVariable("intVar2", objectType("int")) {
                                    it.initializer = newLiteral(5)
                                }
                            intVar2DeclStmt.declarations += intVar2
                            scopeManager.addDeclaration(intVar2)
                            block.statements += intVar2DeclStmt

                            val addResultDeclStmt = newDeclarationStatement()
                            val addResult =
                                newVariable("addResult", objectType("int")).also {
                                    it.initializer =
                                        newBinaryOperator("+").also { bin ->
                                            bin.lhs = newReference("intVar")
                                            bin.rhs = newReference("intVar2")
                                        }
                                }
                            addResultDeclStmt.declarations += addResult
                            scopeManager.addDeclaration(addResult)
                            block.statements += addResultDeclStmt

                            block.statements += newReturn().also { it.returnValue = newLiteral(0) }
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

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
        val frontend =
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )

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
                val tu = newTranslationUnit("test")
                scopeManager.resetToGlobal(tu)

                val mainReturnType = objectType("int")
                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(mainReturnType)
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val intVarDeclStmt = newDeclarationStatement()
                            val intVar = newVariable("intVar", objectType("int"))
                            intVarDeclStmt.declarations += intVar
                            scopeManager.addDeclaration(intVar)
                            block.statements += intVarDeclStmt

                            val shortVarDeclStmt = newDeclarationStatement()
                            val shortVar = newVariable("shortVar", objectType("short"))
                            shortVarDeclStmt.declarations += shortVar
                            scopeManager.addDeclaration(shortVar)
                            block.statements += shortVarDeclStmt

                            block.statements +=
                                newAssign(
                                    "=",
                                    listOf(newReference("shortVar")),
                                    listOf(newReference("intVar")),
                                )

                            block.statements +=
                                newReturn().also { it.returnValue = newReference("shortVar") }
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        with(frontend) {
            val main = result.functions["main"]
            assertNotNull(main)

            val assign = (main.body as? Block)?.statements?.get(2) as? Assign
            assertNotNull(assign)

            val shortVar = main.variables["shortVar"]
            assertNotNull(shortVar)
            // At this point, shortVar should only have "short" as type and assigned types
            assertEquals(primitiveType("short"), shortVar.type)
            assertEquals(setOf(primitiveType("short")), shortVar.assignedTypes)

            val rhs = assign.rhs.firstOrNull() as? Reference
            assertNotNull(rhs)
            assertIs<IntegerType>(rhs.type)
            assertLocalName("int", rhs.type)
            assertEquals(32, (rhs.type as IntegerType).bitWidth)

            val shortVarRefLhs = assign.lhs.firstOrNull() as? Reference
            assertNotNull(shortVarRefLhs)
            // At this point, shortVar was target of an assignment of an int variable, however, the
            // int gets truncated into a short, so only short is part of the assigned types.
            assertEquals(primitiveType("short"), shortVarRefLhs.type)
            assertEquals(setOf(primitiveType("short")), shortVarRefLhs.assignedTypes)

            val shortVarRefReturnValue = main.allChildren<Return>().firstOrNull()?.returnValue
            assertNotNull(shortVarRefReturnValue)
            // Finally, the assigned types should propagate along the DFG
            assertEquals(setOf(primitiveType("short")), shortVarRefLhs.assignedTypes)

            val refersTo = shortVarRefLhs.refersTo as? Variable
            assertNotNull(refersTo)
            assertIs<IntegerType>(refersTo.type)
            assertLocalName("short", refersTo.type)
            assertEquals(16, (refersTo.type as IntegerType).bitWidth)
        }
    }

    @Test
    fun testNewPropagation() {
        val frontend =
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )

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
                val tu = newTranslationUnit("test")
                scopeManager.resetToGlobal(tu)

                val mainReturnType = objectType("int")
                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(mainReturnType)
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val bDeclStmt = newDeclarationStatement()
                            val b =
                                newVariable("b", objectType("BaseClass").pointer()).also { v ->
                                    v.initializer =
                                        newNew().also { newExpr ->
                                            newExpr.initializer =
                                                newConstruction("DerivedClass").also {
                                                    it.type = objectType("DerivedClass")
                                                }
                                            newExpr.type = objectType("DerivedClass").pointer()
                                        }
                                }
                            bDeclStmt.declarations += b
                            scopeManager.addDeclaration(b)
                            block.statements += bDeclStmt

                            block.statements +=
                                newMemberCall(
                                    newMemberAccess("doSomething", newReference("b")),
                                    false,
                                )
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        with(frontend) {
            val main = result.functions["main"]
            assertNotNull(main)

            val b = main.variables["b"]
            assertNotNull(b)
            assertEquals(assertResolvedType("BaseClass").pointer(), b.type)
            assertEquals(
                setOf(
                    assertResolvedType("BaseClass").pointer(),
                    assertResolvedType("DerivedClass").pointer(),
                ),
                b.assignedTypes,
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
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
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
                val tu = newTranslationUnit("test")
                scopeManager.resetToGlobal(tu)

                newRecord("BaseClass", "class", holder = tu, enterScope = true) { record ->
                    newMethod("doSomething", holder = record, enterScope = true) { m ->
                        m.returnTypes = listOf(unknownType())
                        m.type = computeType(m)
                    }
                }
                newRecord("DerivedClassA", "class", holder = tu, enterScope = true) { record ->
                    record.superClasses = mutableListOf(objectType("BaseClass"))
                    newMethod("doSomething", holder = record, enterScope = true) { m ->
                        m.returnTypes = listOf(unknownType())
                        m.type = computeType(m)
                    }
                }
                newRecord("DerivedClassB", "class", holder = tu, enterScope = true) { record ->
                    record.superClasses = mutableListOf(objectType("BaseClass"))
                    newMethod("doSomething", holder = record, enterScope = true) { m ->
                        m.returnTypes = listOf(unknownType())
                        m.type = computeType(m)
                    }
                }

                val createReturnType = objectType("BaseClass").pointer().pointer()
                newFunction("create", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(createReturnType)
                    func.type = computeType(func)

                    newParameter("flip", objectType("boolean"), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val bDeclStmt = newDeclarationStatement()
                            val b = newVariable("b", objectType("BaseClass").pointer())
                            bDeclStmt.declarations += b
                            scopeManager.addDeclaration(b)
                            block.statements += bDeclStmt

                            block.statements +=
                                newAssign(
                                    "=",
                                    listOf(newReference("b")),
                                    listOf(
                                        newConditional(
                                            newBinaryOperator("==").also {
                                                it.lhs = newReference("flip")
                                                it.rhs = newLiteral(true)
                                            },
                                            newCast().also { cast ->
                                                cast.castType = objectType("BaseClass").pointer()
                                                cast.expression =
                                                    newNew().also { newExpr ->
                                                        newExpr.initializer =
                                                            newConstruction("DerivedClassA").also {
                                                                it.type =
                                                                    objectType("DerivedClassA")
                                                            }
                                                        newExpr.type =
                                                            objectType("DerivedClassA").pointer()
                                                    }
                                            },
                                            newCast().also { cast ->
                                                cast.castType = objectType("BaseClass").pointer()
                                                cast.expression =
                                                    newNew().also { newExpr ->
                                                        newExpr.initializer =
                                                            newConstruction("DerivedClassB").also {
                                                                it.type =
                                                                    objectType("DerivedClassB")
                                                            }
                                                        newExpr.type =
                                                            objectType("DerivedClassB").pointer()
                                                    }
                                            },
                                        )
                                    ),
                                )

                            val bbDeclStmt = newDeclarationStatement()
                            val bb =
                                newVariable("bb", autoType()).also { v ->
                                    v.initializer =
                                        newInitializerList(
                                                objectType("BaseClass").pointer().array()
                                            )
                                            .also { it.initializers += newReference("b") }
                                }
                            bbDeclStmt.declarations += bb
                            scopeManager.addDeclaration(bb)
                            block.statements += bbDeclStmt

                            block.statements +=
                                newReturn().also { ret ->
                                    ret.returnValue =
                                        newSubscription().also { sub ->
                                            sub.arrayExpression = newReference("bb")
                                            sub.subscriptExpression = newLiteral(1)
                                        }
                                }
                        }
                }

                val mainReturnType = objectType("int")
                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(mainReturnType)
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val randomDeclStmt = newDeclarationStatement()
                            val random = newVariable("random", objectType("boolean"))
                            randomDeclStmt.declarations += random
                            scopeManager.addDeclaration(random)
                            block.statements += randomDeclStmt

                            val bDeclStmt = newDeclarationStatement()
                            val b =
                                newVariable("b", objectType("BaseClass").pointer()).also { v ->
                                    v.initializer =
                                        newCall(newReference("create")).also { call ->
                                            call.arguments += newReference("random")
                                        }
                                }
                            bDeclStmt.declarations += b
                            scopeManager.addDeclaration(b)
                            block.statements += bDeclStmt

                            block.statements +=
                                newMemberCall(
                                    newMemberAccess("doSomething", newReference("b")),
                                    false,
                                )
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        val baseClass = result.records["BaseClass"]
        assertNotNull(baseClass)

        val derivedClassA = result.records["DerivedClassA"]
        assertNotNull(derivedClassA)
        // TODO: For some reason, the element is not found in the set. This is strange because the
        // hashCode matches, the equals matches, identity matches...
        assertContains(derivedClassA.superTypeDeclarations.toIdentitySet(), baseClass)

        val derivedClassB = result.records["DerivedClassB"]
        assertNotNull(derivedClassB)
        assertContains(derivedClassB.superTypeDeclarations.toIdentitySet(), baseClass)

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
                        objectType("DerivedClassB").pointer(),
                    ),
                    it.assignedTypes,
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
                    .commonType,
            )

            val assign = (body as Block).statements<Assign>(1)
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
                    objectType("DerivedClassB").pointer().array(),
                ),
                bb.assignedTypes,
            )

            val returnStatement = (body as Block).statements<Return>(3)
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
                    objectType("DerivedClassB").pointer(),
                ),
                returnValue.assignedTypes,
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
