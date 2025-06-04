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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.testcases.FlowQueriesTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Ignore
class DataflowQueriesTest {

    @Test
    fun testIntraproceduralForwardDFG() {
        val result = FlowQueriesTest.verySimpleDataflow()
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
            dataFlow(
                startNode = literal5,
                direction = Forward(GraphToFollow.DFG),
                scope = Intraprocedural(),
                type = May,
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
            dataFlow(
                startNode = literal5,
                direction = Forward(GraphToFollow.DFG),
                scope = Intraprocedural(1),
                type = May,
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
            dataFlow(
                startNode = literal5,
                direction = Forward(GraphToFollow.DFG),
                scope = Intraprocedural(),
                type = Must,
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
            dataFlow(
                startNode = refB,
                direction = Forward(GraphToFollow.DFG),
                scope = Intraprocedural(),
                type = May,
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
            dataFlow(
                startNode = refB,
                direction = Forward(GraphToFollow.DFG),
                scope = Intraprocedural(),
                type = Must,
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
        val result = FlowQueriesTest.verySimpleDataflow()
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
            dataFlow(
                startNode = refB,
                direction = Bidirectional(GraphToFollow.DFG),
                scope = Intraprocedural(),
                type = May,
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
            dataFlow(
                startNode = refB,
                direction = Bidirectional(GraphToFollow.DFG),
                scope = Intraprocedural(),
                type = Must,
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
        val result = FlowQueriesTest.verySimpleDataflow()
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
            dataFlow(
                startNode = bazARef,
                direction = Backward(GraphToFollow.DFG),
                scope = Intraprocedural(),
                type = May,
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
            dataFlow(
                startNode = bazARef,
                direction = Backward(GraphToFollow.DFG),
                scope = Intraprocedural(1),
                type = May,
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
            dataFlow(
                startNode = bazARef,
                direction = Backward(GraphToFollow.DFG),
                scope = Intraprocedural(),
                type = Must,
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
            dataFlow(
                startNode = refB,
                direction = Backward(GraphToFollow.DFG),
                scope = Intraprocedural(),
                type = May,
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
            dataFlow(
                startNode = refB,
                direction = Backward(GraphToFollow.DFG),
                scope = Intraprocedural(),
                type = Must,
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
            dataFlow(
                startNode = refB,
                direction = Backward(GraphToFollow.DFG),
                scope = Intraprocedural(),
                type = May,
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
            dataFlow(
                startNode = refB,
                direction = Backward(GraphToFollow.DFG),
                scope = Intraprocedural(),
                type = Must,
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
    fun testValidatorDFGSimpleLinear() {
        val resultLinear = FlowQueriesTest.validatorDataflowLinearSimple()
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
                scope = Intraprocedural(),
            )
        assertTrue(
            linearResult.value,
            "There is only one path which goes from the variable through print to baz.",
        )
    }

    @Test
    fun testValidatorDFGLinear() {
        val resultLinearWithB = FlowQueriesTest.validatorDataflowLinear()
        val linearStartAWithB = resultLinearWithB.variables["a"]
        assertNotNull(linearStartAWithB, "There's a variable \"a\" in main")
        val linearResultWithB =
            dataFlowWithValidator(
                source = linearStartAWithB,
                validatorPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "print"
                },
                sinkPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "baz"
                },
                scope = Intraprocedural(),
            )
        assertTrue(
            linearResultWithB.value,
            "There is only one path which goes from the variable through print(b) to baz.",
        )
    }

    @Test
    fun testValidatorDFGSimpleLinearWithCall() {
        val resultLinearWithBInterProc = FlowQueriesTest.validatorDataflowLinearWithCall()
        val linearStartAWithBInterProc = resultLinearWithBInterProc.variables["a"]
        assertNotNull(linearStartAWithBInterProc, "There's a variable \"a\" in main")
        val linearResultWithBInterProc =
            dataFlowWithValidator(
                source = linearStartAWithBInterProc,
                validatorPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "print"
                },
                sinkPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "baz"
                },
                scope = Intraprocedural(),
            )
        assertFalse(
            linearResultWithBInterProc.value,
            "The path using \"b\" cannot be found because we have to go through the function call \"foo\".",
        )

        val resultIf = FlowQueriesTest.validatorDataflowIfSimple()
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
                scope = Intraprocedural(),
            )
        assertFalse(
            ifResult.value,
            "There is a path which goes through the \"else\" branch without passing print before reaching baz.",
        )
    }

    @Test
    fun testValidatorDFGIf() {
        val resultIfWithB = FlowQueriesTest.validatorDataflowIf()
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
                scope = Intraprocedural(),
            )
        assertTrue(
            ifResultWithB.value,
            "There is a path which goes through the \"else\" branch but prints b before reaching baz.",
        )
    }

    @Test
    fun testValidatorDFGSimpleIfWithCall() {
        val resultIfWithBInterProc = FlowQueriesTest.validatorDataflowIfWithCall()
        val ifStartAWithBInterProc = resultIfWithBInterProc.variables["a"]
        assertNotNull(ifStartAWithBInterProc, "There's a variable \"a\" in main")
        val ifResultWithBInterProc =
            dataFlowWithValidator(
                source = ifStartAWithBInterProc,
                validatorPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "print"
                },
                sinkPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "baz"
                },
                scope = Intraprocedural(),
            )
        assertFalse(
            ifResultWithBInterProc.value,
            "The path using \"b\" cannot be found because we have to go through the function call \"foo\".",
        )
    }

    @Test
    fun testValidatorDFGSimpleIfElse() {
        val resultIfElse = FlowQueriesTest.validatorDataflowIfElseSimple()
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
                scope = Intraprocedural(),
            )
        assertTrue(ifElseResult.value, "Both paths go from the variable through print to baz.")
    }

    @Test
    fun testValidatorDataflowOnlyIfSink() {
        val resultIfElse = FlowQueriesTest.validatorDataflowOnlyIfSink()
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
                scope = Intraprocedural(),
            )
        assertTrue(
            ifElseResult.value,
            "Whenever we reach baz, we go through print. No call to baz is also fine.",
        )
    }

    @Test
    fun testValidatorDFGSIfElse() {
        val resultIfElseWithB = FlowQueriesTest.validatorDataflowIfElse()
        val ifElseStartAWithB = resultIfElseWithB.variables["a"]
        assertNotNull(ifElseStartAWithB, "There's a variable \"a\" in main")
        val ifElseWithBResult =
            dataFlowWithValidator(
                source = ifElseStartAWithB,
                validatorPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "print"
                },
                sinkPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "baz"
                },
                scope = Intraprocedural(),
            )
        assertTrue(ifElseWithBResult.value, "Both paths go from the variable through print to baz.")
    }

    @Test
    fun testValidatorDFGIfElseWithCall() {
        val resultIfElseWithBInterProc = FlowQueriesTest.validatorDataflowIfElseWithCall()
        val ifElseStartAWithBInterProc = resultIfElseWithBInterProc.variables["a"]
        assertNotNull(ifElseStartAWithBInterProc, "There's a variable \"a\" in main")
        val ifElseWithBResultInterProc =
            dataFlowWithValidator(
                source = ifElseStartAWithBInterProc,
                validatorPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "print"
                },
                sinkPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "baz"
                },
                scope = Intraprocedural(),
            )
        assertFalse(
            ifElseWithBResultInterProc.value,
            "The path using \"b\" cannot be found because we have to go through the function call \"foo\".",
        )
    }

    @Test
    fun testValidatorDFGLinearInterprocedural() {
        val resultLinearWithBInterProc = FlowQueriesTest.validatorDataflowLinearWithCall()
        val linearStartAWithBInterProc = resultLinearWithBInterProc.variables["a"]
        assertNotNull(linearStartAWithBInterProc, "There's a variable \"a\" in main")
        val linearResultWithBInterProc =
            dataFlowWithValidator(
                source = linearStartAWithBInterProc,
                validatorPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "print"
                },
                sinkPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "baz"
                },
                scope = Interprocedural(),
                sensitivities = ContextSensitive + FieldSensitive + FilterUnreachableEOG,
            )
        assertTrue(
            linearResultWithBInterProc.value,
            "There is only one path which goes from the variable through print(b) to baz.",
        )
    }

    @Test
    fun testValidatorDFGIfInterprocedural() {

        val resultIfWithBInterProc = FlowQueriesTest.validatorDataflowIfWithCall()
        val ifStartAWithBInterProc = resultIfWithBInterProc.variables["a"]
        assertNotNull(ifStartAWithBInterProc, "There's a variable \"a\" in main")
        val ifResultWithBInterProc =
            dataFlowWithValidator(
                source = ifStartAWithBInterProc,
                validatorPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "print"
                },
                sinkPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "baz"
                },
                scope = Interprocedural(),
            )
        assertTrue(
            ifResultWithBInterProc.value,
            "There is a path which goes through the \"else\" branch but prints b before reaching baz.",
        )
    }

    @Test
    fun testValidatorDFGIfElseInterprocedural() {
        val resultIfElseWithBInterProc = FlowQueriesTest.validatorDataflowIfElseWithCall()
        val ifElseStartAWithBInterProc = resultIfElseWithBInterProc.variables["a"]
        assertNotNull(ifElseStartAWithBInterProc, "There's a variable \"a\" in main")
        val ifElseWithBResultInterProc =
            dataFlowWithValidator(
                source = ifElseStartAWithBInterProc,
                validatorPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "print"
                },
                sinkPredicate = { node ->
                    (node.astParent as? CallExpression)?.name?.localName == "baz"
                },
                scope = Interprocedural(),
            )
        assertTrue(
            ifElseWithBResultInterProc.value,
            "Both paths go from the variable through print to baz.",
        )
    }

    @Test
    fun testImplicitFlows() {
        val resultVerySimple = FlowQueriesTest.verySimpleDataflow()

        val bazCall = resultVerySimple.calls["baz"]
        assertNotNull(bazCall, "We expect a call to the function \"baz\".")
        val bazArg = bazCall.arguments.singleOrNull()
        assertIs<BinaryOperator>(
            bazArg,
            "The argument of the call to \"baz\" is expected to be the binary operator \"a +  b\".",
        )
        val bazArgA = bazArg.lhs
        assertIs<Reference>(
            bazArgA,
            "The lhs of the argument is expected to be a Reference with name \"a\".",
        )
        assertLocalName(
            "a",
            bazArgA,
            "The lhs of the argument is expected to be a Reference with name \"a\".",
        )
        val explicitFlowResult =
            dataFlow(
                startNode = bazArgA,
                direction = Backward(GraphToFollow.DFG),
                type = May,
                sensitivities = FieldSensitive + ContextSensitive,
                scope = Interprocedural(),
                earlyTermination = null,
                predicate = { (it as? Literal<*>)?.value == "bla" },
            )
        assertFalse(
            explicitFlowResult.value,
            "We expect that there is no explicit data flow between the reference \"a\" and the string literal \"bla\".",
        )

        val implicitFlowResult =
            dataFlow(
                startNode = bazArgA,
                direction = Backward(GraphToFollow.DFG),
                type = May,
                sensitivities = FieldSensitive + ContextSensitive + Implicit,
                scope = Interprocedural(),
                earlyTermination = null,
                predicate = { (it as? Literal<*>)?.value == "bla" },
            )
        assertTrue(
            implicitFlowResult.value,
            "We expect that there is an implicit data flow between the reference \"a\" and the string literal \"bla\".",
        )
    }
}
