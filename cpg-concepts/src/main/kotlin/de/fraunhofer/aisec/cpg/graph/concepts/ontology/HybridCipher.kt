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
package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class HybridCipher(
    public val keyDerivationFunction: KeyDerivationFunction?,
    public val messageAuthenticationCode: MessageAuthenticationCode?,
    public val symmetricCipher: SymmetricCipher?,
    public val uses: AsymmetricCipher?,
    blockSize: Int?,
    cipherName: String?,
    keySize: Int?,
    padding: Padding?,
    underlyingNode: Node? = null,
) : Cipher(blockSize, cipherName, keySize, padding, underlyingNode) {
    override fun equals(other: Any?): Boolean =
        other is HybridCipher &&
            super.equals(other) &&
            other.keyDerivationFunction == this.keyDerivationFunction &&
            other.messageAuthenticationCode == this.messageAuthenticationCode &&
            other.symmetricCipher == this.symmetricCipher &&
            other.uses == this.uses

    override fun hashCode(): Int =
        Objects.hash(
            super.hashCode(),
            keyDerivationFunction,
            messageAuthenticationCode,
            symmetricCipher,
            uses,
        )
}
