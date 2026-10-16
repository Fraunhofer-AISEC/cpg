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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.functions
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A real, cross-language comparison: one C file implementing several different encryption-related
 * functions (fetching a secret, the actual encrypt-then-log pipeline, persisting the ciphertext),
 * and three Rust files -- one that implements the *same* functionality, one that implements
 * encryption with a *different* cipher (asymmetric instead of symmetric), and one that implements
 * *decryption* instead of encryption.
 *
 * Every call of interest is tagged with a real, pre-existing `cpg-concepts` overlay (see
 * [tagCryptoConcepts]: [de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Cipher]/`Encrypt`/
 * `Decrypt`/`GetSecret`, [de.fraunhofer.aisec.cpg.graph.concepts.ontology.LogWrite],
 * [de.fraunhofer.aisec.cpg.graph.concepts.file.WriteFile]) -- not a demo/placeholder concept.
 *
 * For each file, [sequencesOf] extracts the union of PDG-reachable concept sequences from every
 * entry point ("public" function, see [isEntryPoint]) via [collectMaximalConceptSequences] (a
 * temporary workaround over [collectConceptSequences], see its KDoc), then [compareSequenceSets]
 * (with [sequenceSimilarity]/[lexicographicSimilarity]) scores the C file against each Rust file.
 */
class CryptoConceptSimilarityTest {

    private val listSimilarity: (List<String>, List<String>) -> Double = { la, lb ->
        sequenceSimilarity(la, lb, lexicographicSimilarity())
    }

    private fun sequencesOf(result: TranslationResult): Set<List<String>> {
        val starts =
            result.functions.filter(::isEntryPoint).mapNotNull { it.calls.firstOrNull() }.toSet()

        // Uses the temporary collectMaximalConceptSequences workaround (see its KDoc) instead of
        // collectConceptSequences: without it, spurious non-exclusive-fork dead ends (e.g. from
        // PointsToPass bookkeeping on C's pointer-typed locals) skew compareSequenceSets' `min`
        // aggregation below and break the expected similarity ordering.
        return collectMaximalConceptSequences(
                starts,
                maxDepth = 20,
                predicate = ::conceptOrOperationOf,
            )
            .mapElements(::conceptLabel)
    }

    // the rust example matching the C example's cipher and direction is the most similar, the one
    // with a different cipher is less similar, and the one that decrypts instead of encrypts is
    // the least similar of the three
    @Test
    fun rustSimilarityOrderingMatchesExpectedRanking() {
        val cDir = Path("src/integrationTest/resources/similarity/crypto/c")
        val rustDir = Path("src/integrationTest/resources/similarity/crypto/rust")

        val c = sequencesOf(analyzeC(cDir, "encryption.c"))
        val same = sequencesOf(analyzeRust(rustDir, "encrypt_same.rs"))
        val asymmetric = sequencesOf(analyzeRust(rustDir, "encrypt_asymmetric.rs"))
        val decrypt = sequencesOf(analyzeRust(rustDir, "decrypt.rs"))

        assertTrue(c.isNotEmpty())
        assertTrue(same.isNotEmpty())
        assertTrue(asymmetric.isNotEmpty())
        assertTrue(decrypt.isNotEmpty())

        val simSame = compareSequenceSets(c, same, listSimilarity)
        val simAsymmetric = compareSequenceSets(c, asymmetric, listSimilarity)
        val simDecrypt = compareSequenceSets(c, decrypt, listSimilarity)

        assertTrue(
            simSame > simAsymmetric,
            "same-cipher Rust ($simSame) should be more similar to C than the different-cipher " +
                "Rust ($simAsymmetric)",
        )
        assertTrue(
            simAsymmetric > simDecrypt,
            "different-cipher Rust ($simAsymmetric) should still be more similar to C than the " +
                "decryption Rust ($simDecrypt)",
        )
    }

    /**
     * DEMO ONLY -- walks through the same comparison as
     * [rustSimilarityOrderingMatchesExpectedRanking] layer by layer (ElementSimilarity -> sequence
     * collection -> SequenceSimilarity -> SetSimilarity) and prints each stage's output to the
     * console. Not an assertion-bearing test; just run it and read stdout.
     */
    @Test
    fun demoPrintSimilarityComputation() {
        val cDir = Path("src/integrationTest/resources/similarity/crypto/c")
        val rustDir = Path("src/integrationTest/resources/similarity/crypto/rust")

        // Run every translation up front so the frontends'/passes' own (often noisy) log output
        // all lands before the demo starts printing, instead of interleaving with it below.
        val cResult = analyzeC(cDir, "encryption.c")
        val sameResult = analyzeRust(rustDir, "encrypt_same.rs")
        val asymmetricResult = analyzeRust(rustDir, "encrypt_asymmetric.rs")
        val decryptResult = analyzeRust(rustDir, "decrypt.rs")

        println()
        println("=".repeat(90))
        println(" DEMO: concept-based similarity scoring, layer by layer")
        println("=".repeat(90))

        println()
        println("--- Layer 1: ElementSimilarity (lexicographicSimilarity) ---")
        val elementSim = lexicographicSimilarity()
        listOf(
                "Cipher:AES-256-GCM" to "Cipher:AES-256-GCM",
                "Cipher:AES-256-GCM" to "Cipher:RSA-2048",
                "Encrypt" to "Decrypt",
            )
            .forEach { (a, b) ->
                println("  sim(\"$a\", \"$b\") = ${"%.3f".format(elementSim(a, b))}")
            }

        println()
        println("--- Layer 2: sequence collection ---")
        val cSeq = printSequencesOf("encryption.c", cResult)
        val sameSeq = printSequencesOf("encrypt_same.rs", sameResult)
        val asymmetricSeq = printSequencesOf("encrypt_asymmetric.rs", asymmetricResult)
        val decryptSeq = printSequencesOf("decrypt.rs", decryptResult)

        println()
        println("--- Layer 3: SequenceSimilarity example (Needleman-Wunsch alignment) ---")
        val exampleA = listOf("Cipher:AES-256-GCM", "Encrypt", "Logging")
        val exampleB = listOf("Cipher:RSA-2048", "Encrypt", "Logging")
        println(
            "  sequenceSimilarity($exampleA, $exampleB) = " +
                "%.3f".format(sequenceSimilarity(exampleA, exampleB, elementSim))
        )

        println()
        println("--- Layer 4: SetSimilarity (compareSequenceSets), all traces per comparison ---")
        val simSame = printComparison("encryption.c", cSeq, "encrypt_same.rs", sameSeq)
        val simAsymmetric =
            printComparison("encryption.c", cSeq, "encrypt_asymmetric.rs", asymmetricSeq)
        val simDecrypt = printComparison("encryption.c", cSeq, "decrypt.rs", decryptSeq)

        println()
        println("--- Verdict ---")
        printVerdict("same-cipher Rust", simSame, "different-cipher Rust", simAsymmetric)
        printVerdict("different-cipher Rust", simAsymmetric, "decryption Rust", simDecrypt)
        println("=".repeat(90))
    }

    /**
     * DEMO ONLY -- prints the raw (unpruned) vs. kept (via [collectMaximalConceptSequences])
     * sequence sets for [result], labeled [name], and returns the kept set for further comparison.
     */
    private fun printSequencesOf(name: String, result: TranslationResult): Set<List<String>> {
        val starts =
            result.functions.filter(::isEntryPoint).mapNotNull { it.calls.firstOrNull() }.toSet()

        val raw =
            collectConceptSequences(starts, maxDepth = 20, predicate = ::conceptOrOperationOf)
                .mapElements(::conceptLabel)
        val kept =
            collectMaximalConceptSequences(
                    starts,
                    maxDepth = 20,
                    predicate = ::conceptOrOperationOf,
                )
                .mapElements(::conceptLabel)
        val dropped = raw - kept

        println("  $name: ${kept.size} traces")
        kept.forEach { println("    trace:    $it") }

        return kept
    }

    /**
     * DEMO ONLY -- prints every trace in [a] alongside its best match in [b] and that pair's score,
     * highlights the single weakest trace (the one driving [compareSequenceSets]'s `min`
     * aggregate), prints the final aggregated score, and returns it.
     */
    private fun printComparison(
        nameA: String,
        a: Set<List<String>>,
        nameB: String,
        b: Set<List<String>>,
    ): Double {
        println()
        println("  $nameA  vs  $nameB")

        var worstLa: List<String>? = null
        var worstLb: List<String>? = null
        var worstScore = Double.POSITIVE_INFINITY
        a.forEach { la ->
            val bestLb = b.maxByOrNull { lb -> listSimilarity(la, lb) }
            val score = bestLb?.let { listSimilarity(la, it) } ?: -1.0
            println("    $la  ->  $bestLb  =  ${"%.3f".format(score)}")
            if (score < worstScore) {
                worstScore = score
                worstLa = la
                worstLb = bestLb
            }
        }

        val result = compareSequenceSets(a, b, listSimilarity)
        println("    weakest trace : $worstLa  ->  $worstLb  =  ${"%.3f".format(worstScore)}")
        println("    SetSimilarity = ${"%.3f".format(result)}")
        return result
    }

    /** DEMO ONLY -- prints whether `scoreA > scoreB` held, mirroring the real test's assertions. */
    private fun printVerdict(nameA: String, scoreA: Double, nameB: String, scoreB: Double) {
        val pass = scoreA > scoreB
        println(
            "  ${"%.3f".format(scoreA)} ($nameA) > ${"%.3f".format(scoreB)} ($nameB) ? " +
                if (pass) "YES -- PASS" else "NO -- FAIL"
        )
    }
}
