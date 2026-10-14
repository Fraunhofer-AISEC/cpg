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
}
