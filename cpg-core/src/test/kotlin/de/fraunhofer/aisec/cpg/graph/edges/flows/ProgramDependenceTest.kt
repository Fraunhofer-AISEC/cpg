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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.followPrevPDGUntilHit
import de.fraunhofer.aisec.cpg.graph.newLiteral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProgramDependenceTest {
    @Test
    fun testCombinedAdd() {
        with(TestLanguageFrontend()) {
            // <node1> -- DFG --> <node2>
            // <node1> -- CDG --> <node2>
            var node1 = newLiteral(value = 1)
            var node2 = newLiteral(value = 1)

            node1.nextDFGEdges += node2
            node1.nextCDGEdges.add(node2) { branches = setOf(false) }

            // Add the combined PDG edges. We always to this in an incoming way. This simulates what
            // the PDG pass does.
            // This should result in a combined PDG of 2 edges
            var combined = mutableListOf<Edge<Node>>()
            combined += node2.prevDFGEdges
            combined += node2.prevCDGEdges
            node2.prevPDGEdges += combined

            // Should contain 2 PDG edges now
            assertEquals(2, node2.prevPDGEdges.size)

            // The content should be "equal". We can only do this with a union because we are
            // comparing sets and lists here
            assertEquals(node2.prevPDGEdges.toSet(), node2.prevPDGEdges.union(combined))

            // Assert the mirror property
            assertEquals(node1.nextPDGEdges, node2.prevPDGEdges)
        }
    }

    @Test
    fun testEquals() {
        with(TestLanguageFrontend()) {
            // <node1> -- DFG --> <node2>
            // <node1> -- CDG --> <node2>
            var node1 = newLiteral(value = 1)
            var node2 = newLiteral(value = 1)

            node1.nextDFGEdges += node2
            node1.nextCDGEdges.add(node2) { branches = setOf(false) }

            var dfgEdge = node1.nextDFGEdges.firstOrNull()
            assertNotNull(dfgEdge)

            var cdgEdge = node1.nextCDGEdges.firstOrNull()
            assertNotNull(cdgEdge)

            assertNotEquals<Edge<*>>(dfgEdge, cdgEdge)
        }
    }

    @Test
    fun testFollowPrevPDGUntilHit() {
        with(TestLanguageFrontend()) {
            // node1 -- DFG/CDG --> node2 -- DFG --> node3
            val node1 = newLiteral(value = 1)
            val node2 = newLiteral(value = 2)
            val node3 = newLiteral(value = 3)

            node1.nextDFGEdges += node2
            node1.nextCDGEdges.add(node2) { branches = setOf(false) }
            node2.nextDFGEdges += node3

            // Populate the combined (incoming) PDG edges the way ProgramDependenceGraphPass does.
            val combined2 = mutableListOf<Edge<Node>>()
            combined2 += node2.prevDFGEdges
            combined2 += node2.prevCDGEdges
            node2.prevPDGEdges += combined2

            val combined3 = mutableListOf<Edge<Node>>()
            combined3 += node3.prevDFGEdges
            node3.prevPDGEdges += combined3

            // A backward prev-PDG traversal from node3 must make progress along `edge.start` and
            // reach node1. This is a regression test for the interprocedural end/start mix-up that
            // previously mapped every prev-PDG edge to `edge.end` (i.e. the node itself), so a
            // backward traversal never moved. See the corresponding CDG fix in PR #2816.
            val result = node3.followPrevPDGUntilHit { it === node1 }

            assertTrue(
                result.fulfilled.isNotEmpty(),
                "backward prev-PDG traversal must reach node1 from node3",
            )
            val path = result.fulfilled.first()
            assertEquals(node3, path.nodes.first(), "the path must start at the traversal origin")
            assertEquals(node1, path.nodes.last(), "the path must end at the predicate target")
        }
    }

    @Test
    fun testUnsupported() {
        with(TestLanguageFrontend()) {
            var node1 = newLiteral(value = 1)
            var node2 = newLiteral(value = 1)

            assertFailsWith<UnsupportedOperationException> {
                // We do not allow to "create" new edges, but we can only put existing edges (as in
                // DFG, CDG) in the PDG container
                node1.nextPDGEdges.add(node2)
            }
        }
    }
}
