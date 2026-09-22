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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.AnnotationMember
import de.fraunhofer.aisec.cpg.graph.Node
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Hand-builds small EOGs (via [AnnotationMember] as a stand-in graph node - same choice as
 * [testBlacklistedNodeDoesNotAbortSuccessorScan] below, see its doc for why) and checks
 * [SccPass.tarjan]'s actual output: the `scc` level [SccPass] stamps onto
 * [EvaluationOrder][de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder] edges that are part
 * of a loop.
 *
 * One non-obvious thing every loop-detection test below depends on: a cycle with no connection to
 * the outside world (no predecessor into it, no successor out of it) never gets labeled at
 * all - [SccPass] only stamps edges adjacent to a loop's entry/exit boundary nodes (the back-edge
 * into the entry, and the continuation edge out of it - not every edge structurally inside the
 * cycle). This matches its real use (basic blocks always sit inside a larger EOG), but means every
 * test here wires in a `start`/`end` node around the loop under test, even though those two nodes
 * are otherwise irrelevant to what's being tested.
 */
class SccPassTest {

    private fun newPass() = SccPass(TranslationContext())

    /** The `scc` level of the edge from this node to [other], or `null` if there isn't one. */
    private fun Node.sccTo(other: Node): Int? = nextEOGEdges.find { it.end == other }?.scc

    /**
     * A straight line has no back-edge at all, so nothing should ever be labeled - the baseline "no
     * false positives" case every other test here implicitly relies on.
     */
    @Test
    fun testStraightLineIsNotLabeled() {
        val start = AnnotationMember()
        val a = AnnotationMember()
        val end = AnnotationMember()
        start.nextEOG.add(a)
        a.nextEOG.add(end)

        newPass().tarjan(start)

        assertNull(start.sccTo(a))
        assertNull(a.sccTo(end))
    }

    /**
     * A pure diamond (branch then merge, no back-edge) must not be mistaken for a loop either -
     * reconverging paths are not a cycle.
     */
    @Test
    fun testDiamondWithoutLoopIsNotLabeled() {
        val start = AnnotationMember()
        val a = AnnotationMember()
        val b = AnnotationMember()
        val end = AnnotationMember()
        start.nextEOG.add(a)
        start.nextEOG.add(b)
        a.nextEOG.add(end)
        b.nextEOG.add(end)

        newPass().tarjan(start)

        assertNull(start.sccTo(a))
        assertNull(start.sccTo(b))
        assertNull(a.sccTo(end))
        assertNull(b.sccTo(end))
    }

    /** A single node that loops back to itself is the smallest possible real SCC. */
    @Test
    fun testSelfLoopIsLabeled() {
        val start = AnnotationMember()
        val a = AnnotationMember()
        val end = AnnotationMember()
        start.nextEOG.add(a)
        a.nextEOG.add(a)
        a.nextEOG.add(end)

        newPass().tarjan(start)

        assertEquals(1, a.sccTo(a))
        assertNull(start.sccTo(a))
        assertNull(a.sccTo(end))
    }

    /** The canonical `while` loop shape: a head with a back-edge from the loop body. */
    @Test
    fun testSimpleLoopIsLabeled() {
        val start = AnnotationMember()
        val head = AnnotationMember()
        val body = AnnotationMember()
        val end = AnnotationMember()
        start.nextEOG.add(head)
        head.nextEOG.add(body)
        head.nextEOG.add(end)
        body.nextEOG.add(head)

        newPass().tarjan(start)

        assertEquals(1, body.sccTo(head))
        assertEquals(1, head.sccTo(body))
        assertNull(start.sccTo(head))
        assertNull(head.sccTo(end))
    }

    /** Same as [testSimpleLoopIsLabeled], but with a 3-node loop body instead of 1 node. */
    @Test
    fun testLongerLoopIsLabeled() {
        val start = AnnotationMember()
        val head = AnnotationMember()
        val b = AnnotationMember()
        val c = AnnotationMember()
        val end = AnnotationMember()
        start.nextEOG.add(head)
        head.nextEOG.add(b)
        head.nextEOG.add(end)
        b.nextEOG.add(c)
        c.nextEOG.add(head)

        newPass().tarjan(start)

        assertEquals(1, c.sccTo(head), "the back-edge closing the loop must be labeled")
        assertEquals(1, head.sccTo(b), "the loop's entry continuation must be labeled")
        assertNull(start.sccTo(head))
        assertNull(head.sccTo(end))
    }

    /**
     * Two independent loops, connected only by a one-way bridge from the first into the second -
     * they must be recognized as two separate SCCs, not merged into one just because the second is
     * reachable from the first.
     */
    @Test
    fun testTwoDisjointLoopsAreNotMerged() {
        val start = AnnotationMember()
        val a = AnnotationMember()
        val b = AnnotationMember()
        val bridge = AnnotationMember()
        val c = AnnotationMember()
        val d = AnnotationMember()
        val end = AnnotationMember()
        start.nextEOG.add(a)
        a.nextEOG.add(b)
        b.nextEOG.add(a)
        a.nextEOG.add(bridge)
        bridge.nextEOG.add(c)
        c.nextEOG.add(d)
        d.nextEOG.add(c)
        c.nextEOG.add(end)

        newPass().tarjan(start)

        assertEquals(1, b.sccTo(a), "loop 1's back-edge")
        assertEquals(1, d.sccTo(c), "loop 2's back-edge")
        assertNull(a.sccTo(bridge), "the bridge out of loop 1 is not part of either loop")
        assertNull(bridge.sccTo(c), "the bridge into loop 2 is not part of either loop")
    }

    /**
     * A loop nested directly inside another: `while (outer) { while (inner) { ... } }`. Both loops
     * must be found, and at different levels - the inner one strictly deeper than the outer one.
     */
    @Test
    fun testNestedLoopHasTwoLevels() {
        val start = AnnotationMember()
        val outer = AnnotationMember()
        val inner = AnnotationMember()
        val innerBody = AnnotationMember()
        val outerBody = AnnotationMember()
        val end = AnnotationMember()
        start.nextEOG.add(outer)
        outer.nextEOG.add(inner)
        outer.nextEOG.add(end)
        inner.nextEOG.add(innerBody)
        inner.nextEOG.add(outerBody)
        innerBody.nextEOG.add(inner)
        outerBody.nextEOG.add(outer)

        newPass().tarjan(start)

        assertEquals(1, outerBody.sccTo(outer), "outer loop's back-edge")
        assertEquals(1, outer.sccTo(inner), "outer loop's entry continuation")
        assertEquals(2, innerBody.sccTo(inner), "inner loop's back-edge, one level deeper")
        assertEquals(2, inner.sccTo(innerBody), "inner loop's entry continuation")
        assertNull(start.sccTo(outer))
        assertNull(outer.sccTo(end))
    }

    /**
     * Three loops nested inside each other: `while (a) { while (b) { while (c) { ... } } }`. Each
     * level must be found at its own, strictly increasing, level.
     */
    @Test
    fun testTripleNestedLoopHasThreeLevels() {
        val start = AnnotationMember()
        val a = AnnotationMember()
        val b = AnnotationMember()
        val c = AnnotationMember()
        val innerBody = AnnotationMember()
        val middleBody = AnnotationMember()
        val outerBody = AnnotationMember()
        val end = AnnotationMember()
        start.nextEOG.add(a)
        a.nextEOG.add(b)
        a.nextEOG.add(end)
        b.nextEOG.add(c)
        b.nextEOG.add(outerBody)
        c.nextEOG.add(innerBody)
        c.nextEOG.add(middleBody)
        innerBody.nextEOG.add(c)
        middleBody.nextEOG.add(b)
        outerBody.nextEOG.add(a)

        newPass().tarjan(start)

        assertEquals(1, outerBody.sccTo(a), "outermost loop's back-edge")
        assertEquals(2, middleBody.sccTo(b), "middle loop's back-edge")
        assertEquals(3, innerBody.sccTo(c), "innermost loop's back-edge")
        assertEquals(1, a.sccTo(b), "outermost loop's entry continuation")
        assertEquals(2, b.sccTo(c), "middle loop's entry continuation")
        assertEquals(3, c.sccTo(innerBody), "innermost loop's entry continuation")
        assertNull(start.sccTo(a))
        assertNull(a.sccTo(end))
    }

    /**
     * Regression test for a bug where [SccPass.tarjan] used `break` instead of `continue` when
     * hitting a blacklisted node while iterating a node's `nextEOG` successors. `break` aborts the
     * whole successor scan on the first blacklisted node, silently dropping any successors that
     * come after it - instead of just skipping that one blacklisted successor and continuing to
     * scan the others.
     *
     * The blacklist is normally only ever non-empty for a nested decomposition (populated by
     * [handleSccRoot] itself when it re-decomposes an SCC one level deeper - see its doc), so we
     * exercise the same code path directly here instead: pre-seed depth 1's [SccPass.TarjanInfo]
     * with a blacklist before calling [SccPass.tarjan] (which always starts at depth 1), bypassing
     * the need for a real nested loop to be parsed from source.
     *
     * We use [AnnotationMember] as a stand-in graph node rather than
     * [BasicBlock][de.fraunhofer.aisec.cpg.graph.overlays.BasicBlock]: `BasicBlock.location` is a
     * computed property that returns a non-null placeholder even when the block is empty, which
     * pushes [de.fraunhofer.aisec.cpg.graph.Node.equals] into structural comparison - two empty
     * `BasicBlock`s then compare as equal to each other, breaking the distinct node identities this
     * test depends on. Any plain [Node][de.fraunhofer.aisec.cpg.graph.Node] subclass with `location
     * == null` correctly falls back to reference equality instead.
     */
    @Test
    fun testBlacklistedNodeDoesNotAbortSuccessorScan() {
        val pass = newPass()

        val bb = AnnotationMember()
        val blacklisted = AnnotationMember()
        val live = AnnotationMember()

        // blacklisted comes first, live comes after it - this ordering is what the `break` bug
        // depended on to drop `live` entirely.
        bb.nextEOG.add(blacklisted)
        bb.nextEOG.add(live)

        val level = 1 // tarjan() always starts at depth 1
        pass.tarjanInfoMap[level] = SccPass.TarjanInfo(listOf(blacklisted))

        pass.tarjan(bb)

        assertTrue(
            live in pass.tarjanInfoMap.getValue(level).visited,
            "live successor after a blacklisted one must still be visited",
        )
    }
}
