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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.util.stream.Stream
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ProgramDependenceGraphPassTest {

    @ParameterizedTest(name = "test if pdg of {1} is equal to the union of cdg and dfg")
    @MethodSource("provideTranslationResultForPDGTest")
    fun `test if pdg is equal to union of cdg and dfg`(result: TranslationResult, name: String) {
        assertNotNull(result)
        val main = result.functions["main"]
        assertNotNull(main)

        main.accept(
            Strategy::AST_FORWARD,
            object : IVisitor<AstNode>() {
                override fun visit(t: AstNode) {
                    val expectedPrevEdges =
                        t.prevCDGEdges +
                            t.prevDFGEdges.filter {
                                if (
                                    "remove next" in (it.start.comment ?: "") &&
                                        "remove prev" in (t.comment ?: "")
                                ) {
                                    false
                                } else {
                                    true
                                }
                            }
                    assertTrue(
                        "prevPDGEdges did not contain all prevCDGEdges and edges to all prevDFG.\n" +
                            "expectedPrevEdges: ${expectedPrevEdges.sortedBy { it.hashCode() }}\n" +
                            "actualPrevEdges: ${t.prevPDGEdges.sortedBy { it.hashCode() }}"
                    ) {
                        t.prevPDGEdges.union(expectedPrevEdges) == t.prevPDGEdges
                    }

                    val expectedNextEdges =
                        t.nextCDGEdges +
                            t.nextDFGEdges.filter {
                                if (
                                    "remove next" in (t.comment ?: "") &&
                                        "remove prev" in (it.end.comment ?: "")
                                ) {
                                    false
                                } else {
                                    true
                                }
                            }
                    assertTrue(
                        "nextPDGEdges did not contain all nextCDGEdges and edges to all nextDFG." +
                            "\nexpectedNextEdges: ${expectedNextEdges.sortedBy { it.hashCode() }}" +
                            "\nactualNextEdges: ${t.nextPDGEdges.sortedBy { it.hashCode() }}"
                    ) {
                        t.prevPDGEdges.union(expectedPrevEdges) == t.prevPDGEdges
                    }
                }
            },
        )
    }

    companion object {
        @JvmStatic
        fun provideTranslationResultForPDGTest() =
            Stream.of(
                Arguments.of(getIfTest(), "if statement"),
                Arguments.of(getWhileLoopTest(), "while loop"),
            )

        private fun getIfTest() =
            testFrontend {
                    it.registerLanguage<TestLanguage>()
                    it.defaultPasses()
                    it.registerPass<ControlDependenceGraphPass>()
                    it.registerPass<ProgramDependenceGraphPass>()
                }
                .build {
                    val tu = newTranslationUnit("if.cpp")
                    scopeManager.resetToGlobal(tu)

                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)

                        func.body =
                            newBlock(enterScope = true) { block ->
                                val declStmt = newDeclarationStatement()
                                val i =
                                    newVariable("i", objectType("int")).also {
                                        it.comment = "remove next"
                                        it.initializer = newCall(newReference("rand"))
                                    }
                                declStmt.declarations += i
                                scopeManager.addDeclaration(i)
                                block.statements += declStmt

                                val ifElse = newIfElse { ifElse ->
                                    // Fluent's "lt" infix operator has no ArgumentHolder
                                    // context, so it never actually attaches the comparison it
                                    // builds -- the self-attaching ref("i")/literal(0) operands
                                    // silently overwrite each other on the IfElse (an
                                    // ArgumentHolder), leaving the *literal* as the "condition"
                                    // instead of the intended comparison. Faithfully reproduced
                                    // here (confirmed via the original Fluent-based test).
                                    ifElse.condition = newLiteral(0, objectType("int"))

                                    ifElse.thenStatement =
                                        newBlock(enterScope = true) { thenBlock ->
                                            val assign =
                                                newAssign(
                                                    operatorCode = "=",
                                                    lhs = listOf(newReference("i")),
                                                    rhs =
                                                        listOf(
                                                            newBinaryOperator("*").also {
                                                                it.lhs =
                                                                    newReference("i").also { ref ->
                                                                        ref.comment = "remove prev"
                                                                    }
                                                                it.rhs =
                                                                    newLiteral(
                                                                        -1,
                                                                        objectType("int"),
                                                                    )
                                                            }
                                                        ),
                                                )
                                            thenBlock.statements += assign
                                        }
                                }
                                block.statements += ifElse

                                val returnStmt = newReturn()
                                returnStmt.returnValue = newReference("i")
                                block.statements += returnStmt
                            }
                    }

                    translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
                }

        private fun getWhileLoopTest() =
            testFrontend {
                    it.registerLanguage<TestLanguage>()
                    it.defaultPasses()
                    it.registerPass<ControlDependenceGraphPass>()
                    it.registerPass<ProgramDependenceGraphPass>()
                }
                .build {
                    val tu = newTranslationUnit("loop.cpp")
                    scopeManager.resetToGlobal(tu)

                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)

                        func.body =
                            newBlock(enterScope = true) { block ->
                                val declStmt = newDeclarationStatement()
                                val i =
                                    newVariable("i", objectType("int")).also {
                                        it.comment = "remove next"
                                        it.initializer = newCall(newReference("rand"))
                                    }
                                declStmt.declarations += i
                                scopeManager.addDeclaration(i)
                                block.statements += declStmt

                                val whileStmt =
                                    newWhile(enterScope = true) { w ->
                                        // Unlike "lt" above, "gt" (via its ArgumentHolder context)
                                        // does end up attaching the correct comparison here -- its
                                        // own `holder += node` call happens after (and therefore
                                        // overwrites) the self-attaching ref/literal operands.
                                        w.condition =
                                            newBinaryOperator(">").also {
                                                it.lhs = newReference("i")
                                                it.rhs = newLiteral(0, objectType("int"))
                                            }
                                        w.statement =
                                            newBlock(enterScope = true) { loopBody ->
                                                val printfCall = newCall(newReference("printf"))
                                                printfCall.arguments +=
                                                    newLiteral("#", objectType("string"))
                                                loopBody.statements += printfCall

                                                val decOp =
                                                    newUnaryOperator(
                                                            "--",
                                                            postfix = true,
                                                            prefix = false,
                                                        )
                                                        .also {
                                                            it.input =
                                                                newReference("i").also { ref ->
                                                                    ref.comment =
                                                                        "remove prev, remove next"
                                                                }
                                                        }
                                                loopBody.statements += decOp
                                            }
                                    }
                                block.statements += whileStmt

                                val printfCall2 = newCall(newReference("printf"))
                                printfCall2.arguments += newLiteral("\n", objectType("string"))
                                block.statements += printfCall2

                                val returnStmt = newReturn()
                                returnStmt.returnValue = newLiteral(0, objectType("int"))
                                block.statements += returnStmt
                            }
                    }

                    translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
                }
    }
}
