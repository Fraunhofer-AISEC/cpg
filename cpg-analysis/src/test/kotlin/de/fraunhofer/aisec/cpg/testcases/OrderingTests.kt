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
package de.fraunhofer.aisec.cpg.testcases

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.passes.UnreachableEOGPass

class GraphExamples {

    companion object {
        fun getSimpleOrder(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .registerPass<UnreachableEOGPass>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("SimpleOrder.java")
                scopeManager.resetToGlobal(tu)

                newInclude("kotlin.random.URandomKt", holder = tu)

                newRecord("Botan", "class", holder = tu, enterScope = true) { botan ->
                    newConstructor(
                        botan.name,
                        recordDeclaration = botan,
                        holder = botan,
                        enterScope = true,
                    ) { ctor ->
                        newParameter("i", objectType("int"), holder = ctor)
                        ctor.body = newBlock(enterScope = true)
                    }
                    newMethod("create", holder = botan, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)
                        method.body = newBlock(enterScope = true)
                    }
                    newMethod("finish", holder = botan, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)
                        newParameter("b", objectType("char").array(), holder = method)
                        method.body = newBlock(enterScope = true)
                    }
                    newMethod("init", holder = botan, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)
                        method.body = newBlock(enterScope = true)
                    }
                    newMethod("process", holder = botan, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)
                        method.body = newBlock(enterScope = true)
                    }
                    newMethod("reset", holder = botan, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)
                        method.body = newBlock(enterScope = true)
                    }
                    newMethod("start", holder = botan, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)
                        newParameter("i", objectType("int"), holder = method)
                        method.body = newBlock(enterScope = true)
                    }
                    newMethod("set_key", holder = botan, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)
                        newParameter("i", objectType("int"), holder = method)
                        method.body = newBlock(enterScope = true)
                    }
                }

                newRecord("SimpleOrder", "class", holder = tu, enterScope = true) { record ->
                    newField("cipher", objectType("char").array(), holder = record)
                    newField("key", objectType("int"), holder = record)
                    newField("iv", objectType("int"), holder = record)
                    newField("direction", objectType("Cipher_Dir"), holder = record)
                    newField("buf", objectType("char").array(), holder = record)

                    newMethod("ok", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p4Decl = newDeclarationStatement()
                                val p4 = newVariable("p4", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p4.initializer = construction
                                p4Decl.declarations += p4
                                scopeManager.addDeclaration(p4)
                                block += p4Decl

                                val startCall =
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    )
                                startCall.addArgument(newReference("iv"))
                                block += startCall

                                val finishCall =
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    )
                                finishCall.addArgument(newReference("buf"))
                                block += finishCall

                                block += newReturn()
                            }
                    }

                    newMethod("ok2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p4Decl = newDeclarationStatement()
                                val p4 = newVariable("p4", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p4.initializer = construction
                                p4Decl.declarations += p4
                                scopeManager.addDeclaration(p4)
                                block += p4Decl

                                val startCall =
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    )
                                startCall.addArgument(newReference("iv"))
                                block += startCall

                                // Not in the entity and therefore ignored
                                block +=
                                    newMemberCall(newMemberAccess("foo", newReference("p4")), false)

                                val finishCall =
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    )
                                finishCall.addArgument(newReference("buf"))
                                block += finishCall

                                block += newReturn()
                            }
                    }

                    newMethod("ok3", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p4Decl = newDeclarationStatement()
                                val p4 = newVariable("p4", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p4.initializer = construction
                                p4Decl.declarations += p4
                                scopeManager.addDeclaration(p4)
                                block += p4Decl

                                val xDecl = newDeclarationStatement()
                                val x = newVariable("x", objectType("int"))
                                x.initializer =
                                    newMemberCall(
                                        newMemberAccess("nextUInt", newReference("URandomKt")),
                                        false,
                                    )
                                xDecl.declarations += x
                                scopeManager.addDeclaration(x)
                                block += xDecl

                                val ifElse = newIfElse { ifElse ->
                                    ifElse.condition =
                                        newBinaryOperator("<=").also {
                                            it.lhs = newReference("x")
                                            it.rhs = newLiteral(5, objectType("int"))
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            val startCall =
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p4")),
                                                    false,
                                                )
                                            startCall.addArgument(newReference("iv"))
                                            thenBlock += startCall
                                        }
                                    ifElse.elseStatement =
                                        newBlock(enterScope = true) { elseBlock ->
                                            val startCall =
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p4")),
                                                    false,
                                                )
                                            startCall.addArgument(newReference("iv"))
                                            elseBlock += startCall
                                        }
                                }
                                block += ifElse

                                // Not in the entity and therefore ignored
                                block +=
                                    newMemberCall(newMemberAccess("foo", newReference("p4")), false)

                                val finishCall =
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    )
                                finishCall.addArgument(newReference("buf"))
                                block += finishCall

                                block += newReturn()
                            }
                    }

                    newMethod("nok1", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p4Decl = newDeclarationStatement()
                                val p4 = newVariable("p4", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(1, objectType("int")))
                                p4.initializer = construction
                                p4Decl.declarations += p4
                                scopeManager.addDeclaration(p4)
                                block += p4Decl

                                // Not allowed as start
                                val setKeyCall1 =
                                    newMemberCall(
                                        newMemberAccess("set_key", newReference("p4")),
                                        false,
                                    )
                                setKeyCall1.addArgument(newReference("key"))
                                block += setKeyCall1

                                val startCall =
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    )
                                startCall.addArgument(newReference("iv"))
                                block += startCall

                                val finishCall =
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    )
                                finishCall.addArgument(newReference("buf"))
                                block += finishCall

                                // Not in the entity and therefore ignored
                                block +=
                                    newMemberCall(newMemberAccess("foo", newReference("p4")), false)

                                val setKeyCall2 =
                                    newMemberCall(
                                        newMemberAccess("set_key", newReference("p4")),
                                        false,
                                    )
                                setKeyCall2.addArgument(newReference("key"))
                                block += setKeyCall2

                                block += newReturn()
                            }
                    }

                    newMethod("nok2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p4Decl = newDeclarationStatement()
                                val p4 = newVariable("p4", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p4.initializer = construction
                                p4Decl.declarations += p4
                                scopeManager.addDeclaration(p4)
                                block += p4Decl

                                val startCall =
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    )
                                startCall.addArgument(newReference("iv"))
                                block += startCall

                                // Missing: memberCall("finish", ref("p4")) {ref("buf")}

                                block += newReturn()
                            }
                    }

                    newMethod("nok3", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p4Decl = newDeclarationStatement()
                                val p4 = newVariable("p4", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p4.initializer = construction
                                p4Decl.declarations += p4
                                scopeManager.addDeclaration(p4)
                                block += p4Decl

                                val ifElse = newIfElse { ifElse ->
                                    val nextUIntCall =
                                        newMemberCall(
                                            newMemberAccess("nextUInt", newReference("URandomKt")),
                                            false,
                                        )
                                    ifElse.condition =
                                        newBinaryOperator("<=").also {
                                            it.lhs = nextUIntCall
                                            it.rhs = newLiteral(5, objectType("int"))
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            val startCall =
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p4")),
                                                    false,
                                                )
                                            startCall.addArgument(newReference("iv"))
                                            thenBlock += startCall
                                        }
                                }
                                block += ifElse

                                // start could be missing here
                                val finishCall =
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    )
                                finishCall.addArgument(newReference("buf"))
                                block += finishCall

                                block += newReturn()
                            }
                    }

                    newMethod("nok4", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p4Decl = newDeclarationStatement()
                                val p4 = newVariable("p4", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p4.initializer = construction
                                p4Decl.declarations += p4
                                scopeManager.addDeclaration(p4)
                                block += p4Decl

                                val ifElse = newIfElse { ifElse ->
                                    ifElse.condition = newLiteral(true, objectType("boolean"))
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            val startCall =
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p4")),
                                                    false,
                                                )
                                            startCall.addArgument(newReference("iv"))
                                            thenBlock += startCall

                                            val finishCall =
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p4")),
                                                    false,
                                                )
                                            finishCall.addArgument(newReference("buf"))
                                            thenBlock += finishCall
                                        }
                                }
                                block += ifElse

                                // Not ok because p4 is already finished
                                val startCall2 =
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    )
                                startCall2.addArgument(newReference("iv"))
                                block += startCall2

                                val finishCall2 =
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    )
                                finishCall2.addArgument(newReference("buf"))
                                block += finishCall2

                                block += newReturn()
                            }
                    }

                    newMethod("nok5", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                block +=
                                    newBlock(enterScope = true) { nested1 ->
                                        val p4Decl = newDeclarationStatement()
                                        val p4 = newVariable("p4", objectType("Botan"))
                                        val construction = newConstruction("Botan")
                                        construction.type = objectType("Botan")
                                        construction.addArgument(newLiteral(2, objectType("int")))
                                        p4.initializer = construction
                                        p4Decl.declarations += p4
                                        scopeManager.addDeclaration(p4)
                                        nested1 += p4Decl

                                        val startCall =
                                            newMemberCall(
                                                newMemberAccess("start", newReference("p4")),
                                                false,
                                            )
                                        startCall.addArgument(newReference("iv"))
                                        nested1 += startCall
                                    }

                                block +=
                                    newBlock(enterScope = true) { nested2 ->
                                        val p5Decl = newDeclarationStatement()
                                        val p5 = newVariable("p5", objectType("Botan"))
                                        val construction = newConstruction("Botan")
                                        construction.type = objectType("Botan")
                                        construction.addArgument(newLiteral(2, objectType("int")))
                                        p5.initializer = construction
                                        p5Decl.declarations += p5
                                        scopeManager.addDeclaration(p5)
                                        nested2 += p5Decl

                                        val finishCall =
                                            newMemberCall(
                                                newMemberAccess("finish", newReference("p5")),
                                                false,
                                            )
                                        finishCall.addArgument(newReference("buf"))
                                        nested2 += finishCall

                                        nested2 += newReturn()
                                    }
                            }
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getComplexOrder(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .registerPass<UnreachableEOGPass>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("ComplexOrder.java")
                scopeManager.resetToGlobal(tu)

                newInclude("kotlin.random.URandomKt", holder = tu)

                newRecord("Botan", "class", holder = tu, enterScope = true) { botan ->
                    newConstructor(
                        botan.name,
                        recordDeclaration = botan,
                        holder = botan,
                        enterScope = true,
                    ) { ctor ->
                        newParameter("i", objectType("int"), holder = ctor)
                        ctor.body = newBlock(enterScope = true)
                    }
                    newMethod("create", holder = botan, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)
                        method.body = newBlock(enterScope = true)
                    }
                    newMethod("finish", holder = botan, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)
                        method.body = newBlock(enterScope = true)
                    }
                    newMethod("init", holder = botan, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)
                        method.body = newBlock(enterScope = true)
                    }
                    newMethod("process", holder = botan, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)
                        method.body = newBlock(enterScope = true)
                    }
                    newMethod("reset", holder = botan, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)
                        method.body = newBlock(enterScope = true)
                    }
                    newMethod("start", holder = botan, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)
                        method.body = newBlock(enterScope = true)
                    }
                }

                newRecord("ComplexOrder", "class", holder = tu, enterScope = true) { record ->
                    newMethod("ok_minimal1", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p1.initializer = construction
                                p1Decl.declarations += p1
                                scopeManager.addDeclaration(p1)
                                block += p1Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("ok_minimal2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p1.initializer = construction
                                p1Decl.declarations += p1
                                scopeManager.addDeclaration(p1)
                                block += p1Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("ok_minimal3", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p1.initializer = construction
                                p1Decl.declarations += p1
                                scopeManager.addDeclaration(p1)
                                block += p1Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p1")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("ok2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p2Decl = newDeclarationStatement()
                                val p2 = newVariable("p2", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p2.initializer = construction
                                p2Decl.declarations += p2
                                scopeManager.addDeclaration(p2)
                                block += p2Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p2")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p2")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p2")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p2")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p2")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p2")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p2")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p2")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("ok3", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p3Decl = newDeclarationStatement()
                                val p3 = newVariable("p3", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p3.initializer = construction
                                p3Decl.declarations += p3
                                scopeManager.addDeclaration(p3)
                                block += p3Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p3")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("ok4", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p3Decl = newDeclarationStatement()
                                val p3 = newVariable("p3", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p3.initializer = construction
                                p3Decl.declarations += p3
                                scopeManager.addDeclaration(p3)
                                block += p3Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p3")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p3")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("nok1", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p5Decl = newDeclarationStatement()
                                val p5 = newVariable("p5", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p5.initializer = construction
                                p5Decl.declarations += p5
                                scopeManager.addDeclaration(p5)
                                block += p5Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p5")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p5")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p5")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p5")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("nok2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p6Decl = newDeclarationStatement()
                                val p6 = newVariable("p6", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p6.initializer = construction
                                p6Decl.declarations += p6
                                scopeManager.addDeclaration(p6)
                                block += p6Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p6")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p6")),
                                        false,
                                    )

                                val ifElse = newIfElse { ifElse ->
                                    ifElse.condition = newLiteral(false, objectType("boolean"))
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p6")),
                                                    false,
                                                )
                                            thenBlock +=
                                                newMemberCall(
                                                    newMemberAccess("process", newReference("p6")),
                                                    false,
                                                )
                                            thenBlock +=
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p6")),
                                                    false,
                                                )
                                        }
                                }
                                block += ifElse

                                block +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p6")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("nok3", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p6Decl = newDeclarationStatement()
                                val p6 = newVariable("p6", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p6.initializer = construction
                                p6Decl.declarations += p6
                                scopeManager.addDeclaration(p6)
                                block += p6Decl

                                val whileNode =
                                    newWhile(enterScope = true) { w ->
                                        w.condition = newLiteral(true, objectType("boolean"))
                                        w.statement = newBlock { loopBodyBlock ->
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("create", newReference("p6")),
                                                    false,
                                                )
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("init", newReference("p6")),
                                                    false,
                                                )
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p6")),
                                                    false,
                                                )
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("process", newReference("p6")),
                                                    false,
                                                )
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p6")),
                                                    false,
                                                )
                                        }
                                    }
                                block += whileNode

                                block +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p6")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("nokWhile", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p7Decl = newDeclarationStatement()
                                val p7 = newVariable("p7", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p7.initializer = construction
                                p7Decl.declarations += p7
                                scopeManager.addDeclaration(p7)
                                block += p7Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p7")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p7")),
                                        false,
                                    )

                                val whileNode =
                                    newWhile(enterScope = true) { w ->
                                        val nextUIntCall =
                                            newMemberCall(
                                                newMemberAccess(
                                                    "nextUInt",
                                                    newReference("URandomKt"),
                                                ),
                                                true,
                                            )
                                        w.condition =
                                            newBinaryOperator(">").also {
                                                it.lhs = nextUIntCall
                                                it.rhs = newLiteral(5, objectType("int"))
                                            }
                                        w.statement = newBlock { loopBodyBlock ->
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p7")),
                                                    false,
                                                )
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("process", newReference("p7")),
                                                    false,
                                                )
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p7")),
                                                    false,
                                                )
                                        }
                                    }
                                block += whileNode

                                block +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p7")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("okWhile", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p8Decl = newDeclarationStatement()
                                val p8 = newVariable("p8", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p8.initializer = construction
                                p8Decl.declarations += p8
                                scopeManager.addDeclaration(p8)
                                block += p8Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p8")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p8")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p8")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p8")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p8")),
                                        false,
                                    )

                                val whileNode =
                                    newWhile(enterScope = true) { w ->
                                        w.condition = newLiteral(true, objectType("boolean"))
                                        w.statement = newBlock { loopBodyBlock ->
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p8")),
                                                    false,
                                                )
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("process", newReference("p8")),
                                                    false,
                                                )
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p8")),
                                                    false,
                                                )
                                        }
                                    }
                                block += whileNode

                                block +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p8")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("okWhile2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p8Decl = newDeclarationStatement()
                                val p8 = newVariable("p8", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p8.initializer = construction
                                p8Decl.declarations += p8
                                scopeManager.addDeclaration(p8)
                                block += p8Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p8")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p8")),
                                        false,
                                    )

                                val whileNode =
                                    newWhile(enterScope = true) { w ->
                                        w.condition = newLiteral(true, objectType("boolean"))
                                        w.statement = newBlock { loopBodyBlock ->
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p8")),
                                                    false,
                                                )
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("process", newReference("p8")),
                                                    false,
                                                )
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p8")),
                                                    false,
                                                )
                                        }
                                    }
                                block += whileNode

                                block +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p8")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("okDoWhile", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p6Decl = newDeclarationStatement()
                                val p6 = newVariable("p6", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p6.initializer = construction
                                p6Decl.declarations += p6
                                scopeManager.addDeclaration(p6)
                                block += p6Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p6")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p6")),
                                        false,
                                    )

                                val doWhileNode =
                                    newDoWhile(enterScope = true) { d ->
                                        d.statement = newBlock { loopBodyBlock ->
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p6")),
                                                    false,
                                                )
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("process", newReference("p6")),
                                                    false,
                                                )
                                            loopBodyBlock +=
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p6")),
                                                    false,
                                                )
                                        }

                                        val nextUIntCall =
                                            newMemberCall(
                                                newMemberAccess(
                                                    "nextUInt",
                                                    newReference("URandomKt"),
                                                ),
                                                false,
                                            )
                                        d.condition =
                                            newBinaryOperator(">").also {
                                                it.lhs = nextUIntCall
                                                it.rhs = newLiteral(5, objectType("int"))
                                            }
                                    }
                                block += doWhileNode

                                block +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p6")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("minimalInterprocUnclear", holder = record, enterScope = true) {
                        method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p1.initializer = construction
                                p1Decl.declarations += p1
                                scopeManager.addDeclaration(p1)
                                block += p1Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )

                                val fooCall =
                                    newMemberCall(
                                        newMemberAccess("foo", newReference("this")),
                                        false,
                                    )
                                fooCall.addArgument(newReference("p1"))
                                block += fooCall

                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("minimalInterprocFail", holder = record, enterScope = true) { method
                        ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(2, objectType("int")))
                                p1.initializer = construction
                                p1Decl.declarations += p1
                                scopeManager.addDeclaration(p1)
                                block += p1Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )

                                val ifElse = newIfElse { ifElse ->
                                    val nextUIntCall =
                                        newMemberCall(
                                            newMemberAccess("nextUInt", newReference("URandomKt")),
                                            false,
                                        )
                                    ifElse.condition =
                                        newBinaryOperator(">").also {
                                            it.lhs = nextUIntCall
                                            it.rhs = newLiteral(5, objectType("int"))
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            val fooCall =
                                                newMemberCall(
                                                    newMemberAccess("foo", newReference("this")),
                                                    false,
                                                )
                                            fooCall.addArgument(newReference("p1"))
                                            thenBlock += fooCall
                                        }
                                }
                                block += ifElse

                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("minimalInterprocFail2", holder = record, enterScope = true) { method
                        ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"))
                                val construction1 = newConstruction("Botan")
                                construction1.type = objectType("Botan")
                                construction1.addArgument(newLiteral(1, objectType("int")))
                                p1.initializer = construction1
                                p1Decl.declarations += p1
                                scopeManager.addDeclaration(p1)
                                block += p1Decl

                                val p2Decl = newDeclarationStatement()
                                val p2 = newVariable("p2", objectType("Botan"))
                                val construction2 = newConstruction("Botan")
                                construction2.type = objectType("Botan")
                                construction2.addArgument(newLiteral(2, objectType("int")))
                                p2.initializer = construction2
                                p2Decl.declarations += p2
                                scopeManager.addDeclaration(p2)
                                block += p2Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p2")),
                                        false,
                                    )

                                val fooCall =
                                    newMemberCall(
                                        newMemberAccess("foo", newReference("this")),
                                        false,
                                    )
                                fooCall.addArgument(newReference("p2"))
                                block += fooCall

                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("foo", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        newParameter("p1", objectType("Botan"), holder = method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p1")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod("bar", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(1, objectType("int")))
                                p1.initializer = construction
                                p1Decl.declarations += p1
                                scopeManager.addDeclaration(p1)
                                block += p1Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )

                                val minimalInterprocUnclearArgumentCall =
                                    newMemberCall(
                                        newMemberAccess(
                                            "minimalInterprocUnclearArgument",
                                            newReference("this"),
                                        ),
                                        false,
                                    )
                                minimalInterprocUnclearArgumentCall.addArgument(newReference("p1"))
                                block += minimalInterprocUnclearArgumentCall

                                block += newReturn()
                            }
                    }

                    newMethod(
                        "minimalInterprocUnclearArgument",
                        holder = record,
                        enterScope = true,
                    ) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        newParameter("p1", objectType("Botan"), holder = method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )

                                block += newReturn()
                            }
                    }

                    newMethod(
                        "minimalInterprocUnclearReturn",
                        holder = record,
                        enterScope = true,
                    ) { method ->
                        method.returnTypes = listOf(objectType("Botan"))
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"))
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.addArgument(newLiteral(1, objectType("int")))
                                p1.initializer = construction
                                p1Decl.declarations += p1
                                scopeManager.addDeclaration(p1)
                                block += p1Decl

                                block +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p1")),
                                        false,
                                    )
                                block +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )

                                val ret = newReturn()
                                ret.returnValue = newReference("p1")
                                block += ret
                            }
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
    }
}
