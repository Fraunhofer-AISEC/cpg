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
import kotlin.test.assertEquals
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

                            ref("b") += literal("added", t("string"))

                            ifStmt {
                                condition { ref("b") eq literal("test", t("string")) }
                                thenStmt { ref("a") assign literal(10, t("int")) }
                                elseStmt { ref("b") assign literal("removed", t("string")) }
                            }

                            call("baz") { ref("a") + ref("b") }
                        }
                    }
                }
            }
        }

    fun validatorDataflowLinear(
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
                                        ref("a") +
                                        call("foo") { call("bar") }
                                }
                            }
                            call("print") { ref("b") }

                            call("baz") { ref("a") + ref("b") }
                        }
                    }
                }
            }
        }

    fun validatorDataflowIf(
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
                                        ref("a") +
                                        call("foo") { call("bar") }
                                }
                            }

                            ifStmt {
                                condition { ref("b") eq literal("test", t("string")) }
                                thenStmt { call("print") { ref("a") } }
                            }
                            call("print") { ref("b") }

                            call("baz") { ref("a") + ref("b") }
                        }
                    }
                }
            }
        }

    fun validatorDataflowIfElse(
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
                                        ref("a") +
                                        call("foo") { call("bar") }
                                }
                            }

                            ifStmt {
                                condition { ref("b") eq literal("test", t("string")) }
                                thenStmt { call("print") { ref("a") } }
                                elseStmt { call("print") { ref("b") } }
                                call("print") { ref("b") }
                            }

                            call("baz") { ref("a") + ref("b") }
                        }
                    }
                }
            }
        }

    fun validatorDataflowLinearSimple(
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
                                    literal("bla", t("string")) + call("foo") { call("bar") }
                                }
                            }
                            call("print") { ref("a") }

                            call("baz") { ref("a") + ref("b") }
                        }
                    }
                }
            }
        }

    fun validatorDataflowIfSimple(
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
                                    literal("bla", t("string")) + call("foo") { call("bar") }
                                }
                            }

                            ifStmt {
                                condition { ref("b") eq literal("test", t("string")) }
                                thenStmt { call("print") { ref("a") } }
                            }

                            call("baz") { ref("a") + ref("b") }
                        }
                    }
                }
            }
        }

    fun validatorDataflowIfElseSimple(
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
                                    literal("bla", t("string")) + call("foo") { call("bar") }
                                }
                            }

                            ifStmt {
                                condition { ref("b") eq literal("test", t("string")) }
                                thenStmt { call("print") { ref("a") } }
                                elseStmt { call("print") { ref("a") } }
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

        val assignmentPlus = result.assigns.singleOrNull { it.operatorCode == "+=" }
        assertNotNull(assignmentPlus, "There is exactly one \"+=\" assignment")
        val refB = assignmentPlus.lhs.singleOrNull()
        assertIs<Reference>(
            refB,
            "The lhs of the assignment is expected to be a Reference to \"b\"",
        )
        assertLocalName(
            "b",
            refB,
            "The lhs of the assignment is expected to be a Reference to \"b\"",
        )

        // Intraprocedural forward may analysis. The rest doesn't matter
        val queryResultMayA =
            dataFlowBase(
                startNode = literal5,
                direction = AnalysisDirection.FORWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MAY,
                predicate = { (it.astParent as? CallExpression)?.name?.localName == "baz" },
            )
        assertTrue(
            queryResultMayA.value,
            "For the MAY analysis, we can ignore the then statement which would violate that the data would arrive in baz.",
        )
        queryResultMayA.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }

        // Intraprocedural forward may analysis. The rest doesn't matter
        val queryResultMayAMax1 =
            dataFlowBase(
                startNode = literal5,
                direction = AnalysisDirection.FORWARD,
                scope = INTRAPROCEDURAL(1),
                type = AnalysisType.MAY,
                predicate = { (it.astParent as? CallExpression)?.name?.localName == "baz" },
            )
        assertFalse(queryResultMayAMax1.value, "The path is just too short to arrive in baz.")
        queryResultMayAMax1.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            assertEquals(
                2,
                path.size,
                "The maxSize is set to 1, so there should be the start node and only one more element in the path",
            )
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }

        // Intraprocedural forward may analysis. The rest doesn't matter
        val queryResultMustA =
            dataFlowBase(
                startNode = literal5,
                direction = AnalysisDirection.FORWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MUST,
                predicate = { (it.astParent as? CallExpression)?.name?.localName == "baz" },
            )
        assertFalse(
            queryResultMustA.value,
            "For the MUST analysis, we cannot ignore the then statement which violates that the data arrive in baz.",
        )
        queryResultMustA.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }

        // Intraprocedural bidirectional may analysis. The rest doesn't matter. We should also
        // arrive at baz forward.
        val queryResultMayB =
            dataFlowBase(
                startNode = refB,
                direction = AnalysisDirection.FORWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MAY,
                predicate = { (it.astParent as? CallExpression)?.name?.localName == "baz" },
            )
        assertTrue(
            queryResultMayB.value,
            "For the MAY analysis, we can ignore the else statement which violates that the value in \"b\" arrives in baz.",
        )
        queryResultMayB.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }

        // Intraprocedural forward may analysis. The rest doesn't matter. Either arrive at the 5
        // (backwards) or in baz (forward).
        val queryResultMustB =
            dataFlowBase(
                startNode = refB,
                direction = AnalysisDirection.FORWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MUST,
                predicate = { (it.astParent as? CallExpression)?.name?.localName == "baz" },
            )
        assertFalse(
            queryResultMustB.value,
            "For the MUST analysis, we cannot ignore the else statement which violates that the value in \"b\" arrives in baz.",
        )
        queryResultMustB.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }
    }

    @Test
    fun testIntraproceduralBidirectionalDFG() {
        val result = verySimpleDataflow()
        val assignmentPlus = result.assigns.singleOrNull { it.operatorCode == "+=" }
        assertNotNull(assignmentPlus, "There is exactly one \"+=\" assignment")
        val refB = assignmentPlus.lhs.singleOrNull()
        assertIs<Reference>(
            refB,
            "The lhs of the assignment is expected to be a Reference to \"b\"",
        )
        assertLocalName(
            "b",
            refB,
            "The lhs of the assignment is expected to be a Reference to \"b\"",
        )

        // Intraprocedural bidirectional may analysis. The rest doesn't matter. Either arrive at the
        // 5 (backwards) or in baz (forward).
        val queryResultMay =
            dataFlowBase(
                startNode = refB,
                direction = AnalysisDirection.BIDIRECTIONAL,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MAY,
                predicate = {
                    (it as? Literal<*>)?.value == 5 ||
                        (it.astParent as? CallExpression)?.name?.localName == "baz"
                },
            )
        assertTrue(
            queryResultMay.value,
            "For the MAY analysis, we can ignore the else statement which violates that the value in \"b\" arrives in baz.",
        )
        queryResultMay.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }

        // Intraprocedural forward may analysis. The rest doesn't matter. Either arrive at the 5
        // (backwards) or in baz (forward).
        val queryResultMust =
            dataFlowBase(
                startNode = refB,
                direction = AnalysisDirection.BIDIRECTIONAL,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MUST,
                predicate = {
                    (it as? Literal<*>)?.value == 5 ||
                        (it.astParent as? CallExpression)?.name?.localName == "baz"
                },
            )
        assertFalse(
            queryResultMust.value,
            "For the MUST analysis, we cannot ignore the else statement which violates that the value in \"b\" arrives in baz and in addition, there's the path which won't reach the \"5\".",
        )
        queryResultMust.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
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
        val assignmentPlus = result.assigns.singleOrNull { it.operatorCode == "+=" }
        assertNotNull(assignmentPlus, "There is exactly one \"+=\" assignment")
        val refB = assignmentPlus.lhs.singleOrNull()
        assertIs<Reference>(
            refB,
            "The lhs of the assignment is expected to be a Reference to \"b\"",
        )
        assertLocalName(
            "b",
            refB,
            "The lhs of the assignment is expected to be a Reference to \"b\"",
        )

        // Intraprocedural backward may analysis. The rest doesn't matter
        val queryResultMayA =
            dataFlowBase(
                startNode = bazARef,
                direction = AnalysisDirection.BACKWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MAY,
                predicate = { (it as? Literal<*>)?.value == 5 },
            )
        assertTrue(
            queryResultMayA.value,
            "For the MAY analysis, we can ignore the then statement which would violate that the \"5\" would arrive in baz.",
        )
        queryResultMayA.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }

        // Intraprocedural backward may analysis but we stop too early. The rest doesn't matter
        val queryResultMayAMax1 =
            dataFlowBase(
                startNode = bazARef,
                direction = AnalysisDirection.BACKWARD,
                scope = INTRAPROCEDURAL(1),
                type = AnalysisType.MAY,
                predicate = { (it as? Literal<*>)?.value == 5 },
            )
        assertFalse(
            queryResultMayAMax1.value,
            "The path is just too short to reach the value \"5\"",
        )
        queryResultMayAMax1.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            assertEquals(
                2,
                path.size,
                "The maxSize is set to 1, so there should be the start node and only one more element in the path",
            )
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }

        // Intraprocedural forward may analysis. The rest doesn't matter
        val queryResultMustA =
            dataFlowBase(
                startNode = bazARef,
                direction = AnalysisDirection.BACKWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MUST,
                predicate = { (it as? Literal<*>)?.value == 5 },
            )
        assertFalse(
            queryResultMustA.value,
            "For the MUST analysis, we cannot ignore the then statement which violates that the \"5\" arrives in baz.",
        )
        queryResultMustA.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }

        // Intraprocedural bidirectional may analysis. The rest doesn't matter. We should not arrive
        // at the 5 because there's a function call on the path.
        val queryResultMayBTo5 =
            dataFlowBase(
                startNode = refB,
                direction = AnalysisDirection.BACKWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MAY,
                predicate = { (it as? Literal<*>)?.value == 5 },
            )
        assertFalse(
            queryResultMayBTo5.value,
            "For the MAY analysis, we can ignore the direct route which violates that reaches the \"5\" but we cannot get through the function call on the path.",
        )
        queryResultMayBTo5.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }

        // Intraprocedural forward may analysis. The rest doesn't matter. Either arrive at the 5
        // (backwards) or in baz (forward).
        val queryResultMustBTo5 =
            dataFlowBase(
                startNode = refB,
                direction = AnalysisDirection.BACKWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MUST,
                predicate = { (it as? Literal<*>)?.value == 5 },
            )
        assertFalse(
            queryResultMustBTo5.value,
            "For the MUST analysis, we cannot ignore the direct route which violates that reaches the \"5\" and we cannot get through the function call on the path.",
        )
        queryResultMustBTo5.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }

        // Intraprocedural bidirectional may analysis. The rest doesn't matter. We should arrive at
        // the value "bla".
        val queryResultMayBToBla =
            dataFlowBase(
                startNode = refB,
                direction = AnalysisDirection.BACKWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MAY,
                predicate = { (it as? Literal<*>)?.value == "bla" },
            )
        assertTrue(
            queryResultMayBToBla.value,
            "For the MAY analysis, we can ignore the direct route which violates that reaches the \"bla\".",
        )
        queryResultMayBToBla.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }

        // Intraprocedural forward may analysis. The rest doesn't matter. Either arrive at the value
        // "bla".
        val queryResultMustBToBla =
            dataFlowBase(
                startNode = refB,
                direction = AnalysisDirection.BACKWARD,
                scope = INTRAPROCEDURAL(),
                type = AnalysisType.MUST,
                predicate = { (it as? Literal<*>)?.value == "bla" },
            )
        assertFalse(
            queryResultMustBToBla.value,
            "For the MUST analysis, we cannot ignore the direct route which violates that reaches the \"bla\".",
        )
        queryResultMustBToBla.children.forEach {
            // There are multiple paths which have their own query tree. The children here hold the
            // list of visited nodes in the value.
            val path = it.children.singleOrNull()?.value as? List<*>
            assertNotNull(path, "There should be a path represented by a list of nodes")
            path.forEach { node ->
                assertIs<Node>(node, "The list should contain nodes")
                assertLocalName(
                    "main",
                    node.firstParentOrNull<FunctionDeclaration>(),
                    "We expect that all nodes are within the function \"main\". I.e., there's no node in foo.",
                )
            }
        }
    }

    @Test
    fun testValidatorDFGSimple() {
        val resultLinear = validatorDataflowLinearSimple()
        val linearStartA = resultLinear.variables["a"]
        assertNotNull(linearStartA, "There's a variable \"a\" in main")
        val linearResult =
            dataFlowWithValidator(
                source = linearStartA,
                validatorPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "print"
                },
                sinkPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "baz"
                },
                scope = INTRAPROCEDURAL(),
            )
        assertTrue(
            linearResult.value,
            "There is only one path which goes from the variable through print to baz.",
        )

        val resultLinearFails = validatorDataflowLinear()
        val linearStartAFails = resultLinearFails.variables["a"]
        assertNotNull(linearStartAFails, "There's a variable \"a\" in main")
        val linearResultFails =
            dataFlowWithValidator(
                source = linearStartAFails,
                validatorPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "print"
                },
                sinkPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "baz"
                },
                scope = INTRAPROCEDURAL(),
            )
        assertTrue(
            linearResultFails.value,
            "There is only one path which goes from the variable through print(b) to baz.",
        )

        val resultIf = validatorDataflowIfSimple()
        val ifStartA = resultIf.variables["a"]
        assertNotNull(ifStartA, "There's a variable \"a\" in main")
        val ifResult =
            dataFlowWithValidator(
                source = ifStartA,
                validatorPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "print"
                },
                sinkPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "baz"
                },
                scope = INTRAPROCEDURAL(),
            )
        assertFalse(
            ifResult.value,
            "There is a path which goes through the \"else\" branch without passing print before reaching baz.",
        )

        val resultIfWithB = validatorDataflowIf()
        val ifStartAWithB = resultIfWithB.variables["a"]
        assertNotNull(ifStartAWithB, "There's a variable \"a\" in main")
        val ifResultWithB =
            dataFlowWithValidator(
                source = ifStartAWithB,
                validatorPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "print"
                },
                sinkPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "baz"
                },
                scope = INTRAPROCEDURAL(),
            )
        assertTrue(
            ifResultWithB.value,
            "There is a path which goes through the \"else\" branch but prints b before reaching baz.",
        )

        val resultIfElse = validatorDataflowIfElseSimple()
        val ifElseStartA = resultIfElse.variables["a"]
        assertNotNull(ifElseStartA, "There's a variable \"a\" in main")
        val ifElseResult =
            dataFlowWithValidator(
                source = ifElseStartA,
                validatorPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "print"
                },
                sinkPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "baz"
                },
                scope = INTRAPROCEDURAL(),
            )
        assertTrue(ifElseResult.value, "Both paths go from the variable through print to baz.")
    }
}
