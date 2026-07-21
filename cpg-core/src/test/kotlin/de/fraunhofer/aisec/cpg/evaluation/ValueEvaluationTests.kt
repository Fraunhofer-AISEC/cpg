/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.evaluation

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType

class ValueEvaluationTests {
    companion object {
        fun getSizeExample(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("size.java")
                scopeManager.resetToGlobal(tu)

                newRecord("MainClass", "class", holder = tu, enterScope = true) { record ->
                    newMethod(
                        "main",
                        isStatic = true,
                        recordDeclaration = record,
                        holder = record,
                        enterScope = true,
                    ) { main ->
                        main.returnTypes = listOf(unknownType())
                        main.type = computeType(main)
                        newParameter("args", objectType("String").array(), holder = main)

                        main.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { arrayDeclStmt ->
                                    newVariable(
                                        "array",
                                        objectType("int").array(),
                                        holder = arrayDeclStmt,
                                    ) {
                                        it.initializer =
                                            newArrayConstruction().also { ac ->
                                                ac.addDimension(newLiteral(3, objectType("int")))
                                            }
                                    }
                                }

                                block.statements += newFor { for_ ->
                                    for_.initializerStatement =
                                        newDeclarationStatement { iDeclStmt ->
                                            newVariable(
                                                "i",
                                                objectType("int"),
                                                holder = iDeclStmt,
                                            ) {
                                                it.initializer = newLiteral(0, objectType("int"))
                                            }
                                        }

                                    for_.condition =
                                        newBinaryOperator("<") {
                                            it.lhs = newReference("i")
                                            it.rhs =
                                                newMemberAccess("length", newReference("array"))
                                        }

                                    for_.iterationStatement =
                                        newUnaryOperator("++", postfix = true, prefix = false) {
                                            it.input = newReference("i")
                                        }

                                    for_.statement = newBlock { loopBody ->
                                        loopBody.statements +=
                                            newAssign(
                                                "=",
                                                listOf(
                                                    newSubscription {
                                                        it.arrayExpression = newReference("array")
                                                        it.subscriptExpression = newReference("i")
                                                    }
                                                ),
                                                listOf(newReference("i")),
                                            )
                                    }
                                }

                                val printlnCall1 =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall1.arguments += newSubscription {
                                    it.arrayExpression = newReference("array")
                                    it.subscriptExpression = newLiteral(1, objectType("int"))
                                }
                                block.statements += printlnCall1

                                block.statements += newDeclarationStatement { strDeclStmt ->
                                    newVariable("str", objectType("String"), holder = strDeclStmt) {
                                        it.initializer = newLiteral("abcde", objectType("String"))
                                    }
                                }

                                val printlnCall2 =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall2.arguments += newReference("str")
                                block.statements += printlnCall2

                                block.statements += newReturn()
                            }
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getComplexExample(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("complex.java")
                scopeManager.resetToGlobal(tu)

                newRecord("MainClass", "class", holder = tu, enterScope = true) { record ->
                    newMethod(
                        "main",
                        isStatic = true,
                        recordDeclaration = record,
                        holder = record,
                        enterScope = true,
                    ) { main ->
                        main.returnTypes = listOf(unknownType())
                        main.type = computeType(main)
                        newParameter("args", objectType("String").array(), holder = main)

                        main.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { iDeclStmt ->
                                    newVariable("i", objectType("int"), holder = iDeclStmt) {
                                        it.initializer = newLiteral(3, objectType("int"))
                                    }
                                }

                                block.statements += newDeclarationStatement { sDeclStmt ->
                                    newVariable("s", objectType("String"), holder = sDeclStmt)
                                }

                                block.statements += newIfElse { ifElse ->
                                    ifElse.condition =
                                        newBinaryOperator("<") {
                                            it.lhs = newReference("i")
                                            it.rhs = newLiteral(2, objectType("int"))
                                        }

                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock.statements +=
                                                newAssign(
                                                    "=",
                                                    listOf(newReference("s")),
                                                    listOf(
                                                        newLiteral("small", objectType("String"))
                                                    ),
                                                )
                                        }

                                    ifElse.elseStatement =
                                        newBlock(enterScope = true) { elseBlock ->
                                            elseBlock.statements +=
                                                newAssign(
                                                    "=",
                                                    listOf(newReference("s")),
                                                    listOf(newLiteral("big", objectType("String"))),
                                                )
                                        }
                                }

                                block.statements +=
                                    newAssign(
                                        "+=",
                                        listOf(newReference("s")),
                                        listOf(newLiteral("!", objectType("String"))),
                                    )

                                block.statements +=
                                    newAssign(
                                        "=",
                                        listOf(newReference("s")),
                                        listOf(
                                            newBinaryOperator("+") {
                                                it.lhs = newReference("s")
                                                it.rhs = newLiteral("?", objectType("string"))
                                            }
                                        ),
                                    )

                                block.statements +=
                                    newUnaryOperator("++", postfix = true, prefix = false) {
                                        it.input = newReference("i")
                                    }

                                val printlnCall1 =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall1.arguments += newReference("s")
                                block.statements += printlnCall1

                                val printlnCall2 =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall2.arguments += newReference("i")
                                block.statements += printlnCall2

                                block.statements += newReturn()
                            }
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getExample(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("example.cpp")
                scopeManager.resetToGlobal(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            block.statements += newDeclarationStatement { bDeclStmt ->
                                newVariable("b", objectType("int"), holder = bDeclStmt) {
                                    it.initializer =
                                        newBinaryOperator("+") { bo ->
                                            bo.lhs = newLiteral(1, objectType("int"))
                                            bo.rhs = newLiteral(1, objectType("int"))
                                        }
                                }
                            }

                            block.statements +=
                                newCall(newReference("println")) {
                                    it.arguments += newReference("b")
                                }

                            block.statements += newDeclarationStatement { aDeclStmt ->
                                newVariable("a", objectType("int"), holder = aDeclStmt) {
                                    it.initializer = newLiteral(1, objectType("int"))
                                }
                            }

                            block.statements +=
                                newAssign(
                                    "=",
                                    listOf(newReference("a")),
                                    listOf(newLiteral(2, objectType("int"))),
                                )

                            block.statements +=
                                newCall(newReference("println")) {
                                    it.arguments += newReference("a")
                                }

                            block.statements += newDeclarationStatement { cDeclStmt ->
                                newVariable("c", objectType("int"), holder = cDeclStmt) {
                                    it.initializer =
                                        newBinaryOperator("-") { bo ->
                                            bo.lhs = newLiteral(5, objectType("int"))
                                            bo.rhs = newLiteral(2, objectType("int"))
                                        }
                                }
                            }

                            block.statements += newDeclarationStatement { dDeclStmt ->
                                newVariable("d", objectType("float"), holder = dDeclStmt) {
                                    it.initializer =
                                        newBinaryOperator("/") { bo ->
                                            bo.lhs = newLiteral(8, objectType("int"))
                                            bo.rhs = newLiteral(3, objectType("int"))
                                        }
                                }
                            }

                            block.statements += newDeclarationStatement { eDeclStmt ->
                                newVariable("e", objectType("float"), holder = eDeclStmt) {
                                    it.initializer =
                                        newBinaryOperator("/") { bo ->
                                            bo.lhs = newLiteral(7.0, objectType("float"))
                                            bo.rhs = newLiteral(2, objectType("int"))
                                        }
                                }
                            }

                            block.statements += newDeclarationStatement { fDeclStmt ->
                                newVariable("f", objectType("int"), holder = fDeclStmt) {
                                    it.initializer =
                                        newBinaryOperator("*") { bo ->
                                            bo.lhs = newLiteral(2, objectType("int"))
                                            bo.rhs = newLiteral(5, objectType("int"))
                                        }
                                }
                            }

                            block.statements += newDeclarationStatement { gDeclStmt ->
                                newVariable("g", objectType("int"), holder = gDeclStmt) {
                                    it.initializer =
                                        newUnaryOperator("-", postfix = false, prefix = false) {
                                            it.input = newReference("c")
                                        }
                                }
                            }

                            block.statements +=
                                newCall(newReference("println")) {
                                    it.arguments +=
                                        newBinaryOperator("+") { bo ->
                                            bo.lhs = newLiteral("Hello ", objectType("String"))
                                            bo.rhs = newLiteral("world", objectType("String"))
                                        }
                                }

                            block.statements += newDeclarationStatement { hDeclStmt ->
                                newVariable("h", objectType("bool"), holder = hDeclStmt) {
                                    it.initializer =
                                        newBinaryOperator("<=") { bo ->
                                            bo.lhs = newLiteral(5, objectType("int"))
                                            bo.rhs = newLiteral(2, objectType("int"))
                                        }
                                }
                            }

                            block.statements += newDeclarationStatement { iVarDeclStmt ->
                                newVariable("i", objectType("bool"), holder = iVarDeclStmt) {
                                    it.initializer =
                                        newBinaryOperator(">") { bo ->
                                            bo.lhs = newLiteral(3, objectType("int"))
                                            bo.rhs = newLiteral(3, objectType("int"))
                                        }
                                }
                            }

                            block.statements += newDeclarationStatement { jDeclStmt ->
                                newVariable("j", objectType("bool"), holder = jDeclStmt) {
                                    it.initializer =
                                        newBinaryOperator(">=") { bo ->
                                            bo.lhs = newLiteral(3, objectType("int"))
                                            bo.rhs = newLiteral(3.2, objectType("float"))
                                        }
                                }
                            }

                            block.statements += newDeclarationStatement { kDeclStmt ->
                                newVariable("k", objectType("bool"), holder = kDeclStmt) {
                                    it.initializer =
                                        newBinaryOperator("<=") { bo ->
                                            bo.lhs = newLiteral(3.1, objectType("float"))
                                            bo.rhs = newLiteral(3, objectType("int"))
                                        }
                                }
                            }

                            block.statements += newDeclarationStatement { lDeclStmt ->
                                newVariable("l", objectType("bool"), holder = lDeclStmt) {
                                    it.initializer =
                                        newBinaryOperator(">=") { bo ->
                                            bo.lhs = newLiteral(3L, objectType("long"))
                                            bo.rhs =
                                                newCast().also { cast ->
                                                    cast.castType = objectType("float")
                                                    cast.expression =
                                                        newLiteral(3.1, objectType("float"))
                                                }
                                        }
                                }
                            }

                            block.statements += newDeclarationStatement { mDeclStmt ->
                                newVariable("m", objectType("bool"), holder = mDeclStmt) {
                                    it.initializer =
                                        newBinaryOperator(">=") { bo ->
                                            bo.lhs =
                                                newCast().also { cast ->
                                                    cast.castType = objectType("char")
                                                    cast.expression =
                                                        newLiteral(3, objectType("int"))
                                                }
                                            bo.rhs = newLiteral(3.1, objectType("float"))
                                        }
                                }
                            }

                            block.statements += newDeclarationStatement { nDeclStmt ->
                                newVariable("n", objectType("bool"), holder = nDeclStmt) {
                                    it.initializer =
                                        newBinaryOperator("==") { bo ->
                                            bo.lhs = newLiteral(3, objectType("int"))
                                            bo.rhs = newLiteral(3.1, objectType("float"))
                                        }
                                }
                            }

                            block.statements += newReturn {
                                it.returnValue = newLiteral(0, objectType("int"))
                            }
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getCfExample(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("cfexample.cpp")
                scopeManager.resetToGlobal(tu)

                tu.addDeclaration(newInclude("time.h"))
                tu.addDeclaration(newInclude("stdlib.h"))

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            block.statements +=
                                newCall(newReference("srand")) {
                                    it.arguments +=
                                        newCall(newReference("time")) { timeCall ->
                                            timeCall.arguments += newReference("NULL")
                                        }
                                }

                            block.statements += newDeclarationStatement { bDeclStmt ->
                                newVariable("b", objectType("int"), holder = bDeclStmt) {
                                    it.initializer = newLiteral(1, objectType("int"))
                                }
                            }

                            block.statements += newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("<") {
                                        it.lhs = newCall(newReference("rand"))
                                        it.rhs = newLiteral(10, objectType("int"))
                                    }

                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("b")),
                                                listOf(
                                                    newBinaryOperator("+") {
                                                        it.lhs = newReference("b")
                                                        it.rhs = newLiteral(1, objectType("int"))
                                                    }
                                                ),
                                            )
                                    }
                            }

                            block.statements +=
                                newCall(newReference("println")) {
                                    it.arguments += newReference("b")
                                } // 1, 2

                            block.statements += newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator(">") {
                                        it.lhs = newCall(newReference("rand"))
                                        it.rhs = newLiteral(5, objectType("int"))
                                    }

                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("b")),
                                                listOf(
                                                    newBinaryOperator("-") {
                                                        it.lhs = newReference("b")
                                                        it.rhs = newLiteral(1, objectType("int"))
                                                    }
                                                ),
                                            )
                                    }
                            }

                            block.statements +=
                                newCall(newReference("println")) {
                                    it.arguments += newReference("b")
                                } // 0, 1, 2

                            block.statements += newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator(">") {
                                        it.lhs = newCall(newReference("rand"))
                                        it.rhs = newLiteral(3, objectType("int"))
                                    }

                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("b")),
                                                listOf(
                                                    newBinaryOperator("*") {
                                                        it.lhs = newReference("b")
                                                        it.rhs = newLiteral(2, objectType("int"))
                                                    }
                                                ),
                                            )
                                    }
                            }

                            block.statements +=
                                newCall(newReference("println")) {
                                    it.arguments += newReference("b")
                                } // 0, 1, 2, 4

                            block.statements += newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator(">") {
                                        it.lhs = newCall(newReference("rand"))
                                        it.rhs = newLiteral(1, objectType("int"))
                                    }

                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("b")),
                                                listOf(
                                                    newUnaryOperator(
                                                        "-",
                                                        prefix = true,
                                                        postfix = false,
                                                    ) {
                                                        it.input = newReference("b")
                                                    }
                                                ),
                                            )
                                    }
                            }

                            block.statements +=
                                newCall(newReference("println")) {
                                    it.arguments += newReference("b")
                                } // -4, -2, -1, 0, 1, 2, 4

                            block.statements += newDeclarationStatement { aDeclStmt ->
                                newVariable("a", objectType("int"), holder = aDeclStmt) {
                                    it.initializer =
                                        newConditional(
                                            newBinaryOperator("<") {
                                                it.lhs = newReference("b")
                                                it.rhs = newLiteral(2, objectType("int"))
                                            },
                                            newLiteral(3, objectType("int")),
                                            newUnaryOperator("++", postfix = true, prefix = false) {
                                                it.input = newLiteral(5, objectType("int"))
                                            },
                                        )
                                }
                            }

                            block.statements +=
                                newCall(newReference("println")) {
                                    it.arguments += newReference("a")
                                } // 3, 6

                            block.statements += newReturn {
                                it.returnValue = newLiteral(0, objectType("int"))
                            }
                        }
                }

                newFunction("loop", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            block.statements += newDeclarationStatement { arrayDeclStmt ->
                                newVariable(
                                    "array",
                                    objectType("int").array(),
                                    holder = arrayDeclStmt,
                                ) {
                                    it.initializer =
                                        newArrayConstruction().also { ac ->
                                            ac.addDimension(newLiteral(6, objectType("int")))
                                        }
                                }
                            }

                            block.statements += newFor { for_ ->
                                for_.initializerStatement = newDeclarationStatement { iDeclStmt ->
                                    newVariable("i", objectType("int"), holder = iDeclStmt) {
                                        it.initializer = newLiteral(0, objectType("int"))
                                    }
                                }

                                for_.condition =
                                    newBinaryOperator("<") {
                                        it.lhs = newReference("i")
                                        it.rhs = newLiteral(6, objectType("int"))
                                    }

                                for_.iterationStatement =
                                    newUnaryOperator("++", postfix = true, prefix = false) {
                                        it.input = newReference("i")
                                    }

                                for_.statement = newBlock { loopBody ->
                                    loopBody.statements +=
                                        newAssign(
                                            "=",
                                            listOf(
                                                newSubscription {
                                                    it.arrayExpression = newReference("array")
                                                    it.subscriptExpression = newReference("i")
                                                }
                                            ),
                                            listOf(newReference("i")),
                                        )
                                }
                            }

                            block.statements += newReturn {
                                it.returnValue = newLiteral(0, objectType("int"))
                            }
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
    }
}
