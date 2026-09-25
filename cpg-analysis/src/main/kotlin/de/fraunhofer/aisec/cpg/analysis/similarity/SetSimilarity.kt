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
 * Compares two sets of sequences [a] and [b]: for every sequence in [a], the most similar sequence
 * in [b] is found (via [listSimilarity], e.g. [sequenceSimilarity]), and the resulting
 * per-`a`-sequence best-match similarities are combined into one score via [aggregate].
 *
 * This is intentionally *directional*: `compareSequenceSets(a, b)` need not equal
 * `compareSequenceSets(b, a)` (e.g. if [b] is more diverse, every sequence in [a] may find a great
 * partner without the reverse being true). If a symmetric score is needed, compose it at the call
 * site, e.g. `(compareSequenceSets(a, b) + compareSequenceSets(b, a)) / 2`.
 *
 * @param consumeMatches If `false` (default), sequences in [b] can be matched by more than one
 *   sequence in [a] -- every `a`-sequence picks its best match independently, and [b] does not need
 *   to be fully covered. If `true`, a `b`-sequence is removed from consideration once it has been
 *   claimed as someone's best match, i.e. a greedy (order-dependent -- *not* a globally-optimal
 *   Hungarian-style assignment) one-to-one matching.
 * @param aggregate Combines the per-`a`-sequence best-match similarities into the final score.
 *   Defaults to the minimum: the score is driven by the worst-represented sequence in [a], and
 *   sequences in [b] don't need to all be matched. `a.isEmpty()` is defined to score `1.0`
 *   (vacuously similar, nothing to compare).
 */
fun compareSequenceSets(
    a: Set<List<String>>,
    b: Set<List<String>>,
    listSimilarity: (List<String>, List<String>) -> Double,
    consumeMatches: Boolean = false,
    aggregate: (List<Double>) -> Double = { it.minOrNull() ?: 1.0 },
): Double {
    val pool = if (consumeMatches) b.toMutableList() else null

    val bestPerA =
        a.map { la ->
            val candidates = pool ?: b
            val bestB = candidates.maxByOrNull { lb -> listSimilarity(la, lb) }
            if (consumeMatches && bestB != null) pool?.remove(bestB)
            bestB?.let { listSimilarity(la, it) } ?: -1.0
        }

    return aggregate(bestPerA)
}
