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
package de.fraunhofer.aisec.cpg.test

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.ClassTestLanguage
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.StructTestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI

/**
 * Builds a chain of [MemberAccess]/[de.fraunhofer.aisec.cpg.graph.expressions.Reference] nodes for
 * a (possibly qualified) [name], optionally setting [type] on the innermost node.
 */
private fun LanguageFrontend<*, *>.memberOrRef(name: Name, type: Type? = null): Expression {
    val node =
        if (name.parent != null) {
            newMemberAccess(name.localName, memberOrRef(name.parent))
        } else {
            newReference(name.localName)
        }
    if (type != null) {
        node.type = type
    }
    return node
}

/**
 * Builds a [Call] (or a member call if [name] is a dotted/qualified name) with the given [name] as
 * callee.
 */
private fun LanguageFrontend<*, *>.dottedCall(name: CharSequence, isStatic: Boolean = false): Call {
    val parsedName = parseName(name)
    return if (parsedName.parent != null) {
        newMemberCall(
            newMemberAccess(parsedName.localName, memberOrRef(parsedName.parent)),
            isStatic,
        )
    } else {
        newCall(newReference(parsedName))
    }
}

/**
 * Builds a [MemberAccess] with the given [name]. If [base] is `null`, an implicit `this` reference
 * is used, whose type is resolved to the nearest enclosing record's type, or left as [unknownType]
 * if none is found.
 */
private fun LanguageFrontend<*, *>.newMember(
    name: CharSequence,
    base: Expression? = null,
    operatorCode: String = ".",
): MemberAccess {
    val parsedName = parseName(name)
    val type =
        if (parsedName.parent != null) {
            null
        } else {
            var scope: de.fraunhofer.aisec.cpg.graph.scopes.Scope? = scopeManager.currentScope
            while (scope != null && scope !is RecordScope) {
                scope = scope.parent
            }
            scope?.name?.let { objectType(it) }
        }
    val memberBase = base ?: memberOrRef(parsedName.parent ?: parseName("this"), type)
    return newMemberAccess(name, memberBase, operatorCode = operatorCode)
}

/** Stamps a fake [PhysicalLocation] on the receiver for line [i], using its name as the code. */
private fun Expression.line(tuName: String, i: Int): Expression {
    val code = this.name
    val region = Region(i, 0, i, code.length)
    this.location = PhysicalLocation(URI(tuName), region)
    return this
}

/**
 * Appends the recurring "if (true) break;" statement followed by a `postIf()` call to [block], the
 * shared loop body used by several of the `...WithElseAndBreak` test cases below.
 */
private fun LanguageFrontend<*, *>.addIfTrueBreakAndPostIf(block: Block) {
    block.statements += newIfElse {
        it.condition = newLiteral(true, objectType("bool"))
        it.thenStatement = newBlock(enterScope = true) { it.statements += newBreak() }
    }
    block.statements += newCall(newReference("postIf"))
}

/** Builds the recurring loop `else` block containing a single `elseCall()` call. */
private fun LanguageFrontend<*, *>.elseCallBlock(): Block =
    newBlock(enterScope = true) { it.statements += newCall(newReference("elseCall")) }

/** Builds the recurring `System.out.println(argument)` member call used below. */
private fun LanguageFrontend<*, *>.printlnCall(argument: Expression): MemberCall =
    newMemberCall(
        newMemberAccess(
            "println",
            newMember("out", newReference("System").also { it.isStaticAccess = true }),
        )
    ) {
        it.arguments += argument
    }

/**
 * Creates a [Variable] named [name] of [type], registers it, and sets it as [method]'s receiver.
 */
private fun LanguageFrontend<*, *>.addReceiver(method: Method, name: String, type: Type): Variable {
    val node = newVariable(name, type)
    method.receiver = node
    scopeManager.addDeclaration(node)
    return node
}

/**
 * Declares a single [Variable] named [name] of [type] (optionally initialized via [init]) inside a
 * new declaration statement appended to [block].
 */
private fun LanguageFrontend<*, *>.declareVariable(
    block: Block,
    name: CharSequence,
    type: Type = unknownType(),
    init: ((Variable) -> Unit)? = null,
): Variable {
    val declStmt = newDeclarationStatement()
    val v = newVariable(name, type, holder = declStmt, init = init)
    block.statements += declStmt
    return v
}

class GraphExamples {
    companion object {
        fun getInitializerListExprDFG(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("initializerListExprDFG.cpp") { tu ->
                    newFunction("foo", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)
                        func.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newReturn {
                                    it.returnValue = newLiteral(0, objectType("int"))
                                }
                            }
                    }
                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)
                        func.body =
                            newBlock(enterScope = true) { block ->
                                declareVariable(block, "i", objectType("int")) { v ->
                                    v.initializer =
                                        newInitializerList().also {
                                            it.initializers = mutableListOf(dottedCall("foo"))
                                        }
                                }
                                block.statements += newReturn { it.returnValue = newReference("i") }
                            }
                    }
                }
            }

        fun getWhileWithElseAndBreak(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("whileWithBreakAndElse.py") { tu ->
                    newRecord("someRecord", "class", holder = tu, enterScope = true) { record ->
                        newMethod("func", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements +=
                                        newWhile(enterScope = true) { w1 ->
                                            w1.condition = newLiteral(true, objectType("bool"))
                                            w1.statement =
                                                newBlock().also { addIfTrueBreakAndPostIf(it) }
                                            w1.elseStatement = elseCallBlock()
                                        }

                                    block.statements += dottedCall("postWhile")

                                    block.statements +=
                                        newWhile(enterScope = true) { w2 ->
                                            w2.condition = newLiteral(true, objectType("bool"))
                                            w2.statement =
                                                newBlock().also { addIfTrueBreakAndPostIf(it) }
                                            w2.elseStatement = elseCallBlock()
                                        }
                                }
                        }
                    }
                }
            }

        fun getDoWithElseAndBreak(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("whileWithBreakAndElse.py") { tu ->
                    newRecord("someRecord", "class", holder = tu, enterScope = true) { record ->
                        newMethod("func", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements +=
                                        newDoWhile(enterScope = true) { d1 ->
                                            d1.condition = newLiteral(true, objectType("bool"))
                                            d1.statement =
                                                newBlock().also { addIfTrueBreakAndPostIf(it) }
                                            d1.elseStatement = elseCallBlock()
                                        }

                                    block.statements += dottedCall("postDo")

                                    block.statements +=
                                        newDoWhile(enterScope = true) { d2 ->
                                            d2.condition = newLiteral(true, objectType("bool"))
                                            d2.statement =
                                                newBlock().also { addIfTrueBreakAndPostIf(it) }
                                            d2.elseStatement = elseCallBlock()
                                        }
                                }
                        }
                    }
                }
            }

        fun getForWithElseAndBreak(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("whileWithBreakAndElse.py") { tu ->
                    newRecord("someRecord", "class", holder = tu, enterScope = true) { record ->
                        newMethod("func", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    fun buildFor(): de.fraunhofer.aisec.cpg.graph.expressions.For =
                                        newFor { forNode ->
                                            forNode.statement =
                                                newBlock().also { addIfTrueBreakAndPostIf(it) }
                                            forNode.initializerStatement =
                                                newDeclarationStatement { declStmt ->
                                                    newVariable(
                                                        "a",
                                                        objectType("int"),
                                                        holder = declStmt,
                                                    ) {
                                                        it.initializer =
                                                            newLiteral(0, objectType("int"))
                                                    }
                                                }
                                            forNode.condition = newLiteral(true, objectType("bool"))
                                            forNode.iterationStatement =
                                                newUnaryOperator("++", true, false) {
                                                    it.input = newReference("a")
                                                }
                                            forNode.elseStatement = elseCallBlock()
                                        }

                                    block.statements += buildFor()
                                    block.statements += dottedCall("postFor")
                                    block.statements += buildFor()
                                }
                        }
                    }
                }
            }

        fun getForEachWithElseAndBreak(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("whileWithBreakAndElse.py") { tu ->
                    newRecord("someRecord", "class", holder = tu, enterScope = true) { record ->
                        newMethod("func", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    fun buildForEach():
                                        de.fraunhofer.aisec.cpg.graph.expressions.ForEach =
                                        newForEach { forEach ->
                                            forEach.iterable = dottedCall("listOf")
                                            forEach.variable = newDeclarationStatement { declStmt ->
                                                newVariable("a", unknownType(), holder = declStmt)
                                            }
                                            forEach.statement =
                                                newBlock().also { addIfTrueBreakAndPostIf(it) }
                                            forEach.elseStatement = elseCallBlock()
                                        }

                                    block.statements += buildForEach()
                                    block.statements += dottedCall("postForEach")
                                    block.statements += buildForEach()
                                }
                        }
                    }
                }
            }

        fun getStatementsAsExpressions(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("statementsAsExpressions.py") { tu ->
                    newRecord("someRecord", "class", holder = tu, enterScope = true) { record ->
                        newMethod("func", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newForEach { forEach1 ->
                                        forEach1.usedAsExpression = true
                                        forEach1.iterable = dottedCall("listOf")
                                        forEach1.variable = newDeclarationStatement { declStmt ->
                                            newVariable("a", unknownType(), holder = declStmt)
                                        }
                                        forEach1.statement =
                                            newBlock().also {
                                                it.statements += dottedCall("inBody")
                                            }
                                        forEach1.elseStatement =
                                            newBlock(enterScope = true) {
                                                it.statements += dottedCall("inElse")
                                            }
                                    }

                                    val forNode = newFor {
                                        it.usedAsExpression = true
                                        it.statement =
                                            newBlock().also { blk ->
                                                blk.statements += dottedCall("bodyCall")
                                            }
                                        it.initializerStatement =
                                            newDeclarationStatement { declStmt ->
                                                newVariable(
                                                    "a",
                                                    objectType("int"),
                                                    holder = declStmt,
                                                ) { v ->
                                                    v.initializer = newLiteral(0, objectType("int"))
                                                }
                                            }
                                        it.condition = newLiteral(true, objectType("bool"))
                                        it.iterationStatement =
                                            newUnaryOperator("++", true, false) { u ->
                                                u.input = newReference("a")
                                            }
                                        it.elseStatement = elseCallBlock()
                                    }

                                    declareVariable(block, "a", unknownType()) { v ->
                                        v.initializer =
                                            newBinaryOperator("+") {
                                                it.lhs = newLiteral(1, objectType("int"))
                                                it.rhs = forNode
                                            }
                                    }

                                    block.statements +=
                                        newDoWhile(enterScope = true) { doNode ->
                                            doNode.usedAsExpression = true
                                            doNode.condition = newLiteral(true, objectType("bool"))
                                            doNode.statement =
                                                newBlock().also {
                                                    it.statements += dottedCall("bodyCall")
                                                }
                                            doNode.elseStatement = elseCallBlock()
                                        }

                                    block.statements +=
                                        newLabel().also { labelNode ->
                                            labelNode.label = "lab"
                                            labelNode.usedAsExpression = true
                                            labelNode.subStatement =
                                                newWhile(enterScope = true) { whileNode ->
                                                    whileNode.usedAsExpression = true
                                                    whileNode.condition =
                                                        newLiteral(true, objectType("bool"))
                                                    whileNode.statement =
                                                        newBlock().also {
                                                            it.statements += dottedCall("bodyCall")
                                                        }
                                                    whileNode.elseStatement = elseCallBlock()
                                                }
                                        }

                                    block.statements += newIfElse { ifNode ->
                                        ifNode.usedAsExpression = true
                                        ifNode.condition =
                                            newBinaryOperator(">") {
                                                it.lhs = newReference("param")
                                                it.rhs = newLiteral(7, objectType("int"))
                                            }
                                        ifNode.thenStatement =
                                            newBlock(enterScope = true) {
                                                it.statements += dottedCall("thenCall")
                                            }
                                        ifNode.elseStatement =
                                            newBlock(enterScope = true) {
                                                it.statements += dottedCall("elseCall")
                                            }
                                    }

                                    block.statements +=
                                        newSwitch(enterScope = true) { switchNode ->
                                            switchNode.selector = newReference("someref")
                                            switchNode.usedAsExpression = true
                                            switchNode.statement =
                                                newBlock().also { blk ->
                                                    blk.statements +=
                                                        newCase().also {
                                                            it.caseExpression = newReference("True")
                                                        }
                                                    blk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(newReference("a")),
                                                            listOf(
                                                                newBinaryOperator("*") {
                                                                    it.lhs = newReference("a")
                                                                    it.rhs =
                                                                        newLiteral(
                                                                            2,
                                                                            objectType("int"),
                                                                        )
                                                                }
                                                            ),
                                                        )
                                                    blk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(newReference("c")),
                                                            listOf(
                                                                newLiteral(-2, objectType("int"))
                                                            ),
                                                        )
                                                    blk.statements += newBreak()
                                                    blk.statements +=
                                                        newCase().also {
                                                            it.caseExpression =
                                                                newReference("False")
                                                        }
                                                    blk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(newReference("a")),
                                                            listOf(
                                                                newLiteral(290, objectType("int"))
                                                            ),
                                                        )
                                                    blk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(newReference("d")),
                                                            listOf(
                                                                newLiteral(-2, objectType("int"))
                                                            ),
                                                        )
                                                    blk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(newReference("b")),
                                                            listOf(
                                                                newLiteral(-2, objectType("int"))
                                                            ),
                                                        )
                                                    blk.statements += newBreak()
                                                }
                                        }

                                    block.statements += newDeclarationStatement { ds ->
                                        ds.usedAsExpression = true
                                        newVariable("a", unknownType(), holder = ds) {
                                            it.initializer = newLiteral(42, objectType("int"))
                                        }
                                    }
                                }
                        }
                    }
                }
            }

        fun getNestedComprehensions(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("whileWithBreakAndElse.py") { tu ->
                    newRecord("someRecord", "class", holder = tu, enterScope = true) { record ->
                        newMethod("func", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += dottedCall("preComprehensions")

                                    block.statements += newCollectionComprehension { listComp ->
                                        listComp.statement = newReference("i")
                                        listComp.comprehensionExpressions =
                                            mutableListOf(
                                                newComprehension {
                                                    it.variable = newReference("i")
                                                    it.iterable = newReference("someIterable")
                                                },
                                                newComprehension {
                                                    it.variable = newReference("j")
                                                    it.iterable = newReference("i")
                                                    it.predicate =
                                                        newBinaryOperator(">") { gt ->
                                                            gt.lhs = newReference("j")
                                                            gt.rhs =
                                                                newLiteral(5, objectType("int"))
                                                        }
                                                },
                                            )
                                    }

                                    block.statements += dottedCall("postComprehensions")
                                }
                        }
                    }
                }
            }

        fun getInferenceRecordPtr(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<StructTestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder().inferRecords(true).build()
                    )
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("record.cpp") { tu ->
                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)
                        func.body =
                            newBlock(enterScope = true) { block ->
                                declareVariable(
                                    block,
                                    "node",
                                    objectType("T").reference(PointerType.PointerOrigin.POINTER),
                                )
                                block.statements +=
                                    newAssign(
                                        "=",
                                        listOf(newMember("value", newReference("node"), "->")),
                                        listOf(newLiteral(42, objectType("int"))),
                                    )
                                block.statements +=
                                    newAssign(
                                        "=",
                                        listOf(newMember("next", newReference("node"), "->")),
                                        listOf(newReference("node")),
                                    )
                                block.statements +=
                                    newMemberCall(newMemberAccess("dump", newReference("node")))
                                block.statements += newReturn { it.isImplicit = true }
                            }
                    }
                }
            }

        fun getInferenceRecord(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<StructTestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder().inferRecords(true).build()
                    )
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("record.cpp") { tu ->
                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)
                        func.body =
                            newBlock(enterScope = true) { block ->
                                declareVariable(block, "node", objectType("T"))
                                block.statements +=
                                    newAssign(
                                        "=",
                                        listOf(newMember("value", newReference("node"))),
                                        listOf(newLiteral(42, objectType("int"))),
                                    )
                                block.statements +=
                                    newAssign(
                                        "=",
                                        listOf(newMember("next", newReference("node"))),
                                        listOf(
                                            newUnaryOperator("&", false, false) {
                                                it.input = newReference("node")
                                            }
                                        ),
                                    )
                                block.statements += newReturn { it.isImplicit = true }
                            }
                    }
                }
            }

        fun getInferenceBinaryOperatorReturnType(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<StructTestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder()
                            .inferRecords(true)
                            .inferReturnTypes(true)
                            .build()
                    )
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("test.python") { tu ->
                    newFunction("foo", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)
                        func.body =
                            newBlock(enterScope = true) { block ->
                                declareVariable(block, "a")
                                declareVariable(block, "b")
                                block.statements +=
                                    newAssign(
                                        "=",
                                        listOf(newReference("a")),
                                        listOf(
                                            newBinaryOperator("+") {
                                                it.lhs = dottedCall("bar")
                                                it.rhs = newLiteral(2, objectType("int"))
                                            }
                                        ),
                                    )
                                block.statements +=
                                    newAssign(
                                        "=",
                                        listOf(newReference("b")),
                                        listOf(
                                            newBinaryOperator("+") {
                                                it.lhs = newLiteral(2L, objectType("long"))
                                                it.rhs = dottedCall("baz")
                                            }
                                        ),
                                    )
                            }
                    }
                }
            }

        fun getInferenceTupleReturnType(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<StructTestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder()
                            .inferRecords(true)
                            .inferReturnTypes(true)
                            .build()
                    )
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("test.python") { tu ->

                    // The return types are resolved in the enclosing scope before entering "foo"'s
                    // scope.
                    val returnTypes = listOf(objectType("Foo"), objectType("Bar"))
                    newFunction("foo", holder = tu, enterScope = true) { func ->
                        func.returnTypes = returnTypes
                        func.type = computeType(func)
                        func.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newReturn { it.returnValue = dottedCall("bar") }
                            }
                    }
                }
            }

        fun getInferenceUnaryOperatorReturnType(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<StructTestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder()
                            .inferRecords(true)
                            .inferReturnTypes(true)
                            .build()
                    )
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("Test.java") { tu ->
                    newRecord("Test", "class", holder = tu, enterScope = true) { record ->
                        newMethod("foo", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newReturn {
                                        it.returnValue =
                                            newUnaryOperator("-", false, false) { u ->
                                                u.input = dottedCall("bar")
                                            }
                                    }
                                }
                        }
                    }
                }
            }

        fun getInferenceNestedNamespace(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<ClassTestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder()
                            .inferRecords(true)
                            .inferNamespaces(true)
                            .build()
                    )
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("Test.java") { tu ->
                    newRecord("Test", "class", holder = tu, enterScope = true) { record ->
                        newMethod("foo", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "node", objectType("java.lang.String"))
                                    block.statements += newReturn { it.isImplicit = true }
                                }
                        }
                    }
                }
            }

        fun getVariables(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("Variables.java") { tu ->
                    newRecord("Variables", "class", holder = tu, enterScope = true) { record ->
                        newField("field", objectType("int"), holder = record) {
                            it.initializer = newLiteral(42, objectType("int"))
                            it.modifiers = setOf("private")
                        }

                        newMethod("getField", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(objectType("int"))
                            method.type = computeType(method)
                            method.receiver = newVariable("this", objectType("Variables"))
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newReturn {
                                        it.returnValue = newMember("field")
                                    }
                                }
                        }

                        newMethod("getLocal", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(objectType("int"))
                            method.type = computeType(method)
                            method.receiver = newVariable("this", objectType("Variables"))
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "local", objectType("int")) {
                                        it.initializer = newLiteral(42, objectType("int"))
                                    }
                                    block.statements += newReturn {
                                        it.returnValue = newReference("local")
                                    }
                                }
                        }

                        newMethod("getShadow", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(objectType("int"))
                            method.type = computeType(method)
                            method.receiver = newVariable("this", objectType("Variables"))
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "field", objectType("int")) {
                                        it.initializer = newLiteral(43, objectType("int"))
                                    }
                                    block.statements += newReturn {
                                        it.returnValue = newReference("field")
                                    }
                                }
                        }

                        newMethod("getNoShadow", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(objectType("int"))
                            method.type = computeType(method)
                            method.receiver = newVariable("this", objectType("Variables"))
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "field", objectType("int")) {
                                        it.initializer = newLiteral(43, objectType("int"))
                                    }
                                    block.statements += newReturn {
                                        it.returnValue = newMember("field", newReference("this"))
                                    }
                                }
                        }
                    }
                }
            }

        fun getUnaryOperator(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("unaryoperator.cpp") { tu ->
                    newFunction("somefunc", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(unknownType())
                        func.type = computeType(func)
                        func.body =
                            newBlock(enterScope = true) { block ->
                                declareVariable(block, "i", objectType("int")) {
                                    it.initializer = newLiteral(0, objectType("int"))
                                }
                                block.statements +=
                                    newUnaryOperator("++", true, false) {
                                        it.input = newReference("i")
                                    }
                                block.statements += newReturn { it.isImplicit = true }
                            }
                    }
                }
            }

        fun getCompoundOperator(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("compoundoperator.cpp") { tu ->
                    newFunction("somefunc", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(unknownType())
                        func.type = computeType(func)
                        func.body =
                            newBlock(enterScope = true) { block ->
                                declareVariable(block, "i", objectType("int")) {
                                    it.initializer = newLiteral(0, objectType("int"))
                                }
                                block.statements +=
                                    newAssign(
                                        "+=",
                                        listOf(newReference("i")),
                                        listOf(newLiteral(0, objectType("int"))),
                                    )
                                block.statements += newReturn { it.isImplicit = true }
                            }
                    }
                }
            }

        fun getConditional(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tuName = "conditional_expression.cpp"
                singleTranslationUnit(tuName) { tu ->
                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)
                        func.body =
                            newBlock(enterScope = true) { block ->
                                declareVariable(block, "a", objectType("int")) {
                                    it.initializer = newLiteral(0, objectType("int"))
                                }
                                declareVariable(block, "b", objectType("int")) {
                                    it.initializer = newLiteral(1, objectType("int"))
                                }

                                val a1 =
                                    newReference("a").also {
                                        it.location =
                                            PhysicalLocation(URI(tuName), Region(5, 3, 5, 4))
                                    }
                                val cond =
                                    newReference("a")
                                        .also {
                                            it.location =
                                                PhysicalLocation(URI(tuName), Region(5, 7, 5, 8))
                                        }
                                        .let { lhs ->
                                            newBinaryOperator("==") {
                                                it.lhs = lhs
                                                it.rhs =
                                                    newReference("b").also { rhs ->
                                                        rhs.location =
                                                            PhysicalLocation(
                                                                URI(tuName),
                                                                Region(5, 12, 5, 13),
                                                            )
                                                    }
                                            }
                                        }
                                val thenAssign =
                                    newAssign(
                                        "=",
                                        listOf(
                                            newReference("b").also {
                                                it.location =
                                                    PhysicalLocation(
                                                        URI(tuName),
                                                        Region(5, 16, 5, 17),
                                                    )
                                            }
                                        ),
                                    ) {
                                        it.rhs = mutableListOf(newLiteral(2, objectType("int")))
                                        it.usedAsExpression = true
                                    }
                                val elseAssign =
                                    newAssign(
                                        "=",
                                        listOf(
                                            newReference("b").also {
                                                it.location =
                                                    PhysicalLocation(
                                                        URI(tuName),
                                                        Region(5, 23, 5, 24),
                                                    )
                                            }
                                        ),
                                    ) {
                                        it.rhs = mutableListOf(newLiteral(3, objectType("int")))
                                        it.usedAsExpression = true
                                    }
                                block.statements +=
                                    newAssign(
                                        "=",
                                        listOf(a1),
                                        listOf(newConditional(cond, thenAssign, elseAssign)),
                                    )

                                val a2 =
                                    newReference("a").also {
                                        it.location =
                                            PhysicalLocation(URI(tuName), Region(6, 3, 6, 4))
                                    }
                                val b2 =
                                    newReference("b").also {
                                        it.location =
                                            PhysicalLocation(URI(tuName), Region(6, 7, 6, 8))
                                    }
                                block.statements += newAssign("=", listOf(a2), listOf(b2))

                                block.statements += newReturn { it.isImplicit = true }
                            }
                    }
                }
            }

        fun getBasicSlice(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("BasicSlice.java") { tu ->
                    newRecord("BasicSlice", "class", holder = tu, enterScope = true) { record ->
                        newMethod("main", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.isStatic = true
                            newParameter("args", objectType("String[]"), holder = method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "a", objectType("int")) {
                                        it.initializer = newLiteral(0, objectType("int"))
                                    }

                                    block.statements += newDeclarationStatement { declStmtBCD ->
                                        newVariable("b", objectType("int"), holder = declStmtBCD) {
                                            it.initializer = newLiteral(1, objectType("int"))
                                        }
                                        newVariable("c", objectType("int"), holder = declStmtBCD) {
                                            it.initializer = newLiteral(0, objectType("int"))
                                        }
                                        newVariable("d", objectType("int"), holder = declStmtBCD) {
                                            it.initializer = newLiteral(0, objectType("int"))
                                        }
                                    }

                                    declareVariable(block, "sunShines", objectType("boolean")) {
                                        it.initializer = newLiteral(true, objectType("boolean"))
                                    }

                                    block.statements += newIfElse { outerIf ->
                                        outerIf.condition =
                                            newBinaryOperator(">") {
                                                it.lhs = newReference("a")
                                                it.rhs = newLiteral(0, objectType("int"))
                                            }
                                        outerIf.thenStatement =
                                            newBlock(enterScope = true) { thenBlk ->
                                                thenBlk.statements +=
                                                    newAssign(
                                                        "=",
                                                        listOf(newReference("d")),
                                                        listOf(newLiteral(5, objectType("int"))),
                                                    )
                                                thenBlk.statements +=
                                                    newAssign(
                                                        "=",
                                                        listOf(newReference("c")),
                                                        listOf(newLiteral(2, objectType("int"))),
                                                    )

                                                thenBlk.statements += newIfElse { innerIf ->
                                                    innerIf.condition =
                                                        newBinaryOperator(">") {
                                                            it.lhs = newReference("b")
                                                            it.rhs =
                                                                newLiteral(0, objectType("int"))
                                                        }
                                                    innerIf.thenStatement =
                                                        newBlock(enterScope = true) { innerThen ->
                                                            innerThen.statements +=
                                                                newAssign(
                                                                    "=",
                                                                    listOf(newReference("d")),
                                                                    listOf(
                                                                        newBinaryOperator("*") {
                                                                            it.lhs =
                                                                                newReference("a")
                                                                            it.rhs =
                                                                                newLiteral(
                                                                                    2,
                                                                                    objectType(
                                                                                        "int"
                                                                                    ),
                                                                                )
                                                                        }
                                                                    ),
                                                                )
                                                            innerThen.statements +=
                                                                newAssign(
                                                                    "=",
                                                                    listOf(newReference("a")),
                                                                    listOf(
                                                                        newBinaryOperator("+") {
                                                                            it.lhs =
                                                                                newReference("a")
                                                                            it.rhs =
                                                                                newBinaryOperator(
                                                                                    "*"
                                                                                ) { m ->
                                                                                    m.lhs =
                                                                                        newReference(
                                                                                            "d"
                                                                                        )
                                                                                    m.rhs =
                                                                                        newLiteral(
                                                                                            2,
                                                                                            objectType(
                                                                                                "int"
                                                                                            ),
                                                                                        )
                                                                                }
                                                                        }
                                                                    ),
                                                                )
                                                        }
                                                    // The else-if condition is intentionally just
                                                    // this
                                                    // literal and does not reference a;
                                                    // DFGTest.testOutgoingDFGFromVariableDeclaration
                                                    // asserts exactly 6 out-edges for a.
                                                    innerIf.elseStatement =
                                                        newIfElse { elseIfNode ->
                                                            elseIfNode.condition =
                                                                newLiteral(-2, objectType("int"))
                                                            elseIfNode.thenStatement =
                                                                newBlock(enterScope = true) {
                                                                    elseIfThen ->
                                                                    elseIfThen.statements +=
                                                                        newAssign(
                                                                            "=",
                                                                            listOf(
                                                                                newReference("a")
                                                                            ),
                                                                            listOf(
                                                                                newBinaryOperator(
                                                                                    "-"
                                                                                ) {
                                                                                    it.lhs =
                                                                                        newReference(
                                                                                            "a"
                                                                                        )
                                                                                    it.rhs =
                                                                                        newLiteral(
                                                                                            10,
                                                                                            objectType(
                                                                                                "int"
                                                                                            ),
                                                                                        )
                                                                                }
                                                                            ),
                                                                        )
                                                                }
                                                        }
                                                }
                                            }
                                        outerIf.elseStatement =
                                            newBlock(enterScope = true) { elseBlk ->
                                                elseBlk.statements +=
                                                    newAssign(
                                                        "=",
                                                        listOf(newReference("b")),
                                                        listOf(newLiteral(-2, objectType("int"))),
                                                    )
                                                elseBlk.statements +=
                                                    newAssign(
                                                        "=",
                                                        listOf(newReference("d")),
                                                        listOf(newLiteral(-2, objectType("int"))),
                                                    )
                                                elseBlk.statements +=
                                                    newUnaryOperator("--", true, false) {
                                                        it.input = newReference("a")
                                                    }
                                            }
                                    }

                                    block.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("a")),
                                            listOf(
                                                newBinaryOperator("+") {
                                                    it.lhs = newReference("a")
                                                    it.rhs = newReference("b")
                                                }
                                            ),
                                        )

                                    block.statements +=
                                        newSwitch(enterScope = true) { switchNode ->
                                            switchNode.selector = newReference("sunShines")
                                            switchNode.statement =
                                                newBlock().also { blk ->
                                                    blk.statements +=
                                                        newCase().also {
                                                            it.caseExpression = newReference("True")
                                                        }
                                                    blk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(newReference("a")),
                                                            listOf(
                                                                newBinaryOperator("*") {
                                                                    it.lhs = newReference("a")
                                                                    it.rhs =
                                                                        newLiteral(
                                                                            2,
                                                                            objectType("int"),
                                                                        )
                                                                }
                                                            ),
                                                        )
                                                    blk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(newReference("c")),
                                                            listOf(
                                                                newLiteral(-2, objectType("int"))
                                                            ),
                                                        )
                                                    blk.statements += newBreak()
                                                    blk.statements +=
                                                        newCase().also {
                                                            it.caseExpression =
                                                                newReference("False")
                                                        }
                                                    blk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(newReference("a")),
                                                            listOf(
                                                                newLiteral(290, objectType("int"))
                                                            ),
                                                        )
                                                    blk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(newReference("d")),
                                                            listOf(
                                                                newLiteral(-2, objectType("int"))
                                                            ),
                                                        )
                                                    blk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(newReference("b")),
                                                            listOf(
                                                                newLiteral(-2, objectType("int"))
                                                            ),
                                                        )
                                                    blk.statements += newBreak()
                                                }
                                        }

                                    block.statements += newReturn { it.isImplicit = true }
                                }
                        }
                    }
                }
            }

        fun getControlFlowSensitiveDFGIfMerge(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("ControlFlowSensitiveDFGIfMerge.java") { tu ->
                    newRecord(
                        "ControlFlowSensitiveDFGIfMerge",
                        "class",
                        holder = tu,
                        enterScope = true,
                    ) { record ->
                        newField("bla", objectType("int"), holder = record)

                        newConstructor(record.name, record, holder = record, enterScope = true) {
                            ctor ->
                            ctor.isImplicit = true
                            ctor.receiver =
                                newVariable("this", objectType("ControlFlowSensitiveDFGIfMerge"))
                            ctor.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newReturn { it.isImplicit = true }
                                }
                        }

                        newMethod("func", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.receiver =
                                newVariable("this", objectType("ControlFlowSensitiveDFGIfMerge"))
                            newParameter("args", objectType("int[]"), holder = method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "a", objectType("int")) {
                                        it.initializer = newLiteral(1, objectType("int"))
                                    }

                                    block.statements += newIfElse { ifNode ->
                                        ifNode.condition =
                                            newBinaryOperator(">") {
                                                it.lhs = newMember("length", newReference("args"))
                                                it.rhs = newLiteral(3, objectType("int"))
                                            }
                                        ifNode.thenStatement =
                                            newBlock(enterScope = true) { thenBlk ->
                                                thenBlk.statements +=
                                                    newAssign(
                                                        "=",
                                                        listOf(newReference("a")),
                                                        listOf(newLiteral(2, objectType("int"))),
                                                    )
                                            }
                                        ifNode.elseStatement =
                                            newBlock(enterScope = true) { elseBlk ->
                                                elseBlk.statements += printlnCall(newReference("a"))
                                            }
                                    }

                                    declareVariable(block, "b", objectType("int")) {
                                        it.initializer = newReference("a")
                                    }
                                    block.statements += newReturn { it.isImplicit = true }
                                }
                        }

                        newMethod("main", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.isStatic = true
                            newParameter("args", objectType("String[]"), holder = method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(
                                        block,
                                        "obj",
                                        objectType("ControlFlowSensitiveDFGIfMerge"),
                                    ) {
                                        it.initializer =
                                            newNew().also { n ->
                                                n.initializer =
                                                    newConstruction(
                                                        parseName("ControlFlowSensitiveDFGIfMerge")
                                                    ) { c ->
                                                        c.type =
                                                            objectType(
                                                                "ControlFlowSensitiveDFGIfMerge"
                                                            )
                                                    }
                                            }
                                    }
                                    block.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newMember("bla", newReference("obj"))),
                                            listOf(newLiteral(3, objectType("int"))),
                                        )
                                    block.statements += newReturn { it.isImplicit = true }
                                }
                        }
                    }
                }
            }

        fun getControlFlowSesitiveDFGSwitch(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tuName = "ControlFlowSesitiveDFGSwitch.java"
                singleTranslationUnit(tuName) { tu ->
                    newRecord(
                        "ControlFlowSesitiveDFGSwitch",
                        "class",
                        holder = tu,
                        enterScope = true,
                    ) { record ->
                        newMethod("func3", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.receiver =
                                newVariable("this", objectType("ControlFlowSesitiveDFGSwitch"))
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "switchVal", objectType("int")) {
                                        it.initializer = newLiteral(3, objectType("int"))
                                    }
                                    declareVariable(block, "a", objectType("int")) {
                                        it.initializer = newLiteral(0, objectType("int"))
                                    }

                                    block.statements +=
                                        newSwitch(enterScope = true) { switchNode ->
                                            switchNode.selector = newReference("switchVal")
                                            switchNode.statement =
                                                newBlock().also { blk ->
                                                    blk.statements +=
                                                        newCase().also {
                                                            it.caseExpression =
                                                                newLiteral(1, objectType("int"))
                                                        }
                                                    blk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(
                                                                newReference("a").also {
                                                                    it.location =
                                                                        PhysicalLocation(
                                                                            URI(tuName),
                                                                            Region(8, 9, 8, 10),
                                                                        )
                                                                }
                                                            ),
                                                            listOf(
                                                                newLiteral(10, objectType("int"))
                                                            ),
                                                        )
                                                    blk.statements += newBreak()
                                                    blk.statements +=
                                                        newCase().also {
                                                            it.caseExpression =
                                                                newLiteral(2, objectType("int"))
                                                        }
                                                    blk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(
                                                                newReference("a").also {
                                                                    it.location =
                                                                        PhysicalLocation(
                                                                            URI(tuName),
                                                                            Region(11, 9, 11, 10),
                                                                        )
                                                                }
                                                            ),
                                                            listOf(
                                                                newLiteral(11, objectType("int"))
                                                            ),
                                                        )
                                                    blk.statements += newBreak()
                                                    blk.statements +=
                                                        newCase().also {
                                                            it.caseExpression =
                                                                newLiteral(3, objectType("int"))
                                                        }
                                                    blk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(
                                                                newReference("a").also {
                                                                    it.location =
                                                                        PhysicalLocation(
                                                                            URI(tuName),
                                                                            Region(14, 9, 14, 10),
                                                                        )
                                                                }
                                                            ),
                                                            listOf(
                                                                newLiteral(12, objectType("int"))
                                                            ),
                                                        )
                                                    blk.statements += newDefault()
                                                    blk.statements += printlnCall(newReference("a"))
                                                    blk.statements += newBreak()
                                                }
                                        }

                                    declareVariable(block, "b", objectType("int")) {
                                        it.initializer = newReference("a")
                                    }
                                    block.statements += newReturn { it.isImplicit = true }
                                }
                        }
                    }
                }
            }

        fun getControlFlowSensitiveDFGIfNoMerge(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("ControlFlowSensitiveDFGIfNoMerge.java") { tu ->
                    newRecord(
                        "ControlFlowSensitiveDFGIfNoMerge",
                        "class",
                        holder = tu,
                        enterScope = true,
                    ) { record ->
                        newMethod("func2", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.receiver =
                                newVariable("this", objectType("ControlFlowSensitiveDFGIfNoMerge"))
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "a", objectType("int")) {
                                        it.initializer = newLiteral(1, objectType("int"))
                                    }

                                    block.statements += newIfElse { ifNode ->
                                        ifNode.condition =
                                            newBinaryOperator(">") {
                                                it.lhs = newMember("length", newReference("args"))
                                                it.rhs = newLiteral(3, objectType("int"))
                                            }
                                        ifNode.thenStatement =
                                            newBlock(enterScope = true) { thenBlk ->
                                                thenBlk.statements +=
                                                    newAssign(
                                                        "=",
                                                        listOf(newReference("a")),
                                                        listOf(newLiteral(2, objectType("int"))),
                                                    )
                                            }
                                        ifNode.elseStatement =
                                            newBlock(enterScope = true) { elseBlk ->
                                                elseBlk.statements +=
                                                    newAssign(
                                                        "=",
                                                        listOf(newReference("a")),
                                                        listOf(newLiteral(4, objectType("int"))),
                                                    )
                                                declareVariable(elseBlk, "b", objectType("int")) {
                                                    it.initializer = newReference("a")
                                                }
                                            }
                                    }

                                    block.statements += newReturn { it.isImplicit = true }
                                }
                        }
                    }
                }
            }

        fun getLabeledBreakContinueLoopDFG(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("LoopDFGs.java") { tu ->
                    newRecord("LoopDFGs", "class", holder = tu, enterScope = true) { record ->
                        newMethod("labeledBreakContinue", holder = record, enterScope = true) {
                            method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.receiver = newVariable("this", objectType("LoopDFGs"))
                            newParameter("param", objectType("int"), holder = method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "a", objectType("int")) {
                                        it.initializer = newLiteral(0, objectType("int"))
                                    }

                                    block.statements +=
                                        newLabel().also { labelNode ->
                                            labelNode.label = "lab1"
                                            labelNode.subStatement =
                                                newWhile(enterScope = true) { outerWhile ->
                                                    outerWhile.condition =
                                                        newBinaryOperator("<") {
                                                            it.lhs = newReference("param")
                                                            it.rhs =
                                                                newLiteral(5, objectType("int"))
                                                        }
                                                    outerWhile.statement =
                                                        newBlock().also { outerBody ->
                                                            outerBody.statements +=
                                                                newWhile(enterScope = true) {
                                                                    innerWhile ->
                                                                    innerWhile.condition =
                                                                        newBinaryOperator(">") {
                                                                            it.lhs =
                                                                                newReference(
                                                                                    "param"
                                                                                )
                                                                            it.rhs =
                                                                                newLiteral(
                                                                                    6,
                                                                                    objectType(
                                                                                        "int"
                                                                                    ),
                                                                                )
                                                                        }
                                                                    innerWhile.statement =
                                                                        newBlock().also { innerBody
                                                                            ->
                                                                            innerBody.statements +=
                                                                                newIfElse { innerIf
                                                                                    ->
                                                                                    innerIf
                                                                                        .condition =
                                                                                        newBinaryOperator(
                                                                                            ">"
                                                                                        ) {
                                                                                            it.lhs =
                                                                                                newReference(
                                                                                                    "param"
                                                                                                )
                                                                                            it.rhs =
                                                                                                newLiteral(
                                                                                                    7,
                                                                                                    objectType(
                                                                                                        "int"
                                                                                                    ),
                                                                                                )
                                                                                        }
                                                                                    innerIf
                                                                                        .thenStatement =
                                                                                        newBlock(
                                                                                            enterScope =
                                                                                                true
                                                                                        ) { thenBlk
                                                                                            ->
                                                                                            thenBlk
                                                                                                .statements +=
                                                                                                newAssign(
                                                                                                    "=",
                                                                                                    listOf(
                                                                                                        newReference(
                                                                                                            "a"
                                                                                                        )
                                                                                                    ),
                                                                                                    listOf(
                                                                                                        newLiteral(
                                                                                                            1,
                                                                                                            objectType(
                                                                                                                "int"
                                                                                                            ),
                                                                                                        )
                                                                                                    ),
                                                                                                )
                                                                                            thenBlk
                                                                                                .statements +=
                                                                                                newContinue()
                                                                                                    .also {
                                                                                                        it
                                                                                                            .label =
                                                                                                            "lab1"
                                                                                                    }
                                                                                        }
                                                                                    innerIf
                                                                                        .elseStatement =
                                                                                        newBlock(
                                                                                            enterScope =
                                                                                                true
                                                                                        ) { elseBlk
                                                                                            ->
                                                                                            elseBlk
                                                                                                .statements +=
                                                                                                printlnCall(
                                                                                                    newReference(
                                                                                                        "a"
                                                                                                    )
                                                                                                )
                                                                                            elseBlk
                                                                                                .statements +=
                                                                                                newAssign(
                                                                                                    "=",
                                                                                                    listOf(
                                                                                                        newReference(
                                                                                                            "a"
                                                                                                        )
                                                                                                    ),
                                                                                                    listOf(
                                                                                                        newLiteral(
                                                                                                            2,
                                                                                                            objectType(
                                                                                                                "int"
                                                                                                            ),
                                                                                                        )
                                                                                                    ),
                                                                                                )
                                                                                            elseBlk
                                                                                                .statements +=
                                                                                                newBreak()
                                                                                                    .also {
                                                                                                        it
                                                                                                            .label =
                                                                                                            "lab1"
                                                                                                    }
                                                                                        }
                                                                                }
                                                                            innerBody.statements +=
                                                                                newAssign(
                                                                                    "=",
                                                                                    listOf(
                                                                                        newReference(
                                                                                            "a"
                                                                                        )
                                                                                    ),
                                                                                    listOf(
                                                                                        newLiteral(
                                                                                            4,
                                                                                            objectType(
                                                                                                "int"
                                                                                            ),
                                                                                        )
                                                                                    ),
                                                                                )
                                                                        }
                                                                }

                                                            outerBody.statements +=
                                                                printlnCall(newReference("a"))
                                                            outerBody.statements +=
                                                                newAssign(
                                                                    "=",
                                                                    listOf(newReference("a")),
                                                                    listOf(
                                                                        newLiteral(
                                                                            3,
                                                                            objectType("int"),
                                                                        )
                                                                    ),
                                                                )
                                                        }
                                                }
                                        }

                                    block.statements += printlnCall(newReference("a"))
                                    block.statements += newReturn { it.isImplicit = true }
                                }
                        }
                    }
                }
            }

        fun getLoopingDFG(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("LoopDFGs.java") { tu ->
                    newRecord("LoopDFGs", "class", holder = tu, enterScope = true) { record ->
                        newMethod("looping", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.receiver = newVariable("this", objectType("LoopDFGs"))
                            newParameter("param", objectType("int"), holder = method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "a", objectType("int")) {
                                        it.initializer = newLiteral(0, objectType("int"))
                                    }

                                    block.statements +=
                                        newWhile(enterScope = true) { whileNode ->
                                            whileNode.condition =
                                                newBinaryOperator("==") {
                                                    it.lhs =
                                                        newBinaryOperator("%") { m ->
                                                            m.lhs = newReference("param")
                                                            m.rhs = newLiteral(6, objectType("int"))
                                                        }
                                                    it.rhs = newLiteral(5, objectType("int"))
                                                }
                                            whileNode.statement =
                                                newBlock().also { body ->
                                                    body.statements += newIfElse { ifNode ->
                                                        ifNode.condition =
                                                            newBinaryOperator(">") {
                                                                it.lhs = newReference("param")
                                                                it.rhs =
                                                                    newLiteral(7, objectType("int"))
                                                            }
                                                        ifNode.thenStatement =
                                                            newBlock(enterScope = true) { thenBlk ->
                                                                thenBlk.statements +=
                                                                    newAssign(
                                                                        "=",
                                                                        listOf(newReference("a")),
                                                                        listOf(
                                                                            newLiteral(
                                                                                1,
                                                                                objectType("int"),
                                                                            )
                                                                        ),
                                                                    )
                                                            }
                                                        ifNode.elseStatement =
                                                            newBlock(enterScope = true) { elseBlk ->
                                                                elseBlk.statements +=
                                                                    printlnCall(newReference("a"))
                                                                elseBlk.statements +=
                                                                    newAssign(
                                                                        "=",
                                                                        listOf(newReference("a")),
                                                                        listOf(
                                                                            newLiteral(
                                                                                2,
                                                                                objectType("int"),
                                                                            )
                                                                        ),
                                                                    )
                                                            }
                                                    }
                                                }
                                        }

                                    block.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("a")),
                                            listOf(newLiteral(3, objectType("int"))),
                                        )
                                    block.statements += newReturn { it.isImplicit = true }
                                }
                        }
                    }
                }
            }

        fun getDelayedAssignmentAfterRHS(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("DelayedAssignmentAfterRHS.java") { tu ->
                    newRecord(
                        "DelayedAssignmentAfterRHS",
                        "class",
                        holder = tu,
                        enterScope = true,
                    ) { record ->
                        newMethod("main", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.isStatic = true
                            newParameter("args", objectType("String[]"), holder = method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "a", objectType("int")) {
                                        it.initializer = newLiteral(0, objectType("int"))
                                    }
                                    declareVariable(block, "b", objectType("int")) {
                                        it.initializer = newLiteral(1, objectType("int"))
                                    }
                                    block.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("a")),
                                            listOf(
                                                newBinaryOperator("+") {
                                                    it.lhs = newReference("a")
                                                    it.rhs = newReference("b")
                                                }
                                            ),
                                        )
                                }
                        }
                    }
                }
            }

        fun getReturnTest(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tuName = "ReturnTest.java"
                singleTranslationUnit(tuName) { tu ->
                    newRecord("ReturnTest", "class", holder = tu, enterScope = true) { record ->
                        newMethod("testReturn", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(objectType("int"))
                            method.type = computeType(method)
                            method.receiver = newVariable("this", objectType("ReturnTest"))
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "a", objectType("int")) {
                                        it.initializer = newLiteral(1, objectType("int"))
                                    }

                                    block.statements += newIfElse { ifNode ->
                                        ifNode.condition =
                                            newBinaryOperator("==") {
                                                it.lhs = newReference("a")
                                                it.rhs = newLiteral(5, objectType("int"))
                                            }
                                        ifNode.thenStatement =
                                            newBlock(enterScope = true) { thenBlk ->
                                                thenBlk.statements += newReturn {
                                                    it.returnValue =
                                                        newLiteral(2, objectType("int"))
                                                    it.location =
                                                        PhysicalLocation(
                                                            URI(tuName),
                                                            Region(5, 13, 5, 21),
                                                        )
                                                }
                                            }
                                        ifNode.elseStatement =
                                            newBlock(enterScope = true) { elseBlk ->
                                                elseBlk.statements += newReturn {
                                                    it.returnValue = newReference("a")
                                                    it.location =
                                                        PhysicalLocation(
                                                            URI(tuName),
                                                            Region(7, 13, 7, 21),
                                                        )
                                                }
                                            }
                                    }

                                    block.statements += newReturn { it.isImplicit = true }
                                }
                        }
                    }
                }
            }

        fun getVisitorTest(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("Record.java") { tu ->
                    newNamespace("compiling", holder = tu, enterScope = true) { ns ->
                        newRecord("SimpleClass", "class", holder = ns, enterScope = true) { record
                            ->
                            newField("field", objectType("int"), holder = record)

                            newConstructor(
                                record.name,
                                record,
                                holder = record,
                                enterScope = true,
                            ) { ctor ->
                                ctor.receiver = newVariable("this", objectType("SimpleClass"))
                                ctor.body =
                                    newBlock(enterScope = true) { block ->
                                        block.statements += newReturn { it.isImplicit = true }
                                    }
                            }

                            newMethod("method", holder = record, enterScope = true) { method ->
                                method.returnTypes = listOf(objectType("Integer"))
                                method.type = computeType(method)
                                method.receiver = newVariable("this", objectType("SimpleClass"))
                                method.body =
                                    newBlock(enterScope = true) { block ->
                                        block.statements +=
                                            printlnCall(newLiteral("Hello world", unknownType()))

                                        declareVariable(block, "x", objectType("int")) {
                                            it.initializer = newLiteral(0, unknownType())
                                        }

                                        block.statements += newIfElse { ifNode ->
                                            ifNode.condition =
                                                newBinaryOperator(">") {
                                                    it.lhs =
                                                        newMemberCall(
                                                            newMemberAccess(
                                                                "currentTimeMillis",
                                                                newReference("System").also {
                                                                    it.isStaticAccess = true
                                                                },
                                                            )
                                                        )
                                                    it.rhs = newLiteral(0, unknownType())
                                                }
                                            ifNode.thenStatement =
                                                newBlock(enterScope = true) { thenBlk ->
                                                    thenBlk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(newReference("x")),
                                                            listOf(
                                                                newBinaryOperator("+") {
                                                                    it.lhs = newReference("x")
                                                                    it.rhs =
                                                                        newLiteral(1, unknownType())
                                                                }
                                                            ),
                                                        )
                                                }
                                            ifNode.elseStatement =
                                                newBlock(enterScope = true) { elseBlk ->
                                                    elseBlk.statements +=
                                                        newAssign(
                                                            "=",
                                                            listOf(newReference("x")),
                                                            listOf(
                                                                newBinaryOperator("-") {
                                                                    it.lhs = newReference("x")
                                                                    it.rhs =
                                                                        newLiteral(1, unknownType())
                                                                }
                                                            ),
                                                        )
                                                }
                                        }

                                        block.statements += newReturn {
                                            it.returnValue = newReference("x")
                                        }
                                    }
                            }
                        }
                    }
                }
            }

        fun getDataflowClass(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("Dataflow.java") { tu ->
                    newRecord("Dataflow", "class", holder = tu, enterScope = true) { record ->
                        newField("attr", objectType("String"), holder = record) {
                            it.initializer = newLiteral("", objectType("String"))
                        }

                        newConstructor(record.name, record, holder = record, enterScope = true) {
                            ctor ->
                            ctor.isImplicit = true
                            ctor.receiver = newVariable("this", objectType("Dataflow"))
                            ctor.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newReturn { it.isImplicit = true }
                                }
                        }

                        newMethod("toString", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(objectType("String"))
                            method.type = computeType(method)
                            method.receiver = newVariable("this", objectType("Dataflow"))
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newReturn {
                                        it.returnValue =
                                            newBinaryOperator("+") { op ->
                                                op.lhs =
                                                    newLiteral(
                                                        "ShortcutClass: attr=",
                                                        objectType("String"),
                                                    )
                                                op.rhs = newMember("attr")
                                            }
                                    }
                                }
                        }

                        newMethod("test", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(objectType("String"))
                            method.type = computeType(method)
                            method.receiver = newVariable("this", objectType("Dataflow"))
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newReturn {
                                        it.returnValue = newLiteral("abcd", unknownType())
                                    }
                                }
                        }

                        newMethod("print", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(objectType("int"))
                            method.type = computeType(method)
                            method.receiver = newVariable("this", objectType("Dataflow"))
                            newParameter("s", objectType("String"), holder = method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += printlnCall(newReference("s"))
                                    block.statements += newReturn { it.isImplicit = true }
                                }
                        }

                        newMethod("main", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.isStatic = true
                            newParameter("args", objectType("String[]"), holder = method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "sc", objectType("Dataflow")) {
                                        it.initializer =
                                            newNew().also { n ->
                                                n.initializer =
                                                    newConstruction(parseName("Dataflow")) { c ->
                                                        c.type = objectType("Dataflow")
                                                    }
                                            }
                                    }
                                    declareVariable(block, "s", objectType("String")) {
                                        it.initializer = dottedCall("sc.toString")
                                    }
                                    block.statements +=
                                        dottedCall("sc.print").also {
                                            it.arguments += newReference("s")
                                        }
                                    block.statements +=
                                        dottedCall("sc.print").also {
                                            it.arguments += dottedCall("sc.toString")
                                        }
                                }
                        }
                    }
                }
            }

        fun getShortcutClass(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("ShortcutClass.java") { tu ->
                    newRecord("ShortcutClass", "class", holder = tu, enterScope = true) { record ->
                        newField("attr", objectType("int"), holder = record) {
                            it.initializer = newLiteral(0, objectType("int"))
                        }

                        newConstructor(record.name, record, holder = record, enterScope = true) {
                            ctor ->
                            ctor.receiver = newVariable("this", objectType("ShortcutClass"))
                            ctor.isImplicit = true
                            ctor.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newReturn { it.isImplicit = true }
                                }
                        }

                        newMethod("toString", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(objectType("String"))
                            method.type = computeType(method)
                            method.receiver = newVariable("this", objectType("ShortcutClass"))
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newReturn {
                                        it.returnValue =
                                            newBinaryOperator("+") { op ->
                                                op.lhs =
                                                    newLiteral(
                                                        "ShortcutClass: attr=",
                                                        objectType("String"),
                                                    )
                                                op.rhs = newMember("attr")
                                            }
                                    }
                                }
                        }

                        newMethod("print", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(objectType("int"))
                            method.type = computeType(method)
                            method.receiver = newVariable("this", objectType("ShortcutClass"))
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += printlnCall(dottedCall("this.toString"))
                                }
                        }

                        newMethod("magic", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.receiver = newVariable("this", objectType("ShortcutClass"))
                            newParameter("b", objectType("int"), holder = method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newIfElse { outerIf ->
                                        outerIf.condition =
                                            newBinaryOperator("==") {
                                                it.lhs = newReference("b")
                                                it.rhs = newLiteral(5, objectType("int"))
                                            }
                                        outerIf.thenStatement =
                                            newBlock(enterScope = true) { thenBlk ->
                                                thenBlk.statements += newIfElse { innerIf ->
                                                    innerIf.condition =
                                                        newBinaryOperator("==") {
                                                            it.lhs = newMember("attr")
                                                            it.rhs =
                                                                newLiteral(2, objectType("int"))
                                                        }
                                                    innerIf.thenStatement =
                                                        newBlock(enterScope = true) { innerThen ->
                                                            innerThen.statements +=
                                                                newAssign(
                                                                    "=",
                                                                    listOf(newMember("attr")),
                                                                    listOf(
                                                                        newLiteral(
                                                                            3,
                                                                            objectType("int"),
                                                                        )
                                                                    ),
                                                                )
                                                        }
                                                    innerIf.elseStatement =
                                                        newBlock(enterScope = true) { innerElse ->
                                                            innerElse.statements +=
                                                                newAssign(
                                                                    "=",
                                                                    listOf(newMember("attr")),
                                                                    listOf(
                                                                        newLiteral(
                                                                            2,
                                                                            objectType("int"),
                                                                        )
                                                                    ),
                                                                )
                                                        }
                                                }
                                            }
                                        outerIf.elseStatement =
                                            newBlock(enterScope = true) { elseBlk ->
                                                elseBlk.statements +=
                                                    newAssign(
                                                        "=",
                                                        listOf(newMember("attr")),
                                                        listOf(newReference("b")),
                                                    )
                                            }
                                    }
                                }
                        }

                        newMethod("magic2", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            newParameter("b", objectType("int"), holder = method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "a")

                                    block.statements += newIfElse { outerIf ->
                                        outerIf.condition =
                                            newBinaryOperator(">") {
                                                it.lhs = newReference("b")
                                                it.rhs = newLiteral(5, objectType("int"))
                                            }
                                        outerIf.thenStatement =
                                            newBlock(enterScope = true) { thenBlk ->
                                                thenBlk.statements += newIfElse { innerIf ->
                                                    innerIf.condition =
                                                        newBinaryOperator("==") {
                                                            it.lhs = newMember("attr")
                                                            it.rhs =
                                                                newLiteral(2, objectType("int"))
                                                        }
                                                    innerIf.thenStatement =
                                                        newBlock(enterScope = true) { innerThen ->
                                                            innerThen.statements +=
                                                                newAssign(
                                                                    "=",
                                                                    listOf(newReference("a")),
                                                                    listOf(
                                                                        newLiteral(
                                                                            3,
                                                                            objectType("int"),
                                                                        )
                                                                    ),
                                                                )
                                                        }
                                                    innerIf.elseStatement =
                                                        newBlock(enterScope = true) { innerElse ->
                                                            innerElse.statements +=
                                                                newAssign(
                                                                    "=",
                                                                    listOf(newReference("a")),
                                                                    listOf(
                                                                        newLiteral(
                                                                            2,
                                                                            objectType("int"),
                                                                        )
                                                                    ),
                                                                )
                                                        }
                                                }
                                            }
                                        outerIf.elseStatement =
                                            newBlock(enterScope = true) { elseBlk ->
                                                elseBlk.statements +=
                                                    newAssign(
                                                        "=",
                                                        listOf(newReference("a")),
                                                        listOf(newReference("b")),
                                                    )
                                            }
                                    }
                                }
                        }

                        newMethod("main", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            method.isStatic = true
                            newParameter("args", objectType("int[]"), holder = method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "sc", objectType("ShortcutClass")) {
                                        it.initializer =
                                            newNew().also { n ->
                                                n.initializer =
                                                    newConstruction(parseName("ShortcutClass")) { c
                                                        ->
                                                        c.type = objectType("ShortcutClass")
                                                    }
                                            }
                                    }
                                    block.statements += dottedCall("sc.print")
                                    block.statements +=
                                        dottedCall("sc.magic").also {
                                            it.arguments += newLiteral(3, objectType("int"))
                                        }
                                    block.statements +=
                                        dottedCall("sc.magic2").also {
                                            it.arguments += newLiteral(5, objectType("int"))
                                        }
                                }
                        }
                    }
                }
            }

        /**
         * This roughly represents the following Java Code:
         * ```java
         * public class TestClass {
         *   public TestClass(int i) {
         *
         *   };
         *
         *   public TestClass method1() {
         *     return new TestClass(4);
         *   }
         *
         *   public void method2() {
         *      var variable = this.method1();
         *      variable.method2();
         *      return;
         *   }
         * }
         * ```
         */
        fun getCombinedVariableAndCallTest(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("CombinedVariableAndCall.java") { tu ->
                    newRecord("TestClass", "class", holder = tu, enterScope = true) { record ->
                        newConstructor(record.name, record, holder = record, enterScope = true) {
                            ctor ->
                            newParameter("i", objectType("int"), holder = ctor)
                        }

                        // The return type is resolved in the enclosing scope before entering
                        // "method1"'s scope.
                        val method1ReturnType = objectType("TestClass")
                        newMethod("method1", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(method1ReturnType)
                            method.type = computeType(method)
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newReturn {
                                        it.returnValue =
                                            newConstruction(parseName("TestClass")) { c ->
                                                c.type = objectType("TestClass")
                                                c.arguments += newLiteral(4, objectType("int"))
                                            }
                                    }
                                }
                        }

                        newMethod("method2", holder = record, enterScope = true) { method ->
                            method.returnTypes = listOf(unknownType())
                            method.type = computeType(method)
                            addReceiver(method, "this", objectType("TestClass"))
                            method.body =
                                newBlock(enterScope = true) { block ->
                                    declareVariable(block, "variable", autoType()) {
                                        it.initializer =
                                            newMemberCall(
                                                newMemberAccess("method1", newReference("this"))
                                            )
                                    }
                                    block.statements +=
                                        newMemberCall(
                                            newMemberAccess("method2", newReference("variable"))
                                        )
                                }
                        }
                    }
                }
            }

        /**
         * This roughly represents the following C code:
         * ```c
         * struct myStruct {
         *   int field1;
         * };
         *
         * void doSomething(int i) {}
         *
         * int main() {
         *   struct myStruct s1;
         *   struct myStruct s2;
         *
         *   doSomething(s1.field1);
         *
         *   s1.field1 = 1;
         *   s2.field1 = 2;
         *
         *   doSomething(s1.field1);
         *   doSomething(s2.field1);
         * }
         * ```
         */
        fun getSimpleFieldDataflow(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tuName = "dataflow_field.c"
                singleTranslationUnit(tuName) { tu ->
                    newRecord("myStruct", "class", holder = tu, enterScope = true) { record ->
                        newField("field1", objectType("int"), holder = record)
                    }
                    newFunction("doSomething", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(unknownType())
                        func.type = computeType(func)
                        newParameter("i", objectType("int"), holder = func)
                    }
                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)
                        func.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newDeclarationStatement { declStmt ->
                                    newVariable("s1", objectType("myStruct"), holder = declStmt)
                                    newVariable("s2", objectType("myStruct"), holder = declStmt)
                                }

                                block.statements +=
                                    newCall(newReference("doSomething")) {
                                            it.arguments +=
                                                newMember(
                                                        "field1",
                                                        newReference("s1").line(tuName, 11),
                                                    )
                                                    .line(tuName, 11)
                                        }
                                        .line(tuName, 11)

                                block.statements +=
                                    newAssign(
                                        "=",
                                        listOf(
                                            newMember("field1", newReference("s1").line(tuName, 13))
                                                .line(tuName, 13)
                                        ),
                                        listOf(newLiteral(1, unknownType())),
                                    )
                                block.statements +=
                                    newAssign(
                                        "=",
                                        listOf(
                                            newMember("field1", newReference("s2").line(tuName, 14))
                                                .line(tuName, 14)
                                        ),
                                        listOf(newLiteral(2, unknownType())),
                                    )

                                block.statements +=
                                    newCall(newReference("doSomething")) {
                                            it.arguments +=
                                                newMember(
                                                        "field1",
                                                        newReference("s1").line(tuName, 15),
                                                    )
                                                    .line(tuName, 15)
                                        }
                                        .line(tuName, 15)
                                block.statements +=
                                    newCall(newReference("doSomething")) {
                                            it.arguments +=
                                                newMember(
                                                        "field1",
                                                        newReference("s2").line(tuName, 16),
                                                    )
                                                    .line(tuName, 16)
                                        }
                                        .line(tuName, 16)
                            }
                    }
                }
            }

        /**
         * This roughly represents the following C code:
         * ```c
         * struct inner {
         *   int field;
         * };
         *
         * struct outer {
         *   struct inner in;
         * };
         *
         * void doSomething(int i) {}
         *
         * int main() {
         *   struct outer o;
         *   o.in.field = 1;
         *
         *   doSomething(o.in.field);
         * }
         * ```
         */
        fun getNestedFieldDataflow(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                val tuName = "dataflow_field.c"
                singleTranslationUnit(tuName) { tu ->
                    newRecord("inner", "class", holder = tu, enterScope = true) { record ->
                        newField("field", objectType("int"), holder = record)
                    }
                    newRecord("outer", "class", holder = tu, enterScope = true) { record ->
                        newField("in", objectType("inner"), holder = record)
                    }
                    newFunction("doSomething", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(unknownType())
                        func.type = computeType(func)
                        newParameter("i", objectType("int"), holder = func)
                    }
                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)
                        func.body =
                            newBlock(enterScope = true) { block ->
                                declareVariable(block, "o", objectType("outer"))

                                block.statements +=
                                    newAssign(
                                        "=",
                                        listOf(
                                            newMember(
                                                    "field",
                                                    newMember(
                                                            "in",
                                                            newReference("o").line(tuName, 13),
                                                        )
                                                        .line(tuName, 13),
                                                )
                                                .line(tuName, 13)
                                        ),
                                        listOf(newLiteral(1, unknownType())),
                                    )

                                block.statements +=
                                    newCall(newReference("doSomething")) {
                                            it.arguments +=
                                                newMember(
                                                        "field",
                                                        newMember(
                                                                "in",
                                                                newReference("o").line(tuName, 15),
                                                            )
                                                            .line(tuName, 15),
                                                    )
                                                    .line(tuName, 15)
                                        }
                                        .line(tuName, 15)
                            }
                    }
                }
            }

        fun prepareThrowDFGTest(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                singleTranslationUnit("some.file") { tu ->
                    newFunction("foo", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("void"))
                        func.type = computeType(func)
                        func.body =
                            newBlock(enterScope = true) { block ->
                                declareVariable(block, "a", objectType("short")) {
                                    it.initializer = newLiteral(42, unknownType())
                                }
                                block.statements += newThrow {
                                    it.exception =
                                        dottedCall("SomeError").also { c ->
                                            c.arguments += newReference("a")
                                        }
                                }
                            }
                    }
                }
            }
    }
}
