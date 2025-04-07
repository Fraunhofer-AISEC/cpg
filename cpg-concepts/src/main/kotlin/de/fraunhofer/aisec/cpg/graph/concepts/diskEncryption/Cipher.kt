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
package de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import java.util.Objects

/** Represents a cipher suite. E.g. `AES-XTS-plain64` */
class Cipher(underlyingNode: Node) : Concept(underlyingNode = underlyingNode), IsDiskEncryption {
    /** A string representing the cipher used, e.g. `AES-XTS-plain64`. */
    var cipherName: String? = null

    /** Cipher block size. */
    var blockSize: Int? = null

    /** Key size. */
    var keySize: Int? = null

    override fun equals(other: Any?): Boolean {
        return other is Cipher &&
            super.equals(other) &&
            other.cipherName == this.cipherName &&
            other.blockSize == this.blockSize &&
            other.keySize == this.keySize
    }

    override fun hashCode() = Objects.hash(super.hashCode(), cipherName, blockSize, keySize)
}
