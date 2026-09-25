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

class LexicographicSimilarityTest {
    private val sim = lexicographicSimilarity()

    private fun assertApprox(expected: Double, actual: Double) =
        assertTrue(abs(expected - actual) < 1e-9, "expected $expected but got $actual")

    // identical labels score 1
    @Test
    fun identicalLabelsScoreOne() {
        assertEquals(1.0, sim("Concept:secret:aes", "Concept:secret:aes"))
    }

    // mismatching concept class scores the mismatch score regardless of attributes
    @Test
    fun classMismatchScoresMismatchScore() {
        assertEquals(-1.0, sim("Concept:secret", "Operation:secret"))
    }

    // one mismatch out of n minus 1 attributes scores (n-2)/(n-1)
    @Test
    fun oneAttributeMismatchScoresPartial() {
        // 4 fields total ("A", "x", "y", "z"), 3 attributes after the class, 1 mismatch (y vs q).
        val result = sim("A:x:y:z", "A:x:q:z")
        assertApprox(2.0 / 3.0, result)
    }

    // class match with no attributes scores 1
    @Test
    fun classMatchNoAttributesScoresOne() {
        assertEquals(1.0, sim("A", "A"))
    }

    // missing attributes on the shorter side count as mismatches
    @Test
    fun missingAttributesCountAsMismatch() {
        // "A:x:y" vs "A:x": class matches, attribute 1 matches (x==x), attribute 2 is present on
        // one side only -> counts as a mismatch. 1 match out of 2 attributes.
        val result = sim("A:x:y", "A:x")
        assertApprox(0.5, result)
    }

    // custom delimiter and mismatch score are honored
    @Test
    fun customDelimiterAndMismatchScoreHonored() {
        val custom = lexicographicSimilarity(delimiter = "|", mismatchScore = 0.0)
        assertEquals(0.0, custom("A|x", "B|x"))
        assertEquals(1.0, custom("A|x", "A|x"))
    }
}
