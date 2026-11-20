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

import org.slf4j.LoggerFactory

private val log =
    LoggerFactory.getLogger("de.fraunhofer.aisec.cpg.analysis.similarity.ElementSimilarity")

/**
 * The contract every element-similarity metric in this file implements, and the type
 * [sequenceSimilarity]/[sequenceAlignment] expect for their own `elementSimilarity` parameter:
 * given two elements of a comparison (e.g. the structured concept-trace labels [mapElements]
 * produces), return how similar they are - conventionally in `[-1.0, 1.0]`, but exact bounds and
 * what counts as a "mismatch" are metric-specific, see each implementation's own KDoc.
 *
 * Nothing in this type requires `f(a, b) == f(b, a)`. [lexicographicSimilarity] happens to be
 * symmetric; [tokenContainmentSimilarity] deliberately isn't (see its own KDoc for why). Both are
 * equally valid [ElementSimilarity] implementations - callers must not assume symmetry just because
 * the signature takes two same-typed arguments.
 */
typealias ElementSimilarity = (a: String, b: String) -> Double

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
): ElementSimilarity = { a, b ->
    val fieldsA = a.split(delimiter)
    val fieldsB = b.split(delimiter)
    // Here we may need something more relaxed than to fail hard every time the strings are just a
    // bit mismatched.
    val score =
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
    log.trace("concept similarity \"{}\" vs \"{}\" = {}", a, b, score)
    score
}

/**
 * A general-purpose set of English function words (articles, prepositions, conjunctions, auxiliary
 * verbs, ...) that carry no domain-specific meaning on their own and are filtered out by
 * [semanticTokensOf]/[tokenContainmentSimilarity] before comparing tokens.
 *
 * This is the classic 33-word "SMART"/Lucene stop-word list - the same default
 * `StopAnalyzer`/`EnglishAnalyzer` ship in Apache Lucene. It was picked because it's small, makes
 * no assumptions about this project's domain, and is already a de-facto standard for exactly this
 * job (stripping closed-class words before a bag-of-words comparison) rather than because it was
 * tuned for source-code/concept-trace text specifically.
 *
 * A second, much larger option - [SPACY_ENGLISH_STOP_WORDS] - lives next to this one; both are
 * valid `stopWords` arguments for [semanticTokensOf]/[tokenContainmentSimilarity], see that
 * constant's own KDoc for when to prefer it. If neither proves to be the right fit in practice,
 * consider instead of hand-extending one of these two:
 * - Depending on Lucene directly (`org.apache.lucene:lucene-analysis-common`) and using
 *   [`EnglishAnalyzer.ENGLISH_STOP_WORDS_SET`](https://lucene.apache.org/core/) - actively
 *   maintained, Apache-2.0 licensed (same license family as this project), and a
 *   superset-compatible drop-in for the list below.
 * - A corpus-driven list fitted to *this* label format instead of general English: compute term
 *   frequency across every concept-trace label this project has produced (e.g. everything under
 *   `logs/cache/**/concept-traces.yaml` in the DUST repo) and drop whatever occurs in a very large
 *   fraction of traces regardless of library - those are structural to how concepts/operations get
 *   rendered (recurring glue words from LLM-generated prose) rather than discriminative, the same
 *   justification as classic stop-word removal, just data-driven instead of hand-picked.
 *
 * (Apache OpenNLP was considered too, but - unlike Lucene and spaCy - it doesn't ship a bundled
 * English stop-word list of its own; its tools focus on tokenization/POS-tagging/NER/parsing, not
 * stop-word removal, so there is nothing canonical of theirs to port here.)
 *
 * Deliberately does *not* include generic code/documentation words (e.g. "value", "data", "result",
 * "object") - some stop-word lists aimed at technical text include those, but they risk discarding
 * exactly the semantically-relevant tokens this metric exists to compare. Start conservative and
 * widen only with evidence a specific word is hurting results.
 */
val ENGLISH_STOP_WORDS: Set<String> =
    setOf(
        "a",
        "an",
        "and",
        "are",
        "as",
        "at",
        "be",
        "but",
        "by",
        "for",
        "if",
        "in",
        "into",
        "is",
        "it",
        "no",
        "not",
        "of",
        "on",
        "or",
        "such",
        "that",
        "the",
        "their",
        "then",
        "there",
        "these",
        "they",
        "this",
        "to",
        "was",
        "will",
        "with",
    )

/**
 * spaCy's bundled English stop-word list (`spacy/lang/en/stop_words.py`, MIT-licensed), ported
 * verbatim except for its apostrophe-contraction entries (`"n't"`, `"'s"`, `"'ve"`, ...) - those
 * can never match a [semanticTokensOf] token in the first place, since that function's separator
 * split already treats `'` as a delimiter and strips it before stop words are even checked.
 *
 * At ~300 entries, this is roughly 9x [ENGLISH_STOP_WORDS]' size: besides the same closed-class
 * words (articles/prepositions/conjunctions/pronouns), it also drops modal and hedging verbs
 * ("could", "would", "should", "might"), vague quantifiers ("many", "much", "various", "several"),
 * spelled-out numbers ("one" through "hundred"), and discourse connectives ("however", "therefore",
 * "moreover", "nevertheless"). Prefer this over [ENGLISH_STOP_WORDS] when concept-trace labels are
 * closer to prose (e.g. the LLM-generated property values seen in `concept-traces.yaml`, as opposed
 * to short identifier-like concept/operation names) - the larger cut strips more of that prose's
 * connective tissue, leaving `tokenContainmentSimilarity` a token set that is more concentrated on
 * actual domain nouns/verbs; the tradeoff is a higher chance of also discarding a word that
 * happened to be semantically load-bearing in a specific label (e.g. "various" inside a label that
 * is specifically about handling multiple/heterogeneous inputs).
 */
val SPACY_ENGLISH_STOP_WORDS: Set<String> =
    setOf(
        "a",
        "about",
        "above",
        "across",
        "after",
        "afterwards",
        "again",
        "against",
        "all",
        "almost",
        "alone",
        "along",
        "already",
        "also",
        "although",
        "always",
        "am",
        "among",
        "amongst",
        "amount",
        "an",
        "and",
        "another",
        "any",
        "anyhow",
        "anyone",
        "anything",
        "anyway",
        "anywhere",
        "are",
        "around",
        "as",
        "at",
        "back",
        "be",
        "became",
        "because",
        "become",
        "becomes",
        "becoming",
        "been",
        "before",
        "beforehand",
        "behind",
        "being",
        "below",
        "beside",
        "besides",
        "between",
        "beyond",
        "both",
        "bottom",
        "but",
        "by",
        "ca",
        "call",
        "can",
        "cannot",
        "could",
        "did",
        "do",
        "does",
        "doing",
        "done",
        "down",
        "due",
        "during",
        "each",
        "eight",
        "either",
        "eleven",
        "else",
        "elsewhere",
        "empty",
        "enough",
        "even",
        "ever",
        "every",
        "everyone",
        "everything",
        "everywhere",
        "except",
        "few",
        "fifteen",
        "fifty",
        "first",
        "five",
        "for",
        "former",
        "formerly",
        "forty",
        "four",
        "from",
        "front",
        "full",
        "further",
        "get",
        "give",
        "go",
        "had",
        "has",
        "have",
        "he",
        "hence",
        "her",
        "here",
        "hereafter",
        "hereby",
        "herein",
        "hereupon",
        "hers",
        "herself",
        "him",
        "himself",
        "his",
        "how",
        "however",
        "hundred",
        "i",
        "if",
        "in",
        "indeed",
        "into",
        "is",
        "it",
        "its",
        "itself",
        "just",
        "keep",
        "last",
        "latter",
        "latterly",
        "least",
        "less",
        "made",
        "make",
        "many",
        "may",
        "me",
        "meanwhile",
        "might",
        "mine",
        "more",
        "moreover",
        "most",
        "mostly",
        "move",
        "much",
        "must",
        "my",
        "myself",
        "name",
        "namely",
        "neither",
        "never",
        "nevertheless",
        "next",
        "nine",
        "no",
        "nobody",
        "none",
        "noone",
        "nor",
        "not",
        "nothing",
        "now",
        "nowhere",
        "of",
        "off",
        "often",
        "on",
        "once",
        "one",
        "only",
        "onto",
        "or",
        "other",
        "others",
        "otherwise",
        "our",
        "ours",
        "ourselves",
        "out",
        "over",
        "own",
        "part",
        "per",
        "perhaps",
        "please",
        "put",
        "quite",
        "rather",
        "re",
        "really",
        "regarding",
        "same",
        "say",
        "see",
        "seem",
        "seemed",
        "seeming",
        "seems",
        "serious",
        "several",
        "she",
        "should",
        "show",
        "side",
        "since",
        "six",
        "sixty",
        "so",
        "some",
        "somehow",
        "someone",
        "something",
        "sometime",
        "sometimes",
        "somewhere",
        "still",
        "such",
        "take",
        "ten",
        "than",
        "that",
        "the",
        "their",
        "them",
        "themselves",
        "then",
        "thence",
        "there",
        "thereafter",
        "thereby",
        "therefore",
        "therein",
        "thereupon",
        "these",
        "they",
        "third",
        "this",
        "those",
        "though",
        "three",
        "through",
        "throughout",
        "thru",
        "thus",
        "to",
        "together",
        "too",
        "top",
        "toward",
        "towards",
        "twelve",
        "twenty",
        "two",
        "under",
        "unless",
        "until",
        "up",
        "upon",
        "us",
        "used",
        "using",
        "various",
        "very",
        "via",
        "was",
        "we",
        "well",
        "were",
        "what",
        "whatever",
        "when",
        "whence",
        "whenever",
        "where",
        "whereafter",
        "whereas",
        "whereby",
        "wherein",
        "whereupon",
        "wherever",
        "whether",
        "which",
        "while",
        "whither",
        "who",
        "whoever",
        "whole",
        "whom",
        "whose",
        "why",
        "will",
        "with",
        "within",
        "without",
        "would",
        "yet",
        "you",
        "your",
        "yours",
        "yourself",
        "yourselves",
    )

/** Matches any run of characters that is neither a letter nor a digit (see [semanticTokensOf]). */
private val nonAlphanumericRunRegex = Regex("[^\\p{L}\\p{N}]+")

/**
 * Splits [label] into its semantically-relevant lowercase tokens, as used by
 * [tokenContainmentSimilarity]:
 * 1. Split on every run of characters that is neither a letter nor a digit - this is what strips
 *    structural delimiters (the `:` [lexicographicSimilarity] relies on, the `.` between a concept
 *    class and its operation, and the `,`/`-`/`_`/parentheses/angle-brackets/backticks/`*`/`&`/`=`/
 *    ... that show up inside concept-trace property values - see any `concept-traces.yaml` under
 *    `logs/cache/` in the DUST repo) without having to enumerate every symbol by hand.
 * 2. Lowercase every token, so e.g. `"Memory"` and `"memory"` compare equal.
 * 3. Drop empty tokens produced by consecutive separators, and any token in [stopWords] - matched
 *    against the now-lowercased token, so [stopWords] itself only needs lowercase entries (see
 *    [ENGLISH_STOP_WORDS]).
 */
fun semanticTokensOf(label: String, stopWords: Set<String> = ENGLISH_STOP_WORDS): List<String> =
    label
        .split(nonAlphanumericRunRegex)
        .map { it.lowercase() }
        .filter { it.isNotEmpty() && it !in stopWords }

/**
 * Element-similarity that compares the *semantically relevant* tokens of [a] and [b] instead of
 * [lexicographicSimilarity]'s positional field-by-field string comparison: both are tokenized with
 * [semanticTokensOf] (split on separators, lowercased, stop words dropped), and the score is the
 * fraction of [a]'s tokens found in [b]'s, linearly rescaled from `[0.0, 1.0]` to `[-1.0, 1.0]`
 * (`0%` contained -> `-1.0`, `100%` contained -> `1.0`) so this lines up with
 * [lexicographicSimilarity]'s own range and, more importantly, with [sequenceAlignment]'s default
 * `gapPenalty` of `-1.0` - a total mismatch here costs exactly as much as a gap, instead of only
 * half as much. Intended usage is `a` = an element from the "original" side of a comparison (e.g. a
 * C concept-trace label) and `b` = an element from the "candidate" side (e.g. a Rust concept-trace
 * label), mirroring [de.fraunhofer.aisec.tools.compareLibraries]'s own one-directional "does every
 * C concept have a good Rust match" framing.
 *
 * A token is considered "found" via a bidirectional substring check (`bt.contains(t) ||
 * t.contains(bt)`), not exact equality: this is a deliberately lightweight, stemming-free stand-in
 * for morphological matching, so e.g. a C-side token `"decode"` is treated as present against a
 * Rust-side `"decoder"`/`"decoded"` without needing a real stemmer.
 *
 * **This is not symmetric.** `tokenContainmentSimilarity()(a, b)` need not equal the same call with
 * [a] and [b] swapped - e.g. [b] may simply have many more/richer tokens, so everything in [a] is
 * found in [b] without the reverse holding. That asymmetry is the point: it makes this metric
 * tolerant of two independently-written implementations describing the same concept with
 * differently-sized vocabularies, at the cost of no longer being a true distance metric. Compose a
 * symmetric score at the call site if one is needed, e.g. the average of both directions.
 *
 * Returns `1.0` (vacuously similar, nothing left to disagree on - the same convention
 * [lexicographicSimilarity] uses once its own fields run out) when [a] has no tokens left after
 * filtering, e.g. an empty label or one made entirely of stop words/punctuation.
 */
fun tokenContainmentSimilarity(stopWords: Set<String> = ENGLISH_STOP_WORDS): ElementSimilarity =
    { a, b ->
        val tokensA = semanticTokensOf(a, stopWords)
        val tokensB = semanticTokensOf(b, stopWords)
        val score =
            if (tokensA.isEmpty()) {
                1.0
            } else {
                val contained =
                    tokensA.count { t -> tokensB.any { bt -> bt.contains(t) || t.contains(bt) } }
                // The raw contained-fraction is [0.0, 1.0]; rescale to [-1.0, 1.0] so a total
                // mismatch (0% contained) lands at -1.0, the same worst case
                // lexicographicSimilarity's default mismatchScore and sequenceAlignment's default
                // gapPenalty use - without this, every mismatch here would cost only half as much
                // as a gap in the surrounding Needleman-Wunsch alignment.
                (contained.toDouble() / tokensA.size) * 2.0 - 1.0
            }
        log.trace("token containment similarity \"{}\" vs \"{}\" = {}", a, b, score)
        score
    }
