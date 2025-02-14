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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.array
import de.fraunhofer.aisec.cpg.graph.builder.assign
import de.fraunhofer.aisec.cpg.graph.builder.body
import de.fraunhofer.aisec.cpg.graph.builder.call
import de.fraunhofer.aisec.cpg.graph.builder.condition
import de.fraunhofer.aisec.cpg.graph.builder.declare
import de.fraunhofer.aisec.cpg.graph.builder.eq
import de.fraunhofer.aisec.cpg.graph.builder.function
import de.fraunhofer.aisec.cpg.graph.builder.ifStmt
import de.fraunhofer.aisec.cpg.graph.builder.literal
import de.fraunhofer.aisec.cpg.graph.builder.param
import de.fraunhofer.aisec.cpg.graph.builder.plus
import de.fraunhofer.aisec.cpg.graph.builder.ref
import de.fraunhofer.aisec.cpg.graph.builder.returnStmt
import de.fraunhofer.aisec.cpg.graph.builder.t
import de.fraunhofer.aisec.cpg.graph.builder.thenStmt
import de.fraunhofer.aisec.cpg.graph.builder.translationResult
import de.fraunhofer.aisec.cpg.graph.builder.translationUnit
import de.fraunhofer.aisec.cpg.graph.builder.variable
import de.fraunhofer.aisec.cpg.graph.builder.void
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.graph.literals
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.testcases.testFrontend
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FlowQueriesTest {
    fun verySimpleDataflow(
        config: TranslationConfiguration =
            TranslationConfiguration.builder()
                .defaultPasses()
                .registerLanguage(TestLanguage("."))
                .build()
    ) =
        testFrontend(config).build {
            translationResult {
                translationUnit("Dataflow.java") {
                    function("foo", t("string")) {
                        param("arg", t("int"))
                        body { returnStmt { call("toString") { ref("arg") } } }
                    }

                    function("main", void()) {
                        param("args", t("string").array())
                        body {
                            declare { variable("a", t("int")) { literal(5, t("int")) } }

                            declare {
                                variable("b", t("string")) {
                                    literal("bla", t("string")) +
                                        call("foo") { ref("a") } +
                                        call("foo") { call("bar") }
                                }
                            }
                            call("print") { ref("a") }

                            call("print") { ref("b") }

                            ifStmt {
                                condition { ref("b") eq literal("test", t("string")) }
                                thenStmt { ref("a") assign literal(10, t("int")) }
                            }

                            call("baz") { ref("a") + ref("b") }
                        }
                    }
                }
            }
        }

    @Test
    fun testNextDFGIntraprocedurally() {
        val result = verySimpleDataflow()
        val literal5 = result.literals.singleOrNull { it.value == 5 }
        // Intraprocedural forward may analysis. The rest doesn't matter
        assertNotNull(literal5)
        val queryResultMay =
            dataFlowBase(
                startNode = literal5,
                direction = AnalysisDirection.FORWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MAY,
                predicate = { (it.astParent as? CallExpression)?.name?.localName == "baz" },
            )
        print(queryResultMay.printNicely())
        assertTrue(
            queryResultMay.value,
            "For the MAY analysis, we can ignore the then statement which would violate that the data would arrive in baz.",
        )
        queryResultMay.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<Node>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }

        // Intraprocedural forward may analysis. The rest doesn't matter
        assertNotNull(literal5)
        val queryResultMust =
            dataFlowBase(
                startNode = literal5,
                direction = AnalysisDirection.FORWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MUST,
                predicate = { (it.astParent as? CallExpression)?.name?.localName == "baz" },
            )
        print(queryResultMust.printNicely())
        assertFalse(
            queryResultMust.value,
            "For the MUST analysis, we cannot ignore the then statement which violates that the data arrive in baz.",
        )
        queryResultMust.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<Node>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }
    }
}
