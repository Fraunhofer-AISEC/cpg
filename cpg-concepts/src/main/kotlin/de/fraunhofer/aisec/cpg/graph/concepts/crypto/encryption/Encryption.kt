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
import java.util.Objects

/** This concept represents an encryption. */
open class Encryption<T>(underlyingNode: Node? = null) :
    Concept(underlyingNode = underlyingNode), IsEncryption {
    /** The encryption target */
    var target: T? = null

    /** The cipher suite used for disk encryption */
    var cipher: Cipher? = null

    /** The encryption key used for disk encryption */
    var key: Secret? = null

    override fun equals(other: Any?): Boolean {
        return other is Encryption<T> &&
            super.equals(other) &&
            other.target == this.cipher &&
            other.cipher == this.cipher &&
            other.key == this.key
    }

    override fun hashCode() = Objects.hash(super.hashCode(), target, cipher, key)
}
