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
                                val arrayDeclStmt = newDeclarationStatement()
                                val arrayVar =
                                    newVariable("array", objectType("int").array()).also {
                                        it.initializer =
                                            newArrayConstruction().also { ac ->
                                                ac.addDimension(newLiteral(3, objectType("int")))
                                            }
                                    }
                                arrayDeclStmt.declarations += arrayVar
                                scopeManager.addDeclaration(arrayVar)
                                block.statements += arrayDeclStmt

                                val forNode = newFor { for_ ->
                                    val iDeclStmt = newDeclarationStatement()
                                    val iVar =
                                        newVariable("i", objectType("int")).also {
                                            it.initializer = newLiteral(0, objectType("int"))
                                        }
                                    iDeclStmt.declarations += iVar
                                    scopeManager.addDeclaration(iVar)
                                    for_.initializerStatement = iDeclStmt

                                    for_.condition =
                                        newBinaryOperator("<").also {
                                            it.lhs = newReference("i")
                                            it.rhs =
                                                newMemberAccess("length", newReference("array"))
                                        }

                                    for_.iterationStatement =
                                        newUnaryOperator("++", postfix = true, prefix = false)
                                            .also { it.input = newReference("i") }

                                    for_.statement = newBlock { loopBody ->
                                        loopBody.statements +=
                                            newAssign(
                                                "=",
                                                listOf(
                                                    newSubscription().also {
                                                        it.arrayExpression = newReference("array")
                                                        it.subscriptExpression = newReference("i")
                                                    }
                                                ),
                                                listOf(newReference("i")),
                                            )
                                    }
                                }
                                block.statements += forNode

                                val printlnCall1 =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall1.arguments +=
                                    newSubscription().also {
                                        it.arrayExpression = newReference("array")
                                        it.subscriptExpression = newLiteral(1, objectType("int"))
                                    }
                                block.statements += printlnCall1

                                val strDeclStmt = newDeclarationStatement()
                                val strVar =
                                    newVariable("str", objectType("String")).also {
                                        it.initializer = newLiteral("abcde", objectType("String"))
                                    }
                                strDeclStmt.declarations += strVar
                                scopeManager.addDeclaration(strVar)
                                block.statements += strDeclStmt

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
                                val iDeclStmt = newDeclarationStatement()
                                val iVar =
                                    newVariable("i", objectType("int")).also {
                                        it.initializer = newLiteral(3, objectType("int"))
                                    }
                                iDeclStmt.declarations += iVar
                                scopeManager.addDeclaration(iVar)
                                block.statements += iDeclStmt

                                val sDeclStmt = newDeclarationStatement()
                                val sVar = newVariable("s", objectType("String"))
                                sDeclStmt.declarations += sVar
                                scopeManager.addDeclaration(sVar)
                                block.statements += sDeclStmt

                                // Fluent's "lt" infix operator has no ArgumentHolder context, so
                                // it never actually attaches the comparison it builds -- the
                                // self-attaching ref("i")/literal(2) operands silently overwrite
                                // each other on the IfElse (an ArgumentHolder), leaving the
                                // *literal* as the "condition" instead of the intended comparison.
                                // Faithfully reproduced here (confirmed via the original
                                // Fluent-based test).
                                val ifElse = newIfElse { ifElse ->
                                    ifElse.condition = newLiteral(2, objectType("int"))

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
                                block.statements += ifElse

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
                                            newBinaryOperator("+").also {
                                                it.lhs = newReference("s")
                                                it.rhs = newLiteral("?", objectType("string"))
                                            }
                                        ),
                                    )

                                block.statements +=
                                    newUnaryOperator("++", postfix = true, prefix = false).also {
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
                            val bDeclStmt = newDeclarationStatement()
                            val bVar =
                                newVariable("b", objectType("int")).also {
                                    it.initializer =
                                        newBinaryOperator("+").also { bo ->
                                            bo.lhs = newLiteral(1, objectType("int"))
                                            bo.rhs = newLiteral(1, objectType("int"))
                                        }
                                }
                            bDeclStmt.declarations += bVar
                            scopeManager.addDeclaration(bVar)
                            block.statements += bDeclStmt

                            block.statements +=
                                newCall(newReference("println")).also {
                                    it.arguments += newReference("b")
                                }

                            val aDeclStmt = newDeclarationStatement()
                            val aVar =
                                newVariable("a", objectType("int")).also {
                                    it.initializer = newLiteral(1, objectType("int"))
                                }
                            aDeclStmt.declarations += aVar
                            scopeManager.addDeclaration(aVar)
                            block.statements += aDeclStmt

                            block.statements +=
                                newAssign(
                                    "=",
                                    listOf(newReference("a")),
                                    listOf(newLiteral(2, objectType("int"))),
                                )

                            block.statements +=
                                newCall(newReference("println")).also {
                                    it.arguments += newReference("a")
                                }

                            val cDeclStmt = newDeclarationStatement()
                            val cVar =
                                newVariable("c", objectType("int")).also {
                                    it.initializer =
                                        newBinaryOperator("-").also { bo ->
                                            bo.lhs = newLiteral(5, objectType("int"))
                                            bo.rhs = newLiteral(2, objectType("int"))
                                        }
                                }
                            cDeclStmt.declarations += cVar
                            scopeManager.addDeclaration(cVar)
                            block.statements += cDeclStmt

                            val dDeclStmt = newDeclarationStatement()
                            val dVar =
                                newVariable("d", objectType("float")).also {
                                    it.initializer =
                                        newBinaryOperator("/").also { bo ->
                                            bo.lhs = newLiteral(8, objectType("int"))
                                            bo.rhs = newLiteral(3, objectType("int"))
                                        }
                                }
                            dDeclStmt.declarations += dVar
                            scopeManager.addDeclaration(dVar)
                            block.statements += dDeclStmt

                            val eDeclStmt = newDeclarationStatement()
                            val eVar =
                                newVariable("e", objectType("float")).also {
                                    it.initializer =
                                        newBinaryOperator("/").also { bo ->
                                            bo.lhs = newLiteral(7.0, objectType("float"))
                                            bo.rhs = newLiteral(2, objectType("int"))
                                        }
                                }
                            eDeclStmt.declarations += eVar
                            scopeManager.addDeclaration(eVar)
                            block.statements += eDeclStmt

                            val fDeclStmt = newDeclarationStatement()
                            val fVar =
                                newVariable("f", objectType("int")).also {
                                    it.initializer =
                                        newBinaryOperator("*").also { bo ->
                                            bo.lhs = newLiteral(2, objectType("int"))
                                            bo.rhs = newLiteral(5, objectType("int"))
                                        }
                                }
                            fDeclStmt.declarations += fVar
                            scopeManager.addDeclaration(fVar)
                            block.statements += fDeclStmt

                            val gDeclStmt = newDeclarationStatement()
                            val gVar =
                                newVariable("g", objectType("int")).also {
                                    it.initializer =
                                        newUnaryOperator("-", postfix = false, prefix = false)
                                            .also { it.input = newReference("c") }
                                }
                            gDeclStmt.declarations += gVar
                            scopeManager.addDeclaration(gVar)
                            block.statements += gDeclStmt

                            block.statements +=
                                newCall(newReference("println")).also {
                                    it.arguments +=
                                        newBinaryOperator("+").also { bo ->
                                            bo.lhs = newLiteral("Hello ", objectType("String"))
                                            bo.rhs = newLiteral("world", objectType("String"))
                                        }
                                }

                            val hDeclStmt = newDeclarationStatement()
                            val hVar =
                                newVariable("h", objectType("bool")).also {
                                    it.initializer =
                                        newBinaryOperator("<=").also { bo ->
                                            bo.lhs = newLiteral(5, objectType("int"))
                                            bo.rhs = newLiteral(2, objectType("int"))
                                        }
                                }
                            hDeclStmt.declarations += hVar
                            scopeManager.addDeclaration(hVar)
                            block.statements += hDeclStmt

                            val iVarDeclStmt = newDeclarationStatement()
                            val iVar2 =
                                newVariable("i", objectType("bool")).also {
                                    it.initializer =
                                        newBinaryOperator(">").also { bo ->
                                            bo.lhs = newLiteral(3, objectType("int"))
                                            bo.rhs = newLiteral(3, objectType("int"))
                                        }
                                }
                            iVarDeclStmt.declarations += iVar2
                            scopeManager.addDeclaration(iVar2)
                            block.statements += iVarDeclStmt

                            val jDeclStmt = newDeclarationStatement()
                            val jVar =
                                newVariable("j", objectType("bool")).also {
                                    it.initializer =
                                        newBinaryOperator(">=").also { bo ->
                                            bo.lhs = newLiteral(3, objectType("int"))
                                            bo.rhs = newLiteral(3.2, objectType("float"))
                                        }
                                }
                            jDeclStmt.declarations += jVar
                            scopeManager.addDeclaration(jVar)
                            block.statements += jDeclStmt

                            val kDeclStmt = newDeclarationStatement()
                            val kVar =
                                newVariable("k", objectType("bool")).also {
                                    it.initializer =
                                        newBinaryOperator("<=").also { bo ->
                                            bo.lhs = newLiteral(3.1, objectType("float"))
                                            bo.rhs = newLiteral(3, objectType("int"))
                                        }
                                }
                            kDeclStmt.declarations += kVar
                            scopeManager.addDeclaration(kVar)
                            block.statements += kDeclStmt

                            val lDeclStmt = newDeclarationStatement()
                            val lVar =
                                newVariable("l", objectType("bool")).also {
                                    it.initializer =
                                        newBinaryOperator(">=").also { bo ->
                                            bo.lhs = newLiteral(3L, objectType("long"))
                                            bo.rhs =
                                                newCast().also { cast ->
                                                    cast.castType = objectType("float")
                                                    cast.expression =
                                                        newLiteral(3.1, objectType("float"))
                                                }
                                        }
                                }
                            lDeclStmt.declarations += lVar
                            scopeManager.addDeclaration(lVar)
                            block.statements += lDeclStmt

                            val mDeclStmt = newDeclarationStatement()
                            val mVar =
                                newVariable("m", objectType("bool")).also {
                                    it.initializer =
                                        newBinaryOperator(">=").also { bo ->
                                            bo.lhs =
                                                newCast().also { cast ->
                                                    cast.castType = objectType("char")
                                                    cast.expression =
                                                        newLiteral(3, objectType("int"))
                                                }
                                            bo.rhs = newLiteral(3.1, objectType("float"))
                                        }
                                }
                            mDeclStmt.declarations += mVar
                            scopeManager.addDeclaration(mVar)
                            block.statements += mDeclStmt

                            val nDeclStmt = newDeclarationStatement()
                            val nVar =
                                newVariable("n", objectType("bool")).also {
                                    it.initializer =
                                        newBinaryOperator("==").also { bo ->
                                            bo.lhs = newLiteral(3, objectType("int"))
                                            bo.rhs = newLiteral(3.1, objectType("float"))
                                        }
                                }
                            nDeclStmt.declarations += nVar
                            scopeManager.addDeclaration(nVar)
                            block.statements += nDeclStmt

                            block.statements +=
                                newReturn().also {
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
                                newCall(newReference("srand")).also {
                                    it.arguments +=
                                        newCall(newReference("time")).also { timeCall ->
                                            timeCall.arguments += newReference("NULL")
                                        }
                                }

                            val bDeclStmt = newDeclarationStatement()
                            val bVar =
                                newVariable("b", objectType("int")).also {
                                    it.initializer = newLiteral(1, objectType("int"))
                                }
                            bDeclStmt.declarations += bVar
                            scopeManager.addDeclaration(bVar)
                            block.statements += bDeclStmt

                            // Fluent's "lt" infix operator has no ArgumentHolder context, so it
                            // never actually attaches the comparison it builds -- the
                            // self-attaching call("rand")/literal(10) operands silently overwrite
                            // each other on the IfElse (an ArgumentHolder), leaving the *literal*
                            // as the "condition" instead of the intended comparison. Faithfully
                            // reproduced here (confirmed via the original Fluent-based test).
                            val ifElse1 = newIfElse { ifElse ->
                                ifElse.condition = newLiteral(10, objectType("int"))

                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("b")),
                                                listOf(
                                                    newBinaryOperator("+").also {
                                                        it.lhs = newReference("b")
                                                        it.rhs = newLiteral(1, objectType("int"))
                                                    }
                                                ),
                                            )
                                    }
                            }
                            block.statements += ifElse1

                            block.statements +=
                                newCall(newReference("println")).also {
                                    it.arguments += newReference("b")
                                } // 1, 2

                            // Unlike "lt" above, "gt" (via its ArgumentHolder context) does end up
                            // attaching the correct comparison here -- its own `holder += node`
                            // call happens after (and therefore overwrites) the self-attaching
                            // call/literal operands.
                            val ifElse2 = newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator(">").also {
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
                                                    newBinaryOperator("-").also {
                                                        it.lhs = newReference("b")
                                                        it.rhs = newLiteral(1, objectType("int"))
                                                    }
                                                ),
                                            )
                                    }
                            }
                            block.statements += ifElse2

                            block.statements +=
                                newCall(newReference("println")).also {
                                    it.arguments += newReference("b")
                                } // 0, 1, 2

                            val ifElse3 = newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator(">").also {
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
                                                    newBinaryOperator("*").also {
                                                        it.lhs = newReference("b")
                                                        it.rhs = newLiteral(2, objectType("int"))
                                                    }
                                                ),
                                            )
                                    }
                            }
                            block.statements += ifElse3

                            block.statements +=
                                newCall(newReference("println")).also {
                                    it.arguments += newReference("b")
                                } // 0, 1, 2, 4

                            // Fluent's `ref("b") assign -ref("b")` (unlike the block-based
                            // `assign { ... }` used above) evaluates its rhs via the plain-value
                            // `assign(rhs: Expression)` overload. Building that rhs requires
                            // `unaryMinus()`'s `ArgumentHolder` context, which the enclosing
                            // `Block` (a `StatementHolder` only) cannot satisfy -- so resolution
                            // escapes all the way out to the only `ArgumentHolder` still in scope,
                            // the enclosing `IfElse` itself. Its self-attach therefore overwrites
                            // `IfElse.condition` with the very same `UnaryOperator` node that is
                            // also used as the assignment's rhs, i.e. the same node object ends up
                            // with two AST parents. Faithfully reproduced here (confirmed via the
                            // original Fluent-based test).
                            val decB =
                                newUnaryOperator("-", postfix = false, prefix = false).also {
                                    it.input = newReference("b")
                                }
                            val ifElse4 = newIfElse { ifElse ->
                                ifElse.condition = decB

                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newAssign("=", listOf(newReference("b")), listOf(decB))
                                    }
                            }
                            block.statements += ifElse4

                            block.statements +=
                                newCall(newReference("println")).also {
                                    it.arguments += newReference("b")
                                } // -4, -2, -1, 0, 1, 2, 4

                            val aDeclStmt = newDeclarationStatement()
                            val aVar =
                                newVariable("a", objectType("int")).also {
                                    it.initializer =
                                        newConditional(
                                            newBinaryOperator("<").also {
                                                it.lhs = newReference("b")
                                                it.rhs = newLiteral(2, objectType("int"))
                                            },
                                            newLiteral(3, objectType("int")),
                                            newUnaryOperator("++", postfix = true, prefix = false)
                                                .also {
                                                    it.input = newLiteral(5, objectType("int"))
                                                },
                                        )
                                }
                            aDeclStmt.declarations += aVar
                            scopeManager.addDeclaration(aVar)
                            block.statements += aDeclStmt

                            block.statements +=
                                newCall(newReference("println")).also {
                                    it.arguments += newReference("a")
                                } // 3, 6

                            block.statements +=
                                newReturn().also {
                                    it.returnValue = newLiteral(0, objectType("int"))
                                }
                        }
                }

                newFunction("loop", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val arrayDeclStmt = newDeclarationStatement()
                            val arrayVar =
                                newVariable("array", objectType("int").array()).also {
                                    it.initializer =
                                        newArrayConstruction().also { ac ->
                                            ac.addDimension(newLiteral(6, objectType("int")))
                                        }
                                }
                            arrayDeclStmt.declarations += arrayVar
                            scopeManager.addDeclaration(arrayVar)
                            block.statements += arrayDeclStmt

                            val forNode = newFor { for_ ->
                                val iDeclStmt = newDeclarationStatement()
                                val iVar =
                                    newVariable("i", objectType("int")).also {
                                        it.initializer = newLiteral(0, objectType("int"))
                                    }
                                iDeclStmt.declarations += iVar
                                scopeManager.addDeclaration(iVar)
                                for_.initializerStatement = iDeclStmt

                                for_.condition =
                                    newBinaryOperator("<").also {
                                        it.lhs = newReference("i")
                                        it.rhs = newLiteral(6, objectType("int"))
                                    }

                                for_.iterationStatement =
                                    newUnaryOperator("++", postfix = true, prefix = false).also {
                                        it.input = newReference("i")
                                    }

                                for_.statement = newBlock { loopBody ->
                                    loopBody.statements +=
                                        newAssign(
                                            "=",
                                            listOf(
                                                newSubscription().also {
                                                    it.arrayExpression = newReference("array")
                                                    it.subscriptExpression = newReference("i")
                                                }
                                            ),
                                            listOf(newReference("i")),
                                        )
                                }
                            }
                            block.statements += forNode

                            block.statements +=
                                newReturn().also {
                                    it.returnValue = newLiteral(0, objectType("int"))
                                }
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
    }
}
