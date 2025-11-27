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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ast.AstNode
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdge
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdges
import de.fraunhofer.aisec.cpg.graph.newLiteral
import kotlin.test.Test
import kotlin.test.assertEquals

class UnwrappedEdgeListTest {
    @Test
    fun testAdd() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(1)
            val node2 = newLiteral(2)
            val node3 = newLiteral(3)

            node1.nextEOGEdges += node2

            // this should trigger "add" of the edge underneath (node1.nextEOGEdges += node3)
            node1.nextEOG += node3

            // should contain 2 nodes now
            assertEquals(2, node1.nextEOGEdges.size)
            assertEquals(2, node1.nextEOG.size)
            // mirroring should also work
            assertEquals(1, node2.prevEOGEdges.size)
            assertEquals(1, node3.prevEOGEdges.size)
            assertEquals(1, node3.prevEOG.size)

            assertEquals(listOf<Node>(node2, node3), node1.nextEOG.subList(0, 2))
            assertEquals(listOf<Node>(node1), node3.prevEOG.subList(0, 1))
        }
    }

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

            // insert something at position 1 (using the unwrapped list), this should shift the
            // existing entries (after the position) + 1
            val unwrapped = list.unwrap()
            unwrapped.add(1, node4)
            assertEquals(3, list.size)

            // indices should still be in sync afterward
            list.forEachIndexed { i, edge ->
                assertEquals(i, edge.index, "index mismatch $i != ${edge.index}")
            }

            // the order should be node2, node4, node3
            assertEquals<List<Node>>(listOf(node2, node4, node3), unwrapped)
        }
    }

    @Test
    fun testRemoveAt() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(1)
            val node2 = newLiteral(2)
            val node3 = newLiteral(3)

            node1.nextEOG += node2
            node1.nextEOG += node3

            assertEquals(2, node1.nextEOGEdges.size)

            // remove the element at index 1 (node3)
            node1.nextEOG.removeAt(1)

            assertEquals(node2, node1.nextEOG.singleOrNull())
        }
    }

    @Test
    fun testIterator() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(1)
            val node2 = newLiteral(2)
            val node3 = newLiteral(3)

            node1.nextEOGEdges += node2

            node1.nextEOG += node3

            val list = node1.nextEOG.toList()
            assertEquals(2, list.size)

            // test our mutable iterator
            val iter = node1.nextEOG.iterator()
            iter.next()
            iter.remove()

            assertEquals(1, node1.nextEOGEdges.size)

            // test our list iterator
            val listIter = node1.nextEOG.listIterator()
            listIter.add(node2)

            assertEquals(2, node1.nextEOGEdges.size)
        }
    }

    @Test
    fun testEquals() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(1)
            val node2 = newLiteral(2)
            val node3 = newLiteral(3)

            node1.nextEOG += node2
            node1.nextEOG += node3

            val eogList = node1.nextEOG
            val nodeList = listOf<Node>(node2, node3)

            assertEquals(nodeList, eogList)
            assertEquals(eogList, nodeList)
        }
    }
}
