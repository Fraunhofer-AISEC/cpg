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
package de.fraunhofer.aisec.cpg.testcases

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.passes.UnreachableEOGPass

abstract class AbstractEvaluationTests {
    companion object {
        /*
        public class IntegerTest {
            public void f1() {
                Bar b = new Bar();
                int a = 5;

                a = 0;
                a -= 2;
                a += 3;

                b.f(a);
            }

            public void f2() {
               Bar f = new Bar();
               int a = 5;

               a = 3;
               a++;
               ++a;
               a -= 2;
               a += 3;
               a--;
               --a;
               a *= 4;
               a /= 2;
               a %= 3;

               b.f(a);
            }

            public void f3() {
                Bar b = new Bar();
                int a = 5;

                if (new Random().nextBoolean()) {
                    a -= 1;
                }

                b.f(a);
            }

            public void f4() {
                Bar b = new Bar();
                int a = 5;

                if (new Random().nextBoolean()) {
                    a -= 1;
                } else {
                    a = 3;
                }

                b.f(a);
            }

            public void f5() {
                Bar b = new Bar();
                int a = 5;

                for (int i = 0; i < 5; i++) {
                    a += 1;
                }

                b.f(a);
            }
        }

        class Bar {
            public void f(int a) {}
        }
         */
        fun getIntegerExample(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .registerPass<UnreachableEOGPass>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("integer.java")
                scopeManager.resetToGlobal(tu)

                newRecord("Foo", "class", holder = tu, enterScope = true) { foo ->
                    newMethod("f1", holder = foo, enterScope = true) { method ->
                        method.returnTypes = listOf(unknownType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val bDeclStmt = newDeclarationStatement()
                                val b = newVariable("b", objectType("Bar"))
                                bDeclStmt.declarations += b
                                scopeManager.addDeclaration(b)
                                block += bDeclStmt

                                val aDeclStmt = newDeclarationStatement()
                                val a = newVariable("a", objectType("int"))
                                a.initializer = newLiteral(5, objectType("int"))
                                aDeclStmt.declarations += a
                                scopeManager.addDeclaration(a)
                                block += aDeclStmt

                                block +=
                                    newAssign(
                                        "=",
                                        listOf(newReference("a")),
                                        listOf(newLiteral(0, objectType("int"))),
                                    )
                                block +=
                                    newAssign(
                                        "-=",
                                        listOf(newReference("a")),
                                        listOf(newLiteral(2, objectType("int"))),
                                    )
                                block +=
                                    newAssign(
                                        "+=",
                                        listOf(newReference("a")),
                                        listOf(newLiteral(3, objectType("int"))),
                                    )

                                val fCall =
                                    newMemberCall(newMemberAccess("f", newReference("Bar")), false)
                                fCall.arguments += newReference("a")
                                block += fCall
                            }
                    }
                    newMethod("f2", holder = foo, enterScope = true) { method ->
                        method.returnTypes = listOf(unknownType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val bDeclStmt = newDeclarationStatement()
                                val b = newVariable("b", objectType("Bar"))
                                bDeclStmt.declarations += b
                                scopeManager.addDeclaration(b)
                                block += bDeclStmt

                                val aDeclStmt = newDeclarationStatement()
                                val a = newVariable("a", objectType("int"))
                                a.initializer = newLiteral(5, objectType("int"))
                                aDeclStmt.declarations += a
                                scopeManager.addDeclaration(a)
                                block += aDeclStmt

                                block +=
                                    newAssign(
                                        "=",
                                        listOf(newReference("a")),
                                        listOf(newLiteral(3, objectType("int"))),
                                    )

                                block +=
                                    newUnaryOperator("++", postfix = true, prefix = false).also {
                                        it.input = newReference("a")
                                    }
                                block +=
                                    newUnaryOperator("++", postfix = false, prefix = true).also {
                                        it.input = newReference("a")
                                    }

                                val cCall =
                                    newMemberCall(newMemberAccess("c", newReference("Bar")), false)
                                cCall.arguments += newReference("a")
                                block += cCall

                                block +=
                                    newAssign(
                                        "-=",
                                        listOf(newReference("a")),
                                        listOf(newLiteral(2, objectType("int"))),
                                    )
                                block +=
                                    newAssign(
                                        "+=",
                                        listOf(newReference("a")),
                                        listOf(newLiteral(3, objectType("int"))),
                                    )

                                block +=
                                    newUnaryOperator("--", postfix = true, prefix = false).also {
                                        it.input = newReference("a")
                                    }
                                block +=
                                    newUnaryOperator("--", postfix = false, prefix = true).also {
                                        it.input = newReference("a")
                                    }

                                block +=
                                    newAssign(
                                        "*=",
                                        listOf(newReference("a")),
                                        listOf(newLiteral(4, objectType("int"))),
                                    )
                                block +=
                                    newAssign(
                                        "/=",
                                        listOf(newReference("a")),
                                        listOf(newLiteral(2, objectType("int"))),
                                    )
                                block +=
                                    newAssign(
                                        "%=",
                                        listOf(newReference("a")),
                                        listOf(newLiteral(3, objectType("int"))),
                                    )

                                val fCall =
                                    newMemberCall(newMemberAccess("f", newReference("Bar")), false)
                                fCall.arguments += newReference("a")
                                block += fCall
                            }
                    }
                    newMethod("f3", holder = foo, enterScope = true) { method ->
                        method.returnTypes = listOf(unknownType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val bDeclStmt = newDeclarationStatement()
                                val b = newVariable("b", objectType("Bar"))
                                bDeclStmt.declarations += b
                                scopeManager.addDeclaration(b)
                                block += bDeclStmt

                                val aDeclStmt = newDeclarationStatement()
                                val a = newVariable("a", objectType("int"))
                                a.initializer = newLiteral(5, objectType("int"))
                                aDeclStmt.declarations += a
                                scopeManager.addDeclaration(a)
                                block += aDeclStmt

                                val ifElse = newIfElse { ifElse ->
                                    val nextBooleanCall =
                                        newMemberCall(
                                            newMemberAccess("nextBoolean", newReference("Random")),
                                            false,
                                        )
                                    ifElse.condition = nextBooleanCall

                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock +=
                                                newAssign(
                                                    "-=",
                                                    listOf(newReference("a")),
                                                    listOf(newLiteral(1, objectType("int"))),
                                                )
                                        }
                                }
                                block += ifElse

                                val fCall =
                                    newMemberCall(newMemberAccess("f", newReference("Bar")), false)
                                fCall.arguments += newReference("a")
                                block += fCall
                            }
                    }
                    newMethod("f4", holder = foo, enterScope = true) { method ->
                        method.returnTypes = listOf(unknownType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val bDeclStmt = newDeclarationStatement()
                                val b = newVariable("b", objectType("Bar"))
                                bDeclStmt.declarations += b
                                scopeManager.addDeclaration(b)
                                block += bDeclStmt

                                val aDeclStmt = newDeclarationStatement()
                                val a = newVariable("a", objectType("int"))
                                a.initializer = newLiteral(5, objectType("int"))
                                aDeclStmt.declarations += a
                                scopeManager.addDeclaration(a)
                                block += aDeclStmt

                                val ifElse = newIfElse { ifElse ->
                                    val nextBooleanCall =
                                        newMemberCall(
                                            newMemberAccess("nextBoolean", newReference("Random")),
                                            false,
                                        )
                                    ifElse.condition = nextBooleanCall

                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock +=
                                                newAssign(
                                                    "-=",
                                                    listOf(newReference("a")),
                                                    listOf(newLiteral(1, objectType("int"))),
                                                )
                                        }
                                    ifElse.elseStatement =
                                        newBlock(enterScope = true) { elseBlock ->
                                            elseBlock +=
                                                newAssign(
                                                    "=",
                                                    listOf(newReference("a")),
                                                    listOf(newLiteral(3, objectType("int"))),
                                                )
                                        }
                                }
                                block += ifElse

                                val fCall =
                                    newMemberCall(newMemberAccess("f", newReference("Bar")), false)
                                fCall.arguments += newReference("a")
                                block += fCall
                            }
                    }
                    newMethod("f5", holder = foo, enterScope = true) { method ->
                        method.returnTypes = listOf(unknownType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val bDeclStmt = newDeclarationStatement()
                                val b = newVariable("b", objectType("Bar"))
                                bDeclStmt.declarations += b
                                scopeManager.addDeclaration(b)
                                block += bDeclStmt

                                val aDeclStmt = newDeclarationStatement()
                                val a = newVariable("a", objectType("int"))
                                a.initializer = newLiteral(5, objectType("int"))
                                aDeclStmt.declarations += a
                                scopeManager.addDeclaration(a)
                                block += aDeclStmt

                                // Fluent's forStmt() never enters/leaves a scope for the `For`
                                // node itself (unlike whileStmt/forEachStmt), so the loop
                                // variable "i" and the loop body end up declared/evaluated
                                // directly in the enclosing method scope. Faithfully reproduced
                                // here by not passing enterScope to newFor()/newBlock() below.
                                val forNode = newFor { for_ ->
                                    val iDeclStmt = newDeclarationStatement()
                                    val i = newVariable("i", objectType("int"))
                                    i.initializer = newLiteral(0, objectType("int"))
                                    iDeclStmt.declarations += i
                                    scopeManager.addDeclaration(i)
                                    for_.initializerStatement = iDeclStmt

                                    for_.condition =
                                        newBinaryOperator("<").also {
                                            it.lhs = newReference("i")
                                            it.rhs = newLiteral(5, objectType("int"))
                                        }

                                    for_.iterationStatement =
                                        newUnaryOperator("++", postfix = true, prefix = false)
                                            .also { it.input = newReference("i") }

                                    for_.statement = newBlock { loopBodyBlock ->
                                        loopBodyBlock +=
                                            newAssign(
                                                "+=",
                                                listOf(newReference("a")),
                                                listOf(newLiteral(1, objectType("int"))),
                                            )

                                        val printlnCall = newCall(newReference("println"))
                                        printlnCall.arguments += newReference("i")
                                        loopBodyBlock += printlnCall
                                    }
                                }
                                block += forNode

                                val fCall =
                                    newMemberCall(newMemberAccess("f", newReference("Bar")), false)
                                fCall.arguments += newReference("a")
                                block += fCall
                            }
                    }
                    newMethod("f6", holder = foo, enterScope = true) { method ->
                        method.returnTypes = listOf(unknownType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val iDeclStmt = newDeclarationStatement()
                                val i = newVariable("i", objectType("int"))
                                i.initializer = newLiteral(0, objectType("int"))
                                iDeclStmt.declarations += i
                                scopeManager.addDeclaration(i)
                                block += iDeclStmt

                                val forNode = newFor { for_ ->
                                    for_.initializerStatement =
                                        newAssign(
                                            "=",
                                            listOf(newReference("i")),
                                            listOf(newLiteral(0, objectType("int"))),
                                        )

                                    for_.condition =
                                        newBinaryOperator("<").also {
                                            it.lhs = newReference("i")
                                            it.rhs = newLiteral(5, objectType("int"))
                                        }

                                    for_.iterationStatement =
                                        newUnaryOperator("++", postfix = true, prefix = false)
                                            .also { it.input = newReference("i") }

                                    for_.statement = newBlock { loopBodyBlock ->
                                        val innerIf = newIfElse { inner ->
                                            inner.condition =
                                                newBinaryOperator("<").also {
                                                    it.lhs = newReference("i")
                                                    it.rhs = newLiteral(3, objectType("int"))
                                                }

                                            inner.thenStatement =
                                                newBlock(enterScope = true) { tb ->
                                                    val call =
                                                        newCall(newReference("lessThanThree"))
                                                    call.arguments += newReference("i")
                                                    tb += call
                                                }
                                            inner.elseStatement =
                                                newBlock(enterScope = true) { eb ->
                                                    val call =
                                                        newCall(newReference("greaterEqualThree"))
                                                    call.arguments += newReference("i")
                                                    eb += call
                                                }
                                        }
                                        loopBodyBlock += innerIf

                                        val printlnCall = newCall(newReference("println"))
                                        printlnCall.arguments += newReference("i")
                                        loopBodyBlock += printlnCall
                                    }
                                }
                                block += forNode

                                val afterLoopCall = newCall(newReference("afterLoop"))
                                afterLoopCall.arguments += newReference("i")
                                block += afterLoopCall
                            }
                    }
                }
                newRecord("Bar", "class", holder = tu, enterScope = true) { bar ->
                    newMethod("main", holder = bar, enterScope = true) { method ->
                        method.returnTypes = listOf(unknownType())
                        method.type = computeType(method)

                        newParameter("a", objectType("int"), holder = method)

                        method.body = newBlock(enterScope = true)
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
    }
}
