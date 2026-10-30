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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.Timeout

class CollectAllPrevDFGNodesTest {

    /**
     * `diamonds` diamonds in a row: every diamond doubles the number of backward paths (2^diamonds
     * in total) but only adds three nodes. Returns the start node and every node with its distance
     * from it.
     */
    private fun diamondChain(diamonds: Int): Pair<Node, Map<Node, Int>> {
        val all = mutableMapOf<Node, Int>()
        var depth = 0
        // Distinct names: nodes compare structurally, and unnamed References would all be equal.
        fun node() =
            Reference().also {
                it.name = Name("n${all.size}")
                all[it] = depth
            }
        var join = node()
        val start = join
        repeat(diamonds) {
            depth++
            val left = node()
            val right = node()
            depth++
            val next = node()
            join.prevDFGEdges.add(left)
            join.prevDFGEdges.add(right)
            left.prevDFGEdges.add(next)
            right.prevDFGEdges.add(next)
            join = next
        }
        return start to all
    }

    @Test
    fun theSliceContainsEveryNodeOnABackwardPath() {
        val (start, all) = diamondChain(3)

        val slice = start.collectAllPrevDFGNodes()

        assertEquals(all.keys, slice.toSet())
        assertEquals(slice.size, slice.toSet().size, "no node twice")
        assertEquals(start, slice.first())
    }

    @Test
    fun nearerNodesComeFirst() {
        val (start, all) = diamondChain(3)

        val depths = start.collectAllPrevDFGNodes().map { all.getValue(it) }

        assertEquals(depths.sorted(), depths, "breadth-first: $depths")
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun exponentiallyManyPathsStayLinear() {
        // ~10^9 backward paths, 91 nodes: enumerating every path never finishes.
        val (start, all) = diamondChain(30)

        assertEquals(all.keys, start.collectAllPrevDFGNodes().toSet())
    }
}
