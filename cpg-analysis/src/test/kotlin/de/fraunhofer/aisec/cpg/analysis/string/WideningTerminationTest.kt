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
package de.fraunhofer.aisec.cpg.analysis.string

import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import kotlin.test.*
import kotlinx.coroutines.runBlocking

/**
 * Simulates the "append/prepend in a loop" ascending chains that [StringLattice.widen] must turn
 * into a bounded fixpoint - see the termination sketch on [StringLattice.widen]. This is the
 * property that makes loops and recursion terminate in the (not yet implemented) flow-sensitive
 * evaluator, so it is worth testing well beyond what a real loop unrolling would ever need.
 */
class WideningTerminationTest {
    // Deliberately tighter than the defaults so the ascending chains below hit the widening logic
    // (rather than plain lub's own generous size cap) within a handful of iterations, keeping the
    // test both fast and a precise check of the termination property itself.
    private val lattice = StringLattice(maxTermSize = 16, maxTermDepth = 8, maxUnionSize = 6)

    @Test
    fun testAppendInLoopStabilizes() = runBlocking {
        var current: StringPattern = const("a")
        var stabilizedAt = -1
        for (i in 1..200) {
            val next = lattice.lub(current, concat(current, const("a")))
            val widened = lattice.widen(current, next)
            assertTrue(
                size(widened) <= 200,
                "size must stay bounded, was ${size(widened)} at iteration $i for $widened",
            )
            if (stabilizedAt == -1 && widened == current) {
                stabilizedAt = i
            }
            current = widened
        }
        assertTrue(
            stabilizedAt in 1..5,
            "expected a fixpoint within 5 iterations, got $stabilizedAt",
        )
        assertFixpoint(current)
    }

    @Test
    fun testPrependInLoopStabilizes() = runBlocking {
        var current: StringPattern = const("a")
        var stabilizedAt = -1
        for (i in 1..200) {
            val next = lattice.lub(current, concat(const("x"), current))
            val widened = lattice.widen(current, next)
            assertTrue(
                size(widened) <= 200,
                "size must stay bounded, was ${size(widened)} at iteration $i for $widened",
            )
            if (stabilizedAt == -1 && widened == current) {
                stabilizedAt = i
            }
            current = widened
        }
        assertTrue(
            stabilizedAt in 1..5,
            "expected a fixpoint within 5 iterations, got $stabilizedAt",
        )
    }

    @Test
    fun testAlternatingAppendPrependStabilizes() = runBlocking {
        var current: StringPattern = const("a")
        var stabilizedAt = -1
        for (i in 1..200) {
            val step = if (i % 2 == 0) concat(current, const("a")) else concat(const("x"), current)
            val next = lattice.lub(current, step)
            val widened = lattice.widen(current, next)
            assertTrue(
                size(widened) <= 200,
                "size must stay bounded, was ${size(widened)} at iteration $i for $widened",
            )
            if (stabilizedAt == -1 && widened == current) {
                stabilizedAt = i
            }
            current = widened
        }
        assertTrue(
            stabilizedAt in 1..20,
            "expected a fixpoint within 20 iterations, got $stabilizedAt",
        )
    }

    /** Further widening from [current] against the append-growth step must be a no-op. */
    private suspend fun assertFixpoint(current: StringPattern) {
        val next = lattice.lub(current, concat(current, const("a")))
        val widened = lattice.widen(current, next)
        assertEquals(current, widened, "widen should be a no-op once a fixpoint is reached")
    }

    /**
     * Regression test for the bug found by adversarial review of Phase 1: a `Union`-wrapped growing
     * tail (e.g. from a loop with an `if` that appends a literal on one branch) defeated the old,
     * purely size/depth-gated [StringLattice.widen] - its shape stabilised immediately, but a
     * nested [StringPattern.Unknown] leaf's length interval upper bound grew by roughly one per
     * iteration forever, because the fresh, exact `Unknown` produced by `normalize`'s own
     * auto-collapse (see [StringLattice.widenLeaves]'s KDoc reference on [StringLattice.widen])
     * silently subsumed and discarded the previous iteration's accumulated `Unknown` in the
     * top-level `Union` dedup, without ever invoking [LatticeInterval.widen] on it. Uses the
     * default, generous bounds (unlike the other tests in this class) since the bug was independent
     * of the size/depth thresholds - it is about a leaf value growing forever, not about the term's
     * shape.
     */
    @Test
    fun testUnionWrappedGrowingTailStabilizes() = runBlocking {
        val defaultLattice = StringLattice()
        var current: StringPattern = const("a")
        var stabilizedAt = -1
        for (i in 1..50) {
            val step = union(concat(current, const("a")), const("zzz"))
            val next = defaultLattice.lub(current, step)
            val widened = defaultLattice.widen(current, next)
            if (widened == current) {
                stabilizedAt = i
                break
            }
            current = widened
        }
        assertTrue(
            stabilizedAt in 1..50,
            "expected a fixpoint within 50 iterations, got $stabilizedAt",
        )
    }
}
