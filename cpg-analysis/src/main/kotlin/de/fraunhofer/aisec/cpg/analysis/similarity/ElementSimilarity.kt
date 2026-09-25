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
 * Element-similarity for structured labels of the form
 * `"<conceptClass><delimiter><attr1><delimiter>...<attrN>"` (e.g. produced by [mapElements] from
 * [de.fraunhofer.aisec.cpg.graph.concepts.Concept]/
 * [de.fraunhofer.aisec.cpg.graph.concepts.Operation] overlay nodes), meant to be plugged in as the
 * `elementSimilarity` of [sequenceSimilarity].
 * - Field `0` (the concept class) is a hard gate: if it doesn't match between [a] and [b], the pair
 *   is treated as fundamentally unrelated and this returns [mismatchScore], regardless of any other
 *   field.
 * - If field `0` matches, the score is the fraction of the remaining fields that match
 *   position-wise, in `[0.0, 1.0]`. A label with no attribute fields beyond the class scores `1.0`
 *   once the class matches (nothing left to disagree on).
 * - If [a] and [b] have a different number of fields, the comparison is padded to
 *   `max(fieldsA.size, fieldsB.size)`; a field present on one side and absent on the other counts
 *   as a mismatch at that position.
 */
fun lexicographicSimilarity(
    delimiter: String = ":",
    mismatchScore: Double = -1.0,
): (String, String) -> Double = { a, b ->
    val fieldsA = a.split(delimiter)
    val fieldsB = b.split(delimiter)

    if (fieldsA[0] != fieldsB[0]) {
        mismatchScore
    } else {
        val attributeCount = maxOf(fieldsA.size, fieldsB.size) - 1
        if (attributeCount <= 0) {
            1.0
        } else {
            val matches =
                (1..attributeCount).count { i -> fieldsA.getOrNull(i) == fieldsB.getOrNull(i) }
            matches.toDouble() / attributeCount
        }
    }
}
