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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.assertTimeout

class WalkerTest : BaseTest() {
    @Test
    fun testWalkerSpeed() {
        // Traversal of about 80.000 nodes should not exceed 1s (on GitHub). On a recently fast
        // machine, such as MacBook M1, this should take about 200-300ms.
        assertTimeout(Duration.of(1500, ChronoUnit.MILLIS)) {
            val bench = Benchmark(WalkerTest::class.java, "Speed of Walker")
            val tu = TranslationUnitDeclaration()

            // Let's build some fake CPG trees with a good amount of classes
            for (i in 0..100) {
                val record = RecordDeclaration()
                record.name = "class${i}"

                // Each class should have a couple of dozen functions
                for (j in 0..20) {
                    val method = MethodDeclaration()
                    method.name = "method${j}"

                    val comp = CompoundStatement()

                    // Each method has a body with contains a fair amount of variable declarations
                    for (k in 0..10) {
                        val stmt = DeclarationStatement()
                        val decl = VariableDeclaration()
                        decl.name = "var${i}"

                        // With a literal initializer
                        val lit = Literal<Int>()
                        lit.value = k
                        decl.initializer = lit

                        stmt.addToPropertyEdgeDeclaration(decl)

                        comp.addStatement(stmt)
                    }

                    method.body = comp

                    record.addMethod(method)
                }

                // And a couple of fields
                for (j in 0..40) {
                    val field = FieldDeclaration()
                    field.name = "field${j}"

                    // With a literal initializer
                    val lit = Literal<Int>()
                    lit.value = j
                    field.initializer = lit

                    record.addField(field)
                }

                tu.addDeclaration(record)
            }

            val flat = SubgraphWalker.flattenAST(tu)

            assertNotNull(flat)

            assertEquals(82619, flat.size)

            log.info("Flat AST has {} nodes", flat.size)

            bench.stop()
        }
    }
}
