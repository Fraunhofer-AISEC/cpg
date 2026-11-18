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
 * One position of a [sequenceAlignment]'s optimal (traceback) path: either a substitution/match
 * (both [a] and [b] non-null), a deletion (only [a], [b] is a gap), or an insertion (only [b], [a]
 * is a gap). [score] is the [sequenceAlignment]'s `elementSimilarity` result for a substitution, or
 * its `gapPenalty` for a gap. In sequence order (index 0 is the alignment's first position).
 */
data class AlignedElement(val a: String?, val b: String?, val score: Double)

/** [sequenceAlignment]'s result: the same normalized [score] [sequenceSimilarity] returns, plus the optimal alignment itself as [elements]. */
data class SequenceAlignmentResult(val score: Double, val elements: List<AlignedElement>)

/**
 * Compares two sequences [a] and [b] with a generalized global sequence alignment (Needleman &
 * Wunsch, 1970): the classic edit-distance recurrence, but with a real-valued [elementSimilarity]
 * (in `[-1.0, 1.0]`, e.g. [lexicographicSimilarity]) instead of a binary match/mismatch, and a
 * configurable [gapPenalty] for insertions/deletions:
 * ```
 * H[i][0] = i * gapPenalty
 * H[0][j] = j * gapPenalty
 * H[i][j] = max(
 *     H[i-1][j-1] + elementSimilarity(a[i-1], b[j-1]),  // substitute/match
 *     H[i-1][j] + gapPenalty,                            // a[i-1] omitted from b
 *     H[i][j-1] + gapPenalty,                             // b[j-1] inserted relative to a
 * )
 * ```
 *
 * This is what gives the comparison its tolerance for omission, insertion and reordering: a
 * missing/extra element only costs [gapPenalty] instead of a full mismatch, and a substitution
 * between two related-but-different elements costs `1 - elementSimilarity(...)` instead of a binary
 * right/wrong. Reordering is *not* free -- alignment is inherently order-preserving, so two
 * transposed elements are scored as a deletion plus an insertion rather than a match -- but it
 * still costs less than treating the whole sequences as unrelated, since everything else still
 * aligns normally.
 *
 * The final `H[n][m]` is normalized by `max(n, m)` (the best possible score for sequences of that
 * length), so the result is roughly comparable across different sequence lengths and lands in (but
 * is not strictly bounded to) `[-1.0, 1.0]`. Two empty sequences are defined to be identical
 * (`1.0`).
 *
 * [gapPenalty] defaults to `-1.0`, the same magnitude as the worst possible [elementSimilarity].
 * This is the classic *uniform-cost* alignment scheme (the same one plain Levenshtein edit distance
 * and the textbook Needleman-Wunsch baseline use, before affine gap costs are introduced): a gap
 * costs exactly as much as pairing two completely unrelated elements. This matters for *global*
 * alignment specifically, since it keeps the aligner from cheaply absorbing a long run of genuinely
 * unrelated extra elements as "free" insertions instead of correctly reporting them as
 * dissimilarity. If this ends up over-penalizing sequences that mostly agree but differ a lot in
 * length, consider an affine scheme (separate, cheaper, per-element extension cost after an initial
 * gap-open cost) instead.
 *
 * Only the final score is kept - the DP table itself is discarded, so the optimal alignment (which
 * element paired with which) isn't recoverable afterwards. Use [sequenceAlignment] instead if that
 * alignment itself is needed.
 */
fun sequenceSimilarity(
    a: List<String>,
    b: List<String>,
    elementSimilarity: (String, String) -> Double,
    gapPenalty: Double = -1.0,
): Double = sequenceAlignment(a, b, elementSimilarity, gapPenalty).score

/**
 * Same global alignment as [sequenceSimilarity], but additionally reconstructs (via traceback
 * through the DP table) and returns the optimal alignment path itself, not just its score - see
 * [SequenceAlignmentResult]. Ties between substitute/delete/insert at a DP cell are broken in that
 * order (substitution preferred, then deletion, then insertion), so the result is deterministic.
 */
fun sequenceAlignment(
    a: List<String>,
    b: List<String>,
    elementSimilarity: (String, String) -> Double,
    gapPenalty: Double = -1.0,
): SequenceAlignmentResult {
    if (a.isEmpty() && b.isEmpty()) return SequenceAlignmentResult(1.0, emptyList())

    val h = Array(a.size + 1) { DoubleArray(b.size + 1) }
    // 0 = substitute (diagonal), 1 = delete (from above, gap in b), 2 = insert (from the left, gap
    // in a) - the move that produced h[i][j], so the optimal path can be traced back afterwards.
    val move = Array(a.size + 1) { IntArray(b.size + 1) }
    for (i in 1..a.size) {
        h[i][0] = i * gapPenalty
        move[i][0] = 1
    }
    for (j in 1..b.size) {
        h[0][j] = j * gapPenalty
        move[0][j] = 2
    }

    for (i in 1..a.size) {
        for (j in 1..b.size) {
            val substitute = h[i - 1][j - 1] + elementSimilarity(a[i - 1], b[j - 1])
            val delete = h[i - 1][j] + gapPenalty
            val insert = h[i][j - 1] + gapPenalty
            var best = substitute
            var bestMove = 0
            if (delete > best) {
                best = delete
                bestMove = 1
            }
            if (insert > best) {
                best = insert
                bestMove = 2
            }
            h[i][j] = best
            move[i][j] = bestMove
        }
    }

    val elements = mutableListOf<AlignedElement>()
    var i = a.size
    var j = b.size
    while (i > 0 || j > 0) {
        when {
            i > 0 && j > 0 && move[i][j] == 0 -> {
                elements += AlignedElement(a[i - 1], b[j - 1], elementSimilarity(a[i - 1], b[j - 1]))
                i--
                j--
            }
            i > 0 && (j == 0 || move[i][j] == 1) -> {
                elements += AlignedElement(a[i - 1], null, gapPenalty)
                i--
            }
            else -> {
                elements += AlignedElement(null, b[j - 1], gapPenalty)
                j--
            }
        }
    }
    elements.reverse()

    return SequenceAlignmentResult(h[a.size][b.size] / maxOf(a.size, b.size), elements)
}
