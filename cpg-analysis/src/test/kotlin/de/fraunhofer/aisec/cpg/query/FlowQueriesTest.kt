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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.testcases.testFrontend
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
    fun testIntraproceduralForwardDFG() {
        val result = verySimpleDataflow()
        val literal5 = result.literals.singleOrNull { it.value == 5 }
        assertNotNull(literal5)

        // Intraprocedural forward may analysis. The rest doesn't matter
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

    @Test
    fun testIntraproceduralBackwardDFG() {
        val result = verySimpleDataflow()
        val bazCall = result.calls["baz"]
        assertNotNull(bazCall, "There is exactly one call to a function called baz")
        val addition = bazCall.arguments.singleOrNull()
        assertIs<BinaryOperator>(
            addition,
            "There should be a single argument for baz which is a binary operator",
        )
        val bazARef = addition.lhs
        assertIs<Reference>(
            bazARef,
            "The lhs of the addition is expected to be a Reference to \"a\"",
        )
        assertLocalName(
            "a",
            bazARef,
            "The lhs of the addition is expected to be a Reference to \"a\"",
        )

        // Intraprocedural backward may analysis. The rest doesn't matter
        val queryResultMay =
            dataFlowBase(
                startNode = bazARef,
                direction = AnalysisDirection.BACKWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MAY,
                predicate = { (it as? Literal<*>)?.value == 5 },
            )
        print(queryResultMay.printNicely())
        assertTrue(
            queryResultMay.value,
            "For the MAY analysis, we can ignore the then statement which would violate that the \"5\" would arrive in baz.",
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
        val queryResultMust =
            dataFlowBase(
                startNode = bazARef,
                direction = AnalysisDirection.BACKWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MUST,
                predicate = { (it as? Literal<*>)?.value == 5 },
            )
        print(queryResultMust.printNicely())
        assertFalse(
            queryResultMust.value,
            "For the MUST analysis, we cannot ignore the then statement which violates that the \"5\" arrives in baz.",
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
