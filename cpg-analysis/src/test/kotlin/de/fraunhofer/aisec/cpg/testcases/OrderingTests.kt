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
                                block.statements += newDeclarationStatement { p4Decl ->
                                    newVariable("p4", objectType("Botan"), holder = p4Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    ) {
                                        it.arguments += newReference("iv")
                                    }

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    ) {
                                        it.arguments += newReference("buf")
                                    }

                                block.statements += newReturn()
                            }
                    }

                    newMethod("ok2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { p4Decl ->
                                    newVariable("p4", objectType("Botan"), holder = p4Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    ) {
                                        it.arguments += newReference("iv")
                                    }

                                // Not in the entity and therefore ignored
                                block.statements +=
                                    newMemberCall(newMemberAccess("foo", newReference("p4")), false)

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    ) {
                                        it.arguments += newReference("buf")
                                    }

                                block.statements += newReturn()
                            }
                    }

                    newMethod("ok3", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { p4Decl ->
                                    newVariable("p4", objectType("Botan"), holder = p4Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

                                block.statements += newDeclarationStatement { xDecl ->
                                    newVariable("x", objectType("int"), holder = xDecl) {
                                        it.initializer =
                                            newMemberCall(
                                                newMemberAccess(
                                                    "nextUInt",
                                                    newReference("URandomKt"),
                                                ),
                                                false,
                                            )
                                    }
                                }

                                block.statements += newIfElse { ifElse ->
                                    ifElse.condition =
                                        newBinaryOperator("<=") {
                                            it.lhs = newReference("x")
                                            it.rhs = newLiteral(5, objectType("int"))
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p4")),
                                                    false,
                                                ) {
                                                    it.arguments += newReference("iv")
                                                }
                                        }
                                    ifElse.elseStatement =
                                        newBlock(enterScope = true) { elseBlock ->
                                            elseBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p4")),
                                                    false,
                                                ) {
                                                    it.arguments += newReference("iv")
                                                }
                                        }
                                }

                                // Not in the entity and therefore ignored
                                block.statements +=
                                    newMemberCall(newMemberAccess("foo", newReference("p4")), false)

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    ) {
                                        it.arguments += newReference("buf")
                                    }

                                block.statements += newReturn()
                            }
                    }

                    newMethod("nok1", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { p4Decl ->
                                    newVariable("p4", objectType("Botan"), holder = p4Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(1, objectType("int"))
                                            }
                                    }
                                }

                                // Not allowed as start
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("set_key", newReference("p4")),
                                        false,
                                    ) {
                                        it.arguments += newReference("key")
                                    }

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    ) {
                                        it.arguments += newReference("iv")
                                    }

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    ) {
                                        it.arguments += newReference("buf")
                                    }

                                // Not in the entity and therefore ignored
                                block.statements +=
                                    newMemberCall(newMemberAccess("foo", newReference("p4")), false)

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("set_key", newReference("p4")),
                                        false,
                                    ) {
                                        it.arguments += newReference("key")
                                    }

                                block.statements += newReturn()
                            }
                    }

                    newMethod("nok2", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { p4Decl ->
                                    newVariable("p4", objectType("Botan"), holder = p4Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    ) {
                                        it.arguments += newReference("iv")
                                    }

                                // Missing: memberCall("finish", ref("p4")) {ref("buf")}

                                block.statements += newReturn()
                            }
                    }

                    newMethod("nok3", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { p4Decl ->
                                    newVariable("p4", objectType("Botan"), holder = p4Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

                                block.statements += newIfElse { ifElse ->
                                    ifElse.condition =
                                        newBinaryOperator("<=") {
                                            it.lhs =
                                                newMemberCall(
                                                    newMemberAccess(
                                                        "nextUInt",
                                                        newReference("URandomKt"),
                                                    ),
                                                    false,
                                                )
                                            it.rhs = newLiteral(5, objectType("int"))
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p4")),
                                                    false,
                                                ) {
                                                    it.arguments += newReference("iv")
                                                }
                                        }
                                }

                                // start could be missing here
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    ) {
                                        it.arguments += newReference("buf")
                                    }

                                block.statements += newReturn()
                            }
                    }

                    newMethod("nok4", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { p4Decl ->
                                    newVariable("p4", objectType("Botan"), holder = p4Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

                                block.statements += newIfElse { ifElse ->
                                    ifElse.condition = newLiteral(true, objectType("boolean"))
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("start", newReference("p4")),
                                                    false,
                                                ) {
                                                    it.arguments += newReference("iv")
                                                }

                                            thenBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("finish", newReference("p4")),
                                                    false,
                                                ) {
                                                    it.arguments += newReference("buf")
                                                }
                                        }
                                }

                                // Not ok because p4 is already finished
                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("start", newReference("p4")),
                                        false,
                                    ) {
                                        it.arguments += newReference("iv")
                                    }

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("finish", newReference("p4")),
                                        false,
                                    ) {
                                        it.arguments += newReference("buf")
                                    }

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
                                        nested1.statements += newDeclarationStatement { p4Decl ->
                                            newVariable(
                                                "p4",
                                                objectType("Botan"),
                                                holder = p4Decl,
                                            ) {
                                                it.initializer =
                                                    newConstruction("Botan") { construction ->
                                                        construction.type = objectType("Botan")
                                                        construction.arguments +=
                                                            newLiteral(2, objectType("int"))
                                                    }
                                            }
                                        }

                                        nested1.statements +=
                                            newMemberCall(
                                                newMemberAccess("start", newReference("p4")),
                                                false,
                                            ) {
                                                it.arguments += newReference("iv")
                                            }
                                    }

                                block.statements +=
                                    newBlock(enterScope = true) { nested2 ->
                                        nested2.statements += newDeclarationStatement { p5Decl ->
                                            newVariable(
                                                "p5",
                                                objectType("Botan"),
                                                holder = p5Decl,
                                            ) {
                                                it.initializer =
                                                    newConstruction("Botan") { construction ->
                                                        construction.type = objectType("Botan")
                                                        construction.arguments +=
                                                            newLiteral(2, objectType("int"))
                                                    }
                                            }
                                        }

                                        nested2.statements +=
                                            newMemberCall(
                                                newMemberAccess("finish", newReference("p5")),
                                                false,
                                            ) {
                                                it.arguments += newReference("buf")
                                            }

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
                                block.statements += newDeclarationStatement { p1Decl ->
                                    newVariable("p1", objectType("Botan"), holder = p1Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

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
                                block.statements += newDeclarationStatement { p1Decl ->
                                    newVariable("p1", objectType("Botan"), holder = p1Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

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
                                block.statements += newDeclarationStatement { p1Decl ->
                                    newVariable("p1", objectType("Botan"), holder = p1Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

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
                                block.statements += newDeclarationStatement { p2Decl ->
                                    newVariable("p2", objectType("Botan"), holder = p2Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

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
                                block.statements += newDeclarationStatement { p3Decl ->
                                    newVariable("p3", objectType("Botan"), holder = p3Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

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
                                block.statements += newDeclarationStatement { p3Decl ->
                                    newVariable("p3", objectType("Botan"), holder = p3Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

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
                                block.statements += newDeclarationStatement { p5Decl ->
                                    newVariable("p5", objectType("Botan"), holder = p5Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

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
                                block.statements += newDeclarationStatement { p6Decl ->
                                    newVariable("p6", objectType("Botan"), holder = p6Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

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

                                block.statements += newIfElse { ifElse ->
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
                                block.statements += newDeclarationStatement { p6Decl ->
                                    newVariable("p6", objectType("Botan"), holder = p6Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

                                block.statements +=
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
                                block.statements += newDeclarationStatement { p7Decl ->
                                    newVariable("p7", objectType("Botan"), holder = p7Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

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

                                block.statements +=
                                    newWhile(enterScope = true) { w ->
                                        w.condition =
                                            newBinaryOperator(">") {
                                                it.lhs =
                                                    newMemberCall(
                                                        newMemberAccess(
                                                            "nextUInt",
                                                            newReference("URandomKt"),
                                                        ),
                                                        true,
                                                    )
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
                                block.statements += newDeclarationStatement { p8Decl ->
                                    newVariable("p8", objectType("Botan"), holder = p8Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

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

                                block.statements +=
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
                                block.statements += newDeclarationStatement { p8Decl ->
                                    newVariable("p8", objectType("Botan"), holder = p8Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

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
                                block.statements += newDeclarationStatement { p6Decl ->
                                    newVariable("p6", objectType("Botan"), holder = p6Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

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

                                block.statements +=
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

                                        d.condition =
                                            newBinaryOperator(">") {
                                                it.lhs =
                                                    newMemberCall(
                                                        newMemberAccess(
                                                            "nextUInt",
                                                            newReference("URandomKt"),
                                                        ),
                                                        false,
                                                    )
                                                it.rhs = newLiteral(5, objectType("int"))
                                            }
                                    }

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
                                block.statements += newDeclarationStatement { p1Decl ->
                                    newVariable("p1", objectType("Botan"), holder = p1Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("foo", newReference("this")),
                                        false,
                                    ) {
                                        it.arguments += newReference("p1")
                                    }

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
                                block.statements += newDeclarationStatement { p1Decl ->
                                    newVariable("p1", objectType("Botan"), holder = p1Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )

                                block.statements += newIfElse { ifElse ->
                                    ifElse.condition =
                                        newBinaryOperator(">") {
                                            it.lhs =
                                                newMemberCall(
                                                    newMemberAccess(
                                                        "nextUInt",
                                                        newReference("URandomKt"),
                                                    ),
                                                    false,
                                                )
                                            it.rhs = newLiteral(5, objectType("int"))
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock.statements +=
                                                newMemberCall(
                                                    newMemberAccess("foo", newReference("this")),
                                                    false,
                                                ) {
                                                    it.arguments += newReference("p1")
                                                }
                                        }
                                }

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
                                block.statements += newDeclarationStatement { p1Decl ->
                                    newVariable("p1", objectType("Botan"), holder = p1Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(1, objectType("int"))
                                            }
                                    }
                                }

                                block.statements += newDeclarationStatement { p2Decl ->
                                    newVariable("p2", objectType("Botan"), holder = p2Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(2, objectType("int"))
                                            }
                                    }
                                }

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

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("foo", newReference("this")),
                                        false,
                                    ) {
                                        it.arguments += newReference("p2")
                                    }

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
                                block.statements += newDeclarationStatement { p1Decl ->
                                    newVariable("p1", objectType("Botan"), holder = p1Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(1, objectType("int"))
                                            }
                                    }
                                }

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess("create", newReference("p1")),
                                        false,
                                    )

                                block.statements +=
                                    newMemberCall(
                                        newMemberAccess(
                                            "minimalInterprocUnclearArgument",
                                            newReference("this"),
                                        ),
                                        false,
                                    ) {
                                        it.arguments += newReference("p1")
                                    }

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
                                block.statements += newDeclarationStatement { p1Decl ->
                                    newVariable("p1", objectType("Botan"), holder = p1Decl) {
                                        it.initializer =
                                            newConstruction("Botan") { construction ->
                                                construction.type = objectType("Botan")
                                                construction.arguments +=
                                                    newLiteral(1, objectType("int"))
                                            }
                                    }
                                }

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

                                block.statements += newReturn {
                                    it.returnValue = newReference("p1")
                                }
                            }
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
    }
}
