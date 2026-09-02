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

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import java.util.IdentityHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the path-free regime-2 variant [followXUntilHitNodes] (and its DFG wrappers).
 *
 * Each test proves that the new API returns exactly the same *set* of frontier predicate-satisfying
 * nodes (by object identity) as the existing `followXUntilHit(findAllPossiblePaths = false,
 * ...).fulfilled.map { it.nodes.last() }`, on hand built graphs that exercise the interesting
 * cases: a diamond (shared node reached two ways → reported once), a match behind another match
 * (frontier pruning), and a cycle (termination).
 */
class FollowXUntilHitNodesTest {

    /** A minimal concrete [Edge] so the synthetic graph can hand real edges to the engine. */
    private class TestEdge(start: Node, end: Node) : Edge<Node>(start, end) {
        override var labels: Set<String> = emptySet()

        override fun clone(): Edge<Node> = TestEdge(start, end)
    }

    /**
     * Builds an `x` next-step callback from a simple identity-keyed adjacency map. Each successor
     * gets a fresh cloned [Context]; no call-stack manipulation, so intraprocedural cycles are cut
     * by the `(node, callStack)` dedup.
     */
    private fun nextStepFrom(
        adjacency: IdentityHashMap<Node, List<Node>>
    ): (
        Node, Context, List<Triple<Node, Edge<Node>?, Context>>, MutableSet<NodePath>,
    ) -> Collection<Triple<Node, Edge<Node>, Context>> = { current, ctx, _, _ ->
        adjacency[current].orEmpty().map { next ->
            Triple(next, TestEdge(current, next) as Edge<Node>, ctx.clone())
        }
    }

    /**
     * Asserts that [followXUntilHitNodes] returns exactly the same frontier node set (by identity)
     * as the endpoint nodes of the fulfilled paths of the MAY [followXUntilHit].
     */
    private fun assertSameFrontier(
        start: Node,
        adjacency: IdentityHashMap<Node, List<Node>>,
        predicate: (Node) -> Boolean,
    ): Set<Node> {
        val x = nextStepFrom(adjacency)

        val expected =
            start
                .followXUntilHit(
                    x = x,
                    collectFailedPaths = false,
                    findAllPossiblePaths = false,
                    continueAfterHit = true,
                    earlyTermination = { _, _ -> false },
                    predicate = predicate,
                )
                .fulfilled
                .map { it.nodes.last() }

        val actual = start.followXUntilHitNodes(x = x, predicate = predicate)

        // Compare as identity sets: the frontier reported once, regardless of the number of paths.
        val expectedSet = identitySetOf<Node>().apply { addAll(expected) }
        assertEquals(
            expectedSet.size,
            actual.size,
            "The path-free variant must report the same number of distinct frontier nodes",
        )
        for (n in actual) {
            assertTrue(expectedSet.any { it === n }, "Unexpected frontier node $n")
        }
        for (n in expectedSet) {
            assertTrue(actual.any { it === n }, "Missing frontier node $n")
        }
        return actual
    }

    /**
     * Diamond: start -> {a, b} -> merge (a hit). `merge` is reachable via two paths but must be
     * reported exactly once.
     */
    @Test
    fun testDiamondReportedOnce() {
        with(TestLanguageFrontend()) {
            val start = newReference("start")
            val a = newReference("a")
            val b = newReference("b")
            val merge = newReference("merge")

            val adj =
                IdentityHashMap<Node, List<Node>>().apply {
                    put(start, listOf(a, b))
                    put(a, listOf(merge))
                    put(b, listOf(merge))
                }

            val frontier = assertSameFrontier(start, adj) { it === merge }
            assertEquals(1, frontier.size)
            assertTrue(frontier.single() === merge)
        }
    }

    /**
     * Match behind a match: start -> hit1 -> hit2, both satisfy the predicate. Frontier pruning
     * means only `hit1` is reported; the deeper `hit2` behind it is NOT (the traversal does not
     * expand past a hit).
     */
    @Test
    fun testMatchBehindMatchNotReported() {
        with(TestLanguageFrontend()) {
            val start = newReference("start")
            val hit1 = newReference("hit1")
            val hit2 = newReference("hit2")

            val adj =
                IdentityHashMap<Node, List<Node>>().apply {
                    put(start, listOf(hit1))
                    put(hit1, listOf(hit2))
                }

            val frontier = assertSameFrontier(start, adj) { it === hit1 || it === hit2 }
            assertEquals(1, frontier.size)
            assertTrue(frontier.single() === hit1, "Only the nearest match is on the frontier")
        }
    }

    /**
     * Two distinct frontier hits reachable from the start, plus a match hidden behind one of them.
     * Both frontier hits are reported; the hidden one is pruned.
     */
    @Test
    fun testTwoFrontierHitsAndOnePruned() {
        with(TestLanguageFrontend()) {
            val start = newReference("start")
            val hitA = newReference("hitA")
            val hitB = newReference("hitB")
            val behindA = newReference("behindA")

            val adj =
                IdentityHashMap<Node, List<Node>>().apply {
                    put(start, listOf(hitA, hitB))
                    put(hitA, listOf(behindA))
                }

            val frontier =
                assertSameFrontier(start, adj) { it === hitA || it === hitB || it === behindA }
            assertEquals(2, frontier.size)
            assertTrue(frontier.any { it === hitA })
            assertTrue(frontier.any { it === hitB })
            assertTrue(frontier.none { it === behindA }, "The match behind hitA must be pruned")
        }
    }

    /**
     * A cycle: start -> loop -> loop (self-edge) and loop -> hit. The `(node, callStack)` dedup
     * must terminate the traversal and still report the single hit.
     */
    @Test
    fun testCycleTerminates() {
        with(TestLanguageFrontend()) {
            val start = newReference("start")
            val loop = newReference("loop")
            val hit = newReference("hit")

            val adj =
                IdentityHashMap<Node, List<Node>>().apply {
                    put(start, listOf(loop))
                    put(loop, listOf(loop, hit))
                }

            val frontier = assertSameFrontier(start, adj) { it === hit }
            assertEquals(1, frontier.size)
            assertTrue(frontier.single() === hit)
        }
    }

    /** The start node itself satisfying the predicate is the only frontier node. */
    @Test
    fun testStartIsHit() {
        with(TestLanguageFrontend()) {
            val start = newReference("start")
            val other = newReference("other")
            val adj = IdentityHashMap<Node, List<Node>>().apply { put(start, listOf(other)) }

            val frontier = assertSameFrontier(start, adj) { it === start }
            assertEquals(1, frontier.size)
            assertTrue(frontier.single() === start)
        }
    }

    /** No node satisfies the predicate → empty frontier. */
    @Test
    fun testNoHit() {
        with(TestLanguageFrontend()) {
            val start = newReference("start")
            val a = newReference("a")
            val adj = IdentityHashMap<Node, List<Node>>().apply { put(start, listOf(a)) }

            val frontier = assertSameFrontier(start, adj) { false }
            assertTrue(frontier.isEmpty())
        }
    }

    /**
     * End-to-end test of the public DFG wrapper on a real DFG graph: node1 -> node2 -> {node3
     * (hit), node4} and node1 -> node4 -> node3 (diamond onto the hit). Compares
     * [followNextFullDFGEdgesUntilHitNodes] against
     * `followNextFullDFGEdgesUntilHit(...).fulfilled.map { it.nodes.last() }`.
     */
    @Test
    fun testPublicDfgWrapperMatchesPathVariant() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(value = 1)
            val node2 = newLiteral(value = 2)
            val node3 = newLiteral(value = 3)
            val node4 = newLiteral(value = 4)

            // node1 -> node2 ; node2 -> node3 ; node2 -> node4 ; node4 -> node3
            // node3 is reachable two ways (diamond) and is the target.
            node1.nextDFGEdges += node2
            node2.nextDFGEdges += node3
            node2.nextDFGEdges += node4
            node4.nextDFGEdges += node3

            val predicate: (Node) -> Boolean = { it === node3 }

            val expected =
                node1
                    .followNextFullDFGEdgesUntilHit(
                        collectFailedPaths = false,
                        findAllPossiblePaths = false,
                        predicate = predicate,
                    )
                    .fulfilled
                    .map { it.nodes.last() }
            val expectedSet = identitySetOf<Node>().apply { addAll(expected) }

            val actual = node1.followNextFullDFGEdgesUntilHitNodes(predicate = predicate)

            assertEquals(expectedSet.size, actual.size)
            assertEquals(1, actual.size, "node3 must be reported exactly once")
            assertTrue(actual.single() === node3)

            // Backward variant sanity check: from node3 back to node1.
            val back = node3.followPrevFullDFGEdgesUntilHitNodes(predicate = { it === node1 })
            assertEquals(1, back.size)
            assertTrue(back.single() === node1)
        }
    }
}
