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

/**
 * Builds a one-sided outlier detector for [compareSequenceSets]'s `detectOutliers` parameter, using
 * the modified z-score / median absolute deviation (MAD) method (Iglewicz & Hoaglin): for every
 * score `x`, `modifiedZ = 0.6745 * (median - x) / mad`, and `x` is flagged whenever
 * `modifiedZ > [threshold]`.
 *
 * This is deliberately one-sided: only abnormally *low* scores are ever flagged (`modifiedZ` is
 * only large when `x < median`). An abnormally *good* match is never a problem worth excluding, so
 * there is no symmetric high-side check -- unlike a plain two-sided z-score/MAD test, which would
 * also flag unusually high values.
 *
 * Two guards make "detect nothing" the safe default whenever the statistics aren't trustworthy, so
 * this never manufactures a false positive out of insufficient or degenerate data:
 * - Fewer than [minSampleSize] scores: too little data to call anything an outlier.
 * - `mad == 0.0` (e.g. a majority of scores tied at the same value, which is common whenever
 *   several traces match perfectly): avoids a division by zero, and more fundamentally, MAD-based
 *   spread is undefined/degenerate once more than half the sample shares one value.
 *
 * [threshold] defaults to `3.5`, the constant Iglewicz & Hoaglin recommend for this method.
 * [minSampleSize] defaults to `5`; below that, a handful of traces is too few to tell a genuine
 * outlier apart from ordinary variance.
 */
fun modifiedZScoreLowOutliers(
    threshold: Double = 3.5,
    minSampleSize: Int = 5,
): (List<Double>) -> Set<Int> = { scores ->
    if (scores.size < minSampleSize) {
        emptySet()
    } else {
        val median = scores.median()
        val mad = scores.map { abs(it - median) }.median()
        if (mad == 0.0) {
            emptySet()
        } else {
            scores.indices
                .filter { i -> scores[i] < median }
                .filter { i -> 0.6745 * (median - scores[i]) / mad > threshold }
                .toSet()
        }
    }
}

/** The median of `this`: the middle element, or the average of the two middle elements. */
private fun List<Double>.median(): Double {
    val sorted = sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
}
