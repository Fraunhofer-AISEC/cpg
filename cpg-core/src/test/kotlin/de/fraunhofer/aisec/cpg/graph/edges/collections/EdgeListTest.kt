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
package de.fraunhofer.aisec.cpg.graph.edges.collections

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdge
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdges
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.newLiteral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EdgeListTest {
    @Test
    fun testAddIndex() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(1)
            val node2 = newLiteral(2)
            val node3 = newLiteral(3)
            val node4 = newLiteral(4)

            val list = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)
            list += node2
            list += node3

            assertEquals(2, list.size)
            list.forEachIndexed { i, edge ->
                assertEquals(i, edge.index, "index mismatch $i != ${edge.index}")
            }

            // insert something at position 1, this should shift the existing entries (after the
            // position) + 1
            list.add(1, AstEdge(node1, node4))
            assertEquals(3, list.size)

            // indices should still be in sync afterward
            list.forEachIndexed { i, edge ->
                assertEquals(i, edge.index, "index mismatch $i != ${edge.index}")
            }

            // the order should be node2, node4, node3
            val unwrapped = list.unwrap()
            assertEquals<List<Node>>(listOf(node2, node4, node3), unwrapped)
        }
    }

    @Test
    fun testCompactStorageTransitions() {
        with(TestLanguageFrontend()) {
            val owner = newLiteral(0)
            val n1 = newLiteral(1)
            val n2 = newLiteral(2)
            val n3 = newLiteral(3)

            val list = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = owner)
            assertEquals(0, list.size)

            list += n1
            assertEquals<List<Node>>(listOf(n1), list.unwrap())

            list += n2
            assertEquals<List<Node>>(listOf(n1, n2), list.unwrap())

            list.removeAt(0)
            assertEquals<List<Node>>(listOf(n2), list.unwrap())

            list += n3
            assertEquals<List<Node>>(listOf(n2, n3), list.unwrap())

            list.clear()
            assertEquals(0, list.size)
            assertEquals(emptyList(), list.unwrap())
        }
    }

    @Test
    fun testIdentityBasedOperations() {
        with(TestLanguageFrontend()) {
            val owner = newLiteral(0)
            val target = newLiteral(1)

            val list = owner.nextEOGEdges
            val edge1 = EvaluationOrder(owner, target)
            val edge2 = EvaluationOrder(owner, target)

            list.add(edge1)

            // Identity checks must distinguish object instances.
            assertTrue(list.containsByIdentity(edge1))
            assertFalse(list.containsByIdentity(edge2))

            assertFalse(list.removeByIdentity(edge2))
            assertEquals(1, list.size)

            assertTrue(list.removeByIdentity(edge1))
            assertEquals(0, list.size)
        }
    }
}
