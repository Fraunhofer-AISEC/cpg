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
package de.fraunhofer.aisec.cpg.testcases

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass

class FlowQueriesTest {

    companion object {
        /** Builds the `foo(arg: int): string { return toString(arg) }` helper function. */
        private fun LanguageFrontend<*, *>.buildFoo(tu: TranslationUnit) {
            newFunction("foo", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)

                newParameter("arg", objectType("int"), holder = func)

                func.body =
                    newBlock(enterScope = true) { block ->
                        val ret = newReturn()
                        val toStringCall = newCall(newReference("toString"))
                        toStringCall.arguments += newReference("arg")
                        ret.returnValue = toStringCall
                        block.statements += ret
                    }
            }
        }

        fun verySimpleDataflow(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerPass<ControlDependenceGraphPass>()
                    .registerPass<ProgramDependenceGraphPass>()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Dataflow.java")
                scopeManager.resetToGlobal(tu)

                buildFoo(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(incompleteType())
                    func.type = computeType(func)

                    newParameter("args", objectType("string").array(), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"), holder = aDecl)
                            a.initializer = newLiteral(5, objectType("int"))
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            val b = newVariable("b", objectType("string"), holder = bDecl)
                            val fooCallA = newCall(newReference("foo"))
                            fooCallA.arguments += newReference("a")
                            val fooCallBar = newCall(newReference("foo"))
                            fooCallBar.arguments += newCall(newReference("bar"))
                            b.initializer =
                                newBinaryOperator("+") { outer ->
                                    outer.lhs =
                                        newBinaryOperator("+") { inner ->
                                            inner.lhs = newLiteral("bla", objectType("string"))
                                            inner.rhs = fooCallA
                                        }
                                    outer.rhs = fooCallBar
                                }
                            block.statements += bDecl

                            val printA = newCall(newReference("print"))
                            printA.arguments += newReference("a")
                            block.statements += printA

                            val printB = newCall(newReference("print"))
                            printB.arguments += newReference("b")
                            block.statements += printB

                            block.statements +=
                                newAssign(
                                    "+=",
                                    listOf(newReference("b")),
                                    listOf(newLiteral("added", objectType("string"))),
                                )

                            val ifElse = newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==") {
                                        it.lhs = newReference("b")
                                        it.rhs = newLiteral("test", objectType("string"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("a")),
                                                listOf(newLiteral(10, objectType("int"))),
                                            )
                                    }
                                ifElse.elseStatement =
                                    newBlock(enterScope = true) { elseBlock ->
                                        elseBlock.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("b")),
                                                listOf(newLiteral("removed", objectType("string"))),
                                            )
                                    }
                            }
                            block.statements += ifElse

                            val bazCall = newCall(newReference("baz"))
                            bazCall.arguments +=
                                newBinaryOperator("+") {
                                    it.lhs = newReference("a")
                                    it.rhs = newReference("b")
                                }
                            block.statements += bazCall
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun validatorDataflowLinear(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Dataflow.java")
                scopeManager.resetToGlobal(tu)

                buildFoo(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(incompleteType())
                    func.type = computeType(func)

                    newParameter("args", objectType("string").array(), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"), holder = aDecl)
                            a.initializer = newLiteral(5, objectType("int"))
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            val b = newVariable("b", objectType("string"), holder = bDecl)
                            val fooCallBar = newCall(newReference("foo"))
                            fooCallBar.arguments += newCall(newReference("bar"))
                            b.initializer =
                                newBinaryOperator("+") { outer ->
                                    outer.lhs =
                                        newBinaryOperator("+") { inner ->
                                            inner.lhs = newLiteral("bla", objectType("string"))
                                            inner.rhs = newReference("a")
                                        }
                                    outer.rhs = fooCallBar
                                }
                            block.statements += bDecl

                            val printB = newCall(newReference("print"))
                            printB.arguments += newReference("b")
                            block.statements += printB

                            val bazCall = newCall(newReference("baz"))
                            bazCall.arguments +=
                                newBinaryOperator("+") {
                                    it.lhs = newReference("a")
                                    it.rhs = newReference("b")
                                }
                            block.statements += bazCall
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun validatorDataflowIf(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Dataflow.java")
                scopeManager.resetToGlobal(tu)

                buildFoo(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(incompleteType())
                    func.type = computeType(func)

                    newParameter("args", objectType("string").array(), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"), holder = aDecl)
                            a.initializer = newLiteral(5, objectType("int"))
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            val b = newVariable("b", objectType("string"), holder = bDecl)
                            val fooCallBar = newCall(newReference("foo"))
                            fooCallBar.arguments += newCall(newReference("bar"))
                            b.initializer =
                                newBinaryOperator("+") { outer ->
                                    outer.lhs =
                                        newBinaryOperator("+") { inner ->
                                            inner.lhs = newLiteral("bla", objectType("string"))
                                            inner.rhs = newReference("a")
                                        }
                                    outer.rhs = fooCallBar
                                }
                            block.statements += bDecl

                            val ifElse = newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==") {
                                        it.lhs = newReference("b")
                                        it.rhs = newLiteral("test", objectType("string"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        val printA = newCall(newReference("print"))
                                        printA.arguments += newReference("a")
                                        thenBlock.statements += printA
                                    }
                            }
                            block.statements += ifElse

                            val printB = newCall(newReference("print"))
                            printB.arguments += newReference("b")
                            block.statements += printB

                            val bazCall = newCall(newReference("baz"))
                            bazCall.arguments +=
                                newBinaryOperator("+") {
                                    it.lhs = newReference("a")
                                    it.rhs = newReference("b")
                                }
                            block.statements += bazCall
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun validatorDataflowIfElse(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Dataflow.java")
                scopeManager.resetToGlobal(tu)

                buildFoo(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(incompleteType())
                    func.type = computeType(func)

                    newParameter("args", objectType("string").array(), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"), holder = aDecl)
                            a.initializer = newLiteral(5, objectType("int"))
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            val b = newVariable("b", objectType("string"), holder = bDecl)
                            val fooCallBar = newCall(newReference("foo"))
                            fooCallBar.arguments += newCall(newReference("bar"))
                            b.initializer =
                                newBinaryOperator("+") { outer ->
                                    outer.lhs =
                                        newBinaryOperator("+") { inner ->
                                            inner.lhs = newLiteral("bla", objectType("string"))
                                            inner.rhs = newReference("a")
                                        }
                                    outer.rhs = fooCallBar
                                }
                            block.statements += bDecl

                            // Fluent's `IfElse.addArgument` unconditionally does
                            // `condition = expression` (see `IfElse.addArgument`). The original
                            // code has a bare `call("print") { ref("b") }` written directly
                            // inside `ifStmt {}`'s own block (not inside `thenStmt`/`elseStmt`),
                            // which self-attaches to the IfElse the same way `condition {}`'s
                            // "b == 'test'" did -- silently overwriting the intended condition
                            // with the print(b) call expression. The "b == 'test'" comparison is
                            // therefore built and then immediately discarded, never part of the
                            // final AST. Faithfully reproduced here: we skip building the
                            // discarded comparison and go straight to the condition that
                            // actually survives.
                            val ifElse = newIfElse { ifElse ->
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        val printA = newCall(newReference("print"))
                                        printA.arguments += newReference("a")
                                        thenBlock.statements += printA
                                    }
                                ifElse.elseStatement =
                                    newBlock(enterScope = true) { elseBlock ->
                                        val printB = newCall(newReference("print"))
                                        printB.arguments += newReference("b")
                                        elseBlock.statements += printB
                                    }
                                val printB2 = newCall(newReference("print"))
                                printB2.arguments += newReference("b")
                                ifElse.condition = printB2
                            }
                            block.statements += ifElse

                            val bazCall = newCall(newReference("baz"))
                            bazCall.arguments +=
                                newBinaryOperator("+") {
                                    it.lhs = newReference("a")
                                    it.rhs = newReference("b")
                                }
                            block.statements += bazCall
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun validatorDataflowLinearSimple(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Dataflow.java")
                scopeManager.resetToGlobal(tu)

                buildFoo(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(incompleteType())
                    func.type = computeType(func)

                    newParameter("args", objectType("string").array(), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"), holder = aDecl)
                            a.initializer = newLiteral(5, objectType("int"))
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            val b = newVariable("b", objectType("string"), holder = bDecl)
                            val fooCallBar = newCall(newReference("foo"))
                            fooCallBar.arguments += newCall(newReference("bar"))
                            b.initializer =
                                newBinaryOperator("+") {
                                    it.lhs = newLiteral("bla", objectType("string"))
                                    it.rhs = fooCallBar
                                }
                            block.statements += bDecl

                            val printA = newCall(newReference("print"))
                            printA.arguments += newReference("a")
                            block.statements += printA

                            val bazCall = newCall(newReference("baz"))
                            bazCall.arguments +=
                                newBinaryOperator("+") {
                                    it.lhs = newReference("a")
                                    it.rhs = newReference("b")
                                }
                            block.statements += bazCall
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun validatorDataflowIfSimple(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Dataflow.java")
                scopeManager.resetToGlobal(tu)

                buildFoo(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(incompleteType())
                    func.type = computeType(func)

                    newParameter("args", objectType("string").array(), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"), holder = aDecl)
                            a.initializer = newLiteral(5, objectType("int"))
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            val b = newVariable("b", objectType("string"), holder = bDecl)
                            val fooCallBar = newCall(newReference("foo"))
                            fooCallBar.arguments += newCall(newReference("bar"))
                            b.initializer =
                                newBinaryOperator("+") {
                                    it.lhs = newLiteral("bla", objectType("string"))
                                    it.rhs = fooCallBar
                                }
                            block.statements += bDecl

                            val ifElse = newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==") {
                                        it.lhs = newReference("b")
                                        it.rhs = newLiteral("test", objectType("string"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        val printA = newCall(newReference("print"))
                                        printA.arguments += newReference("a")
                                        thenBlock.statements += printA
                                    }
                            }
                            block.statements += ifElse

                            val bazCall = newCall(newReference("baz"))
                            bazCall.arguments +=
                                newBinaryOperator("+") {
                                    it.lhs = newReference("a")
                                    it.rhs = newReference("b")
                                }
                            block.statements += bazCall
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun validatorDataflowIfElseSimple(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Dataflow.java")
                scopeManager.resetToGlobal(tu)

                buildFoo(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(incompleteType())
                    func.type = computeType(func)

                    newParameter("args", objectType("string").array(), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"), holder = aDecl)
                            a.initializer = newLiteral(5, objectType("int"))
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            val b = newVariable("b", objectType("string"), holder = bDecl)
                            val fooCallBar = newCall(newReference("foo"))
                            fooCallBar.arguments += newCall(newReference("bar"))
                            b.initializer =
                                newBinaryOperator("+") {
                                    it.lhs = newLiteral("bla", objectType("string"))
                                    it.rhs = fooCallBar
                                }
                            block.statements += bDecl

                            val ifElse = newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==") {
                                        it.lhs = newReference("b")
                                        it.rhs = newLiteral("test", objectType("string"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        val printA = newCall(newReference("print"))
                                        printA.arguments += newReference("a")
                                        thenBlock.statements += printA
                                    }
                                ifElse.elseStatement =
                                    newBlock(enterScope = true) { elseBlock ->
                                        val printA2 = newCall(newReference("print"))
                                        printA2.arguments += newReference("a")
                                        elseBlock.statements += printA2
                                    }
                            }
                            block.statements += ifElse

                            val bazCall = newCall(newReference("baz"))
                            bazCall.arguments +=
                                newBinaryOperator("+") {
                                    it.lhs = newReference("a")
                                    it.rhs = newReference("b")
                                }
                            block.statements += bazCall
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun validatorDataflowOnlyIfSink(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Dataflow.java")
                scopeManager.resetToGlobal(tu)

                buildFoo(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(incompleteType())
                    func.type = computeType(func)

                    newParameter("args", objectType("string").array(), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"), holder = aDecl)
                            a.initializer = newLiteral(5, objectType("int"))
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            val b = newVariable("b", objectType("string"), holder = bDecl)
                            val fooCallBar = newCall(newReference("foo"))
                            fooCallBar.arguments += newCall(newReference("bar"))
                            b.initializer =
                                newBinaryOperator("+") {
                                    it.lhs = newLiteral("bla", objectType("string"))
                                    it.rhs = fooCallBar
                                }
                            block.statements += bDecl

                            val ifElse = newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==") {
                                        it.lhs = newReference("b")
                                        it.rhs = newLiteral("test", objectType("string"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        val printA = newCall(newReference("print"))
                                        printA.arguments += newReference("a")
                                        thenBlock.statements += printA

                                        val bazCall = newCall(newReference("baz"))
                                        bazCall.arguments +=
                                            newBinaryOperator("+") {
                                                it.lhs = newReference("a")
                                                it.rhs = newReference("b")
                                            }
                                        thenBlock.statements += bazCall
                                    }
                                ifElse.elseStatement =
                                    newBlock(enterScope = true) { elseBlock ->
                                        val printC = newCall(newReference("print"))
                                        printC.arguments += newReference("c")
                                        elseBlock.statements += printC
                                    }
                            }
                            block.statements += ifElse
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun validatorDataflowLinearWithCall(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Dataflow.java")
                scopeManager.resetToGlobal(tu)

                buildFoo(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(incompleteType())
                    func.type = computeType(func)

                    newParameter("args", objectType("string").array(), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"), holder = aDecl)
                            a.initializer = newLiteral(5, objectType("int"))
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            val b = newVariable("b", objectType("string"), holder = bDecl)
                            val fooCallA = newCall(newReference("foo"))
                            fooCallA.arguments += newReference("a")
                            val fooCallBar = newCall(newReference("foo"))
                            fooCallBar.arguments += newCall(newReference("bar"))
                            b.initializer =
                                newBinaryOperator("+") { outer ->
                                    outer.lhs =
                                        newBinaryOperator("+") { inner ->
                                            inner.lhs = newLiteral("bla", objectType("string"))
                                            inner.rhs = fooCallA
                                        }
                                    outer.rhs = fooCallBar
                                }
                            block.statements += bDecl

                            val printB = newCall(newReference("print"))
                            printB.arguments += newReference("b")
                            block.statements += printB

                            val bazCall = newCall(newReference("baz"))
                            bazCall.arguments +=
                                newBinaryOperator("+") {
                                    it.lhs = newReference("a")
                                    it.rhs = newReference("b")
                                }
                            block.statements += bazCall
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun validatorDataflowIfWithCall(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Dataflow.java")
                scopeManager.resetToGlobal(tu)

                buildFoo(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(incompleteType())
                    func.type = computeType(func)

                    newParameter("args", objectType("string").array(), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"), holder = aDecl)
                            a.initializer = newLiteral(5, objectType("int"))
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            val b = newVariable("b", objectType("string"), holder = bDecl)
                            val fooCallA = newCall(newReference("foo"))
                            fooCallA.arguments += newReference("a")
                            val fooCallBar = newCall(newReference("foo"))
                            fooCallBar.arguments += newCall(newReference("bar"))
                            b.initializer =
                                newBinaryOperator("+") { outer ->
                                    outer.lhs =
                                        newBinaryOperator("+") { inner ->
                                            inner.lhs = newLiteral("bla", objectType("string"))
                                            inner.rhs = fooCallA
                                        }
                                    outer.rhs = fooCallBar
                                }
                            block.statements += bDecl

                            val ifElse = newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==") {
                                        it.lhs = newReference("b")
                                        it.rhs = newLiteral("test", objectType("string"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        val printA = newCall(newReference("print"))
                                        printA.arguments += newReference("a")
                                        thenBlock.statements += printA
                                    }
                            }
                            block.statements += ifElse

                            val printB = newCall(newReference("print"))
                            printB.arguments += newReference("b")
                            block.statements += printB

                            val bazCall = newCall(newReference("baz"))
                            bazCall.arguments +=
                                newBinaryOperator("+") {
                                    it.lhs = newReference("a")
                                    it.rhs = newReference("b")
                                }
                            block.statements += bazCall
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun validatorDataflowIfElseWithCall(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Dataflow.java")
                scopeManager.resetToGlobal(tu)

                buildFoo(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(incompleteType())
                    func.type = computeType(func)

                    newParameter("args", objectType("string").array(), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"), holder = aDecl)
                            a.initializer = newLiteral(5, objectType("int"))
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            val b = newVariable("b", objectType("string"), holder = bDecl)
                            val fooCallA = newCall(newReference("foo"))
                            fooCallA.arguments += newReference("a")
                            val fooCallBar = newCall(newReference("foo"))
                            fooCallBar.arguments += newCall(newReference("bar"))
                            b.initializer =
                                newBinaryOperator("+") { outer ->
                                    outer.lhs =
                                        newBinaryOperator("+") { inner ->
                                            inner.lhs = newLiteral("bla", objectType("string"))
                                            inner.rhs = fooCallA
                                        }
                                    outer.rhs = fooCallBar
                                }
                            block.statements += bDecl

                            // Same `IfElse.addArgument` condition-overwrite quirk as in
                            // `validatorDataflowIfElse` -- see the comment there. The bare
                            // `call("print") { ref("b") }` written directly inside `ifStmt {}`'s
                            // block overwrites the "b == 'test'" condition, which is therefore
                            // built and immediately discarded in the original. Faithfully
                            // reproduced by skipping straight to the surviving condition.
                            val ifElse = newIfElse { ifElse ->
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        val printA = newCall(newReference("print"))
                                        printA.arguments += newReference("a")
                                        thenBlock.statements += printA
                                    }
                                ifElse.elseStatement =
                                    newBlock(enterScope = true) { elseBlock ->
                                        val printB = newCall(newReference("print"))
                                        printB.arguments += newReference("b")
                                        elseBlock.statements += printB
                                    }
                                val printB2 = newCall(newReference("print"))
                                printB2.arguments += newReference("b")
                                ifElse.condition = printB2
                            }
                            block.statements += ifElse

                            val bazCall = newCall(newReference("baz"))
                            bazCall.arguments +=
                                newBinaryOperator("+") {
                                    it.lhs = newReference("a")
                                    it.rhs = newReference("b")
                                }
                            block.statements += bazCall
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun loopDetection(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Dataflow.java")
                scopeManager.resetToGlobal(tu)

                newFunction("a", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("void"))
                    func.type = computeType(func)

                    newParameter("value", objectType("int"), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val ifElse = newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==") {
                                        it.lhs = newReference("i")
                                        it.rhs = newLiteral(1, objectType("int"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        val printlnCall = newCall(newReference("println"))
                                        printlnCall.arguments +=
                                            newLiteral("Then branch", objectType("string"))
                                        thenBlock.statements += printlnCall
                                    }
                                ifElse.elseStatement =
                                    newBlock(enterScope = true) { elseBlock ->
                                        val recCall = newCall(newReference("a"))
                                        recCall.arguments += newLiteral(1, objectType("int"))
                                        elseBlock.statements += recCall
                                    }
                            }
                            block.statements += ifElse

                            block.statements += newReturn()
                        }
                }

                newFunction("b", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("void"))
                    func.type = computeType(func)

                    newParameter("value", objectType("int"), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val aCall = newCall(newReference("a"))
                            aCall.arguments += newReference("value", objectType("int"))
                            block.statements += aCall

                            block.statements += newReturn()
                        }
                }

                newFunction("c", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("void"))
                    func.type = computeType(func)

                    newParameter("value", objectType("int"), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val bCall = newCall(newReference("b"))
                            bCall.arguments += newReference("value", objectType("int"))
                            block.statements += bCall

                            block.statements += newReturn()
                        }
                }

                // Fluent's `forEachStmt()` never enters/leaves a scope for the `ForEach` node
                // itself (unlike `whileStmt`), so the loop variable "i" ends up declared
                // directly in the enclosing method scope. Faithfully reproduced here by not
                // passing `enterScope` to `newForEach()`/`newBlock()` below.
                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(incompleteType())
                    func.type = computeType(func)

                    newParameter("args", objectType("string").array(), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val forEachNode = newForEach { forEach ->
                                val iDeclStmt = newDeclarationStatement()
                                val i = newVariable("i", objectType("int"), holder = iDeclStmt)
                                forEach.variable = iDeclStmt

                                val rangeCall = newCall(newReference("range"))
                                rangeCall.arguments += newLiteral(1, objectType("int"))
                                forEach.iterable = rangeCall

                                forEach.statement = newBlock { loopBodyBlock ->
                                    val tempDecl = newDeclarationStatement()
                                    val temp = newVariable("temp", holder = tempDecl)
                                    temp.initializer = newLiteral("start")
                                    loopBodyBlock.statements += tempDecl

                                    val aCall = newCall(newReference("a"))
                                    aCall.arguments += newReference("i", objectType("int"))
                                    loopBodyBlock.statements += aCall

                                    val bCall = newCall(newReference("b"))
                                    bCall.arguments += newReference("i", objectType("int"))
                                    loopBodyBlock.statements += bCall

                                    val cCall = newCall(newReference("c"))
                                    cCall.arguments += newReference("i", objectType("int"))
                                    loopBodyBlock.statements += cCall
                                }
                            }
                            block.statements += forEachNode
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
    }
}
