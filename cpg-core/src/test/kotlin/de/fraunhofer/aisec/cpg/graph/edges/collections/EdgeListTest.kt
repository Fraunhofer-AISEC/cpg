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
import de.fraunhofer.aisec.cpg.graph.newLiteral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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
    fun testEquality() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(1)
            val node2 = newLiteral(2)
            val node3 = newLiteral(3)
            val node4 = newLiteral(4)

            // Test equal lists
            val list1 = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)
            list1 += node2
            list1 += node3

            val list2 = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)
            list2 += node2
            list2 += node3

            assertEquals(list1, list2, "Lists with same nodes should be equal")

            // Test different sizes
            val list3 = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)
            list3 += node2

            assertNotEquals(list1, list3, "Lists with different sizes should not be equal")

            // Test different nodes
            val list4 = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)
            list4 += node2
            list4 += node4

            assertNotEquals(list1, list4, "Lists with different nodes should not be equal")

            // Test different order
            val list5 = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)
            list5 += node3
            list5 += node2

            assertNotEquals(list1, list5, "Lists with different order should not be equal")

            // Test empty lists
            val list6 = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)
            val list7 = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)

            assertEquals(list6, list7, "Empty lists should be equal")

            // Test reference equality for nodes (not structural)
            val node2Copy = newLiteral(2)
            val list8 = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)
            list8 += node2Copy
            list8 += node3

            assertNotEquals(
                list1,
                list8,
                "Lists with structurally equal but different node instances should not be equal",
            )
        }
    }

    @Test
    fun testEqualityWithIndex() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(1)
            val node2 = newLiteral(2)
            val node3 = newLiteral(3)

            val list1 = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)
            list1 += node2
            list1 += node3

            val list2 = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)
            list2 += node2
            list2 += node3

            // Edges should have same indices and be equal
            assertEquals(list1[0].index, list2[0].index)
            assertEquals(list1[1].index, list2[1].index)
            assertEquals(list1, list2)
        }
    }

    @Test
    fun testEqualityAfterModification() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(1)
            val node2 = newLiteral(2)
            val node3 = newLiteral(3)

            val list1 = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)
            list1 += node2
            list1 += node3

            val list2 = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)
            list2 += node2

            assertNotEquals(list1, list2, "Lists should not be equal before modification")

            list2 += node3

            assertEquals(list1, list2, "Lists should be equal after adding same node")

            // Remove and check
            list2.remove(list2.last())

            assertNotEquals(list1, list2, "Lists should not be equal after removal")
        }
    }
}
