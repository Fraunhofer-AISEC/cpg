/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.edges.flows

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.builder.body
import de.fraunhofer.aisec.cpg.graph.builder.call
import de.fraunhofer.aisec.cpg.graph.builder.declare
import de.fraunhofer.aisec.cpg.graph.builder.function
import de.fraunhofer.aisec.cpg.graph.builder.literal
import de.fraunhofer.aisec.cpg.graph.builder.t
import de.fraunhofer.aisec.cpg.graph.builder.`throw`
import de.fraunhofer.aisec.cpg.graph.builder.translationResult
import de.fraunhofer.aisec.cpg.graph.builder.translationUnit
import de.fraunhofer.aisec.cpg.graph.builder.variable
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.newLiteral
import de.fraunhofer.aisec.cpg.graph.newReference
import de.fraunhofer.aisec.cpg.graph.statements.ThrowStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import kotlin.collections.firstOrNull
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class DataflowTest {
    @Test
    fun testDataflows() {
        with(TestLanguageFrontend()) {
            // <node1> -- DFG --> <node2>
            // this should be 1 nextDFG for node1 and 1 prevDFG for node2
            var node1 = newLiteral(value = 1)
            var node2 = newLiteral(value = 1)

            node1.nextDFGEdges += node2
            // should contain 1 prevDFG edge now
            assertEquals(1, node2.prevDFGEdges.size)
            // and it should be the same as the nextDFG of node1
            assertSame(node1.nextDFGEdges.firstOrNull(), node2.prevDFGEdges.firstOrNull())
        }
    }

    @Test
    fun testReferenceTypeListener() {
        with(TestLanguageFrontend()) {
            // <node1> -- DFG --> <node2>
            // this should be 1 nextDFG for node1 and 1 prevDFG for node2
            var node1 = newLiteral(value = 1)
            var node2 = newReference("a")

            node1.nextDFGEdges += node2
            // should contain 1 prevDFG edge now
            assertEquals(1, node2.prevDFGEdges.size)

            // node2 should now be a type observer on node1
            assertContains(node1.typeObservers, node2)
        }
    }

    @Test
    fun testRemove() {
        with(TestLanguageFrontend()) {
            // <node1> -- DFG --> <node2>
            // this should be 1 nextDFG for node1 and 1 prevDFG for node2
            var node1 = newLiteral(value = 1)
            var node2 = newLiteral(value = 1)

            node1.nextDFGEdges += node2
            assertEquals(1, node2.prevDFGEdges.size)

            node1.nextDFGEdges.removeIf { it.end == node2 }
            // should contain 0 prevDFG edge now
            assertEquals(0, node2.prevDFGEdges.size)
        }
    }

    @Test
    fun testClear() {
        with(TestLanguageFrontend()) {
            // <node1> -- DFG --> <node2>
            // this should be 1 nextDFG for node1 and 1 prevDFG for node2
            var node1 = newLiteral(value = 1)
            var node2 = newLiteral(value = 1)

            node1.nextDFGEdges += node2
            assertEquals(1, node2.prevDFGEdges.size)

            node1.nextDFGEdges.clear()
            // should contain 0 prevDFG edge now
            assertEquals(0, node2.prevDFGEdges.size)
        }
    }

    @Test
    fun testRaise() {
        val result =
            TestLanguageFrontend().build {
                translationResult {
                    translationUnit("some.file") {
                        function("foo", t("void")) {
                            body {
                                declare { variable("a", t("short")) { literal(42) } }
                                `throw` { call("SomeError") { /* TODO parameter a */} }
                            }
                        }
                    }
                }
            }

        // Let's assert that we did this correctly
        val main = result.functions[0] // TODO: why can't I use "foo" like with the other tests?
        assertNotNull(main)
        val body = main.body
        assertIs<Block>(body)

        val throwStmt = body.statements.getOrNull(0)
        assertIs<ThrowStatement>(throwStmt)
        assertNotNull(throwStmt.exception)
        val throwCall = throwStmt.exception
        assertIs<CallExpression>(throwCall)

        // TODO dataflow var to call
    }
}
