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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Goto
import de.fraunhofer.aisec.cpg.graph.expressions.Label
import de.fraunhofer.aisec.cpg.graph.expressions.While
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Hand-builds small EOGs and checks [SccPass.tarjan]'s actual output: the `scc` level [SccPass]
 * stamps onto [EvaluationOrder][de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder] edges
 * that are part of a loop.
 *
 * Graphs are wired directly via `nextEOG.add(...)` rather than parsed from source, using whichever
 * real node type ([Block], [While], [Goto], [Label]) best matches the shape under test - e.g. a
 * loop head is a [While], a jump into the middle of a loop is a [Goto]/[Label] pair. Any of these
 * works as a plain, distinct graph node here because none of them override `location` to a non-null
 * computed value: [de.fraunhofer.aisec.cpg.graph.Node.equals] falls back to reference equality
 * whenever `location == null` (see its doc), so two freshly-constructed instances are never
 * mistaken for each other. That does *not* hold for
 * [BasicBlock][de.fraunhofer.aisec.cpg.graph.overlays.BasicBlock], whose `location` is a computed
 * property that returns a non-null placeholder even when empty - two empty `BasicBlock`s would
 * compare equal to each other, which is why none of the tests below use it directly.
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
        val start = Block()
        val a = Block()
        val end = Block()
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
        val start = Block()
        val a = Block()
        val b = Block()
        val end = Block()
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
        val start = Block()
        val a = While()
        val end = Block()
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
        val start = Block()
        val head = While()
        val body = Block()
        val end = Block()
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
        val start = Block()
        val head = While()
        val b = Block()
        val c = Block()
        val end = Block()
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
        val start = Block()
        val a = While()
        val b = Block()
        val bridge = Block()
        val c = While()
        val d = Block()
        val end = Block()
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
        val start = Block()
        val outer = While()
        val inner = While()
        val innerBody = Block()
        val outerBody = Block()
        val end = Block()
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
        val start = Block()
        val a = While()
        val b = While()
        val c = While()
        val innerBody = Block()
        val middleBody = Block()
        val outerBody = Block()
        val end = Block()
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
     * Verifies that a blacklisted node encountered while scanning a node's `nextEOG` successors
     * only skips that one successor - the scan continues to any successors after it, rather than
     * aborting entirely. [SccPass.tarjan]'s blacklist is normally only ever non-empty for a nested
     * decomposition (populated by [handleSccRoot] when it re-decomposes an SCC one level deeper -
     * see its doc), so this exercises the same code path directly: pre-seed depth 1's
     * [SccPass.TarjanInfo] with a blacklist before calling [SccPass.tarjan] (which always starts at
     * depth 1), without needing a real nested loop to produce one.
     */
    @Test
    fun testBlacklistedNodeDoesNotAbortSuccessorScan() {
        val pass = newPass()

        val bb = Block()
        val blacklisted = Block()
        val live = Block()

        // blacklisted comes before live in nextEOG, so a scan that stops at the first blacklisted
        // successor would never reach live.
        bb.nextEOG.add(blacklisted)
        bb.nextEOG.add(live)

        val level = 1 // tarjan() always starts at depth 1
        pass.tarjanInfoMap[level] = SccPass.TarjanInfo(setOf(blacklisted))

        pass.tarjan(bb)

        assertTrue(
            live in pass.tarjanInfoMap.getValue(level).visited,
            "live successor after a blacklisted one must still be visited",
        )
    }

    /**
     * A loop with more than one distinct entry point: `a` is entered directly from `start`, while a
     * `goto` (`d`) jumps directly into `b`, a label in the middle of the loop body. `SccPass` must
     * treat both as loop entries (`loopEntryElements` = `{a, b}`) and still terminate and label the
     * loop correctly - handled by [handleSccRoot] stripping all current entries at once rather than
     * one per level (see its doc).
     *
     * Runs on a background thread with a timeout rather than calling `tarjan` directly: a
     * non-terminating case here would otherwise hang this test (and the whole suite) instead of
     * failing it.
     */
    @Test
    fun testLoopWithTwoEntriesTerminates() {
        val start = Block()
        val a = While()
        val d = Goto()
        val b = Label()
        val c = Block()
        val end = Block()
        start.nextEOG.add(a)
        start.nextEOG.add(d)
        d.nextEOG.add(b)
        a.nextEOG.add(b)
        b.nextEOG.add(c)
        c.nextEOG.add(a)
        c.nextEOG.add(end)

        val thread = Thread { newPass().tarjan(start) }
        thread.isDaemon = true
        thread.start()
        thread.join(5000)

        assertFalse(thread.isAlive, "tarjan() did not terminate within 5s")

        // Boundary edges (outside the SCC, or the direct external-entry edges themselves) must
        // never be labeled.
        assertNull(start.sccTo(a))
        assertNull(d.sccTo(b))
        assertNull(c.sccTo(end))
        // The ring's back-edge into each entry, from its in-SCC predecessor, must be labeled - and
        // since removing either entry leaves a plain path (no real nested loop), nothing should
        // ever be labeled at a deeper level than 1.
        assertEquals(1, c.sccTo(a), "back-edge into entry `a`")
        assertEquals(1, a.sccTo(b), "back-edge into entry `b`")
    }

    /**
     * A loop with three entries and redundant internal connectivity, resembling a computed-goto
     * dispatch loop where every label can jump directly to either of the other two: `x`, `y`, `z`
     * each reach the other two directly, and each is also reachable directly from `start`. Removing
     * any *one* entry still leaves the other two connected in a cycle, so all three must be
     * recognized as entries at once (`loopEntryElements` = `{x, y, z}`) - if [handleSccRoot]
     * instead only stripped one entry per level, this shape would keep re-decomposing and relabel
     * `x`/`y`/`z` edges at ever deeper, spurious levels instead of the single level this loop
     * actually has.
     */
    @Test
    fun testFullyConnectedLoopWithThreeEntriesStaysAtOneLevel() {
        val start = Block()
        val x = Label()
        val y = Label()
        val z = Label()
        val end = Block()
        start.nextEOG.add(x)
        start.nextEOG.add(y)
        start.nextEOG.add(z)
        x.nextEOG.add(y)
        x.nextEOG.add(z)
        y.nextEOG.add(x)
        y.nextEOG.add(z)
        z.nextEOG.add(x)
        z.nextEOG.add(y)
        z.nextEOG.add(end)

        val thread = Thread { newPass().tarjan(start) }
        thread.isDaemon = true
        thread.start()
        thread.join(5000)

        assertFalse(thread.isAlive, "tarjan() did not terminate within 5s")

        assertNull(start.sccTo(x))
        assertNull(start.sccTo(y))
        assertNull(start.sccTo(z))
        assertNull(z.sccTo(end))
        // The key assertion: every edge among x/y/z must stay at level 1, not be relabeled deeper.
        assertEquals(1, x.sccTo(y))
        assertEquals(1, y.sccTo(x))
        assertEquals(1, x.sccTo(z))
        assertEquals(1, z.sccTo(x))
        assertEquals(1, y.sccTo(z))
        assertEquals(1, z.sccTo(y))
    }

    /**
     * A chain of sequential do-while-shaped loops (`b -> c -> b`, where the exit `c` is *not* the
     * entry `b`). Stripping a loop's entry `b` for the nested decomposition leaves `c`, whose exit
     * edge leads out of the loop into the next one. The nested decomposition must stay inside its
     * own SCC: if it followed that exit edge, it would rediscover every later loop as a spurious
     * "nested" loop one level deeper - and since each of those re-decomposes again, the work grows
     * exponentially with the number of loops in the chain.
     */
    @Test
    fun testSequentialLoopsWithExitAfterEntryStayLinear() {
        val loops = 40
        val start = Block()
        val end = Block()
        val heads = List(loops) { Label() }
        val tails = List(loops) { Block() }
        start.nextEOG.add(heads.first())
        for (i in 0 until loops) {
            heads[i].nextEOG.add(tails[i])
            tails[i].nextEOG.add(heads[i])
            tails[i].nextEOG.add(heads.getOrNull(i + 1) ?: end)
        }

        val thread = Thread { newPass().tarjan(start) }
        thread.isDaemon = true
        thread.start()
        thread.join(5000)

        assertFalse(thread.isAlive, "tarjan() did not terminate within 5s")

        for (i in 0 until loops) {
            assertEquals(1, tails[i].sccTo(heads[i]), "loop $i's back-edge")
            assertNull(tails[i].sccTo(heads.getOrNull(i + 1) ?: end), "bridge out of loop $i")
        }
    }
}
