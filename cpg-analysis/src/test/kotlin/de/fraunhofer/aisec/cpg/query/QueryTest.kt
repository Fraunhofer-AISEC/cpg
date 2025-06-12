/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.evaluation.MultiValueEvaluator
import de.fraunhofer.aisec.cpg.evaluation.NumberSet
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.testcases.Query
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class QueryTest {
    @Test
    fun testMemcpyTooLargeQuery2() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<CallExpression>(
                { it.name.localName == "memcpy" },
                { sizeof(it.arguments[0]) > sizeof(it.arguments[1]) },
            )

        assertFalse(queryTreeResult.first)

        val queryTreeResult2: QueryTree<Boolean> =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { sizeof(it.arguments[0]) gt sizeof(it.arguments[1]) },
            )

        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())

        // result.calls["memcpy"].all { n -> sizeof(n.arguments[0]) >= sizeof(n.arguments[1]) }
    }

    @Test
    fun testMemcpyTooLargeQuery() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[0].size > it.arguments[1].size
            }
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[0].size gt it.arguments[1].size },
            )

        assertFalse(queryTreeResult2.value)
    }

    @Test
    fun testMemcpyTooLargeQueryImplies() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.allExtended<CallExpression>(
                mustSatisfy = {
                    ("memcpy" eq it.name.localName) implies
                        (lazy { it.arguments[0].size gt it.arguments[1].size })
                }
            )

        assertFalse(queryTreeResult.value)
    }

    @Test
    fun testUseAfterFree() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "free" }) { outer ->
                !executionPath(outer) {
                        (it as? Reference)?.refersTo == (outer.arguments[0] as? Reference)?.refersTo
                    }
                    .value
            }

        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "free" },
                { outer ->
                    not(
                        executionPath(outer) {
                            (it as? Reference)?.refersTo ==
                                (outer.arguments[0] as? Reference)?.refersTo
                        }
                    )
                },
            )

        assertFalse(queryTreeResult2.value)
    }

    @Test
    fun testDoubleFree() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "free" }) { outer ->
                !executionPath(outer) {
                        (it as? CallExpression)?.name?.localName == "free" &&
                            (it.arguments[0] as? Reference)?.refersTo ==
                                (outer.arguments[0] as? Reference)?.refersTo
                    }
                    .value
            }
        assertFalse(queryTreeResult.first)
        println(queryTreeResult.second)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "free" },
                { outer ->
                    not(
                        executionPath(outer) {
                            (it as? CallExpression)?.name?.localName == "free" &&
                                (it.arguments[0] as? Reference)?.refersTo ==
                                    (outer.arguments[0] as? Reference)?.refersTo
                        }
                    )
                },
            )

        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testParameterGreaterThanOrEqualConst() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue >= 11
            }
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue ge 11 },
            )

        assertTrue(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue ge 11 },
            )

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testParameterGreaterThanConst() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue > 11
            }
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue gt 11 },
            )

        assertFalse(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue gt 11 },
            )

        assertFalse(queryTreeResult3.value)
    }

    @Test
    fun testParameterLessThanOrEqualConst() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue <= 11
            }
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue le 11 },
            )

        assertTrue(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue le 11 },
            )

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testParameterEqualsConst() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue?.value == 11
            }
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue eq 11 },
            )

        assertTrue(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue eq 11 },
            )

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testParameterLessThanConst() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue < 11
            }
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue lt 11 },
            )

        assertFalse(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue lt 11 },
            )

        assertFalse(queryTreeResult3.value)
    }

    @Test
    fun testParameterNotEqualsConst() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue?.value != 11
            }
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue ne 11 },
            )

        assertFalse(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue ne 11 },
            )

        assertFalse(queryTreeResult3.value)
    }

    @Test
    fun testParameterIn() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue?.value in listOf(11, 2, 3)
            }
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue IN listOf(11, 2, 3) },
            )

        assertTrue(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue IN listOf(11, 2, 3) },
            )

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testAssign() {
        val result = Query.getAssign()

        val queryTreeResult =
            result.all<AssignmentHolder>(
                mustSatisfy = { it.assignments.all { (it.value() as QueryTree<Number>) < 5 } }
            )
        assertTrue(queryTreeResult.first)

        /*val queryTreeResult2 =
            result.allExtended<AssignmentHolder>(
                mustSatisfy = { it.assignments.all { it.value.invoke() as QueryTree<Number> lt 5 } }
            )

        assertTrue(queryTreeResult2.value)*/
    }

    @Test
    fun testOutOfBoundsQuery() {
        val result = Query.getArray()

        val queryTreeResult =
            result.all<SubscriptExpression>(
                mustSatisfy = {
                    max(it.subscriptExpression) < min(it.arraySize) &&
                        min(it.subscriptExpression) >= 0
                }
            )
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<SubscriptExpression>(
                mustSatisfy = {
                    (max(it.subscriptExpression) lt min(it.arraySize)) and
                        (min(it.subscriptExpression) ge 0)
                }
            )
        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testOutOfBoundsQueryExists() {
        val result = Query.getArray()

        val queryTreeResult =
            result.exists<SubscriptExpression>(
                mustSatisfy = {
                    max(it.subscriptExpression) >= min(it.arraySize) ||
                        min(it.subscriptExpression) < 0
                }
            )
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.existsExtended<SubscriptExpression>(
                mustSatisfy = {
                    (it.subscriptExpression.max ge it.arraySize.min) or
                        (it.subscriptExpression.min lt 0)
                }
            )
        assertTrue(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testOutOfBoundsQuery2() {
        val result = Query.getArray2()

        val queryTreeResult =
            result.all<SubscriptExpression>(
                mustSatisfy = {
                    max(it.subscriptExpression) < min(it.arraySize) &&
                        min(it.subscriptExpression) >= 0
                }
            )
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<SubscriptExpression>(
                mustSatisfy = {
                    (max(it.subscriptExpression) lt min(it.arraySize)) and
                        (min(it.subscriptExpression) ge 0)
                }
            )
        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testOutOfBoundsQuery3() {
        val result = Query.getArray3()

        val queryTreeResult =
            result.all<SubscriptExpression>(
                mustSatisfy = {
                    max(it.subscriptExpression) <
                        min(
                            it.arrayExpression
                                .followPrevFullDFGEdgesUntilHit { node ->
                                    node is NewArrayExpression
                                }
                                .fulfilled
                                .map { it2 ->
                                    (it2.nodes.last() as NewArrayExpression).dimensions[0]
                                }
                        ) && min(it.subscriptExpression) > 0
                }
            )
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<SubscriptExpression>(
                mustSatisfy = {
                    (max(it.subscriptExpression) lt
                        min(
                            it.arrayExpression
                                .followPrevFullDFGEdgesUntilHit { node ->
                                    node is NewArrayExpression
                                }
                                .fulfilled
                                .map { it2 ->
                                    (it2.nodes.last() as NewArrayExpression).dimensions[0]
                                }
                        )) and (min(it.subscriptExpression) ge 0)
                }
            )
        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testOutOfBoundsQueryCorrect() {
        val result = Query.getArrayCorrect()

        val queryTreeResult =
            result.all<SubscriptExpression>(
                mustSatisfy = {
                    val max_sub = max(it.subscriptExpression)
                    val min_dim = min(it.arraySize)
                    val min_sub = min(it.subscriptExpression)
                    return@all max_sub < min_dim && min_sub >= 0
                }
            )
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<SubscriptExpression>(
                mustSatisfy = {
                    val max_sub = max(it.subscriptExpression)
                    val min_dim = min(it.arraySize)
                    val min_sub = min(it.subscriptExpression)
                    return@allExtended (max_sub lt min_dim) and (min_sub ge 0)
                }
            )
        assertTrue(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testDivisionBy0() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<BinaryOperator>(
                { it.operatorCode == "/" },
                { !(it.rhs.evaluate(MultiValueEvaluator()) as NumberSet).maybe(0) },
            )
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<BinaryOperator>(
                { it.operatorCode == "/" },
                { not((it.rhs.evaluate(MultiValueEvaluator()) as NumberSet).maybe(0)) },
            )

        assertFalse(queryTreeResult2.value)
    }

    @Test
    fun testIntOverflowAssignment() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<AssignmentHolder>(
                { it.assignments.all { assign -> assign.target.type.isPrimitive } },
                {
                    it.assignments.any {
                        max(it.value) <= maxSizeOfType(it.target.type) &&
                            min(it.value) >= minSizeOfType(it.target.type)
                    }
                },
            )
        assertFalse(queryTreeResult.first)

        /*
        TODO: This test will not work anymore because we cannot put it into a QueryTree
        val queryTreeResult2 =
            result.allExtended<AssignmentHolder>(
                { it.assignments.all { assign -> assign.target.type.isPrimitive } },
                {
                    QueryTree(it.assignments.any {
                        (max(it.value) le maxSizeOfType(it.target.type)) and
                                (min(it.value) ge minSizeOfType(it.target.type))
                    })
                }
            )

        println(queryTreeResult2.printNicely())
        assertFalse(queryTreeResult2.value)*/
    }

    @Test
    fun testDataFlowRequirement() {
        val result = Query.getDataflow()

        val queryTreeResult =
            result.all<CallExpression>(
                { it.name.localName == "toString" },
                { n1 ->
                    result
                        .all<FunctionDeclaration>(
                            { it.name.localName == "print" },
                            { n2 -> dataFlow(n1) { node -> node == n2.parameters[0] }.value },
                        )
                        .first
                },
            )

        assertTrue(queryTreeResult.first)
        assertEquals(0, queryTreeResult.second.size)

        val queryTreeResultExtended =
            result.allExtended<CallExpression>(
                { it.name.localName == "toString" },
                { n1 ->
                    result.allExtended<FunctionDeclaration>(
                        { it.name.localName == "print" },
                        { n2 -> dataFlow(n1) { node -> node == n2.parameters[0] } },
                    )
                },
            )

        assertTrue(queryTreeResultExtended.value)
        assertEquals(1, queryTreeResultExtended.children.size)

        val queryTreeResult2 =
            result.all<CallExpression>(
                { it.name.localName == "test" },
                { n1 ->
                    result
                        .all<FunctionDeclaration>(
                            { it.name.localName == "print" },
                            { n2 -> dataFlow(n1) { node -> node == n2.parameters[0] }.value },
                        )
                        .first
                },
            )

        assertTrue(queryTreeResult2.first)
        assertEquals(0, queryTreeResult2.second.size)

        val queryTreeResult2Extended =
            result.allExtended<CallExpression>(
                { it.name.localName == "test" },
                { n1 ->
                    result.allExtended<FunctionDeclaration>(
                        { it.name.localName == "print" },
                        { n2 -> dataFlow(n1) { node -> node == n2.parameters[0] } },
                    )
                },
            )

        assertTrue(queryTreeResult2Extended.value)
        assertEquals(1, queryTreeResult2Extended.children.size)
    }

    @Test
    fun testComplexDFGAndEOGRequirement() {
        val result = Query.getComplexDataflow()

        val queryTreeResult =
            result.allExtended<CallExpression>(
                { it.name.localName == "highlyCriticalOperation" },
                { n1 ->
                    n1.arguments[0].allNonLiteralsFlowTo(
                        predicate = { (it as? CallExpression)?.name.toString() == "Logger.log" },
                        allowOverwritingValue = false,
                        scope = Interprocedural(),
                        sensitivities = ContextSensitive + FieldSensitive,
                    )
                },
            )

        println(queryTreeResult.printNicely())
        assertTrue(queryTreeResult.value)
        assertEquals(1, queryTreeResult.children.size)
    }

    @Test
    fun testComplexDFGAndEOGRequirement2() {
        val result = Query.getComplexDataflow2()

        val queryTreeResult =
            result.allExtended<CallExpression>(
                { it.name.localName == "highlyCriticalOperation" },
                { n1 ->
                    n1.arguments[0].allNonLiteralsFlowTo(
                        predicate = { (it as? CallExpression)?.name.toString() == "Logger.log" },
                        allowOverwritingValue = false,
                        scope = Interprocedural(),
                        sensitivities = ContextSensitive + FieldSensitive,
                    )
                },
            )

        println(queryTreeResult.printNicely())
        assertTrue(queryTreeResult.value)
        assertEquals(1, queryTreeResult.children.size)
    }

    @Test
    fun testClomplexDFGAndEOGRequirement3() {
        val result = Query.getComplexDataflow3()

        val queryTreeResult =
            result.allExtended<CallExpression>(
                { it.name.localName == "highlyCriticalOperation" },
                { n1 ->
                    n1.arguments[0].allNonLiteralsFlowTo(
                        predicate = { (it as? CallExpression)?.name.toString() == "Logger.log" },
                        allowOverwritingValue = false,
                        scope = Interprocedural(),
                        sensitivities = ContextSensitive + FieldSensitive + FilterUnreachableEOG,
                    )
                },
            )

        println(queryTreeResult.printNicely())
        assertFalse(queryTreeResult.value)
        assertEquals(1, queryTreeResult.children.size)
    }

    @Test
    fun testNode() {
        with(TestLanguageFrontend()) {
            val lit1 = newLiteral(1)
            val lit2 = newLiteral(2)

            val queryTree1 = lit1.intValue
            assertNotNull(queryTree1)
            assertEquals(lit1, queryTree1.node)
            val queryTree2 = lit2.intValue
            assertNotNull(queryTree2)
            assertEquals(lit2, queryTree2.node)

            val queryTree3 = queryTree1 eq queryTree2
            assertNotNull(queryTree3)
            assertNull(queryTree3.node)

            val tu = newTranslationUnitDeclaration("tu")
            val func1 = newFunctionDeclaration("func1")
            tu.declarations += func1
            val func2 = newFunctionDeclaration("func2")
            tu.declarations += func2
            val func3 = newFunctionDeclaration("func3")
            tu.declarations += func3

            val queryTree4 =
                tu.allExtended<FunctionDeclaration>(
                    mustSatisfy = { QueryTree(true, confidence = AcceptedResult) }
                )
            assertNotNull(queryTree4)
            assertEquals(tu, queryTree4.node)
            assertEquals(listOf(func1, func2, func3), queryTree4.children.map { it.node })
        }
    }
}
