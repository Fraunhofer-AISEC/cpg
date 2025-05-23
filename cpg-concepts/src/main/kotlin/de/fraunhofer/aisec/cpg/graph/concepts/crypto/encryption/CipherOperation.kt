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
package de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.Secret
import java.util.Objects

abstract class CipherOperation(underlyingNode: Node?, override val concept: Encryption<Node>) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsEncryption

open class Encrypt(
    underlyingNode: Node? = null,
    concept: Encryption<Node>,
    /** The key used for encryption */
    val key: Secret,
    var plaintext: Node? = null,
    var ciphertext: Node? = null,
) : CipherOperation(underlyingNode = underlyingNode, concept = concept) {

    override fun equals(other: Any?): Boolean {
        return other is Encrypt &&
            super.equals(other) &&
            other.key == this.key &&
            plaintext == other.plaintext &&
            ciphertext == other.ciphertext
    }

    override fun hashCode() = Objects.hash(super.hashCode(), key, plaintext, ciphertext)
}

open class Decrypt(
    underlyingNode: Node? = null,
    concept: Encryption<Node>,
    /** The key used for encryption */
    val key: Secret,
    var plaintext: Node? = null,
    var ciphertext: Node? = null,
) : CipherOperation(underlyingNode = underlyingNode, concept = concept) {

    override fun equals(other: Any?): Boolean {
        return other is Decrypt &&
            super.equals(other) &&
            other.key == this.key &&
            plaintext == other.plaintext &&
            ciphertext == other.ciphertext
    }

    override fun hashCode() = Objects.hash(super.hashCode(), key, plaintext, ciphertext)
}
