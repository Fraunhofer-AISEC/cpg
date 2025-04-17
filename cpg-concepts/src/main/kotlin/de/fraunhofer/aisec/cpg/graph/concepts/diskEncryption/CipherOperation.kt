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
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import java.util.Objects

abstract class CipherOperation(underlyingNode: Node?, override val concept: Cipher) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsDiskEncryption

class Encrypt(
    underlyingNode: Node? = null,
    concept: Cipher,
    /** The key used for encryption */
    val key: Secret,
) : CipherOperation(underlyingNode = underlyingNode, concept = concept) {

    override fun equals(other: Any?): Boolean {
        return other is Encrypt && super.equals(other) && other.key == this.key
    }

    override fun hashCode() = Objects.hash(super.hashCode(), key)
}
