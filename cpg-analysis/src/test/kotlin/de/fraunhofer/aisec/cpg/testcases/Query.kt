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

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType

class Query {
    companion object {
        fun getDataflow(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("Dataflow.java")
                scopeManager.resetToGlobal(tu)

                newRecord("Dataflow", "class", holder = tu, enterScope = true) { record ->
                    newField("attr", objectType("string"), holder = record)

                    newMethod("toString", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(objectType("string"))
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val ret = newReturn()
                                ret.returnValue =
                                    newBinaryOperator("+").also {
                                        it.lhs = newLiteral("Dataflow: attr=", objectType("string"))
                                        it.rhs = newReference("attr")
                                    }
                                block += ret
                            }
                    }

                    newMethod("test", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(objectType("string"))
                        method.type = computeType(method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val ret = newReturn()
                                ret.returnValue = newLiteral("abcd", objectType("string"))
                                block += ret
                            }
                    }

                    newMethod("print", holder = record, enterScope = true) { method ->
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        newParameter("s", objectType("string"), holder = method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val printlnCall =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall.addArgument(newReference("s"))
                                block += printlnCall

                                block += newReturn()
                            }
                    }

                    newMethod("main", holder = record, enterScope = true) { method ->
                        method.isStatic = true
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        newParameter("args", objectType("string").array(), holder = method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val scDecl = newDeclarationStatement()
                                val sc = newVariable("sc", objectType("Dataflow"))
                                val newExpr = newNew()
                                val construction = newConstruction("Dataflow")
                                construction.type = objectType("Dataflow")
                                newExpr.initializer = construction
                                sc.initializer = newExpr
                                scDecl.declarations += sc
                                scopeManager.addDeclaration(sc)
                                block += scDecl

                                val sDecl = newDeclarationStatement()
                                val s = newVariable("s", objectType("string"))
                                s.initializer =
                                    newMemberCall(
                                        newMemberAccess("toString", newReference("sc")),
                                        false,
                                    )
                                sDecl.declarations += s
                                scopeManager.addDeclaration(s)
                                block += sDecl

                                val printCall =
                                    newMemberCall(
                                        newMemberAccess("print", newReference("sc")),
                                        false,
                                    )
                                printCall.addArgument(newReference("s"))
                                block += printCall

                                val printCall2 =
                                    newMemberCall(
                                        newMemberAccess("print", newReference("sc")),
                                        false,
                                    )
                                val testCall =
                                    newMemberCall(
                                        newMemberAccess("test", newReference("sc")),
                                        false,
                                    )
                                printCall2.addArgument(testCall)
                                block += printCall2
                            }
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getComplexDataflow(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .inferenceConfiguration(InferenceConfiguration.builder().enabled(false).build())
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("ComplexDataflow.java")
                scopeManager.resetToGlobal(tu)

                newRecord("Dataflow", "class", holder = tu, enterScope = true) { record ->
                    // TODO: this field is static. How do we model this?
                    val logger =
                        newField("logger", objectType("Logger"), holder = record).also { field ->
                            field.modifiers = setOf("static")
                            val getLoggerCall =
                                newMemberCall(
                                    newMemberAccess("getLogger", newReference("Logger")),
                                    false,
                                )
                            getLoggerCall.addArgument(
                                newLiteral("DataflowLogger", objectType("string"))
                            )
                            field.initializer = getLoggerCall
                        }

                    newField("a", objectType("int"), holder = record)

                    newMethod("highlyCriticalOperation", holder = record, enterScope = true) {
                        method ->
                        method.isStatic = true
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        newParameter("s", objectType("string"), holder = method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val printlnCall =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall.addArgument(newReference("s"))
                                block += printlnCall

                                block += newReturn()
                            }
                    }

                    newMethod("main", holder = record, enterScope = true) { method ->
                        method.isStatic = true
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        newParameter("args", objectType("string").array(), holder = method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val scDecl = newDeclarationStatement()
                                val sc = newVariable("sc", objectType("Dataflow"))
                                val newExpr = newNew()
                                val construction = newConstruction("Dataflow")
                                construction.type = objectType("Dataflow")
                                newExpr.initializer = construction
                                sc.initializer = newExpr
                                scDecl.declarations += sc
                                scopeManager.addDeclaration(sc)
                                block += scDecl

                                block +=
                                    newAssign(
                                        "=",
                                        listOf(newMemberAccess("a", newReference("sc"))),
                                        listOf(newLiteral(5, objectType("int"))),
                                    )

                                val dataflowRef = newReference("Dataflow", objectType("Dataflow"))
                                dataflowRef.refersTo = record
                                val outerCall =
                                    newMemberCall(
                                        newMemberAccess("highlyCriticalOperation", dataflowRef),
                                        false,
                                    )
                                outerCall.isStatic = true
                                val integerRef = newReference("Integer", objectType("Integer"))
                                val innerCall =
                                    newMemberCall(newMemberAccess("toString", integerRef), false)
                                innerCall.type = objectType("string")
                                innerCall.isStatic = true
                                innerCall.addArgument(newMemberAccess("a", newReference("sc")))
                                outerCall.addArgument(innerCall)
                                block += outerCall

                                val logCall =
                                    newMemberCall(
                                        newMemberAccess("log", newReference("logger")),
                                        false,
                                    )
                                logCall.addArgument(newMemberAccess("INFO", newReference("Level")))
                                logCall.addArgument(
                                    newBinaryOperator("+").also { outer ->
                                        outer.lhs =
                                            newBinaryOperator("+").also { inner ->
                                                inner.lhs = newLiteral("put ", objectType("string"))
                                                inner.rhs = newMemberAccess("a", newReference("sc"))
                                            }
                                        outer.rhs =
                                            newLiteral(
                                                " into highlyCriticalOperation()",
                                                objectType("string"),
                                            )
                                    }
                                )
                                block += logCall

                                block += newReturn()
                            }
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getComplexDataflow2(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder().inferFunctions(false).build()
                    )
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("ComplexDataflow2.java")
                scopeManager.resetToGlobal(tu)

                newRecord("Dataflow", "class", holder = tu, enterScope = true) { record ->
                    // TODO: this field is static. How do we model this?
                    val logger =
                        newField("logger", objectType("Logger"), holder = record).also { field ->
                            field.modifiers = setOf("static")
                            val getLoggerCall =
                                newMemberCall(
                                    newMemberAccess("getLogger", newReference("Logger")),
                                    false,
                                )
                            getLoggerCall.addArgument(
                                newLiteral("DataflowLogger", objectType("string"))
                            )
                            field.initializer = getLoggerCall
                        }

                    newField("a", objectType("int"), holder = record)

                    newMethod("highlyCriticalOperation", holder = record, enterScope = true) {
                        method ->
                        method.isStatic = true
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        newParameter("s", objectType("string"), holder = method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val printlnCall =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall.addArgument(newReference("s"))
                                block += printlnCall

                                block += newReturn()
                            }
                    }

                    newMethod("main", holder = record, enterScope = true) { method ->
                        method.isStatic = true
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        newParameter("args", objectType("string").array(), holder = method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val scDecl = newDeclarationStatement()
                                val sc = newVariable("sc", objectType("Dataflow"))
                                val newExpr = newNew()
                                val construction = newConstruction("Dataflow")
                                construction.type = objectType("Dataflow")
                                newExpr.initializer = construction
                                sc.initializer = newExpr
                                scDecl.declarations += sc
                                scopeManager.addDeclaration(sc)
                                block += scDecl

                                block +=
                                    newAssign(
                                        "=",
                                        listOf(newMemberAccess("a", newReference("sc"))),
                                        listOf(newLiteral(5, objectType("int"))),
                                    )

                                val logCall =
                                    newMemberCall(
                                        newMemberAccess("log", newReference("logger")),
                                        false,
                                    )
                                logCall.addArgument(newMemberAccess("INFO", newReference("Level")))
                                logCall.addArgument(
                                    newBinaryOperator("+").also { outer ->
                                        outer.lhs =
                                            newBinaryOperator("+").also { inner ->
                                                inner.lhs = newLiteral("put ", objectType("string"))
                                                inner.rhs = newMemberAccess("a", newReference("sc"))
                                            }
                                        outer.rhs =
                                            newLiteral(
                                                " into highlyCriticalOperation()",
                                                objectType("string"),
                                            )
                                    }
                                )
                                block += logCall

                                val dataflowRef = newReference("Dataflow", objectType("Dataflow"))
                                dataflowRef.refersTo = record
                                val outerCall =
                                    newMemberCall(
                                        newMemberAccess("highlyCriticalOperation", dataflowRef),
                                        false,
                                    )
                                outerCall.isStatic = true
                                val integerRef = newReference("Integer", objectType("Integer"))
                                val innerCall =
                                    newMemberCall(newMemberAccess("toString", integerRef), false)
                                innerCall.type = objectType("string")
                                innerCall.isStatic = true
                                innerCall.addArgument(newMemberAccess("a", newReference("sc")))
                                outerCall.addArgument(innerCall)
                                block += outerCall

                                block += newReturn()
                            }
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getComplexDataflow3(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("ComplexDataflow3.java")
                scopeManager.resetToGlobal(tu)

                newRecord("Dataflow", "class", holder = tu, enterScope = true) { record ->
                    // TODO: this field is static. How do we model this?
                    val logger =
                        newField("logger", objectType("Logger"), holder = record).also { field ->
                            field.modifiers = setOf("static")
                            val getLoggerCall =
                                newMemberCall(
                                    newMemberAccess("getLogger", newReference("Logger")),
                                    false,
                                )
                            getLoggerCall.addArgument(
                                newLiteral("DataflowLogger", objectType("string"))
                            )
                            field.initializer = getLoggerCall
                        }

                    newField("a", objectType("int"), holder = record)

                    newMethod("highlyCriticalOperation", holder = record, enterScope = true) {
                        method ->
                        method.isStatic = true
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        newParameter("s", objectType("string"), holder = method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val printlnCall =
                                    newMemberCall(
                                        newMemberAccess(
                                            "println",
                                            newMemberAccess("out", newReference("System")),
                                        ),
                                        false,
                                    )
                                printlnCall.addArgument(newReference("s"))
                                block += printlnCall

                                block += newReturn()
                            }
                    }

                    newMethod("main", holder = record, enterScope = true) { method ->
                        method.isStatic = true
                        method.returnTypes = listOf(incompleteType())
                        method.type = computeType(method)

                        newParameter("args", objectType("string").array(), holder = method)

                        method.body =
                            newBlock(enterScope = true) { block ->
                                val scDecl = newDeclarationStatement()
                                val sc = newVariable("sc", objectType("Dataflow"))
                                val newExpr = newNew()
                                val construction = newConstruction("Dataflow")
                                construction.type = objectType("Dataflow")
                                newExpr.initializer = construction
                                sc.initializer = newExpr
                                scDecl.declarations += sc
                                scopeManager.addDeclaration(sc)
                                block += scDecl

                                block +=
                                    newAssign(
                                        "=",
                                        listOf(newMemberAccess("a", newReference("sc"))),
                                        listOf(newLiteral(5, objectType("int"))),
                                    )

                                val logCall =
                                    newMemberCall(
                                        newMemberAccess("log", newReference("logger")),
                                        false,
                                    )
                                logCall.addArgument(newMemberAccess("INFO", newReference("Level")))
                                logCall.addArgument(
                                    newBinaryOperator("+").also { outer ->
                                        outer.lhs =
                                            newBinaryOperator("+").also { inner ->
                                                inner.lhs = newLiteral("put ", objectType("string"))
                                                // Note: original uses ref("a") as base here (not
                                                // "sc"), faithfully reproduced.
                                                inner.rhs = newMemberAccess("a", newReference("a"))
                                            }
                                        outer.rhs =
                                            newLiteral(
                                                " into highlyCriticalOperation()",
                                                objectType("string"),
                                            )
                                    }
                                )
                                block += logCall

                                block +=
                                    newAssign(
                                        "=",
                                        listOf(newMemberAccess("a", newReference("sc"))),
                                        listOf(newLiteral(3, objectType("int"))),
                                    )

                                val dataflowRef = newReference("Dataflow", objectType("Dataflow"))
                                dataflowRef.refersTo = record
                                val outerCall =
                                    newMemberCall(
                                        newMemberAccess("highlyCriticalOperation", dataflowRef),
                                        false,
                                    )
                                outerCall.isStatic = true
                                val integerRef = newReference("Integer", objectType("Integer"))
                                val innerCall =
                                    newMemberCall(newMemberAccess("toString", integerRef), false)
                                innerCall.type = objectType("string")
                                innerCall.isStatic = true
                                innerCall.addArgument(newMemberAccess("a", newReference("sc")))
                                outerCall.addArgument(innerCall)
                                block += outerCall

                                block += newReturn()
                            }
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getArray(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("array.cpp")
                scopeManager.resetToGlobal(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val cDecl = newDeclarationStatement()
                            val c = newVariable("c", objectType("char").pointer())
                            val creationExpr = newArrayConstruction()
                            creationExpr.addDimension(newLiteral(4, objectType("int")))
                            creationExpr.type = objectType("char")
                            c.initializer = creationExpr
                            cDecl.declarations += c
                            scopeManager.addDeclaration(c)
                            block += cDecl

                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"))
                            a.initializer = newLiteral(4, objectType("int"))
                            aDecl.declarations += a
                            scopeManager.addDeclaration(a)
                            block += aDecl

                            val bDecl = newDeclarationStatement()
                            val b = newVariable("b", objectType("int"))
                            b.initializer =
                                newBinaryOperator("+").also {
                                    it.lhs = newReference("a")
                                    it.rhs = newLiteral(1, objectType("int"))
                                }
                            bDecl.declarations += b
                            scopeManager.addDeclaration(b)
                            block += bDecl

                            val dDecl = newDeclarationStatement()
                            val d = newVariable("d", objectType("char"))
                            d.initializer =
                                newSubscription().also {
                                    it.arrayExpression = newReference("c")
                                    it.subscriptExpression = newReference("b")
                                }
                            dDecl.declarations += d
                            scopeManager.addDeclaration(d)
                            block += dDecl

                            val ret = newReturn()
                            ret.returnValue = newLiteral(0, objectType("int"))
                            block += ret
                        }
                }

                // Fluent's `function()` init lambda receiver is `Function`, which is not a
                // `StatementHolder`; the `declare{}`/`returnStmt{}` calls below (which have no
                // enclosing `body{}` in the original) therefore fell through to the nearest
                // actual `StatementHolder` in the lexical chain -- the enclosing
                // `TranslationUnit` -- so they ended up attached to `tu.statements` instead of
                // `func.body` (which is never assigned). The declaration is still scoped to this
                // function though, since `function()` already entered its scope. Faithfully
                // reproduced here.
                newFunction("some_other_function", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("char"))
                    func.type = computeType(func)

                    val cDecl = newDeclarationStatement()
                    val c = newVariable("c", objectType("char").pointer())
                    val creationExpr = newArrayConstruction()
                    creationExpr.addDimension(newLiteral(100, objectType("int")))
                    creationExpr.type = objectType("char")
                    c.initializer = creationExpr
                    cDecl.declarations += c
                    scopeManager.addDeclaration(c)
                    tu.statements += cDecl

                    val ret = newReturn()
                    ret.returnValue =
                        newSubscription().also {
                            it.arrayExpression = newReference("c")
                            it.subscriptExpression = newLiteral(0, objectType("int"))
                        }
                    tu.statements += ret
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getArray2(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("array2.cpp")
                scopeManager.resetToGlobal(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val cDecl = newDeclarationStatement()
                            val c = newVariable("c", objectType("char").pointer())
                            val creationExpr = newArrayConstruction()
                            creationExpr.addDimension(newLiteral(4, objectType("int")))
                            creationExpr.type = objectType("char")
                            c.initializer = creationExpr
                            cDecl.declarations += c
                            scopeManager.addDeclaration(c)
                            block += cDecl

                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"))
                            a.initializer = newLiteral(0, objectType("int"))
                            aDecl.declarations += a
                            scopeManager.addDeclaration(a)
                            block += aDecl

                            val forNode = newFor { for_ ->
                                val iDecl = newDeclarationStatement()
                                val i = newVariable("i", objectType("int"))
                                i.initializer = newLiteral(0, objectType("int"))
                                iDecl.declarations += i
                                scopeManager.addDeclaration(i)
                                for_.initializerStatement = iDecl

                                for_.condition =
                                    newBinaryOperator("<").also {
                                        it.lhs = newReference("i")
                                        it.rhs = newLiteral(5, objectType("int"))
                                    }

                                for_.iterationStatement =
                                    newUnaryOperator("++", postfix = true, prefix = false).also {
                                        it.input = newReference("i")
                                    }

                                for_.statement = newBlock { loopBodyBlock ->
                                    loopBodyBlock +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("a")),
                                            listOf(
                                                newBinaryOperator("+").also {
                                                    it.lhs = newReference("a")
                                                    it.rhs =
                                                        newSubscription().also { sub ->
                                                            sub.arrayExpression = newReference("c")
                                                            sub.subscriptExpression =
                                                                newReference("i")
                                                        }
                                                }
                                            ),
                                        )
                                }
                            }
                            block += forNode

                            val ret = newReturn()
                            ret.returnValue = newReference("a")
                            block += ret
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getArray3(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("array3.cpp")
                scopeManager.resetToGlobal(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val cDecl = newDeclarationStatement()
                            val c = newVariable("c", objectType("char").pointer())
                            cDecl.declarations += c
                            scopeManager.addDeclaration(c)
                            block += cDecl

                            val ifElse = newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator(">").also {
                                        it.lhs = newLiteral(5, objectType("int"))
                                        it.rhs = newLiteral(4, objectType("int"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        val creationExpr = newArrayConstruction()
                                        creationExpr.addDimension(newLiteral(4, objectType("int")))
                                        creationExpr.type = objectType("char")
                                        thenBlock +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("c")),
                                                listOf(creationExpr),
                                            )
                                    }
                                ifElse.elseStatement =
                                    newBlock(enterScope = true) { elseBlock ->
                                        val creationExpr = newArrayConstruction()
                                        creationExpr.addDimension(newLiteral(5, objectType("int")))
                                        creationExpr.type = objectType("char")
                                        elseBlock +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("c")),
                                                listOf(creationExpr),
                                            )
                                    }
                            }
                            block += ifElse

                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"))
                            a.initializer = newLiteral(0, objectType("int"))
                            aDecl.declarations += a
                            scopeManager.addDeclaration(a)
                            block += aDecl

                            val forNode = newFor { for_ ->
                                val iDecl = newDeclarationStatement()
                                val i = newVariable("i", objectType("int"))
                                i.initializer = newLiteral(0, objectType("int"))
                                iDecl.declarations += i
                                scopeManager.addDeclaration(i)
                                for_.initializerStatement = iDecl

                                for_.condition =
                                    newBinaryOperator("<").also {
                                        it.lhs = newReference("i")
                                        it.rhs = newLiteral(5, objectType("int"))
                                    }

                                for_.iterationStatement =
                                    newUnaryOperator("++", postfix = true, prefix = false).also {
                                        it.input = newReference("i")
                                    }

                                for_.statement = newBlock { loopBodyBlock ->
                                    loopBodyBlock +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("a")),
                                            listOf(
                                                newBinaryOperator("+").also {
                                                    it.lhs = newReference("a")
                                                    it.rhs =
                                                        newSubscription().also { sub ->
                                                            sub.arrayExpression = newReference("c")
                                                            sub.subscriptExpression =
                                                                newReference("i")
                                                        }
                                                }
                                            ),
                                        )
                                }
                            }
                            block += forNode

                            val ret = newReturn()
                            ret.returnValue = newReference("a")
                            block += ret
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getArrayCorrect(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("array_correct.cpp")
                scopeManager.resetToGlobal(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val cDecl = newDeclarationStatement()
                            val c = newVariable("c", objectType("char").pointer())
                            val creationExpr = newArrayConstruction()
                            creationExpr.addDimension(newLiteral(4, objectType("int")))
                            creationExpr.type = objectType("char")
                            c.initializer = creationExpr
                            cDecl.declarations += c
                            scopeManager.addDeclaration(c)
                            block += cDecl

                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"))
                            a.initializer = newLiteral(0, objectType("int"))
                            aDecl.declarations += a
                            scopeManager.addDeclaration(a)
                            block += aDecl

                            val forNode = newFor { for_ ->
                                val iDecl = newDeclarationStatement()
                                val i = newVariable("i", objectType("int"))
                                i.initializer = newLiteral(0, objectType("int"))
                                iDecl.declarations += i
                                scopeManager.addDeclaration(i)
                                for_.initializerStatement = iDecl

                                for_.condition =
                                    newBinaryOperator("<").also {
                                        it.lhs = newReference("i")
                                        it.rhs = newLiteral(4, objectType("int"))
                                    }

                                for_.iterationStatement =
                                    newUnaryOperator("++", postfix = true, prefix = false).also {
                                        it.input = newReference("i")
                                    }

                                for_.statement = newBlock { loopBodyBlock ->
                                    loopBodyBlock +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("a")),
                                            listOf(
                                                newBinaryOperator("+").also {
                                                    it.lhs = newReference("a")
                                                    it.rhs =
                                                        newSubscription().also { sub ->
                                                            sub.arrayExpression = newReference("c")
                                                            sub.subscriptExpression =
                                                                newReference("i")
                                                        }
                                                }
                                            ),
                                        )
                                }
                            }
                            block += forNode

                            val ret = newReturn()
                            ret.returnValue = newReference("a")
                            block += ret
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getAssign(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("assign.cpp")
                scopeManager.resetToGlobal(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("int"))
                            a.initializer = newLiteral(4, objectType("int"))
                            aDecl.declarations += a
                            scopeManager.addDeclaration(a)
                            block += aDecl
                            // TODO: There was a commented-out line. No idea what to do with it:
                            // int a, b = 4; // this is broken, a is missing an initializer

                            block +=
                                newAssign(
                                    "=",
                                    listOf(newReference("a")),
                                    listOf(newLiteral(3, objectType("int"))),
                                )

                            val ret = newReturn()
                            ret.returnValue = newReference("a")
                            block += ret
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getDivBy0(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("assign.cpp")
                scopeManager.resetToGlobal(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val arrayDecl = newDeclarationStatement()
                            val array = newVariable("array", objectType("char").array())
                            array.initializer = newLiteral("hello", objectType("char").array())
                            arrayDecl.declarations += array
                            scopeManager.addDeclaration(array)
                            block += arrayDecl

                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("short"))
                            a.initializer = newLiteral(2, objectType("int"))
                            aDecl.declarations += a
                            scopeManager.addDeclaration(a)
                            block += aDecl

                            val ifElse = newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==").also {
                                        it.lhs = newReference("array")
                                        it.rhs = newLiteral("hello", objectType("string"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("a")),
                                                listOf(newLiteral(0, objectType("int"))),
                                            )
                                    }
                            }
                            block += ifElse

                            val xDecl = newDeclarationStatement()
                            val x = newVariable("x", objectType("double"))
                            x.initializer =
                                newBinaryOperator("/").also {
                                    it.lhs = newLiteral(5, objectType("int"))
                                    it.rhs = newReference("a")
                                }
                            xDecl.declarations += x
                            scopeManager.addDeclaration(x)
                            block += xDecl

                            val ret = newReturn()
                            ret.returnValue = newLiteral(0, objectType("int"))
                            block += ret
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        fun getVulnerable(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tu = newTranslationUnit("assign.cpp")
                scopeManager.resetToGlobal(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val arrayDecl = newDeclarationStatement()
                            val array = newVariable("array", objectType("char").array())
                            array.initializer = newLiteral("hello", objectType("char").array())
                            arrayDecl.declarations += array
                            scopeManager.addDeclaration(array)
                            block += arrayDecl

                            val memcpyCall = newCall(newReference("memcpy"))
                            memcpyCall.addArgument(newReference("array"))
                            memcpyCall.addArgument(
                                newLiteral("Hello world", objectType("char").array())
                            )
                            memcpyCall.addArgument(newLiteral(11, objectType("int")))
                            block += memcpyCall

                            val printfCall = newCall(newReference("printf"))
                            printfCall.addArgument(newReference("array"))
                            block += printfCall

                            val freeCall1 = newCall(newReference("free"))
                            freeCall1.addArgument(newReference("array"))
                            block += freeCall1

                            val freeCall2 = newCall(newReference("free"))
                            freeCall2.addArgument(newReference("array"))
                            block += freeCall2

                            val aDecl = newDeclarationStatement()
                            val a = newVariable("a", objectType("short"))
                            a.initializer = newLiteral(2, objectType("int"))
                            aDecl.declarations += a
                            scopeManager.addDeclaration(a)
                            block += aDecl

                            val ifElse = newIfElse { ifElse ->
                                ifElse.condition =
                                    newBinaryOperator("==").also {
                                        it.lhs = newReference("array")
                                        it.rhs = newLiteral("hello", objectType("string"))
                                    }
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("a")),
                                                listOf(newLiteral(1, objectType("int"))),
                                            )
                                    }
                            }
                            block += ifElse

                            val xDecl = newDeclarationStatement()
                            val x = newVariable("x", objectType("double"))
                            x.initializer =
                                newBinaryOperator("/").also {
                                    it.lhs = newLiteral(5, objectType("int"))
                                    it.rhs = newReference("a")
                                }
                            xDecl.declarations += x
                            scopeManager.addDeclaration(x)
                            block += xDecl

                            val bDecl = newDeclarationStatement()
                            val b = newVariable("b", objectType("int"))
                            b.initializer = newLiteral(2147483648, objectType("int"))
                            bDecl.declarations += b
                            scopeManager.addDeclaration(b)
                            block += bDecl

                            block +=
                                newAssign(
                                    "=",
                                    listOf(newReference("b")),
                                    listOf(newLiteral(2147483648, objectType("int"))),
                                )

                            val cDecl = newDeclarationStatement()
                            val c = newVariable("c", objectType("long"))
                            c.initializer = newLiteral(-10000, objectType("long"))
                            cDecl.declarations += c
                            scopeManager.addDeclaration(c)
                            block += cDecl

                            val ret = newReturn()
                            ret.returnValue = newLiteral(0, objectType("int"))
                            block += ret
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
    }
}
