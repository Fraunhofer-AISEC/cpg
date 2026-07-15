/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.edges.collections

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.newLiteral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnwrappedEdgeSetTest {
    @Test
    fun testEquals() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(1)
            val node2 = newLiteral(2)
            val node3 = newLiteral(3)

            node1.nextDFG += node2
            node1.nextDFG += node3

            val dfgSet = node1.nextDFG
            val nodeSet = setOf<Node>(node3, node2)

            assertEquals(nodeSet, dfgSet)
            assertEquals(dfgSet, nodeSet)
        }
    }

    @Test
    fun testCompactStorageTransitions() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(1)
            val node2 = newLiteral(2)
            val node3 = newLiteral(3)

            assertEquals(0, node1.nextDFGEdges.size)
            assertEquals(0, node1.nextDFG.size)

            node1.nextDFG += node2
            assertEquals(1, node1.nextDFGEdges.size)
            assertEquals(setOf<Node>(node2), node1.nextDFG.toSet())
            assertEquals(setOf<Node>(node1), node2.prevDFG.toSet())

            node1.nextDFG += node3
            assertEquals(2, node1.nextDFGEdges.size)
            assertEquals(setOf<Node>(node2, node3), node1.nextDFG.toSet())
            assertEquals(setOf<Node>(node1), node2.prevDFG.toSet())
            assertEquals(setOf<Node>(node1), node3.prevDFG.toSet())

            node1.nextDFG.remove(node2)
            assertEquals(1, node1.nextDFGEdges.size)
            assertEquals(setOf<Node>(node3), node1.nextDFG.toSet())
            assertEquals(0, node2.prevDFG.size)

            node1.nextDFG.clear()
            assertEquals(0, node1.nextDFGEdges.size)
            assertEquals(0, node3.prevDFG.size)
        }
    }

    @Test
    fun testIdentityBasedOperations() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(1)
            val node2 = newLiteral(2)

            val edge1 = Dataflow(node1, node2)
            val edge2 = Dataflow(node1, node2)

            node1.nextDFGEdges.add(edge1)

            // Equality matches by start/end+properties, identity must still distinguish instances.
            assertTrue(node1.nextDFGEdges.contains(edge2))
            assertTrue(node1.nextDFGEdges.containsByIdentity(edge1))
            assertFalse(node1.nextDFGEdges.containsByIdentity(edge2))

            assertFalse(node1.nextDFGEdges.removeByIdentity(edge2))
            assertEquals(1, node1.nextDFGEdges.size)

            assertTrue(node1.nextDFGEdges.removeByIdentity(edge1))
            assertEquals(0, node1.nextDFGEdges.size)
        }
    }
}
