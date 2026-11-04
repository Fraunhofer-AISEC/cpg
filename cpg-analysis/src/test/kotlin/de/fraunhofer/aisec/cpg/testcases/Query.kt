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
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
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
                singleTranslationUnit("Dataflow.java") { tu ->
                    newRecord("Dataflow", "class", holder = tu, enterScope = true) { record ->
                        newField("attr", objectType("string"), holder = record)

                        newMethod("toString", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(objectType("string"))
                            method.type = computeType(method)

                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newReturn { ret ->
                                        ret.returnValue =
                                            newBinaryOperator("+") {
                                                it.lhs =
                                                    newLiteral(
                                                        "Dataflow: attr=",
                                                        objectType("string"),
                                                    )
                                                it.rhs = newReference("attr")
                                            }
                                    }
                                }
                        }

                        newMethod("test", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(objectType("string"))
                            method.type = computeType(method)

                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newReturn {
                                        it.returnValue = newLiteral("abcd", objectType("string"))
                                    }
                                }
                        }

                        newMethod("print", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(incompleteType())
                            method.type = computeType(method)

                            newParameter("s", objectType("string"), holder = method)

                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements +=
                                        newMemberCall(
                                            newMemberAccess(
                                                "println",
                                                newMemberAccess("out", newReference("System")),
                                            ),
                                            false,
                                        ) {
                                            it.arguments += newReference("s")
                                        }

                                    block.statements += newReturn()
                                }
                        }

                        newMethod("main", holder = record, enterScope = true) { method ->
                            method.isStatic = true
                            method.returnTypes = listOf(incompleteType())
                            method.type = computeType(method)

                            newParameter("args", objectType("string").array(), holder = method)

                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newDeclarationStatement { scDecl ->
                                        val newExpr = newNew()
                                        newExpr.initializer =
                                            newConstruction("Dataflow") {
                                                it.type = objectType("Dataflow")
                                            }
                                        newVariable("sc", objectType("Dataflow"), holder = scDecl) {
                                            it.initializer = newExpr
                                        }
                                    }

                                    block.statements += newDeclarationStatement { sDecl ->
                                        newVariable("s", objectType("string"), holder = sDecl) {
                                            it.initializer =
                                                newMemberCall(
                                                    newMemberAccess("toString", newReference("sc")),
                                                    false,
                                                )
                                        }
                                    }

                                    block.statements +=
                                        newMemberCall(
                                            newMemberAccess("print", newReference("sc")),
                                            false,
                                        ) {
                                            it.arguments += newReference("s")
                                        }

                                    block.statements +=
                                        newMemberCall(
                                            newMemberAccess("print", newReference("sc")),
                                            false,
                                        ) {
                                            it.arguments +=
                                                newMemberCall(
                                                    newMemberAccess("test", newReference("sc")),
                                                    false,
                                                )
                                        }
                                }
                        }
                    }
                }
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
                singleTranslationUnit("ComplexDataflow.java") { tu ->
                    newRecord("Dataflow", "class", holder = tu, enterScope = true) { record ->
                        // TODO: this field is static. How do we model this?
                        newField("logger", objectType("Logger"), holder = record) { field ->
                            field.modifiers = setOf("static")
                            field.initializer =
                                newMemberCall(
                                    newMemberAccess("getLogger", newReference("Logger")),
                                    false,
                                ) {
                                    it.arguments +=
                                        newLiteral("DataflowLogger", objectType("string"))
                                }
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
                                    block.statements +=
                                        newMemberCall(
                                            newMemberAccess(
                                                "println",
                                                newMemberAccess("out", newReference("System")),
                                            ),
                                            false,
                                        ) {
                                            it.arguments += newReference("s")
                                        }

                                    block.statements += newReturn()
                                }
                        }

                        newMethod("main", holder = record, enterScope = true) { method ->
                            method.isStatic = true
                            method.returnTypes = listOf(incompleteType())
                            method.type = computeType(method)

                            newParameter("args", objectType("string").array(), holder = method)

                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newDeclarationStatement { scDecl ->
                                        val newExpr = newNew()
                                        newExpr.initializer =
                                            newConstruction("Dataflow") {
                                                it.type = objectType("Dataflow")
                                            }
                                        newVariable("sc", objectType("Dataflow"), holder = scDecl) {
                                            it.initializer = newExpr
                                        }
                                    }

                                    block.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newMemberAccess("a", newReference("sc"))),
                                            listOf(newLiteral(5, objectType("int"))),
                                        )

                                    val dataflowRef =
                                        newReference("Dataflow", objectType("Dataflow"))
                                    dataflowRef.refersTo = record
                                    block.statements +=
                                        newMemberCall(
                                            newMemberAccess("highlyCriticalOperation", dataflowRef),
                                            false,
                                        ) { outerCall ->
                                            outerCall.isStatic = true
                                            outerCall.arguments +=
                                                newMemberCall(
                                                    newMemberAccess(
                                                        "toString",
                                                        newReference(
                                                            "Integer",
                                                            objectType("Integer"),
                                                        ),
                                                    ),
                                                    false,
                                                ) { innerCall ->
                                                    innerCall.type = objectType("string")
                                                    innerCall.isStatic = true
                                                    innerCall.arguments +=
                                                        newMemberAccess("a", newReference("sc"))
                                                }
                                        }

                                    block.statements +=
                                        newMemberCall(
                                            newMemberAccess("log", newReference("logger")),
                                            false,
                                        ) { logCall ->
                                            logCall.arguments +=
                                                newMemberAccess("INFO", newReference("Level"))
                                            logCall.arguments +=
                                                newBinaryOperator("+") { outer ->
                                                    outer.lhs =
                                                        newBinaryOperator("+") { inner ->
                                                            inner.lhs =
                                                                newLiteral(
                                                                    "put ",
                                                                    objectType("string"),
                                                                )
                                                            inner.rhs =
                                                                newMemberAccess(
                                                                    "a",
                                                                    newReference("sc"),
                                                                )
                                                        }
                                                    outer.rhs =
                                                        newLiteral(
                                                            " into highlyCriticalOperation()",
                                                            objectType("string"),
                                                        )
                                                }
                                        }

                                    block.statements += newReturn()
                                }
                        }
                    }
                }
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
                singleTranslationUnit("ComplexDataflow2.java") { tu ->
                    newRecord("Dataflow", "class", holder = tu, enterScope = true) { record ->
                        // TODO: this field is static. How do we model this?
                        newField("logger", objectType("Logger"), holder = record) { field ->
                            field.modifiers = setOf("static")
                            field.initializer =
                                newMemberCall(
                                    newMemberAccess("getLogger", newReference("Logger")),
                                    false,
                                ) {
                                    it.arguments +=
                                        newLiteral("DataflowLogger", objectType("string"))
                                }
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
                                    block.statements +=
                                        newMemberCall(
                                            newMemberAccess(
                                                "println",
                                                newMemberAccess("out", newReference("System")),
                                            ),
                                            false,
                                        ) {
                                            it.arguments += newReference("s")
                                        }

                                    block.statements += newReturn()
                                }
                        }

                        newMethod("main", holder = record, enterScope = true) { method ->
                            method.isStatic = true
                            method.returnTypes = listOf(incompleteType())
                            method.type = computeType(method)

                            newParameter("args", objectType("string").array(), holder = method)

                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newDeclarationStatement { scDecl ->
                                        val newExpr = newNew()
                                        newExpr.initializer =
                                            newConstruction("Dataflow") {
                                                it.type = objectType("Dataflow")
                                            }
                                        newVariable("sc", objectType("Dataflow"), holder = scDecl) {
                                            it.initializer = newExpr
                                        }
                                    }

                                    block.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newMemberAccess("a", newReference("sc"))),
                                            listOf(newLiteral(5, objectType("int"))),
                                        )

                                    block.statements +=
                                        newMemberCall(
                                            newMemberAccess("log", newReference("logger")),
                                            false,
                                        ) { logCall ->
                                            logCall.arguments +=
                                                newMemberAccess("INFO", newReference("Level"))
                                            logCall.arguments +=
                                                newBinaryOperator("+") { outer ->
                                                    outer.lhs =
                                                        newBinaryOperator("+") { inner ->
                                                            inner.lhs =
                                                                newLiteral(
                                                                    "put ",
                                                                    objectType("string"),
                                                                )
                                                            inner.rhs =
                                                                newMemberAccess(
                                                                    "a",
                                                                    newReference("sc"),
                                                                )
                                                        }
                                                    outer.rhs =
                                                        newLiteral(
                                                            " into highlyCriticalOperation()",
                                                            objectType("string"),
                                                        )
                                                }
                                        }

                                    val dataflowRef =
                                        newReference("Dataflow", objectType("Dataflow"))
                                    dataflowRef.refersTo = record
                                    block.statements +=
                                        newMemberCall(
                                            newMemberAccess("highlyCriticalOperation", dataflowRef),
                                            false,
                                        ) { outerCall ->
                                            outerCall.isStatic = true
                                            outerCall.arguments +=
                                                newMemberCall(
                                                    newMemberAccess(
                                                        "toString",
                                                        newReference(
                                                            "Integer",
                                                            objectType("Integer"),
                                                        ),
                                                    ),
                                                    false,
                                                ) { innerCall ->
                                                    innerCall.type = objectType("string")
                                                    innerCall.isStatic = true
                                                    innerCall.arguments +=
                                                        newMemberAccess("a", newReference("sc"))
                                                }
                                        }

                                    block.statements += newReturn()
                                }
                        }
                    }
                }
            }

        fun getComplexDataflow3(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("ComplexDataflow3.java") { tu ->
                    newRecord("Dataflow", "class", holder = tu, enterScope = true) { record ->
                        // TODO: this field is static. How do we model this?
                        newField("logger", objectType("Logger"), holder = record) { field ->
                            field.modifiers = setOf("static")
                            field.initializer =
                                newMemberCall(
                                    newMemberAccess("getLogger", newReference("Logger")),
                                    false,
                                ) {
                                    it.arguments +=
                                        newLiteral("DataflowLogger", objectType("string"))
                                }
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
                                    block.statements +=
                                        newMemberCall(
                                            newMemberAccess(
                                                "println",
                                                newMemberAccess("out", newReference("System")),
                                            ),
                                            false,
                                        ) {
                                            it.arguments += newReference("s")
                                        }

                                    block.statements += newReturn()
                                }
                        }

                        newMethod("main", holder = record, enterScope = true) { method ->
                            method.isStatic = true
                            method.returnTypes = listOf(incompleteType())
                            method.type = computeType(method)

                            newParameter("args", objectType("string").array(), holder = method)

                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newDeclarationStatement { scDecl ->
                                        val newExpr = newNew()
                                        newExpr.initializer =
                                            newConstruction("Dataflow") {
                                                it.type = objectType("Dataflow")
                                            }
                                        newVariable("sc", objectType("Dataflow"), holder = scDecl) {
                                            it.initializer = newExpr
                                        }
                                    }

                                    block.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newMemberAccess("a", newReference("sc"))),
                                            listOf(newLiteral(5, objectType("int"))),
                                        )

                                    block.statements +=
                                        newMemberCall(
                                            newMemberAccess("log", newReference("logger")),
                                            false,
                                        ) { logCall ->
                                            logCall.arguments +=
                                                newMemberAccess("INFO", newReference("Level"))
                                            logCall.arguments +=
                                                newBinaryOperator("+") { outer ->
                                                    outer.lhs =
                                                        newBinaryOperator("+") { inner ->
                                                            inner.lhs =
                                                                newLiteral(
                                                                    "put ",
                                                                    objectType("string"),
                                                                )
                                                            // The base of this member access is
                                                            // intentionally the "a" reference (not
                                                            // "sc"), as checked by
                                                            // testComplexDataflow3.
                                                            inner.rhs =
                                                                newMemberAccess(
                                                                    "a",
                                                                    newReference("a"),
                                                                )
                                                        }
                                                    outer.rhs =
                                                        newLiteral(
                                                            " into highlyCriticalOperation()",
                                                            objectType("string"),
                                                        )
                                                }
                                        }

                                    block.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newMemberAccess("a", newReference("sc"))),
                                            listOf(newLiteral(3, objectType("int"))),
                                        )

                                    val dataflowRef =
                                        newReference("Dataflow", objectType("Dataflow"))
                                    dataflowRef.refersTo = record
                                    block.statements +=
                                        newMemberCall(
                                            newMemberAccess("highlyCriticalOperation", dataflowRef),
                                            false,
                                        ) { outerCall ->
                                            outerCall.isStatic = true
                                            outerCall.arguments +=
                                                newMemberCall(
                                                    newMemberAccess(
                                                        "toString",
                                                        newReference(
                                                            "Integer",
                                                            objectType("Integer"),
                                                        ),
                                                    ),
                                                    false,
                                                ) { innerCall ->
                                                    innerCall.type = objectType("string")
                                                    innerCall.isStatic = true
                                                    innerCall.arguments +=
                                                        newMemberAccess("a", newReference("sc"))
                                                }
                                        }

                                    block.statements += newReturn()
                                }
                        }
                    }
                }
            }

        fun getArray(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("array.cpp") { tu ->
                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)

                        func.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { cDecl ->
                                    val creationExpr = newArrayConstruction()
                                    creationExpr.addDimension(newLiteral(4, objectType("int")))
                                    creationExpr.type = objectType("char")
                                    newVariable("c", objectType("char").pointer(), holder = cDecl) {
                                        it.initializer = creationExpr
                                    }
                                }

                                block.statements += newDeclarationStatement { aDecl ->
                                    newVariable("a", objectType("int"), holder = aDecl) {
                                        it.initializer = newLiteral(4, objectType("int"))
                                    }
                                }

                                block.statements += newDeclarationStatement { bDecl ->
                                    newVariable("b", objectType("int"), holder = bDecl) { b ->
                                        b.initializer =
                                            newBinaryOperator("+") {
                                                it.lhs = newReference("a")
                                                it.rhs = newLiteral(1, objectType("int"))
                                            }
                                    }
                                }

                                block.statements += newDeclarationStatement { dDecl ->
                                    newVariable("d", objectType("char"), holder = dDecl) { d ->
                                        d.initializer = newSubscription {
                                            it.arrayExpression = newReference("c")
                                            it.subscriptExpression = newReference("b")
                                        }
                                    }
                                }

                                block.statements += newReturn {
                                    it.returnValue = newLiteral(0, objectType("int"))
                                }
                            }
                    }

                    newFunction("some_other_function", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("char"))
                        func.type = computeType(func)

                        func.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { cDecl ->
                                    val creationExpr = newArrayConstruction()
                                    creationExpr.addDimension(newLiteral(100, objectType("int")))
                                    creationExpr.type = objectType("char")
                                    newVariable("c", objectType("char").pointer(), holder = cDecl) {
                                        it.initializer = creationExpr
                                    }
                                }

                                block.statements += newReturn { ret ->
                                    ret.returnValue = newSubscription {
                                        it.arrayExpression = newReference("c")
                                        it.subscriptExpression = newLiteral(0, objectType("int"))
                                    }
                                }
                            }
                    }
                }
            }

        fun getArray2(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("array2.cpp") { tu ->
                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)

                        func.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { cDecl ->
                                    val creationExpr = newArrayConstruction()
                                    creationExpr.addDimension(newLiteral(4, objectType("int")))
                                    creationExpr.type = objectType("char")
                                    newVariable("c", objectType("char").pointer(), holder = cDecl) {
                                        it.initializer = creationExpr
                                    }
                                }

                                block.statements += newDeclarationStatement { aDecl ->
                                    newVariable("a", objectType("int"), holder = aDecl) {
                                        it.initializer = newLiteral(0, objectType("int"))
                                    }
                                }

                                block.statements += newFor { for_ ->
                                    for_.initializerStatement = newDeclarationStatement { iDecl ->
                                        newVariable("i", objectType("int"), holder = iDecl) {
                                            it.initializer = newLiteral(0, objectType("int"))
                                        }
                                    }

                                    for_.condition =
                                        newBinaryOperator("<") {
                                            it.lhs = newReference("i")
                                            it.rhs = newLiteral(5, objectType("int"))
                                        }

                                    for_.iterationStatement =
                                        newUnaryOperator("++", postfix = true, prefix = false) {
                                            it.input = newReference("i")
                                        }

                                    for_.statement = newBlock { loopBodyBlock ->
                                        loopBodyBlock.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("a")),
                                                listOf(
                                                    newBinaryOperator("+") {
                                                        it.lhs = newReference("a")
                                                        it.rhs = newSubscription { sub ->
                                                            sub.arrayExpression = newReference("c")
                                                            sub.subscriptExpression =
                                                                newReference("i")
                                                        }
                                                    }
                                                ),
                                            )
                                    }
                                }

                                block.statements += newReturn { it.returnValue = newReference("a") }
                            }
                    }
                }
            }

        fun getArray3(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("array3.cpp") { tu ->
                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)

                        func.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { cDecl ->
                                    newVariable("c", objectType("char").pointer(), holder = cDecl)
                                }

                                block.statements += newIfElse { ifElse ->
                                    ifElse.condition =
                                        newBinaryOperator(">") {
                                            it.lhs = newLiteral(5, objectType("int"))
                                            it.rhs = newLiteral(4, objectType("int"))
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            val creationExpr = newArrayConstruction()
                                            creationExpr.addDimension(
                                                newLiteral(4, objectType("int"))
                                            )
                                            creationExpr.type = objectType("char")
                                            thenBlock.statements +=
                                                newAssign(
                                                    "=",
                                                    listOf(newReference("c")),
                                                    listOf(creationExpr),
                                                )
                                        }
                                    ifElse.elseStatement =
                                        newBlock(enterScope = true) { elseBlock ->
                                            val creationExpr = newArrayConstruction()
                                            creationExpr.addDimension(
                                                newLiteral(5, objectType("int"))
                                            )
                                            creationExpr.type = objectType("char")
                                            elseBlock.statements +=
                                                newAssign(
                                                    "=",
                                                    listOf(newReference("c")),
                                                    listOf(creationExpr),
                                                )
                                        }
                                }

                                block.statements += newDeclarationStatement { aDecl ->
                                    newVariable("a", objectType("int"), holder = aDecl) {
                                        it.initializer = newLiteral(0, objectType("int"))
                                    }
                                }

                                block.statements += newFor { for_ ->
                                    for_.initializerStatement = newDeclarationStatement { iDecl ->
                                        newVariable("i", objectType("int"), holder = iDecl) {
                                            it.initializer = newLiteral(0, objectType("int"))
                                        }
                                    }

                                    for_.condition =
                                        newBinaryOperator("<") {
                                            it.lhs = newReference("i")
                                            it.rhs = newLiteral(5, objectType("int"))
                                        }

                                    for_.iterationStatement =
                                        newUnaryOperator("++", postfix = true, prefix = false) {
                                            it.input = newReference("i")
                                        }

                                    for_.statement = newBlock { loopBodyBlock ->
                                        loopBodyBlock.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("a")),
                                                listOf(
                                                    newBinaryOperator("+") {
                                                        it.lhs = newReference("a")
                                                        it.rhs = newSubscription { sub ->
                                                            sub.arrayExpression = newReference("c")
                                                            sub.subscriptExpression =
                                                                newReference("i")
                                                        }
                                                    }
                                                ),
                                            )
                                    }
                                }

                                block.statements += newReturn { it.returnValue = newReference("a") }
                            }
                    }
                }
            }

        fun getArrayCorrect(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("array_correct.cpp") { tu ->
                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)

                        func.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { cDecl ->
                                    val creationExpr = newArrayConstruction()
                                    creationExpr.addDimension(newLiteral(4, objectType("int")))
                                    creationExpr.type = objectType("char")
                                    newVariable("c", objectType("char").pointer(), holder = cDecl) {
                                        it.initializer = creationExpr
                                    }
                                }

                                block.statements += newDeclarationStatement { aDecl ->
                                    newVariable("a", objectType("int"), holder = aDecl) {
                                        it.initializer = newLiteral(0, objectType("int"))
                                    }
                                }

                                block.statements += newFor { for_ ->
                                    for_.initializerStatement = newDeclarationStatement { iDecl ->
                                        newVariable("i", objectType("int"), holder = iDecl) {
                                            it.initializer = newLiteral(0, objectType("int"))
                                        }
                                    }

                                    for_.condition =
                                        newBinaryOperator("<") {
                                            it.lhs = newReference("i")
                                            it.rhs = newLiteral(4, objectType("int"))
                                        }

                                    for_.iterationStatement =
                                        newUnaryOperator("++", postfix = true, prefix = false) {
                                            it.input = newReference("i")
                                        }

                                    for_.statement = newBlock { loopBodyBlock ->
                                        loopBodyBlock.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("a")),
                                                listOf(
                                                    newBinaryOperator("+") {
                                                        it.lhs = newReference("a")
                                                        it.rhs = newSubscription { sub ->
                                                            sub.arrayExpression = newReference("c")
                                                            sub.subscriptExpression =
                                                                newReference("i")
                                                        }
                                                    }
                                                ),
                                            )
                                    }
                                }

                                block.statements += newReturn { it.returnValue = newReference("a") }
                            }
                    }
                }
            }

        fun getAssign(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("assign.cpp") { tu ->
                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)

                        func.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { aDecl ->
                                    newVariable("a", objectType("int"), holder = aDecl) {
                                        it.initializer = newLiteral(4, objectType("int"))
                                    }
                                }
                                // TODO: There was a commented-out line. No idea what to do with it:
                                // int a, b = 4; // this is broken, a is missing an initializer

                                block.statements +=
                                    newAssign(
                                        "=",
                                        listOf(newReference("a")),
                                        listOf(newLiteral(3, objectType("int"))),
                                    )

                                block.statements += newReturn { it.returnValue = newReference("a") }
                            }
                    }
                }
            }

        fun getDivBy0(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("assign.cpp") { tu ->
                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)

                        func.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { arrayDecl ->
                                    newVariable(
                                        "array",
                                        objectType("char").array(),
                                        holder = arrayDecl,
                                    ) {
                                        it.initializer =
                                            newLiteral("hello", objectType("char").array())
                                    }
                                }

                                block.statements += newDeclarationStatement { aDecl ->
                                    newVariable("a", objectType("short"), holder = aDecl) {
                                        it.initializer = newLiteral(2, objectType("int"))
                                    }
                                }

                                block.statements += newIfElse { ifElse ->
                                    ifElse.condition =
                                        newBinaryOperator("==") {
                                            it.lhs = newReference("array")
                                            it.rhs = newLiteral("hello", objectType("string"))
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock.statements +=
                                                newAssign(
                                                    "=",
                                                    listOf(newReference("a")),
                                                    listOf(newLiteral(0, objectType("int"))),
                                                )
                                        }
                                }

                                block.statements += newDeclarationStatement { xDecl ->
                                    newVariable("x", objectType("double"), holder = xDecl) { x ->
                                        x.initializer =
                                            newBinaryOperator("/") {
                                                it.lhs = newLiteral(5, objectType("int"))
                                                it.rhs = newReference("a")
                                            }
                                    }
                                }

                                block.statements += newReturn {
                                    it.returnValue = newLiteral(0, objectType("int"))
                                }
                            }
                    }
                }
            }

        fun getVulnerable(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("assign.cpp") { tu ->
                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)

                        func.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { arrayDecl ->
                                    newVariable(
                                        "array",
                                        objectType("char").array(),
                                        holder = arrayDecl,
                                    ) {
                                        it.initializer =
                                            newLiteral("hello", objectType("char").array())
                                    }
                                }

                                block.statements +=
                                    newCall(newReference("memcpy")) {
                                        it.arguments += newReference("array")
                                        it.arguments +=
                                            newLiteral("Hello world", objectType("char").array())
                                        it.arguments += newLiteral(11, objectType("int"))
                                    }

                                block.statements +=
                                    newCall(newReference("printf")) {
                                        it.arguments += newReference("array")
                                    }

                                block.statements +=
                                    newCall(newReference("free")) {
                                        it.arguments += newReference("array")
                                    }

                                block.statements +=
                                    newCall(newReference("free")) {
                                        it.arguments += newReference("array")
                                    }

                                block.statements += newDeclarationStatement { aDecl ->
                                    newVariable("a", objectType("short"), holder = aDecl) {
                                        it.initializer = newLiteral(2, objectType("int"))
                                    }
                                }

                                block.statements += newIfElse { ifElse ->
                                    ifElse.condition =
                                        newBinaryOperator("==") {
                                            it.lhs = newReference("array")
                                            it.rhs = newLiteral("hello", objectType("string"))
                                        }
                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            thenBlock.statements +=
                                                newAssign(
                                                    "=",
                                                    listOf(newReference("a")),
                                                    listOf(newLiteral(1, objectType("int"))),
                                                )
                                        }
                                }

                                block.statements += newDeclarationStatement { xDecl ->
                                    newVariable("x", objectType("double"), holder = xDecl) { x ->
                                        x.initializer =
                                            newBinaryOperator("/") {
                                                it.lhs = newLiteral(5, objectType("int"))
                                                it.rhs = newReference("a")
                                            }
                                    }
                                }

                                block.statements += newDeclarationStatement { bDecl ->
                                    newVariable("b", objectType("int"), holder = bDecl) {
                                        it.initializer = newLiteral(2147483648, objectType("int"))
                                    }
                                }

                                block.statements +=
                                    newAssign(
                                        "=",
                                        listOf(newReference("b")),
                                        listOf(newLiteral(2147483648, objectType("int"))),
                                    )

                                block.statements += newDeclarationStatement { cDecl ->
                                    newVariable("c", objectType("long"), holder = cDecl) {
                                        it.initializer = newLiteral(-10000, objectType("long"))
                                    }
                                }

                                block.statements += newReturn {
                                    it.returnValue = newLiteral(0, objectType("int"))
                                }
                            }
                    }
                }
            }
    }
}
