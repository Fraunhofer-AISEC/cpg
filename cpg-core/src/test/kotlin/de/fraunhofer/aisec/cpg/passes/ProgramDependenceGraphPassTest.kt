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
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
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
                    translationResult {
                        translationUnit("if.cpp") {
                            // The main method
                            function("main", t("int")) {
                                body {
                                    declare {
                                        variable("i", t("int")) {
                                            comment = "remove next"
                                            call("rand")
                                        }
                                    }
                                    ifStmt {
                                        condition { ref("i") lt literal(0, t("int")) }
                                        thenStmt {
                                            ref("i") assign
                                                {
                                                    ref("i") { comment = "remove prev" } *
                                                        literal(-1, t("int"))
                                                }
                                        }
                                    }
                                    returnStmt { ref("i") }
                                }
                            }
                        }
                    }
                }

        private fun getWhileLoopTest() =
            testFrontend {
                    it.registerLanguage<TestLanguage>()
                    it.defaultPasses()
                    it.registerPass<ControlDependenceGraphPass>()
                    it.registerPass<ProgramDependenceGraphPass>()
                }
                .build {
                    translationResult {
                        translationUnit("loop.cpp") {
                            // The main method
                            function("main", t("int")) {
                                body {
                                    declare {
                                        variable("i", t("int")) {
                                            comment = "remove next"
                                            call("rand")
                                        }
                                    }
                                    whileStmt {
                                        whileCondition { ref("i") gt literal(0, t("int")) }
                                        loopBody {
                                            call("printf") { literal("#", t("string")) }
                                            ref("i") { comment = "remove prev, remove next" }.dec()
                                        }
                                    }
                                    call("printf") { literal("\n", t("string")) }
                                    returnStmt { literal(0, t("int")) }
                                }
                            }
                        }
                    }
                }
    }
}
