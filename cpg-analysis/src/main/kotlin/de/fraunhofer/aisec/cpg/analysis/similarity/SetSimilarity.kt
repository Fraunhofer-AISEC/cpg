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

/**
 * One `a`-sequence's best match against `b` in [compareSequenceSets]: the sequence itself ([a]),
 * its best match in `b` ([b], `null` only when `b` had no candidates at all), and that pair's
 * [compareSequenceSets]-supplied similarity [score].
 */
data class BestMatch(val a: List<String>, val b: List<String>?, val score: Double)

/**
 * [compareSequenceSets]'s result: the [score] actually used, aggregated over every non-outlier best
 * match, and the [outliers] that were excluded from it. [outliers] is empty unless a
 * `detectOutliers` was supplied and found something to exclude -- by default, nothing is ever
 * excluded (see [compareSequenceSets]'s `detectOutliers` parameter).
 */
data class SetSimilarityResult(val score: Double, val outliers: List<BestMatch>)

/**
 * Compares two sets of sequences [a] and [b]: for every sequence in [a], the most similar sequence
 * in [b] is found (via [listSimilarity], e.g. [sequenceSimilarity]). [detectOutliers] then flags
 * which of those per-`a`-sequence best-match scores, if any, are statistical outliers; the
 * remaining ("inlier") scores are combined into the final [SetSimilarityResult.score] via
 * [aggregate], and the flagged ones are returned as [SetSimilarityResult.outliers] -- excluded from
 * the score, but never hidden.
 *
 * This is intentionally *directional*: `compareSequenceSets(a, b)` need not equal
 * `compareSequenceSets(b, a)` (e.g. if [b] is more diverse, every sequence in [a] may find a great
 * partner without the reverse being true). If a symmetric score is needed, compose it at the call
 * site, e.g. `(compareSequenceSets(a, b).score + compareSequenceSets(b, a).score) / 2`.
 *
 * @param consumeMatches If `false` (default), sequences in [b] can be matched by more than one
 *   sequence in [a] -- every `a`-sequence picks its best match independently, and [b] does not need
 *   to be fully covered. If `true`, a `b`-sequence is removed from consideration once it has been
 *   claimed as someone's best match, i.e. a greedy (order-dependent -- *not* a globally-optimal
 *   Hungarian-style assignment) one-to-one matching.
 * @param aggregate Combines the non-outlier per-`a`-sequence best-match similarities into the final
 *   score. Defaults to the minimum: the score is driven by the worst-represented *non-outlier*
 *   sequence in [a], and sequences in [b] don't need to all be matched. `a.isEmpty()` is defined to
 *   score `1.0` (vacuously similar, nothing to compare).
 * @param detectOutliers Given every per-`a`-sequence best-match score, returns the indices (into
 *   that same list, in [a]'s iteration order) considered outliers -- see e.g.
 *   [modifiedZScoreLowOutliers]. Defaults to flagging nothing, i.e. today's plain-[aggregate]
 *   behavior. If this flags *every* score, [aggregate] falls back to running over all of them
 *   instead of a vacuous empty list, and [SetSimilarityResult.outliers] reports empty in that case
 *   too -- nothing was actually excluded from the score, so nothing should be reported as excluded.
 */
fun compareSequenceSets(
    a: Set<List<String>>,
    b: Set<List<String>>,
    listSimilarity: (List<String>, List<String>) -> Double,
    consumeMatches: Boolean = false,
    aggregate: (List<Double>) -> Double = {
        it.minOrNull() ?: 1.0
    }, // what happens if a is empty? vacuously similar, nothing to compare
    detectOutliers: (List<Double>) -> Set<Int> = { emptySet() },
): SetSimilarityResult {
    val pool = if (consumeMatches) b.toMutableList() else null

    val bestPerA =
        a.map { la ->
            val candidates = pool ?: b
            val bestB = candidates.maxByOrNull { lb -> listSimilarity(la, lb) }
            if (consumeMatches && bestB != null) pool?.remove(bestB)
            val score = bestB?.let { listSimilarity(la, it) } ?: -1.0
            BestMatch(la, bestB, score)
        }

    val outlierIndices = detectOutliers(bestPerA.map { it.score })
    val inlierScores = bestPerA.filterIndexed { i, _ -> i !in outlierIndices }.map { it.score }

    return if (inlierScores.isEmpty() && bestPerA.isNotEmpty()) {
        SetSimilarityResult(aggregate(bestPerA.map { it.score }), emptyList())
    } else {
        SetSimilarityResult(aggregate(inlierScores), outlierIndices.map { bestPerA[it] })
    }
}
