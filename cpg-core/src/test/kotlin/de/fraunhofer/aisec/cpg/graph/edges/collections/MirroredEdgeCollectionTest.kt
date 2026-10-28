/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.overlays.BasicBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MirroredEdgeCollectionTest {
    /**
     * Adds many nodes to a single [BasicBlock]. Each addition is mirrored to the node's
     * [de.fraunhofer.aisec.cpg.graph.Node.basicBlockEdges], which checks whether the edge is
     * already present in the basic block's (large) edge list. This used to be a linear scan, making
     * the whole operation quadratic.
     */
    @Test
    fun testLargeMirroredList() {
        with(TestLanguageFrontend()) {
            val count = 50_000
            val nodes = (0 until count).map { newLiteral(it) }

            val bb = BasicBlock()
            nodes.forEach { bb.nodes.add(it) }

            assertEquals(count, bb.nodeEdges.size)
            nodes.forEach { node ->
                assertEquals(1, node.basicBlockEdges.size)
                assertSame(bb, node.basicBlock.single())
            }

            // Adding from the other side must not create duplicates either
            val extra = newLiteral(-1)
            extra.basicBlock.add(bb)
            assertEquals(count + 1, bb.nodeEdges.size)
            assertEquals(1, extra.basicBlockEdges.size)
            assertSame(extra, bb.nodes.last())
        }
    }
}
