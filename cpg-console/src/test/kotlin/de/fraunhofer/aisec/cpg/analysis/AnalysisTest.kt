/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.console.fancyCode
import de.fraunhofer.aisec.cpg.graph.body
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnalysisTest {
    @Test
    fun testOutOfBounds() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/array.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        OutOfBoundsCheck().run(result)
    }

    private fun createArrayOutOfBoundsQuery(
        result: TranslationResult,
        normalEvaluator: Boolean
    ): QueryEvaluation.QueryExpression {
        // Query: forall (n: ArraySubscriptionExpression): max(n.subscriptExpression) <
        // min(n.arrayExpression.refersTo.initializer.dimensions[0])
        // && min(n.subscriptExpression) >= 0
        return forall(result) {
            str = "n: ArraySubscriptionExpression"
            and {
                lt {
                    max {
                        field {
                            str = "n.subscriptExpression"
                            if (!normalEvaluator) evaluator = MultiValueEvaluator()
                        }
                    }
                    min {
                        field {
                            str = "n.arrayExpression.refersTo.initializer.dimensions[0]"
                            if (!normalEvaluator) evaluator = MultiValueEvaluator()
                        }
                    }
                }
                ge {
                    min {
                        field {
                            str = "n.subscriptExpression"
                            if (!normalEvaluator) evaluator = MultiValueEvaluator()
                        }
                    }
                    const(0)
                }
            }
        }
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

        val query = createArrayOutOfBoundsQuery(result, true)
        assertFalse(query.evaluate() as Boolean)
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

        val query = createArrayOutOfBoundsQuery(result, false)
        assertFalse(query.evaluate() as Boolean)
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

        val query = createArrayOutOfBoundsQuery(result, false)
        assertTrue(query.evaluate() as Boolean)
    }

    @Test
    fun testNullPointer() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/Array.java"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        NullPointerCheck().run(result)
    }

    @Test
    fun testAttribute() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/Array.java"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()
        val tu = result.translationUnits.first()

        val main = tu.byNameOrNull<FunctionDeclaration>("Array.main", true)
        assertNotNull(main)
        val call = main.body<CallExpression>(0)

        var code = call.fancyCode(showNumbers = false)

        // assertEquals("obj.\u001B[36mdoSomething\u001B[0m();", code)
        println(code)

        var decl = main.body<DeclarationStatement>(0)
        code = decl.fancyCode(showNumbers = false)
        println(code)

        decl = main.body(1)
        code = decl.fancyCode(showNumbers = false)
        println(code)

        code = main.fancyCode(showNumbers = false)
        println(code)

        code = call.fancyCode(3, true)
        println(code)
    }

    @Test
    fun testNullPointerQuery() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/Array.java"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        // forall (n: Node): n.has_base() => n.base != null
        val query =
            forall("n: HasBase", field("n.base", MultiValueEvaluator()) `!=` const(null), result)

        assertFalse(query.evaluate() as Boolean)
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

        // forall (n: CallExpression): n.invokes.name == "memcpy" => |sizeof(n.arguments[0])| <
        // |sizeof(n.arguments[2])|
        val query =
            forall(
                "n: CallExpression",
                ((field("n.invokesRelationship.name") `==` const("memcpy")) implies
                    (sizeof(field("n.argumentsEdges[0]")) ge sizeof(field("n.argumentsEdges[1]")))),
                result
            )

        assertFalse(query.evaluate() as Boolean)
    }

    @OptIn(ExperimentalGraph::class)
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

        // forall (n: CallExpression): n.invokes.name == "memcpy" => |sizeof(n.arguments[0])| <
        // |sizeof(n.arguments[2])|
        val query =
            forall(
                "n: CallExpression",
                ((field("n.invokesRelationship.name") eq const("memcpy")) implies
                    (sizeof(field("n.argumentsEdges")) ge sizeof(field("n.argumentsEdges[1]")))),
                result
            )

        result.forall2 { n: CallExpression ->
            (field(n.invokes[0].name) eq const("memcpy")) implies
                (sizeof(field(n.arguments[0])) ge sizeof(field(n.arguments[1])))
        }
        assertFalse(query.evaluate() as Boolean)
        println(query.paths)
    }
}
