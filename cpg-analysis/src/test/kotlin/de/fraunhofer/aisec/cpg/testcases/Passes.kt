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

class Passes {
    companion object {
        fun getUnreachability(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerPass<UnreachableEOGPass>()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Unreachability.java")
                scopeManager.resetToGlobal(tu)

                newInclude("kotlin.random.URandomKt", holder = tu)

                newRecord("TestClass", "class", holder = tu, enterScope = true) { record ->
                    newMethod("ifBothPossible", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val yDecl = newDeclarationStatement()
                                val y = newVariable("y", objectType("int"))
                                y.initializer = newLiteral(5, objectType("int"))
                                yDecl.declarations += y
                                scopeManager.addDeclaration(y)
                                block += yDecl

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
                                            it.rhs = newReference("y")
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock +=
                                                newUnaryOperator(
                                                        "++",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                    ifElse.elseStatement =
                                        newBlock(enterScope = true) { elseBlock ->
                                            elseBlock +=
                                                newUnaryOperator(
                                                        "--",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                }
                                block += ifElse

                                val printlnCall =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall.arguments += newReference("y")
                                block += printlnCall

                                block += newReturn()
                            }
                    }

                    newMethod("ifTrue", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val yDecl = newDeclarationStatement()
                                val y = newVariable("y", objectType("int"))
                                y.initializer = newLiteral(6, objectType("int"))
                                yDecl.declarations += y
                                scopeManager.addDeclaration(y)
                                block += yDecl

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
                                    ifElse.condition = newLiteral(true, objectType("boolean"))
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock +=
                                                newUnaryOperator(
                                                        "++",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                    ifElse.elseStatement =
                                        newBlock(enterScope = true) { elseBlock ->
                                            elseBlock +=
                                                newUnaryOperator(
                                                        "--",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                }
                                block += ifElse

                                val printlnCall =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall.arguments += newReference("y")
                                block += printlnCall

                                block += newReturn()
                            }
                    }

                    newMethod("ifFalse", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val yDecl = newDeclarationStatement()
                                val y = newVariable("y", objectType("int"))
                                y.initializer = newLiteral(6, objectType("int"))
                                yDecl.declarations += y
                                scopeManager.addDeclaration(y)
                                block += yDecl

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
                                    ifElse.condition = newLiteral(false, objectType("boolean"))
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock +=
                                                newUnaryOperator(
                                                        "++",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                    ifElse.elseStatement =
                                        newBlock(enterScope = true) { elseBlock ->
                                            elseBlock +=
                                                newUnaryOperator(
                                                        "--",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                }
                                block += ifElse

                                val printlnCall =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall.arguments += newReference("y")
                                block += printlnCall

                                block += newReturn()
                            }
                    }

                    newMethod("ifTrueComputed", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val yDecl = newDeclarationStatement()
                                val y = newVariable("y", objectType("int"))
                                y.initializer = newLiteral(6, objectType("int"))
                                yDecl.declarations += y
                                scopeManager.addDeclaration(y)
                                block += yDecl

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
                                            it.lhs = newReference("y")
                                            it.rhs = newLiteral(9, objectType("int"))
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock +=
                                                newUnaryOperator(
                                                        "++",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                    ifElse.elseStatement =
                                        newBlock(enterScope = true) { elseBlock ->
                                            elseBlock +=
                                                newUnaryOperator(
                                                        "--",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                }
                                block += ifElse

                                val printlnCall =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall.arguments += newReference("y")
                                block += printlnCall

                                block += newReturn()
                            }
                    }

                    newMethod("ifTrueComputedHard", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val zDecl = newDeclarationStatement()
                                val z = newVariable("z", objectType("int"))
                                z.initializer = newLiteral(2, objectType("int"))
                                zDecl.declarations += z
                                scopeManager.addDeclaration(z)
                                block += zDecl

                                val yDecl = newDeclarationStatement()
                                val y = newVariable("y", objectType("int"))
                                y.initializer = newReference("z")
                                yDecl.declarations += y
                                scopeManager.addDeclaration(y)
                                block += yDecl

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
                                            it.lhs =
                                                newBinaryOperator("+").also { plus ->
                                                    plus.lhs = newReference("y")
                                                    plus.rhs = newReference("z")
                                                }
                                            it.rhs = newLiteral(9, objectType("int"))
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock +=
                                                newUnaryOperator(
                                                        "++",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                    ifElse.elseStatement =
                                        newBlock(enterScope = true) { elseBlock ->
                                            elseBlock +=
                                                newUnaryOperator(
                                                        "--",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                }
                                block += ifElse

                                block +=
                                    newAssign(
                                        "=",
                                        listOf(newReference("z")),
                                        listOf(newLiteral(10, objectType("int"))),
                                    )

                                val printlnCall =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall.arguments += newReference("y")
                                block += printlnCall

                                block += newReturn()
                            }
                    }

                    newMethod("ifFalseComputedHard", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val zDecl = newDeclarationStatement()
                                val z = newVariable("z", objectType("int"))
                                z.initializer = newLiteral(5, objectType("int"))
                                zDecl.declarations += z
                                scopeManager.addDeclaration(z)
                                block += zDecl

                                val yDecl = newDeclarationStatement()
                                val y = newVariable("y", objectType("int"))
                                y.initializer = newReference("z")
                                yDecl.declarations += y
                                scopeManager.addDeclaration(y)
                                block += yDecl

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
                                            it.lhs =
                                                newBinaryOperator("+").also { plus ->
                                                    plus.lhs = newReference("y")
                                                    plus.rhs = newReference("z")
                                                }
                                            it.rhs = newLiteral(9, objectType("int"))
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock +=
                                                newUnaryOperator(
                                                        "++",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                    ifElse.elseStatement =
                                        newBlock(enterScope = true) { elseBlock ->
                                            elseBlock +=
                                                newUnaryOperator(
                                                        "--",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                }
                                block += ifElse

                                block +=
                                    newAssign(
                                        "=",
                                        listOf(newReference("z")),
                                        listOf(newLiteral(3, objectType("int"))),
                                    )

                                val printlnCall =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall.arguments += newReference("y")
                                block += printlnCall

                                block += newReturn()
                            }
                    }

                    newMethod("ifFalseComputed", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val yDecl = newDeclarationStatement()
                                val y = newVariable("y", objectType("int"))
                                y.initializer = newLiteral(6, objectType("int"))
                                yDecl.declarations += y
                                scopeManager.addDeclaration(y)
                                block += yDecl

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
                                            it.lhs = newReference("y")
                                            it.rhs = newLiteral(-1, objectType("int"))
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock +=
                                                newUnaryOperator(
                                                        "++",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                    ifElse.elseStatement =
                                        newBlock(enterScope = true) { elseBlock ->
                                            elseBlock +=
                                                newUnaryOperator(
                                                        "--",
                                                        postfix = true,
                                                        prefix = false,
                                                    )
                                                    .also { it.input = newReference("y") }
                                        }
                                }
                                block += ifElse

                                val printlnCall =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall.arguments += newReference("y")
                                block += printlnCall

                                block += newReturn()
                            }
                    }

                    newMethod("whileTrueEndless", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val xDecl = newDeclarationStatement()
                                val x = newVariable("x", objectType("boolean"))
                                x.initializer = newLiteral(true, objectType("boolean"))
                                xDecl.declarations += x
                                scopeManager.addDeclaration(x)
                                block += xDecl

                                val whileNode =
                                    newWhile(enterScope = true) { w ->
                                        w.condition = newReference("x")
                                        w.statement = newBlock { loopBodyBlock ->
                                            val printlnCall =
                                                newMemberCall(
                                                    newMemberAccess(
                                                        "println",
                                                        newMemberAccess(
                                                            "out",
                                                            newReference("System"),
                                                        ),
                                                    ),
                                                    false,
                                                )
                                            printlnCall.arguments +=
                                                newLiteral("Cool loop", objectType("string"))
                                            loopBodyBlock += printlnCall
                                        }
                                    }
                                block += whileNode

                                val printlnCall2 =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall2.arguments +=
                                    newLiteral("After cool loop", objectType("string"))
                                block += printlnCall2

                                block += newReturn()
                            }
                    }

                    newMethod("whileTrue", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val xDecl = newDeclarationStatement()
                                val x = newVariable("x", objectType("boolean"))
                                x.initializer = newLiteral(true, objectType("boolean"))
                                xDecl.declarations += x
                                scopeManager.addDeclaration(x)
                                block += xDecl

                                val whileNode =
                                    newWhile(enterScope = true) { w ->
                                        w.condition = newReference("x")
                                        w.statement = newBlock { loopBodyBlock ->
                                            val printlnCall =
                                                newMemberCall(
                                                    newMemberAccess(
                                                        "println",
                                                        newMemberAccess(
                                                            "out",
                                                            newReference("System"),
                                                        ),
                                                    ),
                                                    false,
                                                )
                                            printlnCall.arguments +=
                                                newLiteral("Cool loop", objectType("string"))
                                            loopBodyBlock += printlnCall

                                            loopBodyBlock +=
                                                newAssign(
                                                    "=",
                                                    listOf(newReference("x")),
                                                    listOf(newLiteral(false, objectType("boolean"))),
                                                )
                                        }
                                    }
                                block += whileNode

                                val printlnCall2 =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall2.arguments +=
                                    newLiteral("After cool loop", objectType("string"))
                                block += printlnCall2

                                block += newReturn()
                            }
                    }

                    newMethod("whileFalse", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val whileNode =
                                    newWhile(enterScope = true) { w ->
                                        w.condition = newLiteral(false, objectType("boolean"))
                                        w.statement = newBlock { loopBodyBlock ->
                                            val printlnCall =
                                                newMemberCall(
                                                    newMemberAccess(
                                                        "println",
                                                        newMemberAccess(
                                                            "out",
                                                            newReference("System"),
                                                        ),
                                                    ),
                                                    false,
                                                )
                                            printlnCall.arguments +=
                                                newLiteral("Cool loop", objectType("string"))
                                            loopBodyBlock += printlnCall
                                        }
                                    }
                                block += whileNode

                                val printlnCall2 =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall2.arguments +=
                                    newLiteral("After cool loop", objectType("string"))
                                block += printlnCall2

                                block += newReturn()
                            }
                    }

                    newMethod("whileComputedTrue", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val xDecl = newDeclarationStatement()
                                val x = newVariable("x", objectType("boolean"))
                                x.initializer = newLiteral(1, objectType("int"))
                                xDecl.declarations += x
                                scopeManager.addDeclaration(x)
                                block += xDecl

                                val whileNode =
                                    newWhile(enterScope = true) { w ->
                                        w.condition =
                                            newBinaryOperator("<=").also {
                                                it.lhs = newReference("x")
                                                it.rhs = newLiteral(2, objectType("int"))
                                            }
                                        w.statement = newBlock { loopBodyBlock ->
                                            val printlnCall =
                                                newMemberCall(
                                                    newMemberAccess(
                                                        "println",
                                                        newMemberAccess(
                                                            "out",
                                                            newReference("System"),
                                                        ),
                                                    ),
                                                    false,
                                                )
                                            printlnCall.arguments +=
                                                newLiteral("Cool loop", objectType("string"))
                                            loopBodyBlock += printlnCall
                                        }
                                    }
                                block += whileNode

                                val printlnCall2 =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall2.arguments +=
                                    newLiteral("After cool loop", objectType("string"))
                                block += printlnCall2

                                block += newReturn()
                            }
                    }

                    newMethod("whileComputedFalse", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val xDecl = newDeclarationStatement()
                                val x = newVariable("x", objectType("boolean"))
                                x.initializer = newLiteral(1, objectType("int"))
                                xDecl.declarations += x
                                scopeManager.addDeclaration(x)
                                block += xDecl

                                val whileNode =
                                    newWhile(enterScope = true) { w ->
                                        w.condition =
                                            newBinaryOperator(">").also {
                                                it.lhs = newReference("x")
                                                it.rhs = newLiteral(3, objectType("int"))
                                            }
                                        w.statement = newBlock { loopBodyBlock ->
                                            val printlnCall =
                                                newMemberCall(
                                                    newMemberAccess(
                                                        "println",
                                                        newMemberAccess(
                                                            "out",
                                                            newReference("System"),
                                                        ),
                                                    ),
                                                    false,
                                                )
                                            printlnCall.arguments +=
                                                newLiteral("Cool loop", objectType("string"))
                                            loopBodyBlock += printlnCall
                                        }
                                    }
                                block += whileNode

                                val printlnCall2 =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall2.arguments +=
                                    newLiteral("After cool loop", objectType("string"))
                                block += printlnCall2

                                block += newReturn()
                            }
                    }

                    newMethod("whileUnknown", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val yDecl = newDeclarationStatement()
                                val y = newVariable("y", objectType("int"))
                                y.initializer =
                                    newMemberCall(
                                        newMemberAccess("nextUInt", newReference("URandomKt")),
                                        false,
                                    )
                                yDecl.declarations += y
                                scopeManager.addDeclaration(y)
                                block += yDecl

                                val whileNode =
                                    newWhile(enterScope = true) { w ->
                                        w.condition =
                                            newBinaryOperator("<=").also {
                                                it.lhs = newReference("y")
                                                it.rhs = newLiteral(2, objectType("int"))
                                            }
                                        w.statement = newBlock { loopBodyBlock ->
                                            val printlnCall =
                                                newMemberCall(
                                                    newMemberAccess(
                                                        "println",
                                                        newMemberAccess(
                                                            "out",
                                                            newReference("System"),
                                                        ),
                                                    ),
                                                    false,
                                                )
                                            printlnCall.arguments +=
                                                newLiteral("Cool loop", objectType("string"))
                                            loopBodyBlock += printlnCall

                                            loopBodyBlock +=
                                                newAssign(
                                                    "=",
                                                    listOf(newReference("y")),
                                                    listOf(
                                                        newMemberCall(
                                                            newMemberAccess(
                                                                "nextUInt",
                                                                newReference("URandomKt"),
                                                            ),
                                                            false,
                                                        )
                                                    ),
                                                )
                                        }
                                    }
                                block += whileNode

                                val printlnCall2 =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall2.arguments +=
                                    newLiteral("After cool loop", objectType("string"))
                                block += printlnCall2

                                block += newReturn()
                            }
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
    }
}
