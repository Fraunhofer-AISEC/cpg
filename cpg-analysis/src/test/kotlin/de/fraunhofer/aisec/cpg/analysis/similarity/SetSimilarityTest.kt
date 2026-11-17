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
        assertEquals(1.0, compareSequenceSets(emptySet(), setOf(listOf("A")), listSim).score)
    }

    // default aggregate is the minimum of the per-a best matches
    @Test
    fun defaultAggregateIsMinOfBestMatches() {
        val a = setOf(listOf("X"), listOf("M"))
        val b = setOf(listOf("X"))

        // ["X"] matches ["X"] perfectly (1.0); ["M"] has nothing to match (-1.0). The default
        // (min) should report the worst-represented sequence in a, i.e. -1.0.
        val result = compareSequenceSets(a, b, listSim)
        assertApprox(-1.0, result.score)
    }

    // aggregate is configurable
    @Test
    fun aggregateIsConfigurable() {
        val a = setOf(listOf("X"), listOf("M"))
        val b = setOf(listOf("X"))

        val average = compareSequenceSets(a, b, listSim, aggregate = { it.average() })
        assertApprox(0.0, average.score) // (1.0 + -1.0) / 2
    }

    // by default b-sequences can be matched by more than one a-sequence
    @Test
    fun bSequencesCanMatchMultipleATimes() {
        val a = setOf(listOf("X", "Q"), listOf("X", "R"))
        val b = setOf(listOf("X"), listOf("Z"))

        // Both a-sequences share the same best match in b (["X"]), scoring 0.0 each; nothing
        // forces them to spread out over different b-sequences.
        val result = compareSequenceSets(a, b, listSim)
        assertApprox(0.0, result.score)
    }

    // consumeMatches forces a greedy one-to-one matching
    @Test
    fun consumeMatchesForcesOneToOneMatching() {
        val a = setOf(listOf("X", "Q"), listOf("X", "R"))
        val b = setOf(listOf("X"), listOf("Z"))

        // With consumeMatches, only the first a-sequence gets the (shared) best match ["X"]; the
        // second is forced onto ["Z"], which is a much worse match.
        val result = compareSequenceSets(a, b, listSim, consumeMatches = true)
        assertApprox(-1.0, result.score)

        val withoutConsuming = compareSequenceSets(a, b, listSim, consumeMatches = false)
        assertTrue(
            result.score < withoutConsuming.score,
            "consuming matches should never score higher than allowing reuse " +
                "(${result.score} vs ${withoutConsuming.score})",
        )
    }

    // by default, no outlier detector is applied, so a statistical outlier still sets the floor
    @Test
    fun defaultDetectOutliersExcludesNothing() {
        val a = setOf(listOf("A"), listOf("B"), listOf("C"), listOf("D"), listOf("M"))
        val b = setOf(listOf("A"), listOf("B"), listOf("C"), listOf("D"))

        // A/B/C/D match perfectly (1.0 each); M has nothing to match (-1.0). Without an
        // outlier detector, the plain min still reports -1.0 and nothing is excluded.
        val result = compareSequenceSets(a, b, listSim)
        assertApprox(-1.0, result.score)
        assertTrue(result.outliers.isEmpty())
    }

    // a supplied detectOutliers excludes the flagged scores from aggregate, and reports them
    @Test
    fun detectOutliersExcludesFlaggedScoresFromTheScoreButReportsThem() {
        val a = setOf(listOf("A"), listOf("B"), listOf("C"), listOf("D"), listOf("M"))
        val b = setOf(listOf("A"), listOf("B"), listOf("C"), listOf("D"))

        // A/B/C/D score 1.0 each; M scores -1.0 (no match). A detector that flags only the
        // single lowest score should leave the min of the remaining (1.0) as the final score,
        // while still reporting M's best match as the excluded outlier.
        val result =
            compareSequenceSets(
                a,
                b,
                listSim,
                detectOutliers = { scores -> setOf(scores.indices.minBy { scores[it] }) },
            )

        assertApprox(1.0, result.score)
        assertEquals(1, result.outliers.size)
        assertEquals(listOf("M"), result.outliers.single().a)
        assertApprox(-1.0, result.outliers.single().score)
    }

    // if detectOutliers flags every score, aggregate falls back to running over all of them
    // instead of a vacuous empty-list result, and outliers is reported as empty
    @Test
    fun detectOutliersFlaggingEverythingFallsBackToAggregatingAllScores() {
        val a = setOf(listOf("X"), listOf("M"))
        val b = setOf(listOf("X"))

        val result =
            compareSequenceSets(
                a,
                b,
                listSim,
                detectOutliers = { scores -> scores.indices.toSet() },
            )

        // Same as the no-outliers-detected case: min(1.0, -1.0) == -1.0, and nothing is reported
        // as excluded since nothing was actually excluded from the score.
        assertApprox(-1.0, result.score)
        assertTrue(result.outliers.isEmpty())
    }
}
