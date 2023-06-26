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

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

class ProgramDependenceGraphPassTest {

    @Test
    fun `test pdg of if statement`() {
        val result = getIfTest()
        assertNotNull(result)
        val main = result.functions["main"]
        assertNotNull(main)

        main.accept(
            Strategy::AST_FORWARD,
            object : IVisitor<Node>() {
                override fun visit(t: Node) {
                    val expectedPrevEdges =
                        (t.prevCDGEdges + t.prevDFG.map { PropertyEdge(t, it) }).sortedBy {
                            it.hashCode()
                        }
                    assertContentEquals(
                        expectedPrevEdges,
                        t.prevPDGEdges.sortedBy { it.hashCode() },
                        "prevPDGEdges did not contain all prevCDGEdges and edges to all prevDFG"
                    )

                    val expectedNextEdges =
                        (t.nextCDGEdges + t.nextDFG.map { PropertyEdge(t, it) }).sortedBy {
                            it.hashCode()
                        }
                    assertContentEquals(
                        expectedNextEdges,
                        t.nextPDGEdges.sortedBy { it.hashCode() },
                        "prevPDGEdges did not contain all nextCDGEdges and edges to all nextDFG"
                    )
                }
            }
        )
    }

    companion object {
        fun testFrontend(config: TranslationConfiguration): TestLanguageFrontend {
            val ctx = TranslationContext(config, ScopeManager(), TypeManager())
            val language = config.languages.filterIsInstance<TestLanguage>().first()
            return TestLanguageFrontend(language.namespaceDelimiter, language, ctx)
        }

        fun getIfTest() =
            testFrontend(
                    TranslationConfiguration.builder()
                        .registerLanguage(TestLanguage("::"))
                        .defaultPasses()
                        .registerPass<ControlDependenceGraphPass>()
                        .registerPass<ProgramDependencyGraphPass>()
                        .build()
                )
                .build {
                    translationResult {
                        translationUnit("if.cpp") {
                            // The main method
                            function("main", t("int")) {
                                body {
                                    declare { variable("i", t("int")) { call("rand") } }
                                    ifStmt {
                                        condition { ref("i") lt literal(0, t("int")) }
                                        thenStmt {
                                            ref("i") assign (ref("i") * literal(-1, t("int")))
                                        }
                                    }
                                    returnStmt { ref("i") }
                                }
                            }
                        }
                    }
                }
    }
}
