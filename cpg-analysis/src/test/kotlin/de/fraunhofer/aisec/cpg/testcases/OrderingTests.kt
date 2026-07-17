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
                                val p4 = newVariable("p4", objectType("Botan"), holder = p4Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p4.initializer = construction
                                block.statements += p4Decl

                                val startCall =
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    )
                                startCall.arguments += newReference("iv")
                                block.statements += startCall

                                val finishCall =
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    )
                                finishCall.arguments += newReference("buf")
                                block.statements += finishCall

                                block.statements += newReturn()
                            }
                    }

                    newMethod("ok2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p4Decl = newDeclarationStatement()
                                val p4 = newVariable("p4", objectType("Botan"), holder = p4Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p4.initializer = construction
                                block.statements += p4Decl

                                val startCall =
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    )
                                startCall.arguments += newReference("iv")
                                block.statements += startCall

                                // Not in the entity and therefore ignored
                                block.statements +=
                                    newMemberCall(newMemberAccess("foo", newReference("p4")), false)

                                val finishCall =
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    )
                                finishCall.arguments += newReference("buf")
                                block.statements += finishCall

                                block.statements += newReturn()
                            }
                    }

                    newMethod("ok3", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p4Decl = newDeclarationStatement()
                                val p4 = newVariable("p4", objectType("Botan"), holder = p4Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p4.initializer = construction
                                block.statements += p4Decl

                                val xDecl = newDeclarationStatement()
                                val x = newVariable("x", objectType("int"), holder = xDecl)
                                x.initializer =
                                    newMemberCall(
                                        newMemberAccess("nextUInt", newReference("URandomKt")),
                                        false,
                                    )
                                block.statements += xDecl

                                val ifElse = newIfElse { ifElse ->
                                    ifElse.condition =
                                        newBinaryOperator("<=") {
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
                                            startCall.arguments += newReference("iv")
                                            thenBlock.statements += startCall
                                        }
                                    ifElse.elseStatement =
                                        newBlock(enterScope = true) { elseBlock ->
                                            val startCall =
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p4")),
                                                    false,
                                                )
                                            startCall.arguments += newReference("iv")
                                            elseBlock.statements += startCall
                                        }
                                }
                                block.statements += ifElse

                                // Not in the entity and therefore ignored
                                block.statements +=
                                    newMemberCall(newMemberAccess("foo", newReference("p4")), false)

                                val finishCall =
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    )
                                finishCall.arguments += newReference("buf")
                                block.statements += finishCall

                                block.statements += newReturn()
                            }
                    }

                    newMethod("nok1", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p4Decl = newDeclarationStatement()
                                val p4 = newVariable("p4", objectType("Botan"), holder = p4Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(1, objectType("int"))
                                p4.initializer = construction
                                block.statements += p4Decl

                                // Not allowed as start
                                val setKeyCall1 =
                                    newMemberCall(
                                        newMemberAccess("set_key", newReference("p4")),
                                        false,
                                    )
                                setKeyCall1.arguments += newReference("key")
                                block.statements += setKeyCall1

                                val startCall =
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    )
                                startCall.arguments += newReference("iv")
                                block.statements += startCall

                                val finishCall =
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    )
                                finishCall.arguments += newReference("buf")
                                block.statements += finishCall

                                // Not in the entity and therefore ignored
                                block.statements +=
                                    newMemberCall(newMemberAccess("foo", newReference("p4")), false)

                                val setKeyCall2 =
                                    newMemberCall(
                                        newMemberAccess("set_key", newReference("p4")),
                                        false,
                                    )
                                setKeyCall2.arguments += newReference("key")
                                block.statements += setKeyCall2

                                block.statements += newReturn()
                            }
                    }

                    newMethod("nok2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p4Decl = newDeclarationStatement()
                                val p4 = newVariable("p4", objectType("Botan"), holder = p4Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p4.initializer = construction
                                block.statements += p4Decl

                                val startCall =
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    )
                                startCall.arguments += newReference("iv")
                                block.statements += startCall

                                // Missing: memberCall("finish", ref("p4")) {ref("buf")}

                                block.statements += newReturn()
                            }
                    }

                    newMethod("nok3", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p4Decl = newDeclarationStatement()
                                val p4 = newVariable("p4", objectType("Botan"), holder = p4Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p4.initializer = construction
                                block.statements += p4Decl

                                val ifElse = newIfElse { ifElse ->
                                    val nextUIntCall =
                                        newMemberCall(
                                            newMemberAccess("nextUInt", newReference("URandomKt")),
                                            false,
                                        )
                                    ifElse.condition =
                                        newBinaryOperator("<=") {
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
                                            startCall.arguments += newReference("iv")
                                            thenBlock.statements += startCall
                                        }
                                }
                                block.statements += ifElse

                                // start could be missing here
                                val finishCall =
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    )
                                finishCall.arguments += newReference("buf")
                                block.statements += finishCall

                                block.statements += newReturn()
                            }
                    }

                    newMethod("nok4", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p4Decl = newDeclarationStatement()
                                val p4 = newVariable("p4", objectType("Botan"), holder = p4Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p4.initializer = construction
                                block.statements += p4Decl

                                val ifElse = newIfElse { ifElse ->
                                    ifElse.condition = newLiteral(true, objectType("boolean"))
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            val startCall =
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p4")),
                                                    false,
                                                )
                                            startCall.arguments += newReference("iv")
                                            thenBlock.statements += startCall

                                            val finishCall =
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p4")),
                                                    false,
                                                )
                                            finishCall.arguments += newReference("buf")
                                            thenBlock.statements += finishCall
                                        }
                                }
                                block.statements += ifElse

                                // Not ok because p4 is already finished
                                val startCall2 =
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    )
                                startCall2.arguments += newReference("iv")
                                block.statements += startCall2

                                val finishCall2 =
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    )
                                finishCall2.arguments += newReference("buf")
                                block.statements += finishCall2

                                block.statements += newReturn()
                            }
                    }

                    newMethod("nok5", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                block.statements +=
                                    newBlock(enterScope = true) { nested1 ->
                                        val p4Decl = newDeclarationStatement()
                                        val p4 =
                                            newVariable("p4", objectType("Botan"), holder = p4Decl)
                                        val construction = newConstruction("Botan")
                                        construction.type = objectType("Botan")
                                        construction.arguments += newLiteral(2, objectType("int"))
                                        p4.initializer = construction
                                        nested1.statements += p4Decl

                                        val startCall =
                                            newMemberCall(
                                                newMemberAccess("start", newReference("p4")),
                                                false,
                                            )
                                        startCall.arguments += newReference("iv")
                                        nested1.statements += startCall
                                    }

                                block.statements +=
                                    newBlock(enterScope = true) { nested2 ->
                                        val p5Decl = newDeclarationStatement()
                                        val p5 =
                                            newVariable("p5", objectType("Botan"), holder = p5Decl)
                                        val construction = newConstruction("Botan")
                                        construction.type = objectType("Botan")
                                        construction.arguments += newLiteral(2, objectType("int"))
                                        p5.initializer = construction
                                        nested2.statements += p5Decl

                                        val finishCall =
                                            newMemberCall(
                                                newMemberAccess("finish", newReference("p5")),
                                                false,
                                            )
                                        finishCall.arguments += newReference("buf")
                                        nested2.statements += finishCall

                                        nested2.statements += newReturn()
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
                                val p1 = newVariable("p1", objectType("Botan"), holder = p1Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p1.initializer = construction
                                block.statements += p1Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("ok_minimal2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"), holder = p1Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p1.initializer = construction
                                block.statements += p1Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("ok_minimal3", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"), holder = p1Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p1.initializer = construction
                                block.statements += p1Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p1")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("ok2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p2Decl = newDeclarationStatement()
                                val p2 = newVariable("p2", objectType("Botan"), holder = p2Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p2.initializer = construction
                                block.statements += p2Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p2")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p2")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p2")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p2")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p2")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p2")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p2")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p2")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("ok3", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p3Decl = newDeclarationStatement()
                                val p3 = newVariable("p3", objectType("Botan"), holder = p3Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p3.initializer = construction
                                block.statements += p3Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p3")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("ok4", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p3Decl = newDeclarationStatement()
                                val p3 = newVariable("p3", objectType("Botan"), holder = p3Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p3.initializer = construction
                                block.statements += p3Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p3")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p3")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("nok1", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p5Decl = newDeclarationStatement()
                                val p5 = newVariable("p5", objectType("Botan"), holder = p5Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p5.initializer = construction
                                block.statements += p5Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p5")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p5")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p5")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p5")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("nok2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p6Decl = newDeclarationStatement()
                                val p6 = newVariable("p6", objectType("Botan"), holder = p6Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p6.initializer = construction
                                block.statements += p6Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p6")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p6")),
                                        false,
                                    )

                                val ifElse = newIfElse { ifElse ->
                                    ifElse.condition = newLiteral(false, objectType("boolean"))
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p6")),
                                                    false,
                                                )
                                            thenBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("process", newReference("p6")),
                                                    false,
                                                )
                                            thenBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p6")),
                                                    false,
                                                )
                                        }
                                }
                                block.statements += ifElse

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p6")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("nok3", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p6Decl = newDeclarationStatement()
                                val p6 = newVariable("p6", objectType("Botan"), holder = p6Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p6.initializer = construction
                                block.statements += p6Decl

                                val whileNode =
                                    newWhile(enterScope = true) { w ->
                                        w.condition = newLiteral(true, objectType("boolean"))
                                        w.statement = newBlock { loopBodyBlock ->
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("create", newReference("p6")),
                                                    false,
                                                )
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("init", newReference("p6")),
                                                    false,
                                                )
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p6")),
                                                    false,
                                                )
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("process", newReference("p6")),
                                                    false,
                                                )
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p6")),
                                                    false,
                                                )
                                        }
                                    }
                                block.statements += whileNode

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p6")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("nokWhile", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p7Decl = newDeclarationStatement()
                                val p7 = newVariable("p7", objectType("Botan"), holder = p7Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p7.initializer = construction
                                block.statements += p7Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p7")),
                                        false,
                                    )
                                block.statements +=
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
                                            newBinaryOperator(">") {
                                                it.lhs = nextUIntCall
                                                it.rhs = newLiteral(5, objectType("int"))
                                            }
                                        w.statement = newBlock { loopBodyBlock ->
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p7")),
                                                    false,
                                                )
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("process", newReference("p7")),
                                                    false,
                                                )
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p7")),
                                                    false,
                                                )
                                        }
                                    }
                                block.statements += whileNode

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p7")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("okWhile", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p8Decl = newDeclarationStatement()
                                val p8 = newVariable("p8", objectType("Botan"), holder = p8Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p8.initializer = construction
                                block.statements += p8Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p8")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p8")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p8")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("process", newReference("p8")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p8")),
                                        false,
                                    )

                                val whileNode =
                                    newWhile(enterScope = true) { w ->
                                        w.condition = newLiteral(true, objectType("boolean"))
                                        w.statement = newBlock { loopBodyBlock ->
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p8")),
                                                    false,
                                                )
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("process", newReference("p8")),
                                                    false,
                                                )
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p8")),
                                                    false,
                                                )
                                        }
                                    }
                                block.statements += whileNode

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p8")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("okWhile2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p8Decl = newDeclarationStatement()
                                val p8 = newVariable("p8", objectType("Botan"), holder = p8Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p8.initializer = construction
                                block.statements += p8Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p8")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p8")),
                                        false,
                                    )

                                val whileNode =
                                    newWhile(enterScope = true) { w ->
                                        w.condition = newLiteral(true, objectType("boolean"))
                                        w.statement = newBlock { loopBodyBlock ->
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p8")),
                                                    false,
                                                )
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("process", newReference("p8")),
                                                    false,
                                                )
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p8")),
                                                    false,
                                                )
                                        }
                                    }
                                block.statements += whileNode

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p8")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("okDoWhile", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p6Decl = newDeclarationStatement()
                                val p6 = newVariable("p6", objectType("Botan"), holder = p6Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p6.initializer = construction
                                block.statements += p6Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p6")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p6")),
                                        false,
                                    )

                                val doWhileNode =
                                    newDoWhile(enterScope = true) { d ->
                                        d.statement = newBlock { loopBodyBlock ->
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p6")),
                                                    false,
                                                )
                                            loopBodyBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("process", newReference("p6")),
                                                    false,
                                                )
                                            loopBodyBlock.statements +=
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
                                            newBinaryOperator(">") {
                                                it.lhs = nextUIntCall
                                                it.rhs = newLiteral(5, objectType("int"))
                                            }
                                    }
                                block.statements += doWhileNode

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("reset", newReference("p6")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("minimalInterprocUnclear", holder = record, enterScope = true) {
                        method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"), holder = p1Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p1.initializer = construction
                                block.statements += p1Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )

                                val fooCall =
                                    newMemberCall(
                                        newMemberAccess("foo", newReference("this")),
                                        false,
                                    )
                                fooCall.arguments += newReference("p1")
                                block.statements += fooCall

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("minimalInterprocFail", holder = record, enterScope = true) { method
                        ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"), holder = p1Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(2, objectType("int"))
                                p1.initializer = construction
                                block.statements += p1Decl

                                block.statements +=
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
                                        newBinaryOperator(">") {
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
                                            fooCall.arguments += newReference("p1")
                                            thenBlock.statements += fooCall
                                        }
                                }
                                block.statements += ifElse

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("minimalInterprocFail2", holder = record, enterScope = true) { method
                        ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"), holder = p1Decl)
                                val construction1 = newConstruction("Botan")
                                construction1.type = objectType("Botan")
                                construction1.arguments += newLiteral(1, objectType("int"))
                                p1.initializer = construction1
                                block.statements += p1Decl

                                val p2Decl = newDeclarationStatement()
                                val p2 = newVariable("p2", objectType("Botan"), holder = p2Decl)
                                val construction2 = newConstruction("Botan")
                                construction2.type = objectType("Botan")
                                construction2.arguments += newLiteral(2, objectType("int"))
                                p2.initializer = construction2
                                block.statements += p2Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p2")),
                                        false,
                                    )

                                val fooCall =
                                    newMemberCall(
                                        newMemberAccess("foo", newReference("this")),
                                        false,
                                    )
                                fooCall.arguments += newReference("p2")
                                block.statements += fooCall

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("foo", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        newParameter("p1", objectType("Botan"), holder = method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p1")),
                                        false,
                                    )

                                block.statements += newReturn()
                            }
                    }

                    newMethod("bar", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val p1Decl = newDeclarationStatement()
                                val p1 = newVariable("p1", objectType("Botan"), holder = p1Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(1, objectType("int"))
                                p1.initializer = construction
                                block.statements += p1Decl

                                block.statements +=
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
                                minimalInterprocUnclearArgumentCall.arguments += newReference("p1")
                                block.statements += minimalInterprocUnclearArgumentCall

                                block.statements += newReturn()
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
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p1")),
                                        false,
                                    )

                                block.statements += newReturn()
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
                                val p1 = newVariable("p1", objectType("Botan"), holder = p1Decl)
                                val construction = newConstruction("Botan")
                                construction.type = objectType("Botan")
                                construction.arguments += newLiteral(1, objectType("int"))
                                p1.initializer = construction
                                block.statements += p1Decl

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("init", newReference("p1")),
                                        false,
                                    )
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p1")),
                                        false,
                                    )

                                val ret = newReturn()
                                ret.returnValue = newReference("p1")
                                block.statements += ret
                            }
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
    }
}
