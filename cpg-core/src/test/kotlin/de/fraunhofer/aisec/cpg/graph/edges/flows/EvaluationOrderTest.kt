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
            // this should be 1 nextDFG for node1 and 1 prevDFG for node2
            var node1 = newLiteral(value = 1)
            var node2 = newLiteral(value = 1)

            node1.nextEOGEdges.add(node2)
            // should contain 1 prevDFG edge now
            assertEquals(1, node2.prevEOGEdges.size)
            // and it should be the same as the nextDFG of node1
            assertSame(node1.nextEOGEdges.firstOrNull(), node2.prevEOGEdges.firstOrNull())

            node1.nextEOGEdges.removeAt(0)
            // should contain 0 prevDFG edge now
            assertEquals(0, node2.prevEOGEdges.size)
        }
    }

    @Test
    fun testClear() {
        with(TestLanguageFrontend()) {
            // <node1> -- EOG --> <node2>
            // this should be 1 nextDFG for node1 and 1 prevDFG for node2
            var node1 = newLiteral(value = 1)
            var node2 = newLiteral(value = 1)

            node1.nextEOGEdges.add(node2)
            assertEquals(1, node2.prevEOGEdges.size)

            node1.nextEOGEdges.clear()
            // should contain 0 prevEOG edge now
            assertEquals(0, node2.prevEOGEdges.size)
        }
    }
}
