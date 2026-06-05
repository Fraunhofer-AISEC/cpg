
/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $$$$$$\  $$$$$$$\   $$$$$$\
 * $$  __$$\ $$  __$$\ $$  __$$\
 * $$ /  \__|$$ |  $$ |$$ /  \__|
 * $$ |      $$$$$$$  |$$ |$$$$\
 * $$ |      $$  ____/ $$ |\_$$ |
 * $$ |  $$\ $$ |      $$ |  $$ |
 * \$$$$$   |$$ |      \$$$$$   |
 * \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.evaluation.MultiValueEvaluator
import de.fraunhofer.aisec.cpg.evaluation.NumberSet
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.ArrayConstruction
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Subscription
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
            result.all<Call>(
                { it.name.localName == "memcpy" },
                { sizeof(it.arguments[0]) > sizeof(it.arguments[1]) },
            )

        assertFalse(queryTreeResult.first)

        val queryTreeResult2: QueryTree<Boolean> =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { sizeof(it.arguments[0]) gt sizeof(it.arguments[1]) },
            )

        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testMemcpyTooLargeQuery() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<Call>({ it.name.localName == "memcpy" }) {
                it.arguments[0].size > it.arguments[1].size
            }
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[0].size gt it.arguments[1].size },
            )

        assertFalse(queryTreeResult2.value)
    }

    @Test
    fun testMemcpyTooLargeQueryImplies() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.allExtended<Call>(
                mustSatisfy = {
                    ("memcpy" eq it.name.localName) implies
                        (lazy {
                            it.arguments.getOrNull(0)?.size gt it.arguments.getOrNull(1)?.size
                        })
                }
            )

        assertFalse(queryTreeResult.value)
    }

    @Test
    fun testUseAfterFree() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<Call>({ it.name.localName == "free" }) { outer ->
                !executionPath(outer) {
                        (it as? Reference)?.refersTo == (outer.arguments[0] as? Reference)?.refersTo
                    }
                    .value
            }

        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<Call>(
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
            result.all<Call>({ it.name.localName == "free" }) { outer ->
                !executionPath(outer) {
                        (it as? Call)?.name?.localName == "free" &&
                            (it.arguments[0] as? Reference)?.refersTo ==
                                (outer.arguments[0] as? Reference)?.refersTo
                    }
                    .value
            }
        assertFalse(queryTreeResult.first)
        println(queryTreeResult.second)

        val queryTreeResult2 =
            result.allExtended<Call>(
                { it.name.localName == "free" },
                { outer ->
                    not(
                        executionPath(outer) {
                            (it as? Call)?.name?.localName == "free" &&
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
            result.all<Call>({ it.name.localName == "memcpy" }) { it.arguments[2].intValue >= 11 }
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue ge 11 },
            )

        assertTrue(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue ge 11 },
            )

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testParameterGreaterThanConst() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<Call>({ it.name.localName == "memcpy" }) { it.arguments[2].intValue > 11 }
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue gt 11 },
            )

        assertFalse(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue gt 11 },
            )

        assertFalse(queryTreeResult3.value)
    }

    @Test
    fun testParameterLessThanOrEqualConst() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<Call>({ it.name.localName == "memcpy" }) { it.arguments[2].intValue <= 11 }
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue le 11 },
            )

        assertTrue(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue le 11 },
            )

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testParameterEqualsConst() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<Call>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue?.value == 11
            }
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue eq 11 },
            )

        assertTrue(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue eq 11 },
            )

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testParameterLessThanConst() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<Call>({ it.name.localName == "memcpy" }) { it.arguments[2].intValue < 11 }
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue lt 11 },
            )

        assertFalse(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue lt 11 },
            )

        assertFalse(queryTreeResult3.value)
    }

    @Test
    fun testParameterNotEqualsConst() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<Call>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue?.value != 11
            }
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue ne 11 },
            )

        assertFalse(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue ne 11 },
            )

        assertFalse(queryTreeResult3.value)
    }

    @Test
    fun testParameterIn() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.all<Call>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue?.value in listOf(11, 2, 3)
            }
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<Call>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue IN listOf(11, 2, 3) },
            )

        assertTrue(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<Call>(
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
    }

    @Test
    fun testOutOfBoundsQuery() {
        val result = Query.getArray()

        val queryTreeResult =
            result.all<Subscription>(
                mustSatisfy = {
                    max(it.subscriptExpression) < min(it.arraySize) &&
                        min(it.subscriptExpression) >= 0
                }
            )
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<Subscription>(
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
            result.exists<Subscription>(
                mustSatisfy = {
                    max(it.subscriptExpression) >= min(it.arraySize) ||
                        min(it.subscriptExpression) < 0
                }
            )
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.existsExtended<Subscription>(
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
            result.all<Subscription>(
                mustSatisfy = {
                    max(it.subscriptExpression) < min(it.arraySize) &&
                        min(it.subscriptExpression) >= 0
                }
            )
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<Subscription>(
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
            result.all<Subscription>(
                mustSatisfy = {
                    max(it.subscriptExpression) <
                        min(
                            it.arrayExpression
                                .followPrevFullDFGEdgesUntilHit { node ->
                                    node is ArrayConstruction
                                }
                                .fulfilled
                                .map { it2 ->
                                    (it2.nodes.last() as ArrayConstruction).dimensions[0]
                                }
                        ) && min(it.subscriptExpression) > 0
                }
            )
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<Subscription>(
                mustSatisfy = {
                    (max(it.subscriptExpression) lt
                        min(
                            it.arrayExpression
                                .followPrevFullDFGEdgesUntilHit { node ->
                                    node is ArrayConstruction
                                }
                                .fulfilled
                                .map { it2 ->
                                    (it2.nodes.last() as ArrayConstruction).dimensions[0]
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
            result.all<Subscription>(
                mustSatisfy = {
                    val max_sub = max(it.subscriptExpression)
                    val min_dim = min(it.arraySize)
                    val min_sub = min(it.subscriptExpression)
                    return@all max_sub < min_dim && min_sub >= 0
                }
            )
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<Subscription>(
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
        val result = Query.getDivBy0()

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
    }

    @Test
    fun testDataFlowRequirement() {
        val result = Query.getDataflow()

        val queryTreeResult =
            result.all<Call>(
                { it.name.localName == "toString" },
                { n1 ->
                    result
                        .all<Function>(
                            { it.name.localName == "print" },
                            { n2 -> dataFlow(n1) { node -> node == n2.parameters[0] }.value },
                        )
                        .first
                },
            )

        assertTrue(queryTreeResult.first)
        assertEquals(0, queryTreeResult.second.size)

        val queryTreeResultExtended =
            result.allExtended<Call>(
                { it.name.localName == "toString" },
                { n1 ->
                    result.allExtended<Function>(
                        { it.name.localName == "print" },
                        { n2 -> dataFlow(n1) { node -> node == n2.parameters[0] } },
                    )
                },
            )

        assertTrue(queryTreeResultExtended.value)
        assertEquals(1, queryTreeResultExtended.children.size)

        val queryTreeResult2 =
            result.all<Call>(
                { it.name.localName == "test" },
                { n1 ->
                    result
                        .all<Function>(
                            { it.name.localName == "print" },
                            { n2 -> dataFlow(n1) { node -> node == n2.parameters[0] }.value },
                        )
                        .first
                },
            )

        assertTrue(queryTreeResult2.first)
        assertEquals(0, queryTreeResult2.second.size)

        val queryTreeResult2Extended =
            result.allExtended<Call>(
                { it.name.localName == "test" },
                { n1 ->
                    result.allExtended<Function>(
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
            result.allExtended<Call>(
                { it.name.localName == "highlyCriticalOperation" },
                { n1 ->
                    n1.arguments[0].allNonLiteralsFlowTo(
                        predicate = { (it as? Call)?.name.toString() == "Logger.log" },
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
            result.allExtended<Call>(
                { it.name.localName == "highlyCriticalOperation" },
                { n1 ->
                    n1.arguments[0].allNonLiteralsFlowTo(
                        predicate = { (it as? Call)?.name.toString() == "Logger.log" },
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
            result.allExtended<Call>(
                { it.name.localName == "highlyCriticalOperation" },
                { n1 ->
                    n1.arguments[0].allNonLiteralsFlowTo(
                        predicate = { (it as? Call)?.name.toString() == "Logger.log" },
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
    fun testInherentlyDangerousFunctionGets() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.allExtended<Call>(
                mustSatisfy = { not(it.name.localName eq "gets") }
            )

        assertFalse(queryTreeResult.value)
    }

    @Test
    fun testTimeOfCheckTimeOfUseTOCTOU() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.allExtended<Call>(
                { it.name.localName == "access" },
                { outer ->
                    not(
                        executionPath(outer) {
                            (it as? Call)?.name?.localName == "open" &&
                                (it.arguments.getOrNull(0) as? Reference)?.refersTo ==
                                    (outer.arguments.getOrNull(0) as? Reference)?.refersTo
                        }
                    )
                },
            )

        assertFalse(queryTreeResult.value)
    }

    @Test
    fun testUncheckedPrivilegeDropReturnValue() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.allExtended<Call>(
                { it.name.localName == "setuid" },
                { setuidCall ->
                    dataFlow(setuidCall) { node ->
                        node is BinaryOperator && node.operatorCode in listOf("==", "!=", "<", "<=")
                    }
                }
            )

        assertTrue(queryTreeResult.value)
    }

    @Test
    fun testHardcodedCredentialsQuery() {
        val result = Query.getVulnerable()

        val queryTreeResult =
            result.allExtended<Call>(
                { it.name.localName in listOf("connect", "login", "setCryptographicKey") },
                { cryptoCall ->
                    val passwordArg = cryptoCall.arguments.getOrNull(1)
                    if (passwordArg != null) {
                        val origins = passwordArg.followPrevFullDFGEdgesUntilHit { node ->
                            node is Literal<*>
                        }
                        QueryTree(origins.fulfilled.isEmpty(), operator = GenericQueryOperators.EVALUATE)
                    } else {
                        QueryTree(true, operator = GenericQueryOperators.EVALUATE)
                    }
                }
            )

        assertFalse(queryTreeResult.value)
    }

    @Test
    fun testSqlInjection() {
        val result = Query.getVulnerable()

        val queryTreeResult = result.allExtended<Call>(
            { it.name.localName in listOf("sqlite3_exec", "mysql_query", "PQexec") },
            { queryCall ->
                val queryArg = queryCall.arguments.getOrNull(1)
                if (queryArg != null) {
                    queryArg.allNonLiteralsFlowTo(
                        predicate = { it == queryArg },
                        allowOverwritingValue = true,
                        scope = Interprocedural()
                    )
                } else {
                    QueryTree(true, operator = GenericQueryOperators.EVALUATE)
                }
            }
        )

        assertFalse(queryTreeResult.value)
    }

    @Test
    fun testFormatStringVulnerability() {
        val result = Query.getVulnerable()

        val queryTreeResult = result.allExtended<Call>(
            { it.name.localName in listOf("printf", "sprintf", "fprintf", "syslog") },
            { printfCall ->
                val formatArg = printfCall.arguments.getOrNull(0)
                QueryTree(formatArg !is Reference, operator = GenericQueryOperators.EVALUATE)
            }
        )

        assertFalse(queryTreeResult.value)
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

            val tu = newTranslationUnit("tu")
            val func1 = newFunction("func1")
            tu.declarations += func1
            val func2 = newFunction("func2")
            tu.declarations += func2
            val func3 = newFunction("func3")
            tu.declarations += func3

            val queryTree4 =
                tu.allExtended<Function>(
                    mustSatisfy = { QueryTree(true, operator = GenericQueryOperators.EVALUATE) }
                )
            assertNotNull(queryTree4)
            assertEquals(tu, queryTree4.node)
            assertEquals(listOf(func1, func2, func3), queryTree4.children.map { it.node })
        }
    }
}

```
