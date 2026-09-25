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
package de.fraunhofer.aisec.cpg.analysis.similarity

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetSimilarityTest {
    private val exact: (String, String) -> Double = { x, y -> if (x == y) 1.0 else -1.0 }
    private val listSim: (List<String>, List<String>) -> Double = { la, lb ->
        sequenceSimilarity(la, lb, exact)
    }

    private fun assertApprox(expected: Double, actual: Double) =
        assertTrue(abs(expected - actual) < 1e-9, "expected $expected but got $actual")

    // an empty first set is vacuously similar
    @Test
    fun emptyFirstSetIsVacuouslySimilar() {
        assertEquals(1.0, compareSequenceSets(emptySet(), setOf(listOf("A")), listSim))
    }

    // default aggregate is the minimum of the per-a best matches
    @Test
    fun defaultAggregateIsMinOfBestMatches() {
        val a = setOf(listOf("X"), listOf("M"))
        val b = setOf(listOf("X"))

        // ["X"] matches ["X"] perfectly (1.0); ["M"] has nothing to match (-1.0). The default
        // (min) should report the worst-represented sequence in a, i.e. -1.0.
        val result = compareSequenceSets(a, b, listSim)
        assertApprox(-1.0, result)
    }

    // aggregate is configurable
    @Test
    fun aggregateIsConfigurable() {
        val a = setOf(listOf("X"), listOf("M"))
        val b = setOf(listOf("X"))

        val average = compareSequenceSets(a, b, listSim, aggregate = { it.average() })
        assertApprox(0.0, average) // (1.0 + -1.0) / 2
    }

    // by default b-sequences can be matched by more than one a-sequence
    @Test
    fun bSequencesCanMatchMultipleATimes() {
        val a = setOf(listOf("X", "Q"), listOf("X", "R"))
        val b = setOf(listOf("X"), listOf("Z"))

        // Both a-sequences share the same best match in b (["X"]), scoring 0.0 each; nothing
        // forces them to spread out over different b-sequences.
        val result = compareSequenceSets(a, b, listSim)
        assertApprox(0.0, result)
    }

    // consumeMatches forces a greedy one-to-one matching
    @Test
    fun consumeMatchesForcesOneToOneMatching() {
        val a = setOf(listOf("X", "Q"), listOf("X", "R"))
        val b = setOf(listOf("X"), listOf("Z"))

        // With consumeMatches, only the first a-sequence gets the (shared) best match ["X"]; the
        // second is forced onto ["Z"], which is a much worse match.
        val result = compareSequenceSets(a, b, listSim, consumeMatches = true)
        assertApprox(-1.0, result)

        val withoutConsuming = compareSequenceSets(a, b, listSim, consumeMatches = false)
        assertTrue(
            result < withoutConsuming,
            "consuming matches should never score higher than allowing reuse ($result vs $withoutConsuming)",
        )
    }
}
