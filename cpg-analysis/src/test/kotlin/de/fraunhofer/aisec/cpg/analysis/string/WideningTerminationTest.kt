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
}
