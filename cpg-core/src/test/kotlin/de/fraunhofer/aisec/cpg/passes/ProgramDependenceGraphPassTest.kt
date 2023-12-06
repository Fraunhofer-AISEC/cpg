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
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.edge.DependenceType
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.util.*
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
            object : IVisitor<Node>() {
                override fun visit(t: Node) {
                    val expectedPrevEdges =
                        t.prevCDGEdges.map {
                            it.apply { addProperty(Properties.DEPENDENCE, DependenceType.CONTROL) }
                        } +
                            t.prevDFG.mapNotNull {
                                if (it.comment == "remove next" && t.comment == "remove prev") {
                                    null
                                } else {
                                    PropertyEdge(it, t).apply {
                                        addProperty(Properties.DEPENDENCE, DependenceType.DATA)
                                    }
                                }
                            }
                    assertTrue(
                        "prevPDGEdges did not contain all prevCDGEdges and edges to all prevDFG.\n" +
                            "expectedPrevEdges: ${expectedPrevEdges.sortedBy { it.hashCode() }}\n" +
                            "actualPrevEdges: ${t.prevPDGEdges.sortedBy { it.hashCode() }}"
                    ) {
                        compareCollectionWithoutOrder(expectedPrevEdges, t.prevPDGEdges)
                    }

                    val expectedNextEdges =
                        t.nextCDGEdges.map {
                            it.apply { addProperty(Properties.DEPENDENCE, DependenceType.CONTROL) }
                        } +
                            t.nextDFG.mapNotNull {
                                if (t.comment == "remove next" && it.comment == "remove prev") {
                                    null
                                } else {
                                    PropertyEdge(t, it).apply {
                                        addProperty(Properties.DEPENDENCE, DependenceType.DATA)
                                    }
                                }
                            }
                    assertTrue(
                        "nextPDGEdges did not contain all nextCDGEdges and edges to all nextDFG." +
                            "\nexpectedNextEdges: ${expectedNextEdges.sortedBy { it.hashCode() }}" +
                            "\nactualNextEdges: ${t.nextPDGEdges.sortedBy { it.hashCode() }}"
                    ) {
                        compareCollectionWithoutOrder(expectedNextEdges, t.nextPDGEdges)
                    }
                }
            }
        )
    }

    private fun <T> compareCollectionWithoutOrder(
        expected: Collection<T>,
        actual: Collection<T>
    ): Boolean {
        val expectedWithDuplicatesGrouped = expected.groupingBy { it }.eachCount()
        val actualWithDuplicatesGrouped = actual.groupingBy { it }.eachCount()

        return expected.size == actual.size &&
            expectedWithDuplicatesGrouped == actualWithDuplicatesGrouped
    }

    companion object {
        fun testFrontend(config: TranslationConfiguration): TestLanguageFrontend {
            val ctx = TranslationContext(config, ScopeManager(), TypeManager())
            val language = config.languages.filterIsInstance<TestLanguage>().first()
            return TestLanguageFrontend(language.namespaceDelimiter, language, ctx)
        }

        @JvmStatic
        fun provideTranslationResultForPDGTest() =
            Stream.of(
                Arguments.of(getIfTest(), "if statement"),
                Arguments.of(getWhileLoopTest(), "while loop")
            )

        private fun getIfTest() =
            testFrontend(
                    TranslationConfiguration.builder()
                        .registerLanguage(TestLanguage("::"))
                        .defaultPasses()
                        .registerPass<ControlDependenceGraphPass>()
                        .registerPass<ProgramDependenceGraphPass>()
                        .build()
                )
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
            testFrontend(
                    TranslationConfiguration.builder()
                        .registerLanguage(TestLanguage("::"))
                        .defaultPasses()
                        .registerPass<ControlDependenceGraphPass>()
                        .registerPass<ProgramDependenceGraphPass>()
                        .build()
                )
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
                                            ref("i") { comment = "remove prev" }.dec()
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
