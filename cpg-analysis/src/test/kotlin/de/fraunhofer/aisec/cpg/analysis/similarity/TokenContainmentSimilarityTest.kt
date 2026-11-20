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

class TokenContainmentSimilarityTest {
    private val sim = tokenContainmentSimilarity()

    private fun assertApprox(expected: Double, actual: Double) =
        assertTrue(abs(expected - actual) < 1e-9, "expected $expected but got $actual")

    @Test
    fun identicalLabelsScoreOne() {
        assertEquals(1.0, sim("Memory.allocate:codec handle", "Memory.allocate:codec handle"))
    }

    // separator symbols (".", ":", ",", "-", " ", "_") are all stripped, not compared literally
    @Test
    fun separatorSymbolsAreTreatedAsDelimiters() {
        val a = "Memory.allocate:codec handle, cleanup-buffer_state"
        val b = "codec handle cleanup buffer state memory allocate"
        assertEquals(1.0, sim(a, b))
    }

    // stop words don't count towards or against the score
    @Test
    fun stopWordsAreFiltered() {
        assertEquals(1.0, sim("the Memory of the decoder", "Memory decoder"))
    }

    // a substring match (no stemmer) is enough - "decode" is found inside "decoder"
    @Test
    fun substringContainmentMatchesMorphologicalVariants() {
        assertEquals(1.0, sim("decode", "decoder"))
    }

    // 0% contained is rescaled to -1.0, the same worst case as lexicographicSimilarity's
    // mismatchScore and sequenceAlignment's gapPenalty default to
    @Test
    fun noOverlapScoresMinusOne() {
        assertEquals(-1.0, sim("Memory", "Checksum"))
    }

    // nothing left of "a" after filtering (all stop words) -> vacuously similar
    @Test
    fun allStopWordsScoresOne() {
        assertEquals(1.0, sim("the of it", "Memory decoder"))
    }

    @Test
    fun emptyLabelScoresOne() {
        assertEquals(1.0, sim("", "Memory decoder"))
    }

    // this is the whole point of the metric: swapping a/b need not give the same score
    @Test
    fun comparisonIsNotSymmetric() {
        val rich = "Decode.decode_stream:parses header"
        val sparse = "Decode"

        // every relevant token of "sparse" ("decode") is found in "rich" -> 100% -> 1.0
        assertEquals(1.0, sim(sparse, rich))
        // only 2 of "rich"'s 5 relevant tokens ("decode" twice) are found in "sparse" -> 40%,
        // rescaled from [0.0, 1.0] to [-1.0, 1.0] -> -0.2
        assertApprox(-0.2, sim(rich, sparse))
    }

    // a custom stop-word list is honored instead of the default ENGLISH_STOP_WORDS
    @Test
    fun customStopWordsAreHonored() {
        assertEquals(-1.0, sim("decoder", "memory"))

        val custom = tokenContainmentSimilarity(stopWords = setOf("decoder"))
        assertEquals(1.0, custom("decoder", "memory"))
    }

    // SPACY_ENGLISH_STOP_WORDS additionally drops modal verbs ("could") and vague quantifiers
    // ("many", "various") that ENGLISH_STOP_WORDS (the 33-word Lucene/SMART list) doesn't - on
    // LLM-generated prose labels, that extra cut removes noise words that can never find a match
    // anyway, instead of letting them drag the score down as unmatched tokens.
    @Test
    fun spacyStopWordsStripMoreProseNoiseThanTheLuceneList() {
        val a = "This could read many various buffer formats"
        val b = "buffer format reading"

        val lucene = tokenContainmentSimilarity(stopWords = ENGLISH_STOP_WORDS)
        val spacy = tokenContainmentSimilarity(stopWords = SPACY_ENGLISH_STOP_WORDS)

        // ENGLISH_STOP_WORDS only drops "this", leaving "could"/"many"/"various" as tokens that can
        // never match anything in "b" - only "read"/"buffer"/"formats" do, so 3 of 6 (50%),
        // rescaled
        // from [0.0, 1.0] to [-1.0, 1.0] -> 0.0.
        assertEquals(0.0, lucene(a, b))
        // SPACY_ENGLISH_STOP_WORDS also drops "could"/"many"/"various", leaving only the 3 tokens
        // that do have a match in "b" - 100% -> 1.0.
        assertEquals(1.0, spacy(a, b))
    }
}
