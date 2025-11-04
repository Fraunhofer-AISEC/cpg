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
package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableList

public open class CipherSuite(
    public val authenticationMechanism: String?,
    public val keyExchangeAlgorithm: String?,
    public val macAlgorithm: String?,
    public val ciphers: MutableList<Cipher?>,
    underlyingNode: Node? = null,
) : Functionality(underlyingNode) {
    override fun equals(other: Any?): Boolean =
        other is CipherSuite &&
            super.equals(other) &&
            other.authenticationMechanism == this.authenticationMechanism &&
            other.keyExchangeAlgorithm == this.keyExchangeAlgorithm &&
            other.macAlgorithm == this.macAlgorithm &&
            other.ciphers == this.ciphers

    override fun hashCode(): Int =
        Objects.hash(
            super.hashCode(),
            authenticationMechanism,
            keyExchangeAlgorithm,
            macAlgorithm,
            ciphers,
        )
}
