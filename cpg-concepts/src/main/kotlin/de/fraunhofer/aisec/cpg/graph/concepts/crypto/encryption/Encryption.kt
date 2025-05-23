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
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.Secret
import java.util.Objects

/** This concept represents an encryption. */
open class Encryption<T>(underlyingNode: Node? = null) :
    Concept(underlyingNode = underlyingNode), IsEncryption {
    /** The encryption target */
    var target: T? = null

    /** The secret key used for the encryption */
    var secretKey: Secret? = null

    override fun equals(other: Any?): Boolean {
        return other is Encryption<T> &&
            super.equals(other) &&
            other.target == this.target &&
            other.secretKey == this.secretKey
    }

    override fun hashCode() = Objects.hash(super.hashCode(), target, secretKey)
}

/** This concept represents things related to symmetric encryption. */
open class SymmetricEncryption<T>(underlyingNode: Node? = null) : Encryption<T>(underlyingNode) {
    /** The cipher suite used for the encryption */
    var cipher: Cipher? = null

    override fun equals(other: Any?): Boolean {
        return other is SymmetricEncryption<T> && super.equals(other) && other.cipher == this.cipher
    }

    override fun hashCode() = Objects.hash(super.hashCode(), cipher)
}

/** This concept represents things related to symmetric encryption. */
open class AsymmetricEncryption<T>(underlyingNode: Node? = null) : Encryption<T>(underlyingNode) {
    /** The name of the algorithm used for encryption and decryption. */
    var algorithmName: String? = null

    /** The size of the key. */
    var keySize: Int? = null

    /** The public key used for the encryption */
    var publicKey: Secret? = null

    override fun equals(other: Any?): Boolean {
        return other is AsymmetricEncryption<T> &&
            super.equals(other) &&
            other.algorithmName == this.algorithmName &&
            other.publicKey == this.publicKey &&
            other.keySize == this.keySize
    }

    override fun hashCode() = Objects.hash(super.hashCode(), algorithmName, publicKey, keySize)
}
