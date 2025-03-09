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

class EdgeListTest {
    @Test
    fun testAddIndex() {
        with(TestLanguageFrontend()) {
            var node1 = newLiteral(1)
            var node2 = newLiteral(2)
            var node3 = newLiteral(3)
            var node4 = newLiteral(4)

            var list = AstEdges<AstNode, AstEdge<AstNode>>(thisRef = node1)
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
            var unwrapped = list.unwrap()
            assertEquals<List<Node>>(listOf(node2, node4, node3), unwrapped)
        }
    }
}
