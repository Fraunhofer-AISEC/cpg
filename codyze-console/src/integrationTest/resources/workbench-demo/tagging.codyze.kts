/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package workbench

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Cipher
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Encrypt
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.GetSecret
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Secret
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.passes.concepts.*

/**
 * Attaches concepts and operations to the demo, because there is no concept pass for C. Concepts
 * are the things (the key, the cipher), operations are what the code does with them (getting the
 * key, encrypting with the cipher):
 * - the variable `key` is a [Secret], which the call of `get_key` gets ([GetSecret]),
 * - the function `encrypt` in `crypto.c` implements a [Cipher], and each call of it encrypts with
 *   it ([Encrypt]), using the secret that flows into its first argument as the key.
 *
 * The concepts show up in the code, the outline, the file tree and the dependence graph.
 */
project {
    // All of the tagging has to be in one tag block, only the last one is used
    tagging {
        tag {
            each<Call>("get_key").withMultiple {
                val secret =
                    Secret()
                        .assume(
                            AssumptionType.CompletenessAssumption,
                            "We assume that get_key always returns a valid key.",
                        )
                // The secret is the variable that holds the key, not the call that gets it
                propagate<Call, Node> { call -> (call.astParent as? Variable) ?: call }
                    .with { secret }
                listOf(GetSecret(concept = secret))
            }
            each<Call>("encrypt").with {
                val key =
                    node.arguments.firstOrNull()?.getOverlaysByPrevDFG<Secret>(state)?.firstOrNull()
                        ?: return@with null
                // The cipher belongs to the function that implements it (in crypto.c), and is
                // created with its first call
                val functions = node.invokes.filter { it.body != null }
                val cipher =
                    functions
                        .flatMap { (state[it] ?: emptySet()) + it.overlays }
                        .filterIsInstance<Cipher>()
                        .firstOrNull()
                        ?: xorCipher().also { cipher ->
                            functions.forEach { function ->
                                propagate<Call, Node> { function }.with { cipher }
                            }
                        }
                Encrypt(concept = cipher, key = key, plaintext = node.arguments.getOrNull(1))
            }
        }
    }
}

/** The cipher of `encrypt`: it XORs the data with the first 8 bytes of the key. */
fun xorCipher() =
    Cipher().apply {
        cipherName = "XOR"
        keySize = 64
    }
