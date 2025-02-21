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
import de.fraunhofer.aisec.cpg.graph.*
import kotlin.test.*

class EvaluationOrderTest {
    @Test
    fun testEvaluationOrders() {
        with(TestLanguageFrontend()) {
            // <node1> -- EOG --> <node2>
            // this should be 1 nextEOG for node1 and 1 prevEOG for node2
            val node1 = newLiteral(value = 1)
            val node2 = newLiteral(value = 2)
            node1.nextEOGEdges.add(node2)

            // should contain 1 prevEOG edge now
            assertEquals(1, node2.prevEOGEdges.size)
            // and it should be the same as the nextEOG of node1
            assertSame(node1.nextEOGEdges.firstOrNull(), node2.prevEOGEdges.firstOrNull())

            node1.nextEOGEdges.removeAt(0)
            // should contain 0 prevEOG edge now
            assertEquals(0, node2.prevEOGEdges.size)
        }
    }

    @Test
    fun testClear() {
        with(TestLanguageFrontend()) {
            // <node1> -- EOG --> <node2>
            // this should be 1 nextEOG for node1 and 1 prevEOG for node2
            val node1 = newLiteral(value = 1)
            val node2 = newLiteral(value = 2)

            node1.nextEOGEdges.add(node2)
            assertEquals(1, node2.prevEOGEdges.size)

            node1.nextEOGEdges.clear()
            // should contain 0 prevEOG edge now
            assertEquals(0, node2.prevEOGEdges.size)
        }
    }

    @Test
    fun testInsertBefore() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(value = 1)
            val node2 = newLiteral(value = 2)
            val node3 = newLiteral(value = 3)

            // <node1> -- EOG -->
            //                    <node3>
            // <node2> -- EOG -->
            node1.nextEOGEdges += node3
            node2.nextEOGEdges += node3
            assertEquals(2, node3.prevEOGEdges.size, "node3 should contain 2 prevEOG edges now")
            assertEquals(
                setOf(node1, node2),
                node3.prevEOG.toSet(),
                "node3 should have node1 and node2 as prevEOG",
            )

            // Now let's insert an edge from node4 to node3 "before" node3
            // <node1> -- EOG -->
            //                    <node4> -- EOG --> <node3>
            // <node2> -- EOG -->
            val node4 = newLiteral(value = 4)
            assertTrue(node3.insertNodeBeforeInEOGPath(node4))
            assertEquals(1, node3.prevEOGEdges.size, "node3 should contain 1 prevEOG edge now")
            assertSame(
                node4,
                node3.prevEOGEdges.singleOrNull()?.start,
                "node4 should be the start of the single prevEOG edge of node3",
            )
            assertEquals(2, node4.prevEOGEdges.size, "node4 should contain 2 prevEOG edges now")
            assertEquals(
                setOf(node1, node2),
                node4.prevEOG.toSet(),
                "node4 should have node1 and node2 as prevEOG",
            )
            assertEquals(1, node1.nextEOGEdges.size, "node1 should contain 1 nextEOG edge now")
            assertSame(
                node4,
                node1.nextEOGEdges.singleOrNull()?.end,
                "node4 should be the end of the single nextEOG edge of node1",
            )
        }
    }

    @Test
    fun testInsertAfterward() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(value = 1)
            val node2 = newLiteral(value = 2)
            val node3 = newLiteral(value = 3)

            //         -- EOG --> <node1>
            // <node3>
            //         -- EOG --> <node2>
            //
            node3.nextEOGEdges += node1
            node3.nextEOGEdges += node2
            assertEquals(2, node3.nextEOGEdges.size, "node3 should contain 2 nextEOG edges now")
            assertEquals(
                setOf(node1, node2),
                node3.nextEOG.toSet(),
                "node3 should have node1 and node2 as nextEOG",
            )

            // Now let's insert an edge from node3 to node4 "after" node3
            //                               -- EOG --> <node1>
            // <node3> -- EOG --> <node4>
            //                               -- EOG --> <node2>
            val node4 = newLiteral(value = 4)
            assertTrue(node3.insertNodeAfterwardInEOGPath(node4))
            assertEquals(1, node3.nextEOGEdges.size, "node3 should contain 1 nextEOG edge now")
            assertSame(
                node4,
                node3.nextEOGEdges.singleOrNull()?.end,
                "node4 should be the end of the single nextEOG edge of node3",
            )
            assertEquals(2, node4.nextEOGEdges.size, "node4 should contain 2 nextEOG edges now")
            assertEquals(
                setOf(node1, node2),
                node4.nextEOG.toSet(),
                "node4 should have node1 and node2 as nextEOG",
            )
            assertEquals(1, node1.prevEOGEdges.size, "node1 should contain 1 prevEOG edge now")
            assertSame(
                node4,
                node1.prevEOGEdges.singleOrNull()?.start,
                "node4 should be the start of the single prevEOG edge of node1",
            )
            assertEquals(1, node2.prevEOGEdges.size, "node2 should contain 1 prevEOG edge now")
            assertSame(
                node4,
                node2.prevEOGEdges.singleOrNull()?.start,
                "node4 should be the start of the single prevEOG edge of node2",
            )
        }
    }
}
