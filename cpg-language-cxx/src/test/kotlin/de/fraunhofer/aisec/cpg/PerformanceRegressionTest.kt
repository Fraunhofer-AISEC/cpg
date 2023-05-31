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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.newLiteral
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.NumericType
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.junit.jupiter.api.assertTimeout

class PerformanceRegressionTest {
    /**
     * This test demonstrates two performance bottlenecks.
     * * First, we want to make a large initializer list with literals and make sure we parse this
     *   in reasonable time. We had issues with literals and their hashcode when they were inserted
     *   into a set.
     * * Second, we want to make that list essentially a one-liner because we had issues when
     *   populating the [Node.location] property using [CXXLanguageFrontend.getLocationFromRawNode].
     */
    @Test
    fun testParseLargeList() {
        val range = 0..40000
        // intentionally make this one very long line, because we had problems with that
        val string = "static int my_array[] = {" + range.toList().joinToString(", ") + "};"

        val tmp = kotlin.io.path.createTempFile("c_range", ".c")
        tmp.writeText(string)

        // this should not exceed 30 seconds (it takes about 2800ms on a good machine, about
        // 10-20s on GitHub, depending on the slowness of the runner)
        assertTimeout(Duration.of(30, ChronoUnit.SECONDS)) {
            val tu =
                analyzeAndGetFirstTU(listOf(tmp.toFile()), tmp.parent, true) {
                    // No need for parallel processing for a single file. this might make it fast
                    // enough for those special moments where for some reasons the GitHub runners
                    // are slowing down (maybe because of some hidden quota).
                    it.useParallelFrontends(false)
                    it.typeSystemActiveInFrontend(true)
                }
            assertNotNull(tu)
        }
    }

    @Test
    fun testTraversal() {
        with(TestLanguageFrontend()) {
            val tu = TranslationUnitDeclaration()
            val decl = VariableDeclaration()
            val list = InitializerListExpression()

            for (i in 0 until 50000) {
                list.initializerEdges.add(
                    PropertyEdge(
                        list,
                        newLiteral(
                            i,
                            IntegerType("int", 32, CPPLanguage(), NumericType.Modifier.UNSIGNED),
                            null
                        )
                    )
                )
            }

            decl.initializer = list
            tu.addDeclaration(decl)

            // Even on a slow machine, this should not exceed 1 second (it should be more like
            // 200-300ms)
            assertTimeout(Duration.of(1, ChronoUnit.SECONDS)) {
                val b = Benchmark(PerformanceRegressionTest::class.java, "getAstChildren")
                doNothing(tu)
                b.addMeasurement()
            }
        }
    }

    fun doNothing(node: Node) {
        for (child in SubgraphWalker.getAstChildren(node)) {
            doNothing(child)
        }
    }
}
