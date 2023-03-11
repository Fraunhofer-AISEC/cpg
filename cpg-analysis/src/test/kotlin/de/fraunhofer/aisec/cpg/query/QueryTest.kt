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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.analysis.MultiValueEvaluator
import de.fraunhofer.aisec.cpg.analysis.NumberSet
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class QueryTest {
    @Test
    fun testMemcpyTooLargeQuery2() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>(
                { it.name.localName == "memcpy" },
                { sizeof(it.arguments[0]) > sizeof(it.arguments[1]) }
            )

        assertFalse(queryTreeResult.first)

        val queryTreeResult2: QueryTree<Boolean> =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { sizeof(it.arguments[0]) gt sizeof(it.arguments[1]) }
            )

        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())

        // result.calls["memcpy"].all { n -> sizeof(n.arguments[0]) >= sizeof(n.arguments[1]) }
    }

    @Test
    fun testMemcpyTooLargeQuery() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[0].size > it.arguments[1].size
            }
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[0].size gt it.arguments[1].size }
            )

        assertFalse(queryTreeResult2.value)
    }

    @Test
    fun testMemcpyTooLargeQueryImplies() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.allExtended<CallExpression>(
                mustSatisfy = {
                    (const("memcpy") eq it.name.localName) implies
                        (lazy { it.arguments[0].size gt it.arguments[1].size })
                }
            )

        assertFalse(queryTreeResult.value)
    }

    @Test
    fun testUseAfterFree() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "free" }) { outer ->
                !executionPath(outer) {
                        (it as? DeclaredReferenceExpression)?.refersTo ==
                            (outer.arguments[0] as? DeclaredReferenceExpression)?.refersTo
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
                            (it as? DeclaredReferenceExpression)?.refersTo ==
                                (outer.arguments[0] as? DeclaredReferenceExpression)?.refersTo
                        }
                    )
                }
            )

        assertFalse(queryTreeResult2.value)
    }

    @Test
    fun testDoubleFree() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "free" }) { outer ->
                !executionPath(outer) {
                        (it as? CallExpression)?.name?.localName == "free" &&
                            ((it as? CallExpression)?.arguments?.getOrNull(0)
                                    as? DeclaredReferenceExpression)
                                ?.refersTo ==
                                (outer.arguments[0] as? DeclaredReferenceExpression)?.refersTo
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
                                ((it as? CallExpression)?.arguments?.getOrNull(0)
                                        as? DeclaredReferenceExpression)
                                    ?.refersTo ==
                                    (outer.arguments[0] as? DeclaredReferenceExpression)?.refersTo
                        }
                    )
                }
            )

        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testParameterGreaterThanOrEqualConst() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue!! >= const(11)
            }
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! ge 11 }
            )

        assertTrue(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! ge const(11) }
            )

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testParameterGreaterThanConst() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue!! > const(11)
            }
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! gt 11 }
            )

        assertFalse(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! gt const(11) }
            )

        assertFalse(queryTreeResult3.value)
    }

    @Test
    fun testParameterLessThanOrEqualConst() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue!! <= const(11)
            }
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! le 11 }
            )

        assertTrue(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! le const(11) }
            )

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testParameterEqualsConst() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue!! == const(11)
            }
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! eq 11 }
            )

        assertTrue(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! eq const(11) }
            )

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testParameterLessThanConst() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue!! < const(11)
            }
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! lt 11 }
            )

        assertFalse(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! lt const(11) }
            )

        assertFalse(queryTreeResult3.value)
    }

    @Test
    fun testParameterNotEqualsConst() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue!! != const(11)
            }
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! ne 11 }
            )

        assertFalse(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! ne const(11) }
            )

        assertFalse(queryTreeResult3.value)
    }

    @Test
    fun testParameterIn() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name.localName == "memcpy" }) {
                it.arguments[2].intValue!!.value in listOf(11, 2, 3)
            }
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! IN listOf(11, 2, 3) }
            )

        assertTrue(queryTreeResult2.value)

        val queryTreeResult3 =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { it.arguments[2].intValue!! IN const(listOf(11, 2, 3)) }
            )

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testAssign() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/assign.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<AssignmentHolder>(
                mustSatisfy = {
                    it.assignments.all { (it.value.invoke() as QueryTree<Number>) < 5 }
                }
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
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/array.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<ArraySubscriptionExpression>(
                mustSatisfy = {
                    max(it.subscriptExpression) < min(it.arraySize) &&
                        min(it.subscriptExpression) >= 0
                }
            )
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<ArraySubscriptionExpression>(
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
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/array.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.exists<ArraySubscriptionExpression>(
                mustSatisfy = {
                    max(it.subscriptExpression) >= min(it.arraySize) ||
                        min(it.subscriptExpression) < 0
                }
            )
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.existsExtended<ArraySubscriptionExpression>(
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
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/array2.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .registerPass(EdgeCachePass())
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<ArraySubscriptionExpression>(
                mustSatisfy = {
                    max(it.subscriptExpression) < min(it.arraySize) &&
                        min(it.subscriptExpression) >= 0
                }
            )
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<ArraySubscriptionExpression>(
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
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/array3.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .registerPass(EdgeCachePass())
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<ArraySubscriptionExpression>(
                mustSatisfy = {
                    max(it.subscriptExpression) <
                        min(
                            it.arrayExpression
                                .followPrevDFGEdgesUntilHit { node ->
                                    node is ArrayCreationExpression
                                }
                                .fulfilled
                                .map { it2 ->
                                    (it2.last() as ArrayCreationExpression).dimensions[0]
                                }
                        ) && min(it.subscriptExpression) > 0
                }
            )
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<ArraySubscriptionExpression>(
                mustSatisfy = {
                    (max(it.subscriptExpression) lt
                        min(
                            it.arrayExpression
                                .followPrevDFGEdgesUntilHit { node ->
                                    node is ArrayCreationExpression
                                }
                                .fulfilled
                                .map { it2 ->
                                    (it2.last() as ArrayCreationExpression).dimensions[0]
                                }
                        )) and (min(it.subscriptExpression) ge 0)
                }
            )
        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testOutOfBoundsQueryCorrect() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/array_correct.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .registerPass(EdgeCachePass())
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<ArraySubscriptionExpression>(
                mustSatisfy = {
                    val max_sub = max(it.subscriptExpression)
                    val min_dim = min(it.arraySize)
                    val min_sub = min(it.subscriptExpression)
                    return@all max_sub < min_dim && min_sub >= 0
                }
            )
        assertTrue(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<ArraySubscriptionExpression>(
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
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<BinaryOperator>(
                { it.operatorCode == "/" },
                { !(it.rhs.evaluate(MultiValueEvaluator()) as NumberSet).maybe(0) }
            )
        assertFalse(queryTreeResult.first)

        val queryTreeResult2 =
            result.allExtended<BinaryOperator>(
                { it.operatorCode == "/" },
                { not((it.rhs.evaluate(MultiValueEvaluator()) as NumberSet).maybe(0)) }
            )

        assertFalse(queryTreeResult2.value)
    }

    @Test
    fun testIntOverflowAssignment() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<AssignmentHolder>(
                { it.assignments.all { assign -> assign.target.type.isPrimitive } },
                {
                    it.assignments.any {
                        max(it.value) <= maxSizeOfType(it.target.type) &&
                            min(it.value) >= minSizeOfType(it.target.type)
                    }
                }
            )
        assertFalse(queryTreeResult.first)

        /*
        TODO: This test will not work anymore because we cannot put it into a QueryTree
        val queryTreeResult2 =
            result.allExtended<AssignmentHolder>(
                { it.assignments.all { assign -> assign.target.type.isPrimitive } },
                {
                    QueryTree(it.assignments.any {
                        (max(it.value) le maxSizeOfType(it.target!!.type)) and
                                (min(it.value) ge minSizeOfType(it.target!!.type))
                    })
                }
            )

        println(queryTreeResult2.printNicely())
        assertFalse(queryTreeResult2.value)*/
    }

    @Test
    fun testDataFlowRequirement() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/Dataflow.java"))
                .defaultPasses()
                .defaultLanguages()
                .registerLanguage(JavaLanguage())
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>(
                { it.name.localName == "toString" },
                { n1 ->
                    result
                        .all<FunctionDeclaration>(
                            { it.name.localName == "print" },
                            { n2 -> dataFlow(n1, n2.parameters[0]).value }
                        )
                        .first
                }
            )

        assertTrue(queryTreeResult.first)
        assertEquals(0, queryTreeResult.second.size)

        val queryTreeResultExtended =
            result.allExtended<CallExpression>(
                { it.name.localName == "toString" },
                { n1 ->
                    result.allExtended<FunctionDeclaration>(
                        { it.name.localName == "print" },
                        { n2 -> dataFlow(n1, n2.parameters[0]) }
                    )
                }
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
                            { n2 -> dataFlow(n1 as Node, n2.parameters[0]).value }
                        )
                        .first
                }
            )

        assertTrue(queryTreeResult2.first)
        assertEquals(0, queryTreeResult2.second.size)

        val queryTreeResult2Extended =
            result.allExtended<CallExpression>(
                { it.name.localName == "test" },
                { n1 ->
                    result.allExtended<FunctionDeclaration>(
                        { it.name.localName == "print" },
                        { n2 -> dataFlow(n1 as Node, n2.parameters[0]) }
                    )
                }
            )

        assertTrue(queryTreeResult2Extended.value)
        assertEquals(1, queryTreeResult2Extended.children.size)
    }

    @Test
    fun testComplexDFGAndEOGRequirement() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/ComplexDataflow.java"))
                .defaultPasses()
                .defaultLanguages()
                .registerLanguage(JavaLanguage())
                .registerPass(EdgeCachePass())
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.allExtended<CallExpression>(
                { it.name.localName == "highlyCriticalOperation" },
                { n1 ->
                    val loggingQueryForward =
                        executionPath(n1) {
                            (it as? CallExpression)?.name.toString() == "Logger.log"
                        }
                    val loggingQueryBackwards =
                        executionPathBackwards(n1) {
                            (it as? CallExpression)?.name.toString() == "Logger.log"
                        }
                    val allChildren = loggingQueryForward.children
                    allChildren.addAll(loggingQueryBackwards.children)
                    val allPaths =
                        allChildren
                            .map { (it.value as? List<*>) }
                            .filter { it != null && it.last() is CallExpression }
                    val allCalls = allPaths.map { it?.last() as CallExpression }
                    val dataFlowPaths =
                        allCalls.map {
                            allNonLiteralsFromFlowTo(
                                n1.arguments[0],
                                it.arguments[1],
                                allPaths as List<List<Node>>
                            )
                        }
                    val dataFlowQuery =
                        QueryTree(dataFlowPaths.all { it.value }, dataFlowPaths.toMutableList())

                    return@allExtended (loggingQueryForward or loggingQueryBackwards) and
                        dataFlowQuery
                }
            )

        println(queryTreeResult.printNicely())
        assertTrue(queryTreeResult.value)
        assertEquals(1, queryTreeResult.children.size)
    }

    @Test
    fun testComplexDFGAndEOGRequirement2() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/ComplexDataflow2.java"))
                .defaultPasses()
                .defaultLanguages()
                .registerLanguage(JavaLanguage())
                .registerPass(EdgeCachePass())
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.allExtended<CallExpression>(
                { it.name.localName == "highlyCriticalOperation" },
                { n1 ->
                    val loggingQueryForward =
                        executionPath(n1) {
                            (it as? CallExpression)?.name.toString() == "Logger.log"
                        }
                    val loggingQueryBackwards =
                        executionPathBackwards(n1) {
                            (it as? CallExpression)?.name.toString() == "Logger.log"
                        }
                    val allChildren = loggingQueryForward.children
                    allChildren.addAll(loggingQueryBackwards.children)
                    val allPaths =
                        allChildren
                            .map { (it.value as? List<*>) }
                            .filter { it != null && it.last() is CallExpression }
                    val allCalls = allPaths.map { it?.last() as CallExpression }
                    val dataFlowPaths =
                        allCalls.map {
                            allNonLiteralsFromFlowTo(
                                n1.arguments[0],
                                it.arguments[1],
                                allPaths as List<List<Node>>
                            )
                        }
                    val dataFlowQuery =
                        QueryTree(dataFlowPaths.all { it.value }, dataFlowPaths.toMutableList())

                    return@allExtended (loggingQueryForward or loggingQueryBackwards) and
                        dataFlowQuery
                }
            )

        println(queryTreeResult.printNicely())
        assertTrue(queryTreeResult.value)
        assertEquals(1, queryTreeResult.children.size)
    }

    @Test
    fun testClomplexDFGAndEOGRequirement3() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/query/ComplexDataflow3.java"))
                .defaultPasses()
                .defaultLanguages()
                .registerLanguage(JavaLanguage())
                .registerPass(EdgeCachePass())
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.allExtended<CallExpression>(
                { it.name.localName == "highlyCriticalOperation" },
                { n1 ->
                    val loggingQueryForward =
                        executionPath(
                            n1,
                            { (it as? CallExpression)?.name.toString() == "Logger.log" }
                        )
                    val loggingQueryBackwards =
                        executionPathBackwards(
                            n1,
                            { (it as? CallExpression)?.name.toString() == "Logger.log" }
                        )
                    val allChildren = loggingQueryForward.children
                    allChildren.addAll(loggingQueryBackwards.children)
                    val allPaths =
                        allChildren
                            .map { (it.value as? List<*>) }
                            .filter { it != null && it.last() is CallExpression }
                    val allCalls = allPaths.map { it?.last() as CallExpression }
                    val dataFlowPaths =
                        allCalls.map {
                            allNonLiteralsFromFlowTo(
                                n1.arguments[0],
                                it.arguments[1],
                                allPaths as List<List<Node>>
                            )
                        }
                    val dataFlowQuery =
                        QueryTree(dataFlowPaths.all { it.value }, dataFlowPaths.toMutableList())

                    return@allExtended (loggingQueryForward or loggingQueryBackwards) and
                        dataFlowQuery
                }
            )

        println(queryTreeResult.printNicely())
        assertFalse(queryTreeResult.value)
        assertEquals(1, queryTreeResult.children.size)
    }
}
