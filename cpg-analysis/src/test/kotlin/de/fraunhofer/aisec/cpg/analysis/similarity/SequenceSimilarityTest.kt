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

class SequenceSimilarityTest {
    /** Exact-match element similarity: `1.0` if equal, `-1.0` otherwise. */
    private val exact: (String, String) -> Double = { x, y -> if (x == y) 1.0 else -1.0 }

    private fun assertApprox(expected: Double, actual: Double) =
        assertTrue(abs(expected - actual) < 1e-9, "expected $expected but got $actual")

    // two empty sequences are identical
    @Test
    fun twoEmptySequencesAreIdentical() {
        assertEquals(1.0, sequenceSimilarity(emptyList(), emptyList(), exact))
    }

    // identical sequences score 1
    @Test
    fun identicalSequencesScoreOne() {
        assertEquals(1.0, sequenceSimilarity(listOf("A", "B", "C"), listOf("A", "B", "C"), exact))
    }

    // one empty and one non-empty sequence score the worst case
    @Test
    fun emptyVsNonEmptyScoresWorstCase() {
        // Every element must be a gap; with the default gapPenalty of -1.0 (equal to the worst
        // mismatch), that normalizes to exactly -1.0.
        assertEquals(-1.0, sequenceSimilarity(listOf("A", "B", "C"), emptyList(), exact))
    }

    // an omission costs less than treating the whole sequence as unrelated
    @Test
    fun omissionCostsLessThanUnrelated() {
        // a = [A, B, C], b = [A, C] (B omitted). Optimal alignment: match A, gap for B, match C.
        val result = sequenceSimilarity(listOf("A", "B", "C"), listOf("A", "C"), exact)
        assertApprox(1.0 / 3.0, result)

        val unrelated = sequenceSimilarity(listOf("A", "B", "C"), listOf("X", "Y"), exact)
        assertTrue(
            result > unrelated,
            "an omission ($result) should score higher than being unrelated ($unrelated)",
        )
    }

    // reordering costs less than being completely unrelated
    @Test
    fun reorderingCostsLessThanUnrelated() {
        // a = [A, B], b = [B, A]: the same elements, fully transposed.
        val reordered = sequenceSimilarity(listOf("A", "B"), listOf("B", "A"), exact)
        assertApprox(-0.5, reordered)

        val unrelated = sequenceSimilarity(listOf("A", "B"), listOf("X", "Y"), exact)
        assertApprox(-1.0, unrelated)
        assertTrue(
            reordered > unrelated,
            "reordering ($reordered) should still score higher than being unrelated ($unrelated)",
        )
    }

    // a softer element similarity produces a softer substitution cost
    @Test
    fun softerElementSimilarityLowersCost() {
        // A partial-credit element similarity should score better than exact-match on a
        // near-but-not-exact substitution.
        val partialCredit: (String, String) -> Double = { x, y -> if (x == y) 1.0 else 0.5 }

        val exactScore = sequenceSimilarity(listOf("A", "B"), listOf("A", "Z"), exact)
        val partialScore = sequenceSimilarity(listOf("A", "B"), listOf("A", "Z"), partialCredit)

        assertTrue(partialScore > exactScore)
    }

    // gapPenalty is configurable
    @Test
    fun gapPenaltyIsConfigurable() {
        val strict =
            sequenceSimilarity(listOf("A", "B", "C"), listOf("A", "C"), exact, gapPenalty = -1.0)
        val lenient =
            sequenceSimilarity(listOf("A", "B", "C"), listOf("A", "C"), exact, gapPenalty = -0.1)

        assertTrue(
            lenient > strict,
            "a smaller (less negative) gap penalty should be more forgiving of omissions",
        )
    }
}
