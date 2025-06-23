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
package de.fraunhofer.aisec.cpg.graph.concepts.encryption

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.Cipher
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.Secret
import java.util.Objects

interface Cryptography

enum class HashingAlgorithm(val algorithmName: String) {
    MD5("md5"),
    SHA1("sha-1"),
    SHA224("sha-224"),
    SHA256("sha-256"),
    SHA384("sha-384"),
    SHA512("sha-512"),
    BLAKE2B("blake2b"),
    BLAKE2S("blake2s"),
    SHA3_224("sha3-224"),
    SHA3_256("sha3-256"),
    SHA3_384("sha3-384"),
    SHA3_512("sha3-512"),
    SHAKE128("shake-128"),
    SHAKE256("shake-256"),
    UNKNOWN("unknown"),
}

class Hashing(underlyingNode: Node) : Concept(underlyingNode), Cryptography {
    var algorithm: HashingAlgorithm = HashingAlgorithm.UNKNOWN

    override fun equals(other: Any?): Boolean {
        return other is Hashing && super.equals(other) && other.algorithm == this.algorithm
    }

    override fun hashCode() = Objects.hash(super.hashCode(), algorithm)
}

open class Encryption(underlyingNode: Node) : Concept(underlyingNode), Cryptography

/** This concept represents things related to symmetric encryption. */
class SymmetricEncryption(underlyingNode: Node) : Encryption(underlyingNode) {
    /** The cipher suite used for encryption and decryption */
    var cipher: Cipher? = null

    /** The secret key used for encrypting and decrypting data */
    var key: Secret? = null

    override fun equals(other: Any?): Boolean {
        return other is SymmetricEncryption &&
            super.equals(other) &&
            other.cipher == this.cipher &&
            other.key == this.key
    }

    override fun hashCode() = Objects.hash(super.hashCode(), cipher, key)
}

/** This concept represents things related to symmetric encryption. */
class AsymmetricEncryption(underlyingNode: Node) : Encryption(underlyingNode) {
    /** The name of the algorithm used for encryption and decryption. */
    var algorithmName: String? = null

    /** The size of the key. */
    var keySize: Int? = null

    /** The public key used for disk encryption */
    var publicKey: Secret? = null
    /** The encryption key used for disk encryption */
    var privateKey: Secret? = null

    override fun equals(other: Any?): Boolean {
        return other is AsymmetricEncryption &&
            super.equals(other) &&
            other.algorithmName == this.algorithmName &&
            other.privateKey == this.privateKey &&
            other.publicKey == this.publicKey &&
            other.keySize == this.keySize
    }

    override fun hashCode() =
        Objects.hash(super.hashCode(), algorithmName, privateKey, publicKey, keySize)
}

class EncryptData(underlyingNode: Node, concept: Encryption) :
    Operation(underlyingNode = underlyingNode, concept = concept), Cryptography {
    var dataToEncrypt: Node? = null
    var encryptedData: Node? = null

    override fun equals(other: Any?): Boolean {
        return other is EncryptData &&
            super.equals(other) &&
            other.dataToEncrypt == this.dataToEncrypt &&
            other.encryptedData == this.encryptedData
    }

    override fun hashCode() = Objects.hash(super.hashCode(), encryptedData, dataToEncrypt)
}

class DecryptData(underlyingNode: Node, concept: Encryption) :
    Operation(underlyingNode = underlyingNode, concept = concept), Cryptography {
    var dataToDecrypt: Node? = null
    var decryptedData: Node? = null

    override fun equals(other: Any?): Boolean {
        return other is DecryptData &&
            super.equals(other) &&
            other.dataToDecrypt == this.dataToDecrypt &&
            other.decryptedData == this.decryptedData
    }

    override fun hashCode() = Objects.hash(super.hashCode(), decryptedData, dataToDecrypt)
}
