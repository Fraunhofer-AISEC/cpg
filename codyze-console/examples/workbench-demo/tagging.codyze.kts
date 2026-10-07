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

import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Cipher
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Encrypt
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.GetSecret
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Secret
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.passes.concepts.*

/**
 * Attaches concepts to the calls of the demo, because there is no concept pass for C: `get_key`
 * returns a secret and `encrypt` encrypts with a cipher. The concepts show up in the code, the
 * outline, the file tree and the dependence graph.
 */
project {
    tagging {
        tag {
            each<Call>("get_key").withMultiple {
                val secret =
                    Secret()
                        .also { secret -> secret.location = node.location }
                        .assume(
                            AssumptionType.CompletenessAssumption,
                            "We assume that get_key always returns a valid key.",
                        )
                listOf(GetSecret(concept = secret), secret)
            }
        }
        tag {
            each<Call>("encrypt").withMultiple {
                val cipher = Cipher().also { cipher -> cipher.location = node.location }
                val key = Secret().also { secret -> secret.location = node.location }
                listOf(Encrypt(concept = cipher, key = key), cipher, key)
            }
        }
    }
}
