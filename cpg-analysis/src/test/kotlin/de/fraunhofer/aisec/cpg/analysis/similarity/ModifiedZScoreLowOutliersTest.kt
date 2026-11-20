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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModifiedZScoreLowOutliersTest {

    // a single, clearly abnormal low score among otherwise-tight ones is flagged
    @Test
    fun flagsAGenuineLowOutlier() {
        val scores = listOf(0.95, 0.96, 0.97, 0.94, 0.98, -1.0)

        val outliers = modifiedZScoreLowOutliers()(scores)

        assertEquals(setOf(5), outliers)
    }

    // an abnormally *high* score is never flagged, only low ones
    @Test
    fun neverFlagsAHighOutlier() {
        val scores = listOf(0.1, 0.12, 0.11, 0.09, 0.1, 0.95)

        val outliers = modifiedZScoreLowOutliers()(scores)

        assertTrue(outliers.isEmpty())
    }

    // below minSampleSize, nothing is ever flagged, however extreme
    @Test
    fun detectsNothingBelowMinSampleSize() {
        val scores = listOf(1.0, 1.0, -1.0)

        val outliers = modifiedZScoreLowOutliers(minSampleSize = 5)(scores)

        assertTrue(outliers.isEmpty())
    }

    // when the MAD is zero (a tied majority), nothing is flagged rather than dividing by zero
    @Test
    fun detectsNothingWhenMadIsZero() {
        // median is 1.0, and at least half the deviations from it are 0.0, so mad == 0.0
        val scores = listOf(1.0, 1.0, 1.0, 1.0, 0.2)

        val outliers = modifiedZScoreLowOutliers()(scores)

        assertTrue(outliers.isEmpty())
    }
}
