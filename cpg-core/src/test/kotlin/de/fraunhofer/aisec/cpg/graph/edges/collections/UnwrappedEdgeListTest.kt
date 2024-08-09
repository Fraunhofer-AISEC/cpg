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
import de.fraunhofer.aisec.cpg.graph.newLiteral
import kotlin.test.Test
import kotlin.test.assertEquals

class UnwrappedEdgeListTest {
    @Test
    fun testAdd() {
        with(TestLanguageFrontend()) {
            var node1 = newLiteral(1)
            var node2 = newLiteral(2)
            var node3 = newLiteral(3)

            node1.nextEOGEdges += node2

            // this should trigger add of the edge underneath (node1.nextEOGEdges += node3)
            node1.nextEOG += node3

            // should contain 2 nodes now
            assertEquals(2, node1.nextEOGEdges.size)
            assertEquals(2, node1.nextEOG.size)
            // mirroring should also work
            assertEquals(1, node2.prevEOGEdges.size)
            assertEquals(1, node3.prevEOGEdges.size)
            assertEquals(1, node3.prevEOG.size)
        }
    }

    @Test
    fun testIterator() {
        with(TestLanguageFrontend()) {
            var node1 = newLiteral(1)
            var node2 = newLiteral(2)
            var node3 = newLiteral(3)

            node1.nextEOGEdges += node2

            node1.nextEOG += node3

            var list = node1.nextEOG.toList()
            assertEquals(2, list.size)

            // test our mutable iterator
            var iter = node1.nextEOG.iterator()
            iter.next()
            iter.remove()

            assertEquals(1, node1.nextEOGEdges.size)

            // test our list iterator
            var listIter = node1.nextEOG.listIterator()
            listIter.add(node2)

            assertEquals(2, node1.nextEOGEdges.size)
        }
    }
}
