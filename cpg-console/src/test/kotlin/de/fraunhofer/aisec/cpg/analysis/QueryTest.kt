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
package de.fraunhofer.aisec.cpg.analysis

import de.fraunhofer.aisec.cpg.ExperimentalGraph
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.Assignment
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.evaluate
import de.fraunhofer.aisec.cpg.graph.followPrevDFGEdgesUntilHit
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass
import de.fraunhofer.aisec.cpg.query.*
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

@ExperimentalGraph
class QueryTest {
    @Test
    fun testMemcpyTooLargeQuery2() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>(
                { it.name == "memcpy" },
                { sizeof(it.arguments[0]) > sizeof(it.arguments[1]) }
            )

        assertFalse(queryTreeResult.first)

        val mustSatisfy = { it: CallExpression ->
            sizeof(it.arguments[0]) gt sizeof(it.arguments[1])
        }
        val queryTreeResult2: QueryTree<Boolean> =
            result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy)

        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())

        // result.calls.name("memcpy").all { n -> sizeof(n.arguments[0]) >= sizeof(n.arguments[1]) }
    }

    @Test
    fun testMemcpyTooLargeQuery() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name == "memcpy" }) {
                it.arguments[0].size > it.arguments[1].size
            }
        assertFalse(queryTreeResult.first)

        val mustSatisfy = { it: CallExpression -> it.arguments[0].size gt it.arguments[1].size }
        val queryTreeResult2 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy)

        assertFalse(queryTreeResult2.value)
    }

    @Test
    fun testMemcpyTooLargeQueryImplies() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val mustSatisfy = { it: CallExpression ->
            (const("memcpy") eq it.name) implies
                (lazy { it.arguments[0].size gt it.arguments[1].size })
        }
        val queryTreeResult = result.all<CallExpression>(mustSatisfy = mustSatisfy)

        assertFalse(queryTreeResult.value)
    }

    @Test
    fun testDoubleFree() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name == "free" }) { outer ->
                !executionPath(outer) {
                        (it as? DeclaredReferenceExpression)?.refersTo ==
                            (outer.arguments[0] as? DeclaredReferenceExpression)?.refersTo
                    }
                    .value
            }
        assertFalse(queryTreeResult.first)
        println(queryTreeResult.second)

        val mustSatisfy = { outer: CallExpression ->
            not(
                executionPath(outer) {
                    (it as? DeclaredReferenceExpression)?.refersTo ==
                        (outer.arguments[0] as? DeclaredReferenceExpression)?.refersTo
                }
            )
        }
        val queryTreeResult2 = result.all<CallExpression>({ it.name == "free" }, mustSatisfy)

        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testParameterGreaterThanOrEqualConst() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name == "memcpy" }) {
                it.arguments[2].intValue!! >= const(11)
            }
        assertTrue(queryTreeResult.first)

        val mustSatisfy = { it: CallExpression -> it.arguments[2].intValue!! ge 11 }
        val queryTreeResult2 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy)

        assertTrue(queryTreeResult2.value)

        val mustSatisfy2 = { it: CallExpression -> it.arguments[2].intValue!! ge const(11) }
        val queryTreeResult3 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy2)

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testParameterGreaterThanConst() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name == "memcpy" }) {
                it.arguments[2].intValue!! > const(11)
            }
        assertFalse(queryTreeResult.first)

        val mustSatisfy = { it: CallExpression -> it.arguments[2].intValue!! gt 11 }
        val queryTreeResult2 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy)

        assertFalse(queryTreeResult2.value)

        val mustSatisfy2 = { it: CallExpression -> it.arguments[2].intValue!! gt const(11) }
        val queryTreeResult3 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy2)

        assertFalse(queryTreeResult3.value)
    }

    @Test
    fun testParameterLessThanOrEqualConst() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name == "memcpy" }) {
                it.arguments[2].intValue!! <= const(11)
            }
        assertTrue(queryTreeResult.first)

        val mustSatisfy = { it: CallExpression -> it.arguments[2].intValue!! le 11 }
        val queryTreeResult2 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy)

        assertTrue(queryTreeResult2.value)

        val mustSatisfy2 = { it: CallExpression -> it.arguments[2].intValue!! le const(11) }
        val queryTreeResult3 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy2)

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testParameterEqualsConst() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name == "memcpy" }) {
                it.arguments[2].intValue!! == const(11)
            }
        assertTrue(queryTreeResult.first)

        val mustSatisfy = { it: CallExpression -> it.arguments[2].intValue!! eq 11 }
        val queryTreeResult2 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy)

        assertTrue(queryTreeResult2.value)

        val mustSatisfy2 = { it: CallExpression -> it.arguments[2].intValue!! eq const(11) }
        val queryTreeResult3 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy2)

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testParameterLessThanConst() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name == "memcpy" }) {
                it.arguments[2].intValue!! < const(11)
            }
        assertFalse(queryTreeResult.first)

        val mustSatisfy = { it: CallExpression -> it.arguments[2].intValue!! lt 11 }
        val queryTreeResult2 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy)

        assertFalse(queryTreeResult2.value)

        val mustSatisfy2 = { it: CallExpression -> it.arguments[2].intValue!! lt const(11) }
        val queryTreeResult3 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy2)

        assertFalse(queryTreeResult3.value)
    }

    @Test
    fun testParameterNotEqualsConst() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name == "memcpy" }) {
                it.arguments[2].intValue!! != const(11)
            }
        assertFalse(queryTreeResult.first)

        val mustSatisfy = { it: CallExpression -> it.arguments[2].intValue!! ne 11 }
        val queryTreeResult2 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy)

        assertFalse(queryTreeResult2.value)

        val mustSatisfy2 = { it: CallExpression -> it.arguments[2].intValue!! ne const(11) }
        val queryTreeResult3 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy2)

        assertFalse(queryTreeResult3.value)
    }

    @Test
    fun testParameterIn() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<CallExpression>({ it.name == "memcpy" }) {
                it.arguments[2].intValue!!.value in listOf(11, 2, 3)
            }
        assertTrue(queryTreeResult.first)

        val mustSatisfy = { it: CallExpression -> it.arguments[2].intValue!! IN listOf(11, 2, 3) }
        val queryTreeResult2 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy)

        assertTrue(queryTreeResult2.value)

        val mustSatisfy2 = { it: CallExpression ->
            it.arguments[2].intValue!! IN const(listOf(11, 2, 3))
        }
        val queryTreeResult3 = result.all<CallExpression>({ it.name == "memcpy" }, mustSatisfy2)

        assertTrue(queryTreeResult3.value)
    }

    @Test
    fun testAssign() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/assign.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<Assignment>(mustSatisfy = { (it.value.invoke() as QueryTree<Number>) < 5 })
        assertTrue(queryTreeResult.first)

        val mustSatisfy = { it: Assignment -> it.value.invoke() as QueryTree<Number> lt 5 }
        val queryTreeResult2 = result.all<Assignment>(mustSatisfy = mustSatisfy)

        assertTrue(queryTreeResult2.value)
    }

    @Test
    fun testOutOfBoundsQuery() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/array.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<ArraySubscriptionExpression>(
                mustSatisfy = {
                    max(it.subscriptExpression) < min(it.size) && min(it.subscriptExpression) >= 0
                }
            )
        assertFalse(queryTreeResult.first)

        val mustSatisfy = { it: ArraySubscriptionExpression ->
            (max(it.subscriptExpression) lt min(it.size)) and (min(it.subscriptExpression) ge 0)
        }
        val queryTreeResult2 = result.all<ArraySubscriptionExpression>(mustSatisfy = mustSatisfy)
        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testOutOfBoundsQueryExists() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/array.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.exists<ArraySubscriptionExpression>(
                mustSatisfy = {
                    max(it.subscriptExpression) >= min(it.size) || min(it.subscriptExpression) < 0
                }
            )
        assertTrue(queryTreeResult.first)

        val mustSatisfy = { it: ArraySubscriptionExpression ->
            (it.subscriptExpression.max ge it.size.min) or (it.subscriptExpression.min lt 0)
        }
        val queryTreeResult2 = result.exists<ArraySubscriptionExpression>(mustSatisfy = mustSatisfy)
        assertTrue(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testOutOfBoundsQuery2() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/array2.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .registerPass(EdgeCachePass())
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<ArraySubscriptionExpression>(
                mustSatisfy = {
                    max(it.subscriptExpression) < min(it.size) && min(it.subscriptExpression) >= 0
                }
            )
        assertFalse(queryTreeResult.first)

        val mustSatisfy = { it: ArraySubscriptionExpression ->
            (max(it.subscriptExpression) lt min(it.size)) and (min(it.subscriptExpression) ge 0)
        }
        val queryTreeResult2 = result.all<ArraySubscriptionExpression>(mustSatisfy = mustSatisfy)
        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testOutOfBoundsQuery3() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/array3.cpp"))
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
                            ((it.arrayExpression as DeclaredReferenceExpression).refersTo
                                    as VariableDeclaration)
                                .followPrevDFGEdgesUntilHit { node ->
                                    node is ArrayCreationExpression
                                }
                                .map { it2 -> (it2 as ArrayCreationExpression).dimensions[0] }
                        ) && min(it.subscriptExpression) > 0
                }
            )
        assertFalse(queryTreeResult.first)

        val mustSatisfy = { it: ArraySubscriptionExpression ->
            (max(it.subscriptExpression) lt
                min(
                    ((it.arrayExpression as DeclaredReferenceExpression).refersTo
                            as VariableDeclaration)
                        .followPrevDFGEdgesUntilHit { node -> node is ArrayCreationExpression }
                        .map { it2 -> (it2 as ArrayCreationExpression).dimensions[0] }
                )) and (min(it.subscriptExpression) ge 0)
        }
        val queryTreeResult2 = result.all<ArraySubscriptionExpression>(mustSatisfy = mustSatisfy)
        assertFalse(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testOutOfBoundsQueryCorrect() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/array_correct.cpp"))
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
                    val min_dim = min(it.size)
                    val min_sub = min(it.subscriptExpression)
                    return@all max_sub < min_dim && min_sub >= 0
                }
            )
        assertTrue(queryTreeResult.first)

        val mustSatisfy = { it: ArraySubscriptionExpression ->
            val max_sub = max(it.subscriptExpression)
            val min_dim = min(it.size)
            val min_sub = min(it.subscriptExpression)
            (max_sub lt min_dim) and (min_sub ge 0)
        }
        val queryTreeResult2 = result.all<ArraySubscriptionExpression>(mustSatisfy = mustSatisfy)
        assertTrue(queryTreeResult2.value)
        println(queryTreeResult2.printNicely())
    }

    @Test
    fun testDivisionBy0() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/vulnerable.cpp"))
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

        val mustSatisfy = { it: BinaryOperator ->
            not((it.rhs.evaluate(MultiValueEvaluator()) as NumberSet).maybe(0))
        }
        val queryTreeResult2 = result.all<BinaryOperator>({ it.operatorCode == "/" }, mustSatisfy)

        assertFalse(queryTreeResult2.value)
    }

    @Test
    fun testIntOverflowAssignment() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/vulnerable.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val queryTreeResult =
            result.all<Assignment>(
                { it.target?.type?.isPrimitive == true },
                {
                    max(it.value) <= maxSizeOfType(it.target!!.type) &&
                        min(it.value) >= minSizeOfType(it.target!!.type)
                }
            )
        assertFalse(queryTreeResult.first)

        val mustSatisfy = { it: Assignment ->
            (max(it.value) le maxSizeOfType(it.target!!.type)) and
                (min(it.value) ge minSizeOfType(it.target!!.type))
        }
        val queryTreeResult2 =
            result.all<Assignment>({ it.target?.type?.isPrimitive == true }, mustSatisfy)

        println(queryTreeResult2.printNicely())
        assertFalse(queryTreeResult2.value)
    }
}
