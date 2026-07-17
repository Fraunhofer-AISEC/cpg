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
                        block.statements += newReturn { ret ->
                            ret.returnValue =
                                newCall(newReference("toString")) {
                                    it.arguments += newReference("arg")
                                }
                        }
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
                            newVariable("a", objectType("int"), holder = aDecl) {
                                it.initializer = newLiteral(5, objectType("int"))
                            }
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            newVariable("b", objectType("string"), holder = bDecl) { b ->
                                b.initializer =
                                    newBinaryOperator("+") { outer ->
                                        outer.lhs =
                                            newBinaryOperator("+") { inner ->
                                                inner.lhs = newLiteral("bla", objectType("string"))
                                                inner.rhs =
                                                    newCall(newReference("foo")) {
                                                        it.arguments += newReference("a")
                                                    }
                                            }
                                        outer.rhs =
                                            newCall(newReference("foo")) {
                                                it.arguments += newCall(newReference("bar"))
                                            }
                                    }
                            }
                            block.statements += bDecl

                            block.statements +=
                                newCall(newReference("print")) { it.arguments += newReference("a") }

                            block.statements +=
                                newCall(newReference("print")) { it.arguments += newReference("b") }

                            block.statements +=
                                newAssign(
                                    "+=",
                                    listOf(newReference("b")),
                                    listOf(newLiteral("added", objectType("string"))),
                                )

                            block.statements += newIfElse { ifElse ->
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

                            block.statements +=
                                newCall(newReference("baz")) { bazCall ->
                                    bazCall.arguments +=
                                        newBinaryOperator("+") {
                                            it.lhs = newReference("a")
                                            it.rhs = newReference("b")
                                        }
                                }
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
                            newVariable("a", objectType("int"), holder = aDecl) {
                                it.initializer = newLiteral(5, objectType("int"))
                            }
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            newVariable("b", objectType("string"), holder = bDecl) { b ->
                                b.initializer =
                                    newBinaryOperator("+") { outer ->
                                        outer.lhs =
                                            newBinaryOperator("+") { inner ->
                                                inner.lhs = newLiteral("bla", objectType("string"))
                                                inner.rhs = newReference("a")
                                            }
                                        outer.rhs =
                                            newCall(newReference("foo")) {
                                                it.arguments += newCall(newReference("bar"))
                                            }
                                    }
                            }
                            block.statements += bDecl

                            block.statements +=
                                newCall(newReference("print")) { it.arguments += newReference("b") }

                            block.statements +=
                                newCall(newReference("baz")) { bazCall ->
                                    bazCall.arguments +=
                                        newBinaryOperator("+") {
                                            it.lhs = newReference("a")
                                            it.rhs = newReference("b")
                                        }
                                }
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
                            newVariable("a", objectType("int"), holder = aDecl) {
                                it.initializer = newLiteral(5, objectType("int"))
                            }
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            newVariable("b", objectType("string"), holder = bDecl) { b ->
                                b.initializer =
                                    newBinaryOperator("+") { outer ->
                                        outer.lhs =
                                            newBinaryOperator("+") { inner ->
                                                inner.lhs = newLiteral("bla", objectType("string"))
                                                inner.rhs = newReference("a")
                                            }
                                        outer.rhs =
                                            newCall(newReference("foo")) {
                                                it.arguments += newCall(newReference("bar"))
                                            }
                                    }
                            }
                            block.statements += bDecl

                            block.statements += newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==") {
                                        it.lhs = newReference("b")
                                        it.rhs = newLiteral("test", objectType("string"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newCall(newReference("print")) {
                                                it.arguments += newReference("a")
                                            }
                                    }
                            }

                            block.statements +=
                                newCall(newReference("print")) { it.arguments += newReference("b") }

                            block.statements +=
                                newCall(newReference("baz")) { bazCall ->
                                    bazCall.arguments +=
                                        newBinaryOperator("+") {
                                            it.lhs = newReference("a")
                                            it.rhs = newReference("b")
                                        }
                                }
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
                            newVariable("a", objectType("int"), holder = aDecl) {
                                it.initializer = newLiteral(5, objectType("int"))
                            }
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            newVariable("b", objectType("string"), holder = bDecl) { b ->
                                b.initializer =
                                    newBinaryOperator("+") { outer ->
                                        outer.lhs =
                                            newBinaryOperator("+") { inner ->
                                                inner.lhs = newLiteral("bla", objectType("string"))
                                                inner.rhs = newReference("a")
                                            }
                                        outer.rhs =
                                            newCall(newReference("foo")) {
                                                it.arguments += newCall(newReference("bar"))
                                            }
                                    }
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
                            block.statements += newIfElse { ifElse ->
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newCall(newReference("print")) {
                                                it.arguments += newReference("a")
                                            }
                                    }
                                ifElse.elseStatement =
                                    newBlock(enterScope = true) { elseBlock ->
                                        elseBlock.statements +=
                                            newCall(newReference("print")) {
                                                it.arguments += newReference("b")
                                            }
                                    }
                                ifElse.condition =
                                    newCall(newReference("print")) {
                                        it.arguments += newReference("b")
                                    }
                            }

                            block.statements +=
                                newCall(newReference("baz")) { bazCall ->
                                    bazCall.arguments +=
                                        newBinaryOperator("+") {
                                            it.lhs = newReference("a")
                                            it.rhs = newReference("b")
                                        }
                                }
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
                            newVariable("a", objectType("int"), holder = aDecl) {
                                it.initializer = newLiteral(5, objectType("int"))
                            }
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            newVariable("b", objectType("string"), holder = bDecl) { b ->
                                b.initializer =
                                    newBinaryOperator("+") {
                                        it.lhs = newLiteral("bla", objectType("string"))
                                        it.rhs =
                                            newCall(newReference("foo")) {
                                                it.arguments += newCall(newReference("bar"))
                                            }
                                    }
                            }
                            block.statements += bDecl

                            block.statements +=
                                newCall(newReference("print")) { it.arguments += newReference("a") }

                            block.statements +=
                                newCall(newReference("baz")) { bazCall ->
                                    bazCall.arguments +=
                                        newBinaryOperator("+") {
                                            it.lhs = newReference("a")
                                            it.rhs = newReference("b")
                                        }
                                }
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
                            newVariable("a", objectType("int"), holder = aDecl) {
                                it.initializer = newLiteral(5, objectType("int"))
                            }
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            newVariable("b", objectType("string"), holder = bDecl) { b ->
                                b.initializer =
                                    newBinaryOperator("+") {
                                        it.lhs = newLiteral("bla", objectType("string"))
                                        it.rhs =
                                            newCall(newReference("foo")) {
                                                it.arguments += newCall(newReference("bar"))
                                            }
                                    }
                            }
                            block.statements += bDecl

                            block.statements += newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==") {
                                        it.lhs = newReference("b")
                                        it.rhs = newLiteral("test", objectType("string"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newCall(newReference("print")) {
                                                it.arguments += newReference("a")
                                            }
                                    }
                            }

                            block.statements +=
                                newCall(newReference("baz")) { bazCall ->
                                    bazCall.arguments +=
                                        newBinaryOperator("+") {
                                            it.lhs = newReference("a")
                                            it.rhs = newReference("b")
                                        }
                                }
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
                            newVariable("a", objectType("int"), holder = aDecl) {
                                it.initializer = newLiteral(5, objectType("int"))
                            }
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            newVariable("b", objectType("string"), holder = bDecl) { b ->
                                b.initializer =
                                    newBinaryOperator("+") {
                                        it.lhs = newLiteral("bla", objectType("string"))
                                        it.rhs =
                                            newCall(newReference("foo")) {
                                                it.arguments += newCall(newReference("bar"))
                                            }
                                    }
                            }
                            block.statements += bDecl

                            block.statements += newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==") {
                                        it.lhs = newReference("b")
                                        it.rhs = newLiteral("test", objectType("string"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newCall(newReference("print")) {
                                                it.arguments += newReference("a")
                                            }
                                    }
                                ifElse.elseStatement =
                                    newBlock(enterScope = true) { elseBlock ->
                                        elseBlock.statements +=
                                            newCall(newReference("print")) {
                                                it.arguments += newReference("a")
                                            }
                                    }
                            }

                            block.statements +=
                                newCall(newReference("baz")) { bazCall ->
                                    bazCall.arguments +=
                                        newBinaryOperator("+") {
                                            it.lhs = newReference("a")
                                            it.rhs = newReference("b")
                                        }
                                }
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
                            newVariable("a", objectType("int"), holder = aDecl) {
                                it.initializer = newLiteral(5, objectType("int"))
                            }
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            newVariable("b", objectType("string"), holder = bDecl) { b ->
                                b.initializer =
                                    newBinaryOperator("+") {
                                        it.lhs = newLiteral("bla", objectType("string"))
                                        it.rhs =
                                            newCall(newReference("foo")) {
                                                it.arguments += newCall(newReference("bar"))
                                            }
                                    }
                            }
                            block.statements += bDecl

                            block.statements += newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==") {
                                        it.lhs = newReference("b")
                                        it.rhs = newLiteral("test", objectType("string"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newCall(newReference("print")) {
                                                it.arguments += newReference("a")
                                            }

                                        thenBlock.statements +=
                                            newCall(newReference("baz")) { bazCall ->
                                                bazCall.arguments +=
                                                    newBinaryOperator("+") {
                                                        it.lhs = newReference("a")
                                                        it.rhs = newReference("b")
                                                    }
                                            }
                                    }
                                ifElse.elseStatement =
                                    newBlock(enterScope = true) { elseBlock ->
                                        elseBlock.statements +=
                                            newCall(newReference("print")) {
                                                it.arguments += newReference("c")
                                            }
                                    }
                            }
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
                            newVariable("a", objectType("int"), holder = aDecl) {
                                it.initializer = newLiteral(5, objectType("int"))
                            }
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            newVariable("b", objectType("string"), holder = bDecl) { b ->
                                b.initializer =
                                    newBinaryOperator("+") { outer ->
                                        outer.lhs =
                                            newBinaryOperator("+") { inner ->
                                                inner.lhs = newLiteral("bla", objectType("string"))
                                                inner.rhs =
                                                    newCall(newReference("foo")) {
                                                        it.arguments += newReference("a")
                                                    }
                                            }
                                        outer.rhs =
                                            newCall(newReference("foo")) {
                                                it.arguments += newCall(newReference("bar"))
                                            }
                                    }
                            }
                            block.statements += bDecl

                            block.statements +=
                                newCall(newReference("print")) { it.arguments += newReference("b") }

                            block.statements +=
                                newCall(newReference("baz")) { bazCall ->
                                    bazCall.arguments +=
                                        newBinaryOperator("+") {
                                            it.lhs = newReference("a")
                                            it.rhs = newReference("b")
                                        }
                                }
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
                            newVariable("a", objectType("int"), holder = aDecl) {
                                it.initializer = newLiteral(5, objectType("int"))
                            }
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            newVariable("b", objectType("string"), holder = bDecl) { b ->
                                b.initializer =
                                    newBinaryOperator("+") { outer ->
                                        outer.lhs =
                                            newBinaryOperator("+") { inner ->
                                                inner.lhs = newLiteral("bla", objectType("string"))
                                                inner.rhs =
                                                    newCall(newReference("foo")) {
                                                        it.arguments += newReference("a")
                                                    }
                                            }
                                        outer.rhs =
                                            newCall(newReference("foo")) {
                                                it.arguments += newCall(newReference("bar"))
                                            }
                                    }
                            }
                            block.statements += bDecl

                            block.statements += newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==") {
                                        it.lhs = newReference("b")
                                        it.rhs = newLiteral("test", objectType("string"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newCall(newReference("print")) {
                                                it.arguments += newReference("a")
                                            }
                                    }
                            }

                            block.statements +=
                                newCall(newReference("print")) { it.arguments += newReference("b") }

                            block.statements +=
                                newCall(newReference("baz")) { bazCall ->
                                    bazCall.arguments +=
                                        newBinaryOperator("+") {
                                            it.lhs = newReference("a")
                                            it.rhs = newReference("b")
                                        }
                                }
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
                            newVariable("a", objectType("int"), holder = aDecl) {
                                it.initializer = newLiteral(5, objectType("int"))
                            }
                            block.statements += aDecl

                            val bDecl = newDeclarationStatement()
                            newVariable("b", objectType("string"), holder = bDecl) { b ->
                                b.initializer =
                                    newBinaryOperator("+") { outer ->
                                        outer.lhs =
                                            newBinaryOperator("+") { inner ->
                                                inner.lhs = newLiteral("bla", objectType("string"))
                                                inner.rhs =
                                                    newCall(newReference("foo")) {
                                                        it.arguments += newReference("a")
                                                    }
                                            }
                                        outer.rhs =
                                            newCall(newReference("foo")) {
                                                it.arguments += newCall(newReference("bar"))
                                            }
                                    }
                            }
                            block.statements += bDecl

                            // Same `IfElse.addArgument` condition-overwrite quirk as in
                            // `validatorDataflowIfElse` -- see the comment there. The bare
                            // `call("print") { ref("b") }` written directly inside `ifStmt {}`'s
                            // block overwrites the "b == 'test'" condition, which is therefore
                            // built and immediately discarded in the original. Faithfully
                            // reproduced by skipping straight to the surviving condition.
                            block.statements += newIfElse { ifElse ->
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newCall(newReference("print")) {
                                                it.arguments += newReference("a")
                                            }
                                    }
                                ifElse.elseStatement =
                                    newBlock(enterScope = true) { elseBlock ->
                                        elseBlock.statements +=
                                            newCall(newReference("print")) {
                                                it.arguments += newReference("b")
                                            }
                                    }
                                ifElse.condition =
                                    newCall(newReference("print")) {
                                        it.arguments += newReference("b")
                                    }
                            }

                            block.statements +=
                                newCall(newReference("baz")) { bazCall ->
                                    bazCall.arguments +=
                                        newBinaryOperator("+") {
                                            it.lhs = newReference("a")
                                            it.rhs = newReference("b")
                                        }
                                }
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
                            block.statements += newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==") {
                                        it.lhs = newReference("i")
                                        it.rhs = newLiteral(1, objectType("int"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newCall(newReference("println")) {
                                                it.arguments +=
                                                    newLiteral("Then branch", objectType("string"))
                                            }
                                    }
                                ifElse.elseStatement =
                                    newBlock(enterScope = true) { elseBlock ->
                                        elseBlock.statements +=
                                            newCall(newReference("a")) {
                                                it.arguments += newLiteral(1, objectType("int"))
                                            }
                                    }
                            }

                            block.statements += newReturn()
                        }
                }

                newFunction("b", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("void"))
                    func.type = computeType(func)

                    newParameter("value", objectType("int"), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            block.statements +=
                                newCall(newReference("a")) {
                                    it.arguments += newReference("value", objectType("int"))
                                }

                            block.statements += newReturn()
                        }
                }

                newFunction("c", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("void"))
                    func.type = computeType(func)

                    newParameter("value", objectType("int"), holder = func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            block.statements +=
                                newCall(newReference("b")) {
                                    it.arguments += newReference("value", objectType("int"))
                                }

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
                            block.statements += newForEach { forEach ->
                                val iDeclStmt = newDeclarationStatement()
                                newVariable("i", objectType("int"), holder = iDeclStmt)
                                forEach.variable = iDeclStmt

                                forEach.iterable =
                                    newCall(newReference("range")) {
                                        it.arguments += newLiteral(1, objectType("int"))
                                    }

                                forEach.statement = newBlock { loopBodyBlock ->
                                    val tempDecl = newDeclarationStatement()
                                    newVariable("temp", holder = tempDecl) {
                                        it.initializer = newLiteral("start")
                                    }
                                    loopBodyBlock.statements += tempDecl

                                    loopBodyBlock.statements +=
                                        newCall(newReference("a")) {
                                            it.arguments += newReference("i", objectType("int"))
                                        }

                                    loopBodyBlock.statements +=
                                        newCall(newReference("b")) {
                                            it.arguments += newReference("i", objectType("int"))
                                        }

                                    loopBodyBlock.statements +=
                                        newCall(newReference("c")) {
                                            it.arguments += newReference("i", objectType("int"))
                                        }
                                }
                            }
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
    }
}
