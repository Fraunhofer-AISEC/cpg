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
package de.fraunhofer.aisec.cpg.graph.concepts.crypto.hashing

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.IsCryptography
import java.util.Objects

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

open class Hashing(underlyingNode: Node) : Concept(underlyingNode), IsCryptography {
    var algorithm: HashingAlgorithm = HashingAlgorithm.UNKNOWN

    override fun equals(other: Any?): Boolean {
        return other is Hashing && super.equals(other) && other.algorithm == this.algorithm
    }

    override fun hashCode() = Objects.hash(super.hashCode(), algorithm)
}

open class HashOperation(
    underlyingNode: Node?,
    val input: Node? = null,
    val output: Node? = null,
    override val concept: Hashing,
) : Operation(underlyingNode = underlyingNode, concept = concept), IsCryptography {

    override fun equals(other: Any?): Boolean {
        return other is HashOperation &&
            super.equals(other) &&
            this.input == other.input &&
            this.output == other.output
    }

    override fun hashCode() = Objects.hash(super.hashCode(), input, output)
}
