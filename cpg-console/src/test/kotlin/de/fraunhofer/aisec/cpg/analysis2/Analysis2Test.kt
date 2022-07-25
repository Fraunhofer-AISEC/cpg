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
package de.fraunhofer.aisec.cpg.analysis2

import de.fraunhofer.aisec.cpg.ExperimentalGraph
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.Assignment
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.followPrevDFGEdgesUntilHit
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ArrayCreationExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ArraySubscriptionExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass
import de.fraunhofer.aisec.cpg.passes.followNextEOG
import de.fraunhofer.aisec.cpg.query.*
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

@ExperimentalGraph
class Analysis2Test {
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
            result.all<CallExpression>({ it.name == "memcpy" }) {
                sizeof(it.arguments[0]) gt sizeof(it.arguments[1])
            }

        assertFalse(queryTreeResult.value)
        println(queryTreeResult.printNicely())

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
                it.arguments[0].size gt it.arguments[1].size
            }

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
                val path =
                    outer.followNextEOG {
                        (it.end as? DeclaredReferenceExpression)?.refersTo ==
                            (outer.arguments[0] as? DeclaredReferenceExpression)?.refersTo
                    }
                return@all const(path?.isEmpty()) eq true
            }

        assertFalse(queryTreeResult.value)
        println(queryTreeResult.printNicely())
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
            result.all<CallExpression>({ it.name == "memcpy" }) { it.arguments[2].intValue!! eq 11 }

        assertTrue(queryTreeResult.value)
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

        val queryTreeResult = result.all<Assignment> { it.value.invoke() as QueryTree<Number> lt 5 }

        assertTrue(queryTreeResult.value)
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
            result.all<ArraySubscriptionExpression> {
                (max(it.subscriptExpression) lt min(it.size)) and (min(it.subscriptExpression) ge 0)
            }
        assertFalse(queryTreeResult.value)
        println(queryTreeResult.printNicely())
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
            result.all<ArraySubscriptionExpression> {
                (max(it.subscriptExpression) lt min(it.size)) and (min(it.subscriptExpression) ge 0)
            }
        assertFalse(queryTreeResult.value)
        println(queryTreeResult.printNicely())
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
            result.all<ArraySubscriptionExpression> {
                (max(it.subscriptExpression) lt
                    min(
                        ((it.arrayExpression as DeclaredReferenceExpression).refersTo
                                as VariableDeclaration)
                            .followPrevDFGEdgesUntilHit { node -> node is ArrayCreationExpression }
                            .map { it2 -> (it2 as ArrayCreationExpression).dimensions[0] }
                    )) and (min(it.subscriptExpression) ge 0)
            }
        assertFalse(queryTreeResult.value)
        println(queryTreeResult)
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
            result.all<ArraySubscriptionExpression> {
                val max_sub = max(it.subscriptExpression)
                val min_dim = min(it.size)
                val min_sub = min(it.subscriptExpression)
                return@all (max_sub lt min_dim) and (min_sub ge 0)
            }
        assertTrue(queryTreeResult.value)
        println(queryTreeResult)
    }
}
